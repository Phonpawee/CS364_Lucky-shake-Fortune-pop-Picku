package com.cs364.luckyshake.logic;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class WeightedDrawEngineTest {
    private static final int RUNS = 1000;
    /** Allowed difference between observed and expected share: ±5 percentage points. */
    private static final double TOLERANCE = 0.05;

    private final DrawEngine engine = new WeightedDrawEngine(new Random(42));

    private static final List<Long> NO_RECENT = Collections.emptyList();

    private static Map<Long, Integer> countPicks(DrawEngine e, List<Candidate> pool,
                                                 List<Long> recent, Long excludeId) {
        Map<Long, Integer> counts = new HashMap<>();
        for (Candidate c : pool) counts.put(c.id, 0);
        for (int i = 0; i < RUNS; i++) {
            Candidate c = excludeId == null ? e.pick(pool, recent) : e.pick(pool, recent, excludeId);
            counts.put(c.id, counts.get(c.id) + 1);
        }
        return counts;
    }

    private static void assertShare(String label, double expected, int count) {
        double actual = count / (double) RUNS;
        assertTrue(label + ": expected " + expected + " but was " + actual,
                Math.abs(actual - expected) <= TOLERANCE);
    }

    // ---------------------------------------------------------------- create

    @Test
    public void create_returnsWeightedEngine() {
        assertTrue(DrawEngine.create() instanceof WeightedDrawEngine);
    }

    // ------------------------------------------------------------------ pick

    @Test
    public void pick_1000Draws_matchWeights() {
        List<Candidate> pool = Arrays.asList(
                new Candidate(1, 1), new Candidate(2, 3), new Candidate(3, 6));
        Map<Long, Integer> counts = countPicks(engine, pool, NO_RECENT, null);
        assertShare("id 1", 0.1, counts.get(1L));
        assertShare("id 2", 0.3, counts.get(2L));
        assertShare("id 3", 0.6, counts.get(3L));
    }

    @Test
    public void pick_recentItems_getPenalty() {
        // Weights 5,5,5; ids 1 and 2 are recent -> 1.5, 1.5, 5 (total 8).
        List<Candidate> pool = Arrays.asList(
                new Candidate(1, 5), new Candidate(2, 5), new Candidate(3, 5));
        Map<Long, Integer> counts = countPicks(engine, pool, Arrays.asList(1L, 2L), null);
        assertShare("id 1", 1.5 / 8, counts.get(1L));
        assertShare("id 2", 1.5 / 8, counts.get(2L));
        assertShare("id 3", 5.0 / 8, counts.get(3L));
    }

    @Test
    public void pick_onlyLastRecentCountIdsArePenalized() {
        // recentIds is newest first; id 3 is the 3rd entry, older than RECENT_COUNT (2).
        assertEquals(2, GameConfig.RECENT_COUNT);
        List<Candidate> pool = Arrays.asList(
                new Candidate(1, 5), new Candidate(2, 5), new Candidate(3, 5));
        Map<Long, Integer> counts = countPicks(engine, pool, Arrays.asList(1L, 2L, 3L), null);
        assertShare("id 3 (not penalized)", 5.0 / 8, counts.get(3L));
    }

    @Test
    public void pick_recentItemChanceDropsByPenalty() {
        List<Candidate> pool = Arrays.asList(new Candidate(1, 5), new Candidate(2, 5));
        int fresh = countPicks(engine, pool, NO_RECENT, null).get(1L);
        int penalized = countPicks(engine, pool, Collections.singletonList(1L), null).get(1L);
        assertTrue(penalized < fresh);
        // 5×0.3 / (5×0.3 + 5) ≈ 0.23
        assertShare("penalized id 1", 1.5 / 6.5, penalized);
    }

    @Test
    public void pick_singleItemPool_returnsThatItem() {
        Candidate only = new Candidate(7, GameConfig.DEFAULT_WEIGHT);
        List<Candidate> pool = Collections.singletonList(only);
        assertSame(only, engine.pick(pool, Collections.singletonList(7L)));
        assertSame(only, engine.pick(pool, NO_RECENT, 7L));
    }

    @Test
    public void pick_nullRecent_isAllowed() {
        List<Candidate> pool = Arrays.asList(new Candidate(1, 5), new Candidate(2, 5));
        assertTrue(pool.contains(engine.pick(pool, null)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void pick_emptyPool_throws() {
        engine.pick(Collections.<Candidate>emptyList(), NO_RECENT);
    }

    @Test(expected = IllegalArgumentException.class)
    public void pick_nullPool_throws() {
        engine.pick(null, NO_RECENT);
    }

    // ------------------------------------------------------- pick (re-draw)

    @Test
    public void pickWithExclude_neverReturnsExcludedId() {
        List<Candidate> pool = Arrays.asList(
                new Candidate(1, 10), new Candidate(2, 1), new Candidate(3, 1));
        for (int i = 0; i < RUNS; i++) {
            assertNotEquals(1L, engine.pick(pool, Collections.singletonList(1L), 1L).id);
        }
    }

    @Test
    public void pickWithExclude_twoItems_alwaysReturnsTheOther() {
        List<Candidate> pool = Arrays.asList(new Candidate(1, 5), new Candidate(2, 5));
        for (int i = 0; i < 100; i++) {
            assertEquals(2L, engine.pick(pool, NO_RECENT, 1L).id);
        }
    }

    @Test
    public void pickWithExclude_restKeepsWeights() {
        List<Candidate> pool = Arrays.asList(
                new Candidate(1, 5), new Candidate(2, 2), new Candidate(3, 8));
        Map<Long, Integer> counts = countPicks(engine, pool, NO_RECENT, 1L);
        assertEquals(0, (int) counts.get(1L));
        assertShare("id 2", 0.2, counts.get(2L));
        assertShare("id 3", 0.8, counts.get(3L));
    }

    @Test
    public void pickWithExclude_nullExclude_behavesLikePick() {
        List<Candidate> pool = Arrays.asList(new Candidate(1, 5), new Candidate(2, 5));
        assertTrue(pool.contains(engine.pick(pool, NO_RECENT, null)));
    }

    // ------------------------------------------------------------- pickLevel

    @Test
    public void pickLevel_1000Draws_matchConfig() {
        int[] counts = new int[GameConfig.LEVEL_COUNT];
        for (int i = 0; i < RUNS; i++) {
            int level = engine.pickLevel();
            assertTrue(level >= 0 && level < GameConfig.LEVEL_COUNT);
            counts[level]++;
        }
        int[] weights = GameConfig.levelWeights();
        for (int i = 0; i < GameConfig.LEVEL_COUNT; i++) {
            assertShare("level " + i, weights[i] / 100.0, counts[i]);
        }
    }

    // ---------------------------------------------------------- weightedPick

    @Test
    public void weightedPick_1000Draws_matchWeights() {
        List<String> items = Arrays.asList("A", "B", "C", "D");
        List<Double> weights = Arrays.asList(1.0, 2.0, 3.0, 4.0);
        Map<String, Integer> counts = new HashMap<>();
        for (int i = 0; i < RUNS; i++) {
            counts.merge(engine.weightedPick(items, weights), 1, Integer::sum);
        }
        for (int i = 0; i < items.size(); i++) {
            assertShare(items.get(i), weights.get(i) / 10.0, counts.getOrDefault(items.get(i), 0));
        }
    }

    @Test
    public void weightedPick_zeroWeightItem_isNeverPicked() {
        List<String> items = Arrays.asList("never", "always");
        for (int i = 0; i < RUNS; i++) {
            assertEquals("always", engine.weightedPick(items, Arrays.asList(0.0, 1.0)));
        }
    }

    @Test
    public void weightedPick_allZeroWeights_picksFromList() {
        List<String> items = Arrays.asList("a", "b");
        assertTrue(items.contains(engine.weightedPick(items, Arrays.asList(0.0, 0.0))));
    }

    @Test
    public void weightedPick_singleItem_returnsIt() {
        assertEquals("x", engine.weightedPick(Collections.singletonList("x"),
                Collections.singletonList(3.0)));
    }

    @Test
    public void weightedPick_sameSeed_sameResults() {
        DrawEngine a = new WeightedDrawEngine(new Random(7));
        DrawEngine b = new WeightedDrawEngine(new Random(7));
        List<Integer> items = Arrays.asList(1, 2, 3, 4, 5);
        List<Double> weights = Arrays.asList(1.0, 1.0, 1.0, 1.0, 1.0);
        for (int i = 0; i < 50; i++) {
            assertEquals(a.weightedPick(items, weights), b.weightedPick(items, weights));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void weightedPick_emptyItems_throws() {
        engine.weightedPick(Collections.emptyList(), Collections.<Double>emptyList());
    }

    @Test(expected = IllegalArgumentException.class)
    public void weightedPick_sizeMismatch_throws() {
        engine.weightedPick(Arrays.asList("a", "b"), Collections.singletonList(1.0));
    }

    @Test(expected = IllegalArgumentException.class)
    public void weightedPick_negativeWeight_throws() {
        engine.weightedPick(Arrays.asList("a", "b"), Arrays.asList(1.0, -1.0));
    }

    @Test(expected = IllegalArgumentException.class)
    public void weightedPick_nanWeight_throws() {
        engine.weightedPick(Arrays.asList("a", "b"), Arrays.asList(1.0, Double.NaN));
    }
}
