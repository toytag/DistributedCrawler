package edu.upenn.cis.cis455.crawler.distributed.thread.singleton;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.regions.Regions;
import com.amazonaws.retry.RetryMode;
import com.amazonaws.retry.RetryPolicy;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;

import static com.amazonaws.retry.PredefinedRetryPolicies.*;

public class AmazonSqsSingleton {

    public static final int MAX_BATCH_SIZE = 10;
    public static final String URL_QUEUE = "https://sqs.us-east-1.amazonaws.com/454852304144/UrlQueue";
    public static final String URL_QUEUE_FIFO = "https://sqs.us-east-1.amazonaws.com/454852304144/UrlQueue.fifo";

    static final AmazonSQS INSTANCE = AmazonSQSClientBuilder
            .standard().withRegion(Regions.US_EAST_1)
            .withClientConfiguration(new ClientConfiguration()
                    .withMaxErrorRetry(DEFAULT_MAX_ERROR_RETRY)
                    .withRetryMode(RetryMode.STANDARD).withThrottledRetries(true)
                    .withRetryPolicy(new RetryPolicy(DEFAULT_RETRY_CONDITION, DEFAULT_BACKOFF_STRATEGY,
                            DEFAULT_MAX_ERROR_RETRY, false))).build();

    public static AmazonSQS getSingleton() {
        return INSTANCE;
    }

}
