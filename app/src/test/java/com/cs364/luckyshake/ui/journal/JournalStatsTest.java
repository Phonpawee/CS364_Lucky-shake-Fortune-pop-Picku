package com.cs364.luckyshake.ui.journal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.cs364.luckyshake.data.HistoryRepository;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Pure Java unit test for {@link JournalStats}.
 */
public class JournalStatsTest {

    @Test
    public void testEmptyData() {
        JournalStats stats = new JournalStats(Collections.emptyList(), 0, 0, new int[7]);

        assertTrue(stats.isEmpty());
        assertEquals(0, stats.acceptPercent());
        assertEquals(1, stats.maxPerDay());
        assertEquals(1, stats.maxTopCount());
    }

    @Test
    public void testAcceptPercent100() {
        JournalStats stats = new JournalStats(Collections.emptyList(), 4, 0, new int[7]);

        assertFalse(stats.isEmpty());
        assertEquals(100, stats.acceptPercent());
    }

    @Test
    public void testAcceptPercent33() {
        JournalStats stats = new JournalStats(Collections.emptyList(), 1, 2, new int[7]);

        assertFalse(stats.isEmpty());
        assertEquals(33, stats.acceptPercent());
    }

    @Test
    public void testMaxPerDay() {
        int[] perDay = {0, 3, 5, 1, 0, 8, 2};
        JournalStats stats = new JournalStats(Collections.emptyList(), 10, 5, perDay);

        assertEquals(8, stats.maxPerDay());
    }

    @Test
    public void testMaxTopCount() {
        List<HistoryRepository.NameCount> list = Arrays.asList(
                new HistoryRepository.NameCount("ชัย", "Chai", 2),
                new HistoryRepository.NameCount("ฟาสต์ฟู้ด", "Fast Food", 5),
                new HistoryRepository.NameCount("ชาบู", "Shabu", 1)
        );
        JournalStats stats = new JournalStats(list, 8, 0, new int[7]);

        assertEquals(5, stats.maxTopCount());
    }
}
