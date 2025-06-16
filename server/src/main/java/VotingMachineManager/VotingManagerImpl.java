package VotingMachineManager;

import ConnectionDB.ConnectionDBinterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.zeroc.Ice.Current;

import java.security.Timestamp;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import VotingsSystem.ConfigurationService;


public class VotingManagerImpl implements ConfigurationService {

    private static final Logger logger = LoggerFactory.getLogger(VotingManagerImpl.class);
    private final ConnectionDBinterface connectionDB;
    private static final String PACKAGE_VERSION = "1.0";

    private static final String FIELD_SEPARATOR = "-";
    private static final String RECORD_SEPARATOR = "#";
    private static final String ARRAY_SEPARATOR = "|";

    public VotingManagerImpl(ConnectionDBinterface connectionDB) {
        this.connectionDB = connectionDB;
        logger.info("VotingManagerImpl initialized for Ice communication with string formatting");
    }


    @Override
    public String getConfiguration(int mesaId, int electionId, Current current) {
        logger.debug("Ice request: getConfiguration for mesa {} election {}", mesaId, electionId);

        try {
            return generateMachineConfigurationString(mesaId, electionId);
        } catch (Exception e) {
            logger.error("Error generating configuration string for mesa {} election {}", mesaId, electionId, e);
            return createErrorString("Error generating configuration: " + e.getMessage());
        }
    }

    @Override
    public boolean isConfigurationReady(int mesaId, int electionId, Current current) {
        logger.debug("Ice request: isConfigurationReady for mesa {} election {}", mesaId, electionId);
        return validateMesaConfiguration(mesaId, electionId);
    }

    @Override
    public void preloadConfigurations(int[] mesaIds, int electionId, Current current) {
        logger.info("Ice request: preloadConfigurations for {} mesas election {}", mesaIds.length, electionId);

        try {
            // Preload configurations for each mesa (caching logic could be added here)
            for (int mesaId : mesaIds) {
                generateMachineConfigurationString(mesaId, electionId);
            }
            logger.info("Preloading completed for {} mesas", mesaIds.length);
        } catch (Exception e) {
            logger.error("Error during preloading for election {}", electionId, e);
        }
    }

    @Override
    public String[] getBatchConfigurations(int[] mesaIds, int electionId, Current current) {
        logger.debug("Ice request: getBatchConfigurations for {} mesas election {}", mesaIds.length, electionId);

        try {
            return generateBatchMachineConfigurationStrings(
                    Arrays.stream(mesaIds).boxed().collect(Collectors.toList()),
                    electionId
            );
        } catch (Exception e) {
            logger.error("Error generating batch configuration strings for election {}", electionId, e);
            return new String[]{createErrorString("Error generating batch configurations: " + e.getMessage())};
        }
    }

    @Override
    public String[] getDepartmentConfigurations(int departmentId, int electionId, Current current) {
        logger.debug("Ice request: getDepartmentConfigurations for department {} election {}", departmentId, electionId);

        try {
            return generateDepartmentConfigurationStrings(departmentId, electionId);
        } catch (Exception e) {
            logger.error("Error generating department configuration strings for department {} election {}", departmentId, electionId, e);
            return new String[]{createErrorString("Error generating department configurations: " + e.getMessage())};
        }
    }

    @Override
    public String[] getPuestoConfigurations(int puestoId, int electionId, Current current) {
        logger.debug("Ice request: getPuestoConfigurations for puesto {} election {}", puestoId, electionId);

        try {
            return generatePuestoConfigurationStrings(puestoId, electionId);
        } catch (Exception e) {
            logger.error("Error generating puesto configuration strings for puesto {} election {}", puestoId, electionId, e);
            return new String[]{createErrorString("Error generating puesto configurations: " + e.getMessage())};
        }
    }

    @Override
    public String getConfigurationStatistics(int electionId, Current current) {
        logger.debug("Ice request: getConfigurationStatistics for election {}", electionId);

        try {
            return generateConfigurationStatisticsString(electionId);
        } catch (Exception e) {
            logger.error("Error generating configuration statistics string for election {}", electionId, e);
            return createErrorString("Error generating statistics: " + e.getMessage());
        }
    }

    @Override
    public boolean isElectionReadyForConfiguration(int electionId, Current current) {
        return isElectionReadyForConfiguration(electionId);
    }


