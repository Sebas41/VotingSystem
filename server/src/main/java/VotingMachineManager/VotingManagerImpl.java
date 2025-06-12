package VotingMachineManager;

import ConnectionDB.ConnectionDBinterface;
import VotingSystem.*;
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
 * Ice-based implementation of VotingManagerInterface
 * Uses Ice serialization for efficient binary storage and transmission
 */
public class VotingManagerImpl implements VotingManagerInterface {

    private static final Logger logger = LoggerFactory.getLogger(VotingManagerImpl.class);
    private final ConnectionDBinterface connectionDB;
    private final ObjectMapper jsonMapper;
    private final Communicator iceCommunicator;

    // Configuration constants
    private static final String PACKAGE_VERSION = "1.0";

    public VotingManagerImpl(ConnectionDBinterface connectionDB) {
        this.connectionDB = connectionDB;
        this.jsonMapper = new ObjectMapper();
        this.jsonMapper.enable(SerializationFeature.INDENT_OUTPUT);

        // Initialize Ice communicator for serialization
        this.iceCommunicator = Util.initialize();

        logger.info("VotingManagerImpl initialized with Ice serialization support");
    }

    @Override
    public VotingConfiguration generateMachineConfiguration(int mesaId, int electionId) {
        logger.info("Generating Ice configuration for mesa {} and election {}", mesaId, electionId);

        try {
            // 1. Get mesa information
            Map<String, Object> mesaInfoMap = connectionDB.getMesaConfiguration(mesaId);
            if (mesaInfoMap == null) {
                logger.error("Mesa {} not found", mesaId);
                return null;
            }

            // 2. Get election information
            Map<String, Object> electionInfoMap = connectionDB.getElectionInfo(electionId);
            if (electionInfoMap == null) {
                logger.error("Election {} not found", electionId);
                return null;
            }

            // 3. Get candidates for this election
            List<Map<String, Object>> candidatesMap = connectionDB.getCandidatesByElection(electionId);

            // 4. Get assigned citizens for this mesa
            List<Map<String, Object>> citizensMap = connectionDB.getCitizensByMesa(mesaId);

            // 5. Convert to Ice structs
            MesaInfo mesaInfo = convertToMesaInfo(mesaInfoMap);
            ElectionInfo electionInfo = convertToElectionInfo(electionInfoMap);
            Candidate[] candidates = convertToCandidates(candidatesMap);
            Citizen[] citizens = convertToCitizens(citizensMap);

            // 6. Build Ice configuration
            VotingConfiguration configuration = new VotingConfiguration();
            configuration.mesaInfo = mesaInfo;
            configuration.electionInfo = electionInfo;
            configuration.candidates = candidates;
            configuration.citizens = citizens;
            configuration.packageVersion = PACKAGE_VERSION;
            configuration.generationTimestamp = System.currentTimeMillis();

            logger.info("Ice configuration generated for mesa {} - {} citizens, {} candidates",
                    mesaId, citizens.length, candidates.length);

            return configuration;

        } catch (Exception e) {
            logger.error("Error generating Ice configuration for mesa {} and election {}", mesaId, electionId, e);
            return null;
        }
    }

