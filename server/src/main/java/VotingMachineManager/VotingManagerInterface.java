package VotingMachineManager;

import VotingSystem.VotingConfiguration;
import java.util.List;
import java.util.Map;

/**
 * Interface for managing voting machine configurations
 * Generates configuration packages that will be sent to proxy cache
 * and then distributed to individual voting machines using Ice serialization
 */
public interface VotingManagerInterface {

    /**
     * Generate Ice-serialized configuration for a single mesa
     */
    VotingConfiguration generateMachineConfiguration(int mesaId, int electionId);

    /**
     * Generate Ice-serialized configurations for multiple mesas in batch
     */
    Map<Integer, VotingConfiguration> generateBatchMachineConfigurations(List<Integer> mesaIds, int electionId);

    /**
     * Generate Ice-serialized configurations for all mesas in a department
     */
    Map<Integer, VotingConfiguration> generateDepartmentConfigurations(int departmentId, int electionId);

    /**
     * Generate Ice-serialized configurations for all mesas in the system
     */
    Map<Integer, VotingConfiguration> generateAllMachineConfigurations(int electionId);

    /**
     * Generate Ice-serialized configurations for all mesas in a specific puesto
     */
    Map<Integer, VotingConfiguration> generatePuestoConfigurations(int puestoId, int electionId);

    /**
     * Validate an Ice voting configuration
     */
    boolean validateConfiguration(VotingConfiguration configuration);

    /**
     * Export Ice configuration to bytes for storage/transmission
     */
    byte[] exportConfigurationToBytes(VotingConfiguration configuration);

    /**
     * Import Ice configuration from bytes
     */
    VotingConfiguration importConfigurationFromBytes(byte[] data);

    /**
     * Export Ice configuration to JSON for debugging/inspection
     */
    String exportConfigurationToJson(VotingConfiguration configuration);

    /**
     * Get configuration statistics for an election
     */
    Map<String, Object> getConfigurationStatistics(int electionId);

    /**
     * Check if election is ready for configuration generation
     */
    boolean isElectionReadyForConfiguration(int electionId);

    /**
     * Save Ice configuration to file system as bytes
     */
    boolean saveConfigurationToFile(VotingConfiguration configuration, String filePath);

    /**
     * Load Ice configuration from file system
     */
    VotingConfiguration loadConfigurationFromFile(String filePath);
}