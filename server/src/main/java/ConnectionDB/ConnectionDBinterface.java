package ConnectionDB;

import Elections.models.Vote;

import java.util.Date;

public interface ConnectionDBinterface {
    void storeVote(Vote newVote);
    void storeElection(int id, String name, Date start, Date end, String status);
    void storeCandidate(int id, String name, String party, int electionId);
}
