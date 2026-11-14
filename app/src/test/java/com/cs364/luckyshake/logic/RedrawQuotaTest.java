package com.cs364.luckyshake.logic;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RedrawQuotaTest {

    @Test
    public void default_usesGameConfigMax() {
        RedrawQuota quota = new RedrawQuota();
        assertEquals(GameConfig.MAX_REDRAWS, quota.max());
        assertEquals(GameConfig.MAX_REDRAWS, quota.remaining());
    }

    @Test
    public void consume_decreasesByOneUntilEmpty() {
        RedrawQuota quota = new RedrawQuota(3);
        for (int left = 2; left >= 0; left--) {
            assertTrue(quota.consume());
            assertEquals(left, quota.remaining());
        }
        assertTrue(quota.isEmpty());
    }

    @Test
    public void consume_whenEmpty_returnsFalseAndStaysAtZero() {
        RedrawQuota quota = new RedrawQuota(1);
        quota.consume();
        assertFalse(quota.consume());
        assertFalse(quota.consume());
        assertEquals(0, quota.remaining());
    }

    @Test
    public void reset_restoresFullQuota() {
        RedrawQuota quota = new RedrawQuota(3);
        quota.consume();
        quota.consume();
        quota.reset();
        assertEquals(3, quota.remaining());
    }

    @Test
    public void constructor_clampsToSettingsRange() {
        assertEquals(GameConfig.MIN_REDRAW_LIMIT, new RedrawQuota(0).max());
        assertEquals(GameConfig.MIN_REDRAW_LIMIT, new RedrawQuota(-4).max());
        assertEquals(GameConfig.MAX_REDRAW_LIMIT, new RedrawQuota(99).max());
    }

    @Test
    public void setMax_lowerThanUsed_leavesZero() {
        RedrawQuota quota = new RedrawQuota(5);
        quota.consume();
        quota.consume();
        quota.consume();
        quota.setMax(2);
        assertEquals(0, quota.remaining());
        quota.setMax(4);
        assertEquals(2, quota.remaining());
    }
}
