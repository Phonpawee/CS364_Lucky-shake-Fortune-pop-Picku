package com.cs364.luckyshake.data;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * สร้างประวัติตัวอย่างสำหรับซ้อมเดโม (เมนูลับในหน้าตั้งค่า) ให้สมุดและสถิติมีข้อมูลโชว์
 * โดยไม่ต้องสุ่มจริงหลายวัน
 *
 * <p>แยกจาก Android เพื่อให้ทดสอบได้ด้วย unit test ปกติ คนเรียกเป็นคนบันทึกผลลง
 * {@link HistoryRepository#add}
 */
public final class DemoHistory {
    /** จำนวนรายการที่ปุ่มเติมประวัติตัวอย่างสร้าง */
    public static final int DEFAULT_COUNT = 24;
    /** กระจายรายการในกี่วันล่าสุด (รวมวันนี้) ตรงกับกราฟ 7 วันของหน้าสถิติ */
    public static final int DEFAULT_DAYS = 7;

    /** ช่วงเวลามื้อเช้า/กลางวัน/เย็น: ชั่วโมงเริ่ม และความยาวช่วงเป็นนาที */
    private static final int[] MEAL_START_HOUR = {7, 11, 17};
    private static final int MEAL_WINDOW_MINUTES = 150;
    /** ขอใหม่ก่อนยอมรับราว 1 ใน 3 ของมื้อ */
    private static final int REDRAW_ONE_IN = 3;
    private static final long MINUTE = 60_000L;

    private DemoHistory() {
    }

    /**
     * @param menus เมนูที่จะใช้ (ต้องมีอย่างน้อย 1) เลขไม้คือลำดับในลิสต์ + 1
     * @param count จำนวนรายการ
     * @param days  กระจายในกี่วันล่าสุด (รวมวันนี้) อย่างน้อย 2
     * @param now   เวลาปัจจุบัน ทุกรายการจะไม่เกินเวลานี้
     * @return รายการเรียงจากเก่าไปใหม่ ไม่มีรายการไหนเก่ากว่าเที่ยงคืนของวันแรก
     */
    public static List<HistoryEntry> generate(List<Menu> menus, int count, int days,
                                              long now, Random random) {
        if (menus == null || menus.isEmpty()) {
            throw new IllegalArgumentException("menus must not be empty");
        }
        if (days < 2) {
            throw new IllegalArgumentException("days must be at least 2");
        }
        List<HistoryEntry> out = new ArrayList<>(Math.max(0, count));
        // ไล่มื้อเช้าของทุกวันก่อน แล้วค่อยมื้อกลางวัน มื้อเย็น ทุกวันจึงได้ข้อมูลแม้ใช้ไม่ครบทุกมื้อ
        // ถ้ายังไม่ครบก็วนรอบใหม่
        int meal = 0;
        while (out.size() < count) {
            int dayIndex = meal % days;                                // 0 = วันเก่าสุด
            int slot = (meal / days) % MEAL_START_HOUR.length;
            meal++;
            long acceptedAt = mealTime(now, days - 1 - dayIndex, slot, random);
            if (acceptedAt > now) {
                continue;   // มื้อของวันนี้ที่ยังมาไม่ถึง
            }
            boolean redraw = random.nextInt(REDRAW_ONE_IN) == 0 && out.size() + 2 <= count;
            int accepted = random.nextInt(menus.size());
            if (redraw && menus.size() > 1) {
                int rejected = (accepted + 1 + random.nextInt(menus.size() - 1)) % menus.size();
                long redrawnAt = acceptedAt - (1 + random.nextInt(3)) * MINUTE;
                out.add(entry(menus, rejected, HistoryEntry.STATUS_REDRAWN, redrawnAt, random));
            }
            out.add(entry(menus, accepted, HistoryEntry.STATUS_ACCEPTED, acceptedAt, random));
        }
        // ถ้าวนรอบที่สอง มื้อของวันเก่าจะต่อท้าย จึงเรียงใหม่ตามเวลา
        out.sort((a, b) -> Long.compare(a.createdAt, b.createdAt));
        return out;
    }

    /** เวลาสุ่มในช่วงมื้อ {@code slot} ของ {@code daysAgo} วันก่อน (0 = วันนี้) */
    private static long mealTime(long now, int daysAgo, int slot, Random random) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(now);
        cal.add(Calendar.DAY_OF_YEAR, -daysAgo);
        cal.set(Calendar.HOUR_OF_DAY, MEAL_START_HOUR[slot]);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        // เผื่อ 5 นาทีแรกไว้ให้รายการขอใหม่ที่อยู่ก่อนหน้า ไม่ให้ตกไปอีกวัน
        cal.add(Calendar.MINUTE, 5 + random.nextInt(MEAL_WINDOW_MINUTES - 5));
        return cal.getTimeInMillis();
    }

    private static HistoryEntry entry(List<Menu> menus, int index, String status, long at,
                                      Random random) {
        Menu m = menus.get(index);
        HistoryEntry e = new HistoryEntry();
        e.menuId = m.id;
        e.nameTh = m.nameTh;
        e.nameEn = m.nameEn;
        e.level = 1 + random.nextInt(4);
        e.stickNo = index + 1;
        e.status = status;
        e.mode = HistoryEntry.MODE_SOLO;
        e.createdAt = at;
        return e;
    }
}
