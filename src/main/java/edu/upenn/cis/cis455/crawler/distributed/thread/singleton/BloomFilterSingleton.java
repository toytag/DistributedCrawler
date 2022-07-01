package edu.upenn.cis.cis455.crawler.distributed.thread.singleton;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;

public class BloomFilterSingleton {

    static final BloomFilter<String> INBOUND_FILTER =
            BloomFilter.create(Funnels.unencodedCharsFunnel(), 1_000_000, 0.01);

    static final BloomFilter<String> OUTBOUND_FILTER =
            BloomFilter.create(Funnels.unencodedCharsFunnel(), 100_000_000, 0.01);

    public static BloomFilter<String> getInboundFilter() {
        return INBOUND_FILTER;
    }

    public static BloomFilter<String> getOutboundFilter() {
        return OUTBOUND_FILTER;
    }

}
