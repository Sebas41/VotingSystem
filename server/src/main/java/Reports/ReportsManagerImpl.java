package Reports;

import ConnectionDB.ConnectionDBinterface;
import com.zeroc.Ice.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.Exception;
import java.lang.Object;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

// =================== IMPORTS ACTUALIZADOS DE ReportsSystem ===================
import ReportsSystem.ReportsService;

public class ReportsManagerImpl implements ReportsService {

    private static final Logger logger = LoggerFactory.getLogger(ReportsManagerImpl.class);
    private final ConnectionDBinterface connectionDB;
    private static final String PACKAGE_VERSION = "1.0";

    // Delimitadores para formatear strings (como en máquina de café)
    private static final String FIELD_SEPARATOR = "-";     // Para separar campos
    private static final String RECORD_SEPARATOR = "#";    // Para separar registros
    private static final String ARRAY_SEPARATOR = "|";     // Para separar arrays

    public ReportsManagerImpl(ConnectionDBinterface connectionDB) {
        this.connectionDB = connectionDB;
        logger.info("ReportsManagerImpl initialized for Ice communication with string formatting");
    }

    // =================== MÉTODOS @Override SIMPLIFICADOS ===================

    @Override
    public String getCitizenReports(String documento, int electionId, Current current) {
        logger.debug("Ice request: getCitizenReports for document {} election {}", documento, electionId);

        try {
            return generateCitizenReportString(documento, electionId);
        } catch (Exception e) {
            logger.error("Error generating citizen report string for document {} election {}", documento, electionId, e);
            return createErrorString("Error generating citizen report: " + e.getMessage());
        }
    }

    @Override
    public String getElectionReports(int electionId, Current current) {
        logger.debug("Ice request: getElectionReports for election {}", electionId);

        try {
            return generateElectionResultsReportString(electionId);
        } catch (Exception e) {
            logger.error("Error generating election report string for election {}", electionId, e);
            return createErrorString("Error generating election report: " + e.getMessage());
        }
    }

    @Override
    public String getGeographicReports(int locationId, String locationType, int electionId, Current current) {
        logger.debug("Ice request: getGeographicReports for {} {} election {}", locationType, locationId, electionId);

        try {
            switch (locationType.toLowerCase()) {
                case "department":
                case "departamento":
                    return generateDepartmentReportString(locationId, electionId);
                case "municipality":
                case "municipio":
                    return generateMunicipalityReportString(locationId, electionId);
                case "puesto":
                    return generatePuestoReportString(locationId, electionId);
                default:
                    logger.warn("Unknown location type: {}", locationType);
                    return createErrorString("Unknown location type: " + locationType);
            }
        } catch (Exception e) {
            logger.error("Error generating geographic report string for {} {} election {}", locationType, locationId, electionId, e);
            return createErrorString("Error generating geographic report: " + e.getMessage());
        }
    }

    @Override
    public String[] searchCitizenReports(String nombre, String apellido, int electionId, int limit, Current current) {
        logger.debug("Ice request: searchCitizenReports for {} {} election {} limit {}", nombre, apellido, electionId, limit);

        try {
            return searchCitizenReportsStrings(nombre, apellido, electionId, limit);
        } catch (Exception e) {
            logger.error("Error searching citizen reports strings for {} {}", nombre, apellido, e);
            return new String[]{createErrorString("Error searching citizen reports: " + e.getMessage())};
        }
    }

    @Override
    public String[] getMesaCitizenReports(int mesaId, int electionId, Current current) {
        logger.debug("Ice request: getMesaCitizenReports for mesa {} election {}", mesaId, electionId);

        try {
            return generateMesaCitizenReportsStrings(mesaId, electionId);
        } catch (Exception e) {
            logger.error("Error generating mesa citizen reports strings for mesa {} election {}", mesaId, electionId, e);
            return new String[]{createErrorString("Error generating mesa citizen reports: " + e.getMessage())};
        }
    }

    @Override
    public boolean validateCitizenEligibility(String documento, Current current) {
        return validateCitizenEligibility(documento);
    }

    @Override
    public boolean areReportsReady(int electionId, Current current) {
        return isElectionReadyForReports(electionId);
    }

