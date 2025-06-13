package Reports;

import ConnectionDB.ConnectionDBinterface;
import ReportsSystem.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.OutputStream;
import com.zeroc.Ice.InputStream;
import com.zeroc.Ice.Util;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Ice-based implementation of ReportsManagerInterface
 * Uses Ice serialization for efficient binary storage and transmission
 * Generates citizen lookup and election reporting configurations
 */
public class ReportsManagerImpl implements ReportsManagerInterface {

    private static final Logger logger = LoggerFactory.getLogger(ReportsManagerImpl.class);
    private final ConnectionDBinterface connectionDB;
    private final ObjectMapper jsonMapper;
    private final Communicator iceCommunicator;

    // Configuration constants
    private static final String PACKAGE_VERSION = "1.0";

    public ReportsManagerImpl(ConnectionDBinterface connectionDB) {
        this.connectionDB = connectionDB;
        this.jsonMapper = new ObjectMapper();
        this.jsonMapper.enable(SerializationFeature.INDENT_OUTPUT);

        // Initialize Ice communicator for serialization
        this.iceCommunicator = Util.initialize();

        logger.info("ReportsManagerImpl initialized with Ice serialization support");
    }

    // =================== CITIZEN LOOKUP REPORTS ===================

    @Override
    public CitizenReportsConfiguration generateCitizenReport(String documento, int electionId) {
        logger.info("Generating citizen report for document {} and election {}", documento, electionId);

        try {
            // 1. Get citizen voting assignment
            Map<String, Object> assignmentMap = connectionDB.getCitizenVotingAssignment(documento);
            if (assignmentMap == null) {
                logger.warn("No voting assignment found for document: {}", documento);
                return null;
            }

            // 2. Get election information
            Map<String, Object> electionInfoMap = connectionDB.getElectionInfo(electionId);
            if (electionInfoMap == null) {
                logger.error("Election {} not found", electionId);
                return null;
            }

            // 3. Get available elections
            List<Map<String, Object>> availableElectionsMap = connectionDB.getAllActiveElections();

            // 4. Convert to Ice structs
            CitizenVotingAssignment assignment = convertToCitizenVotingAssignment(assignmentMap, electionInfoMap);
            ElectionInfo[] availableElections = convertToElectionInfoArray(availableElectionsMap);

            // 5. Build Ice configuration
            CitizenReportsConfiguration configuration = new CitizenReportsConfiguration();
            configuration.assignment = assignment;
            configuration.availableElections = availableElections;
            configuration.packageVersion = PACKAGE_VERSION;
            configuration.generationTimestamp = System.currentTimeMillis();

            logger.info("Citizen report generated for document {}", documento);
            return configuration;

        } catch (Exception e) {
            logger.error("Error generating citizen report for document {} and election {}", documento, electionId, e);
            return null;
        }
    }

    @Override
    public List<CitizenReportsConfiguration> searchCitizenReports(String nombre, String apellido, int electionId, int limit) {
        logger.info("Searching citizen reports for name: {} {} (election {}, limit {})", nombre, apellido, electionId, limit);

        List<CitizenReportsConfiguration> results = new ArrayList<>();

        try {
            // 1. Search citizens by name
            List<Map<String, Object>> citizensMap = connectionDB.searchCitizensByName(nombre, apellido, limit);

            // 2. Get election info once
            Map<String, Object> electionInfoMap = connectionDB.getElectionInfo(electionId);
            if (electionInfoMap == null) {
                logger.error("Election {} not found", electionId);
                return results;
            }

            // 3. Get available elections once
            List<Map<String, Object>> availableElectionsMap = connectionDB.getAllActiveElections();
            ElectionInfo[] availableElections = convertToElectionInfoArray(availableElectionsMap);

            // 4. Generate report for each citizen found
            for (Map<String, Object> citizenMap : citizensMap) {
                try {
                    String documento = (String) citizenMap.get("documento");

                    // Get full assignment for this citizen
                    Map<String, Object> assignmentMap = connectionDB.getCitizenVotingAssignment(documento);
                    if (assignmentMap != null) {
                        CitizenVotingAssignment assignment = convertToCitizenVotingAssignment(assignmentMap, electionInfoMap);

                        CitizenReportsConfiguration config = new CitizenReportsConfiguration();
                        config.assignment = assignment;
                        config.availableElections = availableElections;
                        config.packageVersion = PACKAGE_VERSION;
                        config.generationTimestamp = System.currentTimeMillis();

                        results.add(config);
                    }
                } catch (Exception e) {
                    logger.warn("Error generating report for citizen in search results", e);
                }
            }

            logger.info("Found {} citizen reports for name search: {} {}", results.size(), nombre, apellido);

        } catch (Exception e) {
            logger.error("Error searching citizen reports for name: {} {}", nombre, apellido, e);
        }

        return results;
    }

    @Override
    public boolean validateCitizenEligibility(String documento) {
        try {
            return connectionDB.validateCitizenDocument(documento);
        } catch (Exception e) {
            logger.error("Error validating citizen eligibility for document: {}", documento, e);
            return false;
        }
    }

    // =================== ELECTION REPORTS ===================

    @Override
    public ElectionReportsConfiguration generateElectionResultsReport(int electionId) {
        logger.info("Generating election results report for election {}", electionId);

        try {
            // 1. Get election results summary
            Map<String, Object> resultsMap = connectionDB.getElectionResultsSummary(electionId);
            if (resultsMap == null) {
                logger.error("No results found for election {}", electionId);
                return null;
            }

            // 2. Get national statistics
            Map<String, Object> nationalStatsMap = connectionDB.getVotingStatsByDepartment(electionId, 0); // 0 = national level

            // 3. Get available locations (departments)
            List<Map<String, Object>> departmentsMap = connectionDB.getAllDepartments();

            // 4. Convert to Ice structs
            ElectionResults results = convertToElectionResults(resultsMap);
            GeographicStats nationalStats = convertToGeographicStats(nationalStatsMap, "national", electionId);
            LocationInfo[] availableLocations = convertToLocationInfoArray(departmentsMap, "department");

            // 5. Build Ice configuration
            ElectionReportsConfiguration configuration = new ElectionReportsConfiguration();
            configuration.results = results;
            configuration.nationalStats = nationalStats;
            configuration.availableLocations = availableLocations;
            configuration.packageVersion = PACKAGE_VERSION;
            configuration.generationTimestamp = System.currentTimeMillis();

            logger.info("Election results report generated for election {}", electionId);
            return configuration;

        } catch (Exception e) {
            logger.error("Error generating election results report for election {}", electionId, e);
            return null;
        }
    }

    @Override
    public Map<String, Object> getElectionStatistics(int electionId) {
        try {
            return connectionDB.getElectionConfigurationStats(electionId);
        } catch (Exception e) {
            logger.error("Error getting election statistics for election {}", electionId, e);
            return new HashMap<>();
        }
    }

    // =================== GEOGRAPHIC REPORTS ===================

