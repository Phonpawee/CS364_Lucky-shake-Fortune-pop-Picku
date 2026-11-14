package com.cs364.luckyshake.logic;

import androidx.annotation.NonNull;

/**
 * นับโควตาขอเซียมซีใหม่ต่อรอบ
 *
 * <p>ค่าสูงสุดมาจากหน้าตั้งค่า (ค่าเริ่มต้น {@link GameConfig#MAX_REDRAWS}) และถูกบีบให้อยู่ในช่วง
 * {@link GameConfig#MIN_REDRAW_LIMIT}..{@link GameConfig#MAX_REDRAW_LIMIT} เสมอ
 * คลาสนี้ไม่อ่าน SharedPreferences เอง ให้ ViewModel อ่านค่าแล้วส่งเข้ามา
 */
public final class RedrawQuota {
    private int max;
    private int used;

    public RedrawQuota() {
        this(GameConfig.MAX_REDRAWS);
    }

    public RedrawQuota(int max) {
        this.max = GameConfig.clampRedrawLimit(max);
    }

    /** จำนวนครั้งที่ยังขอใหม่ได้ ไม่ติดลบ */
    public int remaining() {
        return Math.max(0, max - used);
    }

    public boolean isEmpty() {
        return remaining() == 0;
    }

    public int max() {
        return max;
    }

    /**
     * ใช้โควตา 1 ครั้ง
     *
     * @return true ถ้าใช้ได้, false ถ้าโควตาหมดแล้ว (ค่าไม่เปลี่ยนและไม่โยน exception)
     */
    public boolean consume() {
        if (used >= max) {
            return false;
        }
        used++;
        return true;
    }

    /** เริ่มนับใหม่ (เรียกเมื่อยอมรับผลหรือเริ่มรอบใหม่) */
    public void reset() {
        used = 0;
    }

    /**
     * เปลี่ยนค่าสูงสุด เช่น เมื่อผู้ใช้แก้ค่าในหน้าตั้งค่า จำนวนที่ใช้ไปแล้วในรอบนี้ไม่เปลี่ยน
     * ถ้าใช้ไปเกินค่าใหม่แล้ว โควตาจะเหลือ 0 จนกว่าจะ reset
     */
    public void setMax(int max) {
        this.max = GameConfig.clampRedrawLimit(max);
    }

    @NonNull
    @Override
    public String toString() {
        return "RedrawQuota{remaining=" + remaining() + "/" + max + "}";
    }
}
