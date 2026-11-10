package com.cs364.luckyshake.data;

/**
 * One row of the journal. The dish names are copied into the row so the entry
 * stays readable even after the dish is deleted. Table: history.
 */
public class HistoryEntry {
    public static final String STATUS_ACCEPTED = "accepted";
    public static final String STATUS_REDRAWN = "redrawn";

    public static final String MODE_SOLO = "solo";
    public static final String MODE_GROUP = "group";

    public long id;
    public Long menuId;        // null when the dish was deleted or came from group mode
    public String nameTh;
    public String nameEn;
    public int level;          // fortune level 1..4 (4 = best)
    public int stickNo;        // stick number shown to the user
    public String status;      // STATUS_*
    public String mode;        // MODE_*
    public long createdAt;     // epoch millis

    public String getName(boolean thai) {
        return (thai || nameEn == null || nameEn.isEmpty()) ? nameTh : nameEn;
    }
}
