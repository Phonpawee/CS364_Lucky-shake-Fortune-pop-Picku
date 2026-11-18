package com.cs364.luckyshake.ui.group;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.cs364.luckyshake.logic.GameConfig;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Pure Java unit test for {@link GroupSession}.
 * Does not depend on Android framework classes (no Looper or LiveData required).
 */
public class GroupSessionTest {
    private GroupSession session;

    @Before
    public void setUp() {
        session = new GroupSession();
    }

    @Test
    public void testDefaultState() {
        assertEquals(GameConfig.MIN_GROUP_PLAYERS, session.getParticipantCount());
        assertEquals(ProposalMethod.FOOD_TYPE, session.getProposalMethod());
        assertEquals(GroupPhase.SETUP, session.getPhase());
        assertEquals(0, session.getCurrentTurnIndex());
        assertEquals(1, session.getCurrentTurnNumber());
        assertTrue(session.getProposals().isEmpty());
    }

    @Test
    public void testIncrementParticipantCount() {
        assertTrue(session.incrementParticipantCount());
        assertEquals(3, session.getParticipantCount());

        for (int i = 3; i < GameConfig.MAX_GROUP_PLAYERS; i++) {
            assertTrue(session.incrementParticipantCount());
        }
        assertEquals(GameConfig.MAX_GROUP_PLAYERS, session.getParticipantCount());

        assertFalse(session.incrementParticipantCount());
        assertEquals(GameConfig.MAX_GROUP_PLAYERS, session.getParticipantCount());
    }

    @Test
    public void testDecrementParticipantCount() {
        session.setParticipantCount(4);
        assertTrue(session.decrementParticipantCount());
        assertEquals(3, session.getParticipantCount());

        assertTrue(session.decrementParticipantCount());
        assertEquals(GameConfig.MIN_GROUP_PLAYERS, session.getParticipantCount());

        assertFalse(session.decrementParticipantCount());
        assertEquals(GameConfig.MIN_GROUP_PLAYERS, session.getParticipantCount());
    }

    @Test
    public void testSetParticipantCountClamping() {
        session.setParticipantCount(1);
        assertEquals(GameConfig.MIN_GROUP_PLAYERS, session.getParticipantCount());

        session.setParticipantCount(99);
        assertEquals(GameConfig.MAX_GROUP_PLAYERS, session.getParticipantCount());
    }

    @Test
    public void testSetProposalMethod() {
        session.setProposalMethod(ProposalMethod.RESTAURANT_NAME);
        assertEquals(ProposalMethod.RESTAURANT_NAME, session.getProposalMethod());

        session.setProposalMethod(ProposalMethod.FOOD_TYPE);
        assertEquals(ProposalMethod.FOOD_TYPE, session.getProposalMethod());
    }

    @Test
    public void testStartSession() {
        session.setParticipantCount(4);
        session.setProposalMethod(ProposalMethod.RESTAURANT_NAME);

        assertTrue(session.startSession());
        assertEquals(GroupPhase.COLLECT, session.getPhase());
        assertEquals(0, session.getCurrentTurnIndex());
        assertEquals(1, session.getCurrentTurnNumber());
        assertEquals(4, session.getProposals().size());

        for (int i = 0; i < 4; i++) {
            assertTrue(session.getProposals().get(i).isEmpty());
        }
    }

    @Test
    public void testStartSessionOnlyAllowedInSetupPhase() {
        session.startSession();
        assertEquals(GroupPhase.COLLECT, session.getPhase());
        assertFalse(session.startSession());
    }

    @Test
    public void testSettingsLockedAfterStartSession() {
        session.setParticipantCount(3);
        session.setProposalMethod(ProposalMethod.FOOD_TYPE);
        session.startSession();

        // Attempts to modify settings in COLLECT phase should be ignored / return false
        session.setParticipantCount(5);
        assertEquals(3, session.getParticipantCount());

        assertFalse(session.incrementParticipantCount());
        assertEquals(3, session.getParticipantCount());

        assertFalse(session.decrementParticipantCount());
        assertEquals(3, session.getParticipantCount());

        session.setProposalMethod(ProposalMethod.RESTAURANT_NAME);
        assertEquals(ProposalMethod.FOOD_TYPE, session.getProposalMethod());
    }

    @Test
    public void testSubmitProposalAdvancesTurnAndFinalizes() {
        session.setParticipantCount(2);
        session.startSession();

        assertFalse(session.isLastTurn());

        List<String> player1Proposals = Arrays.asList("Ramen", "Sushi");
        assertTrue(session.submitProposal(player1Proposals));

        assertEquals(1, session.getCurrentTurnIndex());
        assertEquals(GroupPhase.COLLECT, session.getPhase());
        assertTrue(session.isLastTurn());
        assertEquals(player1Proposals, session.getProposals().get(0));

        List<String> player2Proposals = Collections.singletonList("Burger");
        assertTrue(session.submitProposal(player2Proposals));

        // On last turn, phase becomes FINAL and turn index stays at 1 (participantCount - 1)
        assertEquals(GroupPhase.FINAL, session.getPhase());
        assertEquals(1, session.getCurrentTurnIndex());
        assertEquals(player2Proposals, session.getProposals().get(1));

        // Submitting while in FINAL phase must return false
        assertFalse(session.submitProposal(Collections.singletonList("Pizza")));
    }

