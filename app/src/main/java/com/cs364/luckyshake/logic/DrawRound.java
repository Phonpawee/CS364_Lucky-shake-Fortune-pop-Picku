package com.cs364.luckyshake.logic;

import java.util.ArrayList;
import java.util.List;

/**
 * หนึ่งรอบของการสุ่มแบบเล่นคนเดียว: สุ่มครั้งแรก → ขอใหม่ได้ตามโควตา → ยอมรับ
 *
 * <p>ให้ ViewModel ถือ instance เดียวไว้ (รอดการหมุนจอ) แล้วเรียก {@link #draw}, {@link #redraw},
 * {@link #accept} ทุกเมธอดคืน {@link DrawResult} พร้อม {@link DrawStatus} และไม่โยน exception
 * สำหรับกรณีขอบ (ชุดว่าง, ไม่มีเมนูผ่านตัวกรอง, โควตาหมด)
 *
 * <p>โควตาจะ reset เมื่อเริ่มรอบใหม่ ({@link #draw}) และเมื่อยอมรับ ({@link #accept})
 */
public class DrawRound {
    private final DrawEngine engine;
    private final RedrawQuota quota;
    private Candidate current;
    private int currentLevel = DrawResult.NO_LEVEL;

    public DrawRound(DrawEngine engine, RedrawQuota quota) {
        if (engine == null || quota == null) {
            throw new IllegalArgumentException("engine and quota must not be null");
        }
        this.engine = engine;
        this.quota = quota;
    }

    public DrawRound(DrawEngine engine) {
        this(engine, new RedrawQuota());
    }

    /**
     * เริ่มรอบใหม่: reset โควตาแล้วสุ่ม
     *
     * @param setSize   จำนวนเมนูทั้งหมดในชุด (ก่อนกรอง) ใช้แยก SET_EMPTY ออกจาก NO_MATCH
     * @param pool      เมนูที่ผ่านตัวกรองแล้ว เป็น null หรือว่างได้
     * @param recentIds id ที่ออกล่าสุด เรียงจากใหม่ไปเก่า เป็น null ได้
     */
    public DrawResult draw(int setSize, List<Candidate> pool, List<Long> recentIds) {
        quota.reset();
        clearCurrent();
        return pickNew(setSize, pool, recentIds, null);
    }

    /**
     * ขอใหม่: ใช้โควตา 1 ครั้งแล้วสุ่มใหม่ โดยไม่คืนเมนูเดิม (ถ้ามีตัวเลือกอื่น)
     *
     * <ul>
     *   <li>ถ้ายังไม่มีผลในรอบนี้ จะทำงานเหมือน {@link #draw} (ไม่ใช้โควตา)</li>
     *   <li>ถ้าโควตาหมด คืน {@link DrawStatus#QUOTA_EMPTY} พร้อมเมนูเดิม</li>
     *   <li>ถ้าชุดว่างหรือไม่มีเมนูผ่านตัวกรอง คืนสถานะนั้นโดยไม่ใช้โควตา และยังจำเมนูเดิมไว้</li>
     * </ul>
     */
    public DrawResult redraw(int setSize, List<Candidate> pool, List<Long> recentIds) {
        if (current == null) {
            return draw(setSize, pool, recentIds);
        }
        if (quota.isEmpty()) {
            return new DrawResult(DrawStatus.QUOTA_EMPTY, current, currentLevel, 0);
        }
        return pickNew(setSize, pool, recentIds, current.id);
    }

    /**
     * ยอมรับผลปัจจุบัน แล้ว reset โควตาและจบรอบ
     *
     * @return เมนูที่ยอมรับ หรือ null ถ้ายังไม่มีผล
     */
    public Candidate accept() {
        Candidate accepted = current;
        discard();
        return accepted;
    }

    public void discard() {
        quota.reset();
        clearCurrent();
    }

    /** เมนูที่แสดงอยู่ตอนนี้ หรือ null */
    public Candidate current() {
        return current;
    }

    /** ระดับดวงของเมนูที่แสดงอยู่ หรือ {@link DrawResult#NO_LEVEL} */
    public int currentLevel() {
        return currentLevel;
    }

    public int redrawsLeft() {
        return quota.remaining();
    }

    /** ใช้เมื่อผู้ใช้เปลี่ยนโควตาในหน้าตั้งค่า */
    public void setMaxRedraws(int max) {
        quota.setMax(max);
    }

    private DrawResult pickNew(int setSize, List<Candidate> pool, List<Long> recentIds,
                               Long excludeId) {
        List<Candidate> items = nonNull(pool);
        if (items.isEmpty()) {
            DrawStatus status = setSize > 0 ? DrawStatus.NO_MATCH : DrawStatus.SET_EMPTY;
            return new DrawResult(status, null, DrawResult.NO_LEVEL, quota.remaining());
        }
        if (excludeId != null) {
            quota.consume();
        }
        current = engine.pick(items, recentIds, excludeId);
        currentLevel = engine.pickLevel();
        DrawStatus status = items.size() < GameConfig.FEW_ITEMS_THRESHOLD
                ? DrawStatus.FEW_ITEMS : DrawStatus.OK;
        return new DrawResult(status, current, currentLevel, quota.remaining());
    }

    private void clearCurrent() {
        current = null;
        currentLevel = DrawResult.NO_LEVEL;
    }

    private static List<Candidate> nonNull(List<Candidate> pool) {
        List<Candidate> items = new ArrayList<>();
        if (pool != null) {
            for (Candidate c : pool) {
                if (c != null) items.add(c);
            }
        }
        return items;
    }
}
