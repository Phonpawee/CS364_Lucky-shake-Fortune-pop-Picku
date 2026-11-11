package com.cs364.luckyshake.logic;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GameConfigTest {

    @Test
    public void levelWeights_haveOnePerLevelAndSumTo100() {
        int[] weights = GameConfig.levelWeights();
        assertEquals(GameConfig.LEVEL_COUNT, weights.length);
        int sum = 0;
        for (int w : weights) sum += w;
        assertEquals(100, sum);
    }

    @Test
    public void levelWeights_returnsCopy() {
        GameConfig.levelWeights()[0] = 999;
        assertEquals(20, GameConfig.levelWeights()[0]);
    }

    @Test
    public void weightRange_isOrdered() {
        assertTrue(GameConfig.MIN_WEIGHT <= GameConfig.DEFAULT_WEIGHT);
        assertTrue(GameConfig.DEFAULT_WEIGHT <= GameConfig.MAX_WEIGHT);
        assertEquals(GameConfig.MIN_WEIGHT, GameConfig.clampWeight(-5));
        assertEquals(GameConfig.MAX_WEIGHT, GameConfig.clampWeight(50));
    }

    @Test
    public void shakeThreshold_staysPositiveAtMaxSensitivity() {
        double t = GameConfig.SHAKE_THRESHOLD_BASE
                - GameConfig.SHAKE_THRESHOLD_STEP * GameConfig.MAX_SENSITIVITY;
        assertTrue(t > 0);
    }
}