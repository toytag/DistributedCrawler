package edu.upenn.cis.cis455.crawler.utils;

import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class DateUtils {

    /**
     * Returns a date.
     */
    @NotNull
    public static Date getDate() {
        return Calendar.getInstance().getTime();
    }

    /**
     * Returns a date string in RFC 1123 format.
     */
    @NotNull
    public static String getDateString(Date date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        return dateFormat.format(date);
    }

    /**
     * Returns a date string in RFC 1123 format.
     */
    @NotNull
    public static String getDateString(long date) {
        return getDateString(new Date(date));
    }

    /**
     * Returns a date string in RFC 1123 format.
     */
    @NotNull
    public static String getDateString() {
        return getDateString(getDate());
    }

    /**
     * Parse a date string in RFC 1123 format.
     */
    public static Date parseDateString(String dateString) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        try {
            return dateFormat.parse(dateString);
        } catch (Exception e) {
            return getDate();
        }
    }

}
