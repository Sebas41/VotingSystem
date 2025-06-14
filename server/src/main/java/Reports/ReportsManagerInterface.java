// server/src/main/java/Reports/ReportsManagerInterface.java

package Reports;

import ReportsSystem.*;
import java.util.List;
import java.util.Map;

/**
 * Interface for Reports Manager - now extends ReportsCache for proxy compatibility
 * Provides both generation and file serving capabilities
 */
public interface ReportsManagerInterface extends ReportsCache {

    // =================== ORIGINAL GENERATION METHODS ===================

    CitizenReportsConfiguration generateCitizenReport(String documento, int electionId);
    List<CitizenReportsConfiguration> searchCitizenReports(String nombre, String apellido, int electionId, int limit);
    boolean validateCitizenEligibility(String documento);

    ElectionReportsConfiguration generateElectionResultsReport(int electionId);
    Map<String, Object> getElectionStatistics(int electionId);

    GeographicReportsConfiguration generateDepartmentReport(int departmentId, int electionId);
    GeographicReportsConfiguration generateMunicipalityReport(int municipalityId, int electionId);
    GeographicReportsConfiguration generatePuestoReport(int puestoId, int electionId);

    Map<String, CitizenReportsConfiguration> generateBatchCitizenReports(List<String> documentos, int electionId);
    List<CitizenReportsConfiguration> generateMesaCitizenReports(int mesaId, int electionId);

    // =================== FULL CITIZEN REPORTS GENERATION ===================

    Map<String, Object> generateDepartmentCitizenReports(int departmentId, int electionId);
    Map<String, Object> generateMunicipalityCitizenReports(int municipalityId, int electionId);
    Map<String, Object> generatePuestoCitizenReports(int puestoId, int electionId);

    // =================== CONFIGURATION AND EXPORT ===================

    boolean validateConfiguration(Object configuration);
    byte[] exportConfigurationToBytes(Object configuration);
    Object importConfigurationFromBytes(byte[] data, String configType);
    String exportConfigurationToJson(Object configuration);
    boolean saveConfigurationToFile(Object configuration, String filePath);
    Object loadConfigurationFromFile(String filePath, String configType);

    // =================== UTILITY METHODS ===================

    Map<String, Object> getReportsStatistics(int electionId);
    boolean isElectionReadyForReports(int electionId);
    List<Map<String, Object>> getAvailableElections();

    // =================== NEW FILE SERVING METHODS FOR PROXY ===================

    /**
     * Get existing citizen report file as ICE binary data
     * @param documento Citizen document ID
     * @param electionId Election ID
     * @return ICE binary data or null if not found
     */
    byte[] getCitizenReportFile(String documento, int electionId);

    /**
     * Get existing election report file as ICE binary data
     * @param electionId Election ID
     * @return ICE binary data or null if not found
     */
    byte[] getElectionReportFile(int electionId);

    /**
     * Get existing geographic report file as ICE binary data
     * @param locationId Location ID (department, municipality, or puesto)
     * @param locationType Type: "department", "municipality", "puesto"
     * @param electionId Election ID
     * @return ICE binary data or null if not found
     */
    byte[] getGeographicReportFile(int locationId, String locationType, int electionId);

    /**
     * List available report files for synchronization
     * @param reportType Type: "citizen", "election", "geographic", "all"
     * @param electionId Election ID (0 for all elections)
     * @return List of available file names
     */
    List<String> getAvailableReportFiles(String reportType, int electionId);

    /**
     * Get file metadata for synchronization
     * @param fileName File name
     * @return Map with file metadata (size, timestamp, etc.)
     */
    Map<String, Object> getReportFileMetadata(String fileName);

    /**
     * Check if specific report file exists
     * @param fileName File name to check
     * @return true if file exists
     */
    boolean reportFileExists(String fileName);

    /**
     * Get bulk report files for initial synchronization
     * @param fileNames List of file names to retrieve
     * @return Map of filename -> ICE binary data
     */
    Map<String, byte[]> getBulkReportFiles(List<String> fileNames);
}