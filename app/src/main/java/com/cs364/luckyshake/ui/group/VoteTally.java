package com.cs364.luckyshake.ui.group;

import com.cs364.luckyshake.logic.DrawEngine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Pure Java utility for tallying group vote options and drawing a weighted winner (#37).
 * Contains no Android framework dependencies to allow pure JUnit unit testing.
 */
public final class VoteTally {

    private VoteTally() {
    }

    private static class Entry {
        final String firstLabel;
        final Set<Integer> proposers = new HashSet<>();
        final int firstAppearanceOrder;

        Entry(String firstLabel, int order) {
            this.firstLabel = firstLabel;
            this.firstAppearanceOrder = order;
        }
    }

    /**
     * Tallies vote proposals from participants into a list of {@link VoteOption} items.
     * Case-insensitive key matching preserves the first encountered label string.
     * Deduplicates multiple votes for the same option by the same participant.
     */
    public static List<VoteOption> tally(List<List<String>> proposals) {
        if (proposals == null || proposals.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, Entry> map = new LinkedHashMap<>();
        int orderCounter = 0;

        for (int pIndex = 0; pIndex < proposals.size(); pIndex++) {
            List<String> userProposals = proposals.get(pIndex);
            if (userProposals == null || userProposals.isEmpty()) {
                continue;
            }

            int participantNum = pIndex + 1; // 1-based participant number
            Set<String> seenForUser = new HashSet<>();

            for (String raw : userProposals) {
                if (raw == null) {
                    continue;
                }
                String trimmed = raw.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }

                String key = trimmed.toLowerCase(Locale.ROOT);
                if (!seenForUser.add(key)) {
                    // Already counted for this participant
                    continue;
                }

                Entry entry = map.get(key);
                if (entry == null) {
                    entry = new Entry(trimmed, orderCounter++);
                    map.put(key, entry);
                }
                entry.proposers.add(participantNum);
            }
        }

        List<Entry> entries = new ArrayList<>(map.values());
        // Sort by votes DESC, then by firstAppearanceOrder ASC (stable)
        Collections.sort(entries, (a, b) -> {
            int voteDiff = Integer.compare(b.proposers.size(), a.proposers.size());
            if (voteDiff != 0) {
                return voteDiff;
            }
            return Integer.compare(a.firstAppearanceOrder, b.firstAppearanceOrder);
        });

        List<VoteOption> result = new ArrayList<>(entries.size());
        for (Entry e : entries) {
            result.add(new VoteOption(e.firstLabel, e.proposers.size(), new ArrayList<>(e.proposers)));
        }

        return Collections.unmodifiableList(result);
    }

    /**
     * Draws a winner from the vote options weighted by vote count.
     *
     * @param options candidate options to pick from
     * @param engine  draw engine instance
     * @return the winning VoteOption
     * @throws IllegalArgumentException if options is null/empty or engine is null
     */
    public static VoteOption draw(List<VoteOption> options, DrawEngine engine) {
        if (options == null || options.isEmpty()) {
            throw new IllegalArgumentException("options must not be null or empty");
        }
        if (engine == null) {
            throw new IllegalArgumentException("engine must not be null");
        }

        List<Double> weights = new ArrayList<>(options.size());
        for (VoteOption opt : options) {
            weights.add((double) opt.votes);
        }

        return engine.weightedPick(options, weights);
    }
}
