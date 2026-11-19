package com.cs364.luckyshake.ui.weights;

import com.cs364.luckyshake.logic.GameConfig;

import java.util.List;

/**
 * Pure Java utility for formatting, clamping, and categorizing menu weights (#42).
 * Contains no Android framework dependencies to allow pure JUnit unit testing.
 */
public final class WeightLabels {

    public static final int LIKED = 1;
    public static final int NEUTRAL = 0;
    public static final int DISLIKED = -1;

    private WeightLabels() {
    }

    /**
     * Clamps weight to 1..10 (MIN_WEIGHT..MAX_WEIGHT).
     *
     * @param weight raw menu weight
     * @return progress value between 1 and 10
     */
    public static int toProgress(int weight) {
        return GameConfig.clampWeight(weight);
    }

    /**
     * Categorizes weight into LIKED (>5), NEUTRAL (==5), or DISLIKED (<5).
     *
     * @param weight raw menu weight
     * @return LIKED (1), NEUTRAL (0), or DISLIKED (-1)
     */
    public static int category(int weight) {
        int clamped = GameConfig.clampWeight(weight);
        if (clamped > GameConfig.DEFAULT_WEIGHT) {
            return LIKED;
        }
        if (clamped < GameConfig.DEFAULT_WEIGHT) {
            return DISLIKED;
        }
        return NEUTRAL;
    }

    /**
     * Returns true if all menu weights are equal to GameConfig.DEFAULT_WEIGHT (5),
     * or if the list is empty/null.
     *
     * @param weights list of menu weights
     * @return true if all weights are 5 or list is empty/null
     */
    public static boolean allDefault(List<Integer> weights) {
        if (weights == null || weights.isEmpty()) {
            return true;
        }
        for (Integer w : weights) {
            if (w == null || GameConfig.clampWeight(w) != GameConfig.DEFAULT_WEIGHT) {
                return false;
            }
        }
        return true;
    }
}
