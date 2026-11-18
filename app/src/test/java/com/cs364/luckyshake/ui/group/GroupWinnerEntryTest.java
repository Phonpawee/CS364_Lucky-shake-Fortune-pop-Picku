package com.cs364.luckyshake.ui.group;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.cs364.luckyshake.data.HistoryEntry;

import org.junit.Test;

/**
 * Pure Java unit tests for {@link GroupWinnerEntry}.
 */
public class GroupWinnerEntryTest {

    @Test
    public void testCreateGroupWinnerEntryFields() {
        long now = 1700000000000L;
        HistoryEntry entry = GroupWinnerEntry.create("ปิ้งย่าง / หมูกระทะ", "Grill & Thai barbecue", 0, 7, now);

        assertNull(entry.menuId);
        assertEquals("ปิ้งย่าง / หมูกระทะ", entry.nameTh);
        assertEquals("Grill & Thai barbecue", entry.nameEn);
        assertEquals(4, entry.level); // 4 - 0 = 4
        assertEquals(7, entry.stickNo);
        assertEquals(HistoryEntry.STATUS_ACCEPTED, entry.status);
        assertEquals(HistoryEntry.MODE_GROUP, entry.mode);
        assertEquals(now, entry.createdAt);
    }

    @Test
    public void testCreateGroupWinnerEntryDrawLevelMapping() {
        HistoryEntry entryLevel1 = GroupWinnerEntry.create("ร้านลุงชัย", "ร้านลุงชัย", 3, 12, 0);
        assertEquals(1, entryLevel1.level); // 4 - 3 = 1
        assertEquals("ร้านลุงชัย", entryLevel1.nameTh);
        assertEquals("ร้านลุงชัย", entryLevel1.nameEn);

        HistoryEntry entryLevel2 = GroupWinnerEntry.create("KFC", "KFC", 2, 5, 0);
        assertEquals(2, entryLevel2.level); // 4 - 2 = 2
    }
}
