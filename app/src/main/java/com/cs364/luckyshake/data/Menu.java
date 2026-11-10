package com.cs364.luckyshake.data;

/** One dish that can be drawn. Table: menu. */
public class Menu {
    // Dish types (column "type").
    public static final String TYPE_RICE = "rice";
    public static final String TYPE_NOODLE = "noodle";
    public static final String TYPE_DESSERT = "dessert";
    public static final String TYPE_DRINK = "drink";
    public static final String TYPE_OTHER = "other";

    // Tags are stored together as a bitmask in column "tags".
    public static final int TAG_SPICY = 1;
    public static final int TAG_FRIED = 2;
    public static final int TAG_VEGGIE = 4;

    public long id;
    public long setId;
    public String nameTh;
    public String nameEn;      // may be empty for dishes typed by the user
    public String type;        // one of TYPE_*
    public int tags;           // bitmask of TAG_*
    public int weight;         // adaptive weight 1..10 (default 5)
    public long lastTouched;   // epoch millis, used by the weight decay
    public long createdAt;     // epoch millis

    public boolean hasTag(int tag) {
        return (tags & tag) != 0;
    }

    /** Name for display: Thai if {@code thai} is true, otherwise English (falls back to Thai). */
    public String getName(boolean thai) {
        return (thai || nameEn == null || nameEn.isEmpty()) ? nameTh : nameEn;
    }
}