    @Override
    public String[] getAvailableElections(Current current) {
        try {
            List<Map<String, Object>> electionsMap = getAvailableElections();

            // Convierte cada elección a string formateado
            String[] electionsArray = new String[electionsMap.size()];
            for (int i = 0; i < electionsMap.size(); i++) {
                electionsArray[i] = formatElectionString(electionsMap.get(i));
            }
            return electionsArray;

        } catch (Exception e) {
            logger.error("Error getting available elections strings", e);
            return new String[]{createErrorString("Error getting available elections: " + e.getMessage())};
        }
    }

    @Override
    public void preloadReports(int electionId, Current current) {
        logger.info("Ice request: preloadReports for election {}", electionId);

        try {
            // Preload election report
            generateElectionResultsReportString(electionId);

            // Preload some geographic reports
            List<Map<String, Object>> departments = connectionDB.getAllDepartments();
            for (Map<String, Object> dept : departments) {
                int deptId = (Integer) dept.get("id");
                generateDepartmentReportString(deptId, electionId);
            }

            logger.info("Preloading completed for election {}", electionId);
        } catch (Exception e) {
            logger.error("Error during preloading for election {}", electionId, e);
        }
    }

    // =================== MÉTODOS PARA GENERAR STRINGS FORMATEADOS ===================

    public String generateCitizenReportString(String documento, int electionId) {
        logger.info("Generating citizen report string for document {} and election {}", documento, electionId);

        try {
            Map<String, Object> assignmentMap = connectionDB.getCitizenVotingAssignment(documento);
            if (assignmentMap == null) {
                logger.warn("No voting assignment found for document: {}", documento);
                return createErrorString("No voting assignment found");
            }

            Map<String, Object> electionInfoMap = connectionDB.getElectionInfo(electionId);
            if (electionInfoMap == null) {
                logger.error("Election {} not found", electionId);
                return createErrorString("Election not found");
            }

            List<Map<String, Object>> availableElectionsMap = connectionDB.getAllActiveElections();

            // Formato: CITIZEN_DATA#LOCATION_DATA#ELECTION_DATA#AVAILABLE_ELECTIONS
            StringBuilder report = new StringBuilder();

            // 1. Citizen data: id-documento-nombre-apellido
            report.append(formatCitizenString(assignmentMap)).append(RECORD_SEPARATOR);

            // 2. Location data: deptId-deptNombre-munId-munNombre-puestoId-puestoNombre-mesaId
            report.append(formatLocationString(assignmentMap)).append(RECORD_SEPARATOR);

            // 3. Election data: id-nombre-estado-fechaInicio-fechaFin
            report.append(formatElectionString(electionInfoMap)).append(RECORD_SEPARATOR);

            // 4. Available elections: election1|election2|election3
            report.append(formatElectionsArray(availableElectionsMap)).append(RECORD_SEPARATOR);

            // 5. Metadata: packageVersion-timestamp
            report.append(PACKAGE_VERSION).append(FIELD_SEPARATOR).append(System.currentTimeMillis());

            logger.info("Citizen report string generated for document {}", documento);
            return report.toString();

        } catch (Exception e) {
            logger.error("Error generating citizen report string for document {} and election {}", documento, electionId, e);
            return createErrorString("Error generating citizen report: " + e.getMessage());
        }
    }

    public String[] searchCitizenReportsStrings(String nombre, String apellido, int electionId, int limit) {
        logger.info("Searching citizen report strings for name: {} {} (election {}, limit {})", nombre, apellido, electionId, limit);

        try {
            // 1. Search citizens by name
            List<Map<String, Object>> citizensMap = connectionDB.searchCitizensByName(nombre, apellido, limit);

            // 2. Get election info once
            Map<String, Object> electionInfoMap = connectionDB.getElectionInfo(electionId);
            if (electionInfoMap == null) {
                logger.error("Election {} not found", electionId);
                return new String[]{createErrorString("Election not found")};
            }

            // 3. Get available elections once
            List<Map<String, Object>> availableElectionsMap = connectionDB.getAllActiveElections();
            String availableElectionsString = formatElectionsArray(availableElectionsMap);
            String electionString = formatElectionString(electionInfoMap);

            // 4. Generate report string for each citizen found
            List<String> results = new ArrayList<>();
            for (Map<String, Object> citizenMap : citizensMap) {
                try {
                    String documento = (String) citizenMap.get("documento");

                    // Get full assignment for this citizen
                    Map<String, Object> assignmentMap = connectionDB.getCitizenVotingAssignment(documento);
                    if (assignmentMap != null) {
                        StringBuilder report = new StringBuilder();
                        report.append(formatCitizenString(assignmentMap)).append(RECORD_SEPARATOR);
                        report.append(formatLocationString(assignmentMap)).append(RECORD_SEPARATOR);
                        report.append(electionString).append(RECORD_SEPARATOR);
                        report.append(availableElectionsString).append(RECORD_SEPARATOR);
                        report.append(PACKAGE_VERSION).append(FIELD_SEPARATOR).append(System.currentTimeMillis());

                        results.add(report.toString());
                    }
                } catch (Exception e) {
                    logger.warn("Error generating report for citizen in search results", e);
                }
            }

            logger.info("Found {} citizen report strings for name search: {} {}", results.size(), nombre, apellido);
            return results.toArray(new String[0]);

        } catch (Exception e) {
            logger.error("Error searching citizen report strings for name: {} {}", nombre, apellido, e);
            return new String[]{createErrorString("Error searching citizen reports: " + e.getMessage())};
        }
    }

