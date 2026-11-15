package com.cs364.luckyshake.logic;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static com.cs364.luckyshake.logic.GameConfig.DEFAULT_WEIGHT;
import static com.cs364.luckyshake.logic.GameConfig.MAX_WEIGHT;
import static com.cs364.luckyshake.logic.GameConfig.MIN_WEIGHT;
import static com.cs364.luckyshake.logic.GameConfig.WEIGHT_DECAY_PERIOD_MS;
import static com.cs364.luckyshake.logic.WeightPolicy.Event.ACCEPT;
import static com.cs364.luckyshake.logic.WeightPolicy.Event.REDRAW;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class WeightPolicyTest {
    private static final long WEEK = WEIGHT_DECAY_PERIOD_MS;
    private static final long T0 = 1_700_000_000_000L;

    // ------------------------------------------------------------- +1 / -1

    @Test
    public void accept_addsOne() {
        assertEquals(6, WeightPolicy.apply(5, ACCEPT));
        assertEquals(2, WeightPolicy.apply(1, ACCEPT));
    }

    @Test
    public void redraw_subtractsOne() {
        assertEquals(4, WeightPolicy.apply(5, REDRAW));
        assertEquals(9, WeightPolicy.apply(10, REDRAW));
    }

    @Test
    public void nullEvent_onlyClamps() {
        assertEquals(5, WeightPolicy.apply(5, null));
        assertEquals(MAX_WEIGHT, WeightPolicy.apply(99, null));
    }

    // -------------------------------------------------------------- bounds

    @Test
    public void accept_atMax_staysAtMax() {
        assertEquals(MAX_WEIGHT, WeightPolicy.apply(MAX_WEIGHT, ACCEPT));
    }

    @Test
    public void redraw_atMin_staysAtMin_soMenuNeverDisappears() {
        assertEquals(MIN_WEIGHT, WeightPolicy.apply(MIN_WEIGHT, REDRAW));
        assertTrue(true);
    }

    @Test
    public void outOfRangeInput_isClampedFirst() {
        assertEquals(MIN_WEIGHT + 1, WeightPolicy.apply(-50, ACCEPT));
        assertEquals(MAX_WEIGHT - 1, WeightPolicy.apply(50, REDRAW));
    }

    @Test
    public void anySequenceOfEvents_staysWithin1To10() {
        Random r = new Random(7);
        int w = DEFAULT_WEIGHT;
        for (int i = 0; i < 1000; i++) {
            w = WeightPolicy.apply(w, r.nextBoolean() ? ACCEPT : REDRAW);
            assertTrue(w >= MIN_WEIGHT && w <= MAX_WEIGHT);
        }
    }

    // --------------------------------------------------------------- decay

    @Test
    public void decay_lessThanAWeek_noChange() {
        assertEquals(8, WeightPolicy.decay(8, T0, T0 + WEEK - 1));
        assertEquals(2, WeightPolicy.decay(2, T0, T0));
    }

    @Test
    public void decay_oneWeek_movesOneStepTowardDefault() {
        assertEquals(7, WeightPolicy.decay(8, T0, T0 + WEEK));
        assertEquals(3, WeightPolicy.decay(2, T0, T0 + WEEK));
        assertEquals(DEFAULT_WEIGHT, WeightPolicy.decay(DEFAULT_WEIGHT, T0, T0 + WEEK));
    }

    @Test
    public void decay_severalWeeks_movesOneStepPerWeek() {
        assertEquals(7, WeightPolicy.decay(10, T0, T0 + 3 * WEEK));
        assertEquals(4, WeightPolicy.decay(1, T0, T0 + 3 * WEEK + WEEK / 2));
    }

    @Test
    public void decay_neverOvershootsDefault() {
        assertEquals(DEFAULT_WEIGHT, WeightPolicy.decay(MAX_WEIGHT, T0, T0 + 100 * WEEK));
        assertEquals(DEFAULT_WEIGHT, WeightPolicy.decay(MIN_WEIGHT, T0, T0 + 100 * WEEK));
    }

    @Test
    public void decay_clockGoingBackwards_noChange() {
        assertEquals(9, WeightPolicy.decay(9, T0, T0 - 5 * WEEK));
        assertEquals(T0, WeightPolicy.decayedLastTouched(T0, T0 - 5 * WEEK));
    }

    @Test
    public void decayedLastTouched_movesByWholeWeeksOnly() {
        assertEquals(T0, WeightPolicy.decayedLastTouched(T0, T0 + WEEK - 1));
        assertEquals(T0 + 2 * WEEK, WeightPolicy.decayedLastTouched(T0, T0 + 2 * WEEK + 123));
    }

    /** การคืนค่าทีละสัปดาห์ (อ่านทุกสัปดาห์) ต้องได้ผลเท่ากับคำนวณรวดเดียว */
    @Test
    public void decay_readEveryWeek_sameAsReadOnce() {
        long touched = T0;
        int w = MAX_WEIGHT;
        long now = T0;
        for (int i = 0; i < 3; i++) {
            now += WEEK + WEEK / 3;
            w = WeightPolicy.decay(w, touched, now);
            touched = WeightPolicy.decayedLastTouched(touched, now);
        }
        assertEquals(WeightPolicy.decay(MAX_WEIGHT, T0, now), w);
    }

    // ---------------------------------------------- effect on draw chance

    /** ขอใหม่กับเมนูเดิมหลายครั้ง แล้วโอกาสที่เมนูนั้นออกต้องลดลงเรื่อย ๆ จนถึงขั้นต่ำแต่ไม่เป็นศูนย์ */
    @Test
    public void repeatedRedraws_lowerTheChanceOfThatMenu() {
        WeightedDrawEngine engine = new WeightedDrawEngine(new Random(1));
        int w = DEFAULT_WEIGHT;
        double previous = share(engine, w);
        for (int i = 0; i < DEFAULT_WEIGHT - MIN_WEIGHT; i++) {
            w = WeightPolicy.apply(w, REDRAW);
            double now = share(engine, w);
            assertTrue("chance should drop: " + previous + " -> " + now, now < previous);
            previous = now;
        }
        assertEquals(MIN_WEIGHT, w);
        assertTrue(previous > 0);
        // ถึงขั้นต่ำแล้ว ขอใหม่อีกก็ไม่ลดต่อ
        assertEquals(MIN_WEIGHT, WeightPolicy.apply(w, REDRAW));
    }

    /** สัดส่วนที่เมนู id 1 (น้ำหนัก w) ออก เทียบกับอีก 3 เมนูน้ำหนักเริ่มต้น */
    private static double share(WeightedDrawEngine engine, int w) {
        List<Candidate> pool = new ArrayList<>(Arrays.asList(
                new Candidate(1, w),
                new Candidate(2, DEFAULT_WEIGHT),
                new Candidate(3, DEFAULT_WEIGHT),
                new Candidate(4, DEFAULT_WEIGHT)));
        int n = 20_000;
        int hits = 0;
        for (int i = 0; i < n; i++) {
            if (engine.pick(pool, null).id == 1) hits++;
        }
        return hits / (double) n;
    }
}
