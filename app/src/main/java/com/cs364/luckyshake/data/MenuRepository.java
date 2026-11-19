package com.cs364.luckyshake.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.cs364.luckyshake.logic.GameConfig;
import com.cs364.luckyshake.logic.WeightPolicy;

/**
 * Reads and writes menu sets and menus. Calls are synchronous: the data is tiny,
 * so calling from the main thread is fine for this project.
 */
public class MenuRepository {
    // Keep in sync with GameConfig (issue #1).
    public static final int MIN_WEIGHT = GameConfig.MIN_WEIGHT;
    public static final int MAX_WEIGHT = GameConfig.MAX_WEIGHT;

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

    /**
     * Adds back the starter sets and dishes that are missing. The user's own sets and dishes
     * are not touched. Returns how many dishes were added, or -1 if it failed (nothing is changed).
     */
    public int restoreStarterMenus() {
        return SeedData.restoreMissing(helper.getWritableDatabase());
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
     * can be used as the stick number. The weekly weight decay is applied (and saved) here.
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
        long now = System.currentTimeMillis();
        for (Menu m : out) {
            applyDecay(m, now);
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

    /** One menu (weekly decay applied), or null if it does not exist. */
    public Menu getMenu(long id) {
        Menu m = readMenuRow(id);
        if (m != null) {
            applyDecay(m, System.currentTimeMillis());
        }
        return m;
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
     * ignoring upper/lower case, including accented letters such as with an accent).
     * Used by form validation.
     *
     * <p>The comparison is done in Java: SQLite's built-in LOWER() only folds ASCII letters,
     * and the app does not load the ICU extension.
     *
     * @param excludeId id of the menu being edited (0 when adding a new one)
     */
    public boolean nameExists(long setId, String name, long excludeId) {
        String wanted = foldName(name);
        if (wanted.isEmpty()) {
            return false;
        }
        String sql = "SELECT " + DbHelper.C_MENU_NAME_TH + ", " + DbHelper.C_MENU_NAME_EN
                + " FROM " + DbHelper.T_MENU
                + " WHERE " + DbHelper.C_MENU_SET_ID + " = ? AND " + DbHelper.C_MENU_ID + " != ?";
        try (Cursor c = helper.getReadableDatabase().rawQuery(sql,
                new String[]{String.valueOf(setId), String.valueOf(excludeId)})) {
            while (c.moveToNext()) {
                String nameEn = foldName(c.getString(1));
                if (wanted.equals(foldName(c.getString(0)))
                        || (!nameEn.isEmpty() && wanted.equals(nameEn))) {
                    return true;
                }
            }
            return false;
        }
    }

    /** Trims and lower-cases for name comparison; Locale. ROOT so the result never depends on the device language. */
    private static String foldName(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }

    // --------------------------------------------------------------- weights

    /**
     * Applies an accept / ask-again event through {@link WeightPolicy} (after the weekly decay)
     * and stamps last_touched = now. Returns the new weight, or -1 if the menu does not exist.
     *
     * <p>The decay and the new last_touched use the same {@code now}, so a week boundary
     * crossed between them cannot swallow that week's decay.
     */
    public int recordEvent(long menuId, WeightPolicy.Event event) {
        long now = System.currentTimeMillis();
        Menu m = readMenuRow(menuId);
        if (m == null) {
            return -1;
        }
        int w = WeightPolicy.apply(WeightPolicy.decay(m.weight, m.lastTouched, now), event);
        updateWeight(menuId, w, now);
        return w;
    }

    /**
     * Adds {@code delta} to the weight (clamped to 1..10, after the weekly decay) and stamps
     * last_touched = now. Returns the new weight, or -1 if the menu does not exist.
     * Prefer {@link #recordEvent} for accept / ask again.
     */
    public int adjustWeight(long menuId, int delta) {
        long now = System.currentTimeMillis();
        Menu m = readMenuRow(menuId);
        if (m == null) {
            return -1;
        }
        int w = clamp(WeightPolicy.decay(m.weight, m.lastTouched, now) + delta);
        updateWeight(menuId, w, now);
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

    /**
     * Moves the weight one step toward the default for every whole week since last_touched
     * (see {@link WeightPolicy#decay}) and saves it, so the decay is counted only once.
     * Updates {@code m} in place.
     */
    /** One menu row exactly as stored (no decay), or null. */
    private Menu readMenuRow(long id) {
        try (Cursor c = helper.getReadableDatabase().query(DbHelper.T_MENU, null, BY_ID,
                new String[]{String.valueOf(id)}, null, null, null)) {
            return c.moveToFirst() ? readMenu(c) : null;
        }
    }

    private void applyDecay(Menu m, long now) {
        int decayed = WeightPolicy.decay(m.weight, m.lastTouched, now);
        if (decayed == m.weight) {
            return;
        }
        long touched = WeightPolicy.decayedLastTouched(m.lastTouched, now);
        updateWeight(m.id, decayed, touched);
        m.weight = decayed;
        m.lastTouched = touched;
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