    public String generateElectionResultsReportString(int electionId) {
        logger.info("Generating election results report string for election {}", electionId);

        try {
            Map<String, Object> resultsMap = connectionDB.getElectionResultsSummary(electionId);
            if (resultsMap == null) {
                logger.error("No results found for election {}", electionId);
                return createErrorString("No results found for election");
            }

            Map<String, Object> nationalStatsMap = connectionDB.getVotingStatsByDepartment(electionId, 0);
            List<Map<String, Object>> departmentsMap = connectionDB.getAllDepartments();

            // Formato: ELECTION_INFO#CANDIDATE_RESULTS#NATIONAL_STATS#AVAILABLE_LOCATIONS#METADATA
            StringBuilder report = new StringBuilder();

            // 1. Election info
            report.append(formatElectionResultsString(resultsMap)).append(RECORD_SEPARATOR);

            // 2. National stats
            report.append(formatGeographicStatsString(nationalStatsMap, "national", electionId)).append(RECORD_SEPARATOR);

            // 3. Available locations (departments)
            report.append(formatLocationsArray(departmentsMap, "department")).append(RECORD_SEPARATOR);

            // 4. Metadata
            report.append(PACKAGE_VERSION).append(FIELD_SEPARATOR).append(System.currentTimeMillis());

            logger.info("Election results report string generated for election {}", electionId);
            return report.toString();

        } catch (Exception e) {
            logger.error("Error generating election results report string for election {}", electionId, e);
            return createErrorString("Error generating election results report: " + e.getMessage());
        }
    }

    public String generateDepartmentReportString(int departmentId, int electionId) {
        logger.info("Generating department report string for department {} and election {}", departmentId, electionId);

        try {
            // 1. Get department voting stats
            Map<String, Object> statsMap = connectionDB.getVotingStatsByDepartment(electionId, departmentId);

            // 2. Get municipalities in this department
            List<Map<String, Object>> municipalitiesMap = connectionDB.getMunicipalitiesByDepartment(departmentId);

            // 3. Get sample citizen assignments (first 10 citizens in department)
            List<Integer> mesaIds = connectionDB.getMesaIdsByDepartment(departmentId);
            List<Map<String, Object>> sampleCitizens = new ArrayList<>();
            if (!mesaIds.isEmpty()) {
                sampleCitizens = connectionDB.getCitizensByMesa(mesaIds.get(0));
                if (sampleCitizens.size() > 10) {
                    sampleCitizens = sampleCitizens.subList(0, 10);
                }
            }

            // Formato: STATS#SUB_LOCATIONS#SAMPLE_ASSIGNMENTS#METADATA
            StringBuilder report = new StringBuilder();

            // 1. Geographic stats
            report.append(formatGeographicStatsString(statsMap, "department", electionId)).append(RECORD_SEPARATOR);

            // 2. Sub-locations (municipalities)
            report.append(formatLocationsArray(municipalitiesMap, "municipality")).append(RECORD_SEPARATOR);

            // 3. Sample assignments
            report.append(formatSampleAssignmentsArray(sampleCitizens, electionId)).append(RECORD_SEPARATOR);

            // 4. Metadata
            report.append(PACKAGE_VERSION).append(FIELD_SEPARATOR).append(System.currentTimeMillis());

            logger.info("Department report string generated for department {}", departmentId);
            return report.toString();

        } catch (Exception e) {
            logger.error("Error generating department report string for department {} and election {}", departmentId, electionId, e);
            return createErrorString("Error generating department report: " + e.getMessage());
        }
    }

