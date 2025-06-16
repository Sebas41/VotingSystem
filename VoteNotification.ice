#pragma once

module VoteNotification {

    interface VoteObserver {
        void onVoteReceived(string voteInfo);
        void onElectionResultsUpdated(string resultsData);
        bool ping();
    };

    interface VoteNotifier {
        void registerObserver(VoteObserver* observer, int electionId);
        void unregisterObserver(VoteObserver* observer, int electionId);
        int getObserverCount(int electionId);
        void forceResultsUpdate(int electionId);
    };

    exception VoteNotificationException {
        string reason;
        int errorCode;
    };
}