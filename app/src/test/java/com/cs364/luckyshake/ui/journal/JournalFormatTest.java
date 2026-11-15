package com.cs364.luckyshake.ui.journal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Locale;
import java.util.TimeZone;

/**
 * Pure Java unit test for {@link JournalFormat}.
 */
public class JournalFormatTest {

    @Test
    public void testFormatTime() {
        // 2026-11-13 09:45:00 UTC
        long epochMillis = 1794563100000L;
        TimeZone utcZone = TimeZone.getTimeZone("UTC");

        String formattedEn = JournalFormat.formatTime(epochMillis, Locale.ENGLISH, utcZone);
        assertEquals("13 Nov 09:45", formattedEn);

        String formattedTh = JournalFormat.formatTime(epochMillis, Locale.forLanguageTag("th"), utcZone);
        assertTrue(formattedTh.contains("พ.ย."));
        assertTrue(formattedTh.contains("09:45"));
    }

    @Test
    public void testStars() {
        assertEquals("★☆☆☆", JournalFormat.stars(1, "★", "☆"));
        assertEquals("★★☆☆", JournalFormat.stars(2, "★", "☆"));
        assertEquals("★★★★", JournalFormat.stars(4, "★", "☆"));

        // Clamping checks
        assertEquals("★☆☆☆", JournalFormat.stars(0, "★", "☆"));
        assertEquals("★★★★", JournalFormat.stars(9, "★", "☆"));
    }

    @Test
    public void testClampLevel() {
        assertEquals(1, JournalFormat.clampLevel(0));
        assertEquals(1, JournalFormat.clampLevel(1));
        assertEquals(2, JournalFormat.clampLevel(2));
        assertEquals(3, JournalFormat.clampLevel(3));
        assertEquals(4, JournalFormat.clampLevel(4));
        assertEquals(4, JournalFormat.clampLevel(5));
    }
}
