package com.cs364.luckyshake.ui.group;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import com.cs364.luckyshake.logic.GameConfig;

/**
 * ViewModel for the group decision mode setup screen.
 * Retains state across process death and configuration changes (such as screen rotations).
 */
public class GroupViewModel extends ViewModel {
    private static final String KEY_COUNT = "participant_count";
    private static final String KEY_METHOD = "proposal_method";

    private final GroupSession session;
    private final SavedStateHandle state;

    private final MutableLiveData<Integer> participantCount = new MutableLiveData<>();
    private final MutableLiveData<Boolean> canIncrement = new MutableLiveData<>();
    private final MutableLiveData<Boolean> canDecrement = new MutableLiveData<>();
    private final MutableLiveData<ProposalMethod> proposalMethod = new MutableLiveData<>();
    private final MutableLiveData<GroupPhase> phase = new MutableLiveData<>();

    public GroupViewModel() {
        this(new SavedStateHandle());
    }

    public GroupViewModel(@NonNull SavedStateHandle state) {
        this.state = state;
        session = new GroupSession();

        Integer savedCount = state.get(KEY_COUNT);
        if (savedCount != null) {
            session.setParticipantCount(savedCount);
        }

        String savedMethod = state.get(KEY_METHOD);
        if (savedMethod != null) {
            try {
                session.setProposalMethod(ProposalMethod.valueOf(savedMethod));
            } catch (Exception ignored) {
            }
        }

        updateLiveData();
    }

    public LiveData<Integer> getParticipantCount() {
        return participantCount;
    }

    public LiveData<Boolean> getCanIncrement() {
        return canIncrement;
    }

    public LiveData<Boolean> getCanDecrement() {
        return canDecrement;
    }

    public LiveData<ProposalMethod> getProposalMethod() {
        return proposalMethod;
    }

    public LiveData<GroupPhase> getPhase() {
        return phase;
    }

    public void setParticipantCount(int count) {
        session.setParticipantCount(count);
        saveState();
        updateLiveData();
    }

    public void incrementParticipantCount() {
        if (session.incrementParticipantCount()) {
            saveState();
            updateLiveData();
        }
    }

    public void decrementParticipantCount() {
        if (session.decrementParticipantCount()) {
            saveState();
            updateLiveData();
        }
    }

    public void setProposalMethod(ProposalMethod method) {
        session.setProposalMethod(method);
        saveState();
        updateLiveData();
    }

    public boolean startGroupSession() {
        boolean started = session.startSession();
        if (started) {
            saveState();
            updateLiveData();
        }
        return started;
    }

    public void resetSession() {
        session.reset();
        saveState();
        updateLiveData();
    }

    public GroupSession getSession() {
        return session;
    }

    private void saveState() {
        state.set(KEY_COUNT, session.getParticipantCount());
        state.set(KEY_METHOD, session.getProposalMethod() != null ? session.getProposalMethod().name() : null);
    }

    private void updateLiveData() {
        int count = session.getParticipantCount();
        participantCount.setValue(count);
        canDecrement.setValue(count > GameConfig.MIN_GROUP_PLAYERS);
        canIncrement.setValue(count < GameConfig.MAX_GROUP_PLAYERS);
        proposalMethod.setValue(session.getProposalMethod());
        phase.setValue(session.getPhase());
    }
}
