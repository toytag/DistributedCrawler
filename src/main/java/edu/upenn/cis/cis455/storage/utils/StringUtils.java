package edu.upenn.cis.cis455.storage.utils;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class StringUtils {

    public static String hash(String input, String algorithm) {
        try {
            // hashing
            byte[] hash = MessageDigest.getInstance(algorithm).digest(input.getBytes("UTF-8"));
            // hex encode the hash bytes
            StringBuilder hexString = new StringBuilder(2 * hash.length);
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException nsae) {
            nsae.printStackTrace();
        } catch (UnsupportedEncodingException uee) {
            uee.printStackTrace();
        }
        return null;
    }

}
