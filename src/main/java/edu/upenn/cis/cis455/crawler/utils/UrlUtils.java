package edu.upenn.cis.cis455.crawler.utils;

import crawlercommons.filters.basic.BasicURLNormalizer;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UrlUtils {

    static final BasicURLNormalizer basicURLNormalizer = new BasicURLNormalizer();
    static final Pattern hostnamePattern = Pattern.compile("(?<=://)[^/]+(?=/)");

    @NotNull
    public static String normalize(String url) {
        String normalizedURL = basicURLNormalizer.filter(url);
        if (normalizedURL == null)
            return "";
        return normalizedURL;
    }

    @NotNull
    public static String robots(String url) {
        return normalize(url).replaceFirst("(?<=[^:/])/.*$", "/robots.txt");
    }

    @NotNull
    public static String hostname(String url) {
        Matcher m = hostnamePattern.matcher(normalize(url));
        if (m.find()) {
            return m.group();
        }
        return "url";
    }

}
