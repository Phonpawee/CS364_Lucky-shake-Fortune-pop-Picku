package com.cs364.luckyshake.ui.home;

import com.cs364.luckyshake.data.HistoryEntry;
import com.cs364.luckyshake.data.Menu;
import com.cs364.luckyshake.data.MenuFilter;
import com.cs364.luckyshake.logic.DrawRound;
import com.cs364.luckyshake.logic.DrawStatus;
import com.cs364.luckyshake.logic.GameConfig;
import com.cs364.luckyshake.logic.RedrawQuota;
import com.cs364.luckyshake.logic.WeightPolicy;
import com.cs364.luckyshake.logic.WeightedDrawEngine;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
        for (Menu m : Objects.requireNonNull(sets.getOrDefault(setId, new ArrayList<>()))) {
            if (filter != null && filter.noSpicy && m.hasTag(Menu.TAG_SPICY)) continue;
            out.add(m);
        }
        return out;
    };

    /** น้ำหนักที่ถูกบันทึก ในรูป "id:EVENT" ตามลำดับ */
    private final List<String> recorded = new ArrayList<>();

    private final HomeSession.WeightRecorder recorder =
            (menuId, event) -> recorded.add(menuId + ":" + event);

    /** ประวัติที่ถูกบันทึก ตามลำดับ */
    private final List<HistoryEntry> history = new ArrayList<>();

    private static Menu menu(long id, int tags) {
        Menu m = new Menu();
        m.id = id;
        m.setId = RICE;
        m.nameTh = "เมนู" + id;
        m.nameEn = "Menu " + id;
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
                source, recorder, history::add);
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
            assertNotNull(shown.menu);
            assertNotNull(r.menu);
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
        shown.add(0, Objects.requireNonNull(s.draw().menu).id);
        for (int i = 0; i < 3; i++) shown.add(0, Objects.requireNonNull(s.redraw().menu).id);
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

    // --------------------------------------------------------------- weights

    @Test
    public void accept_recordsPlusOneForAcceptedMenu() {
        HomeSession s = newSession(3);
        Menu shown = s.draw().menu;
        s.accept();
        assertNotNull(shown);
        assertEquals(List.of(shown.id + ":" + WeightPolicy.Event.ACCEPT), recorded);
        s.accept(); // ยอมรับซ้ำไม่บันทึกซ้ำ
        assertEquals(1, recorded.size());
    }

    @Test
    public void redraw_recordsMinusOneForRejectedMenu() {
        HomeSession s = newSession(3);
        Menu first = s.draw().menu;
        Menu second = s.redraw().menu;
        s.redraw();
        assertNotNull(first);
        assertNotNull(second);
        assertEquals(List.of(first.id + ":" + WeightPolicy.Event.REDRAW,
                second.id + ":" + WeightPolicy.Event.REDRAW), recorded);
    }

    @Test
    public void draw_recordsNothing() {
        HomeSession s = newSession(3);
        s.draw();
        s.draw();
        assertTrue(recorded.isEmpty());
    }

    @Test
    public void redraw_quotaEmpty_recordsNothing() {
        HomeSession s = newSession(1);
        s.draw();
        s.redraw();
        recorded.clear();
        assertEquals(DrawStatus.QUOTA_EMPTY, s.redraw().status);
        assertTrue(recorded.isEmpty());
    }

    @Test
    public void redraw_noMatch_recordsNothing() {
        HomeSession s = newSession(3);
        s.draw();
        s.setFilter(new MenuFilter(null, false, true));
        assertEquals(DrawStatus.NO_MATCH, s.redraw().status);
        assertTrue(recorded.isEmpty());
    }

    @Test
    public void redraw_afterNoMatch_stillPenalisesMenuOnScreen() {
        HomeSession s = newSession(3);
        Menu shown = s.draw().menu;
        s.setFilter(new MenuFilter(null, false, true));
        s.redraw();                 // NO_MATCH: เมนูเดิมยังแสดงอยู่
        s.setFilter(null);
        s.redraw();                 // ได้เมนูใหม่จริง → เมนูเดิมถูกปฏิเสธ
        assertNotNull(shown);
        assertEquals(List.of(shown.id + ":" + WeightPolicy.Event.REDRAW), recorded);
    }

    @Test
    public void redraw_afterAccept_startsNewRound_recordsOnlyTheAccept() {
        HomeSession s = newSession(3);
        Menu shown = s.draw().menu;
        s.accept();
        s.redraw();
        assertNotNull(shown);
        assertEquals(List.of(shown.id + ":" + WeightPolicy.Event.ACCEPT), recorded);
    }

    // --------------------------------------------------------------- history

    private static void assertEntry(DrawState shown, String status, HistoryEntry e) {
        assertNotNull(shown.menu);
        assertEquals(Long.valueOf(shown.menu.id), e.menuId);
        assertEquals(shown.menu.nameTh, e.nameTh);
        assertEquals(shown.menu.nameEn, e.nameEn);
        assertEquals(GameConfig.LEVEL_COUNT - shown.level, e.level);
        assertEquals(shown.stickNo, e.stickNo);
        assertEquals(status, e.status);
        assertEquals(HistoryEntry.MODE_SOLO, e.mode);
        assertTrue(e.createdAt > 0);
    }

    @Test
    public void redrawOnce_acceptOnce_recordsTwoMatchingEntries() {
        HomeSession s = newSession(3);
        DrawState first = s.draw();
        DrawState second = s.redraw();
        s.accept();
        assertEquals(2, history.size());
        assertEntry(first, HistoryEntry.STATUS_REDRAWN, history.get(0));
        assertEntry(second, HistoryEntry.STATUS_ACCEPTED, history.get(1));
    }

    @Test
    public void draw_recordsNoHistory() {
        HomeSession s = newSession(3);
        s.draw();
        s.draw();
        assertTrue(history.isEmpty());
    }

    @Test
    public void acceptTwice_recordsOnce() {
        HomeSession s = newSession(3);
        s.draw();
        s.accept();
        s.accept();
        assertEquals(1, history.size());
    }

    @Test
    public void redraw_quotaEmpty_recordsNoHistory() {
        HomeSession s = newSession(1);
        s.draw();
        s.redraw();
        history.clear();
        assertEquals(DrawStatus.QUOTA_EMPTY, s.redraw().status);
        assertTrue(history.isEmpty());
    }

    @Test
    public void redraw_noMatch_recordsNoHistory() {
        HomeSession s = newSession(3);
        s.draw();
        s.setFilter(new MenuFilter(null, false, true));
        assertEquals(DrawStatus.NO_MATCH, s.redraw().status);
        assertTrue(history.isEmpty());
    }

    @Test
    public void redraw_afterAccept_recordsOnlyTheAccept() {
        HomeSession s = newSession(3);
        s.draw();
        s.accept();
        s.redraw();
        assertEquals(1, history.size());
        assertEquals(HistoryEntry.STATUS_ACCEPTED, history.get(0).status);
    }

    @Test
    public void historyLevel_bestLevelHasMostStars() {
        assertEquals(4, HomeSession.historyLevel(GameConfig.LEVEL_GREAT));
        assertEquals(3, HomeSession.historyLevel(GameConfig.LEVEL_GOOD));
        assertEquals(2, HomeSession.historyLevel(GameConfig.LEVEL_NEUTRAL));
        assertEquals(1, HomeSession.historyLevel(GameConfig.LEVEL_CAUTION));
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullHistory_isRejected() {
        new HomeSession(new DrawRound(new WeightedDrawEngine()), source, recorder, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullRecorder_isRejected() {
        new HomeSession(new DrawRound(new WeightedDrawEngine()), source, null);
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