    public String generateMunicipalityReportString(int municipalityId, int electionId) {
        logger.info("Generating municipality report string for municipality {} and election {}", municipalityId, electionId);

        try {
            // 1. Get municipality voting stats
            Map<String, Object> statsMap = connectionDB.getVotingStatsByMunicipality(electionId, municipalityId);

            // 2. Get puestos in this municipality
            List<Map<String, Object>> puestosMap = connectionDB.getPuestosByMunicipality(municipalityId);

            // Formato: STATS#SUB_LOCATIONS#METADATA
            StringBuilder report = new StringBuilder();

            // 1. Geographic stats
            report.append(formatGeographicStatsString(statsMap, "municipality", electionId)).append(RECORD_SEPARATOR);

            // 2. Sub-locations (puestos)
            report.append(formatLocationsArray(puestosMap, "puesto")).append(RECORD_SEPARATOR);

            // 3. Empty sample assignments (municipality level)
            report.append("").append(RECORD_SEPARATOR);

            // 4. Metadata
            report.append(PACKAGE_VERSION).append(FIELD_SEPARATOR).append(System.currentTimeMillis());

            logger.info("Municipality report string generated for municipality {}", municipalityId);
            return report.toString();

        } catch (Exception e) {
            logger.error("Error generating municipality report string for municipality {} and election {}", municipalityId, electionId, e);
            return createErrorString("Error generating municipality report: " + e.getMessage());
        }
    }

    public String generatePuestoReportString(int puestoId, int electionId) {
        logger.info("Generating puesto report string for puesto {} and election {}", puestoId, electionId);

        try {
            // 1. Get puesto voting stats
            Map<String, Object> statsMap = connectionDB.getVotingStatsByPuesto(electionId, puestoId);

            // 2. Get mesas in this puesto
            List<Map<String, Object>> mesasMap = connectionDB.getMesasByPuesto(puestoId);

            // Formato: STATS#SUB_LOCATIONS#METADATA
            StringBuilder report = new StringBuilder();

            // 1. Geographic stats
            report.append(formatGeographicStatsString(statsMap, "puesto", electionId)).append(RECORD_SEPARATOR);

            // 2. Sub-locations (mesas)
            report.append(formatLocationsArray(mesasMap, "mesa")).append(RECORD_SEPARATOR);

            // 3. Empty sample assignments (puesto level)
            report.append("").append(RECORD_SEPARATOR);

            // 4. Metadata
            report.append(PACKAGE_VERSION).append(FIELD_SEPARATOR).append(System.currentTimeMillis());

            logger.info("Puesto report string generated for puesto {}", puestoId);
            return report.toString();

        } catch (Exception e) {
            logger.error("Error generating puesto report string for puesto {} and election {}", puestoId, electionId, e);
            return createErrorString("Error generating puesto report: " + e.getMessage());
        }
    }

    public String[] generateMesaCitizenReportsStrings(int mesaId, int electionId) {
        logger.info("Generating citizen report strings for all citizens in mesa {} and election {}", mesaId, electionId);

        try {
            // Get all citizens in mesa
            List<Map<String, Object>> citizensMap = connectionDB.getCitizensByMesa(mesaId);

            // Extract document IDs
            List<String> documentos = new ArrayList<>();
            for (Map<String, Object> citizen : citizensMap) {
                documentos.add((String) citizen.get("documento"));
            }

            // Generate report for each citizen
            List<String> mesaReports = new ArrayList<>();
            for (String documento : documentos) {
                String citizenReport = generateCitizenReportString(documento, electionId);
                if (!citizenReport.startsWith("ERROR")) {
                    mesaReports.add(citizenReport);
                }
            }

            logger.info("Generated {} citizen report strings for mesa {}", mesaReports.size(), mesaId);
            return mesaReports.toArray(new String[0]);

        } catch (Exception e) {
            logger.error("Error generating mesa citizen report strings for mesa {} and election {}", mesaId, electionId, e);
            return new String[]{createErrorString("Error generating mesa citizen reports: " + e.getMessage())};
        }
    }

