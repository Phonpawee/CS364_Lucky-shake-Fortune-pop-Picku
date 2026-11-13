package com.cs364.luckyshake.ui.group;

import com.cs364.luckyshake.logic.GameConfig;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Pure Java utility for normalizing and validating restaurant name inputs (#36).
 */
public final class RestaurantNames {

    private RestaurantNames() {
    }

    /**
     * Normalizes raw restaurant inputs:
     * 1) Trims leading/trailing whitespace.
     * 2) Truncates to max 40 characters (GameConfig.MAX_RESTAURANT_NAME_LENGTH).
     * 3) Discards empty strings.
     * 4) Deduplicates names case-insensitively (preserves casing of first occurrence).
     * 5) Limits result list to max 3 items (GameConfig.MAX_RESTAURANTS_PER_PLAYER).
     */
    public static List<String> normalize(List<String> raw) {
        if (raw == null) {
            return new ArrayList<>();
        }

        List<String> result = new ArrayList<>();
        Set<String> seenKeys = new HashSet<>();

        for (String item : raw) {
            if (item == null) {
                continue;
            }
            String trimmed = item.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            if (trimmed.length() > GameConfig.MAX_RESTAURANT_NAME_LENGTH) {
                trimmed = trimmed.substring(0, GameConfig.MAX_RESTAURANT_NAME_LENGTH);
            }

            String key = trimmed.toLowerCase(Locale.ROOT);
            if (!seenKeys.contains(key)) {
                seenKeys.add(key);
                result.add(trimmed);
                if (result.size() >= GameConfig.MAX_RESTAURANTS_PER_PLAYER) {
                    break;
                }
            }
        }

        return result;
    }
}