    public String generateMachineConfigurationString(int mesaId, int electionId) {
        logger.info("Generating machine configuration string for mesa {} and election {}", mesaId, electionId);

        try {
            Map<String, Object> mesaInfoMap = connectionDB.getMesaConfiguration(mesaId);
            if (mesaInfoMap == null) {
                logger.error("Mesa {} not found", mesaId);
                return createErrorString("Mesa not found");
            }


            Map<String, Object> electionInfoMap = connectionDB.getElectionInfo(electionId);
            if (electionInfoMap == null) {
                logger.error("Election {} not found", electionId);
                return createErrorString("Election not found");
            }

            List<Map<String, Object>> candidatesMap = connectionDB.getCandidatesByElection(electionId);


            List<Map<String, Object>> citizensMap = connectionDB.getCitizensByMesa(mesaId);


            StringBuilder config = new StringBuilder();

            config.append(formatMesaInfoString(mesaInfoMap)).append(RECORD_SEPARATOR);

            config.append(formatElectionInfoString(electionInfoMap)).append(RECORD_SEPARATOR);

            config.append(formatCandidatesArray(candidatesMap)).append(RECORD_SEPARATOR);

            config.append(formatCitizensArray(citizensMap)).append(RECORD_SEPARATOR);

            config.append(PACKAGE_VERSION).append(FIELD_SEPARATOR).append(System.currentTimeMillis());

            logger.info("Machine configuration string generated for mesa {} - {} citizens, {} candidates",
                    mesaId, citizensMap.size(), candidatesMap.size());

            logger.info("Election {} includes voting schedule restrictions", electionId);

            return config.toString();

        } catch (Exception e) {
            logger.error("Error generating machine configuration string for mesa {} and election {}", mesaId, electionId, e);
            return createErrorString("Error generating machine configuration: " + e.getMessage());
        }
    }

    public String[] generateBatchMachineConfigurationStrings(List<Integer> mesaIds, int electionId) {
        logger.info("Generating batch machine configuration strings for {} mesas and election {}", mesaIds.size(), electionId);

        try {
            // 1. Get election info and candidates once (they're the same for all machines)
            Map<String, Object> electionInfoMap = connectionDB.getElectionInfo(electionId);
            if (electionInfoMap == null) {
                logger.error("Election {} not found", electionId);
                return new String[]{createErrorString("Election not found")};
            }

            List<Map<String, Object>> candidatesMap = connectionDB.getCandidatesByElection(electionId);

            // Convert to formatted strings once
            String electionInfoString = formatElectionInfoString(electionInfoMap);
            String candidatesString = formatCandidatesArray(candidatesMap);

            // 2. Get citizens for all mesas in batch (optimized query)
            Map<Integer, List<Map<String, Object>>> citizensByMesa = connectionDB.getCitizensByMesaBatch(mesaIds);

            // 3. Generate configuration strings for each mesa
            List<String> batchConfigurations = new ArrayList<>();
            for (Integer mesaId : mesaIds) {
                try {
                    Map<String, Object> mesaInfoMap = connectionDB.getMesaConfiguration(mesaId);
                    if (mesaInfoMap == null) {
                        logger.warn("Mesa {} not found, skipping", mesaId);
                        continue;
                    }

                    List<Map<String, Object>> assignedCitizensMap = citizensByMesa.getOrDefault(mesaId, new ArrayList<>());

                    // Build configuration string
                    StringBuilder config = new StringBuilder();
                    config.append(formatMesaInfoString(mesaInfoMap)).append(RECORD_SEPARATOR);
                    config.append(electionInfoString).append(RECORD_SEPARATOR);
                    config.append(candidatesString).append(RECORD_SEPARATOR);
                    config.append(formatCitizensArray(assignedCitizensMap)).append(RECORD_SEPARATOR);
                    config.append(PACKAGE_VERSION).append(FIELD_SEPARATOR).append(System.currentTimeMillis());

                    batchConfigurations.add(config.toString());

                } catch (Exception e) {
                    logger.error("Error generating configuration string for mesa {} in batch", mesaId, e);
                }
            }

            logger.info("Batch configuration strings generated for {} mesas successfully", batchConfigurations.size());
            return batchConfigurations.toArray(new String[0]);

        } catch (Exception e) {
            logger.error("Error in batch configuration string generation", e);
            return new String[]{createErrorString("Error in batch generation: " + e.getMessage())};
        }
    }

