package com.toytag.crawler.distributed.thread.singleton;

import com.toytag.crawler.utils.RobotRulesCache;

public class RobotRulesCacheSingleton {

    static final RobotRulesCache INSTANCE = new RobotRulesCache();

    public static RobotRulesCache getSingleton() {
        return INSTANCE;
    }

}