    @Override
    public GeographicReportsConfiguration generateDepartmentReport(int departmentId, int electionId) {
        logger.info("Generating department report for department {} and election {}", departmentId, electionId);

        try {
            // 1. Get department voting stats
            Map<String, Object> statsMap = connectionDB.getVotingStatsByDepartment(electionId, departmentId);

            // 2. Get municipalities in this department
            List<Map<String, Object>> municipalitiesMap = connectionDB.getMunicipalitiesByDepartment(departmentId);

            // 3. Get sample citizen assignments (first 10 citizens in department)
            List<Integer> mesaIds = connectionDB.getMesaIdsByDepartment(departmentId);
            List<Map<String, Object>> sampleCitizens = new ArrayList<>();
            if (!mesaIds.isEmpty()) {
                // Get first few citizens from first mesa as sample
                sampleCitizens = connectionDB.getCitizensByMesa(mesaIds.get(0));
                if (sampleCitizens.size() > 10) {
                    sampleCitizens = sampleCitizens.subList(0, 10);
                }
            }

            // 4. Convert to Ice structs
            GeographicStats stats = convertToGeographicStats(statsMap, "department", electionId);
            LocationInfo[] subLocations = convertToLocationInfoArray(municipalitiesMap, "municipality");
            CitizenVotingAssignment[] sampleAssignments = convertToCitizenAssignmentArray(sampleCitizens, electionId);

            // 5. Build Ice configuration
            GeographicReportsConfiguration configuration = new GeographicReportsConfiguration();
            configuration.stats = stats;
            configuration.subLocations = subLocations;
            configuration.sampleAssignments = sampleAssignments;
            configuration.packageVersion = PACKAGE_VERSION;
            configuration.generationTimestamp = System.currentTimeMillis();

            logger.info("Department report generated for department {}", departmentId);
            return configuration;

        } catch (Exception e) {
            logger.error("Error generating department report for department {} and election {}", departmentId, electionId, e);
            return null;
        }
    }

    @Override
    public GeographicReportsConfiguration generateMunicipalityReport(int municipalityId, int electionId) {
        logger.info("Generating municipality report for municipality {} and election {}", municipalityId, electionId);

        try {
            // 1. Get municipality voting stats
            Map<String, Object> statsMap = connectionDB.getVotingStatsByMunicipality(electionId, municipalityId);

            // 2. Get puestos in this municipality
            List<Map<String, Object>> puestosMap = connectionDB.getPuestosByMunicipality(municipalityId);

            // 3. Convert to Ice structs
            GeographicStats stats = convertToGeographicStats(statsMap, "municipality", electionId);
            LocationInfo[] subLocations = convertToLocationInfoArray(puestosMap, "puesto");

            // 4. Build Ice configuration
            GeographicReportsConfiguration configuration = new GeographicReportsConfiguration();
            configuration.stats = stats;
            configuration.subLocations = subLocations;
            configuration.sampleAssignments = new CitizenVotingAssignment[0]; // Empty for municipality level
            configuration.packageVersion = PACKAGE_VERSION;
            configuration.generationTimestamp = System.currentTimeMillis();

            logger.info("Municipality report generated for municipality {}", municipalityId);
            return configuration;

        } catch (Exception e) {
            logger.error("Error generating municipality report for municipality {} and election {}", municipalityId, electionId, e);
            return null;
        }
    }

    @Override
    public GeographicReportsConfiguration generatePuestoReport(int puestoId, int electionId) {
        logger.info("Generating puesto report for puesto {} and election {}", puestoId, electionId);

        try {
            // 1. Get puesto voting stats
            Map<String, Object> statsMap = connectionDB.getVotingStatsByPuesto(electionId, puestoId);

            // 2. Get mesas in this puesto
            List<Map<String, Object>> mesasMap = connectionDB.getMesasByPuesto(puestoId);

            // 3. Convert to Ice structs
            GeographicStats stats = convertToGeographicStats(statsMap, "puesto", electionId);
            LocationInfo[] subLocations = convertToLocationInfoArray(mesasMap, "mesa");

            // 4. Build Ice configuration
            GeographicReportsConfiguration configuration = new GeographicReportsConfiguration();
            configuration.stats = stats;
            configuration.subLocations = subLocations;
            configuration.sampleAssignments = new CitizenVotingAssignment[0]; // Empty for puesto level
            configuration.packageVersion = PACKAGE_VERSION;
            configuration.generationTimestamp = System.currentTimeMillis();

            logger.info("Puesto report generated for puesto {}", puestoId);
            return configuration;

        } catch (Exception e) {
            logger.error("Error generating puesto report for puesto {} and election {}", puestoId, electionId, e);
            return null;
        }
    }

    // =================== BATCH OPERATIONS ===================

    @Override
    public Map<String, CitizenReportsConfiguration> generateBatchCitizenReports(List<String> documentos, int electionId) {
        logger.info("Generating batch citizen reports for {} documents and election {}", documentos.size(), electionId);

        Map<String, CitizenReportsConfiguration> batchReports = new ConcurrentHashMap<>();

        try {
            // Get election info and available elections once
            Map<String, Object> electionInfoMap = connectionDB.getElectionInfo(electionId);
            if (electionInfoMap == null) {
                logger.error("Election {} not found", electionId);
                return batchReports;
            }

            List<Map<String, Object>> availableElectionsMap = connectionDB.getAllActiveElections();
            ElectionInfo[] availableElections = convertToElectionInfoArray(availableElectionsMap);

            // Generate report for each document
            for (String documento : documentos) {
                try {
                    Map<String, Object> assignmentMap = connectionDB.getCitizenVotingAssignment(documento);
                    if (assignmentMap != null) {
                        CitizenVotingAssignment assignment = convertToCitizenVotingAssignment(assignmentMap, electionInfoMap);

                        CitizenReportsConfiguration config = new CitizenReportsConfiguration();
                        config.assignment = assignment;
                        config.availableElections = availableElections;
                        config.packageVersion = PACKAGE_VERSION;
                        config.generationTimestamp = System.currentTimeMillis();

                        batchReports.put(documento, config);
                    }
                } catch (Exception e) {
                    logger.warn("Error generating report for document {} in batch", documento, e);
                }
            }

            logger.info("Generated {} citizen reports in batch", batchReports.size());

        } catch (Exception e) {
            logger.error("Error in batch citizen reports generation", e);
        }

        return batchReports;
    }

    @Override
    public List<CitizenReportsConfiguration> generateMesaCitizenReports(int mesaId, int electionId) {
        logger.info("Generating citizen reports for all citizens in mesa {} and election {}", mesaId, electionId);

        List<CitizenReportsConfiguration> mesaReports = new ArrayList<>();

        try {
            // Get all citizens in mesa
            List<Map<String, Object>> citizensMap = connectionDB.getCitizensByMesa(mesaId);

            // Extract document IDs
            List<String> documentos = new ArrayList<>();
            for (Map<String, Object> citizen : citizensMap) {
                documentos.add((String) citizen.get("documento"));
            }

            // Use batch generation
            Map<String, CitizenReportsConfiguration> batchReports = generateBatchCitizenReports(documentos, electionId);
            mesaReports.addAll(batchReports.values());

            logger.info("Generated {} citizen reports for mesa {}", mesaReports.size(), mesaId);

        } catch (Exception e) {
            logger.error("Error generating mesa citizen reports for mesa {} and election {}", mesaId, electionId, e);
        }

        return mesaReports;
    }

