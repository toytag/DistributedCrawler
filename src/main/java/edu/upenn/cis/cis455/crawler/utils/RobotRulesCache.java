package edu.upenn.cis.cis455.crawler.utils;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import crawlercommons.robots.SimpleRobotRules;
import crawlercommons.robots.SimpleRobotRulesParser;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Connection.Method;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;

import java.util.concurrent.ExecutionException;

public class RobotRulesCache {

    SimpleRobotRulesParser robotstxtParser;
    LoadingCache<String, SimpleRobotRules> cache;

    public RobotRulesCache() {
        robotstxtParser = new SimpleRobotRulesParser();
        cache = CacheBuilder.newBuilder()
                .maximumSize(1024)
                .build(new CacheLoader<String, SimpleRobotRules>() {
                    @NotNull
                    @Override
                    public SimpleRobotRules load(@NotNull String url) {
                        try {
                            Response res = Jsoup.connect(url).method(Method.GET).execute();
                            return robotstxtParser.parseContent(url, res.bodyAsBytes(), res.contentType(), "cis455crawler");
                        } catch (Exception e) {
                            return new SimpleRobotRules(SimpleRobotRules.RobotRulesMode.ALLOW_ALL);
                        }
                    }
                });
    }

    public SimpleRobotRules get(String url) {
        return cache.getUnchecked(url);
    }
    
}
