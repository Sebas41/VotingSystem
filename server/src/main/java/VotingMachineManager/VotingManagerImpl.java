package VotingMachineManager;

import ConnectionDB.ConnectionDBinterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


public class VotingManagerImpl implements VotingManagerInterface {

    private static final Logger logger = LoggerFactory.getLogger(VotingManagerImpl.class);
    private final ConnectionDBinterface connectionDB;
    private final ObjectMapper jsonMapper;

    // Configuration package structure constants
    private static final String ELECTION_INFO = "electionInfo";
    private static final String CANDIDATES = "candidates";
    private static final String ASSIGNED_CITIZENS = "assignedCitizens";
    private static final String MESA_INFO = "mesaInfo";
    private static final String GENERATION_TIMESTAMP = "generationTimestamp";
    private static final String PACKAGE_VERSION = "packageVersion";

    public VotingManagerImpl(ConnectionDBinterface connectionDB) {
        this.connectionDB = connectionDB;
        this.jsonMapper = new ObjectMapper();
        this.jsonMapper.enable(SerializationFeature.INDENT_OUTPUT);

        logger.info("VotingManagerImpl initialized with optimized ConnectionDB");
    }

    @Override
    public Map<String, Object> generateMachineConfiguration(int mesaId, int electionId) {
        logger.info("Generating configuration for mesa {} and election {}", mesaId, electionId);

        try {
            Map<String, Object> configuration = new HashMap<>();

            // 1. Get mesa information (location, etc.)
            Map<String, Object> mesaInfo = connectionDB.getMesaConfiguration(mesaId);
            if (mesaInfo == null) {
                logger.error("Mesa {} not found", mesaId);
                return null;
            }

            // 2. Get election information
            Map<String, Object> electionInfo = connectionDB.getElectionInfo(electionId);
            if (electionInfo == null) {
                logger.error("Election {} not found", electionId);
                return null;
            }

            // 3. Get candidates for this election
            List<Map<String, Object>> candidates = connectionDB.getCandidatesByElection(electionId);

            // 4. Get assigned citizens for this mesa
            List<Map<String, Object>> assignedCitizens = connectionDB.getCitizensByMesa(mesaId);

            // 5. Build complete configuration package
            configuration.put(MESA_INFO, mesaInfo);
            configuration.put(ELECTION_INFO, electionInfo);
            configuration.put(CANDIDATES, candidates);
            configuration.put(ASSIGNED_CITIZENS, assignedCitizens);
            configuration.put(GENERATION_TIMESTAMP, new Date());
            configuration.put(PACKAGE_VERSION, "1.0");

            // Add summary statistics
            Map<String, Object> summary = new HashMap<>();
            summary.put("totalCandidates", candidates.size());
            summary.put("totalAssignedCitizens", assignedCitizens.size());
            summary.put("mesaId", mesaId);
            summary.put("electionId", electionId);
            configuration.put("summary", summary);

            logger.info("Configuration generated for mesa {} - {} citizens, {} candidates",
                    mesaId, assignedCitizens.size(), candidates.size());

            return configuration;

        } catch (Exception e) {
            logger.error("Error generating configuration for mesa {} and election {}", mesaId, electionId, e);
            return null;
        }
    }

    @Override
    public Map<Integer, Map<String, Object>> generateBatchMachineConfigurations(List<Integer> mesaIds, int electionId) {
        logger.info("Generating batch configurations for {} mesas and election {}", mesaIds.size(), electionId);

        Map<Integer, Map<String, Object>> batchConfigurations = new ConcurrentHashMap<>();

        try {
            // 1. Get election info and candidates once (they're the same for all machines)
            Map<String, Object> electionInfo = connectionDB.getElectionInfo(electionId);
            if (electionInfo == null) {
                logger.error("Election {} not found", electionId);
                return batchConfigurations;
            }

            List<Map<String, Object>> candidates = connectionDB.getCandidatesByElection(electionId);

            // 2. Get citizens for all mesas in batch (optimized query)
            Map<Integer, List<Map<String, Object>>> citizensByMesa = connectionDB.getCitizensByMesaBatch(mesaIds);

            // 3. Generate configurations for each mesa
            for (Integer mesaId : mesaIds) {
                try {
                    Map<String, Object> mesaInfo = connectionDB.getMesaConfiguration(mesaId);
                    if (mesaInfo == null) {
                        logger.warn("Mesa {} not found, skipping", mesaId);
                        continue;
                    }

                    List<Map<String, Object>> assignedCitizens = citizensByMesa.getOrDefault(mesaId, new ArrayList<>());

                    // Build configuration package
                    Map<String, Object> configuration = new HashMap<>();
                    configuration.put(MESA_INFO, mesaInfo);
                    configuration.put(ELECTION_INFO, electionInfo);
                    configuration.put(CANDIDATES, candidates);
                    configuration.put(ASSIGNED_CITIZENS, assignedCitizens);
                    configuration.put(GENERATION_TIMESTAMP, new Date());
                    configuration.put(PACKAGE_VERSION, "1.0");

                    // Add summary
                    Map<String, Object> summary = new HashMap<>();
                    summary.put("totalCandidates", candidates.size());
                    summary.put("totalAssignedCitizens", assignedCitizens.size());
                    summary.put("mesaId", mesaId);
                    summary.put("electionId", electionId);
                    configuration.put("summary", summary);

                    batchConfigurations.put(mesaId, configuration);

                } catch (Exception e) {
                    logger.error("Error generating configuration for mesa {} in batch", mesaId, e);
                }
            }

            logger.info("Batch configuration generated for {} mesas successfully", batchConfigurations.size());

        } catch (Exception e) {
            logger.error("Error in batch configuration generation", e);
        }

        return batchConfigurations;
    }