    // =================== MÉTODOS HELPER PARA FORMATEAR STRINGS ===================

    private String createErrorString(String message) {
        return "ERROR" + FIELD_SEPARATOR + message + FIELD_SEPARATOR + System.currentTimeMillis();
    }

    private String formatCitizenString(Map<String, Object> assignmentMap) {
        // Formato: id-documento-nombre-apellido
        return String.join(FIELD_SEPARATOR,
                String.valueOf(assignmentMap.getOrDefault("ciudadano_id", 0)),
                String.valueOf(assignmentMap.getOrDefault("documento", "")),
                String.valueOf(assignmentMap.getOrDefault("nombre", "")),
                String.valueOf(assignmentMap.getOrDefault("apellido", ""))
        );
    }

    private String formatLocationString(Map<String, Object> assignmentMap) {
        // Formato: deptId-deptNombre-munId-munNombre-puestoId-puestoNombre-puestoDireccion-puestoConsecutive-mesaId-mesaConsecutive
        return String.join(FIELD_SEPARATOR,
                String.valueOf(assignmentMap.getOrDefault("departamento_id", 0)),
                String.valueOf(assignmentMap.getOrDefault("departamento_nombre", "")),
                String.valueOf(assignmentMap.getOrDefault("municipio_id", 0)),
                String.valueOf(assignmentMap.getOrDefault("municipio_nombre", "")),
                String.valueOf(assignmentMap.getOrDefault("puesto_id", 0)),
                String.valueOf(assignmentMap.getOrDefault("puesto_nombre", "")),
                String.valueOf(assignmentMap.getOrDefault("puesto_direccion", "")),
                String.valueOf(assignmentMap.getOrDefault("puesto_consecutive", 0)),
                String.valueOf(assignmentMap.getOrDefault("mesa_id", 0)),
                String.valueOf(assignmentMap.getOrDefault("mesa_consecutive", 0))
        );
    }

    private String formatElectionString(Map<String, Object> electionInfoMap) {
        // Formato: id-nombre-estado-fechaInicio-fechaFin
        long fechaInicio = 0L;
        long fechaFin = 0L;

        Object fechaInicioObj = electionInfoMap.get("fecha_inicio");
        if (fechaInicioObj instanceof java.sql.Timestamp) {
            fechaInicio = ((java.sql.Timestamp) fechaInicioObj).getTime();
        } else if (fechaInicioObj instanceof Long) {
            fechaInicio = (Long) fechaInicioObj;
        }

        Object fechaFinObj = electionInfoMap.get("fecha_fin");
        if (fechaFinObj instanceof java.sql.Timestamp) {
            fechaFin = ((java.sql.Timestamp) fechaFinObj).getTime();
        } else if (fechaFinObj instanceof Long) {
            fechaFin = (Long) fechaFinObj;
        }

        return String.join(FIELD_SEPARATOR,
                String.valueOf(electionInfoMap.getOrDefault("id", 0)),
                String.valueOf(electionInfoMap.getOrDefault("nombre", "")),
                String.valueOf(electionInfoMap.getOrDefault("estado", "")),
                String.valueOf(fechaInicio),
                String.valueOf(fechaFin)
        );
    }

    private String formatElectionsArray(List<Map<String, Object>> electionsMap) {
        // Formato: election1|election2|election3
        List<String> formattedElections = new ArrayList<>();
        for (Map<String, Object> election : electionsMap) {
            formattedElections.add(formatElectionString(election));
        }
        return String.join(ARRAY_SEPARATOR, formattedElections);
    }

    private String formatElectionResultsString(Map<String, Object> resultsMap) {
        // Formato: electionName-electionStatus-totalVotes-candidateResults
        StringBuilder candidates = new StringBuilder();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> candidateResults = (List<Map<String, Object>>) resultsMap.get("candidate_results");
        if (candidateResults != null) {
            List<String> candidateStrings = new ArrayList<>();
            for (Map<String, Object> candidate : candidateResults) {
                // candidateId:candidateName:voteCount:percentage
                candidateStrings.add(
                        candidate.get("candidate_id") + ":" +
                                candidate.get("candidate_name") + ":" +
                                candidate.get("vote_count") + ":" +
                                candidate.get("percentage")
                );
            }
            candidates.append(String.join("|", candidateStrings));
        }

        return String.join(FIELD_SEPARATOR,
                String.valueOf(resultsMap.get("election_name")),
                String.valueOf(resultsMap.get("election_status")),
                String.valueOf(resultsMap.getOrDefault("total_votes", 0)),
                candidates.toString()
        );
    }