    @Override
    public Map<Integer, VotingConfiguration> generateBatchMachineConfigurations(List<Integer> mesaIds, int electionId) {
        logger.info("Generating batch Ice configurations for {} mesas and election {}", mesaIds.size(), electionId);

        Map<Integer, VotingConfiguration> batchConfigurations = new ConcurrentHashMap<>();

        try {
            // 1. Get election info and candidates once (they're the same for all machines)
            Map<String, Object> electionInfoMap = connectionDB.getElectionInfo(electionId);
            if (electionInfoMap == null) {
                logger.error("Election {} not found", electionId);
                return batchConfigurations;
            }

            List<Map<String, Object>> candidatesMap = connectionDB.getCandidatesByElection(electionId);

            /* Convert to Ice structs once */
            ElectionInfo electionInfo = convertToElectionInfo(electionInfoMap);
            Candidate[] candidates = convertToCandidates(candidatesMap);

            // 2. Get citizens for all mesas in batch (optimized query)
            Map<Integer, List<Map<String, Object>>> citizensByMesa = connectionDB.getCitizensByMesaBatch(mesaIds);

            // 3. Generate configurations for each mesa
            for (Integer mesaId : mesaIds) {
                try {
                    Map<String, Object> mesaInfoMap = connectionDB.getMesaConfiguration(mesaId);
                    if (mesaInfoMap == null) {
                        logger.warn("Mesa {} not found, skipping", mesaId);
                        continue;
                    }

                    List<Map<String, Object>> assignedCitizensMap = citizensByMesa.getOrDefault(mesaId, new ArrayList<>());

                    // Convert to Ice structs
                    MesaInfo mesaInfo = convertToMesaInfo(mesaInfoMap);
                    Citizen[] assignedCitizens = convertToCitizens(assignedCitizensMap);

                    // Build Ice configuration
                    VotingConfiguration configuration = new VotingConfiguration();
                    configuration.mesaInfo = mesaInfo;
                    configuration.electionInfo = electionInfo;
                    configuration.candidates = candidates;
                    configuration.citizens = assignedCitizens;
                    configuration.packageVersion = PACKAGE_VERSION;
                    configuration.generationTimestamp = System.currentTimeMillis();

                    batchConfigurations.put(mesaId, configuration);

                } catch (Exception e) {
                    logger.error("Error generating Ice configuration for mesa {} in batch", mesaId, e);
                }
            }

            logger.info("Batch Ice configuration generated for {} mesas successfully", batchConfigurations.size());

        } catch (Exception e) {
            logger.error("Error in batch Ice configuration generation", e);
        }

        return batchConfigurations;
    }

