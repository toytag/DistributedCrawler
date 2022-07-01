package edu.upenn.cis.cis455.crawler.distributed.thread;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.*;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.collect.Streams;
import com.google.common.hash.BloomFilter;
import com.google.common.util.concurrent.MoreExecutors;
import crawlercommons.robots.SimpleRobotRules;
import edu.upenn.cis.cis455.crawler.distributed.thread.singleton.*;
import edu.upenn.cis.cis455.crawler.utils.*;
import edu.upenn.cis.cis455.storage.dynamodb.DocumentData;
import edu.upenn.cis.cis455.storage.dynamodb.DynamoDBInterface;
import edu.upenn.cis.cis455.storage.dynamodb.UrlData;
import edu.upenn.cis.cis455.storage.utils.StringUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.javatuples.Pair;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class AsyncCrawler {
    static final Logger LOGGER = LoggerFactory.getLogger(AsyncCrawler.class);

    static final int DEFAULT_SIZE_LIMIT = 1024 * 1024;

    static final AtomicInteger count = CounterSingleton.getSingleton();
    static final AmazonSQS sqs = AmazonSqsSingleton.getSingleton();
    static final DynamoDBInterface dynamoDb = DynamoDbSingleton.getSingleton();
    static final BloomFilter<String> inboundFilter = BloomFilterSingleton.getInboundFilter();
    static final BloomFilter<String> outboundFilter = BloomFilterSingleton.getOutboundFilter();
    static final RobotRulesCache robotRulesCache = RobotRulesCacheSingleton.getSingleton();

    static final int NUM_WORKERS = 2 * Runtime.getRuntime().availableProcessors();
    static final ExecutorService main = Executors.newFixedThreadPool(NUM_WORKERS);

    public AsyncCrawler() {
    }

    public void start() {
        for (int i = 0; i < NUM_WORKERS; i++)
            main.execute(() -> {
                while (!main.isShutdown()) {

                    List<Message> messages = getURLs(50);

                    LOGGER.debug("Emitting " + messages.size() + " messages");

                    List<Pair<UrlData, DocumentData>> data = messages.parallelStream()
                            .flatMap(AsyncCrawler::getResponse)
                            .flatMap(AsyncCrawler::parseDocument)
                            .collect(Collectors.toUnmodifiableList());

                    LOGGER.debug(data.size() + " data parsed");

                    // async upload
                    CompletableFuture.runAsync(() -> {
                        List<SendMessageBatchRequestEntry> entries = Streams.mapWithIndex(
                                data.stream()
                                        .flatMap((item) -> item.getValue0().getOutboundLinks().stream())
                                        .collect(Collectors.toUnmodifiableSet()).stream(),
                                (link, idx) -> new SendMessageBatchRequestEntry()
                                        .withMessageGroupId(link).withId(String.valueOf(idx)).withMessageBody(link)
                        ).collect(Collectors.toUnmodifiableList());

                        // 10 max per batch for sqs request
                        Lists.partition(entries, AmazonSqsSingleton.MAX_BATCH_SIZE)
                                .parallelStream().forEach(AsyncCrawler::sendURLs);

                        LOGGER.debug("Uploading " + entries.size() + " messages");


                        // 25 max per batch for db update
                        Lists.partition(data, DynamoDbSingleton.MAX_BATCH_SIZE)
                                .parallelStream().forEach(AsyncCrawler::uploadData);

                        // 10 max per batch for sqs request
                        Lists.partition(messages, AmazonSqsSingleton.MAX_BATCH_SIZE)
                                .parallelStream().forEach(AsyncCrawler::ack);
                    });
                }
            });
    }

    static List<Message> getURLs(int number) {
        return IntStream.range(0, number / AmazonSqsSingleton.MAX_BATCH_SIZE).parallel()
                .mapToObj((i) -> sqs.receiveMessage(new ReceiveMessageRequest()
                        .withQueueUrl(AmazonSqsSingleton.URL_QUEUE_FIFO).withMaxNumberOfMessages(10)
                        .withWaitTimeSeconds(20)).getMessages())
                .flatMap(List::stream)
                .collect(Collectors.toUnmodifiableList());
    }

    static Stream<Pair<String, Connection.Response>> getResponse(Message msg) {
        try {
            String url = UrlUtils.normalize(msg.getBody());

            // deduplication
            if (inboundFilter.mightContain(url))
                return Stream.empty();
            inboundFilter.put(url);
            outboundFilter.put(url);
            // robots.txt check
            SimpleRobotRules robotRules = robotRulesCache.get(UrlUtils.robots(url));
            if (!robotRules.isAllowed(url))
                return Stream.empty();

            Connection.Response response = Jsoup.connect(url)
                    .method(Connection.Method.GET)
                    .userAgent("cis455crawler")
                    .ignoreContentType(true)
                    .followRedirects(true)
                    .maxBodySize(DEFAULT_SIZE_LIMIT)
                    .execute();

            return Stream.of(new Pair<>(url, response));
        } catch (Exception ignored) {
            return Stream.empty();
        }
    }

    static Stream<Pair<UrlData, DocumentData>> parseDocument(Pair<String, Connection.Response> in) {
        String url = in.getValue0();
        Connection.Response response = in.getValue1();
        SimpleRobotRules robotRules = robotRulesCache.get(UrlUtils.robots(url));

        // preparing document
        Date date = DateUtils.getDate();
        String document;

        // find all links in the document if html
        Set<String> outboundLinks = Sets.newHashSet(url);
        try {
            if (TikaUtils.isHTMLorXMLorTEXT(response.contentType())) {
                Document doc = response.parse();
                document = doc.text();
                for (Element linkElem : doc.select("a[href]")) {
                    String link = UrlUtils.normalize(linkElem.attr("abs:href"));
                    // outbound url filtering, robots.txt not allowed urls will never be visited
                    if (!link.isEmpty() && !outboundFilter.mightContain(link) && robotRules.isAllowed(link)) {
                        outboundFilter.put(link);
                        outboundLinks.add(link);
                    }
                }
            } else {
                // possible exception TikaUtils.parseContent
                document = TikaUtils.parseContent(new ByteArrayInputStream(response.bodyAsBytes()));
            }
        } catch (Exception ignored) {
            return Stream.empty();
        }

        // only save pages in english language
        if (!TikaUtils.detectLanguage(document).equals("en")) {
            return Stream.empty();
        }

        // simple processing to save space
        document = document.replaceAll("\\s+", " ");

        // dynamodb has 400KB item size limit
        if (document.length() > 392 * 1000) {
            document = document.substring(0, 392 * 1000);
            document = document.substring(0, document.lastIndexOf(" "));
        }

        // md5 indexing
        String md5 = StringUtils.hash(document, "MD5");

        // generate data for db storage
        UrlData urlData = new UrlData();
        urlData.setUrl(url);
        urlData.setMd5(md5);
        urlData.setDate(date);
        urlData.setOutboundLinks(outboundLinks);
        DocumentData documentData = new DocumentData();
        documentData.setMd5(md5);
        documentData.setDate(date);
        documentData.setDocument(document);

        return Stream.of(new Pair<>(urlData, documentData));
    }

    static void uploadData(Collection<Pair<UrlData, DocumentData>> dataList) {
        List<UrlData> urlDataList = Lists.newArrayList();
        List<DocumentData> documentDataList = Lists.newArrayList();
        Set<String> documentDataDedupSet = Sets.newHashSet();
        for (Pair<UrlData, DocumentData> pair : dataList) {
            urlDataList.add(pair.getValue0());
            DocumentData documentData = pair.getValue1();
            if (!documentDataDedupSet.contains(documentData.getMd5())) {
                documentDataList.add(documentData);
                documentDataDedupSet.add(documentData.getMd5());
            }
        }

        count.addAndGet(documentDataList.size());
        LOGGER.debug(dynamoDb.batchUpdateUrlData(urlDataList) + " failed URL data");
        LOGGER.debug(dynamoDb.batchUpdateDocumentData(documentDataList) + " failed URL data");
    }

    static void sendURLs(Collection<SendMessageBatchRequestEntry> entries) {
        sqs.sendMessageBatch(new SendMessageBatchRequest()
                .withQueueUrl(AmazonSqsSingleton.URL_QUEUE_FIFO).withEntries(entries));
    }

    static void ack(Collection<Message> messages) {
        List<DeleteMessageBatchRequestEntry> entries = Lists.newArrayList();
        for (Message message : messages) {
            entries.add(new DeleteMessageBatchRequestEntry(
                    message.getMessageId(), message.getReceiptHandle()));
        }
        sqs.deleteMessageBatch(new DeleteMessageBatchRequest()
                .withQueueUrl(AmazonSqsSingleton.URL_QUEUE_FIFO).withEntries(entries));
    }

    public void stop() {
        MoreExecutors.shutdownAndAwaitTermination(main, 60L, TimeUnit.SECONDS);
        MoreExecutors.shutdownAndAwaitTermination(ForkJoinPool.commonPool(), 60L, TimeUnit.SECONDS);
    }

    /**
     * Main program: init database, start crawler, wait for it to notify that it is done, then close.
     */
    public static void main(String[] args) {
        Configurator.setLevel("edu.upenn.cis", Level.INFO);
        Configurator.setLevel("com.google", Level.ERROR);
        Configurator.setLevel("crawlercommons.robots", Level.ERROR);
        Configurator.setLevel("org.apache", Level.ERROR);

        // default 10 minutes (600 seconds) time limit
        int timeLimit = 600;
        try {
            timeLimit = Integer.parseInt(System.getenv("TIME_LIMIT"));
        } catch (Exception ignored) {
        }

        AsyncCrawler crawler = new AsyncCrawler();

        crawler.start();
        LOGGER.info("Crawler start, " +
                String.format("%d:%02d:%02d", timeLimit / 3600, (timeLimit % 3600) / 60, (timeLimit % 60))
                + " time scheduled");

        long start = System.currentTimeMillis();
        while (true) {
            long timeDelta = timeLimit - (System.currentTimeMillis() - start) / 1000;
            if (timeDelta <= 0) break;
            System.out.print("Document count: " + count.get() + " \ttime left: " +
                    String.format("%d:%02d:%02d", timeDelta / 3600, (timeDelta % 3600) / 60, (timeDelta % 60)));
            System.out.print("\r");
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        crawler.stop();
        LOGGER.info("Graceful exit, " + count.get() + " pages crawled");

    }

}
