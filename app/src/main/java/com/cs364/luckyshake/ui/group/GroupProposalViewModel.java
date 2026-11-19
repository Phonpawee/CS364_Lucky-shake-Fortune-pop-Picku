package com.cs364.luckyshake.ui.group;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import com.cs364.luckyshake.data.HistoryEntry;
import com.cs364.luckyshake.fortune.FortuneProvider;
import com.cs364.luckyshake.logic.DrawEngine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * ViewModel for the group proposal collection and winner draw screen (#36, #35, #37, #38, #39).
 * Retains and restores state across process death and screen rotations using {@link SavedStateHandle}.
 */
public class GroupProposalViewModel extends ViewModel {

    public interface HistoryRecorder {
        boolean add(HistoryEntry entry);
    }

    private static final String KEY_COUNT = "count";
    private static final String KEY_METHOD = "method";
    private static final String KEY_PHASE = "phase";
    private static final String KEY_TURN_INDEX = "turnIndex";
    private static final String KEY_PROPOSALS = "proposals";
    private static final String KEY_SELECTED_FOOD_TYPE = "selectedFoodType";
    private static final String KEY_WINNER_LABEL = "winnerLabel";
    private static final String KEY_EXIT_CONFIRM_SHOWING = "exitConfirmShowing";
    private static final String KEY_DRAW_LEVEL = "drawLevel";
    private static final String KEY_STICK_NO = "stickNo";
    private static final String KEY_HISTORY_LOGGED = "historyLogged";
    private static final String KEY_PENDING_WINNER_LABEL = "pendingWinnerLabel";

    private final GroupSession session;
    private final SavedStateHandle savedStateHandle;

    private final MutableLiveData<Integer> currentTurnNumber = new MutableLiveData<>();
    private final MutableLiveData<Integer> participantCount = new MutableLiveData<>();
    private final MutableLiveData<GroupPhase> phase = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLastTurn = new MutableLiveData<>();
    private final MutableLiveData<List<List<String>>> proposals = new MutableLiveData<>();
    private final MutableLiveData<String> selectedFoodType = new MutableLiveData<>(null);

    private final MutableLiveData<List<VoteOption>> tally = new MutableLiveData<>(null);
    private final MutableLiveData<VoteOption> winner = new MutableLiveData<>(null);

    private DrawEngine drawEngine = DrawEngine.create();
    private HistoryRecorder historyRecorder = entry -> false;
    private FortuneProvider fortuneProvider;

    public GroupProposalViewModel() {
        this(new SavedStateHandle());
    }

    public GroupProposalViewModel(@NonNull SavedStateHandle savedStateHandle) {
        this.savedStateHandle = savedStateHandle;
        session = new GroupSession();

        if (savedStateHandle.contains(KEY_PHASE)) {
            restoreState();
        }
        updateLiveData();
    }

    public void setDrawEngine(DrawEngine drawEngine) {
        if (drawEngine != null) {
            this.drawEngine = drawEngine;
        }
    }

    public void setHistoryRecorder(HistoryRecorder historyRecorder) {
        if (historyRecorder != null) {
            this.historyRecorder = historyRecorder;
        }
    }

    public void setFortuneProvider(FortuneProvider fortuneProvider) {
        this.fortuneProvider = fortuneProvider;
    }

    /**
     * Initializes the session with the specified count and method.
     * Starts the round ONLY if the session is currently in the SETUP phase,
     * ensuring process restoration or screen rotations during turn 2+ do not reset the active session.
     */
    public void init(int count, ProposalMethod method) {
        if (session.getPhase() == GroupPhase.SETUP) {
            session.setParticipantCount(count);
            session.setProposalMethod(method);
            session.startSession();
            saveState();
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

    public int getDrawLevel() {
        Integer level = savedStateHandle.get(KEY_DRAW_LEVEL);
        return level != null ? level : 0;
    }

    public int getStickNo() {
        Integer stick = savedStateHandle.get(KEY_STICK_NO);
        return stick != null ? stick : 1;
    }

    public boolean isTallyEmpty() {
        List<VoteOption> list = tally.getValue();
        return list == null || list.isEmpty();
    }

    /**
     * Returns true if an active round is in progress without a drawn winner (#38).
     */
    public boolean hasUnsavedProgress() {
        GroupPhase currentPhase = session.getPhase();
        return currentPhase == GroupPhase.COLLECT || currentPhase == GroupPhase.FINAL;
    }

    public boolean isExitConfirmShowing() {
        Boolean b = savedStateHandle.get(KEY_EXIT_CONFIRM_SHOWING);
        return b != null && b;
    }

    public void setExitConfirmShowing(boolean showing) {
        savedStateHandle.set(KEY_EXIT_CONFIRM_SHOWING, showing);
    }

    /**
     * Cancels current active round, resetting session back to SETUP while preserving participant count & method (#38).
     * Clears all temporary state and SavedStateHandle keys to prevent leftover data.
     */
    public void cancelRound() {
        session.reset();
        selectedFoodType.setValue(null);
        tally.setValue(null);
        winner.setValue(null);

        savedStateHandle.remove(KEY_COUNT);
        savedStateHandle.remove(KEY_METHOD);
        savedStateHandle.remove(KEY_PHASE);
        savedStateHandle.remove(KEY_TURN_INDEX);
        savedStateHandle.remove(KEY_PROPOSALS);
        savedStateHandle.remove(KEY_SELECTED_FOOD_TYPE);
        savedStateHandle.remove(KEY_WINNER_LABEL);
        savedStateHandle.remove(KEY_EXIT_CONFIRM_SHOWING);
        savedStateHandle.remove(KEY_DRAW_LEVEL);
        savedStateHandle.remove(KEY_STICK_NO);
        savedStateHandle.remove(KEY_HISTORY_LOGGED);
        savedStateHandle.remove(KEY_PENDING_WINNER_LABEL);

        updateLiveData();
    }

    /**
     * Draws the winning option weighted by votes (#37, #39).
     * Logs history entry exactly once and persists draw level and stick number.
     * Returns false if a winner was already drawn, if history save fails, or if the tally is empty.
     */
    public boolean drawWinner() {
        if (winner.getValue() != null || isTallyEmpty()) {
            return false;
        }
        List<VoteOption> currentTally = tally.getValue();
        if (currentTally == null || currentTally.isEmpty()) {
            return false;
        }

        String pendingLabel = savedStateHandle.get(KEY_PENDING_WINNER_LABEL);
        VoteOption picked = null;
        if (pendingLabel != null) {
            for (VoteOption opt : currentTally) {
                if (opt.label.equals(pendingLabel)) {
                    picked = opt;
                    break;
                }
            }
        }
        if (picked == null) {
            picked = VoteTally.draw(currentTally, drawEngine);
            savedStateHandle.set(KEY_PENDING_WINNER_LABEL, picked.label);
        }

        int drawLevel = savedStateHandle.contains(KEY_DRAW_LEVEL) ? getDrawLevel() : drawEngine.pickLevel();
        int stickNo = savedStateHandle.contains(KEY_STICK_NO) ? getStickNo() : (1 + (int) (Math.random() * 28));

        savedStateHandle.set(KEY_DRAW_LEVEL, drawLevel);
        savedStateHandle.set(KEY_STICK_NO, stickNo);

        if (!Boolean.TRUE.equals(savedStateHandle.get(KEY_HISTORY_LOGGED))) {
            String nameTh;
            String nameEn;
            if (session.getProposalMethod() == ProposalMethod.FOOD_TYPE && fortuneProvider != null) {
                String th = fortuneProvider.getFoodTypeName(picked.label, Locale.forLanguageTag("th"));
                String en = fortuneProvider.getFoodTypeName(picked.label, Locale.ENGLISH);
                nameTh = th != null ? th : picked.label;
                nameEn = en != null ? en : picked.label;
            } else {
                nameTh = picked.label;
                nameEn = picked.label;
            }

            HistoryEntry entry = GroupWinnerEntry.create(nameTh, nameEn, drawLevel, stickNo, System.currentTimeMillis());
            boolean success = historyRecorder.add(entry);
            if (!success) {
                return false;
            }
            savedStateHandle.set(KEY_HISTORY_LOGGED, true);
        }

        winner.setValue(picked);
        session.setPhase(GroupPhase.DONE);
        saveState();
        updateLiveData();
        return true;
    }

    /**
     * Updates the active food type selection for the current turn if valid.
     */
    public void selectFoodType(String key) {
        if (key != null && FoodTypes.isValid(key)) {
            selectedFoodType.setValue(key);
            saveState();
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
            saveState();
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
            saveState();
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
            saveState();
            updateLiveData();
        }
        return ok;
    }

    public GroupSession getSession() {
        return session;
    }

    private void restoreState() {
        Integer count = savedStateHandle.get(KEY_COUNT);
        String methodStr = savedStateHandle.get(KEY_METHOD);
        String phaseStr = savedStateHandle.get(KEY_PHASE);
        Integer turnIndex = savedStateHandle.get(KEY_TURN_INDEX);
        ArrayList<ArrayList<String>> props = savedStateHandle.get(KEY_PROPOSALS);

        ProposalMethod method = methodStr != null ? ProposalMethod.valueOf(methodStr) : ProposalMethod.FOOD_TYPE;
        GroupPhase restoredPhase = phaseStr != null ? GroupPhase.valueOf(phaseStr) : GroupPhase.SETUP;
        int pCount = count != null ? count : 2;
        int index = turnIndex != null ? turnIndex : 0;

        List<List<String>> proposalsList = new ArrayList<>();
        if (props != null) {
            for (ArrayList<String> inner : props) {
                proposalsList.add(inner != null ? new ArrayList<>(inner) : new ArrayList<>());
            }
        }

        boolean ok = session.restore(pCount, method, restoredPhase, index, proposalsList);
        if (ok) {
            String foodType = savedStateHandle.get(KEY_SELECTED_FOOD_TYPE);
            selectedFoodType.setValue(foodType);

            if (restoredPhase == GroupPhase.FINAL || restoredPhase == GroupPhase.DONE) {
                List<VoteOption> computedTally = VoteTally.tally(session.getProposals());
                tally.setValue(computedTally);

                String winnerLabel = savedStateHandle.get(KEY_WINNER_LABEL);
                if (winnerLabel != null && computedTally != null) {
                    for (VoteOption opt : computedTally) {
                        if (opt.label.equals(winnerLabel)) {
                            winner.setValue(opt);
                            break;
                        }
                    }
                }
            }
        }
    }

    private void saveState() {
        savedStateHandle.set(KEY_COUNT, session.getParticipantCount());
        savedStateHandle.set(KEY_METHOD, session.getProposalMethod() != null ? session.getProposalMethod().name() : null);
        savedStateHandle.set(KEY_PHASE, session.getPhase() != null ? session.getPhase().name() : null);
        savedStateHandle.set(KEY_TURN_INDEX, session.getCurrentTurnIndex());

        ArrayList<ArrayList<String>> props = new ArrayList<>();
        for (List<String> list : session.getProposals()) {
            props.add(list != null ? new ArrayList<>(list) : new ArrayList<>());
        }
        savedStateHandle.set(KEY_PROPOSALS, props);
        savedStateHandle.set(KEY_SELECTED_FOOD_TYPE, selectedFoodType.getValue());

        VoteOption w = winner.getValue();
        savedStateHandle.set(KEY_WINNER_LABEL, w != null ? w.label : null);
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
