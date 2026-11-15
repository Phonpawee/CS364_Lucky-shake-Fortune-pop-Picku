package com.cs364.luckyshake.ui.home;

import com.cs364.luckyshake.data.Menu;
import com.cs364.luckyshake.data.MenuFilter;
import com.cs364.luckyshake.logic.Candidate;
import com.cs364.luckyshake.logic.DrawResult;
import com.cs364.luckyshake.logic.DrawRound;
import com.cs364.luckyshake.logic.DrawStatus;
import com.cs364.luckyshake.logic.GameConfig;
import com.cs364.luckyshake.logic.WeightPolicy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * สถานะและการทำงานของหน้าหลัก/หน้าผล แยกจาก {@link HomeViewModel} เพื่อให้ทดสอบได้โดยไม่ต้องใช้ Android
 * (แบบเดียวกับ GroupSession/GroupViewModel)
 *
 * <p>เก็บ: ชุดเมนูที่เลือก, ตัวกรอง, ผลล่าสุด ({@link DrawState}), โควตาที่เหลือ (ใน {@link DrawRound})
 * และรายการ recent id
 */
public class HomeSession {
    /** ค่าของ setId เมื่อยังไม่ได้เลือกชุด */
    public static final long NO_SET = -1;

    /** ที่มาของเมนู ใน ViewModel คือ MenuRepository ในเทสต์ใช้ของปลอมได้ */
    public interface MenuSource {
        /** เมนูในชุด เรียงตาม id (ลำดับเดียวกับเลขไม้) filter เป็น null = ไม่กรอง */
        List<Menu> getMenus(long setId, MenuFilter filter);
    }

    /** ที่บันทึกน้ำหนัก (อิชชู #6) ใน ViewModel คือ MenuRepository.recordEvent ในเทสต์ใช้ของปลอมได้ */
    public interface WeightRecorder {
        void record(long menuId, WeightPolicy.Event event);
    }

    private static final WeightRecorder NO_RECORDER = (menuId, event) -> { };

    private final DrawRound round;
    private final MenuSource source;
    private final WeightRecorder weights;

    private long setId = NO_SET;
    private MenuFilter filter = MenuFilter.none();
    /** id ที่ออกล่าสุด เรียงจากใหม่ไปเก่า (ซ้ำได้) เก็บไม่เกิน GameConfig.RECENT_COUNT ตัว */
    private final List<Long> recentIds = new ArrayList<>();
    private DrawState state = DrawState.IDLE;

    public HomeSession(DrawRound round, MenuSource source) {
        this(round, source, NO_RECORDER);
    }

    /** @param weights ถูกเรียกเมื่อยอมรับ (+1) หรือขอใหม่แล้วได้เมนูใหม่ (−1 ให้เมนูที่ถูกปฏิเสธ) */
    public HomeSession(DrawRound round, MenuSource source, WeightRecorder weights) {
        if (round == null || source == null || weights == null) {
            throw new IllegalArgumentException("round, source and weights must not be null");
        }
        this.round = round;
        this.source = source;
        this.weights = weights;
    }

    // ------------------------------------------------------------ settings

    public long getSetId() {
        return setId;
    }

    public void setSetId(long setId) {
        this.setId = setId;
    }

    /** คืนสำเนา แก้แล้วไม่กระทบค่าที่เก็บไว้ */
    public MenuFilter getFilter() {
        return copy(filter);
    }

    /** null = ไม่กรอง */
    public void setFilter(MenuFilter filter) {
        this.filter = filter == null ? MenuFilter.none() : copy(filter);
    }

    public void setMaxRedraws(int max) {
        round.setMaxRedraws(max);
        if (state.hasResult() && !state.accepted) {
            state = new DrawState(state.status, state.menu, state.level, state.stickNo,
                    round.redrawsLeft(), false);
        }
    }

    public List<Long> getRecentIds() {
        // ไม่ใช้ List.copyOf() เพราะต้องใช้ API 30 แต่แอปรองรับตั้งแต่ API 26
        //noinspection Java9CollectionFactory
        return Collections.unmodifiableList(new ArrayList<>(recentIds));
    }

    public DrawState getState() {
        return state;
    }

    // ------------------------------------------------------------- actions

    /** เริ่มรอบใหม่ (เขย่า/กดสุ่มที่หน้าหลัก) */
    public DrawState draw() {
        Pool pool = loadPool();
        DrawResult r = round.draw(pool.all.size(), pool.candidates, recentIds);
        return apply(r, pool, false);
    }

