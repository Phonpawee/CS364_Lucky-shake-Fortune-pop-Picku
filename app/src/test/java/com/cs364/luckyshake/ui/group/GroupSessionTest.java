package com.cs364.luckyshake.ui.group;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.cs364.luckyshake.logic.GameConfig;

import org.junit.Before;
import org.junit.Test;

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

        // Fill up to max
        for (int i = 3; i < GameConfig.MAX_GROUP_PLAYERS; i++) {
            assertTrue(session.incrementParticipantCount());
        }
        assertEquals(GameConfig.MAX_GROUP_PLAYERS, session.getParticipantCount());

        // Beyond max must return false and stay at max
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

        // Below min must return false and stay at min
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

        // Trying to start again while in COLLECT phase must return false
        assertFalse(session.startSession());
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
}
