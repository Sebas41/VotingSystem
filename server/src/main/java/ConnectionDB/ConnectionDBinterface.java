package ConnectionDB;

import Elections.models.Candidate;
//import Elections.models.Vote;
import model.Vote;
import java.util.Date;
import java.util.List;
import java.util.Map;

public interface ConnectionDBinterface {
    void storeVote(Vote newVote);
    void storeElection(int id, String name, Date start, Date end, String status);
    void storeCandidate(int id, String name, String party, int electionId);
    Map<Integer, Integer> getVotesPerCandidate(int electionId);
    Map<String, Map<Integer, Integer>> getVotesPerCandidateGroupedByMachine(int electionId);


    String getCandidateNameById(Integer key);
}