    /**
     * ขอใหม่ (หน้าผล) ใช้โควตา 1 ครั้ง ถ้ากดหลังยอมรับแล้วจะเริ่มรอบใหม่
     *
     * <p>เมนูที่ถูกปฏิเสธได้น้ำหนัก −1 เฉพาะเมื่อสุ่มใหม่ได้จริง (ใช้โควตา) ถ้าโควตาหมด
     * หรือไม่มีเมนูผ่านตัวกรอง เมนูเดิมยังแสดงอยู่ จึงไม่นับเป็นการปฏิเสธ
     */
    public DrawState redraw() {
        // state.menu ยังแสดงอยู่ได้แม้สถานะเป็น NO_MATCH (ขอใหม่ครั้งก่อนกรองหมด)
        Menu rejected = state.accepted ? null : state.menu;
        Pool pool = loadPool();
        DrawResult r = round.redraw(pool.all.size(), pool.candidates, recentIds);
        DrawState next = apply(r, pool, !state.accepted);
        if (rejected != null && r.hasResult() && r.status != DrawStatus.QUOTA_EMPTY) {
            weights.record(rejected.id, WeightPolicy.Event.REDRAW);
        }
        return next;
    }

    /**
     * ยอมรับผลปัจจุบัน โควตาจะ reset สำหรับรอบหน้า ผลยังแสดงอยู่โดยมี accepted = true
     *
     * @return เมนูที่ยอมรับ หรือ null ถ้ายังไม่มีผล/ยอมรับไปแล้ว
     */
    public Menu accept() {
        if (!state.hasResult() || state.accepted) {
            return null;
        }
        round.accept();
        state = state.asAccepted();
        if (state.menu != null) {
            weights.record(state.menu.id, WeightPolicy.Event.ACCEPT);
        }
        return state.menu;
    }

    // ------------------------------------------------------------- helpers

    /** @param keepShown true เมื่อเป็นการขอใหม่ระหว่างแสดงผล: ถ้าสุ่มไม่ได้ให้คงเมนูเดิมไว้ */
    private DrawState apply(DrawResult r, Pool pool, boolean keepShown) {
        if (!r.hasResult()) {
            // ชุดว่าง/ไม่มีเมนูผ่านตัวกรอง: รหัสผลเปลี่ยน แต่ถ้าเป็นการขอใหม่ เมนูเดิมยังแสดงอยู่
            Menu keep = keepShown ? state.menu : null;
            state = new DrawState(r.status, keep, state.level, state.stickNo, r.redrawsLeft, false);
            return state;
        }
        if (r.status == DrawStatus.QUOTA_EMPTY) {
            state = new DrawState(r.status, state.menu, state.level, state.stickNo, 0, false);
            return state;
        }
        Menu menu = find(pool.all, r.candidate.id);
        rememberRecent(r.candidate.id);
        state = new DrawState(r.status, menu, r.level, pool.all.indexOf(menu) + 1,
                r.redrawsLeft, false);
        return state;
    }

    private void rememberRecent(long id) {
        // ไม่ตัดตัวซ้ำ: recentIds คือ "RECENT_COUNT ครั้งล่าสุด" ถ้าออก A, B, B ต้องได้ [B, B]
        // เพื่อให้ A หลุดจากช่วงลดโอกาสตามจริง
        recentIds.add(0, id);
        while (recentIds.size() > GameConfig.RECENT_COUNT) {
            recentIds.remove(recentIds.size() - 1);
        }
    }

    private Pool loadPool() {
        if (setId == NO_SET) {
            return new Pool(Collections.emptyList(), Collections.emptyList());
        }
        List<Menu> all = nonNull(source.getMenus(setId, null));
        List<Menu> filtered = filter.isEmpty() ? all : nonNull(source.getMenus(setId, filter));
        List<Candidate> candidates = new ArrayList<>(filtered.size());
        for (Menu m : filtered) {
            candidates.add(new Candidate(m.id, m.weight));
        }
        return new Pool(all, candidates);
    }

    private static Menu find(List<Menu> menus, long id) {
        for (Menu m : menus) {
            if (m.id == id) return m;
        }
        return null;
    }

    private static List<Menu> nonNull(List<Menu> menus) {
        List<Menu> out = new ArrayList<>();
        if (menus != null) {
            for (Menu m : menus) {
                if (m != null) out.add(m);
            }
        }
        return out;
    }

    private static MenuFilter copy(MenuFilter f) {
        return new MenuFilter(f.type, f.noFried, f.noSpicy);
    }

    private static final class Pool {
        final List<Menu> all;
        final List<Candidate> candidates;

        Pool(List<Menu> all, List<Candidate> candidates) {
            this.all = all;
            this.candidates = candidates;
        }
    }
}
