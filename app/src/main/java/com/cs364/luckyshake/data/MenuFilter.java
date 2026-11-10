package com.cs364.luckyshake.data;

/** Filter applied before drawing (screen: Home). All fields are optional. */
public class MenuFilter {
    public String type;        // one of Menu.TYPE_*, or null for all types
    public boolean noFried;    // exclude dishes tagged TAG_FRIED
    public boolean noSpicy;    // exclude dishes tagged TAG_SPICY

    public MenuFilter() {
    }

    public MenuFilter(String type, boolean noFried, boolean noSpicy) {
        this.type = type;
        this.noFried = noFried;
        this.noSpicy = noSpicy;
    }

    /** A filter that lets everything through. */
    public static MenuFilter none() {
        return new MenuFilter();
    }

    public boolean isEmpty() {
        return (type == null || type.isEmpty()) && !noFried && !noSpicy;
    }
}
