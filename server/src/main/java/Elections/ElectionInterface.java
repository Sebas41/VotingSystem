package Elections;

import Elections.models.Candidate;
import Elections.models.ELECTION_STATUS;
//import Elections.models.Vote;
import model.Vote;
import java.util.Date;
import java.util.List;

public interface ElectionInterface {


    void registerElection(int electionId, String name, Date startDate, Date endDate);
    String getElectionName();
    int getElectionId();
    Date getStartDate();
    Date getEndDate();
    ELECTION_STATUS getElectionStatus();
    void changeElectionStatus(ELECTION_STATUS newStatus);


    void addCandidate(int id, String name, String party);
    boolean editCandidate(int id, String newName, String newParty);
    boolean removeCandidate(int id);
    void loadCandidatesFromCSV(String filepath);
    List<Candidate> getCandidates();


    boolean isElectionActive();
    boolean isElectionClosed();

    String getElectionInfo();

    void addVoteToCandidate(int candidateId, Vote vote);

}
