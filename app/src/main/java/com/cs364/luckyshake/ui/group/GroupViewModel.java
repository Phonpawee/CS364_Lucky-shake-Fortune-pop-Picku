package com.cs364.luckyshake.ui.group;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.cs364.luckyshake.logic.GameConfig;

/**
 * ViewModel for the group decision mode setup screen.
 * Retains state across configuration changes (such as screen rotations).
 */
public class GroupViewModel extends ViewModel {
    private final GroupSession session;

    private final MutableLiveData<Integer> participantCount = new MutableLiveData<>();
    private final MutableLiveData<Boolean> canIncrement = new MutableLiveData<>();
    private final MutableLiveData<Boolean> canDecrement = new MutableLiveData<>();
    private final MutableLiveData<ProposalMethod> proposalMethod = new MutableLiveData<>();
    private final MutableLiveData<GroupPhase> phase = new MutableLiveData<>();

    public GroupViewModel() {
        session = new GroupSession();
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
        updateLiveData();
    }

    public void incrementParticipantCount() {
        if (session.incrementParticipantCount()) {
            updateLiveData();
        }
    }

    public void decrementParticipantCount() {
        if (session.decrementParticipantCount()) {
            updateLiveData();
        }
    }

    public void setProposalMethod(ProposalMethod method) {
        session.setProposalMethod(method);
        updateLiveData();
    }

    public boolean startGroupSession() {
        boolean started = session.startSession();
        if (started) {
            updateLiveData();
        }
        return started;
    }

    public void resetSession() {
        session.reset();
        updateLiveData();
    }

    public GroupSession getSession() {
        return session;
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