    @Override
    public Map<Integer, Map<String, Object>> generateDepartmentConfigurations(int departmentId, int electionId) {
        logger.info("Generating configurations for department {} and election {}", departmentId, electionId);

        try {
            // Get all mesa IDs for the department
            List<Integer> mesaIds = connectionDB.getMesaIdsByDepartment(departmentId);

            if (mesaIds.isEmpty()) {
                logger.warn("No mesas found for department {}", departmentId);
                return new HashMap<>();
            }

            // Use batch generation for efficiency
            return generateBatchMachineConfigurations(mesaIds, electionId);

        } catch (Exception e) {
            logger.error("Error generating department configurations for department {} and election {}",
                    departmentId, electionId, e);
            return new HashMap<>();
        }
    }

    @Override
    public Map<Integer, Map<String, Object>> generateAllMachineConfigurations(int electionId) {
        logger.info("Generating configurations for ALL mesas and election {}", electionId);

        try {
            // Get all mesa IDs in the system
            List<Integer> allMesaIds = connectionDB.getAllMesaIds();

            if (allMesaIds.isEmpty()) {
                logger.warn("No mesas found in the system");
                return new HashMap<>();
            }

            logger.info("Processing {} total mesas for national election configuration", allMesaIds.size());


            Map<Integer, Map<String, Object>> allConfigurations = new ConcurrentHashMap<>();
            int batchSize = 1000;

            for (int i = 0; i < allMesaIds.size(); i += batchSize) {
                int endIndex = Math.min(i + batchSize, allMesaIds.size());
                List<Integer> batch = allMesaIds.subList(i, endIndex);

                logger.info("Processing batch {}/{} ({} mesas)",
                        (i / batchSize) + 1, (allMesaIds.size() + batchSize - 1) / batchSize, batch.size());

                Map<Integer, Map<String, Object>> batchConfigurations = generateBatchMachineConfigurations(batch, electionId);
                allConfigurations.putAll(batchConfigurations);
            }

            logger.info("Generated configurations for {} mesas nationally", allConfigurations.size());
            return allConfigurations;

        } catch (Exception e) {
            logger.error("Error generating all machine configurations for election {}", electionId, e);
            return new HashMap<>();
        }
    }

    @Override
    public boolean validateConfiguration(Map<String, Object> configuration) {
        if (configuration == null) {
            return false;
        }

        try {
            boolean hasElectionInfo = configuration.containsKey(ELECTION_INFO) && configuration.get(ELECTION_INFO) != null;
            boolean hasCandidates = configuration.containsKey(CANDIDATES) && configuration.get(CANDIDATES) != null;
            boolean hasAssignedCitizens = configuration.containsKey(ASSIGNED_CITIZENS) && configuration.get(ASSIGNED_CITIZENS) != null;
            boolean hasMesaInfo = configuration.containsKey(MESA_INFO) && configuration.get(MESA_INFO) != null;

            if (!hasElectionInfo || !hasCandidates || !hasAssignedCitizens || !hasMesaInfo) {
                logger.warn("Configuration missing required sections");
                return false;
            }


            Map<String, Object> electionInfo = (Map<String, Object>) configuration.get(ELECTION_INFO);
            if (!electionInfo.containsKey("nombre") || !electionInfo.containsKey("estado")) {
                logger.warn("Election info missing required fields");
                return false;
            }


            List<Map<String, Object>> candidates = (List<Map<String, Object>>) configuration.get(CANDIDATES);
            if (candidates.isEmpty()) {
                logger.warn("No candidates found in configuration");
                return false;
            }


            List<Map<String, Object>> citizens = (List<Map<String, Object>>) configuration.get(ASSIGNED_CITIZENS);
            for (Map<String, Object> citizen : citizens) {
                if (!citizen.containsKey("documento") || !citizen.containsKey("nombre") || !citizen.containsKey("apellido")) {
                    logger.warn("Citizen missing required fields");
                    return false;
                }
            }

            logger.debug("Configuration validation passed");
            return true;

        } catch (Exception e) {
            logger.error("Error validating configuration", e);
            return false;
        }
    }

