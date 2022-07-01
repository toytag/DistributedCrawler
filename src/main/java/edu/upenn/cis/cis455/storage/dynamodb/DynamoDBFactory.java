package edu.upenn.cis.cis455.storage.dynamodb;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.regions.Regions;
import com.amazonaws.retry.RetryMode;
import com.amazonaws.retry.RetryPolicy;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;

import static com.amazonaws.retry.PredefinedRetryPolicies.*;


/**
 * Initiate database connection
 */
public class DynamoDBFactory {
    private final static AmazonDynamoDBClientBuilder builder =
            AmazonDynamoDBClientBuilder.standard().withRegion(Regions.US_EAST_1)
                    .withClientConfiguration(new ClientConfiguration()
                            .withMaxErrorRetry(DEFAULT_MAX_ERROR_RETRY)
                            .withRetryMode(RetryMode.STANDARD).withThrottledRetries(true)
                            .withRetryPolicy(new RetryPolicy(DEFAULT_RETRY_CONDITION, DEFAULT_BACKOFF_STRATEGY,
                                    DEFAULT_MAX_ERROR_RETRY, false)));

    public static AmazonDynamoDB createClient() {
        return builder.build();
    }
}
