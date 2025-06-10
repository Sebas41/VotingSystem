package Controller;

import Elections.models.Candidate;
import Elections.models.ELECTION_STATUS;
import model.ReliableMessage;

import java.util.Date;
import java.util.List;
import java.util.Map;

public interface ServerControllerInterface {

    // =================== EXISTING ELECTION METHODS ===================
    void registerVote(ReliableMessage newVote);
    String getElectionInfo();
    void createElection(int id, String name, Date start, Date end);
    void changeElectionStatus(ELECTION_STATUS status);
    void addCandidate(int id, String name, String party);
    void editCandidate(int id, String newName, String newParty);
    void removeCandidate(int id);
    void loadCandidatesFromCSV(String filepath);
    List<Candidate> getCandidates();
    String getTotalVotesPerCandidate(int electionId);
    String getVotesPerCandidateByMachine(int electionId);
    List<Map<String, Object>> getAvailableDepartments();
    List<Map<String, Object>> getMunicipalitiesByDepartment(int departmentId);
    List<Map<String, Object>> getPuestosByMunicipality(int municipalityId);
    List<Map<String, Object>> getMesasByPuesto(int puestoId);
    void generateSelectiveMesaConfigurations(List<Integer> mesaIds, int electionId, String testName);
    void generatePuestoConfiguration(int puestoId, int electionId, String testName);
    void generateDepartmentSampleConfiguration(int departmentId, int sampleSize, int electionId, String testName);
    String previewMesaConfiguration(int mesaId);
    List<String> getSelectiveTestHistory();
    String getSelectiveTestSummary(String testName);
    void generateAllVotingMachineConfigurations(int electionId);
    void generateDepartmentConfigurations(int departmentId, int electionId);
    String getConfigurationStatus();
    String getConfigurationStatistics(int electionId);
    boolean hasVotingMachineConfigurations(int electionId);
    String getMesaConfiguration(int mesaId, int electionId);
    String validateAllConfigurations(int electionId);
    void shutdown();

    void exportVotesPerMachineCSV(int electionId, String path);

    void exportElectionResultsCSV(int electionId, String path);
}