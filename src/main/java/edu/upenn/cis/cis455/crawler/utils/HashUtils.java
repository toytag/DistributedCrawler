package edu.upenn.cis.cis455.crawler.utils;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashUtils {

    public static String hash(String input, String algorithm) {
        try {
            // hashing
            byte[] hash = MessageDigest.getInstance(algorithm).digest(input.getBytes(StandardCharsets.UTF_8));
            // hex encode the hash bytes
            return new BigInteger(1, hash).toString(16);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static int hashToRange(String input, int buckets) {
        try {
            // hashing
            byte[] hash = MessageDigest.getInstance("MD5").digest(input.getBytes(StandardCharsets.UTF_8));
            // modulo buckets
            return new BigInteger(1, hash).mod(BigInteger.valueOf(buckets)).intValue();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return (int) (Math.random() * buckets);
    }

}
