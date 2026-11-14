package com.cs364.luckyshake.logic;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * The real DrawEngine (issue #3).
 *
 * <ul>
 *   <li>{@link #pick}: effective weight = candidate.weight × (id in the last
 *       {@link GameConfig#RECENT_COUNT} draws ? {@link GameConfig#RECENT_PENALTY} : 1),
 *       then a cumulative-sum weighted pick.</li>
 *   <li>{@link #pickLevel}: weighted pick over {@link GameConfig#levelWeights()}.</li>
 *   <li>{@link #weightedPick}: generic weighted pick for any list (group mode).</li>
 * </ul>
 *
 * Inject a seeded {@link Random} to get repeatable results in tests.
 */
public class WeightedDrawEngine implements DrawEngine {
    private final Random random;

    public WeightedDrawEngine() {
        this(new Random());
    }

    /** Pass a seeded Random to get the same results every run (tests). */
    public WeightedDrawEngine(Random random) {
        if (random == null) {
            throw new IllegalArgumentException("random must not be null");
        }
        this.random = random;
    }

    @Override
    public Candidate pick(List<Candidate> pool, List<Long> recentIds) {
        if (pool == null || pool.isEmpty()) {
            throw new IllegalArgumentException("pool must not be empty");
        }
        if (pool.size() == 1) {
            return pool.get(0);
        }

        Set<Long> recent = new HashSet<>();
        if (recentIds != null) {
            int n = Math.min(GameConfig.RECENT_COUNT, recentIds.size());
            for (int i = 0; i < n; i++) {
                Long id = recentIds.get(i);
                if (id != null) recent.add(id);
            }
        }

        List<Double> weights = new ArrayList<>(pool.size());
        for (Candidate c : pool) {
            double w = Math.max(0, c.weight);
            if (recent.contains(c.id)) {
                w *= GameConfig.RECENT_PENALTY;
            }
            weights.add(w);
        }
        return weightedPick(pool, weights);
    }

    @Override
    public int pickLevel() {
        int[] levelWeights = GameConfig.levelWeights();
        List<Integer> levels = new ArrayList<>(levelWeights.length);
        List<Double> weights = new ArrayList<>(levelWeights.length);
        for (int i = 0; i < levelWeights.length; i++) {
            levels.add(i);
            weights.add((double) levelWeights[i]);
        }
        return weightedPick(levels, weights);
    }

    /**
     * Cumulative-sum weighted pick. Items with weight 0 are never chosen, unless every
     * weight is 0, in which case every item has the same chance.
     *
     * @throws IllegalArgumentException if the lists are null/empty, have different sizes,
     *                                  or a weight is null, negative, NaN or infinite
     */
    @Override
    public <T> T weightedPick(List<T> items, List<Double> weights) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("items must not be empty");
        }
        if (weights == null || weights.size() != items.size()) {
            throw new IllegalArgumentException("weights must have the same size as items");
        }

        double total = 0;
        for (Double w : weights) {
            if (w == null || w.isNaN() || w.isInfinite() || w < 0) {
                throw new IllegalArgumentException("weights must be finite and non-negative: " + w);
            }
            total += w;
        }
        if (items.size() == 1) {
            return items.get(0);
        }
        if (total <= 0) {
            return items.get(random.nextInt(items.size()));
        }

        double r = random.nextDouble() * total; // [0, total)
        double cumulative = 0;
        int lastPositive = -1;
        for (int i = 0; i < items.size(); i++) {
            double w = weights.get(i);
            if (w <= 0) continue;
            lastPositive = i;
            cumulative += w;
            if (r < cumulative) {
                return items.get(i);
            }
        }
        // Only reached through floating-point rounding; fall back to the last weighted item.
        return items.get(lastPositive);
    }
}
