package com.cs364.luckyshake.ui.group;

import com.cs364.luckyshake.logic.GameConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Core domain state for a group decision session.
 * Stores participant count, proposal method, current turn index (0-based), collected proposals,
 * and current session phase.
 */
public class GroupSession {
    private int participantCount;
    private ProposalMethod proposalMethod;
    private int currentTurnIndex;
    private final List<List<String>> proposals;
    private GroupPhase phase;

    public GroupSession() {
        this.participantCount = GameConfig.MIN_GROUP_PLAYERS;
        this.proposalMethod = ProposalMethod.FOOD_TYPE;
        this.currentTurnIndex = 0;
        this.proposals = new ArrayList<>();
        this.phase = GroupPhase.SETUP;
    }

    public int getParticipantCount() {
        return participantCount;
    }

    public void setParticipantCount(int count) {
        this.participantCount = Math.max(GameConfig.MIN_GROUP_PLAYERS,
                Math.min(GameConfig.MAX_GROUP_PLAYERS, count));
    }

    public boolean incrementParticipantCount() {
        if (participantCount < GameConfig.MAX_GROUP_PLAYERS) {
            participantCount++;
            return true;
        }
        return false;
    }

    public boolean decrementParticipantCount() {
        if (participantCount > GameConfig.MIN_GROUP_PLAYERS) {
            participantCount--;
            return true;
        }
        return false;
    }

    public ProposalMethod getProposalMethod() {
        return proposalMethod;
    }

    public void setProposalMethod(ProposalMethod proposalMethod) {
        if (proposalMethod != null) {
            this.proposalMethod = proposalMethod;
        }
    }

    public int getCurrentTurnIndex() {
        return currentTurnIndex;
    }

    /**
     * Returns 1-based turn number for UI display (currentTurnIndex + 1).
     */
    public int getCurrentTurnNumber() {
        return currentTurnIndex + 1;
    }

    public GroupPhase getPhase() {
        return phase;
    }

    public void setPhase(GroupPhase phase) {
        if (phase != null) {
            this.phase = phase;
        }
    }

    public List<List<String>> getProposals() {
        return Collections.unmodifiableList(proposals);
    }

    /**
     * Initializes a new group session round.
     * Prepares empty proposal lists for each participant and resets turn to 0.
     * Can only be started from the SETUP phase.
     */
    public boolean startSession() {
        if (phase != GroupPhase.SETUP) {
            return false;
        }
        phase = GroupPhase.COLLECT;
        currentTurnIndex = 0;
        proposals.clear();
        for (int i = 0; i < participantCount; i++) {
            proposals.add(new ArrayList<>());
        }
        return true;
    }

    /**
     * Resets the session back to SETUP phase while preserving participant count and proposal method.
     */
    public void reset() {
        phase = GroupPhase.SETUP;
        currentTurnIndex = 0;
        proposals.clear();
    }
}
