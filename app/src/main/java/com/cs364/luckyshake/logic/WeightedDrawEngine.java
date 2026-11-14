package com.cs364.luckyshake.logic;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * DrawEngine ตัวจริง (อิชชู #3)
 *
 * <ul>
 *   <li>{@link #pick}: น้ำหนักจริง = candidate.weight × (id อยู่ใน
 *       {@link GameConfig#RECENT_COUNT} ครั้งล่าสุด ? {@link GameConfig#RECENT_PENALTY} : 1)
 *       แล้วสุ่มแบบสะสมน้ำหนัก (cumulative sum)</li>
 *   <li>{@link #pickLevel}: สุ่มระดับดวงตาม {@link GameConfig#levelWeights()}</li>
 *   <li>{@link #weightedPick}: สุ่มถ่วงน้ำหนักแบบทั่วไป ใช้กับ list ใดก็ได้ (โหมดกลุ่ม)</li>
 * </ul>
 *
 * ส่ง {@link Random} ที่กำหนด seed เข้ามาได้ เพื่อให้ผลการทดสอบซ้ำได้ทุกครั้ง
 */
public class WeightedDrawEngine implements DrawEngine {
    private final Random random;

    public WeightedDrawEngine() {
        this(new Random());
    }

    /** ส่ง Random ที่กำหนด seed เข้ามา เพื่อให้ได้ผลเหมือนเดิมทุกครั้ง (ใช้ในเทสต์) */
    public WeightedDrawEngine(Random random) {
        if (random == null) {
            throw new IllegalArgumentException("random must not be null");
        }
        this.random = random;
    }

    @Override
    public Candidate pick(List<Candidate> pool, List<Long> recentIds) {
        if (pool == null || pool.isEmpty()) {
            throw new IllegalArgumentException("pool must not be empty");
        }
        if (pool.size() == 1) {
            return pool.get(0);
        }

        // นับเฉพาะ RECENT_COUNT ตัวแรก (recentIds เรียงจากใหม่สุดไปเก่าสุด)
        Set<Long> recent = new HashSet<>();
        if (recentIds != null) {
            int n = Math.min(GameConfig.RECENT_COUNT, recentIds.size());
            for (int i = 0; i < n; i++) {
                Long id = recentIds.get(i);
                if (id != null) recent.add(id);
            }
        }

        // เมนูที่เพิ่งออกจะถูกคูณน้ำหนักด้วย RECENT_PENALTY
        List<Double> weights = new ArrayList<>(pool.size());
        for (Candidate c : pool) {
            double w = Math.max(0, c.weight);
            if (recent.contains(c.id)) {
                w *= GameConfig.RECENT_PENALTY;
            }
            weights.add(w);
        }
        return weightedPick(pool, weights);
    }

    @Override
    public int pickLevel() {
        int[] levelWeights = GameConfig.levelWeights();
        List<Integer> levels = new ArrayList<>(levelWeights.length);
        List<Double> weights = new ArrayList<>(levelWeights.length);
        for (int i = 0; i < levelWeights.length; i++) {
            levels.add(i);
            weights.add((double) levelWeights[i]);
        }
        return weightedPick(levels, weights);
    }

    /**
     * สุ่มถ่วงน้ำหนักด้วยวิธีสะสมน้ำหนัก ตัวที่น้ำหนักเป็น 0 จะไม่ถูกเลือก
     * ยกเว้นกรณีที่น้ำหนักเป็น 0 ทุกตัว จะสุ่มโดยทุกตัวมีโอกาสเท่ากัน
     *
     * @throws IllegalArgumentException ถ้า list เป็น null/ว่าง, ขนาดไม่เท่ากัน
     *                                  หรือมีน้ำหนักที่เป็น null, ติดลบ, NaN หรืออนันต์
     */
    @Override
    public <T> T weightedPick(List<T> items, List<Double> weights) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("items must not be empty");
        }
        if (weights == null || weights.size() != items.size()) {
            throw new IllegalArgumentException("weights must have the same size as items");
        }

        double total = 0;
        for (Double w : weights) {
            if (w == null || w.isNaN() || w.isInfinite() || w < 0) {
                throw new IllegalArgumentException("weights must be finite and non-negative: " + w);
            }
            total += w;
        }
        if (items.size() == 1) {
            return items.get(0);
        }
        if (total <= 0) {
            return items.get(random.nextInt(items.size()));
        }

        double r = random.nextDouble() * total; // อยู่ในช่วง [0, total)
        double cumulative = 0;
        int lastPositive = -1;
        for (int i = 0; i < items.size(); i++) {
            double w = weights.get(i);
            if (w <= 0) continue;
            lastPositive = i;
            cumulative += w;
            if (r < cumulative) {
                return items.get(i);
            }
        }
        // มาถึงตรงนี้ได้เฉพาะกรณีปัดเศษทศนิยมเท่านั้น ให้คืนตัวสุดท้ายที่มีน้ำหนัก
        return items.get(lastPositive);
    }
}
