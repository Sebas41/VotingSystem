package ConnectionDB;

import model.Vote;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Complete interface for database operations including voting machine configuration
 * Optimized for handling 100M citizen records efficiently
 */
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
}