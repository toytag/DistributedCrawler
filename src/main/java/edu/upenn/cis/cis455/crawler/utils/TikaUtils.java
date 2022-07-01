package edu.upenn.cis.cis455.crawler.utils;

import org.apache.tika.langdetect.optimaize.OptimaizeLangDetector;
import org.apache.tika.language.detect.LanguageDetector;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;

import java.io.InputStream;

public class TikaUtils {

    static final AutoDetectParser parser = new AutoDetectParser();
    static final LanguageDetector detector = new OptimaizeLangDetector().loadModels();

    public static void init() {
    }

    public static boolean isHTML(String mimeType) {
        if (mimeType == null) return false;
        String normalizedMimeType = mimeType.toLowerCase();
        return normalizedMimeType.contains("html");
    }

    public static boolean isXML(String mimeType) {
        if (mimeType == null) return false;
        String normalizedMimeType = mimeType.toLowerCase();
        return normalizedMimeType.contains("xml") || normalizedMimeType.contains("rss");
    }

    public static boolean isTEXT(String mimeType) {
        if (mimeType == null) return false;
        String normalizedMimeType = mimeType.toLowerCase();
        return normalizedMimeType.contains("text");
    }

    public static boolean isHTMLorXML(String mimeType) {
        return isHTML(mimeType) || isXML(mimeType);
    }

    public static boolean isHTMLorXMLorTEXT(String mimeType) {
        return isHTML(mimeType) || isXML(mimeType) || isTEXT(mimeType);
    }

    public static String parseContent(InputStream in) {
        BodyContentHandler handler = new BodyContentHandler(-1);
        Metadata metadata = new Metadata();

        try {
            parser.parse(in, handler, metadata);
        } catch (Exception ignored) {
        }

        return handler.toString();
    }

    public static String detectLanguage(String content) {
        return detector.detect(content).getLanguage();
    }

}
