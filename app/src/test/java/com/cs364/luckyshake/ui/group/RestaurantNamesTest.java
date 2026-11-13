package com.cs364.luckyshake.ui.group;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Pure Java unit tests for {@link RestaurantNames}.
 */
public class RestaurantNamesTest {

    @Test
    public void testNullOrEmptyInputReturnsEmptyList() {
        assertTrue(RestaurantNames.normalize(null).isEmpty());
        assertTrue(RestaurantNames.normalize(Collections.emptyList()).isEmpty());
        assertTrue(RestaurantNames.normalize(Arrays.asList("   ", "", "  \t ")).isEmpty());
    }

    @Test
    public void testCaseInsensitiveDeduplication() {
        List<String> raw = Arrays.asList("Pizza", " pizza ", "PIZZA");
        List<String> result = RestaurantNames.normalize(raw);

        assertEquals(1, result.size());
        assertEquals("Pizza", result.get(0));
    }

    @Test
    public void testLengthTruncation() {
        String longName = "1234567890123456789012345678901234567890EXTRA_TEXT";
        List<String> result = RestaurantNames.normalize(Collections.singletonList(longName));

        assertEquals(1, result.size());
        assertEquals(40, result.get(0).length());
        assertEquals("1234567890123456789012345678901234567890", result.get(0));
    }

    @Test
    public void testOptionalFieldsEmpty() {
        List<String> raw = Arrays.asList("Shabu Lab", "", "  ");
        List<String> result = RestaurantNames.normalize(raw);

        assertEquals(1, result.size());
        assertEquals("Shabu Lab", result.get(0));
    }

    @Test
    public void testMaxThreeItems() {
        List<String> raw = Arrays.asList("Store A", "Store B", "Store C", "Store D");
        List<String> result = RestaurantNames.normalize(raw);

        assertEquals(3, result.size());
        assertEquals("Store A", result.get(0));
        assertEquals("Store B", result.get(1));
        assertEquals("Store C", result.get(2));
    }
}
