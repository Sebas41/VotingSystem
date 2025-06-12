package Controller;

import Elections.models.Candidate;
import Elections.models.ELECTION_STATUS;
import model.ReliableMessage;

import java.util.Date;
import java.util.List;
import java.util.Map;

public interface ServerControllerInterface {


    void registerVote(ReliableMessage newVote);
    String getElectionInfo();
    void createElection(int id, String name, Date start, Date end);
    void changeElectionStatus(ELECTION_STATUS status);
    void addCandidate(int id, String name, String party);
    void editCandidate(int id, String newName, String newParty);
    void removeCandidate(int id);
    void loadCandidatesFromCSV(String filepath);
    List<Candidate> getCandidates();
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
    String generateCitizenReport(String documento, int electionId);
    List<String> searchCitizenReports(String nombre, String apellido, int electionId, int limit);
    boolean validateCitizenEligibility(String documento);
    Map<String, String> generateBatchCitizenReports(List<String> documentos, int electionId);
    List<String> generateMesaCitizenReports(int mesaId, int electionId);
    String generateElectionResultsReport(int electionId);
    String getElectionStatistics(int electionId);
    List<Map<String, Object>> getAvailableElections();
    String generateDepartmentReport(int departmentId, int electionId);
    String generateMunicipalityReport(int municipalityId, int electionId);
    String generatePuestoReport(int puestoId, int electionId);
    String getReportsStatistics(int electionId);
    boolean isElectionReadyForReports(int electionId);
    String validateReportsSystem();
    boolean exportCitizenReport(String documento, int electionId, String filePath);
    boolean exportElectionResultsReport(int electionId, String filePath);
    boolean exportGeographicReport(int locationId, String locationType, int electionId, String filePath);
    void shutdown();
}