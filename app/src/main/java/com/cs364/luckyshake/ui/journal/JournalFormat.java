package com.cs364.luckyshake.ui.journal;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Pure Java utility for formatting timestamps, star ratings, and level values for the Journal screen.
 * Contains no Android framework dependencies to allow pure JUnit unit testing.
 */
public final class JournalFormat {

    public static final int MIN_LEVEL = 1;
    public static final int MAX_LEVEL = 4;

    private JournalFormat() {
    }

    /**
     * Formats an epoch timestamp into a localized date-time string ("d MMM HH:mm").
     * Example: "13 Nov 09:45" (English) or "13 พ.ย. 09:45" (Thai).
     *
     * @param epochMillis the timestamp in milliseconds
     * @param locale      the desired Locale, or default if null
     * @param zone        the desired TimeZone, or default if null
     * @return the formatted date-time string
     */
    public static String formatTime(long epochMillis, Locale locale, TimeZone zone) {
        Locale loc = locale != null ? locale : Locale.getDefault();
        TimeZone tz = zone != null ? zone : TimeZone.getDefault();
        SimpleDateFormat sdf = new SimpleDateFormat("d MMM HH:mm", loc);
        sdf.setTimeZone(tz);
        return sdf.format(new Date(epochMillis));
    }

    /**
     * Clamps a fortune level to the valid range of MIN_LEVEL (1) to MAX_LEVEL (4).
     *
     * @param level the input level
     * @return the clamped level value between 1 and 4
     */
    public static int clampLevel(int level) {
        return Math.max(MIN_LEVEL, Math.min(MAX_LEVEL, level));
    }

    /**
     * Constructs a star rating string representing the given fortune level.
     * Output contains clamped level filled stars followed by empty stars up to MAX_LEVEL (4).
     * Example for level 2 with "★" and "☆": "★★☆☆".
     *
     * @param level  the fortune level (1-4)
     * @param filled the string representation for a filled star
     * @param empty  the string representation for an empty star
     * @return the formatted star rating string
     */
    public static String stars(int level, String filled, String empty) {
        int clamped = clampLevel(level);
        String f = filled != null ? filled : "★";
        String e = empty != null ? empty : "☆";

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < clamped; i++) {
            sb.append(f);
        }
        for (int i = clamped; i < MAX_LEVEL; i++) {
            sb.append(e);
        }
        return sb.toString();
    }
}
