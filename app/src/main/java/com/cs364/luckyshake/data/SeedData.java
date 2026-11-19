package com.cs364.luckyshake.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

/**
 * Starter data: 3 menu sets with 22 dishes. Inserted once, when the database is created, and
 * again on request by {@link #restoreMissing} (the "restore starter dishes" button).
 */
final class SeedData {

    private static final class Dish {
        final String nameTh;
        final String nameEn;
        final String type;
        final int tags;

        Dish(String nameTh, String nameEn, String type, int tags) {
            this.nameTh = nameTh;
            this.nameEn = nameEn;
            this.type = type;
            this.tags = tags;
        }
    }

    private static final class Group {
        final String nameTh;
        final String nameEn;
        final Dish[] dishes;

        Group(String nameTh, String nameEn, Dish... dishes) {
            this.nameTh = nameTh;
            this.nameEn = nameEn;
            this.dishes = dishes;
        }
    }

    private static Dish d(String nameTh, String nameEn, String type, int tags) {
        return new Dish(nameTh, nameEn, type, tags);
    }

    /** Order matters: sets and dishes get ids in this order (the stick number is the position). */
    private static final Group[] STARTER = {
            new Group("อาหารกลางวัน", "Lunch",
                    d("ข้าวมันไก่", "Hainanese chicken rice", Menu.TYPE_RICE, 0),
                    d("กะเพราไก่ไข่ดาว", "Basil chicken & fried egg", Menu.TYPE_RICE, Menu.TAG_SPICY),
                    d("ก๋วยเตี๋ยวเรือ", "Boat noodles", Menu.TYPE_NOODLE, Menu.TAG_SPICY),
                    d("ส้มตำไก่ย่าง", "Papaya salad & grilled chicken", Menu.TYPE_OTHER, Menu.TAG_SPICY),
                    d("ข้าวหน้าไก่ทอด", "Fried chicken rice", Menu.TYPE_RICE, Menu.TAG_FRIED),
                    d("ผัดซีอิ๊ว", "Pad see ew", Menu.TYPE_NOODLE, 0),
                    d("ข้าวผัดปู", "Crab fried rice", Menu.TYPE_RICE, 0),
                    d("ราดหน้าหมู", "Pork gravy noodles", Menu.TYPE_NOODLE, 0),
                    d("ข้าวผัดผักรวม", "Mixed veggie rice", Menu.TYPE_RICE, Menu.TAG_VEGGIE),
                    d("ข้าวหมูทอดกระเทียม", "Garlic pork rice", Menu.TYPE_RICE, Menu.TAG_FRIED)),
            new Group("อาหารเย็น", "Dinner",
                    d("หมูกระทะ", "Thai BBQ hotpot", Menu.TYPE_OTHER, 0),
                    d("ต้มยำกุ้ง", "Tom yum goong", Menu.TYPE_OTHER, Menu.TAG_SPICY),
                    d("ปลาทอดน้ำปลา", "Fried fish with fish sauce", Menu.TYPE_OTHER, Menu.TAG_FRIED),
                    d("แกงเขียวหวานไก่", "Green curry chicken", Menu.TYPE_OTHER, Menu.TAG_SPICY),
                    d("ข้าวต้มปลา", "Fish rice porridge", Menu.TYPE_RICE, 0),
                    d("ผัดไทยกุ้งสด", "Pad Thai with shrimp", Menu.TYPE_NOODLE, 0)),
            new Group("ของหวาน", "Sweets & drinks",
                    d("มะม่วงข้าวเหนียว", "Mango sticky rice", Menu.TYPE_DESSERT, 0),
                    d("บัวลอย", "Bua loy", Menu.TYPE_DESSERT, 0),
                    d("ไอศกรีมกะทิ", "Coconut ice cream", Menu.TYPE_DESSERT, 0),
                    d("ชานมไข่มุก", "Bubble milk tea", Menu.TYPE_DRINK, 0),
                    d("กล้วยทอด", "Fried bananas", Menu.TYPE_DESSERT, Menu.TAG_FRIED),
                    d("น้ำแข็งใสรวมมิตร", "Shaved ice medley", Menu.TYPE_DESSERT, 0)),
    };

    private SeedData() {
    }

    /** Fills a brand-new database. */
    static void insertAll(SQLiteDatabase db) {
        if (restoreMissing(db) < 0) {
            throw new SQLException("Cannot insert the starter data");
        }
    }

    /**
     * Adds the starter sets and dishes that are not there (matched by Thai name). Nothing is
     * deleted or changed, so the user's own sets and dishes stay. Returns how many dishes were added,
     * or -1 if an insert failed (everything is then rolled back).
     */
    static int restoreMissing(SQLiteDatabase db) {
        long now = System.currentTimeMillis();
        int added = 0;
        db.beginTransaction();
        try {
            for (Group g : STARTER) {
                long setId = findSet(db, g.nameTh);
                if (setId < 0) {
                    setId = insertSet(db, g.nameTh, g.nameEn);
                    if (setId < 0) {
                        return -1;      // no setTransactionSuccessful(): the transaction rolls back
                    }
                }
                for (Dish dish : g.dishes) {
                    if (!menuExists(db, setId, dish.nameTh)) {
                        if (insertMenu(db, setId, dish, now) < 0) {
                            return -1;
                        }
                        added++;
                    }
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        return added;
    }

    private static long findSet(SQLiteDatabase db, String nameTh) {
        try (Cursor c = db.query(DbHelper.T_SET, new String[]{DbHelper.C_SET_ID},
                DbHelper.C_SET_NAME_TH + " = ?", new String[]{nameTh}, null, null, null, "1")) {
            return c.moveToFirst() ? c.getLong(0) : -1;
        }
    }

    private static boolean menuExists(SQLiteDatabase db, long setId, String nameTh) {
        try (Cursor c = db.query(DbHelper.T_MENU, new String[]{DbHelper.C_MENU_ID},
                DbHelper.C_MENU_SET_ID + " = ? AND " + DbHelper.C_MENU_NAME_TH + " = ?",
                new String[]{String.valueOf(setId), nameTh}, null, null, null, "1")) {
            return c.moveToFirst();
        }
    }

    private static long insertSet(SQLiteDatabase db, String nameTh, String nameEn) {
        ContentValues v = new ContentValues();
        v.put(DbHelper.C_SET_NAME_TH, nameTh);
        v.put(DbHelper.C_SET_NAME_EN, nameEn);
        return db.insert(DbHelper.T_SET, null, v);
    }

    private static long insertMenu(SQLiteDatabase db, long setId, Dish dish, long now) {
        ContentValues v = new ContentValues();
        v.put(DbHelper.C_MENU_SET_ID, setId);
        v.put(DbHelper.C_MENU_NAME_TH, dish.nameTh);
        v.put(DbHelper.C_MENU_NAME_EN, dish.nameEn);
        v.put(DbHelper.C_MENU_TYPE, dish.type);
        v.put(DbHelper.C_MENU_TAGS, dish.tags);
        v.put(DbHelper.C_MENU_WEIGHT, DbHelper.DEFAULT_WEIGHT);
        v.put(DbHelper.C_MENU_LAST_TOUCHED, now);
        v.put(DbHelper.C_MENU_CREATED_AT, now);
        return db.insert(DbHelper.T_MENU, null, v);
    }
}
