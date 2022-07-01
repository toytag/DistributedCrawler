package edu.upenn.cis.cis455.crawler.utils;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;

import java.util.Collection;
import java.util.concurrent.LinkedBlockingQueue;


public class NonRepeatingURLQueue extends LinkedBlockingQueue<String> {

    BloomFilter<String> bloomFilter;
    long lastEntryTime;

    public NonRepeatingURLQueue() {
        super();
        bloomFilter = BloomFilter.create(Funnels.unencodedCharsFunnel(), 100_000, 0.01);
        lastEntryTime = System.currentTimeMillis();
    }

    @Override
    public synchronized boolean add(String item) {
        if (!bloomFilter.mightContain(item)) {
            bloomFilter.put(item);
            lastEntryTime = System.currentTimeMillis();
            return super.add(item);
        }
        return false;
    }

    @Override
    public synchronized boolean addAll(Collection<? extends String> items) {
        boolean result = false;
        for (String item : items) {
            boolean addResult = add(item);
            result = result || addResult;
        }
        return result;
    }

    public synchronized boolean timeout(long millis) {
        return (System.currentTimeMillis() - lastEntryTime) > millis;
    }

}
