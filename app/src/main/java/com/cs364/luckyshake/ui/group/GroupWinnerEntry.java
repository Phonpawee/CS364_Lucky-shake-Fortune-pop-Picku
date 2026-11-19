package com.cs364.luckyshake.ui.group;

import com.cs364.luckyshake.data.HistoryEntry;
import com.cs364.luckyshake.logic.GameConfig;

/**
 * Pure Java factory for creating a group mode history entry (#39).
 * Contains no Android framework dependencies to allow pure JUnit unit testing.
 */
public final class GroupWinnerEntry {

    private GroupWinnerEntry() {
    }

    /**
     * Creates a {@link HistoryEntry} for a group winner.
     *
     * @param nameTh    Thai menu or food type name
     * @param nameEn    English menu or food type name
     * @param drawLevel level returned by DrawEngine.pickLevel() (0..3)
     * @param stickNo   stick number (1..28)
     * @param now       timestamp in epoch millis, or 0 for system time
     * @return populated HistoryEntry for HistoryRepository.add()
     */
    public static HistoryEntry create(String nameTh, String nameEn, int drawLevel, int stickNo, long now) {
        HistoryEntry entry = new HistoryEntry();
        entry.menuId = null;
        entry.nameTh = nameTh;
        entry.nameEn = nameEn;
        entry.level = GameConfig.LEVEL_COUNT - drawLevel;
        entry.stickNo = stickNo;
        entry.status = HistoryEntry.STATUS_ACCEPTED;
        entry.mode = HistoryEntry.MODE_GROUP;
        entry.createdAt = now > 0 ? now : System.currentTimeMillis();
        return entry;
    }
}
