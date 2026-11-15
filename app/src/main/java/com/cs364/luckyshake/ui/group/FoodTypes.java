package com.cs364.luckyshake.ui.group;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Pure Java utility defining the 10 food category keys for group proposal mode (#35).
 * Contains no Android framework dependencies to allow pure JUnit unit testing.
 */
public final class FoodTypes {

    public static final List<String> KEYS = Collections.unmodifiableList(Arrays.asList(
            "food_type_rice",
            "food_type_noodles",
            "food_type_grill",
            "food_type_hotpot",
            "food_type_japanese",
            "food_type_korean",
            "food_type_isan",
            "food_type_fast_food",
            "food_type_dessert_cafe",
            "food_type_healthy_vegetarian"
    ));

    private FoodTypes() {
    }

    /**
     * Checks whether the given string is one of the valid 10 food type keys.
     *
     * @param key the food type key to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValid(String key) {
        return key != null && KEYS.contains(key);
    }
}
