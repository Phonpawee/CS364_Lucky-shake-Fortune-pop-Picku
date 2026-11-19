package com.cs364.luckyshake.ui.weights;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

/**
 * Pure Java unit tests for {@link WeightLabels}.
 */
public class WeightLabelsTest {

    @Test
    public void testToProgressClamp() {
        assertEquals(1, WeightLabels.toProgress(0));
        assertEquals(1, WeightLabels.toProgress(-5));
        assertEquals(10, WeightLabels.toProgress(11));
        assertEquals(10, WeightLabels.toProgress(99));
        assertEquals(7, WeightLabels.toProgress(7));
        assertEquals(5, WeightLabels.toProgress(5));
    }

    @Test
    public void testCategory() {
        assertEquals(WeightLabels.LIKED, WeightLabels.category(6));
        assertEquals(WeightLabels.LIKED, WeightLabels.category(10));
        assertEquals(WeightLabels.NEUTRAL, WeightLabels.category(5));
        assertEquals(WeightLabels.DISLIKED, WeightLabels.category(4));
        assertEquals(WeightLabels.DISLIKED, WeightLabels.category(1));
    }

    @Test
    public void testAllDefault() {
        assertTrue(WeightLabels.allDefault(Arrays.asList(5, 5, 5)));
        assertFalse(WeightLabels.allDefault(Arrays.asList(5, 6)));
        assertFalse(WeightLabels.allDefault(Arrays.asList(4, 5, 5)));
        assertTrue(WeightLabels.allDefault(Collections.emptyList()));
        assertTrue(WeightLabels.allDefault(null));
    }
}
