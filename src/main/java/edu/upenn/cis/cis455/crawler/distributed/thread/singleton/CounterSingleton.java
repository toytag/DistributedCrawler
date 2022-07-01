package edu.upenn.cis.cis455.crawler.distributed.thread.singleton;

import java.util.concurrent.atomic.AtomicInteger;

public class CounterSingleton {

    static final AtomicInteger INSTANCE = new AtomicInteger(0);

    public static AtomicInteger getSingleton() {
        return INSTANCE;
    }

}
