package com.cs364.luckyshake.ui.home;

import com.cs364.luckyshake.data.Menu;
import com.cs364.luckyshake.data.MenuFilter;
import com.cs364.luckyshake.logic.DrawRound;
import com.cs364.luckyshake.logic.DrawStatus;
import com.cs364.luckyshake.logic.GameConfig;
import com.cs364.luckyshake.logic.RedrawQuota;
import com.cs364.luckyshake.logic.WeightedDrawEngine;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class HomeSessionTest {
    private static final long RICE = 1;
    private static final long EMPTY = 2;

    /** ชุดเมนูปลอม: กรองเฉพาะ noSpicy เพื่อให้ทดสอบ NO_MATCH ได้ */
    private final Map<Long, List<Menu>> sets = new HashMap<>();

    private final HomeSession.MenuSource source = (setId, filter) -> {
        List<Menu> out = new ArrayList<>();
        for (Menu m : sets.getOrDefault(setId, new ArrayList<>())) {
            if (filter != null && filter.noSpicy && m.hasTag(Menu.TAG_SPICY)) continue;
            out.add(m);
        }
        return out;
    };

    private static Menu menu(long id, int tags) {
        Menu m = new Menu();
        m.id = id;
        m.setId = RICE;
        m.nameTh = "เมนู" + id;
        m.tags = tags;
        m.weight = GameConfig.DEFAULT_WEIGHT;
        return m;
    }

    private HomeSession newSession(int maxRedraws) {
        List<Menu> rice = new ArrayList<>();
        for (long id = 10; id <= 14; id++) rice.add(menu(id, Menu.TAG_SPICY));
        sets.put(RICE, rice);
        sets.put(EMPTY, new ArrayList<>());
        HomeSession s = new HomeSession(
                new DrawRound(new WeightedDrawEngine(new Random(42)), new RedrawQuota(maxRedraws)),
                source);
        s.setSetId(RICE);
        return s;
    }

    // ----------------------------------------------------------------- state

    @Test
    public void initialState_isIdle() {
        DrawState s = newSession(3).getState();
        assertSame(DrawState.IDLE, s);
        assertNull(s.status);
        assertFalse(s.hasResult());
        assertFalse(s.canRedraw());
    }

    @Test
    public void draw_ok_hasMenuLevelStickAndQuota() {
        DrawState s = newSession(3).draw();
        assertEquals(DrawStatus.OK, s.status);
        assertNotNull(s.menu);
        assertTrue(s.level >= 0 && s.level < GameConfig.LEVEL_COUNT);
        assertEquals(s.menu.id - 9, s.stickNo); // id 10..14 → ไม้ 1..5
        assertEquals(3, s.redrawsLeft);
        assertTrue(s.canRedraw());
    }

    @Test
    public void draw_noSetSelected_isSetEmpty() {
        HomeSession s = newSession(3);
        s.setSetId(HomeSession.NO_SET);
        assertEquals(DrawStatus.SET_EMPTY, s.draw().status);
    }

    @Test
    public void draw_emptySet_isSetEmpty() {
        HomeSession s = newSession(3);
        s.setSetId(EMPTY);
        DrawState r = s.draw();
        assertEquals(DrawStatus.SET_EMPTY, r.status);
        assertFalse(r.hasResult());
    }

    @Test
    public void draw_filterRemovesAll_isNoMatch() {
        HomeSession s = newSession(3);
        s.setFilter(new MenuFilter(null, false, true));
        assertEquals(DrawStatus.NO_MATCH, s.draw().status);
    }

    @Test
    public void draw_afterResult_emptySet_clearsOldMenu() {
        HomeSession s = newSession(3);
        s.draw();
        s.setSetId(EMPTY);
        assertFalse(s.draw().hasResult());
    }

    // ---------------------------------------------------------------- redraw

    @Test
    public void redraw_usesQuotaThenQuotaEmptyKeepsMenu() {
        HomeSession s = newSession(2);
        DrawState shown = s.draw();
        for (int left = 1; left >= 0; left--) {
            DrawState r = s.redraw();
            assertEquals(left, r.redrawsLeft);
            assertNotEquals(shown.menu.id, r.menu.id);
            shown = r;
        }
        assertFalse(shown.canRedraw());

        DrawState empty = s.redraw();
        assertEquals(DrawStatus.QUOTA_EMPTY, empty.status);
        assertSame(shown.menu, empty.menu);
        assertEquals(shown.level, empty.level);
        assertEquals(shown.stickNo, empty.stickNo);
    }

    @Test
    public void redraw_filterRemovesAll_keepsShownMenu() {
        HomeSession s = newSession(3);
        DrawState shown = s.draw();
        s.setFilter(new MenuFilter(null, false, true));
        DrawState r = s.redraw();
        assertEquals(DrawStatus.NO_MATCH, r.status);
        assertSame(shown.menu, r.menu);
        assertEquals(3, r.redrawsLeft);
    }

    @Test
    public void recentIds_newestFirst_cappedAtRecentCount() {
        HomeSession s = newSession(5);
        List<Long> shown = new ArrayList<>();
        shown.add(0, s.draw().menu.id);
        for (int i = 0; i < 3; i++) shown.add(0, s.redraw().menu.id);
        assertEquals(shown.subList(0, GameConfig.RECENT_COUNT), s.getRecentIds());
    }

    @Test
    public void recentIds_keepRepeats_soOlderIdLeavesWindow() {
        HomeSession s = newSession(3);
        sets.put(3L, new ArrayList<>(List.of(menu(30, 0))));
        sets.put(4L, new ArrayList<>(List.of(menu(40, 0))));

        s.setSetId(3);
        s.draw();           // A
        s.setSetId(4);
        s.draw();           // B
        s.draw();           // B
        assertEquals(List.of(40L, 40L), s.getRecentIds());
    }

    // ---------------------------------------------------------------- accept

    @Test
    public void accept_keepsMenuMarksAcceptedAndBlocksRedraw() {
        HomeSession s = newSession(3);
        DrawState shown = s.redraw();
        assertSame(shown.menu, s.accept());
        DrawState after = s.getState();
        assertTrue(after.accepted);
        assertSame(shown.menu, after.menu);
        assertFalse(after.canRedraw());
        assertNull(s.accept()); // ยอมรับซ้ำไม่ได้
    }

    @Test
    public void accept_beforeDraw_returnsNull() {
        assertNull(newSession(3).accept());
    }

    @Test
    public void afterAccept_newDrawHasFullQuota() {
        HomeSession s = newSession(3);
        s.draw();
        s.redraw();
        s.accept();
        DrawState next = s.draw();
        assertFalse(next.accepted);
        assertEquals(3, next.redrawsLeft);
    }

    @Test
    public void afterAccept_emptyRedraw_doesNotShowAcceptedMenu() {
        HomeSession s = newSession(3);
        s.draw();
        s.accept();
        s.setSetId(EMPTY);
        assertFalse(s.redraw().hasResult());
    }

    // -------------------------------------------------------------- settings

    @Test
    public void filter_isCopiedBothWays() {
        HomeSession s = newSession(3);
        MenuFilter f = new MenuFilter(Menu.TYPE_RICE, true, false);
        s.setFilter(f);
        f.noSpicy = true;
        assertFalse(s.getFilter().noSpicy);
        s.getFilter().noFried = false;
        assertTrue(s.getFilter().noFried);
        s.setFilter(null);
        assertTrue(s.getFilter().isEmpty());
    }

    @Test
    public void setMaxRedraws_updatesShownQuota() {
        HomeSession s = newSession(3);
        s.draw();
        s.setMaxRedraws(5);
        assertEquals(5, s.getState().redrawsLeft);
    }
}
