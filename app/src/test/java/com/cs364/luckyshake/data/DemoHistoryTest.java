package com.cs364.luckyshake.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class DemoHistoryTest {

    private static List<Menu> menus(int n) {
        List<Menu> out = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            Menu m = new Menu();
            m.id = 100 + i;
            m.nameTh = "เมนู" + i;
            m.nameEn = "Dish " + i;
            out.add(m);
        }
        return out;
    }

    /** เที่ยงคืนของวันที่ {@code daysAgo} วันก่อน {@code now} */
    private static long startOfDay(long now, int daysAgo) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(now);
        cal.add(Calendar.DAY_OF_YEAR, -daysAgo);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    private static long at(int hour) {
        Calendar cal = Calendar.getInstance();
        cal.set(2026, Calendar.NOVEMBER, 16, hour, 30, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    @Test
    public void generatesRequestedCount_withinLastSevenDays_notInFuture() {
        for (int hour : new int[]{1, 12, 23}) {
            long now = at(hour);
            for (int seed = 0; seed < 50; seed++) {
                List<HistoryEntry> list = DemoHistory.generate(menus(8), 24, 7, now,
                        new Random(seed));
                assertEquals(24, list.size());
                long earliest = startOfDay(now, 6);
                for (HistoryEntry e : list) {
                    assertTrue("in the future", e.createdAt <= now);
                    assertTrue("older than 7 days", e.createdAt >= earliest);
                }
            }
        }
    }

    @Test
    public void spreadsOverSeveralDays() {
        long now = at(21);
        List<HistoryEntry> list = DemoHistory.generate(menus(8), 24, 7, now, new Random(1));
        Set<Long> days = new HashSet<>();
        for (HistoryEntry e : list) {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(e.createdAt);
            days.add((long) cal.get(Calendar.DAY_OF_YEAR));
        }
        assertEquals(7, days.size());
    }

    @Test
    public void entriesAreValidForJournal_andSortedOldestFirst() {
        List<Menu> menus = menus(5);
        List<HistoryEntry> list = DemoHistory.generate(menus, 24, 7, at(20), new Random(7));
        boolean sawAccepted = false;
        boolean sawRedrawn = false;
        long prev = Long.MIN_VALUE;
        for (HistoryEntry e : list) {
            assertTrue(e.createdAt >= prev);
            prev = e.createdAt;
            assertEquals(HistoryEntry.MODE_SOLO, e.mode);
            assertTrue(e.level >= 1 && e.level <= 4);
            assertTrue(e.stickNo >= 1 && e.stickNo <= menus.size());
            Menu m = menus.get(e.stickNo - 1);
            assertEquals(Long.valueOf(m.id), e.menuId);
            assertEquals(m.nameTh, e.nameTh);
            assertEquals(m.nameEn, e.nameEn);
            sawAccepted |= HistoryEntry.STATUS_ACCEPTED.equals(e.status);
            sawRedrawn |= HistoryEntry.STATUS_REDRAWN.equals(e.status);
        }
        assertTrue(sawAccepted);
        assertTrue(sawRedrawn);
    }

    @Test
    public void redrawnEntry_isFollowedByAcceptOfDifferentDish() {
        List<HistoryEntry> list = DemoHistory.generate(menus(5), 24, 7, at(20), new Random(3));
        for (int i = 0; i < list.size(); i++) {
            HistoryEntry e = list.get(i);
            if (HistoryEntry.STATUS_REDRAWN.equals(e.status)) {
                HistoryEntry next = list.get(i + 1);
                assertEquals(HistoryEntry.STATUS_ACCEPTED, next.status);
                assertNotEquals(e.menuId, next.menuId);
            }
        }
    }

    @Test
    public void singleDish_stillWorks() {
        List<HistoryEntry> list = DemoHistory.generate(menus(1), 24, 7, at(20), new Random(0));
        assertEquals(24, list.size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void noMenus_isRejected() {
        DemoHistory.generate(new ArrayList<>(), 24, 7, at(20), new Random(0));
    }
}
