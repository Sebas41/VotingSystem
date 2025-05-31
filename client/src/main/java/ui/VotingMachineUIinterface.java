package ui;

import votation.Candidate;

import java.util.List;

public interface VotingMachineUIinterface {

    void setCandidates(List<Candidate> candidates);
    void showLoginMessage(String message, boolean isError);
    void showVoteMessage(String message, boolean isError);
    void resetToLoginAfterVote();
}
