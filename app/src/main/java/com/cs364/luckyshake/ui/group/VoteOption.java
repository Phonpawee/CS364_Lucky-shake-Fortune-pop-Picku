package com.cs364.luckyshake.ui.group;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Pure Java immutable data model holding a single group vote candidate option (#37).
 * Contains no Android framework dependencies to allow pure JUnit unit testing.
 */
public final class VoteOption {

    public final String label;
    public final int votes;
    public final List<Integer> proposers;

    public VoteOption(String label, int votes, List<Integer> proposers) {
        this.label = label != null ? label : "";
        this.votes = Math.max(0, votes);
        if (proposers != null) {
            List<Integer> sorted = new ArrayList<>(proposers);
            Collections.sort(sorted);
            this.proposers = Collections.unmodifiableList(sorted);
        } else {
            this.proposers = Collections.emptyList();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VoteOption that = (VoteOption) o;
        return votes == that.votes &&
                Objects.equals(label, that.label) &&
                Objects.equals(proposers, that.proposers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(label, votes, proposers);
    }

    @Override
    public String toString() {
        return "VoteOption{" +
                "label='" + label + '\'' +
                ", votes=" + votes +
                ", proposers=" + proposers +
                '}';
    }
}
