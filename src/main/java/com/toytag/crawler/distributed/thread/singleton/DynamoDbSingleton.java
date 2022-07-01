package com.toytag.crawler.distributed.thread.singleton;

import com.toytag.storage.dynamodb.DynamoDBInterface;

public class DynamoDbSingleton {

    public static final int MAX_BATCH_SIZE = 25;
    static final DynamoDBInterface INSTANCE = new DynamoDBInterface();

    public static DynamoDBInterface getSingleton() {
        return INSTANCE;
    }

}
