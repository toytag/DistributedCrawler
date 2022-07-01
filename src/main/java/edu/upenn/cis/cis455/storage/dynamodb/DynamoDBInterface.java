package edu.upenn.cis.cis455.storage.dynamodb;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;
import com.amazonaws.services.dynamodbv2.document.*;
import com.amazonaws.services.dynamodbv2.document.spec.BatchGetItemSpec;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.dynamodbv2.model.ExecuteStatementRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Work with database
 */
public class DynamoDBInterface implements Serializable {

    AmazonDynamoDB client;
    DynamoDBMapper mapper;
    DynamoDB db;

    static final String URL_TABLE = "URL-DEMO";
    static final String DOCUMENT_TABLE = "DOCUMENT-DEMO";
    static final String INVERTED_INDEX_TABLE = "INVIDX";

    static final ObjectMapper om = new ObjectMapper();

    public DynamoDBInterface() {
        client = DynamoDBFactory.createClient();

        mapper = new DynamoDBMapper(client);
        db = new DynamoDB(client);
    }

    // C --------------------------------------------------
    private <T> void addData(T item) {
        DynamoDBMapperConfig dynamoDBMapperConfig = new DynamoDBMapperConfig.Builder()
                .withConsistentReads(DynamoDBMapperConfig.ConsistentReads.CONSISTENT)
                .withSaveBehavior(DynamoDBMapperConfig.SaveBehavior.PUT)
                .build();
        mapper.save(item, dynamoDBMapperConfig);
    }

    private <T> void batchAddData(Iterable<T> items) {
        mapper.batchSave(items);
    }

    public void addUrlData(UrlData item) {
        addData(item);
    }

    public void batchAddUrlData(Iterable<UrlData> items) {
        batchAddData(items);
    }

    public void addDocumentData(DocumentData item) {
        addData(item);
    }

    public void batchAddDocumentData(Iterable<DocumentData> items) {
        batchAddData(items);
    }

    public void addInvertedIndexData(InvertedIndexData item) {
        addData(item);
    }

    public void batchAddInvertedIndexData(Iterable<InvertedIndexData> items) {
        batchAddData(items);
    }

    // R --------------------------------------------------
    public UrlData getUrlData(String url) {
        return mapper.load(UrlData.class, url);
    }

