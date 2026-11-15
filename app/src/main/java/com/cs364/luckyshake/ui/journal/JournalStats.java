package com.cs364.luckyshake.ui.journal;

import com.cs364.luckyshake.data.HistoryRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Pure Java model holding computed statistics for the Journal screen (#41).
 * Contains no Android framework dependencies to allow pure JUnit unit testing.
 */
public class JournalStats {

    public final List<HistoryRepository.NameCount> topDishes;
    public final int accepted;
    public final int redrawn;
    public final int[] perDay;

    public JournalStats(List<HistoryRepository.NameCount> topDishes, int accepted, int redrawn, int[] perDay) {
        this.topDishes = topDishes != null ? new ArrayList<>(topDishes) : Collections.emptyList();
        this.accepted = Math.max(0, accepted);
        this.redrawn = Math.max(0, redrawn);
        if (perDay != null && perDay.length == 7) {
            this.perDay = perDay.clone();
        } else {
            this.perDay = new int[7];
        }
    }

    /**
     * Returns true if there are no history entries recorded at all.
     */
    public boolean isEmpty() {
        if (accepted > 0 || redrawn > 0) {
            return false;
        }
        if (topDishes != null && !topDishes.isEmpty()) {
            return false;
        }
        for (int count : perDay) {
            if (count > 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Calculates the acceptance percentage integer value (0-100).
     * Prevents division by zero if both accepted and redrawn are zero.
     */
    public int acceptPercent() {
        int total = accepted + redrawn;
        if (total == 0) {
            return 0;
        }
        return (accepted * 100) / total;
    }

    /**
     * Finds the maximum count per day among the 7-day trend array.
     * Returns at least 1 to prevent division by zero when calculating bar heights.
     */
    public int maxPerDay() {
        int max = 0;
        for (int count : perDay) {
            if (count > max) {
                max = count;
            }
        }
        return Math.max(1, max);
    }

    /**
     * Finds the maximum count among top dishes.
     * Returns at least 1 to prevent division by zero when calculating progress bars.
     */
    public int maxTopCount() {
        int max = 0;
        if (topDishes != null) {
            for (HistoryRepository.NameCount nc : topDishes) {
                if (nc != null && nc.count > max) {
                    max = nc.count;
                }
            }
        }
        return Math.max(1, max);
    }
}