    public String[] generateDepartmentConfigurationStrings(int departmentId, int electionId) {
        logger.info("Generating configuration strings for department {} and election {}", departmentId, electionId);

        try {
            // Get all mesa IDs for the department
            List<Integer> mesaIds = connectionDB.getMesaIdsByDepartment(departmentId);

            if (mesaIds.isEmpty()) {
                logger.warn("No mesas found for department {}", departmentId);
                return new String[]{createErrorString("No mesas found for department")};
            }

            // Use batch generation for efficiency
            return generateBatchMachineConfigurationStrings(mesaIds, electionId);

        } catch (Exception e) {
            logger.error("Error generating department configuration strings for department {} and election {}",
                    departmentId, electionId, e);
            return new String[]{createErrorString("Error generating department configurations: " + e.getMessage())};
        }
    }

    public String[] generatePuestoConfigurationStrings(int puestoId, int electionId) {
        logger.info("Generating configuration strings for puesto {} and election {}", puestoId, electionId);

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
                return new String[]{createErrorString("No mesas found for puesto")};
            }

            logger.info("Found {} mesas in puesto {}", mesaIds.size(), puestoId);

            // Use batch generation for efficiency
            return generateBatchMachineConfigurationStrings(mesaIds, electionId);

        } catch (Exception e) {
            logger.error("Error generating puesto configuration strings for puesto {} and election {}",
                    puestoId, electionId, e);
            return new String[]{createErrorString("Error generating puesto configurations: " + e.getMessage())};
        }
    }

    public String generateConfigurationStatisticsString(int electionId) {
        logger.info("Generating configuration statistics string for election {}", electionId);

        try {
            // Get database metrics
            Map<String, Object> dbMetrics = connectionDB.getPerformanceMetrics();

            // Get election info
            Map<String, Object> electionInfo = connectionDB.getElectionInfo(electionId);

            // Get all mesa IDs
            List<Integer> allMesaIds = connectionDB.getAllMesaIds();

            // Get candidates
            List<Map<String, Object>> candidates = connectionDB.getCandidatesByElection(electionId);

            // Formato: totalMesas-totalCitizens-totalCandidates-electionName-electionStatus-timestamp-serializationType-estimatedTotalRecords
            StringBuilder stats = new StringBuilder();

            stats.append(allMesaIds.size()).append(FIELD_SEPARATOR);
            stats.append(dbMetrics.get("total_citizens")).append(FIELD_SEPARATOR);
            stats.append(candidates.size()).append(FIELD_SEPARATOR);
            stats.append(electionInfo != null ? electionInfo.get("nombre") : "Unknown").append(FIELD_SEPARATOR);
            stats.append(electionInfo != null ? electionInfo.get("estado") : "Unknown").append(FIELD_SEPARATOR);
            stats.append(System.currentTimeMillis()).append(FIELD_SEPARATOR);
            stats.append("String Formatted").append(FIELD_SEPARATOR);

            int avgCitizensPerMesa = 979;
            long estimatedTotalRecords = (long) allMesaIds.size() * avgCitizensPerMesa;
            stats.append(estimatedTotalRecords);

            logger.info("Configuration statistics string generated for election {}", electionId);
            return stats.toString();

        } catch (Exception e) {
            logger.error("Error getting configuration statistics string for election {}", electionId, e);
            return createErrorString("Failed to generate statistics: " + e.getMessage());
        }
    }


    private String createErrorString(String message) {
        return "ERROR" + FIELD_SEPARATOR + message + FIELD_SEPARATOR + System.currentTimeMillis();
    }

    private String formatMesaInfoString(Map<String, Object> mesaInfoMap) {
        // Formato: mesaId-mesaConsecutive-puestoId-puestoNombre-puestoDireccion-municipioId-municipioNombre-departamentoId-departamentoNombre-totalCiudadanos
        return String.join(FIELD_SEPARATOR,
                String.valueOf(mesaInfoMap.get("mesa_id")),
                String.valueOf(mesaInfoMap.get("mesa_consecutive")),
                String.valueOf(mesaInfoMap.get("puesto_id")),
                String.valueOf(mesaInfoMap.get("puesto_nombre")),
                String.valueOf(mesaInfoMap.get("puesto_direccion")),
                String.valueOf(mesaInfoMap.get("municipio_id")),
                String.valueOf(mesaInfoMap.get("municipio_nombre")),
                String.valueOf(mesaInfoMap.get("departamento_id")),
                String.valueOf(mesaInfoMap.get("departamento_nombre")),
                String.valueOf(mesaInfoMap.get("total_ciudadanos"))
        );
    }

    private String formatElectionInfoString(Map<String, Object> electionInfoMap) {
        java.sql.Timestamp fechaInicio = (java.sql.Timestamp) electionInfoMap.get("fecha_inicio");
        java.sql.Timestamp fechaFin = (java.sql.Timestamp) electionInfoMap.get("fecha_fin");

        final int JORNADA_HORA_INICIO = 8;   // 8:00 AM
        final int JORNADA_HORA_FIN = 18;     // 6:00 PM

        Calendar cal = Calendar.getInstance();
        cal.setTime(fechaInicio);

        // Jornada: 8:00 AM - 6:00 PM del día de la elección
        cal.set(Calendar.HOUR_OF_DAY, JORNADA_HORA_INICIO);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long jornadaInicio = cal.getTimeInMillis();

        cal.set(Calendar.HOUR_OF_DAY, JORNADA_HORA_FIN);
        long jornadaFin = cal.getTimeInMillis();

        logger.debug(" Horarios de jornada calculados para elección {}:", electionInfoMap.get("id"));
        logger.debug("   - Inicio: {} ({})", new Date(jornadaInicio), jornadaInicio);
        logger.debug("   - Fin: {} ({})", new Date(jornadaFin), jornadaFin);

        return String.join(FIELD_SEPARATOR,
                String.valueOf(electionInfoMap.get("id")),
                String.valueOf(electionInfoMap.get("nombre")),
                String.valueOf(electionInfoMap.get("estado")),
                String.valueOf(fechaInicio.getTime()),
                String.valueOf(fechaFin.getTime()),
                String.valueOf(jornadaInicio),
                String.valueOf(jornadaFin)
        );
    }

    private String formatCandidatesArray(List<Map<String, Object>> candidatesMap) {
        // Formato: candidate1|candidate2|candidate3 (cada candidate como id:nombre:partido)
        List<String> formattedCandidates = new ArrayList<>();
        for (Map<String, Object> candidate : candidatesMap) {
            formattedCandidates.add(
                    candidate.get("id") + ":" +
                            candidate.get("nombre") + ":" +
                            candidate.get("partido")
            );
        }
        return String.join(ARRAY_SEPARATOR, formattedCandidates);
    }

    private String formatCitizensArray(List<Map<String, Object>> citizensMap) {
        // Formato: citizen1|citizen2|citizen3 (cada citizen como id:documento:nombre:apellido)
        List<String> formattedCitizens = new ArrayList<>();
        for (Map<String, Object> citizen : citizensMap) {
            formattedCitizens.add(
                    citizen.get("id") + ":" +
                            citizen.get("documento") + ":" +
                            citizen.get("nombre") + ":" +
                            citizen.get("apellido")
            );
        }
        return String.join(ARRAY_SEPARATOR, formattedCitizens);
    }


    public boolean validateMesaConfiguration(int mesaId, int electionId) {
        try {
            // Check if mesa exists
            Map<String, Object> mesaInfo = connectionDB.getMesaConfiguration(mesaId);
            if (mesaInfo == null) {
                logger.warn("Mesa {} not found", mesaId);
                return false;
            }

            // Check if election exists and is ready
            return isElectionReadyForConfiguration(electionId);

        } catch (Exception e) {
            logger.error("Error validating mesa {} configuration for election {}", mesaId, electionId, e);
            return false;
        }
    }

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

            logger.debug("Election {} is ready for configuration generation", electionId);
            return true;

        } catch (Exception e) {
            logger.error("Error checking if election {} is ready", electionId, e);
            return false;
        }
    }


    public List<Integer> getAllMesaIds() {
        try {
            return connectionDB.getAllMesaIds();
        } catch (Exception e) {
            logger.error("Error obteniendo IDs de todas las mesas: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }


    public List<Integer> getMesaIdsByDepartment(int departmentId) {
        try {
            return connectionDB.getMesaIdsByDepartment(departmentId);
        } catch (Exception e) {
            logger.error("Error obteniendo mesas por departamento {}: {}", departmentId, e.getMessage(), e);
            return new ArrayList<>();
        }
    }


    public List<Integer> getMesaIdsByPuesto(int puestoId) {
        try {
            List<Integer> mesaIds = new ArrayList<>();
            List<Integer> allMesaIds = connectionDB.getAllMesaIds();

            // Filter mesas that belong to this puesto
            for (Integer mesaId : allMesaIds) {
                Map<String, Object> mesaConfig = connectionDB.getMesaConfiguration(mesaId);
                if (mesaConfig != null && puestoId == (Integer) mesaConfig.get("puesto_id")) {
                    mesaIds.add(mesaId);
                }
            }

            logger.info("Found {} mesas for puesto {}", mesaIds.size(), puestoId);
            return mesaIds;

        } catch (Exception e) {
            logger.error("Error obteniendo mesas por puesto {}: {}", puestoId, e.getMessage(), e);
            return new ArrayList<>();
        }
    }


    public boolean isValidElectionStatus(String status) {
        return status != null && (
                status.equals("PRE") ||
                        status.equals("DURING") ||
                        status.equals("CLOSED")
        );
    }


    public Map<String, Object> getElectionDetails(int electionId) {
        try {
            Map<String, Object> electionInfo = connectionDB.getElectionInfo(electionId);
            if (electionInfo == null) {
                return null;
            }

            // Agregar información adicional
            List<Map<String, Object>> candidates = connectionDB.getCandidatesByElection(electionId);
            List<Integer> allMesas = connectionDB.getAllMesaIds();

            Map<String, Object> details = new HashMap<>(electionInfo);
            details.put("total_candidates", candidates.size());
            details.put("total_mesas", allMesas.size());
            details.put("candidates", candidates);

            return details;

        } catch (Exception e) {
            logger.error("Error obteniendo detalles de elección {}: {}", electionId, e.getMessage(), e);
            return null;
        }
    }


    public Map<String, Object> getSystemStatistics() {
        try {
            Map<String, Object> stats = new HashMap<>();

            // Obtener métricas básicas
            Map<String, Object> dbMetrics = connectionDB.getPerformanceMetrics();
            List<Integer> allMesas = connectionDB.getAllMesaIds();

            stats.put("total_mesas", allMesas.size());
            stats.put("total_citizens", dbMetrics.get("total_citizens"));
            stats.put("total_puestos", dbMetrics.get("total_puestos"));
            stats.put("timestamp", new Date());
            stats.put("package_version", PACKAGE_VERSION);

            logger.info("System statistics generated: {} mesas, {} citizens",
                    allMesas.size(), dbMetrics.get("total_citizens"));

            return stats;

        } catch (Exception e) {
            logger.error("Error obteniendo estadísticas del sistema: {}", e.getMessage(), e);
            return new HashMap<>();
        }
    }


    public Map<String, Object> runConfigurationDiagnostic(int electionId) {
        Map<String, Object> diagnostic = new HashMap<>();

        try {
            logger.info("Running configuration diagnostic for election {}", electionId);

            // 1. Verificar elección
            Map<String, Object> electionInfo = connectionDB.getElectionInfo(electionId);
            diagnostic.put("election_exists", electionInfo != null);

            if (electionInfo != null) {
                diagnostic.put("election_name", electionInfo.get("nombre"));
                diagnostic.put("election_status", electionInfo.get("estado"));
            }

            // 2. Verificar candidatos
            List<Map<String, Object>> candidates = connectionDB.getCandidatesByElection(electionId);
            diagnostic.put("candidates_count", candidates.size());
            diagnostic.put("has_candidates", !candidates.isEmpty());

            // 3. Verificar mesas
            List<Integer> allMesas = connectionDB.getAllMesaIds();
            diagnostic.put("total_mesas", allMesas.size());
            diagnostic.put("has_mesas", !allMesas.isEmpty());

            // 4. Verificar ciudadanos
            Map<String, Object> dbMetrics = connectionDB.getPerformanceMetrics();
            long totalCitizens = (Long) dbMetrics.get("total_citizens");
            diagnostic.put("total_citizens", totalCitizens);
            diagnostic.put("has_citizens", totalCitizens > 0);

            // 5. Verificar integridad
            boolean configurationReady = isElectionReadyForConfiguration(electionId);
            diagnostic.put("configuration_ready", configurationReady);

            // 6. Timestamp
            diagnostic.put("diagnostic_timestamp", new Date());

            logger.info("Configuration diagnostic completed for election {}: ready={}",
                    electionId, configurationReady);

            return diagnostic;

        } catch (Exception e) {
            logger.error("Error running configuration diagnostic for election {}: {}",
                    electionId, e.getMessage(), e);

            diagnostic.put("error", true);
            diagnostic.put("error_message", e.getMessage());
            diagnostic.put("diagnostic_timestamp", new Date());

            return diagnostic;
        }
    }
}