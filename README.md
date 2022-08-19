# Distributed Crawler

## Build & Usage

### Maven

```bash
mvn clean install
```

### Docker

```bash
sudo docker build . -t distributed-crawler
```

## Introduction

We built a distributed crawler based on the thread pool and event loop concurrent programming, and deployed to distributed servers using docker swarm. Threading and event loop asynchronous upload provides us with optimal speed for crawler on each single machine, and docker swarm provides us with easy and seamless scaling and deployment to multiple machines. We crawled 1.2M documents in total and they are available at the `DOCUMENT` table.

### Multi-threading and Asynchronous Operations on a Single Machine

![SingleThreadPipeline](https://github.com/toytag/DistributedCrawler/blob/static-files/SingleThreadPipline.png)

The diagram above demonstrates our design for a single thread. Inspired by Spark Streaming, we emulate streaming processing by using mini-batches of data, and do map-reduce-like operations on them. The batch size limit of our distributed message queue Amazon SQS is 10, and the batch size limit of our database DynamoDB is 25. So we choose a Least Common Multiple 50 as our mini-batch size to fully utilize the batch download and upload capability provided by the queuing and database service. The critical path for a single iteration is to get urls, get documents from the internet, parse and extract links. Uploading the data is not in the critical path, so we simply run it asynchronously on the backend to prevent the upload network IO from blocking and threading and losing performance. We could further optimize the get and fetch documents from the internet part of our pipeline using a pre-fetch buffer, or we could simply launch multiple threads running this pipeline to compensate for the IO delay and improve the CPU utilization.

### Distributed Crawler Deployment

All the crawler instances on all the machines share the same url queue and database, so scaling the container size, then we deployed our containerized crawler like a service. It will spread evenly on all worker nodes, provides centralized control and status report over all instances, and it will automatically restart and recover the state of the container if any container error occurs or worker node dies. In this way, we achieved fast and robust distributed crawling.

### Distributed Message Queue and Deduplication

![MsgDedupService](https://github.com/toytag/DistributedCrawler/blob/static-files/MsgDedupService.png)

For the distributed message queue, we simply adopted the Amazon Simple Queue Service. It is a reliable message queue for queuing the urls during crawling. Currently we are doing deduplication within each crawler is very natural. Running multiple crawler instances on all the machines, then it is distributed. But such a simple way of distributing inevitably brings us many downsides. We don’t have a centralized control over all the crawler instances; we have to manually distribute crawlers; and most importantly, we have zero to none fault tolerance. Our solution is to use docker swarm orchestration. Much like the kubernetes orchestration, docker swarm provides us with a simple set of functionality and a native docker interface which allows us to quickly adapt to it. We containerized our crawler using docker multi-stage build to save crawler instance, and rely on the Amazon SQS FIFO deduplication. However, it has certain limitations. The number of duplications will be as large as the number of crawler instances we open. Once we really scale up our crawler to hundreds or thousands, we would have thousands of duplicate urls inside the url queue, which could easily lead to the explosion of number of items given the size of the internet and ultimately causing too much strain on the distributed queuing system and leading to failure. We designed the message deduplication service as a standalone service to help lighten the load of the queuing system. It receives the urls output from the crawler, and does a modulus hash of the url to determine which deduplication instance each url will be sent to. For each dedup instance we are using bloom filters as a functioning unit. We can now scale the deduplication service as our crawler scales. In this way we have an extremely high probability of achieving exactly one computation.

### Miscellaneous

Apart from the design of our crawler, we also embedded several utility features in our document fetching and parsing. For the robots.txt rules of each domain, we use a LRU cache to store them, avoiding having to send too many get requests to the robots rules url. For non-HTML documents, we convert them into byte input streams and parse them using Apache Tika. Tika provides an amazing tool for parsing PDF, pictures and all other file types. It also can determine the language, allowing us to only store English content.

## Efficiency
For the crawler, as it was given more nodes, the rate at which it crawled and downloaded increased greatly. Specifically, we were able to crawl 20000 documents in 10 minutes with 6 nodes. Below is a sketch performance testing results.

<img src="https://github.com/toytag/DistributedCrawler/blob/static-files/PerformanceTesting.png" alt="PerformanceTesting" width="640"/>
