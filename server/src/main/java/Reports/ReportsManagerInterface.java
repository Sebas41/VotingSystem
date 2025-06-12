package Reports;

import ReportsSystem.*;
import java.util.List;
import java.util.Map;

/**
 * Interface for Reports Manager - handles citizen lookup and election reporting
 * Uses Ice serialization for efficient binary storage and transmission
 */
public interface ReportsManagerInterface {

    // =================== CITIZEN LOOKUP REPORTS ===================

    /**
     * Generate citizen voting assignment report (where to vote)
     * @param documento Citizen document ID
     * @param electionId Election ID
     * @return CitizenReportsConfiguration with location assignment
     */
    CitizenReportsConfiguration generateCitizenReport(String documento, int electionId);

    /**
     * Search citizens by name and generate their reports
     * @param nombre Citizen first name (partial match)
     * @param apellido Citizen last name (partial match)
     * @param electionId Election ID
     * @param limit Maximum results to return
     * @return List of CitizenReportsConfiguration
     */
    List<CitizenReportsConfiguration> searchCitizenReports(String nombre, String apellido, int electionId, int limit);

    /**
     * Validate if citizen document exists and can vote
     * @param documento Citizen document ID
     * @return true if citizen is registered and assigned
     */
    boolean validateCitizenEligibility(String documento);

    // =================== ELECTION REPORTS ===================

    /**
     * Generate real-time election results report
     * @param electionId Election ID
     * @return ElectionReportsConfiguration with current results
     */
    ElectionReportsConfiguration generateElectionResultsReport(int electionId);

    /**
     * Get election summary statistics
     * @param electionId Election ID
     * @return Map with election statistics
     */
    Map<String, Object> getElectionStatistics(int electionId);

    // =================== GEOGRAPHIC REPORTS ===================

    /**
     * Generate geographic voting statistics by department
     * @param departmentId Department ID
     * @param electionId Election ID
     * @return GeographicReportsConfiguration with department stats
     */
    GeographicReportsConfiguration generateDepartmentReport(int departmentId, int electionId);

    /**
     * Generate geographic voting statistics by municipality
     * @param municipalityId Municipality ID
     * @param electionId Election ID
     * @return GeographicReportsConfiguration with municipality stats
     */
    GeographicReportsConfiguration generateMunicipalityReport(int municipalityId, int electionId);

    /**
     * Generate geographic voting statistics by puesto
     * @param puestoId Puesto ID
     * @param electionId Election ID
     * @return GeographicReportsConfiguration with puesto stats
     */
    GeographicReportsConfiguration generatePuestoReport(int puestoId, int electionId);

    // =================== BATCH OPERATIONS ===================

    /**
     * Generate citizen reports for multiple documents
     * @param documentos List of citizen document IDs
     * @param electionId Election ID
     * @return Map of documento -> CitizenReportsConfiguration
     */
    Map<String, CitizenReportsConfiguration> generateBatchCitizenReports(List<String> documentos, int electionId);

    /**
     * Generate reports for all citizens in a specific mesa
     * @param mesaId Mesa ID
     * @param electionId Election ID
     * @return List of CitizenReportsConfiguration for all citizens in mesa
     */
    List<CitizenReportsConfiguration> generateMesaCitizenReports(int mesaId, int electionId);

    // =================== CONFIGURATION AND EXPORT ===================

    /**
     * Validate reports configuration
     * @param configuration Any reports configuration
     * @return true if configuration is valid
     */
    boolean validateConfiguration(Object configuration);

    /**
     * Export reports configuration to binary Ice format
     * @param configuration Reports configuration object
     * @return byte array with Ice serialized data
     */
    byte[] exportConfigurationToBytes(Object configuration);

    /**
     * Import reports configuration from binary Ice format
     * @param data Ice serialized byte array
     * @param configType Type of configuration ("citizen", "election", "geographic")
     * @return Reports configuration object
     */
    Object importConfigurationFromBytes(byte[] data, String configType);

    /**
     * Export reports configuration to JSON (for debugging/viewing)
     * @param configuration Reports configuration object
     * @return JSON string representation
     */
    String exportConfigurationToJson(Object configuration);

    /**
     * Save configuration to file
     * @param configuration Reports configuration
     * @param filePath File path to save
     * @return true if successful
     */
    boolean saveConfigurationToFile(Object configuration, String filePath);

    /**
     * Load configuration from file
     * @param filePath File path to load from
     * @param configType Type of configuration expected
     * @return Reports configuration object
     */
    Object loadConfigurationFromFile(String filePath, String configType);

    // =================== UTILITY METHODS ===================

    /**
     * Get reports generation statistics
     * @param electionId Election ID
     * @return Statistics about reports system
     */
    Map<String, Object> getReportsStatistics(int electionId);

    /**
     * Check if election is ready for reports generation
     * @param electionId Election ID
     * @return true if election is configured and ready
     */
    boolean isElectionReadyForReports(int electionId);

    /**
     * Get all active elections available for reports
     * @return List of active elections
     */
    List<Map<String, Object>> getAvailableElections();
}