package com.cs364.luckyshake.ui.home;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.cs364.luckyshake.data.Menu;
import com.cs364.luckyshake.logic.DrawResult;
import com.cs364.luckyshake.logic.DrawStatus;

/**
 * สิ่งที่หน้าหลัก/หน้าผลต้องแสดง ได้จาก {@link HomeViewModel#getState()} (ค่าคงที่ เปลี่ยนไม่ได้)
 *
 * <ul>
 *   <li>{@link #status}: รหัสผล ใช้เลือกข้อความ (ดูตารางใน {@link DrawStatus})</li>
 *   <li>{@link #menu}: เมนูที่สุ่มได้ หรือ null ถ้ายังไม่ได้สุ่ม/ไม่มีผล</li>
 *   <li>{@link #redrawsLeft}: จุดโควตาขอใหม่ที่เหลือ ถ้าเป็น 0 ให้ปิดปุ่มขอใหม่</li>
 *   <li>{@link #accepted}: true หลังกดยอมรับ ให้แสดงสถานะ "ยอมรับแล้ว" และปิดปุ่มขอใหม่</li>
 * </ul>
 */
public final class DrawState {
    /** ค่าของ {@link #stickNo} เมื่อไม่มีผล */
    public static final int NO_STICK = 0;

    /** สถานะเริ่มต้นก่อนสุ่มครั้งแรก: status เป็น null */
    public static final DrawState IDLE = new DrawState(null, null, DrawResult.NO_LEVEL, NO_STICK, 0, false);

    /** null เฉพาะตอนยังไม่เคยสุ่ม ({@link #IDLE}) */
    @Nullable
    public final DrawStatus status;
    @Nullable
    public final Menu menu;
    /** ระดับดวง (GameConfig.LEVEL_*) หรือ {@link DrawResult#NO_LEVEL} */
    public final int level;
    /** เลขไม้ = ลำดับของเมนูในชุด เริ่มที่ 1 หรือ {@link #NO_STICK} */
    public final int stickNo;
    public final int redrawsLeft;
    public final boolean accepted;

    DrawState(@Nullable DrawStatus status, @Nullable Menu menu, int level, int stickNo,
              int redrawsLeft, boolean accepted) {
        this.status = status;
        this.menu = menu;
        this.level = menu == null ? DrawResult.NO_LEVEL : level;
        this.stickNo = menu == null ? NO_STICK : stickNo;
        this.redrawsLeft = redrawsLeft;
        this.accepted = accepted;
    }

    public boolean hasResult() {
        return menu != null;
    }

    public boolean hasPendingResult() {
        return hasResult() && !accepted;
    }

    /** ปุ่มขอใหม่กดได้ไหม */
    public boolean canRedraw() {
        return hasResult() && !accepted && redrawsLeft > 0;
    }

    DrawState asAccepted() {
        return new DrawState(status, menu, level, stickNo, 0, true);
    }

    @NonNull
    @Override
    public String toString() {
        return "DrawState{status=" + status + ", menu=" + (menu == null ? null : menu.id)
                + ", level=" + level + ", stickNo=" + stickNo + ", redrawsLeft=" + redrawsLeft
                + ", accepted=" + accepted + "}";
    }
}