    // =================== CONFIGURATION AND EXPORT ===================

    @Override
    public boolean validateConfiguration(Object configuration) {
        if (configuration == null) {
            logger.warn("Configuration is null");
            return false;
        }

        try {
            if (configuration instanceof CitizenReportsConfiguration) {
                CitizenReportsConfiguration config = (CitizenReportsConfiguration) configuration;

                // Validate assignment
                if (config.assignment == null) {
                    logger.warn("CitizenReportsConfiguration missing assignment");
                    return false;
                }

                // Validate citizen info
                if (config.assignment.citizen == null ||
                        config.assignment.citizen.documento == null ||
                        config.assignment.citizen.documento.isEmpty()) {
                    logger.warn("CitizenReportsConfiguration missing valid citizen info");
                    return false;
                }

                // Validate location info
                if (config.assignment.location == null) {
                    logger.warn("CitizenReportsConfiguration missing location info");
                    return false;
                }

                // Validate election info
                if (config.assignment.election == null) {
                    logger.warn("CitizenReportsConfiguration missing election info");
                    return false;
                }

                logger.debug("CitizenReportsConfiguration validation passed");
                return true;

            } else if (configuration instanceof ElectionReportsConfiguration) {
                ElectionReportsConfiguration config = (ElectionReportsConfiguration) configuration;

                // Validate results
                if (config.results == null) {
                    logger.warn("ElectionReportsConfiguration missing results");
                    return false;
                }

                // Validate election info in results
                if (config.results.election == null) {
                    logger.warn("ElectionReportsConfiguration missing election info");
                    return false;
                }

                // Validate candidate results
                if (config.results.candidateResults == null) {
                    logger.warn("ElectionReportsConfiguration missing candidate results");
                    return false;
                }

                logger.debug("ElectionReportsConfiguration validation passed");
                return true;

            } else if (configuration instanceof GeographicReportsConfiguration) {
                GeographicReportsConfiguration config = (GeographicReportsConfiguration) configuration;

                // Validate stats
                if (config.stats == null) {
                    logger.warn("GeographicReportsConfiguration missing stats");
                    return false;
                }

                // Validate location info in stats
                if (config.stats.location == null) {
                    logger.warn("GeographicReportsConfiguration missing location info");
                    return false;
                }

                // Validate election info in stats
                if (config.stats.election == null) {
                    logger.warn("GeographicReportsConfiguration missing election info");
                    return false;
                }

                logger.debug("GeographicReportsConfiguration validation passed");
                return true;

            } else {
                logger.warn("Unknown configuration type: {}", configuration.getClass().getSimpleName());
                return false;
            }

        } catch (Exception e) {
            logger.error("Error validating configuration", e);
            return false;
        }
    }

    @Override
    public byte[] exportConfigurationToBytes(Object configuration) {
        try {
            // Use Ice 3.7+ serialization API
            com.zeroc.Ice.OutputStream out = new com.zeroc.Ice.OutputStream(iceCommunicator);

            // Cast to proper Ice Value type before writing
            if (configuration instanceof com.zeroc.Ice.Value) {
                out.writeValue((com.zeroc.Ice.Value) configuration);
                out.writePendingValues();

                byte[] data = out.finished();
                logger.debug("Exported configuration to {} bytes", data.length);
                return data;
            } else {
                logger.error("Configuration is not an Ice Value type: {}",
                        configuration != null ? configuration.getClass().getSimpleName() : "null");
                return new byte[0];
            }

        } catch (Exception e) {
            logger.error("Error exporting Ice configuration to bytes", e);
            return new byte[0];
        }
    }

    @Override
    public Object importConfigurationFromBytes(byte[] data, String configType) {
        try {
            if (data == null || data.length == 0) {
                logger.warn("Empty data provided for import");
                return null;
            }

            // Use Ice 3.7+ deserialization API
            com.zeroc.Ice.InputStream in = new com.zeroc.Ice.InputStream(iceCommunicator, data);

            // Create holder for the result
            final Object[] holder = new Object[1];

            // Read the value using Ice's callback mechanism
            in.readValue(value -> {
                holder[0] = value;
            });
            in.readPendingValues();

            logger.debug("Imported configuration from {} bytes, type: {}", data.length, configType);
            return holder[0];

        } catch (Exception e) {
            logger.error("Error importing Ice configuration from bytes", e);
            return null;
        }
    }

    @Override
    public String exportConfigurationToJson(Object configuration) {
        try {
            // Convert to JSON-friendly format - implementation would be similar to VotingManagerImpl
            Map<String, Object> jsonMap = convertConfigurationToJsonMap(configuration);
            return jsonMapper.writeValueAsString(jsonMap);
        } catch (Exception e) {
            logger.error("Error exporting configuration to JSON", e);
            return "{}";
        }
    }

