package com.cs364.luckyshake.logic;

/**
 * กฎการปรับน้ำหนักเมนู (อิชชู #6) เป็นฟังก์ชันล้วน: รับน้ำหนักเดิม + เหตุการณ์ คืนน้ำหนักใหม่
 * ไม่แตะฐานข้อมูล ไม่อ่านนาฬิกาเอง จึงทดสอบได้ง่าย และ MenuRepository เรียกใช้ได้
 *
 * <ul>
 *   <li>ยอมรับ: +{@link GameConfig#WEIGHT_ACCEPT_DELTA}</li>
 *   <li>ขอใหม่: {@link GameConfig#WEIGHT_REDRAW_DELTA}</li>
 *   <li>ผลลัพธ์อยู่ในช่วง {@link GameConfig#MIN_WEIGHT}..{@link GameConfig#MAX_WEIGHT} เสมอ
 *       (ต่ำสุดคือ 1 ไม่ใช่ 0 เมนูจึงไม่หายไปถาวร)</li>
 *   <li>ไม่ถูกแตะครบทุก {@link GameConfig#WEIGHT_DECAY_PERIOD_MS} (1 สัปดาห์) ขยับเข้าหา
 *       {@link GameConfig#DEFAULT_WEIGHT} อีก {@link GameConfig#WEIGHT_DECAY_STEP} ขั้น</li>
 * </ul>
 */
public final class WeightPolicy {

    /** เหตุการณ์ที่ทำให้น้ำหนักเปลี่ยน */
    public enum Event {
        ACCEPT(GameConfig.WEIGHT_ACCEPT_DELTA),
        REDRAW(GameConfig.WEIGHT_REDRAW_DELTA);

        public final int delta;

        Event(int delta) {
            this.delta = delta;
        }
    }

    private WeightPolicy() {
    }

    /** น้ำหนักใหม่หลังเกิดเหตุการณ์ (จำกัดช่วง 1–10) null = ไม่เปลี่ยน แค่จำกัดช่วง */
    public static int apply(int weight, Event event) {
        int delta = event == null ? 0 : event.delta;
        return GameConfig.clampWeight(GameConfig.clampWeight(weight) + delta);
    }

    /** จำนวนสัปดาห์เต็มที่ผ่านไปตั้งแต่ lastTouched (นาฬิกาถอยหลังหรือยังไม่ครบ = 0) */
    public static long weeksSince(long lastTouched, long now) {
        if (now <= lastTouched) {
            return 0;
        }
        return (now - lastTouched) / GameConfig.WEIGHT_DECAY_PERIOD_MS;
    }

    /**
     * น้ำหนักหลังการคืนค่า: ขยับเข้าหา 5 สัปดาห์ละ 1 ขั้น ไม่ข้ามเลย 5
     *
     * @param lastTouched เวลาที่แตะล่าสุด (epoch ms)
     * @param now         เวลาปัจจุบัน (epoch ms)
     */
    public static int decay(int weight, long lastTouched, long now) {
        int w = GameConfig.clampWeight(weight);
        long weeks = weeksSince(lastTouched, now);
        long steps = weeks * GameConfig.WEIGHT_DECAY_STEP;
        int target = GameConfig.DEFAULT_WEIGHT;
        if (w > target) {
            return (int) Math.max(target, w - steps);
        }
        if (w < target) {
            return (int) Math.min(target, w + steps);
        }
        return w;
    }

    /**
     * last_touched ใหม่หลังคืนค่า: เลื่อนไปข้างหน้าเฉพาะสัปดาห์เต็ม เศษที่เหลือยังนับต่อ
     * (เช่น ผ่านไป 1.5 สัปดาห์ ขยับ 1 ขั้น และอีก 0.5 สัปดาห์จะขยับอีกขั้น)
     */
    public static long decayedLastTouched(long lastTouched, long now) {
        return lastTouched + weeksSince(lastTouched, now) * GameConfig.WEIGHT_DECAY_PERIOD_MS;
    }
}