    public List<UrlData> batchGetUrlData(Iterable<String> urls) {

        TableKeysAndAttributes hashKeys = new TableKeysAndAttributes(URL_TABLE);
        for (String url : urls) {
            hashKeys = hashKeys.addHashOnlyPrimaryKey("url", url);
        }
        BatchGetItemSpec spec = new BatchGetItemSpec().withTableKeyAndAttributes(hashKeys);

        List<UrlData> results = new ArrayList<>();
        db.batchGetItem(spec).getTableItems().get(URL_TABLE).forEach(item -> {
            try {
                results.add(om.readValue(item.toJSON(), UrlData.class));
            } catch (JsonProcessingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        });

        return results;
    }

    public DocumentData getDocumentData(String md5) {
        return mapper.load(DocumentData.class, md5);
    }

    public List<DocumentData> batchGetDocumentData(Iterable<String> md5s) {

        TableKeysAndAttributes hashKeys = new TableKeysAndAttributes(DOCUMENT_TABLE);
        for (String md5 : md5s) {
            hashKeys = hashKeys.addHashOnlyPrimaryKey("md5", md5);
        }
        BatchGetItemSpec spec = new BatchGetItemSpec().withTableKeyAndAttributes(hashKeys);

        List<DocumentData> results = new ArrayList<>();
        db.batchGetItem(spec).getTableItems().get(DOCUMENT_TABLE).forEach(item -> {
            try {
                results.add(om.readValue(item.toJSON(), DocumentData.class));
            } catch (JsonProcessingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        });
        return results;

    }

    public InvertedIndexData getInvertedIndexData(String word, String md5) {
        return mapper.load(InvertedIndexData.class, word, md5);
    }

    public List<InvertedIndexData> batchGetInvertedIndexData(Iterable<String> words, Iterable<String> md5s) {
        Iterator<String> wordsIter = words.iterator();
        Iterator<String> md5sIter = md5s.iterator();

        TableKeysAndAttributes hashKeys = new TableKeysAndAttributes(INVERTED_INDEX_TABLE);
        for (String word = wordsIter.next(), md5 = md5sIter.next(); wordsIter.hasNext()
                && md5sIter.hasNext(); word = wordsIter.next(), md5 = md5sIter.next()) {
            hashKeys = hashKeys.addHashAndRangePrimaryKey("word", word, "md5", md5);
        }

        List<InvertedIndexData> results = new ArrayList<>();
        db.batchGetItem(hashKeys).getTableItems().get(INVERTED_INDEX_TABLE).forEach(item -> {
            try {
                results.add(om.readValue(item.toJSON(), InvertedIndexData.class));
            } catch (JsonProcessingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        });
        return results;

    }

    public List<InvertedIndexData> getInvertedIndexData(String word) {
        QuerySpec querySpec = new QuerySpec()
                .withKeyConditionExpression("word = :word")
                .withValueMap(new ValueMap().withString(":word", word));

        List<InvertedIndexData> results = new ArrayList<>();
        db.getTable(INVERTED_INDEX_TABLE).query(querySpec).forEach(item -> {
            results.add(InvertedIndexData.fromJSON(item.toJSON()));
        });

        return results;
    }

    public Page<Item, QueryOutcome> getInvertedIndexDataPage(String word) {
        QuerySpec querySpec = new QuerySpec()
                .withKeyConditionExpression("word = :word")
                .withValueMap(new ValueMap().withString(":word", word))
                .withScanIndexForward(false) // DESC
                .withProjectionExpression("md5, tfidf")
                .withMaxPageSize(10);

        return db.getTable(INVERTED_INDEX_TABLE).getIndex("tfidf-index").query(querySpec).firstPage();
    }

    @Deprecated
    public List<InvertedIndexData> batchGetInvertedIndexData(Iterable<String> words) {
        // PartiQL
        ExecuteStatementRequest request = new ExecuteStatementRequest()
                .withStatement(String.format("SELECT * FROM \"%s\".\"tfidf-index\" WHERE word IN ['%s'] ORDER BY word, tfidf DESC",
                        INVERTED_INDEX_TABLE, String.join("','", ImmutableList.copyOf(words))));

        return mapper.marshallIntoObjects(InvertedIndexData.class, client.executeStatement(request).getItems());
    }

    // U --------------------------------------------------
    private <T> void updateData(T item) {
        DynamoDBMapperConfig dynamoDBMapperConfig = new DynamoDBMapperConfig.Builder()
                .withConsistentReads(DynamoDBMapperConfig.ConsistentReads.CONSISTENT)
                .withSaveBehavior(DynamoDBMapperConfig.SaveBehavior.UPDATE)
                .build();
        mapper.save(item, dynamoDBMapperConfig);
    }

    private <T> int batchUpdateData(Iterable<T> items) {
        List<DynamoDBMapper.FailedBatch> fails = mapper.batchSave(items);
        if (!fails.isEmpty()) {
            fails.get(0).getException().printStackTrace();
            fails.get(0).getUnprocessedItems();
        }
        return fails.size();
    }

    public void updateUrlData(UrlData item) {
        updateData(item);
    }

    public int batchUpdateUrlData(Iterable<UrlData> items) {
        return batchUpdateData(items);
    }

    public void updateDocumentData(DocumentData item) {
        updateData(item);
    }

    public int batchUpdateDocumentData(Iterable<DocumentData> items) {
        return batchUpdateData(items);
    }

    public void updateInvertedIndexData(InvertedIndexData item) {
        updateData(item);
    }

    public void batchUpdateInvertedIndexData(Iterable<InvertedIndexData> items) {
        batchUpdateData(items);
    }

    // D --------------------------------------------------
    public void deleteUrlData(String url) {
        UrlData item = new UrlData();
        item.setUrl(url);
        mapper.delete(item);
    }

    public void deleteDocumentData(String md5) {
        DocumentData item = new DocumentData();
        item.setMd5(md5);
        mapper.delete(item);
    }

    public void deleteInvertedIndexData(String word, String md5) {
        InvertedIndexData item = new InvertedIndexData();
        item.setWord(word);
        item.setMd5(md5);
        mapper.delete(item);
    }
    // ----------------------------------------------------

}