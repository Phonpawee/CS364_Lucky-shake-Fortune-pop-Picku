package com.cs364.luckyshake.logic;

import java.util.ArrayList;
import java.util.List;

/**
 * Random draw used by the Home/Result screens and by group mode.
 * Do not change these signatures without telling the team (see README).
 *
 * เรียกใช้ผ่าน {@link #create()} ซึ่งจะคืน {@link WeightedDrawEngine}
 */
public interface DrawEngine {

    /**
     * Picks one candidate from {@code pool}. Items whose id is in {@code recentIds}
     * (the last GameConfig.RECENT_COUNT draws) have a lower chance (GameConfig.RECENT_PENALTY).
     *
     * @param pool      candidates to choose from; must not be empty
     * @param recentIds ids of recent draws, newest first; may be empty
     * @return the chosen candidate (the only one when the pool has one item)
     * @throws IllegalArgumentException if {@code pool} is null or empty
     */
    Candidate pick(List<Candidate> pool, List<Long> recentIds);

    /**
     * เหมือน {@link #pick(List, List)} แต่ใช้ตอน "ขอใหม่": ถ้า pool มีมากกว่า 1 ตัว
     * จะไม่คืนตัวที่มี id เป็น {@code excludeId} (ตัวที่เพิ่งแสดง) เด็ดขาด
     * ถ้า pool มีตัวเดียว จะคืนตัวนั้นแม้จะถูกยกเว้นก็ตาม
     *
     * @param excludeId id ที่ห้ามคืน; ถ้าเป็น null คือไม่ยกเว้นตัวไหน
     */
    default Candidate pick(List<Candidate> pool, List<Long> recentIds, Long excludeId) {
        if (pool == null || pool.isEmpty()) {
            throw new IllegalArgumentException("pool must not be empty");
        }
        if (excludeId == null || pool.size() == 1) {
            return pick(pool, recentIds);
        }
        List<Candidate> rest = new ArrayList<>(pool.size());
        for (Candidate c : pool) {
            if (c.id != excludeId) rest.add(c);
        }
        return pick(rest.isEmpty() ? pool : rest, recentIds);
    }

    /** Picks a fortune level (one of GameConfig.LEVEL_*) using GameConfig.levelWeights(). */
    int pickLevel();

    /**
     * Generic weighted random pick, for any list (used by group mode).
     *
     * @param items   items to choose from; must not be empty
     * @param weights one non-negative weight per item, same size as {@code items}
     * @return the chosen item
     * @throws IllegalArgumentException if the lists are empty or of different sizes
     */
    <T> T weightedPick(List<T> items, List<Double> weights);

    /** The engine the app should use. */
    static DrawEngine create() {
        return new WeightedDrawEngine();
    }
}