    private String formatGeographicStatsString(Map<String, Object> statsMap, String locationType, int electionId) {
        // Formato: locationType-locationId-locationName-totalMesas-totalPuestos-totalMunicipios-totalCitizens-totalVotes-participationPercentage-electionId
        String locationId = "0";
        String locationName = "";

        if ("department".equals(locationType)) {
            locationId = String.valueOf(statsMap.getOrDefault("departamento_id", 0));
            locationName = "Department Statistics";
        } else if ("municipality".equals(locationType)) {
            locationId = String.valueOf(statsMap.getOrDefault("municipio_id", 0));
            locationName = "Municipality Statistics";
        } else if ("puesto".equals(locationType)) {
            locationId = String.valueOf(statsMap.getOrDefault("puesto_id", 0));
            locationName = "Puesto Statistics";
        }

        return String.join(FIELD_SEPARATOR,
                locationType,
                locationId,
                locationName,
                String.valueOf(statsMap.getOrDefault("total_mesas", 0)),
                String.valueOf(statsMap.getOrDefault("total_puestos", 0)),
                String.valueOf(statsMap.getOrDefault("total_municipios", 0)),
                String.valueOf(((Number) statsMap.getOrDefault("total_ciudadanos", 0L)).longValue()),
                String.valueOf(((Number) statsMap.getOrDefault("total_votos", 0L)).longValue()),
                String.valueOf(statsMap.getOrDefault("participation_percentage", 0.0)),
                String.valueOf(electionId)
        );
    }

    private String formatLocationsArray(List<Map<String, Object>> locationsMap, String locationType) {
        // Formato: location1|location2|location3
        List<String> formattedLocations = new ArrayList<>();
        for (Map<String, Object> location : locationsMap) {
            if ("department".equals(locationType)) {
                formattedLocations.add(
                        location.get("id") + ":" + location.get("nombre") + ":department"
                );
            } else if ("municipality".equals(locationType)) {
                formattedLocations.add(
                        location.get("id") + ":" + location.get("nombre") + ":municipality:" + location.get("departamento_id")
                );
            } else if ("puesto".equals(locationType)) {
                formattedLocations.add(
                        location.get("id") + ":" + location.get("nombre") + ":puesto:" +
                                location.get("direccion") + ":" + location.get("consecutive") + ":" + location.get("municipio_id")
                );
            } else if ("mesa".equals(locationType)) {
                formattedLocations.add(
                        location.get("mesa_id") + ":" + location.get("mesa_consecutive") + ":mesa:" + location.get("puesto_id")
                );
            }
        }
        return String.join(ARRAY_SEPARATOR, formattedLocations);
    }

    private String formatSampleAssignmentsArray(List<Map<String, Object>> citizensMap, int electionId) {
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

    // =================== MÉTODOS HELPER INTERNOS (NO @Override) ===================

    public boolean validateCitizenEligibility(String documento) {
        try {
            return connectionDB.validateCitizenDocument(documento);
        } catch (Exception e) {
            logger.error("Error validating citizen eligibility for document: {}", documento, e);
            return false;
        }
    }

    public boolean isElectionReadyForReports(int electionId) {
        try {
            return connectionDB.validateElectionDataCompleteness(electionId);
        } catch (Exception e) {
            logger.error("Error checking if election {} is ready for reports", electionId, e);
            return false;
        }
    }

    public List<Map<String, Object>> getAvailableElections() {
        try {
            return connectionDB.getAllActiveElections();
        } catch (Exception e) {
            logger.error("Error getting available elections", e);
            return new ArrayList<>();
        }
    }

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
            stats.put("serializationType", "String Formatted");
            stats.put("moduleType", "Reports");

            logger.info("Reports statistics generated for election {}", electionId);

        } catch (Exception e) {
            logger.error("Error getting reports statistics for election {}", electionId, e);
            stats.put("error", "Failed to generate statistics: " + e.getMessage());
        }

        return stats;
    }

}