    @Override
    public Map<String, Object> getConfigurationStatistics(int electionId) {
        Map<String, Object> stats = new HashMap<>();

        try {

            Map<String, Object> dbMetrics = connectionDB.getPerformanceMetrics();


            Map<String, Object> electionInfo = connectionDB.getElectionInfo(electionId);


            List<Integer> allMesaIds = connectionDB.getAllMesaIds();


            List<Map<String, Object>> candidates = connectionDB.getCandidatesByElection(electionId);

            stats.put("totalMesas", allMesaIds.size());
            stats.put("totalCitizens", dbMetrics.get("total_citizens"));
            stats.put("totalCandidates", candidates.size());
            stats.put("electionName", electionInfo != null ? electionInfo.get("nombre") : "Unknown");
            stats.put("electionStatus", electionInfo != null ? electionInfo.get("estado") : "Unknown");
            stats.put("timestamp", new Date());

            int avgCitizensPerMesa = 979;
            long estimatedTotalRecords = (long) allMesaIds.size() * avgCitizensPerMesa;
            stats.put("estimatedTotalRecords", estimatedTotalRecords);

            logger.info("Configuration statistics generated for election {}", electionId);

        } catch (Exception e) {
            logger.error("Error getting configuration statistics for election {}", electionId, e);
            stats.put("error", "Failed to generate statistics: " + e.getMessage());
        }

        return stats;
    }

    @Override
    public String exportConfigurationToJson(Map<String, Object> configuration) {
        try {
            return jsonMapper.writeValueAsString(configuration);
        } catch (Exception e) {
            logger.error("Error exporting configuration to JSON", e);
            return "{}";
        }
    }

    @Override
    public boolean isElectionReadyForConfiguration(int electionId) {
        try {

            Map<String, Object> electionInfo = connectionDB.getElectionInfo(electionId);
            if (electionInfo == null) {
                logger.warn("Election {} not found", electionId);
                return false;
            }


            List<Map<String, Object>> candidates = connectionDB.getCandidatesByElection(electionId);
            if (candidates.isEmpty()) {
                logger.warn("Election {} has no candidates", electionId);
                return false;
            }


            String status = (String) electionInfo.get("estado");
            if (status == null || status.equals("CLOSED")) {
                logger.warn("Election {} is not in a valid status: {}", electionId, status);
                return false;
            }

            logger.info("Election {} is ready for configuration generation", electionId);
            return true;

        } catch (Exception e) {
            logger.error("Error checking if election {} is ready", electionId, e);
            return false;
        }
    }

    // =================== ADD THIS METHOD TO YOUR VotingManagerImpl.java ===================

    /**
     * Generate configurations for all mesas in a specific puesto de votación
     * This provides an optimized way to configure entire voting locations
     */
    @Override
    public Map<Integer, Map<String, Object>> generatePuestoConfigurations(int puestoId, int electionId) {
        logger.info("Generating configurations for puesto {} and election {}", puestoId, electionId);

        try {
            // Get all mesa IDs for this puesto
            List<Integer> mesaIds = new ArrayList<>();

            // Get all mesas and filter by puesto_id
            List<Integer> allMesaIds = connectionDB.getAllMesaIds();

            // Filter mesas that belong to this puesto
            for (Integer mesaId : allMesaIds) {
                Map<String, Object> mesaConfig = connectionDB.getMesaConfiguration(mesaId);
                if (mesaConfig != null && puestoId == (Integer) mesaConfig.get("puesto_id")) {
                    mesaIds.add(mesaId);
                }
            }

            if (mesaIds.isEmpty()) {
                logger.warn("No mesas found for puesto {}", puestoId);
                return new HashMap<>();
            }

            logger.info("Found {} mesas in puesto {}", mesaIds.size(), puestoId);

            // Use batch generation for efficiency
            Map<Integer, Map<String, Object>> puestoConfigurations = generateBatchMachineConfigurations(mesaIds, electionId);

            // Add puesto-level metadata
            for (Map<String, Object> config : puestoConfigurations.values()) {
                if (config != null) {
                    Map<String, Object> summary = (Map<String, Object>) config.get("summary");
                    if (summary != null) {
                        summary.put("puestoId", puestoId);
                        summary.put("configurationType", "PUESTO_LEVEL");
                    }
                }
            }

            logger.info("Generated {} configurations for puesto {}", puestoConfigurations.size(), puestoId);
            return puestoConfigurations;

        } catch (Exception e) {
            logger.error("Error generating puesto configurations for puesto {} and election {}",
                    puestoId, electionId, e);
            return new HashMap<>();
        }
    }



}