    @Override
    public Map<Integer, VotingConfiguration> generateDepartmentConfigurations(int departmentId, int electionId) {
        logger.info("Generating Ice configurations for department {} and election {}", departmentId, electionId);

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
            logger.error("Error generating Ice department configurations for department {} and election {}",
                    departmentId, electionId, e);
            return new HashMap<>();
        }
    }

    @Override
    public Map<Integer, VotingConfiguration> generateAllMachineConfigurations(int electionId) {
        logger.info("Generating Ice configurations for ALL mesas and election {}", electionId);

        try {
            // Get all mesa IDs in the system
            List<Integer> allMesaIds = connectionDB.getAllMesaIds();

            if (allMesaIds.isEmpty()) {
                logger.warn("No mesas found in the system");
                return new HashMap<>();
            }

            logger.info("Processing {} total mesas for national election configuration", allMesaIds.size());

            Map<Integer, VotingConfiguration> allConfigurations = new ConcurrentHashMap<>();
            int batchSize = 1000;

            for (int i = 0; i < allMesaIds.size(); i += batchSize) {
                int endIndex = Math.min(i + batchSize, allMesaIds.size());
                List<Integer> batch = allMesaIds.subList(i, endIndex);

                logger.info("Processing batch {}/{} ({} mesas)",
                        (i / batchSize) + 1, (allMesaIds.size() + batchSize - 1) / batchSize, batch.size());

                Map<Integer, VotingConfiguration> batchConfigurations = generateBatchMachineConfigurations(batch, electionId);
                allConfigurations.putAll(batchConfigurations);
            }

            logger.info("Generated Ice configurations for {} mesas nationally", allConfigurations.size());
            return allConfigurations;

        } catch (Exception e) {
            logger.error("Error generating all Ice machine configurations for election {}", electionId, e);
            return new HashMap<>();
        }
    }

    @Override
    public Map<Integer, VotingConfiguration> generatePuestoConfigurations(int puestoId, int electionId) {
        logger.info("Generating Ice configurations for puesto {} and election {}", puestoId, electionId);

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
            return generateBatchMachineConfigurations(mesaIds, electionId);

        } catch (Exception e) {
            logger.error("Error generating Ice puesto configurations for puesto {} and election {}",
                    puestoId, electionId, e);
            return new HashMap<>();
        }
    }

    @Override
    public boolean validateConfiguration(VotingConfiguration configuration) {
        if (configuration == null) {
            return false;
        }

        try {
            boolean hasElectionInfo = configuration.electionInfo != null;
            boolean hasCandidates = configuration.candidates != null && configuration.candidates.length > 0;
            boolean hasCitizens = configuration.citizens != null;
            boolean hasMesaInfo = configuration.mesaInfo != null;

            if (!hasElectionInfo || !hasCandidates || !hasCitizens || !hasMesaInfo) {
                logger.warn("Ice configuration missing required sections");
                return false;
            }

            // Validate election info
            if (configuration.electionInfo.nombre == null || configuration.electionInfo.nombre.isEmpty() ||
                    configuration.electionInfo.estado == null || configuration.electionInfo.estado.isEmpty()) {
                logger.warn("Election info missing required fields");
                return false;
            }

            // Validate candidates
            for (Candidate candidate : configuration.candidates) {
                if (candidate.nombre == null || candidate.nombre.isEmpty() ||
                        candidate.partido == null || candidate.partido.isEmpty()) {
                    logger.warn("Candidate missing required fields");
                    return false;
                }
            }

            // Validate citizens
            for (Citizen citizen : configuration.citizens) {
                if (citizen.documento == null || citizen.documento.isEmpty() ||
                        citizen.nombre == null || citizen.nombre.isEmpty() ||
                        citizen.apellido == null || citizen.apellido.isEmpty()) {
                    logger.warn("Citizen missing required fields");
                    return false;
                }
            }

            logger.debug("Ice configuration validation passed");
            return true;

        } catch (Exception e) {
            logger.error("Error validating Ice configuration", e);
            return false;
        }
    }

    // REPLACE these two methods in VotingManagerImpl.java with pure Ice serialization:

    // REPLACE these two methods in VotingManagerImpl.java with pure Ice serialization:

    @Override
    public byte[] exportConfigurationToBytes(VotingConfiguration configuration) {
        try {
            // Use modern Ice 3.7+ API for native serialization
            com.zeroc.Ice.OutputStream out = new com.zeroc.Ice.OutputStream(iceCommunicator);

            // Write the VotingConfiguration directly using Ice's encoding
            out.writeValue(configuration);
            out.writePendingValues();

            return out.finished();
        } catch (Exception e) {
            logger.error("Error exporting Ice configuration to bytes", e);
            return new byte[0];
        }
    }

    @Override
    public VotingConfiguration importConfigurationFromBytes(byte[] data) {
        try {
            // Use modern Ice 3.7+ API for native deserialization
            com.zeroc.Ice.InputStream in = new com.zeroc.Ice.InputStream(iceCommunicator, data);

            // Create a container to hold the result
            final VotingConfiguration[] holder = new VotingConfiguration[1];

            // Read the value using Ice's callback mechanism
            in.readValue(value -> {
                holder[0] = (VotingConfiguration) value;
            });
            in.readPendingValues();

            return holder[0];
        } catch (Exception e) {
            logger.error("Error importing Ice configuration from bytes", e);
            return null;
        }
    }



    @Override
    public String exportConfigurationToJson(VotingConfiguration configuration) {
        try {
            // Convert Ice configuration to a JSON-friendly Map
            Map<String, Object> jsonMap = new HashMap<>();

            // Mesa info
            Map<String, Object> mesaInfoMap = new HashMap<>();
            if (configuration.mesaInfo != null) {
                mesaInfoMap.put("mesaId", configuration.mesaInfo.mesaId);
                mesaInfoMap.put("mesaConsecutive", configuration.mesaInfo.mesaConsecutive);
                mesaInfoMap.put("puestoId", configuration.mesaInfo.puestoId);
                mesaInfoMap.put("puestoNombre", configuration.mesaInfo.puestoNombre);
                mesaInfoMap.put("puestoDireccion", configuration.mesaInfo.puestoDireccion);
                mesaInfoMap.put("municipioId", configuration.mesaInfo.municipioId);
                mesaInfoMap.put("municipioNombre", configuration.mesaInfo.municipioNombre);
                mesaInfoMap.put("departamentoId", configuration.mesaInfo.departamentoId);
                mesaInfoMap.put("departamentoNombre", configuration.mesaInfo.departamentoNombre);
                mesaInfoMap.put("totalCiudadanos", configuration.mesaInfo.totalCiudadanos);
            }
            jsonMap.put("mesaInfo", mesaInfoMap);

            // Election info
            Map<String, Object> electionInfoMap = new HashMap<>();
            if (configuration.electionInfo != null) {
                electionInfoMap.put("id", configuration.electionInfo.id);
                electionInfoMap.put("nombre", configuration.electionInfo.nombre);
                electionInfoMap.put("estado", configuration.electionInfo.estado);
                electionInfoMap.put("fechaInicio", configuration.electionInfo.fechaInicio);
                electionInfoMap.put("fechaFin", configuration.electionInfo.fechaFin);
            }
            jsonMap.put("electionInfo", electionInfoMap);

            // Candidates
            List<Map<String, Object>> candidatesList = new ArrayList<>();
            if (configuration.candidates != null) {
                for (Candidate candidate : configuration.candidates) {
                    Map<String, Object> candidateMap = new HashMap<>();
                    candidateMap.put("id", candidate.id);
                    candidateMap.put("nombre", candidate.nombre);
                    candidateMap.put("partido", candidate.partido);
                    candidatesList.add(candidateMap);
                }
            }
            jsonMap.put("candidates", candidatesList);

            // Citizens
            List<Map<String, Object>> citizensList = new ArrayList<>();
            if (configuration.citizens != null) {
                for (Citizen citizen : configuration.citizens) {
                    Map<String, Object> citizenMap = new HashMap<>();
                    citizenMap.put("id", citizen.id);
                    citizenMap.put("documento", citizen.documento);
                    citizenMap.put("nombre", citizen.nombre);
                    citizenMap.put("apellido", citizen.apellido);
                    citizensList.add(citizenMap);
                }
            }
            jsonMap.put("citizens", citizensList);

            // Metadata
            jsonMap.put("packageVersion", configuration.packageVersion);
            jsonMap.put("generationTimestamp", new Date(configuration.generationTimestamp));

            // Summary
            Map<String, Object> summary = new HashMap<>();
            summary.put("totalCandidates", configuration.candidates != null ? configuration.candidates.length : 0);
            summary.put("totalAssignedCitizens", configuration.citizens != null ? configuration.citizens.length : 0);
            summary.put("mesaId", configuration.mesaInfo != null ? configuration.mesaInfo.mesaId : 0);
            jsonMap.put("summary", summary);

            return jsonMapper.writeValueAsString(jsonMap);

        } catch (Exception e) {
            logger.error("Error exporting Ice configuration to JSON", e);
            return "{}";
        }
    }

    @Override
    public Map<String, Object> getConfigurationStatistics(int electionId) {
        Map<String, Object> stats = new HashMap<>();

        try {
            // Get database metrics
            Map<String, Object> dbMetrics = connectionDB.getPerformanceMetrics();

            // Get election info
            Map<String, Object> electionInfo = connectionDB.getElectionInfo(electionId);

            // Get all mesa IDs
            List<Integer> allMesaIds = connectionDB.getAllMesaIds();

            // Get candidates
            List<Map<String, Object>> candidates = connectionDB.getCandidatesByElection(electionId);

            stats.put("totalMesas", allMesaIds.size());
            stats.put("totalCitizens", dbMetrics.get("total_citizens"));
            stats.put("totalCandidates", candidates.size());
            stats.put("electionName", electionInfo != null ? electionInfo.get("nombre") : "Unknown");
            stats.put("electionStatus", electionInfo != null ? electionInfo.get("estado") : "Unknown");
            stats.put("timestamp", new Date());
            stats.put("serializationType", "Ice Binary");

            int avgCitizensPerMesa = 979;
            long estimatedTotalRecords = (long) allMesaIds.size() * avgCitizensPerMesa;
            stats.put("estimatedTotalRecords", estimatedTotalRecords);

            logger.info("Ice configuration statistics generated for election {}", electionId);

        } catch (Exception e) {
            logger.error("Error getting Ice configuration statistics for election {}", electionId, e);
            stats.put("error", "Failed to generate statistics: " + e.getMessage());
        }

        return stats;
    }

    @Override
    public boolean isElectionReadyForConfiguration(int electionId) {
        try {
            // Check if election exists
            Map<String, Object> electionInfo = connectionDB.getElectionInfo(electionId);
            if (electionInfo == null) {
                logger.warn("Election {} not found", electionId);
                return false;
            }

            // Check if election has candidates
            List<Map<String, Object>> candidates = connectionDB.getCandidatesByElection(electionId);
            if (candidates.isEmpty()) {
                logger.warn("Election {} has no candidates", electionId);
                return false;
            }

            // Check election status
            String status = (String) electionInfo.get("estado");
            if (status == null || status.equals("CLOSED")) {
                logger.warn("Election {} is not in a valid status: {}", electionId, status);
                return false;
            }

            logger.info("Election {} is ready for Ice configuration generation", electionId);
            return true;

        } catch (Exception e) {
            logger.error("Error checking if election {} is ready", electionId, e);
            return false;
        }
    }

    @Override
    public boolean saveConfigurationToFile(VotingConfiguration configuration, String filePath) {
        try {
            byte[] configBytes = exportConfigurationToBytes(configuration);
            Files.write(Paths.get(filePath), configBytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            logger.debug("Ice configuration saved to file: {}", filePath);
            return true;
        } catch (Exception e) {
            logger.error("Error saving Ice configuration to file: {}", filePath, e);
            return false;
        }
    }

    @Override
    public VotingConfiguration loadConfigurationFromFile(String filePath) {
        try {
            byte[] configBytes = Files.readAllBytes(Paths.get(filePath));
            VotingConfiguration config = importConfigurationFromBytes(configBytes);
            logger.debug("Ice configuration loaded from file: {}", filePath);
            return config;
        } catch (Exception e) {
            logger.error("Error loading Ice configuration from file: {}", filePath, e);
            return null;
        }
    }

    // =================== HELPER METHODS FOR CONVERSION ===================

    private MesaInfo convertToMesaInfo(Map<String, Object> mesaInfoMap) {
        MesaInfo mesaInfo = new MesaInfo();
        mesaInfo.mesaId = (Integer) mesaInfoMap.get("mesa_id");
        mesaInfo.mesaConsecutive = (Integer) mesaInfoMap.get("mesa_consecutive");
        mesaInfo.puestoId = (Integer) mesaInfoMap.get("puesto_id");
        mesaInfo.puestoNombre = (String) mesaInfoMap.get("puesto_nombre");
        mesaInfo.puestoDireccion = (String) mesaInfoMap.get("puesto_direccion");
        mesaInfo.municipioId = (Integer) mesaInfoMap.get("municipio_id");
        mesaInfo.municipioNombre = (String) mesaInfoMap.get("municipio_nombre");
        mesaInfo.departamentoId = (Integer) mesaInfoMap.get("departamento_id");
        mesaInfo.departamentoNombre = (String) mesaInfoMap.get("departamento_nombre");
        mesaInfo.totalCiudadanos = (Integer) mesaInfoMap.get("total_ciudadanos");
        return mesaInfo;
    }

    private ElectionInfo convertToElectionInfo(Map<String, Object> electionInfoMap) {
        ElectionInfo electionInfo = new ElectionInfo();
        electionInfo.id = (Integer) electionInfoMap.get("id");
        electionInfo.nombre = (String) electionInfoMap.get("nombre");
        electionInfo.estado = (String) electionInfoMap.get("estado");

        // Convert Timestamp to long
        if (electionInfoMap.get("fecha_inicio") instanceof java.sql.Timestamp) {
            electionInfo.fechaInicio = ((java.sql.Timestamp) electionInfoMap.get("fecha_inicio")).getTime();
        }
        if (electionInfoMap.get("fecha_fin") instanceof java.sql.Timestamp) {
            electionInfo.fechaFin = ((java.sql.Timestamp) electionInfoMap.get("fecha_fin")).getTime();
        }

        return electionInfo;
    }

    private Candidate[] convertToCandidates(List<Map<String, Object>> candidatesMap) {
        Candidate[] candidates = new Candidate[candidatesMap.size()];
        for (int i = 0; i < candidatesMap.size(); i++) {
            Map<String, Object> candidateMap = candidatesMap.get(i);
            Candidate candidate = new Candidate();
            candidate.id = (Integer) candidateMap.get("id");
            candidate.nombre = (String) candidateMap.get("nombre");
            candidate.partido = (String) candidateMap.get("partido");
            candidates[i] = candidate;
        }
        return candidates;
    }

    private Citizen[] convertToCitizens(List<Map<String, Object>> citizensMap) {
        Citizen[] citizens = new Citizen[citizensMap.size()];
        for (int i = 0; i < citizensMap.size(); i++) {
            Map<String, Object> citizenMap = citizensMap.get(i);
            Citizen citizen = new Citizen();
            citizen.id = (Integer) citizenMap.get("id");
            citizen.documento = (String) citizenMap.get("documento");
            citizen.nombre = (String) citizenMap.get("nombre");
            citizen.apellido = (String) citizenMap.get("apellido");
            citizens[i] = citizen;
        }
        return citizens;
    }

    /**
     * Cleanup Ice communicator when shutting down
     */
    public void shutdown() {
        if (iceCommunicator != null) {
            iceCommunicator.destroy();
        }
    }
}