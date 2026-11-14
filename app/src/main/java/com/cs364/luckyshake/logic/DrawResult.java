package com.cs364.luckyshake.logic;

/**
 * ผลของ {@link DrawRound#draw}/{@link DrawRound#redraw}: รหัสผลคู่กับเมนูที่สุ่มได้
 *
 * <p>ถ้า {@link #hasResult()} เป็น false ({@link DrawStatus#NO_MATCH}, {@link DrawStatus#SET_EMPTY})
 * {@link #candidate} จะเป็น null และ {@link #level} เป็น {@link #NO_LEVEL}
 */
public final class DrawResult {
    /** ค่าของ {@link #level} เมื่อไม่มีผล */
    public static final int NO_LEVEL = -1;

    public final DrawStatus status;
    /** เมนูที่สุ่มได้ หรือ null ถ้าไม่มีผล */
    public final Candidate candidate;
    /** ระดับดวง (GameConfig.LEVEL_*) หรือ {@link #NO_LEVEL} */
    public final int level;
    /** จำนวนครั้งที่ยังขอใหม่ได้ในรอบนี้ ถ้าเป็น 0 ควรปิดปุ่มขอใหม่ */
    public final int redrawsLeft;

    public DrawResult(DrawStatus status, Candidate candidate, int level, int redrawsLeft) {
        if (status == null) {
            throw new IllegalArgumentException("status must not be null");
        }
        this.status = status;
        this.candidate = candidate;
        this.level = candidate == null ? NO_LEVEL : level;
        this.redrawsLeft = redrawsLeft;
    }

    public boolean hasResult() {
        return candidate != null;
    }

    @Override
    public String toString() {
        return "DrawResult{status=" + status + ", candidate=" + candidate
                + ", level=" + level + ", redrawsLeft=" + redrawsLeft + "}";
    }
}
