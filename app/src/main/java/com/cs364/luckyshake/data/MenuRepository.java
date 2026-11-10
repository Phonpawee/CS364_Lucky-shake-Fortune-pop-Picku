package com.cs364.luckyshake.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

/**
 * Reads and writes menu sets and menus. Calls are synchronous: the data is tiny,
 * so calling from the main thread is fine for this project.
 */
public class MenuRepository {
    // Keep in sync with GameConfig (issue #1).
    public static final int MIN_WEIGHT = 1;
    public static final int MAX_WEIGHT = 10;

    private static final String BY_ID = DbHelper.C_MENU_ID + " = ?";

    private final DbHelper helper;

    public MenuRepository(Context context) {
        this.helper = DbHelper.getInstance(context);
    }

    // ------------------------------------------------------------------ sets

    /** All sets, oldest first. */
    public List<MenuSet> getSets() {
        SQLiteDatabase db = helper.getReadableDatabase();
        List<MenuSet> out = new ArrayList<>();
        try (Cursor c = db.query(DbHelper.T_SET, null, null, null, null, null,
                DbHelper.C_SET_ID + " ASC")) {
            while (c.moveToNext()) {
                out.add(readSet(c));
            }
        }
        return out;
    }

    /** One set, or null if it does not exist. */
    public MenuSet getSet(long id) {
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.query(DbHelper.T_SET, null, DbHelper.C_SET_ID + " = ?",
                new String[]{String.valueOf(id)}, null, null, null)) {
            return c.moveToFirst() ? readSet(c) : null;
        }
    }

    public int countSets() {
        return (int) DatabaseUtils.queryNumEntries(helper.getReadableDatabase(), DbHelper.T_SET);
    }

    /** Adds a set and returns its id, or -1 if the Thai name is empty. */
    public long addSet(String nameTh, String nameEn) {
        String th = nameTh == null ? "" : nameTh.trim();
        if (th.isEmpty()) {
            return -1;
        }
        ContentValues v = new ContentValues();
        v.put(DbHelper.C_SET_NAME_TH, th);
        v.put(DbHelper.C_SET_NAME_EN, nameEn == null ? "" : nameEn.trim());
        return helper.getWritableDatabase().insert(DbHelper.T_SET, null, v);
    }

    /** Renames a set. Returns false if it does not exist or the name is empty. */
    public boolean renameSet(long id, String nameTh, String nameEn) {
        String th = nameTh == null ? "" : nameTh.trim();
        if (th.isEmpty()) {
            return false;
        }
        ContentValues v = new ContentValues();
        v.put(DbHelper.C_SET_NAME_TH, th);
        v.put(DbHelper.C_SET_NAME_EN, nameEn == null ? "" : nameEn.trim());
        return helper.getWritableDatabase().update(DbHelper.T_SET, v,
                DbHelper.C_SET_ID + " = ?", new String[]{String.valueOf(id)}) > 0;
    }

    /**
     * Deletes a set and, through ON DELETE CASCADE, every menu inside it.
     * History rows are kept. The UI decides whether the last set may be deleted
     * (see countSets()).
     */
    public boolean deleteSet(long id) {
        return helper.getWritableDatabase().delete(DbHelper.T_SET,
                DbHelper.C_SET_ID + " = ?", new String[]{String.valueOf(id)}) > 0;
    }

    // ----------------------------------------------------------------- menus

    /**
     * Menus of a set, in a stable order (oldest first), so the position in the list
     * can be used as the stick number.
     *
     * @param filter optional filter (null = no filter)
     * @param search optional text; matches the Thai or English name (null/empty = all)
     */
    public List<Menu> getMenus(long setId, MenuFilter filter, String search) {
        List<String> args = new ArrayList<>();
        String where = buildWhere(setId, filter, search, args);
        List<Menu> out = new ArrayList<>();
        try (Cursor c = helper.getReadableDatabase().query(DbHelper.T_MENU, null, where,
                args.toArray(new String[0]), null, null, DbHelper.C_MENU_ID + " ASC")) {
            while (c.moveToNext()) {
                out.add(readMenu(c));
            }
        }
        return out;
    }

    /** Number of menus in a set that pass the filter (use this for "left 7/10"). */
    public int countMenus(long setId, MenuFilter filter) {
        List<String> args = new ArrayList<>();
        String where = buildWhere(setId, filter, null, args);
        return (int) DatabaseUtils.queryNumEntries(helper.getReadableDatabase(),
                DbHelper.T_MENU, where, args.toArray(new String[0]));
    }

    /** One menu, or null if it does not exist. */
    public Menu getMenu(long id) {
        try (Cursor c = helper.getReadableDatabase().query(DbHelper.T_MENU, null, BY_ID,
                new String[]{String.valueOf(id)}, null, null, null)) {
            return c.moveToFirst() ? readMenu(c) : null;
        }
    }

    /**
     * Adds a menu and returns its id (also stored in {@code m.id}), or -1 if the name is
     * empty. Missing type/weight/timestamps get defaults.
     */
    public long addMenu(Menu m) {
        String th = m.nameTh == null ? "" : m.nameTh.trim();
        if (th.isEmpty()) {
            return -1;
        }
        long now = System.currentTimeMillis();
        ContentValues v = new ContentValues();
        v.put(DbHelper.C_MENU_SET_ID, m.setId);
        v.put(DbHelper.C_MENU_NAME_TH, th);
        v.put(DbHelper.C_MENU_NAME_EN, m.nameEn == null ? "" : m.nameEn.trim());
        v.put(DbHelper.C_MENU_TYPE, (m.type == null || m.type.isEmpty()) ? Menu.TYPE_OTHER : m.type);
        v.put(DbHelper.C_MENU_TAGS, m.tags);
        v.put(DbHelper.C_MENU_WEIGHT, m.weight < MIN_WEIGHT ? DbHelper.DEFAULT_WEIGHT : clamp(m.weight));
        v.put(DbHelper.C_MENU_LAST_TOUCHED, m.lastTouched > 0 ? m.lastTouched : now);
        v.put(DbHelper.C_MENU_CREATED_AT, m.createdAt > 0 ? m.createdAt : now);
        long id = helper.getWritableDatabase().insert(DbHelper.T_MENU, null, v);
        if (id > 0) {
            m.id = id;
        }
        return id;
    }

    /** Updates name, type and tags (not the weight, not the set). */
    public boolean updateMenu(Menu m) {
        String th = m.nameTh == null ? "" : m.nameTh.trim();
        if (th.isEmpty()) {
            return false;
        }
        ContentValues v = new ContentValues();
        v.put(DbHelper.C_MENU_NAME_TH, th);
        v.put(DbHelper.C_MENU_NAME_EN, m.nameEn == null ? "" : m.nameEn.trim());
        v.put(DbHelper.C_MENU_TYPE, (m.type == null || m.type.isEmpty()) ? Menu.TYPE_OTHER : m.type);
        v.put(DbHelper.C_MENU_TAGS, m.tags);
        return helper.getWritableDatabase().update(DbHelper.T_MENU, v, BY_ID,
                new String[]{String.valueOf(m.id)}) > 0;
    }

    /** Deletes a menu. Its history rows stay (their menu_id becomes NULL). */
    public boolean deleteMenu(long id) {
        return helper.getWritableDatabase().delete(DbHelper.T_MENU, BY_ID,
                new String[]{String.valueOf(id)}) > 0;
    }

    /**
     * True if another menu in the same set already has this name (Thai or English,
     * ignoring upper/lower case). Used by form validation.
     *
     * @param excludeId id of the menu being edited (0 when adding a new one)
     */
    public boolean nameExists(long setId, String name, long excludeId) {
        String n = name == null ? "" : name.trim();
        if (n.isEmpty()) {
            return false;
        }
        String sql = "SELECT 1 FROM " + DbHelper.T_MENU
                + " WHERE " + DbHelper.C_MENU_SET_ID + " = ? AND " + DbHelper.C_MENU_ID + " != ?"
                + " AND (LOWER(" + DbHelper.C_MENU_NAME_TH + ") = LOWER(?)"
                + " OR (" + DbHelper.C_MENU_NAME_EN + " != '' AND LOWER(" + DbHelper.C_MENU_NAME_EN + ") = LOWER(?)))"
                + " LIMIT 1";
        try (Cursor c = helper.getReadableDatabase().rawQuery(sql,
                new String[]{String.valueOf(setId), String.valueOf(excludeId), n, n})) {
            return c.moveToFirst();
        }
    }

    // --------------------------------------------------------------- weights

    /**
     * Adds {@code delta} to the weight (clamped to 1..10) and stamps last_touched = now.
     * Used for "accept" (+1) and "ask again" (-1). Returns the new weight, or -1 if the menu
     * does not exist.
     */
    public int adjustWeight(long menuId, int delta) {
        Menu m = getMenu(menuId);
        if (m == null) {
            return -1;
        }
        int w = clamp(m.weight + delta);
        updateWeight(menuId, w, System.currentTimeMillis());
        return w;
    }

    /**
     * Sets the weight (clamped to 1..10) and last_touched exactly as given. Used by the
     * weekly decay, which moves last_touched forward by whole weeks.
     */
    public boolean updateWeight(long menuId, int weight, long lastTouched) {
        ContentValues v = new ContentValues();
        v.put(DbHelper.C_MENU_WEIGHT, clamp(weight));
        v.put(DbHelper.C_MENU_LAST_TOUCHED, lastTouched);
        return helper.getWritableDatabase().update(DbHelper.T_MENU, v, BY_ID,
                new String[]{String.valueOf(menuId)}) > 0;
    }

    /** Sets every menu of a set back to the default weight. */
    public void resetWeights(long setId) {
        ContentValues v = new ContentValues();
        v.put(DbHelper.C_MENU_WEIGHT, DbHelper.DEFAULT_WEIGHT);
        v.put(DbHelper.C_MENU_LAST_TOUCHED, System.currentTimeMillis());
        helper.getWritableDatabase().update(DbHelper.T_MENU, v,
                DbHelper.C_MENU_SET_ID + " = ?", new String[]{String.valueOf(setId)});
    }

    // --------------------------------------------------------------- helpers

    private static int clamp(int w) {
        return Math.max(MIN_WEIGHT, Math.min(MAX_WEIGHT, w));
    }

    /** Escapes % _ \ so user text is matched literally by LIKE ... ESCAPE '\'. */
    static String escapeLike(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private static String buildWhere(long setId, MenuFilter f, String search, List<String> args) {
        StringBuilder w = new StringBuilder(DbHelper.C_MENU_SET_ID + " = ?");
        args.add(String.valueOf(setId));
        if (f != null) {
            if (f.type != null && !f.type.isEmpty()) {
                w.append(" AND ").append(DbHelper.C_MENU_TYPE).append(" = ?");
                args.add(f.type);
            }
            if (f.noFried) {
                w.append(" AND (").append(DbHelper.C_MENU_TAGS).append(" & ")
                        .append(Menu.TAG_FRIED).append(") = 0");
            }
            if (f.noSpicy) {
                w.append(" AND (").append(DbHelper.C_MENU_TAGS).append(" & ")
                        .append(Menu.TAG_SPICY).append(") = 0");
            }
        }
        if (search != null && !search.trim().isEmpty()) {
            String like = "%" + escapeLike(search.trim()) + "%";
            w.append(" AND (").append(DbHelper.C_MENU_NAME_TH).append(" LIKE ? ESCAPE '\\' OR ")
                    .append(DbHelper.C_MENU_NAME_EN).append(" LIKE ? ESCAPE '\\')");
            args.add(like);
            args.add(like);
        }
        return w.toString();
    }

    private static MenuSet readSet(Cursor c) {
        return new MenuSet(
                c.getLong(c.getColumnIndexOrThrow(DbHelper.C_SET_ID)),
                c.getString(c.getColumnIndexOrThrow(DbHelper.C_SET_NAME_TH)),
                c.getString(c.getColumnIndexOrThrow(DbHelper.C_SET_NAME_EN)));
    }

    private static Menu readMenu(Cursor c) {
        Menu m = new Menu();
        m.id = c.getLong(c.getColumnIndexOrThrow(DbHelper.C_MENU_ID));
        m.setId = c.getLong(c.getColumnIndexOrThrow(DbHelper.C_MENU_SET_ID));
        m.nameTh = c.getString(c.getColumnIndexOrThrow(DbHelper.C_MENU_NAME_TH));
        m.nameEn = c.getString(c.getColumnIndexOrThrow(DbHelper.C_MENU_NAME_EN));
        m.type = c.getString(c.getColumnIndexOrThrow(DbHelper.C_MENU_TYPE));
        m.tags = c.getInt(c.getColumnIndexOrThrow(DbHelper.C_MENU_TAGS));
        m.weight = c.getInt(c.getColumnIndexOrThrow(DbHelper.C_MENU_WEIGHT));
        m.lastTouched = c.getLong(c.getColumnIndexOrThrow(DbHelper.C_MENU_LAST_TOUCHED));
        m.createdAt = c.getLong(c.getColumnIndexOrThrow(DbHelper.C_MENU_CREATED_AT));
        return m;
    }
}
