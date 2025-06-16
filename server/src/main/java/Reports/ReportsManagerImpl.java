package Reports;

import ConnectionDB.ConnectionDBinterface;
import com.zeroc.Ice.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.Exception;
import java.lang.Object;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ReportsManagerImpl implements ReportsSystem.ReportsService {

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


            StringBuilder report = new StringBuilder();


            report.append(formatCitizenString(assignmentMap)).append(RECORD_SEPARATOR);


            report.append(formatLocationString(assignmentMap)).append(RECORD_SEPARATOR);


            report.append(formatElectionString(electionInfoMap)).append(RECORD_SEPARATOR);


            report.append(formatElectionsArray(availableElectionsMap)).append(RECORD_SEPARATOR);


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

            StringBuilder report = new StringBuilder();

            report.append(formatElectionResultsString(resultsMap)).append(RECORD_SEPARATOR);

            report.append(formatGeographicStatsString(nationalStatsMap, "national", electionId)).append(RECORD_SEPARATOR);

            report.append(formatLocationsArray(departmentsMap, "department")).append(RECORD_SEPARATOR);

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
            Map<String, Object> statsMap = connectionDB.getVotingStatsByDepartment(electionId, departmentId);

            List<Map<String, Object>> municipalitiesMap = connectionDB.getMunicipalitiesByDepartment(departmentId);

            List<Integer> mesaIds = connectionDB.getMesaIdsByDepartment(departmentId);
            List<Map<String, Object>> sampleCitizens = new ArrayList<>();
            if (!mesaIds.isEmpty()) {
                sampleCitizens = connectionDB.getCitizensByMesa(mesaIds.get(0));
                if (sampleCitizens.size() > 10) {
                    sampleCitizens = sampleCitizens.subList(0, 10);
                }
            }

            StringBuilder report = new StringBuilder();

            report.append(formatGeographicStatsString(statsMap, "department", electionId)).append(RECORD_SEPARATOR);


            report.append(formatLocationsArray(municipalitiesMap, "municipality")).append(RECORD_SEPARATOR);


            report.append(formatSampleAssignmentsArray(sampleCitizens, electionId)).append(RECORD_SEPARATOR);


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

            Map<String, Object> statsMap = connectionDB.getVotingStatsByMunicipality(electionId, municipalityId);


            List<Map<String, Object>> puestosMap = connectionDB.getPuestosByMunicipality(municipalityId);


            StringBuilder report = new StringBuilder();


            report.append(formatGeographicStatsString(statsMap, "municipality", electionId)).append(RECORD_SEPARATOR);


            report.append(formatLocationsArray(puestosMap, "puesto")).append(RECORD_SEPARATOR);


            report.append("").append(RECORD_SEPARATOR);

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

            Map<String, Object> statsMap = connectionDB.getVotingStatsByPuesto(electionId, puestoId);


            List<Map<String, Object>> mesasMap = connectionDB.getMesasByPuesto(puestoId);


            StringBuilder report = new StringBuilder();


            report.append(formatGeographicStatsString(statsMap, "puesto", electionId)).append(RECORD_SEPARATOR);


            report.append(formatLocationsArray(mesasMap, "mesa")).append(RECORD_SEPARATOR);


            report.append("").append(RECORD_SEPARATOR);


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

            List<Map<String, Object>> citizensMap = connectionDB.getCitizensByMesa(mesaId);


            List<String> documentos = new ArrayList<>();
            for (Map<String, Object> citizen : citizensMap) {
                documentos.add((String) citizen.get("documento"));
            }


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


    @Override
    public String[] getDepartmentCitizenDocuments(int departmentId, int electionId, Current current) {
        logger.debug("Ice request: getDepartmentCitizenDocuments for department {} election {}", departmentId, electionId);

        try {
            // Validar que la elección existe
            Map<String, Object> electionInfo = connectionDB.getElectionInfo(electionId);
            if (electionInfo == null) {
                logger.warn("Election {} not found", electionId);
                return new String[]{createErrorString("Election not found")};
            }

            // Obtener todos los ciudadanos del departamento
            List<Map<String, Object>> citizens = connectionDB.getCitizensByDepartment(departmentId);

            // Extraer solo los documentos
            String[] documents = new String[citizens.size()];
            for (int i = 0; i < citizens.size(); i++) {
                documents[i] = (String) citizens.get(i).get("documento");
            }

            logger.info("Retrieved {} citizen documents for department {}", documents.length, departmentId);
            return documents;

        } catch (Exception e) {
            logger.error("Error getting citizen documents for department {} election {}", departmentId, electionId, e);
            return new String[]{createErrorString("Error getting citizen documents: " + e.getMessage())};
        }
    }

    @Override
    public String[] getMunicipalityCitizenDocuments(int municipalityId, int electionId, Current current) {
        logger.debug("Ice request: getMunicipalityCitizenDocuments for municipality {} election {}", municipalityId, electionId);

        try {
            // Validar que la elección existe
            Map<String, Object> electionInfo = connectionDB.getElectionInfo(electionId);
            if (electionInfo == null) {
                logger.warn("Election {} not found", electionId);
                return new String[]{createErrorString("Election not found")};
            }

            // Obtener todos los ciudadanos del municipio
            List<Map<String, Object>> citizens = connectionDB.getCitizensByMunicipality(municipalityId);

            // Extraer solo los documentos
            String[] documents = new String[citizens.size()];
            for (int i = 0; i < citizens.size(); i++) {
                documents[i] = (String) citizens.get(i).get("documento");
            }

            logger.info("Retrieved {} citizen documents for municipality {}", documents.length, municipalityId);
            return documents;

        } catch (Exception e) {
            logger.error("Error getting citizen documents for municipality {} election {}", municipalityId, electionId, e);
            return new String[]{createErrorString("Error getting citizen documents: " + e.getMessage())};
        }
    }

    @Override
    public String[] getPuestoCitizenDocuments(int puestoId, int electionId, Current current) {
        logger.debug("Ice request: getPuestoCitizenDocuments for puesto {} election {}", puestoId, electionId);

        try {
            // Validar que la elección existe
            Map<String, Object> electionInfo = connectionDB.getElectionInfo(electionId);
            if (electionInfo == null) {
                logger.warn("Election {} not found", electionId);
                return new String[]{createErrorString("Election not found")};
            }

            // Obtener todos los ciudadanos del puesto
            List<Map<String, Object>> citizens = connectionDB.getCitizensByPuesto(puestoId);

            // Extraer solo los documentos
            String[] documents = new String[citizens.size()];
            for (int i = 0; i < citizens.size(); i++) {
                documents[i] = (String) citizens.get(i).get("documento");
            }

            logger.info("Retrieved {} citizen documents for puesto {}", documents.length, puestoId);
            return documents;

        } catch (Exception e) {
            logger.error("Error getting citizen documents for puesto {} election {}", puestoId, electionId, e);
            return new String[]{createErrorString("Error getting citizen documents: " + e.getMessage())};
        }
    }

    @Override
    public String[] getMesaCitizenDocuments(int mesaId, int electionId, Current current) {
        logger.debug("Ice request: getMesaCitizenDocuments for mesa {} election {}", mesaId, electionId);

        try {
            // Validar que la elección existe
            Map<String, Object> electionInfo = connectionDB.getElectionInfo(electionId);
            if (electionInfo == null) {
                logger.warn("Election {} not found", electionId);
                return new String[]{createErrorString("Election not found")};
            }

            // Obtener todos los ciudadanos de la mesa
            List<Map<String, Object>> citizens = connectionDB.getCitizensByMesa(mesaId);

            // Extraer solo los documentos
            String[] documents = new String[citizens.size()];
            for (int i = 0; i < citizens.size(); i++) {
                documents[i] = (String) citizens.get(i).get("documento");
            }

            logger.info("Retrieved {} citizen documents for mesa {}", documents.length, mesaId);
            return documents;

        } catch (Exception e) {
            logger.error("Error getting citizen documents for mesa {} election {}", mesaId, electionId, e);
            return new String[]{createErrorString("Error getting citizen documents: " + e.getMessage())};
        }
    }

    @Override
    public String preloadReports(int electionId, String locationType, int locationId, Current current) {
        logger.info("🚀 Ice request: preloadReports type '{}' for election {} location {}", locationType, electionId, locationId);

        long startTime = System.currentTimeMillis();
        StringBuilder result = new StringBuilder();
        result.append("🚀 ========== PRECARGA DE REPORTES ==========\n");
        result.append(String.format(" Elección: %d | Tipo: %s | Ubicación: %d\n\n", electionId, locationType, locationId));

        try {
            switch (locationType.toLowerCase()) {
                case "basic":
                    return preloadBasicReports(electionId, result, startTime);

                case "department":
                case "departamento":
                    return preloadDepartmentReports(electionId, locationId, result, startTime);

                case "municipality":
                case "municipio":
                    return preloadMunicipalityReports(electionId, locationId, result, startTime);

                case "puesto":
                    return preloadPuestoReports(electionId, locationId, result, startTime);

                case "mesa":
                    return preloadMesaReports(electionId, locationId, result, startTime);

                case "all":
                    return preloadAllReports(electionId, result, startTime);

                default:
                    throw new IllegalArgumentException("Tipo de precarga no válido: " + locationType +
                            ". Tipos válidos: basic, department, municipality, puesto, mesa, all");
            }

        } catch (Exception e) {
            logger.error(" Error en precarga tipo '{}': {}", locationType, e.getMessage());
            result.append(" ERROR: ").append(e.getMessage()).append("\n");
            return result.toString();
        }
    }


    @Override
    public String getCacheStats(Current current) {
        logger.debug("Ice request: getCacheStats");

        try {
            StringBuilder stats = new StringBuilder();
            stats.append(" ========== ESTADÍSTICAS DEL SERVIDOR REPORTS ==========\n");

            // 1. Información básica del servidor
            stats.append(String.format("🔧 Versión del paquete: %s\n", PACKAGE_VERSION));
            stats.append(String.format("⏰ Timestamp: %d\n", System.currentTimeMillis()));
            stats.append(String.format("🔄 Estado del servidor: ACTIVO\n"));

            // 2. Estadísticas de la base de datos
            Map<String, Object> dbMetrics = connectionDB.getPerformanceMetrics();
            if (dbMetrics != null && !dbMetrics.containsKey("error")) {
                stats.append("\n Estadísticas de Base de Datos:\n");
                stats.append(String.format("    Total ciudadanos: %s\n",
                        formatNumber(dbMetrics.get("total_citizens"))));
                stats.append(String.format("    Total mesas: %s\n",
                        formatNumber(dbMetrics.get("total_mesas"))));
                stats.append(String.format("    Total puestos: %s\n",
                        formatNumber(dbMetrics.get("total_puestos"))));

                // Pool de conexiones
                if (dbMetrics.containsKey("pool_active_connections")) {
                    stats.append("\n🔌 Pool de Conexiones:\n");
                    stats.append(String.format("   🟢 Conexiones activas: %s\n",
                            dbMetrics.get("pool_active_connections")));
                    stats.append(String.format("   ⭐ Conexiones idle: %s\n",
                            dbMetrics.get("pool_idle_connections")));
                    stats.append(String.format("    Total conexiones: %s\n",
                            dbMetrics.get("pool_total_connections")));
                }
            }

            // 3. Estadísticas de elecciones
            List<Map<String, Object>> elections = connectionDB.getAllActiveElections();
            stats.append(String.format("\n Elecciones activas: %d\n", elections.size()));

            // 4. Estadísticas de departamentos
            List<Map<String, Object>> departments = connectionDB.getAllDepartments();
            stats.append(String.format(" Departamentos: %d\n", departments.size()));

            // 5. Estado de salud de la BD
            boolean isHealthy = connectionDB.isHealthy();
            stats.append(String.format("\n💚 Estado de BD: %s\n", isHealthy ? "SALUDABLE" : "CON PROBLEMAS"));

            // 6. Información de rendimiento
            stats.append("\n⚡ Rendimiento del Servidor:\n");
            stats.append("    Sin cache local (servidor directo a BD)\n");
            stats.append("    Optimizado con HikariCP\n");
            stats.append("    Respuestas en formato string\n");

            // 7. Información adicional
            stats.append("\n Métodos Disponibles:\n");
            stats.append("   • getCitizenReports\n");
            stats.append("   • searchCitizenReports\n");
            stats.append("   • getElectionReports\n");
            stats.append("   • getGeographicReports\n");
            stats.append("   • getMesaCitizenReports\n");
            stats.append("   • preloadReports (básico + geográfico)\n");
            stats.append("   • getDepartmentCitizenDocuments\n");
            stats.append("   • getMunicipalityCitizenDocuments\n");
            stats.append("   • getPuestoCitizenDocuments\n");

            stats.append("\n========================================\n");

            String result = stats.toString();
            logger.info("Server stats generated successfully");
            return result;

        } catch (Exception e) {
            logger.error("Error generating server stats: {}", e.getMessage());
            return createErrorString("Error generating server stats: " + e.getMessage());
        }
    }


    private String preloadBasicReports(int electionId, StringBuilder result, long startTime) {
        try {
            result.append(" PRECARGA BÁSICA\n");
            int itemsPreloaded = 0;

            result.append("⏳ Precargando reporte de elección...\n");
            String electionReport = generateElectionResultsReportString(electionId);
            if (!electionReport.startsWith("ERROR")) {
                itemsPreloaded++;
                result.append("    Reporte de elección generado\n");
            } else {
                result.append("    Error en reporte de elección\n");
            }

            result.append(" Precargando lista de elecciones...\n");
            List<Map<String, Object>> elections = connectionDB.getAllActiveElections();
            itemsPreloaded++;
            result.append("    Lista de elecciones obtenida\n");

            result.append(" Precargando reportes de departamentos principales...\n");
            int[] mainDepartments = {1, 2, 3, 5}; // IDs de departamentos principales
            int deptSuccessCount = 0;

            for (int deptId : mainDepartments) {
                try {
                    String geoReport = generateDepartmentReportString(deptId, electionId);
                    if (!geoReport.startsWith("ERROR")) {
                        deptSuccessCount++;
                    }
                } catch (Exception e) {
                    result.append("    Error con departamento ").append(deptId).append("\n");
                }
            }

            itemsPreloaded += deptSuccessCount;
            result.append(String.format("    %d departamentos precargados\n", deptSuccessCount));

            long duration = System.currentTimeMillis() - startTime;
            result.append(String.format("\n PRECARGA BÁSICA COMPLETADA\n"));
            result.append(String.format(" Items precargados: %d\n", itemsPreloaded));
            result.append(String.format("️ Tiempo: %d ms\n", duration));

            return result.toString();

        } catch (Exception e) {
            logger.error(" Error en precarga básica: {}", e.getMessage());
            result.append(" ERROR: ").append(e.getMessage()).append("\n");
            return result.toString();
        }
    }

    /**
     * Precarga completa de un departamento
     */
    private String preloadDepartmentReports(int electionId, int departmentId, StringBuilder result, long startTime) {
        try {
            result.append(String.format("🏛️ PRECARGA DEPARTAMENTO %d\n", departmentId));

            // 1. Reporte geográfico del departamento
            result.append("⏳ Precargando reporte geográfico del departamento...\n");
            String deptReport = generateDepartmentReportString(departmentId, electionId);
            if (deptReport.startsWith("ERROR")) {
                result.append("    Error generando reporte geográfico\n");
                return result.toString();
            }
            result.append("    Reporte geográfico generado\n");

            // 2. Obtener todos los ciudadanos del departamento
            result.append("⏳ Obteniendo lista de ciudadanos del departamento...\n");
            List<Map<String, Object>> citizens = connectionDB.getCitizensByDepartment(departmentId);
            result.append(String.format("    Encontrados %d ciudadanos\n", citizens.size()));

            // 3. Precargar reportes de ciudadanos en lotes (solo una muestra para evitar sobrecarga)
            result.append("⏳ Precargando muestra de reportes de ciudadanos...\n");
            int maxSample = Math.min(citizens.size(), 100); // Limitar a 100 ciudadanos como muestra
            int preloadedCitizens = 0;

            for (int i = 0; i < maxSample; i++) {
                try {
                    String documento = (String) citizens.get(i).get("documento");
                    String citizenReport = generateCitizenReportString(documento, electionId);
                    if (!citizenReport.startsWith("ERROR")) {
                        preloadedCitizens++;
                    }
                } catch (Exception e) {
                    // Continuar con el siguiente ciudadano
                }

                // Log de progreso cada 25 ciudadanos
                if ((i + 1) % 25 == 0) {
                    result.append(String.format("   📈 Progreso: %d/%d ciudadanos\n", i + 1, maxSample));
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            result.append(String.format("\n PRECARGA DEPARTAMENTO %d COMPLETADA\n", departmentId));
            result.append(String.format(" Ciudadanos precargados: %d/%d (muestra)\n", preloadedCitizens, maxSample));
            result.append(String.format(" Total ciudadanos en departamento: %d\n", citizens.size()));
            result.append(String.format("️ Tiempo total: %d ms\n", duration));

            if (maxSample < citizens.size()) {
                result.append(String.format("💡 Nota: Se precargó una muestra de %d ciudadanos de %d totales\n", maxSample, citizens.size()));
            }

            return result.toString();

        } catch (Exception e) {
            logger.error(" Error en precarga de departamento {}: {}", departmentId, e.getMessage());
            result.append(" ERROR: ").append(e.getMessage()).append("\n");
            return result.toString();
        }
    }

    /**
     * Precarga completa de un municipio
     */
    private String preloadMunicipalityReports(int electionId, int municipalityId, StringBuilder result, long startTime) {
        try {
            result.append(String.format("🏙️ PRECARGA MUNICIPIO %d\n", municipalityId));

            // Reporte geográfico del municipio
            String munReport = generateMunicipalityReportString(municipalityId, electionId);
            if (munReport.startsWith("ERROR")) {
                result.append("    Error generando reporte geográfico\n");
                return result.toString();
            }

            // Obtener ciudadanos del municipio
            List<Map<String, Object>> citizens = connectionDB.getCitizensByMunicipality(municipalityId);
            int maxSample = Math.min(citizens.size(), 50); // Muestra de 50
            int preloaded = 0;

            for (int i = 0; i < maxSample; i++) {
                try {
                    String documento = (String) citizens.get(i).get("documento");
                    String citizenReport = generateCitizenReportString(documento, electionId);
                    if (!citizenReport.startsWith("ERROR")) {
                        preloaded++;
                    }
                } catch (Exception e) {
                    // Continuar
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            result.append(String.format("\n PRECARGA MUNICIPIO %d COMPLETADA\n", municipalityId));
            result.append(String.format(" Ciudadanos precargados: %d/%d (muestra)\n", preloaded, maxSample));
            result.append(String.format(" Total ciudadanos en municipio: %d\n", citizens.size()));
            result.append(String.format("️ Tiempo: %d ms\n", duration));

            return result.toString();

        } catch (Exception e) {
            logger.error(" Error en precarga de municipio {}: {}", municipalityId, e.getMessage());
            result.append(" ERROR: ").append(e.getMessage()).append("\n");
            return result.toString();
        }
    }


    private String preloadPuestoReports(int electionId, int puestoId, StringBuilder result, long startTime) {
        try {
            result.append(String.format("🗳️ PRECARGA PUESTO %d\n", puestoId));

            // Obtener ciudadanos del puesto
            List<Map<String, Object>> citizens = connectionDB.getCitizensByPuesto(puestoId);
            int preloaded = 0;

            for (Map<String, Object> citizen : citizens) {
                try {
                    String documento = (String) citizen.get("documento");
                    String citizenReport = generateCitizenReportString(documento, electionId);
                    if (!citizenReport.startsWith("ERROR")) {
                        preloaded++;
                    }
                } catch (Exception e) {
                    // Continuar
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            result.append(String.format("\n PRECARGA PUESTO %d COMPLETADA\n", puestoId));
            result.append(String.format(" Ciudadanos precargados: %d\n", preloaded));
            result.append(String.format("️ Tiempo: %d ms\n", duration));

            return result.toString();

        } catch (Exception e) {
            logger.error(" Error en precarga de puesto {}: {}", puestoId, e.getMessage());
            result.append(" ERROR: ").append(e.getMessage()).append("\n");
            return result.toString();
        }
    }


    private String preloadMesaReports(int electionId, int mesaId, StringBuilder result, long startTime) {
        try {
            result.append(String.format(" PRECARGA MESA %d\n", mesaId));

            // Obtener ciudadanos de la mesa
            List<Map<String, Object>> citizens = connectionDB.getCitizensByMesa(mesaId);
            int preloaded = 0;

            for (Map<String, Object> citizen : citizens) {
                try {
                    String documento = (String) citizen.get("documento");
                    String citizenReport = generateCitizenReportString(documento, electionId);
                    if (!citizenReport.startsWith("ERROR")) {
                        preloaded++;
                    }
                } catch (Exception e) {
                    // Continuar
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            result.append(String.format("\n PRECARGA MESA %d COMPLETADA\n", mesaId));
            result.append(String.format(" Ciudadanos precargados: %d\n", preloaded));
            result.append(String.format(" Tiempo: %d ms\n", duration));

            return result.toString();

        } catch (Exception e) {
            logger.error(" Error en precarga de mesa {}: {}", mesaId, e.getMessage());
            result.append(" ERROR: ").append(e.getMessage()).append("\n");
            return result.toString();
        }
    }


    private String preloadAllReports(int electionId, StringBuilder result, long startTime) {
        try {
            result.append(" PRECARGA COMPLETA DEL SISTEMA\n");
            result.append(" ADVERTENCIA: Esta operación puede tomar mucho tiempo\n\n");

            // 1. Precarga básica
            result.append(" Fase 1: Precarga básica...\n");
            preloadBasicReports(electionId, new StringBuilder(), System.currentTimeMillis());
            result.append("    Precarga básica completada\n");

            // 2. Precarga de departamentos principales
            result.append("\n Fase 2: Precarga de departamentos principales...\n");
            List<Map<String, Object>> departments = connectionDB.getAllDepartments();
            int deptCount = 0;

            for (Map<String, Object> dept : departments) {
                try {
                    int deptId = (Integer) dept.get("id");
                    if (deptCount < 3) { // Limitar a los primeros 3 departamentos
                        preloadDepartmentReports(electionId, deptId, new StringBuilder(), System.currentTimeMillis());
                        result.append(String.format("    Departamento %d precargado\n", deptId));
                        deptCount++;
                    }
                } catch (Exception e) {
                    result.append(String.format("    Error en departamento\n"));
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            result.append(String.format("\n PRECARGA COMPLETA FINALIZADA\n"));
            result.append(String.format(" Departamentos procesados: %d\n", deptCount));
            result.append(String.format(" Tiempo total: %d ms (%.1f minutos)\n",
                    duration, duration / 60000.0));

            return result.toString();

        } catch (Exception e) {
            logger.error(" Error en precarga completa: {}", e.getMessage());
            result.append(" ERROR: ").append(e.getMessage()).append("\n");
            return result.toString();
        }
    }


    private String formatNumber(Object number) {
        if (number == null) return "0";

        try {
            long num = ((Number) number).longValue();
            if (num >= 1_000_000) {
                return String.format("%.1fM", num / 1_000_000.0);
            } else if (num >= 1_000) {
                return String.format("%.1fK", num / 1_000.0);
            } else {
                return String.valueOf(num);
            }
        } catch (Exception e) {
            return String.valueOf(number);
        }
    }





}