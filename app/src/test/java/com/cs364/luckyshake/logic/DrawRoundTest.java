package com.cs364.luckyshake.logic;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class DrawRoundTest {
    private static final List<Long> NO_RECENT = Collections.emptyList();
    private static final List<Candidate> FIVE = Arrays.asList(
            new Candidate(1, 5), new Candidate(2, 5), new Candidate(3, 5),
            new Candidate(4, 5), new Candidate(5, 5));

    private DrawRound newRound(int maxRedraws) {
        return new DrawRound(new WeightedDrawEngine(new Random(42)), new RedrawQuota(maxRedraws));
    }

    // ------------------------------------------------------------------ draw

    @Test
    public void draw_enoughItems_isOkWithResultAndLevel() {
        DrawResult r = newRound(3).draw(5, FIVE, NO_RECENT);
        assertEquals(DrawStatus.OK, r.status);
        assertTrue(r.hasResult());
        assertTrue(FIVE.contains(r.candidate));
        assertTrue(r.level >= 0 && r.level < GameConfig.LEVEL_COUNT);
        assertEquals(3, r.redrawsLeft);
    }

    @Test
    public void draw_emptySet_isSetEmpty() {
        DrawResult r = newRound(3).draw(0, Collections.emptyList(), NO_RECENT);
        assertEquals(DrawStatus.SET_EMPTY, r.status);
        assertFalse(r.hasResult());
        assertNull(r.candidate);
        assertEquals(DrawResult.NO_LEVEL, r.level);
    }

    @Test
    public void draw_nullPool_doesNotCrash() {
        assertEquals(DrawStatus.SET_EMPTY, newRound(3).draw(0, null, null).status);
        assertEquals(DrawStatus.NO_MATCH, newRound(3).draw(4, null, null).status);
    }

    @Test
    public void draw_nothingPassesFilter_isNoMatch() {
        DrawResult r = newRound(3).draw(5, Collections.emptyList(), NO_RECENT);
        assertEquals(DrawStatus.NO_MATCH, r.status);
        assertFalse(r.hasResult());
    }

    @Test
    public void draw_oneOrTwoItems_isFewItemsWithResult() {
        List<Candidate> one = Collections.singletonList(new Candidate(7, 5));
        DrawResult r1 = newRound(3).draw(5, one, NO_RECENT);
        assertEquals(DrawStatus.FEW_ITEMS, r1.status);
        assertEquals(7L, r1.candidate.id);

        List<Candidate> two = FIVE.subList(0, 2);
        DrawResult r2 = newRound(3).draw(5, two, NO_RECENT);
        assertEquals(DrawStatus.FEW_ITEMS, r2.status);
        assertTrue(r2.hasResult());
    }

    @Test
    public void draw_threeItems_isOk() {
        assertEquals(DrawStatus.OK, newRound(3).draw(3, FIVE.subList(0, 3), NO_RECENT).status);
    }

    @Test
    public void draw_poolWithNulls_ignoresThem() {
        List<Candidate> pool = new ArrayList<>(Arrays.asList(null, new Candidate(9, 5), null));
        DrawResult r = newRound(3).draw(3, pool, NO_RECENT);
        assertEquals(DrawStatus.FEW_ITEMS, r.status);
        assertEquals(9L, r.candidate.id);
    }

    // ---------------------------------------------------------------- redraw

    @Test
    public void redraw_consumesQuotaAndNeverRepeatsCurrent() {
        DrawRound round = newRound(3);
        Candidate previous = round.draw(5, FIVE, NO_RECENT).candidate;
        for (int left = 2; left >= 0; left--) {
            DrawResult r = round.redraw(5, FIVE, NO_RECENT);
            assertEquals(DrawStatus.OK, r.status);
            assertEquals(left, r.redrawsLeft);
            assertNotEquals(previous.id, r.candidate.id);
            previous = r.candidate;
        }
    }

    /** FEW_ITEMS นับจากจำนวนเมนูที่ผ่านตัวกรอง ไม่ใช่ตัวเลือกที่เหลือหลังตัดเมนูเดิม จึงไม่เปลี่ยนไปมาระหว่างรอบ */
    @Test
    public void redraw_exactlyThreeItems_staysOk() {
        DrawRound round = newRound(3);
        List<Candidate> three = FIVE.subList(0, 3);
        assertEquals(DrawStatus.OK, round.draw(3, three, NO_RECENT).status);
        for (int i = 0; i < 3; i++) {
            DrawResult r = round.redraw(3, three, NO_RECENT);
            assertEquals(DrawStatus.OK, r.status);
        }
    }

    @Test
    public void redraw_whenQuotaEmpty_returnsQuotaEmptyWithSameResult() {
        DrawRound round = newRound(1);
        round.draw(5, FIVE, NO_RECENT);
        DrawResult last = round.redraw(5, FIVE, NO_RECENT);

        DrawResult r = round.redraw(5, FIVE, NO_RECENT);
        assertEquals(DrawStatus.QUOTA_EMPTY, r.status);
        assertSame(last.candidate, r.candidate);
        assertEquals(last.level, r.level);
        assertEquals(0, r.redrawsLeft);
        assertTrue(r.hasResult());
    }

    @Test
    public void redraw_beforeAnyDraw_actsAsDrawWithoutUsingQuota() {
        DrawRound round = newRound(3);
        DrawResult r = round.redraw(5, FIVE, NO_RECENT);
        assertEquals(DrawStatus.OK, r.status);
        assertEquals(3, r.redrawsLeft);
    }

    @Test
    public void redraw_emptyPool_keepsCurrentAndQuota() {
        DrawRound round = newRound(3);
        Candidate shown = round.draw(5, FIVE, NO_RECENT).candidate;
        DrawResult r = round.redraw(5, Collections.emptyList(), NO_RECENT);
        assertEquals(DrawStatus.NO_MATCH, r.status);
        assertEquals(3, r.redrawsLeft);
        assertSame(shown, round.current());
    }

    @Test
    public void redraw_singleItem_isFewItemsAndReturnsIt() {
        DrawRound round = newRound(3);
        List<Candidate> one = Collections.singletonList(new Candidate(7, 5));
        round.draw(1, one, NO_RECENT);
        DrawResult r = round.redraw(1, one, NO_RECENT);
        assertEquals(DrawStatus.FEW_ITEMS, r.status);
        assertEquals(7L, r.candidate.id);
        assertEquals(2, r.redrawsLeft);
    }

    // ------------------------------------------------------- accept / reset

    @Test
    public void accept_returnsCurrentAndResetsQuota() {
        DrawRound round = newRound(3);
        round.draw(5, FIVE, NO_RECENT);
        Candidate shown = round.redraw(5, FIVE, NO_RECENT).candidate;
        assertEquals(2, round.redrawsLeft());

        assertSame(shown, round.accept());
        assertEquals(3, round.redrawsLeft());
        assertNull(round.current());
        assertEquals(DrawResult.NO_LEVEL, round.currentLevel());
    }

    @Test
    public void accept_withoutResult_returnsNull() {
        assertNull(newRound(3).accept());
    }

    @Test
    public void draw_newRound_resetsQuota() {
        DrawRound round = newRound(2);
        round.draw(5, FIVE, NO_RECENT);
        round.redraw(5, FIVE, NO_RECENT);
        round.redraw(5, FIVE, NO_RECENT);
        assertEquals(0, round.redrawsLeft());

        assertEquals(2, round.draw(5, FIVE, NO_RECENT).redrawsLeft);
    }

    @Test
    public void setMaxRedraws_appliesToCurrentRound() {
        DrawRound round = newRound(3);
        round.draw(5, FIVE, NO_RECENT);
        round.setMaxRedraws(5);
        assertEquals(5, round.redrawsLeft());
    }

    // ---------------------------------------------------------------- status

    @Test
    public void status_hasResult_matchesDrawResult() {
        assertTrue(DrawStatus.OK.hasResult());
        assertTrue(DrawStatus.FEW_ITEMS.hasResult());
        assertTrue(DrawStatus.QUOTA_EMPTY.hasResult());
        assertFalse(DrawStatus.NO_MATCH.hasResult());
        assertFalse(DrawStatus.SET_EMPTY.hasResult());
    }
}
