package edu.upenn.cis.cis455.crawler.distributed.thread.singleton;

import edu.upenn.cis.cis455.crawler.utils.RobotRulesCache;

public class RobotRulesCacheSingleton {

    static final RobotRulesCache INSTANCE = new RobotRulesCache();

    public static RobotRulesCache getSingleton() {
        return INSTANCE;
    }

}
