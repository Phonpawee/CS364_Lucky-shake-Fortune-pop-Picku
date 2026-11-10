package com.cs364.luckyshake.data;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

/** Starter data: 3 menu sets with 22 dishes. Runs once, when the database is created. */
final class SeedData {

    private SeedData() {
    }

    static void insertAll(SQLiteDatabase db) {
        long now = System.currentTimeMillis();

        long lunch = insertSet(db, "อาหารกลางวัน", "Lunch");
        insertMenu(db, lunch, "ข้าวมันไก่", "Hainanese chicken rice", Menu.TYPE_RICE, 0, now);
        insertMenu(db, lunch, "กะเพราไก่ไข่ดาว", "Basil chicken & fried egg", Menu.TYPE_RICE, Menu.TAG_SPICY, now);
        insertMenu(db, lunch, "ก๋วยเตี๋ยวเรือ", "Boat noodles", Menu.TYPE_NOODLE, Menu.TAG_SPICY, now);
        insertMenu(db, lunch, "ส้มตำไก่ย่าง", "Papaya salad & grilled chicken", Menu.TYPE_OTHER, Menu.TAG_SPICY, now);
        insertMenu(db, lunch, "ข้าวหน้าไก่ทอด", "Fried chicken rice", Menu.TYPE_RICE, Menu.TAG_FRIED, now);
        insertMenu(db, lunch, "ผัดซีอิ๊ว", "Pad see ew", Menu.TYPE_NOODLE, 0, now);
        insertMenu(db, lunch, "ข้าวผัดปู", "Crab fried rice", Menu.TYPE_RICE, 0, now);
        insertMenu(db, lunch, "ราดหน้าหมู", "Pork gravy noodles", Menu.TYPE_NOODLE, 0, now);
        insertMenu(db, lunch, "ข้าวผัดผักรวม", "Mixed veggie rice", Menu.TYPE_RICE, Menu.TAG_VEGGIE, now);
        insertMenu(db, lunch, "ข้าวหมูทอดกระเทียม", "Garlic pork rice", Menu.TYPE_RICE, Menu.TAG_FRIED, now);

        long dinner = insertSet(db, "อาหารเย็น", "Dinner");
        insertMenu(db, dinner, "หมูกระทะ", "Thai BBQ hotpot", Menu.TYPE_OTHER, 0, now);
        insertMenu(db, dinner, "ต้มยำกุ้ง", "Tom yum goong", Menu.TYPE_OTHER, Menu.TAG_SPICY, now);
        insertMenu(db, dinner, "ปลาทอดน้ำปลา", "Fried fish with fish sauce", Menu.TYPE_OTHER, Menu.TAG_FRIED, now);
        insertMenu(db, dinner, "แกงเขียวหวานไก่", "Green curry chicken", Menu.TYPE_OTHER, Menu.TAG_SPICY, now);
        insertMenu(db, dinner, "ข้าวต้มปลา", "Fish rice porridge", Menu.TYPE_RICE, 0, now);
        insertMenu(db, dinner, "ผัดไทยกุ้งสด", "Pad Thai with shrimp", Menu.TYPE_NOODLE, 0, now);

        long sweets = insertSet(db, "ของหวาน", "Sweets & drinks");
        insertMenu(db, sweets, "มะม่วงข้าวเหนียว", "Mango sticky rice", Menu.TYPE_DESSERT, 0, now);
        insertMenu(db, sweets, "บัวลอย", "Bua loy", Menu.TYPE_DESSERT, 0, now);
        insertMenu(db, sweets, "ไอศกรีมกะทิ", "Coconut ice cream", Menu.TYPE_DESSERT, 0, now);
        insertMenu(db, sweets, "ชานมไข่มุก", "Bubble milk tea", Menu.TYPE_DRINK, 0, now);
        insertMenu(db, sweets, "กล้วยทอด", "Fried bananas", Menu.TYPE_DESSERT, Menu.TAG_FRIED, now);
        insertMenu(db, sweets, "น้ำแข็งใสรวมมิตร", "Shaved ice medley", Menu.TYPE_DESSERT, 0, now);
    }

    private static long insertSet(SQLiteDatabase db, String nameTh, String nameEn) {
        ContentValues v = new ContentValues();
        v.put(DbHelper.C_SET_NAME_TH, nameTh);
        v.put(DbHelper.C_SET_NAME_EN, nameEn);
        return db.insert(DbHelper.T_SET, null, v);
    }

    private static void insertMenu(SQLiteDatabase db, long setId, String nameTh, String nameEn,
                                   String type, int tags, long now) {
        ContentValues v = new ContentValues();
        v.put(DbHelper.C_MENU_SET_ID, setId);
        v.put(DbHelper.C_MENU_NAME_TH, nameTh);
        v.put(DbHelper.C_MENU_NAME_EN, nameEn);
        v.put(DbHelper.C_MENU_TYPE, type);
        v.put(DbHelper.C_MENU_TAGS, tags);
        v.put(DbHelper.C_MENU_WEIGHT, DbHelper.DEFAULT_WEIGHT);
        v.put(DbHelper.C_MENU_LAST_TOUCHED, now);
        v.put(DbHelper.C_MENU_CREATED_AT, now);
        db.insert(DbHelper.T_MENU, null, v);
    }
}
