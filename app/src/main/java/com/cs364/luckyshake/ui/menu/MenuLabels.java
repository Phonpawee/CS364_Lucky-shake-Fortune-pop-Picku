package com.cs364.luckyshake.ui.menu;

import android.content.Context;

import com.cs364.luckyshake.R;
import com.cs364.luckyshake.data.Menu;

/**
 * Turns the stored type / tag codes of a menu into text in the current language.
 * Shared by the menu list, the add/edit form and the filters.
 */
public final class MenuLabels {

    private MenuLabels() {
    }

    /** Name of a dish type (Menu.TYPE_*) in the current language. */
    public static String type(Context context, String type) {
        if (Menu.TYPE_RICE.equals(type)) {
            return context.getString(R.string.menu_label_type_rice);
        } else if (Menu.TYPE_NOODLE.equals(type)) {
            return context.getString(R.string.menu_label_type_noodle);
        } else if (Menu.TYPE_DESSERT.equals(type)) {
            return context.getString(R.string.menu_label_type_dessert);
        } else if (Menu.TYPE_DRINK.equals(type)) {
            return context.getString(R.string.menu_label_type_drink);
        }
        return context.getString(R.string.menu_label_type_other);
    }

    /** Tags of a menu joined with " · " (empty text when there are none). */
    public static String tags(Context context, int tags) {
        StringBuilder sb = new StringBuilder();
        if ((tags & Menu.TAG_SPICY) != 0) {
            append(sb, context.getString(R.string.menu_label_tag_spicy));
        }
        if ((tags & Menu.TAG_FRIED) != 0) {
            append(sb, context.getString(R.string.menu_label_tag_fried));
        }
        if ((tags & Menu.TAG_VEGGIE) != 0) {
            append(sb, context.getString(R.string.menu_label_tag_veggie));
        }
        return sb.toString();
    }

    /** "Rice · Spicy · Fried" — the type followed by the tags. */
    public static String typeAndTags(Context context, Menu menu) {
        String tags = tags(context, menu.tags);
        String type = type(context, menu.type);
        return tags.isEmpty() ? type : type + " · " + tags;
    }

    private static void append(StringBuilder sb, String part) {
        if (sb.length() > 0) {
            sb.append(" · ");
        }
        sb.append(part);
    }
}
