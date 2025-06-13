package ConnectionDB;

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
    List<Map<String, Object>> getCitizensByMesa(int mesaId);
    Map<String, Object> getMesaConfiguration(int mesaId);
    Map<Integer, List<Map<String, Object>>> getCitizensByMesaBatch(List<Integer> mesaIds);
    List<Integer> getAllMesaIds();
    List<Integer> getMesaIdsByDepartment(int departmentId);
    Map<String, Object> getElectionInfo(int electionId);
    List<Map<String, Object>> getCandidatesByElection(int electionId);
    String getPoolStats();
    boolean isHealthy();
    Map<String, Object> getPerformanceMetrics();
    List<Map<String, Object>> getAllDepartments();
    List<Map<String, Object>> getMunicipalitiesByDepartment(int departmentId);
    List<Map<String, Object>> getPuestosByMunicipality(int municipalityId);
    Map<String, Object> getElectionConfigurationStats(int electionId);
    boolean validateElectionDataCompleteness(int electionId);
    Map<String, Object> getCitizenVotingAssignment(String documento);
    List<Map<String, Object>> searchCitizensByName(String nombre, String apellido, int limit);
    List<Map<String, Object>> getMesasByPuesto(int puestoId);
    Map<String, Object> getVotingStatsByDepartment(int electionId, int departmentId);
    Map<String, Object> getVotingStatsByMunicipality(int electionId, int municipalityId);
    Map<String, Object> getVotingStatsByPuesto(int electionId, int puestoId);
    Map<String, Object> getElectionResultsSummary(int electionId);
    List<Map<String, Object>> getAllActiveElections();
    boolean validateCitizenDocument(String documento);
    Map<String, Object> getLocationHierarchyByMesa(int mesaId);
// =================== MÉTODOS PARA FULL CITIZEN REPORTS ===================

    /**
     * Obtiene todos los ciudadanos de un departamento específico con información completa de ubicación
     */
    List<Map<String, Object>> getCitizensByDepartment(int departmentId);

    /**
     * Obtiene todos los ciudadanos de un municipio específico con información completa de ubicación
     */
    List<Map<String, Object>> getCitizensByMunicipality(int municipalityId);

    /**
     * Obtiene todos los ciudadanos de un puesto de votación específico con información completa de ubicación
     */
    List<Map<String, Object>> getCitizensByPuesto(int puestoId);


}