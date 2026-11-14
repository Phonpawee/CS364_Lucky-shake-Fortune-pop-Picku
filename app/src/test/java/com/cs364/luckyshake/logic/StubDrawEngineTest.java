package com.cs364.luckyshake.logic;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class StubDrawEngineTest {
    private final DrawEngine engine = new StubDrawEngine(new Random(42));

    @Test
    public void create_returnsAnEngine() {
        assertTrue(DrawEngine.create() != null);
    }

    @Test
    public void pick_singleItemPool_returnsThatItem() {
        Candidate only = new Candidate(7, GameConfig.DEFAULT_WEIGHT);
        assertEquals(only, engine.pick(Collections.singletonList(only), Collections.<Long>emptyList()));
    }

    @Test
    public void pick_returnsItemFromPool() {
        List<Candidate> pool = Arrays.asList(new Candidate(1, 5), new Candidate(2, 5), new Candidate(3, 5));
        for (int i = 0; i < 100; i++) {
            assertTrue(pool.contains(engine.pick(pool, Arrays.asList(1L, 2L))));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void pick_emptyPool_throws() {
        engine.pick(Collections.<Candidate>emptyList(), Collections.<Long>emptyList());
    }

    @Test
    public void pickLevel_isAValidLevel() {
        for (int i = 0; i < 100; i++) {
            int level = engine.pickLevel();
            assertTrue(level >= GameConfig.LEVEL_GREAT && level <= GameConfig.LEVEL_CAUTION);
        }
    }

    @Test
    public void weightedPick_returnsItemFromList() {
        List<String> items = Arrays.asList("a", "b");
        assertTrue(items.contains(engine.weightedPick(items, Arrays.asList(1.0, 2.0))));
    }

    @Test(expected = IllegalArgumentException.class)
    public void weightedPick_sizeMismatch_throws() {
        engine.weightedPick(Arrays.asList("a", "b"), Collections.singletonList(1.0));
    }
}