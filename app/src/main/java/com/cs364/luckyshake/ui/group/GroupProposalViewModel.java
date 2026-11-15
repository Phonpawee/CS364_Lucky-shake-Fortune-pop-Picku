package com.cs364.luckyshake.ui.group;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.cs364.luckyshake.logic.DrawEngine;

import java.util.Collections;
import java.util.List;

/**
 * ViewModel for the group proposal collection and winner draw screen (#36, #35, #37).
 * Retains state across screen rotations and manages proposal submissions and weighted winner draws.
 */
public class GroupProposalViewModel extends ViewModel {
    private final GroupSession session;

    private final MutableLiveData<Integer> currentTurnNumber = new MutableLiveData<>();
    private final MutableLiveData<Integer> participantCount = new MutableLiveData<>();
    private final MutableLiveData<GroupPhase> phase = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLastTurn = new MutableLiveData<>();
    private final MutableLiveData<List<List<String>>> proposals = new MutableLiveData<>();
    private final MutableLiveData<String> selectedFoodType = new MutableLiveData<>(null);

    private final MutableLiveData<List<VoteOption>> tally = new MutableLiveData<>(null);
    private final MutableLiveData<VoteOption> winner = new MutableLiveData<>(null);

    private DrawEngine drawEngine = DrawEngine.create();

    public GroupProposalViewModel() {
        session = new GroupSession();
        updateLiveData();
    }

    public void setDrawEngine(DrawEngine drawEngine) {
        if (drawEngine != null) {
            this.drawEngine = drawEngine;
        }
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

    public LiveData<String> getSelectedFoodType() {
        return selectedFoodType;
    }

    public LiveData<List<VoteOption>> getTally() {
        return tally;
    }

    public LiveData<VoteOption> getWinner() {
        return winner;
    }

    public boolean isTallyEmpty() {
        List<VoteOption> list = tally.getValue();
        return list == null || list.isEmpty();
    }

    /**
     * Draws the winning option weighted by votes.
     * Returns false if a winner was already drawn or if the tally is empty.
     */
    public boolean drawWinner() {
        if (winner.getValue() != null || isTallyEmpty()) {
            return false;
        }
        List<VoteOption> currentTally = tally.getValue();
        if (currentTally == null || currentTally.isEmpty()) {
            return false;
        }
        VoteOption picked = VoteTally.draw(currentTally, drawEngine);
        winner.setValue(picked);
        session.setPhase(GroupPhase.DONE);
        updateLiveData();
        return true;
    }

    /**
     * Updates the active food type selection for the current turn if valid.
     */
    public void selectFoodType(String key) {
        if (key != null && FoodTypes.isValid(key)) {
            selectedFoodType.setValue(key);
        }
    }

    /**
     * Submits the selected food category for the current participant.
     * Returns false if no valid food type is selected.
     */
    public boolean submitFoodType() {
        String key = selectedFoodType.getValue();
        if (key == null || !FoodTypes.isValid(key)) {
            return false;
        }
        boolean ok = session.submitProposal(Collections.singletonList(key));
        if (ok) {
            selectedFoodType.setValue(null);
            updateLiveData();
        }
        return ok;
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
        selectedFoodType.setValue(null);
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
        if (session.getPhase() == GroupPhase.FINAL || session.getPhase() == GroupPhase.DONE) {
            if (tally.getValue() == null) {
                tally.setValue(VoteTally.tally(session.getProposals()));
            }
        }

        currentTurnNumber.setValue(session.getCurrentTurnNumber());
        participantCount.setValue(session.getParticipantCount());
        proposals.setValue(session.getProposals());
        isLastTurn.setValue(session.isLastTurn());
        phase.setValue(session.getPhase());
    }
}