    @Test
    public void testFoodTypeModeProposals() {
        session.setParticipantCount(3);
        session.setProposalMethod(ProposalMethod.FOOD_TYPE);
        session.startSession();

        assertEquals(GroupPhase.COLLECT, session.getPhase());

        // Player 1 submits 1 food category
        assertTrue(session.submitProposal(Collections.singletonList("food_type_grill")));
        assertEquals(1, session.getProposals().get(0).size());
        assertEquals("food_type_grill", session.getProposals().get(0).get(0));

        // Player 2 skips turn (submits empty list)
        assertTrue(session.submitProposal(Collections.emptyList()));
        assertTrue(session.getProposals().get(1).isEmpty());

        // Player 3 submits 1 food category
        assertTrue(session.submitProposal(Collections.singletonList("food_type_hotpot")));
        assertEquals(1, session.getProposals().get(2).size());
        assertEquals("food_type_hotpot", session.getProposals().get(2).get(0));

        // All 3 turns done -> Phase becomes FINAL
        assertEquals(GroupPhase.FINAL, session.getPhase());
    }

    @Test
    public void testResetPreservesSettings() {
        session.setParticipantCount(5);
        session.setProposalMethod(ProposalMethod.RESTAURANT_NAME);
        session.startSession();

        assertEquals(GroupPhase.COLLECT, session.getPhase());
        assertEquals(5, session.getProposals().size());

        session.reset();

        assertEquals(GroupPhase.SETUP, session.getPhase());
        assertEquals(5, session.getParticipantCount());
        assertEquals(ProposalMethod.RESTAURANT_NAME, session.getProposalMethod());
        assertEquals(0, session.getCurrentTurnIndex());
        assertTrue(session.getProposals().isEmpty());
    }

    @Test
    public void testRestoreSuccess() {
        List<List<String>> props = Arrays.asList(
                Arrays.asList("KFC", "MK"),
                Collections.singletonList("Pizza"),
                Collections.emptyList(),
                Collections.emptyList()
        );

        assertTrue(session.restore(4, ProposalMethod.RESTAURANT_NAME, GroupPhase.COLLECT, 2, props));

        assertEquals(4, session.getParticipantCount());
        assertEquals(ProposalMethod.RESTAURANT_NAME, session.getProposalMethod());
        assertEquals(GroupPhase.COLLECT, session.getPhase());
        assertEquals(2, session.getCurrentTurnIndex());
        assertEquals(3, session.getCurrentTurnNumber());
        assertEquals(4, session.getProposals().size());
        assertEquals(Arrays.asList("KFC", "MK"), session.getProposals().get(0));
        assertEquals(Collections.singletonList("Pizza"), session.getProposals().get(1));
    }

    @Test
    public void testRestoreDeepCopy() {
        List<String> innerList = new ArrayList<>(Collections.singletonList("Sushi"));

        assertTrue(session.restore(2, ProposalMethod.FOOD_TYPE, GroupPhase.COLLECT, 0, Arrays.asList(innerList, Collections.emptyList())));

        // Modify local list after restore
        innerList.add("Ramen");

        assertEquals(1, session.getProposals().get(0).size());
        assertEquals("Sushi", session.getProposals().get(0).get(0));
    }

    @Test
    public void testRestoreInvalidParametersReturnsFalse() {
        List<List<String>> validProps = Arrays.asList(
                Collections.singletonList("A"),
                Collections.singletonList("B")
        );

        // Count out of bounds (< 2 or > 8)
        assertFalse(session.restore(1, ProposalMethod.FOOD_TYPE, GroupPhase.COLLECT, 0, validProps));
        assertFalse(session.restore(9, ProposalMethod.FOOD_TYPE, GroupPhase.COLLECT, 0, validProps));

        // Size mismatch
        assertFalse(session.restore(3, ProposalMethod.FOOD_TYPE, GroupPhase.COLLECT, 0, validProps));

        // Turn index out of bounds
        assertFalse(session.restore(2, ProposalMethod.FOOD_TYPE, GroupPhase.COLLECT, -1, validProps));
        assertFalse(session.restore(2, ProposalMethod.FOOD_TYPE, GroupPhase.COLLECT, 2, validProps));

        // Nulls
        assertFalse(session.restore(2, null, GroupPhase.COLLECT, 0, validProps));
        assertFalse(session.restore(2, ProposalMethod.FOOD_TYPE, null, 0, validProps));
        assertFalse(session.restore(2, ProposalMethod.FOOD_TYPE, GroupPhase.COLLECT, 0, null));

        // State remains untouched (default setup)
        assertEquals(GameConfig.MIN_GROUP_PLAYERS, session.getParticipantCount());
        assertEquals(GroupPhase.SETUP, session.getPhase());
    }
}
