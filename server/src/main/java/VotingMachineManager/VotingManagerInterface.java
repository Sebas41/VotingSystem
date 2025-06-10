package VotingMachineManager;

import java.util.List;
import java.util.Map;

/**
 * Interface for managing voting machine configurations
 * Generates configuration packages that will be sent to proxy cache
 * and then distributed to individual voting machines
 */
public interface VotingManagerInterface {


    Map<String, Object> generateMachineConfiguration(int mesaId, int electionId);


    Map<Integer, Map<String, Object>> generateBatchMachineConfigurations(List<Integer> mesaIds, int electionId);


    Map<Integer, Map<String, Object>> generateDepartmentConfigurations(int departmentId, int electionId);


    Map<Integer, Map<String, Object>> generateAllMachineConfigurations(int electionId);


    boolean validateConfiguration(Map<String, Object> configuration);


    Map<String, Object> getConfigurationStatistics(int electionId);


    String exportConfigurationToJson(Map<String, Object> configuration);


    boolean isElectionReadyForConfiguration(int electionId);

    Map<Integer, Map<String, Object>> generatePuestoConfigurations(int puestoId, int electionId);


}