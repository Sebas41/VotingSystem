package Controller;

import Elections.models.ELECTION_STATUS;
import configuration.ConfigurationSender;
import Reports.VoteNotifierImpl;
import model.ReliableMessage;
import Controller.ServerControllerImpl.ElectionResult;

import java.util.Date;

public interface ServerControllerInterface {


    ElectionResult createElection(String name, Date startDate, Date endDate);
    ElectionResult getElectionInfo(int electionId);
    ElectionResult changeElectionStatus(int electionId, ELECTION_STATUS newStatus);
    ElectionResult getAllElections();
    ElectionResult addCandidate(int electionId, String name, String party);
    ElectionResult getCandidates(int electionId);
    ElectionResult loadCandidatesFromCSV(int electionId, String csvFilePath);
    ElectionResult sendConfigurationToMesa(int mesaId, int electionId);
    ElectionResult sendConfigurationToDepartment(int departmentId, int electionId);
    ElectionResult getMesaConfigurationStatus(int mesaId);
    ElectionResult startVoting(int electionId);
    ElectionResult stopVoting(int electionId);
    ElectionResult resetVoting(int electionId);
    ElectionResult registerVote(ReliableMessage voteMessage);
    ElectionResult getCitizenReport(String documento, int electionId);
    ElectionResult searchCitizens(String nombre, String apellido, int limit);
    ElectionResult getElectionResults(int electionId);
    ElectionResult getDepartmentReport(int departmentId, int electionId);
    ElectionResult getSystemStatus();
    ElectionResult runSystemDiagnostic();
    ElectionResult getPerformanceStatistics();
    void setConfigurationSender(ConfigurationSender configurationSender);
    void setVoteNotifier(VoteNotifierImpl voteNotifier);



    void shutdown();
}