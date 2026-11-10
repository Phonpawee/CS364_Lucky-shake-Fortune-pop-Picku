package com.cs364.luckyshake.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/** Reads and writes the journal (history) and the numbers used by the stats screen. */
public class HistoryRepository {

    /** A dish name with how many times it appears in the history. */
    public static class NameCount {
        public final String nameTh;
        public final String nameEn;
        public final int count;

        public NameCount(String nameTh, String nameEn, int count) {
            this.nameTh = nameTh;
            this.nameEn = nameEn;
            this.count = count;
        }

        public String getName(boolean thai) {
            return (thai || nameEn == null || nameEn.isEmpty()) ? nameTh : nameEn;
        }
    }

    private final DbHelper helper;

    public HistoryRepository(Context context) {
        this.helper = DbHelper.getInstance(context);
    }

    /**
     * Adds an entry and returns its id (also stored in {@code e.id}). If createdAt is 0 it
     * becomes "now". Returns -1 on failure.
     */
    public long add(HistoryEntry e) {
        if (e.nameTh == null || e.nameTh.trim().isEmpty() || e.status == null || e.mode == null) {
            return -1;
        }
        ContentValues v = new ContentValues();
        if (e.menuId != null) {
            v.put(DbHelper.C_HIST_MENU_ID, e.menuId);
        } else {
            v.putNull(DbHelper.C_HIST_MENU_ID);
        }
        v.put(DbHelper.C_HIST_NAME_TH, e.nameTh);
        v.put(DbHelper.C_HIST_NAME_EN, e.nameEn == null ? "" : e.nameEn);
        v.put(DbHelper.C_HIST_LEVEL, e.level);
        v.put(DbHelper.C_HIST_STICK_NO, e.stickNo);
        v.put(DbHelper.C_HIST_STATUS, e.status);
        v.put(DbHelper.C_HIST_MODE, e.mode);
        v.put(DbHelper.C_HIST_CREATED_AT, e.createdAt > 0 ? e.createdAt : System.currentTimeMillis());
        long id = helper.getWritableDatabase().insert(DbHelper.T_HISTORY, null, v);
        if (id > 0) {
            e.id = id;
        }
        return id;
    }

    /**
     * Entries, newest first.
     *
     * @param status HistoryEntry.STATUS_* to filter by, or null for all
     * @param limit  maximum rows, or 0 for no limit
     */
    public List<HistoryEntry> list(String status, int limit) {
        String where = status == null ? null : DbHelper.C_HIST_STATUS + " = ?";
        String[] args = status == null ? null : new String[]{status};
        String order = DbHelper.C_HIST_CREATED_AT + " DESC, " + DbHelper.C_HIST_ID + " DESC";
        List<HistoryEntry> out = new ArrayList<>();
        try (Cursor c = helper.getReadableDatabase().query(DbHelper.T_HISTORY, null, where, args,
                null, null, order, limit > 0 ? String.valueOf(limit) : null)) {
            while (c.moveToNext()) {
                out.add(read(c));
            }
        }
        return out;
    }

    public boolean delete(long id) {
        return helper.getWritableDatabase().delete(DbHelper.T_HISTORY,
                DbHelper.C_HIST_ID + " = ?", new String[]{String.valueOf(id)}) > 0;
    }

    /** Deletes every entry and returns how many were removed. */
    public int clearAll() {
        return helper.getWritableDatabase().delete(DbHelper.T_HISTORY, "1", null);
    }

    // ----------------------------------------------------------------- stats

    /** Number of entries, optionally only one status (null = all). */
    public int count(String status) {
        SQLiteDatabase db = helper.getReadableDatabase();
        if (status == null) {
            return (int) DatabaseUtils.queryNumEntries(db, DbHelper.T_HISTORY);
        }
        return (int) DatabaseUtils.queryNumEntries(db, DbHelper.T_HISTORY,
                DbHelper.C_HIST_STATUS + " = ?", new String[]{status});
    }

    /** The dishes that appear most often (accepted and re-drawn together). */
    public List<NameCount> topDishes(int limit) {
        int n = Math.max(1, limit);
        String sql = "SELECT " + DbHelper.C_HIST_NAME_TH + ", MAX(" + DbHelper.C_HIST_NAME_EN
                + "), COUNT(*) AS c FROM " + DbHelper.T_HISTORY
                + " GROUP BY " + DbHelper.C_HIST_NAME_TH
                + " ORDER BY c DESC, " + DbHelper.C_HIST_NAME_TH + " ASC LIMIT " + n;
        List<NameCount> out = new ArrayList<>();
        try (Cursor c = helper.getReadableDatabase().rawQuery(sql, null)) {
            while (c.moveToNext()) {
                out.add(new NameCount(c.getString(0), c.getString(1), c.getInt(2)));
            }
        }
        return out;
    }

    /**
     * Entries per calendar day for the last {@code days} days (local time zone).
     * Index 0 is the oldest day, the last index is today.
     */
    public int[] countPerDay(int days) {
        int n = Math.max(1, days);
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        cal.add(Calendar.DAY_OF_YEAR, -(n - 1));
        int[] out = new int[n];
        SQLiteDatabase db = helper.getReadableDatabase();
        for (int i = 0; i < n; i++) {
            long start = cal.getTimeInMillis();
            cal.add(Calendar.DAY_OF_YEAR, 1);
            long end = cal.getTimeInMillis();
            out[i] = (int) DatabaseUtils.queryNumEntries(db, DbHelper.T_HISTORY,
                    DbHelper.C_HIST_CREATED_AT + " >= ? AND " + DbHelper.C_HIST_CREATED_AT + " < ?",
                    new String[]{String.valueOf(start), String.valueOf(end)});
        }
        return out;
    }

    private static HistoryEntry read(Cursor c) {
        HistoryEntry e = new HistoryEntry();
        e.id = c.getLong(c.getColumnIndexOrThrow(DbHelper.C_HIST_ID));
        int menuCol = c.getColumnIndexOrThrow(DbHelper.C_HIST_MENU_ID);
        e.menuId = c.isNull(menuCol) ? null : c.getLong(menuCol);
        e.nameTh = c.getString(c.getColumnIndexOrThrow(DbHelper.C_HIST_NAME_TH));
        e.nameEn = c.getString(c.getColumnIndexOrThrow(DbHelper.C_HIST_NAME_EN));
        e.level = c.getInt(c.getColumnIndexOrThrow(DbHelper.C_HIST_LEVEL));
        e.stickNo = c.getInt(c.getColumnIndexOrThrow(DbHelper.C_HIST_STICK_NO));
        e.status = c.getString(c.getColumnIndexOrThrow(DbHelper.C_HIST_STATUS));
        e.mode = c.getString(c.getColumnIndexOrThrow(DbHelper.C_HIST_MODE));
        e.createdAt = c.getLong(c.getColumnIndexOrThrow(DbHelper.C_HIST_CREATED_AT));
        return e;
    }
}
