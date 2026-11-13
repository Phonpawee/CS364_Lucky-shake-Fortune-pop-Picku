package com.cs364.luckyshake.ui.group;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.Collections;
import java.util.List;

/**
 * ViewModel for the group proposal collection screen (#36).
 * Retains state across screen rotations and manages proposal submissions per participant.
 */
public class GroupProposalViewModel extends ViewModel {
    private final GroupSession session;

    private final MutableLiveData<Integer> currentTurnNumber = new MutableLiveData<>();
    private final MutableLiveData<Integer> participantCount = new MutableLiveData<>();
    private final MutableLiveData<GroupPhase> phase = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLastTurn = new MutableLiveData<>();
    private final MutableLiveData<List<List<String>>> proposals = new MutableLiveData<>();

    public GroupProposalViewModel() {
        session = new GroupSession();
        updateLiveData();
    }

    /**
     * Initializes the session with the specified count and method.
     * Starts the round ONLY if the session is currently in the SETUP phase,
     * ensuring screen rotations during turn 2+ do not reset the active session.
     */
    public void init(int count, ProposalMethod method) {
        if (session.getPhase() == GroupPhase.SETUP) {
            session.setParticipantCount(count);
            session.setProposalMethod(method);
            session.startSession();
        }
        updateLiveData();
    }

    public LiveData<Integer> getCurrentTurnNumber() {
        return currentTurnNumber;
    }

    public LiveData<Integer> getParticipantCount() {
        return participantCount;
    }

    public LiveData<GroupPhase> getPhase() {
        return phase;
    }

    public LiveData<Boolean> getIsLastTurn() {
        return isLastTurn;
    }

    public LiveData<List<List<String>>> getProposals() {
        return proposals;
    }

    /**
     * Normalizes raw restaurant inputs and submits them for the current participant.
     * Returns false if normalization yields an empty list (all fields empty/whitespace).
     */
    public boolean submitRestaurants(List<String> raw) {
        List<String> normalized = RestaurantNames.normalize(raw);
        if (normalized.isEmpty()) {
            return false;
        }
        boolean ok = session.submitProposal(normalized);
        if (ok) {
            updateLiveData();
        }
        return ok;
    }

    /**
     * Submits an empty proposal list for the current participant to skip their turn.
     */
    public boolean skipTurn() {
        boolean ok = session.submitProposal(Collections.emptyList());
        if (ok) {
            updateLiveData();
        }
        return ok;
    }

    public GroupSession getSession() {
        return session;
    }

    private void updateLiveData() {
        currentTurnNumber.setValue(session.getCurrentTurnNumber());
        participantCount.setValue(session.getParticipantCount());
        phase.setValue(session.getPhase());
        isLastTurn.setValue(session.isLastTurn());
        proposals.setValue(session.getProposals());
    }
}
