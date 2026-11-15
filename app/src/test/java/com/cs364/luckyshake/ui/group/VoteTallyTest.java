package com.cs364.luckyshake.ui.group;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.cs364.luckyshake.logic.DrawEngine;
import com.cs364.luckyshake.logic.WeightedDrawEngine;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Pure Java unit tests for {@link VoteTally} and group proposal final tally calculation.
 */
public class VoteTallyTest {

    @Test
    public void testTallyBasicConsolidation() {
        List<List<String>> proposals = Arrays.asList(
                Arrays.asList("KFC", "MK"),
                Collections.singletonList("kfc"),
                Collections.emptyList()
        );

        List<VoteOption> tally = VoteTally.tally(proposals);

        assertEquals(2, tally.size());

        // KFC has 2 votes (participants 1 and 2), MK has 1 vote (participant 1)
        VoteOption opt1 = tally.get(0);
        assertEquals("KFC", opt1.label);
        assertEquals(2, opt1.votes);
        assertEquals(Arrays.asList(1, 2), opt1.proposers);

        VoteOption opt2 = tally.get(1);
        assertEquals("MK", opt2.label);
        assertEquals(1, opt2.votes);
        assertEquals(Collections.singletonList(1), opt2.proposers);
    }

    @Test
    public void testTallyPreservesFirstLabelCasing() {
        List<List<String>> proposals = Arrays.asList(
                Collections.singletonList("kfc"),
                Collections.singletonList("KFC")
        );

        List<VoteOption> tally = VoteTally.tally(proposals);

        assertEquals(1, tally.size());
        assertEquals("kfc", tally.get(0).label);
        assertEquals(2, tally.get(0).votes);
    }

    @Test
    public void testTallySameUserDuplicateDeduplication() {
        List<List<String>> proposals = Collections.singletonList(
                Arrays.asList("MK", " mk ")
        );

        List<VoteOption> tally = VoteTally.tally(proposals);

        assertEquals(1, tally.size());
        assertEquals("MK", tally.get(0).label);
        assertEquals(1, tally.get(0).votes);
        assertEquals(Collections.singletonList(1), tally.get(0).proposers);
    }

    @Test
    public void testTallyFoodTypeKeys() {
        List<List<String>> proposals = Arrays.asList(
                Collections.singletonList("food_type_japanese"),
                Collections.singletonList("food_type_japanese"),
                Collections.singletonList("food_type_isan")
        );

        List<VoteOption> tally = VoteTally.tally(proposals);

        assertEquals(2, tally.size());
        assertEquals("food_type_japanese", tally.get(0).label);
        assertEquals(2, tally.get(0).votes);
        assertEquals("food_type_isan", tally.get(1).label);
        assertEquals(1, tally.get(1).votes);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAllSkippedThrowsExceptionOnDraw() {
        List<List<String>> proposals = Arrays.asList(
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList()
        );

        List<VoteOption> tally = VoteTally.tally(proposals);
        assertTrue(tally.isEmpty());

        DrawEngine engine = new WeightedDrawEngine(new Random(42));
        VoteTally.draw(tally, engine);
    }

    @Test
    public void testDrawSingleOption() {
        VoteOption option = new VoteOption("Ramen", 1, Collections.singletonList(1));
        DrawEngine engine = new WeightedDrawEngine(new Random(42));

        VoteOption winner = VoteTally.draw(Collections.singletonList(option), engine);
        assertEquals("Ramen", winner.label);
    }

    @Test
    public void testDrawWeightedProbability() {
        VoteOption optionA = new VoteOption("A", 2, Arrays.asList(1, 2));
        VoteOption optionB = new VoteOption("B", 1, Collections.singletonList(3));
        List<VoteOption> options = Arrays.asList(optionA, optionB);

        DrawEngine engine = new WeightedDrawEngine(new Random(42));
        int winsA = 0;
        int winsB = 0;
        int iterations = 30000;

        for (int i = 0; i < iterations; i++) {
            VoteOption winner = VoteTally.draw(options, engine);
            if ("A".equals(winner.label)) {
                winsA++;
            } else if ("B".equals(winner.label)) {
                winsB++;
            }
        }

        double ratioA = (double) winsA / iterations;
        assertTrue("Wins for A (" + winsA + ") should be greater than B (" + winsB + ")", winsA > winsB);
        assertTrue("Ratio for A (" + ratioA + ") should be between 0.64 and 0.69", ratioA >= 0.64 && ratioA <= 0.69);
    }

    @Test
    public void testTallyInitializationOnSessionFinalPhase() {
        GroupSession session = new GroupSession();
        session.setParticipantCount(2);
        session.setProposalMethod(ProposalMethod.FOOD_TYPE);
        session.startSession();

        // Participant 1 submits
        session.submitProposal(Collections.singletonList("food_type_grill"));
        assertEquals(GroupPhase.COLLECT, session.getPhase());

        // Participant 2 submits (final submission)
        session.submitProposal(Collections.singletonList("food_type_hotpot"));
        assertEquals(GroupPhase.FINAL, session.getPhase());

        // Compute tally on FINAL phase
        List<VoteOption> tally = VoteTally.tally(session.getProposals());
        assertNotNull("Tally must be populated on FINAL phase", tally);
        assertFalse("Tally must not be empty when valid proposals submitted", tally.isEmpty());
        assertEquals(2, tally.size());
    }
}
