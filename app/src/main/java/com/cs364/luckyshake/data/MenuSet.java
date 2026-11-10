package com.cs364.luckyshake.data;

/** A named group of menus, e.g. "Lunch". Table: menu_set. */
public class MenuSet {
    public long id;
    public String nameTh;
    public String nameEn;

    public MenuSet() {
    }

    public MenuSet(long id, String nameTh, String nameEn) {
        this.id = id;
        this.nameTh = nameTh;
        this.nameEn = nameEn;
    }

    /** Name for display: Thai if {@code thai} is true, otherwise English (falls back to Thai). */
    public String getName(boolean thai) {
        return (thai || nameEn == null || nameEn.isEmpty()) ? nameTh : nameEn;
    }
}
