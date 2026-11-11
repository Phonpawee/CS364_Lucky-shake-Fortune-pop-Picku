package com.cs364.luckyshake.logic;

import java.util.List;
import java.util.Random;

/**
 * Temporary DrawEngine so the UI can be built before issue #3 is done.
 * It picks uniformly at random and ignores weights and the recent penalty.
 * It checks its arguments the same way the real engine will.
 */
public class StubDrawEngine implements DrawEngine {
    private final Random random;

    public StubDrawEngine() {
        this(new Random());
    }

    /** Pass a seeded Random to get the same results every run (tests). */
    public StubDrawEngine(Random random) {
        this.random = random;
    }

    @Override
    public Candidate pick(List<Candidate> pool, List<Long> recentIds) {
        if (pool == null || pool.isEmpty()) {
            throw new IllegalArgumentException("pool must not be empty");
        }
        return pool.get(random.nextInt(pool.size()));
    }

    @Override
    public int pickLevel() {
        return random.nextInt(GameConfig.LEVEL_COUNT);
    }

    @Override
    public <T> T weightedPick(List<T> items, List<Double> weights) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("items must not be empty");
        }
        if (weights == null || weights.size() != items.size()) {
            throw new IllegalArgumentException("weights must have the same size as items");
        }
        return items.get(random.nextInt(items.size()));
    }
}