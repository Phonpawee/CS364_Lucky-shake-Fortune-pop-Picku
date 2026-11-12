package com.cs364.luckyshake.logic;

/**
 * Every game number lives here, so the rest of the app never hard-codes one.
 * Defaults come from the web demo; change them only after the team agrees.
 */
public final class GameConfig {

    private GameConfig() {
    }

    // ------------------------------------------------------------ re-draw
    /** How many times the user may ask for a new menu per draw. */
    public static final int MAX_REDRAWS = 3;

    // ------------------------------------------------------- fortune level
    // Level ids returned by DrawEngine.pickLevel(). They are also indexes into LEVEL_WEIGHTS.
    public static final int LEVEL_GREAT = 0;    // ดีมาก
    public static final int LEVEL_GOOD = 1;     // ดี
    public static final int LEVEL_NEUTRAL = 2;  // กลาง
    public static final int LEVEL_CAUTION = 3;  // ระวัง
    public static final int LEVEL_COUNT = 4;

    /** Chance (in percent, sums to 100) of each level, indexed by LEVEL_*. */
    private static final int[] LEVEL_WEIGHTS = {20, 40, 30, 10};

    /** Returns a copy of the level weights so callers cannot change the config. */
    public static int[] levelWeights() {
        return LEVEL_WEIGHTS.clone();
    }

    // -------------------------------------------------------- menu weight
    public static final int MIN_WEIGHT = 1;
    public static final int DEFAULT_WEIGHT = 5;
    public static final int MAX_WEIGHT = 10;

    // ----------------------------------------------------- recent penalty
    /** A menu drawn in the last RECENT_COUNT draws has its weight multiplied by this. */
    public static final double RECENT_PENALTY = 0.3;
    /** How many recent draws are remembered for the penalty. */
    public static final int RECENT_COUNT = 2;

    // -------------------------------------------------------------- shake
    /** Minimum time between two shakes that both fire the callback. */
    public static final long SHAKE_DEBOUNCE_MS = 1000L;

    public static final int MIN_SENSITIVITY = 1;
    public static final int DEFAULT_SENSITIVITY = 5;
    public static final int MAX_SENSITIVITY = 10;

    /** Shake threshold in m/s² = SHAKE_THRESHOLD_BASE - SHAKE_THRESHOLD_STEP × sensitivity. */
    public static final double SHAKE_THRESHOLD_BASE = 22.0;
    public static final double SHAKE_THRESHOLD_STEP = 1.6;

    /** Clamps a sensitivity value to MIN_SENSITIVITY..MAX_SENSITIVITY. */
    public static int clampSensitivity(int sensitivity) {
        return Math.max(MIN_SENSITIVITY, Math.min(MAX_SENSITIVITY, sensitivity));
    }

    /** Clamps a menu weight to MIN_WEIGHT..MAX_WEIGHT. */
    public static int clampWeight(int weight) {
        return Math.max(MIN_WEIGHT, Math.min(MAX_WEIGHT, weight));
    }

    // -------------------------------------------------------------- group
    public static final int MIN_GROUP_PLAYERS = 2;
    public static final int MAX_GROUP_PLAYERS = 8;
}