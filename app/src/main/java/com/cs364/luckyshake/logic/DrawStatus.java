package com.cs364.luckyshake.logic;

/**
 * รหัสผลของการสุ่มแต่ละครั้ง ให้หน้าจอเลือกข้อความที่จะแสดง (ดู {@link DrawResult})
 *
 * <p>{@link #OK} และ {@link #FEW_ITEMS} มีผลสุ่มเสมอ ส่วน {@link #NO_MATCH} และ
 * {@link #SET_EMPTY} ไม่มีผล สำหรับ {@link #QUOTA_EMPTY} ผลคือเมนูเดิมที่แสดงอยู่
 */
public enum DrawStatus {
    /** สุ่มได้ตามปกติ ไม่ต้องแสดงข้อความเพิ่ม */
    OK,
    /** ขอใหม่ครบโควตาแล้ว ไม่ได้สุ่มใหม่ ผลยังเป็นเมนูเดิม ควรปิดปุ่มขอใหม่ */
    QUOTA_EMPTY,
    /** ชุดเมนูมีเมนู แต่ไม่มีเมนูไหนผ่านตัวกรองเลย ไม่มีผล */
    NO_MATCH,
    /** ชุดเมนูที่เลือกไม่มีเมนูเลย ไม่มีผล */
    SET_EMPTY,
    /** สุ่มได้ แต่มีเมนูให้สุ่มน้อยกว่า {@link GameConfig#FEW_ITEMS_THRESHOLD} ตัว (เตือน ไม่ใช่ error) */
    FEW_ITEMS;

    /** true ถ้าสถานะนี้มีเมนูให้แสดง */
    public boolean hasResult() {
        return this == OK || this == FEW_ITEMS || this == QUOTA_EMPTY;
    }
}
