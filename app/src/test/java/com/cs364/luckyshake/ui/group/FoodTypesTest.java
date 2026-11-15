package com.cs364.luckyshake.ui.group;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.HashSet;

/**
 * Pure Java unit tests for {@link FoodTypes}.
 */
public class FoodTypesTest {

    @Test
    public void testKeysListCountAndUniqueness() {
        assertEquals(10, FoodTypes.KEYS.size());
        assertEquals(10, new HashSet<>(FoodTypes.KEYS).size());
    }

    @Test
    public void testIsValid() {
        for (String key : FoodTypes.KEYS) {
            assertTrue(FoodTypes.isValid(key));
        }

        assertFalse(FoodTypes.isValid(null));
        assertFalse(FoodTypes.isValid(""));
        assertFalse(FoodTypes.isValid("invalid_key"));
        assertFalse(FoodTypes.isValid("food_type_unknown"));
    }
}