    @Override
    public boolean saveConfigurationToFile(Object configuration, String filePath) {
        try {
            byte[] configBytes = exportConfigurationToBytes(configuration);
            Files.write(Paths.get(filePath), configBytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            logger.debug("Configuration saved to file: {}", filePath);
            return true;
        } catch (Exception e) {
            logger.error("Error saving configuration to file: {}", filePath, e);
            return false;
        }
    }

    @Override
    public Object loadConfigurationFromFile(String filePath, String configType) {
        try {
            byte[] configBytes = Files.readAllBytes(Paths.get(filePath));
            Object config = importConfigurationFromBytes(configBytes, configType);
            logger.debug("Configuration loaded from file: {}", filePath);
            return config;
        } catch (Exception e) {
            logger.error("Error loading configuration from file: {}", filePath, e);
            return null;
        }
    }

    // =================== UTILITY METHODS ===================

    @Override
    public Map<String, Object> getReportsStatistics(int electionId) {
        Map<String, Object> stats = new HashMap<>();

        try {
            // Get database metrics
            Map<String, Object> dbMetrics = connectionDB.getPerformanceMetrics();

            // Get election info
            Map<String, Object> electionInfo = connectionDB.getElectionInfo(electionId);

            stats.put("totalCitizens", dbMetrics.get("total_citizens"));
            stats.put("totalMesas", dbMetrics.get("total_mesas"));
            stats.put("electionName", electionInfo != null ? electionInfo.get("nombre") : "Unknown");
            stats.put("electionStatus", electionInfo != null ? electionInfo.get("estado") : "Unknown");
            stats.put("timestamp", new Date());
            stats.put("serializationType", "Ice Binary");
            stats.put("moduleType", "Reports");

            logger.info("Reports statistics generated for election {}", electionId);

        } catch (Exception e) {
            logger.error("Error getting reports statistics for election {}", electionId, e);
            stats.put("error", "Failed to generate statistics: " + e.getMessage());
        }

        return stats;
    }

    @Override
    public boolean isElectionReadyForReports(int electionId) {
        try {
            return connectionDB.validateElectionDataCompleteness(electionId);
        } catch (Exception e) {
            logger.error("Error checking if election {} is ready for reports", electionId, e);
            return false;
        }
    }

    @Override
    public List<Map<String, Object>> getAvailableElections() {
        try {
            return connectionDB.getAllActiveElections();
        } catch (Exception e) {
            logger.error("Error getting available elections", e);
            return new ArrayList<>();
        }
    }

    // =================== HELPER METHODS FOR CONVERSION ===================

    private CitizenVotingAssignment convertToCitizenVotingAssignment(Map<String, Object> assignmentMap, Map<String, Object> electionInfoMap) {
        try {
            CitizenVotingAssignment assignment = new CitizenVotingAssignment();

            // Convert citizen info with null checks
            CitizenInfo citizen = new CitizenInfo();
            citizen.id = (Integer) assignmentMap.getOrDefault("ciudadano_id", 0);
            citizen.documento = (String) assignmentMap.getOrDefault("documento", "");
            citizen.nombre = (String) assignmentMap.getOrDefault("nombre", "");
            citizen.apellido = (String) assignmentMap.getOrDefault("apellido", "");
            assignment.citizen = citizen;

            // Convert location info with null checks
            LocationInfo location = new LocationInfo();
            location.departamentoId = (Integer) assignmentMap.getOrDefault("departamento_id", 0);
            location.departamentoNombre = (String) assignmentMap.getOrDefault("departamento_nombre", "");
            location.municipioId = (Integer) assignmentMap.getOrDefault("municipio_id", 0);
            location.municipioNombre = (String) assignmentMap.getOrDefault("municipio_nombre", "");
            location.puestoId = (Integer) assignmentMap.getOrDefault("puesto_id", 0);
            location.puestoNombre = (String) assignmentMap.getOrDefault("puesto_nombre", "");
            location.puestoDireccion = (String) assignmentMap.getOrDefault("puesto_direccion", "");
            location.puestoConsecutive = (Integer) assignmentMap.getOrDefault("puesto_consecutive", 0);
            location.mesaId = (Integer) assignmentMap.getOrDefault("mesa_id", 0);
            location.mesaConsecutive = (Integer) assignmentMap.getOrDefault("mesa_consecutive", 0);
            assignment.location = location;

            // Convert election info with null checks
            ElectionInfo election = new ElectionInfo();
            election.id = (Integer) electionInfoMap.getOrDefault("id", 0);
            election.nombre = (String) electionInfoMap.getOrDefault("nombre", "");
            election.estado = (String) electionInfoMap.getOrDefault("estado", "");

            // Handle timestamp conversion safely
            Object fechaInicio = electionInfoMap.get("fecha_inicio");
            if (fechaInicio instanceof java.sql.Timestamp) {
                election.fechaInicio = ((java.sql.Timestamp) fechaInicio).getTime();
            } else if (fechaInicio instanceof Long) {
                election.fechaInicio = (Long) fechaInicio;
            } else {
                election.fechaInicio = 0L;
            }

            Object fechaFin = electionInfoMap.get("fecha_fin");
            if (fechaFin instanceof java.sql.Timestamp) {
                election.fechaFin = ((java.sql.Timestamp) fechaFin).getTime();
            } else if (fechaFin instanceof Long) {
                election.fechaFin = (Long) fechaFin;
            } else {
                election.fechaFin = 0L;
            }

            assignment.election = election;
            assignment.generationTimestamp = System.currentTimeMillis();

            return assignment;

        } catch (Exception e) {
            logger.error("Error converting to CitizenVotingAssignment", e);
            throw new RuntimeException("Failed to convert citizen voting assignment", e);
        }
    }

    private ElectionResults convertToElectionResults(Map<String, Object> resultsMap) {
        ElectionResults results = new ElectionResults();

        // Convert election info
        ElectionInfo election = new ElectionInfo();
        election.nombre = (String) resultsMap.get("election_name");
        election.estado = (String) resultsMap.get("election_status");
        if (resultsMap.get("fecha_inicio") instanceof java.sql.Timestamp) {
            election.fechaInicio = ((java.sql.Timestamp) resultsMap.get("fecha_inicio")).getTime();
        }
        if (resultsMap.get("fecha_fin") instanceof java.sql.Timestamp) {
            election.fechaFin = ((java.sql.Timestamp) resultsMap.get("fecha_fin")).getTime();
        }
        results.election = election;

        // Convert candidate results
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> candidateResults = (List<Map<String, Object>>) resultsMap.get("candidate_results");
        if (candidateResults != null) {
            CandidateInfo[] candidates = new CandidateInfo[candidateResults.size()];
            for (int i = 0; i < candidateResults.size(); i++) {
                Map<String, Object> candidateMap = candidateResults.get(i);
                CandidateInfo candidate = new CandidateInfo();
                candidate.id = (Integer) candidateMap.get("candidate_id");
                candidate.nombre = (String) candidateMap.get("candidate_name");
                candidate.partido = ""; // Not available in results summary
                candidate.voteCount = (Integer) candidateMap.get("vote_count");
                candidate.percentage = (Double) candidateMap.get("percentage");
                candidates[i] = candidate;
            }
            results.candidateResults = candidates;
        } else {
            results.candidateResults = new CandidateInfo[0];
        }

        results.totalVotes = (Integer) resultsMap.getOrDefault("total_votes", 0);
        results.totalEligibleVoters = 0L; // Would need additional query
        results.participationPercentage = 0.0; // Would need calculation
        results.generationTimestamp = System.currentTimeMillis();

        return results;
    }

    private GeographicStats convertToGeographicStats(Map<String, Object> statsMap, String locationType, int electionId) {
        GeographicStats stats = new GeographicStats();

        // Create location info based on type
        LocationInfo location = new LocationInfo();
        if ("department".equals(locationType)) {
            location.departamentoId = (Integer) statsMap.getOrDefault("departamento_id", 0);
            location.departamentoNombre = "Department Statistics";
        } else if ("municipality".equals(locationType)) {
            location.municipioId = (Integer) statsMap.getOrDefault("municipio_id", 0);
            location.municipioNombre = "Municipality Statistics";
        } else if ("puesto".equals(locationType)) {
            location.puestoId = (Integer) statsMap.getOrDefault("puesto_id", 0);
            location.puestoNombre = "Puesto Statistics";
        }
        stats.location = location;

        // Create basic election info
        ElectionInfo election = new ElectionInfo();
        election.id = electionId;
        stats.election = election;

        stats.totalMesas = (Integer) statsMap.getOrDefault("total_mesas", 0);
        stats.totalPuestos = (Integer) statsMap.getOrDefault("total_puestos", 0);
        stats.totalMunicipios = (Integer) statsMap.getOrDefault("total_municipios", 0);
        stats.totalCitizens = ((Number) statsMap.getOrDefault("total_ciudadanos", 0L)).longValue();
        stats.totalVotes = ((Number) statsMap.getOrDefault("total_votos", 0L)).longValue();
        stats.participationPercentage = (Double) statsMap.getOrDefault("participation_percentage", 0.0);
        stats.generationTimestamp = System.currentTimeMillis();

        return stats;
    }

    private ElectionInfo[] convertToElectionInfoArray(List<Map<String, Object>> electionsMap) {
        ElectionInfo[] elections = new ElectionInfo[electionsMap.size()];
        for (int i = 0; i < electionsMap.size(); i++) {
            Map<String, Object> electionMap = electionsMap.get(i);
            ElectionInfo election = new ElectionInfo();
            election.id = (Integer) electionMap.get("id");
            election.nombre = (String) electionMap.get("nombre");
            election.estado = (String) electionMap.get("estado");

            if (electionMap.get("fecha_inicio") instanceof java.sql.Timestamp) {
                election.fechaInicio = ((java.sql.Timestamp) electionMap.get("fecha_inicio")).getTime();
            }
            if (electionMap.get("fecha_fin") instanceof java.sql.Timestamp) {
                election.fechaFin = ((java.sql.Timestamp) electionMap.get("fecha_fin")).getTime();
            }

            elections[i] = election;
        }
        return elections;
    }

    private LocationInfo[] convertToLocationInfoArray(List<Map<String, Object>> locationsMap, String locationType) {
        LocationInfo[] locations = new LocationInfo[locationsMap.size()];
        for (int i = 0; i < locationsMap.size(); i++) {
            Map<String, Object> locationMap = locationsMap.get(i);
            LocationInfo location = new LocationInfo();

            if ("department".equals(locationType)) {
                location.departamentoId = (Integer) locationMap.get("id");
                location.departamentoNombre = (String) locationMap.get("nombre");
            } else if ("municipality".equals(locationType)) {
                location.municipioId = (Integer) locationMap.get("id");
                location.municipioNombre = (String) locationMap.get("nombre");
                location.departamentoId = (Integer) locationMap.get("departamento_id");
            } else if ("puesto".equals(locationType)) {
                location.puestoId = (Integer) locationMap.get("id");
                location.puestoNombre = (String) locationMap.get("nombre");
                location.puestoDireccion = (String) locationMap.get("direccion");
                location.puestoConsecutive = (Integer) locationMap.get("consecutive");
                location.municipioId = (Integer) locationMap.get("municipio_id");
            } else if ("mesa".equals(locationType)) {
                location.mesaId = (Integer) locationMap.get("mesa_id");
                location.mesaConsecutive = (Integer) locationMap.get("mesa_consecutive");
                location.puestoId = (Integer) locationMap.get("puesto_id");
            }

            locations[i] = location;
        }
        return locations;
    }

    private CitizenVotingAssignment[] convertToCitizenAssignmentArray(List<Map<String, Object>> citizensMap, int electionId) {
        CitizenVotingAssignment[] assignments = new CitizenVotingAssignment[citizensMap.size()];
        for (int i = 0; i < citizensMap.size(); i++) {
            Map<String, Object> citizenMap = citizensMap.get(i);

            // Create basic assignment (would need full data for complete assignment)
            CitizenVotingAssignment assignment = new CitizenVotingAssignment();

            CitizenInfo citizen = new CitizenInfo();
            citizen.id = (Integer) citizenMap.get("id");
            citizen.documento = (String) citizenMap.get("documento");
            citizen.nombre = (String) citizenMap.get("nombre");
            citizen.apellido = (String) citizenMap.get("apellido");
            assignment.citizen = citizen;

            // Basic location info (would need full query for complete data)
            LocationInfo location = new LocationInfo();
            assignment.location = location;

            // Basic election info
            ElectionInfo election = new ElectionInfo();
            election.id = electionId;
            assignment.election = election;

            assignment.generationTimestamp = System.currentTimeMillis();
            assignments[i] = assignment;
        }
        return assignments;
    }

    private Map<String, Object> convertConfigurationToJsonMap(Object configuration) {
        Map<String, Object> jsonMap = new HashMap<>();

        try {
            if (configuration instanceof CitizenReportsConfiguration) {
                CitizenReportsConfiguration config = (CitizenReportsConfiguration) configuration;

                jsonMap.put("type", "citizen_reports");
                jsonMap.put("packageVersion", config.packageVersion);
                jsonMap.put("generationTimestamp", new Date(config.generationTimestamp));

                // Convert assignment
                if (config.assignment != null) {
                    Map<String, Object> assignmentMap = new HashMap<>();

                    if (config.assignment.citizen != null) {
                        Map<String, Object> citizenMap = new HashMap<>();
                        citizenMap.put("id", config.assignment.citizen.id);
                        citizenMap.put("documento", config.assignment.citizen.documento);
                        citizenMap.put("nombre", config.assignment.citizen.nombre);
                        citizenMap.put("apellido", config.assignment.citizen.apellido);
                        assignmentMap.put("citizen", citizenMap);
                    }

                    if (config.assignment.location != null) {
                        Map<String, Object> locationMap = new HashMap<>();
                        locationMap.put("departamentoId", config.assignment.location.departamentoId);
                        locationMap.put("departamentoNombre", config.assignment.location.departamentoNombre);
                        locationMap.put("municipioId", config.assignment.location.municipioId);
                        locationMap.put("municipioNombre", config.assignment.location.municipioNombre);
                        locationMap.put("puestoId", config.assignment.location.puestoId);
                        locationMap.put("puestoNombre", config.assignment.location.puestoNombre);
                        locationMap.put("puestoDireccion", config.assignment.location.puestoDireccion);
                        locationMap.put("mesaId", config.assignment.location.mesaId);
                        locationMap.put("mesaConsecutive", config.assignment.location.mesaConsecutive);
                        assignmentMap.put("location", locationMap);
                    }

                    jsonMap.put("assignment", assignmentMap);
                }

                // Convert available elections
                if (config.availableElections != null) {
                    List<Map<String, Object>> electionsList = new ArrayList<>();
                    for (ElectionInfo election : config.availableElections) {
                        Map<String, Object> electionMap = new HashMap<>();
                        electionMap.put("id", election.id);
                        electionMap.put("nombre", election.nombre);
                        electionMap.put("estado", election.estado);
                        electionsList.add(electionMap);
                    }
                    jsonMap.put("availableElections", electionsList);
                }

            } else if (configuration instanceof ElectionReportsConfiguration) {
                ElectionReportsConfiguration config = (ElectionReportsConfiguration) configuration;

                jsonMap.put("type", "election_reports");
                jsonMap.put("packageVersion", config.packageVersion);
                jsonMap.put("generationTimestamp", new Date(config.generationTimestamp));

                // Add election results conversion
                if (config.results != null) {
                    Map<String, Object> resultsMap = new HashMap<>();
                    resultsMap.put("totalVotes", config.results.totalVotes);
                    resultsMap.put("participationPercentage", config.results.participationPercentage);
                    jsonMap.put("results", resultsMap);
                }

            } else if (configuration instanceof GeographicReportsConfiguration) {
                GeographicReportsConfiguration config = (GeographicReportsConfiguration) configuration;

                jsonMap.put("type", "geographic_reports");
                jsonMap.put("packageVersion", config.packageVersion);
                jsonMap.put("generationTimestamp", new Date(config.generationTimestamp));

                // Add geographic stats conversion
                if (config.stats != null) {
                    Map<String, Object> statsMap = new HashMap<>();
                    statsMap.put("totalMesas", config.stats.totalMesas);
                    statsMap.put("totalCitizens", config.stats.totalCitizens);
                    statsMap.put("totalVotes", config.stats.totalVotes);
                    statsMap.put("participationPercentage", config.stats.participationPercentage);
                    jsonMap.put("stats", statsMap);
                }
            }

        } catch (Exception e) {
            logger.error("Error converting configuration to JSON map", e);
            jsonMap.put("error", "Conversion failed: " + e.getMessage());
        }

        return jsonMap;
    }

    // =================== TESTING AND VALIDATION METHODS ===================

    /**
     * Test method to validate the Reports Manager implementation
     * Call this during system startup to ensure everything works
     */
    public Map<String, Object> performSystemTest(int testElectionId) {
        Map<String, Object> testResults = new HashMap<>();
        List<String> errors = new ArrayList<>();

        try {
            logger.info("Starting ReportsManager system test with election {}", testElectionId);

            // Test 1: Check database connectivity
            try {
                boolean dbHealthy = connectionDB.isHealthy();
                testResults.put("databaseHealthy", dbHealthy);
                if (!dbHealthy) {
                    errors.add("Database connectivity failed");
                }
            } catch (Exception e) {
                errors.add("Database test failed: " + e.getMessage());
            }

            // Test 2: Check election readiness
            try {
                boolean electionReady = isElectionReadyForReports(testElectionId);
                testResults.put("electionReady", electionReady);
                if (!electionReady) {
                    errors.add("Test election not ready for reports");
                }
            } catch (Exception e) {
                errors.add("Election readiness test failed: " + e.getMessage());
            }

            // Test 3: Test Ice serialization (skip if classes not properly generated)
            try {
                // Create a simple test configuration
                CitizenReportsConfiguration testConfig = new CitizenReportsConfiguration();
                testConfig.packageVersion = PACKAGE_VERSION;
                testConfig.generationTimestamp = System.currentTimeMillis();
                testConfig.availableElections = new ElectionInfo[0];

                // Check if this is an Ice Value (classes must be generated from .ice files)
                boolean isIceValue = testConfig instanceof com.zeroc.Ice.Value;
                testResults.put("isIceValue", isIceValue);

                if (isIceValue) {
                    // Test serialization only if it's a proper Ice class
                    byte[] serialized = exportConfigurationToBytes(testConfig);
                    Object deserialized = importConfigurationFromBytes(serialized, "citizen");

                    boolean serializationWorks = (serialized.length > 0) &&
                            (deserialized instanceof CitizenReportsConfiguration);
                    testResults.put("iceSerializationWorks", serializationWorks);

                    if (!serializationWorks) {
                        errors.add("Ice serialization test failed");
                    }
                } else {
                    testResults.put("iceSerializationWorks", false);
                    errors.add("Ice classes not properly generated from .ice files - serialization skipped");
                }
            } catch (Exception e) {
                errors.add("Ice serialization test failed: " + e.getMessage());
            }

            // Test 4: Test validation
            try {
                boolean validationWorks = validateConfiguration(null) == false; // Should return false for null
                testResults.put("validationWorks", validationWorks);

                if (!validationWorks) {
                    errors.add("Configuration validation test failed");
                }
            } catch (Exception e) {
                errors.add("Validation test failed: " + e.getMessage());
            }

            // Summary
            testResults.put("allTestsPassed", errors.isEmpty());
            testResults.put("errors", errors);
            testResults.put("totalTests", 4);
            testResults.put("testTimestamp", new Date());

            if (errors.isEmpty()) {
                logger.info("ReportsManager system test completed successfully");
            } else {
                logger.warn("ReportsManager system test completed with {} errors: {}", errors.size(), errors);
            }

        } catch (Exception e) {
            logger.error("Critical error during system test", e);
            errors.add("Critical system test error: " + e.getMessage());
            testResults.put("allTestsPassed", false);
            testResults.put("errors", errors);
        }

        return testResults;
    }

    /**
     * Cleanup Ice communicator when shutting down
     */
    public void shutdown() {
        try {
            if (iceCommunicator != null && !iceCommunicator.isShutdown()) {
                iceCommunicator.destroy();
                logger.info("Ice communicator shutdown successfully");
            }
        } catch (Exception e) {
            logger.error("Error shutting down Ice communicator", e);
        }
    }

// =================== MÉTODOS PARA FULL CITIZEN REPORTS ===================

    /**
     * Genera reportes FULL CITIZEN REPORT para todos los ciudadanos de un departamento específico
     * Guarda cada reporte como archivo ICE en la carpeta Reports/data
     *
     * @param departmentId ID del departamento
     * @param electionId ID de la elección
     * @return Map con estadísticas del proceso de generación
     */
    @Override
    public Map<String, Object> generateDepartmentCitizenReports(int departmentId, int electionId) {
        logger.info("Generating FULL CITIZEN REPORTS for all citizens in department {} and election {}", departmentId, electionId);

        Map<String, Object> result = new HashMap<>();
        List<String> errors = new ArrayList<>();
        int successCount = 0;
        int totalCount = 0;

        try {
            // 1. Obtener información de la elección
            Map<String, Object> electionInfoMap = connectionDB.getElectionInfo(electionId);
            if (electionInfoMap == null) {
                logger.error("Election {} not found", electionId);
                result.put("success", false);
                result.put("error", "Election not found: " + electionId);
                return result;
            }

            // 2. Obtener elecciones disponibles una vez
            List<Map<String, Object>> availableElectionsMap = connectionDB.getAllActiveElections();
            ElectionInfo[] availableElections = convertToElectionInfoArray(availableElectionsMap);

            // 3. Obtener todos los ciudadanos del departamento
            List<Map<String, Object>> citizens = connectionDB.getCitizensByDepartment(departmentId);
            totalCount = citizens.size();

            if (citizens.isEmpty()) {
                logger.warn("No citizens found for department {}", departmentId);
                result.put("success", true);
                result.put("message", "No citizens found for department " + departmentId);
                result.put("totalCitizens", 0);
                result.put("successCount", 0);
                return result;
            }

            // 4. Crear directorio si no existe
            String reportDirectory = "server/src/main/java/Reports/data";
            Path directoryPath = Paths.get(reportDirectory);
            if (!Files.exists(directoryPath)) {
                Files.createDirectories(directoryPath);
                logger.info("Created directory: {}", reportDirectory);
            }

            // 5. Generar reporte para cada ciudadano
            for (Map<String, Object> citizenData : citizens) {
                try {
                    String documento = (String) citizenData.get("documento");
                    String nombre = (String) citizenData.get("nombre");
                    String apellido = (String) citizenData.get("apellido");

                    // Crear assignment usando los datos ya obtenidos
                    CitizenVotingAssignment assignment = convertToCitizenVotingAssignmentFromData(citizenData, electionInfoMap);

                    // Crear configuración del reporte
                    CitizenReportsConfiguration config = new CitizenReportsConfiguration();
                    config.assignment = assignment;
                    config.availableElections = availableElections;
                    config.packageVersion = PACKAGE_VERSION;
                    config.generationTimestamp = System.currentTimeMillis();

                    // Generar nombre del archivo
                    String fileName = String.format("citizen_report_dept_%d_doc_%s.ice", departmentId, documento);
                    String filePath = Paths.get(reportDirectory, fileName).toString();

                    // Guardar como archivo ICE
                    boolean saved = saveConfigurationToFile(config, filePath);

                    if (saved) {
                        successCount++;
                        logger.debug("Saved citizen report for {} {} ({})", apellido, nombre, documento);
                    } else {
                        errors.add("Failed to save report for citizen: " + documento);
                        logger.warn("Failed to save report for citizen: {}", documento);
                    }

                } catch (Exception e) {
                    String documento = (String) citizenData.getOrDefault("documento", "unknown");
                    String errorMsg = "Error generating report for citizen " + documento + ": " + e.getMessage();
                    errors.add(errorMsg);
                    logger.error("Error generating report for citizen: {}", documento, e);
                }
            }

            // 6. Preparar resultado
            result.put("success", true);
            result.put("departmentId", departmentId);
            result.put("electionId", electionId);
            result.put("totalCitizens", totalCount);
            result.put("successCount", successCount);
            result.put("errorCount", errors.size());
            result.put("errors", errors);
            result.put("reportDirectory", reportDirectory);
            result.put("generationTimestamp", System.currentTimeMillis());

            logger.info("Completed FULL CITIZEN REPORTS generation for department {}: {}/{} reports generated successfully",
                    departmentId, successCount, totalCount);

        } catch (Exception e) {
            logger.error("Critical error generating department citizen reports for department {} and election {}",
                    departmentId, electionId, e);
            result.put("success", false);
            result.put("error", "Critical error: " + e.getMessage());
            result.put("totalCitizens", totalCount);
            result.put("successCount", successCount);
        }

        return result;
    }

    /**
     * Genera reportes FULL CITIZEN REPORT para todos los ciudadanos de un municipio específico
     * Guarda cada reporte como archivo ICE en la carpeta Reports/data
     *
     * @param municipalityId ID del municipio
     * @param electionId ID de la elección
     * @return Map con estadísticas del proceso de generación
     */
    @Override
    public Map<String, Object> generateMunicipalityCitizenReports(int municipalityId, int electionId) {
        logger.info("Generating FULL CITIZEN REPORTS for all citizens in municipality {} and election {}", municipalityId, electionId);

        Map<String, Object> result = new HashMap<>();
        List<String> errors = new ArrayList<>();
        int successCount = 0;
        int totalCount = 0;

        try {
            // 1. Obtener información de la elección
            Map<String, Object> electionInfoMap = connectionDB.getElectionInfo(electionId);
            if (electionInfoMap == null) {
                logger.error("Election {} not found", electionId);
                result.put("success", false);
                result.put("error", "Election not found: " + electionId);
                return result;
            }

            // 2. Obtener elecciones disponibles una vez
            List<Map<String, Object>> availableElectionsMap = connectionDB.getAllActiveElections();
            ElectionInfo[] availableElections = convertToElectionInfoArray(availableElectionsMap);

            // 3. Obtener todos los ciudadanos del municipio
            List<Map<String, Object>> citizens = connectionDB.getCitizensByMunicipality(municipalityId);
            totalCount = citizens.size();

            if (citizens.isEmpty()) {
                logger.warn("No citizens found for municipality {}", municipalityId);
                result.put("success", true);
                result.put("message", "No citizens found for municipality " + municipalityId);
                result.put("totalCitizens", 0);
                result.put("successCount", 0);
                return result;
            }

            // 4. Crear directorio si no existe
            String reportDirectory = "server/src/main/java/Reports/data";
            Path directoryPath = Paths.get(reportDirectory);
            if (!Files.exists(directoryPath)) {
                Files.createDirectories(directoryPath);
                logger.info("Created directory: {}", reportDirectory);
            }

            // 5. Generar reporte para cada ciudadano
            for (Map<String, Object> citizenData : citizens) {
                try {
                    String documento = (String) citizenData.get("documento");
                    String nombre = (String) citizenData.get("nombre");
                    String apellido = (String) citizenData.get("apellido");

                    // Crear assignment usando los datos ya obtenidos
                    CitizenVotingAssignment assignment = convertToCitizenVotingAssignmentFromData(citizenData, electionInfoMap);

                    // Crear configuración del reporte
                    CitizenReportsConfiguration config = new CitizenReportsConfiguration();
                    config.assignment = assignment;
                    config.availableElections = availableElections;
                    config.packageVersion = PACKAGE_VERSION;
                    config.generationTimestamp = System.currentTimeMillis();

                    // Generar nombre del archivo
                    String fileName = String.format("citizen_report_mun_%d_doc_%s.ice", municipalityId, documento);
                    String filePath = Paths.get(reportDirectory, fileName).toString();

                    // Guardar como archivo ICE
                    boolean saved = saveConfigurationToFile(config, filePath);

                    if (saved) {
                        successCount++;
                        logger.debug("Saved citizen report for {} {} ({})", apellido, nombre, documento);
                    } else {
                        errors.add("Failed to save report for citizen: " + documento);
                        logger.warn("Failed to save report for citizen: {}", documento);
                    }

                } catch (Exception e) {
                    String documento = (String) citizenData.getOrDefault("documento", "unknown");
                    String errorMsg = "Error generating report for citizen " + documento + ": " + e.getMessage();
                    errors.add(errorMsg);
                    logger.error("Error generating report for citizen: {}", documento, e);
                }
            }

            // 6. Preparar resultado
            result.put("success", true);
            result.put("municipalityId", municipalityId);
            result.put("electionId", electionId);
            result.put("totalCitizens", totalCount);
            result.put("successCount", successCount);
            result.put("errorCount", errors.size());
            result.put("errors", errors);
            result.put("reportDirectory", reportDirectory);
            result.put("generationTimestamp", System.currentTimeMillis());

            logger.info("Completed FULL CITIZEN REPORTS generation for municipality {}: {}/{} reports generated successfully",
                    municipalityId, successCount, totalCount);

        } catch (Exception e) {
            logger.error("Critical error generating municipality citizen reports for municipality {} and election {}",
                    municipalityId, electionId, e);
            result.put("success", false);
            result.put("error", "Critical error: " + e.getMessage());
            result.put("totalCitizens", totalCount);
            result.put("successCount", successCount);
        }

        return result;
    }

    /**
     * Genera reportes FULL CITIZEN REPORT para todos los ciudadanos de un puesto de votación específico
     * Guarda cada reporte como archivo ICE en la carpeta Reports/data
     *
     * @param puestoId ID del puesto de votación
     * @param electionId ID de la elección
     * @return Map con estadísticas del proceso de generación
     */
    @Override
    public Map<String, Object> generatePuestoCitizenReports(int puestoId, int electionId) {
        logger.info("Generating FULL CITIZEN REPORTS for all citizens in puesto {} and election {}", puestoId, electionId);

        Map<String, Object> result = new HashMap<>();
        List<String> errors = new ArrayList<>();
        int successCount = 0;
        int totalCount = 0;

        try {
            // 1. Obtener información de la elección
            Map<String, Object> electionInfoMap = connectionDB.getElectionInfo(electionId);
            if (electionInfoMap == null) {
                logger.error("Election {} not found", electionId);
                result.put("success", false);
                result.put("error", "Election not found: " + electionId);
                return result;
            }

            // 2. Obtener elecciones disponibles una vez
            List<Map<String, Object>> availableElectionsMap = connectionDB.getAllActiveElections();
            ElectionInfo[] availableElections = convertToElectionInfoArray(availableElectionsMap);

            // 3. Obtener todos los ciudadanos del puesto
            List<Map<String, Object>> citizens = connectionDB.getCitizensByPuesto(puestoId);
            totalCount = citizens.size();

            if (citizens.isEmpty()) {
                logger.warn("No citizens found for puesto {}", puestoId);
                result.put("success", true);
                result.put("message", "No citizens found for puesto " + puestoId);
                result.put("totalCitizens", 0);
                result.put("successCount", 0);
                return result;
            }

            // 4. Crear directorio si no existe
            String reportDirectory = "server/src/main/java/Reports/data";
            Path directoryPath = Paths.get(reportDirectory);
            if (!Files.exists(directoryPath)) {
                Files.createDirectories(directoryPath);
                logger.info("Created directory: {}", reportDirectory);
            }

            // 5. Generar reporte para cada ciudadano
            for (Map<String, Object> citizenData : citizens) {
                try {
                    String documento = (String) citizenData.get("documento");
                    String nombre = (String) citizenData.get("nombre");
                    String apellido = (String) citizenData.get("apellido");

                    // Crear assignment usando los datos ya obtenidos
                    CitizenVotingAssignment assignment = convertToCitizenVotingAssignmentFromData(citizenData, electionInfoMap);

                    // Crear configuración del reporte
                    CitizenReportsConfiguration config = new CitizenReportsConfiguration();
                    config.assignment = assignment;
                    config.availableElections = availableElections;
                    config.packageVersion = PACKAGE_VERSION;
                    config.generationTimestamp = System.currentTimeMillis();

                    // Generar nombre del archivo
                    String fileName = String.format("citizen_report_puesto_%d_doc_%s.ice", puestoId, documento);
                    String filePath = Paths.get(reportDirectory, fileName).toString();

                    // Guardar como archivo ICE
                    boolean saved = saveConfigurationToFile(config, filePath);

                    if (saved) {
                        successCount++;
                        logger.debug("Saved citizen report for {} {} ({})", apellido, nombre, documento);
                    } else {
                        errors.add("Failed to save report for citizen: " + documento);
                        logger.warn("Failed to save report for citizen: {}", documento);
                    }

                } catch (Exception e) {
                    String documento = (String) citizenData.getOrDefault("documento", "unknown");
                    String errorMsg = "Error generating report for citizen " + documento + ": " + e.getMessage();
                    errors.add(errorMsg);
                    logger.error("Error generating report for citizen: {}", documento, e);
                }
            }

            // 6. Preparar resultado
            result.put("success", true);
            result.put("puestoId", puestoId);
            result.put("electionId", electionId);
            result.put("totalCitizens", totalCount);
            result.put("successCount", successCount);
            result.put("errorCount", errors.size());
            result.put("errors", errors);
            result.put("reportDirectory", reportDirectory);
            result.put("generationTimestamp", System.currentTimeMillis());

            logger.info("Completed FULL CITIZEN REPORTS generation for puesto {}: {}/{} reports generated successfully",
                    puestoId, successCount, totalCount);

        } catch (Exception e) {
            logger.error("Critical error generating puesto citizen reports for puesto {} and election {}",
                    puestoId, electionId, e);
            result.put("success", false);
            result.put("error", "Critical error: " + e.getMessage());
            result.put("totalCitizens", totalCount);
            result.put("successCount", successCount);
        }

        return result;
    }

// =================== MÉTODO AUXILIAR ===================

    /**
     * Convierte datos de ciudadano obtenidos directamente de la consulta a CitizenVotingAssignment
     * Evita hacer consultas adicionales a la base de datos
     */
    private CitizenVotingAssignment convertToCitizenVotingAssignmentFromData(Map<String, Object> citizenData, Map<String, Object> electionInfoMap) {
        try {
            CitizenVotingAssignment assignment = new CitizenVotingAssignment();

            // Convert citizen info
            CitizenInfo citizen = new CitizenInfo();
            citizen.id = (Integer) citizenData.getOrDefault("ciudadano_id", 0);
            citizen.documento = (String) citizenData.getOrDefault("documento", "");
            citizen.nombre = (String) citizenData.getOrDefault("nombre", "");
            citizen.apellido = (String) citizenData.getOrDefault("apellido", "");
            assignment.citizen = citizen;

            // Convert location info
            LocationInfo location = new LocationInfo();
            location.departamentoId = (Integer) citizenData.getOrDefault("departamento_id", 0);
            location.departamentoNombre = (String) citizenData.getOrDefault("departamento_nombre", "");
            location.municipioId = (Integer) citizenData.getOrDefault("municipio_id", 0);
            location.municipioNombre = (String) citizenData.getOrDefault("municipio_nombre", "");
            location.puestoId = (Integer) citizenData.getOrDefault("puesto_id", 0);
            location.puestoNombre = (String) citizenData.getOrDefault("puesto_nombre", "");
            location.puestoDireccion = (String) citizenData.getOrDefault("puesto_direccion", "");
            location.puestoConsecutive = (Integer) citizenData.getOrDefault("puesto_consecutive", 0);
            location.mesaId = (Integer) citizenData.getOrDefault("mesa_id", 0);
            location.mesaConsecutive = (Integer) citizenData.getOrDefault("mesa_consecutive", 0);
            assignment.location = location;

            // Convert election info
            ElectionInfo election = new ElectionInfo();
            election.id = (Integer) electionInfoMap.getOrDefault("id", 0);
            election.nombre = (String) electionInfoMap.getOrDefault("nombre", "");
            election.estado = (String) electionInfoMap.getOrDefault("estado", "");

            // Handle timestamp conversion safely
            Object fechaInicio = electionInfoMap.get("fecha_inicio");
            if (fechaInicio instanceof java.sql.Timestamp) {
                election.fechaInicio = ((java.sql.Timestamp) fechaInicio).getTime();
            } else if (fechaInicio instanceof Long) {
                election.fechaInicio = (Long) fechaInicio;
            } else {
                election.fechaInicio = 0L;
            }

            Object fechaFin = electionInfoMap.get("fecha_fin");
            if (fechaFin instanceof java.sql.Timestamp) {
                election.fechaFin = ((java.sql.Timestamp) fechaFin).getTime();
            } else if (fechaFin instanceof Long) {
                election.fechaFin = (Long) fechaFin;
            } else {
                election.fechaFin = 0L;
            }

            assignment.election = election;
            assignment.generationTimestamp = System.currentTimeMillis();

            return assignment;

        } catch (Exception e) {
            logger.error("Error converting citizen data to CitizenVotingAssignment", e);
            throw new RuntimeException("Failed to convert citizen voting assignment from data", e);
        }
    }


}
