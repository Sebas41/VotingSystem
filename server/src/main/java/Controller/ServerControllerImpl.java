package Controller;

import ConnectionDB.ConnectionDB;
import ConnectionDB.ConnectionDBinterface;
import Elections.ElectionImpl;
import Elections.ElectionInterface;
import Elections.models.Candidate;
import Elections.models.ELECTION_STATUS;
import Reports.ReportsManagerImpl;
import Reports.VoteNotifierImpl;
import VotingMachineManager.VotingManagerImpl;
import configuration.ConfigurationSender;
import model.ReliableMessage;
import model.Vote;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.votaciones.Server;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;


public class ServerControllerImpl implements ServerControllerInterface {

    private static final Logger logger = LoggerFactory.getLogger(ServerControllerImpl.class);

    private final ConnectionDBinterface connectionDB;
    private final ElectionInterface currentElection;
    private final ReportsManagerImpl reportsManager;
    private final VotingManagerImpl votingManager;
    private final VoteNotifierImpl voteNotifier;
    private ConfigurationSender configurationSender;

    private final Map<String, Object> systemCache = new ConcurrentHashMap<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

    private boolean systemInitialized = false;
    private String systemStatus = "INITIALIZING";
    private Date lastSystemCheck = new Date();


    public ServerControllerImpl() {
        try {
            logger.info("🚀 Inicializando Controlador del Sistema Electoral...");

            // 1. Inicializar base de datos
            this.connectionDB = new ConnectionDB();
            logger.info(" Base de datos conectada");

            // 2. Inicializar elección actual
            this.currentElection = new ElectionImpl(0, new Date(), new Date(), "");
            logger.info(" Sistema de elecciones inicializado");

            // 3. Inicializar managers
            this.reportsManager = new ReportsManagerImpl(connectionDB);
            this.votingManager = new VotingManagerImpl(connectionDB);
            this.voteNotifier = new VoteNotifierImpl();
            logger.info(" Managers de subsistemas inicializados");

            // 4. Inicializar datos de prueba si es necesario
            initializeTestDataIfNeeded();

            // 5. Marcar sistema como listo
            this.systemInitialized = true;
            this.systemStatus = "READY";
            this.lastSystemCheck = new Date();



        } catch (Exception e) {
            logger.error(" Error crítico inicializando el sistema electoral", e);
            this.systemStatus = "ERROR: " + e.getMessage();
            throw new RuntimeException("Failed to initialize Electoral System Controller", e);
        }
    }


    public ElectionResult createElection(String name, Date startDate, Date endDate) {
        try {
            logger.info(" Creando nueva elección: {}", name);

            // Generar ID único para la elección
            int electionId = generateUniqueElectionId();

            // Crear elección en el sistema
            currentElection.registerElection(electionId, name, startDate, endDate);

            // Persistir en base de datos
            connectionDB.storeElection(electionId, name, startDate, endDate, ELECTION_STATUS.PRE.name());

            // Limpiar cache relacionado
            clearElectionCache();

            logger.info(" Elección '{}' creada exitosamente con ID: {}", name, electionId);

            return ElectionResult.success("Elección creada exitosamente",
                    Map.of("electionId", electionId, "name", name, "status", "PRE"));

        } catch (Exception e) {
            logger.error(" Error creando elección: {}", name, e);
            return ElectionResult.error("Error creando elección: " + e.getMessage());
        }
    }


    public ElectionResult getElectionInfo(int electionId) {
        try {
            // Intentar obtener desde cache primero
            String cacheKey = "election_" + electionId;
            if (systemCache.containsKey(cacheKey)) {
                logger.debug("📋 Obteniendo elección {} desde cache", electionId);
                return (ElectionResult) systemCache.get(cacheKey);
            }

            // Obtener desde base de datos
            Map<String, Object> electionData = connectionDB.getElectionInfo(electionId);
            if (electionData == null) {
                return ElectionResult.error("Elección no encontrada: " + electionId);
            }

            // Agregar información adicional
            List<Map<String, Object>> candidates = connectionDB.getCandidatesByElection(electionId);
            Map<String, Object> stats = connectionDB.getElectionConfigurationStats(electionId);

            Map<String, Object> fullInfo = new HashMap<>(electionData);
            fullInfo.put("candidates", candidates);
            fullInfo.put("candidateCount", candidates.size());
            fullInfo.put("statistics", stats);
            fullInfo.put("isReady", connectionDB.validateElectionDataCompleteness(electionId));

            ElectionResult result = ElectionResult.success("Información de elección obtenida", fullInfo);

            // Guardar en cache
            systemCache.put(cacheKey, result);

            logger.info("📊 Información de elección {} obtenida exitosamente", electionId);
            return result;

        } catch (Exception e) {
            logger.error(" Error obteniendo información de elección {}", electionId, e);
            return ElectionResult.error("Error obteniendo información: " + e.getMessage());
        }
    }


    public ElectionResult changeElectionStatus(int electionId, ELECTION_STATUS newStatus) {
        try {
            logger.info(" Cambiando estado de elección {} a {}", electionId, newStatus);

            // Validar que la elección existe
            Map<String, Object> electionInfo = connectionDB.getElectionInfo(electionId);
            if (electionInfo == null) {
                return ElectionResult.error("Elección no encontrada: " + electionId);
            }

            // Cambiar estado local
            currentElection.changeElectionStatus(newStatus);

            // Si hay configuration sender configurado, propagar a todas las mesas
            boolean remoteSuccess = true;
            if (configurationSender != null) {
                remoteSuccess = configurationSender.changeElectionStatusForAllMachines(electionId, newStatus.name());
            }

            // Actualizar cache
            clearElectionCache();

            String message = remoteSuccess ?
                    "Estado cambiado exitosamente en servidor y mesas remotas" :
                    "Estado cambiado en servidor (algunas mesas remotas fallaron)";

            logger.info(" Estado de elección {} cambiado a {}", electionId, newStatus);

            return ElectionResult.success(message,
                    Map.of("electionId", electionId, "newStatus", newStatus.name(), "remoteSuccess", remoteSuccess));

        } catch (Exception e) {
            logger.error(" Error cambiando estado de elección {} a {}", electionId, newStatus, e);
            return ElectionResult.error("Error cambiando estado: " + e.getMessage());
        }
    }

    /**
     * Lista todas las elecciones disponibles
     */
    public ElectionResult getAllElections() {
        try {
            List<Map<String, Object>> elections = connectionDB.getAllActiveElections();

            return ElectionResult.success("Elecciones obtenidas exitosamente",
                    Map.of("elections", elections, "count", elections.size()));

        } catch (Exception e) {
            logger.error(" Error obteniendo lista de elecciones", e);
            return ElectionResult.error("Error obteniendo elecciones: " + e.getMessage());
        }
    }

    // =================== 👥 GESTIÓN DE CANDIDATOS ===================

    /**
     * Agrega un candidato a una elección
     */
    public ElectionResult addCandidate(int electionId, String name, String party) {
        try {
            logger.info("👤 Agregando candidato: {} - {} a elección {}", name, party, electionId);

            // Validar que la elección existe
            Map<String, Object> electionInfo = connectionDB.getElectionInfo(electionId);
            if (electionInfo == null) {
                return ElectionResult.error("Elección no encontrada: " + electionId);
            }

            // Generar ID único para el candidato
            int candidateId = generateUniqueCandidateId();

            // Agregar candidato al sistema
            currentElection.addCandidate(candidateId, name, party);

            // Persistir en base de datos
            connectionDB.storeCandidate(candidateId, name, party, electionId);

            // Limpiar cache
            clearCandidatesCache(electionId);

            logger.info(" Candidato '{}' agregado exitosamente con ID: {}", name, candidateId);

            return ElectionResult.success("Candidato agregado exitosamente",
                    Map.of("candidateId", candidateId, "name", name, "party", party));

        } catch (Exception e) {
            logger.error(" Error agregando candidato: {} - {}", name, party, e);
            return ElectionResult.error("Error agregando candidato: " + e.getMessage());
        }
    }

    /**
     * Obtiene todos los candidatos de una elección
     */
    public ElectionResult getCandidates(int electionId) {
        try {
            List<Map<String, Object>> candidates = connectionDB.getCandidatesByElection(electionId);

            return ElectionResult.success("Candidatos obtenidos exitosamente",
                    Map.of("candidates", candidates, "count", candidates.size(), "electionId", electionId));

        } catch (Exception e) {
            logger.error(" Error obteniendo candidatos para elección {}", electionId, e);
            return ElectionResult.error("Error obteniendo candidatos: " + e.getMessage());
        }
    }

    /**
     * Carga candidatos desde un archivo CSV
     */
    public ElectionResult loadCandidatesFromCSV(int electionId, String csvFilePath) {
        try {
            logger.info("📄 Cargando candidatos desde CSV: {} para elección {}", csvFilePath, electionId);

            // Validar que la elección existe
            Map<String, Object> electionInfo = connectionDB.getElectionInfo(electionId);
            if (electionInfo == null) {
                return ElectionResult.error("Elección no encontrada: " + electionId);
            }

            // Cargar desde CSV usando el método existente
            currentElection.loadCandidatesFromCSV(csvFilePath);

            // Obtener candidatos cargados para persistir en BD
            List<Candidate> loadedCandidates = currentElection.getCandidates();
            int newCandidatesCount = 0;

            for (Candidate candidate : loadedCandidates) {
                try {
                    connectionDB.storeCandidate(candidate.getId(), candidate.getName(),
                            candidate.getPoliticalParty(), electionId);
                    newCandidatesCount++;
                } catch (Exception e) {
                    logger.warn("⚠️ Error persistiendo candidato {}: {}", candidate.getName(), e.getMessage());
                }
            }

            // Limpiar cache
            clearCandidatesCache(electionId);

            logger.info(" {} candidatos cargados desde CSV", newCandidatesCount);

            return ElectionResult.success("Candidatos cargados desde CSV",
                    Map.of("loadedCount", newCandidatesCount, "filePath", csvFilePath));

        } catch (Exception e) {
            logger.error(" Error cargando candidatos desde CSV: {}", csvFilePath, e);
            return ElectionResult.error("Error cargando CSV: " + e.getMessage());
        }
    }


    public ElectionResult sendConfigurationToMesa(int mesaId, int electionId) {
        try {
            logger.info("📤 Enviando configuración a mesa {} para elección {}", mesaId, electionId);

            if (configurationSender == null) {
                return ElectionResult.error("ConfigurationSender no está disponible");
            }

            // Validar configuración antes del envío
            if (!votingManager.validateMesaConfiguration(mesaId, electionId)) {
                return ElectionResult.error("Configuración de mesa no es válida");
            }

            // Enviar configuración
            boolean success = configurationSender.sendConfigurationToMachine(mesaId, electionId);

            if (success) {
                logger.info(" Configuración enviada exitosamente a mesa {}", mesaId);
                return ElectionResult.success("Configuración enviada exitosamente",
                        Map.of("mesaId", mesaId, "electionId", electionId));
            } else {
                return ElectionResult.error("Error enviando configuración a mesa " + mesaId);
            }

        } catch (Exception e) {
            logger.error(" Error enviando configuración a mesa {} para elección {}", mesaId, electionId, e);
            return ElectionResult.error("Error enviando configuración: " + e.getMessage());
        }
    }

    /**
     * Envía configuraciones a todas las mesas de un departamento
     */
    public ElectionResult sendConfigurationToDepartment(int departmentId, int electionId) {
        try {

            if (configurationSender == null) {
                return ElectionResult.error("ConfigurationSender no está disponible");
            }

            // Obtener todas las mesas del departamento
            List<Integer> mesaIds = connectionDB.getMesaIdsByDepartment(departmentId);

            if (mesaIds.isEmpty()) {
                return ElectionResult.error("No se encontraron mesas en el departamento " + departmentId);
            }

            // Enviar configuraciones en lotes usando CompletableFuture para paralelismo
            int successCount = 0;
            int totalMesas = mesaIds.size();

            for (Integer mesaId : mesaIds) {
                try {
                    boolean success = configurationSender.sendConfigurationToMachine(mesaId, electionId);
                    if (success) successCount++;

                    // Pequeña pausa para no saturar la red
                    Thread.sleep(100);
                } catch (Exception e) {
                    logger.warn("⚠️ Error configurando mesa {}: {}", mesaId, e.getMessage());
                }
            }

            double successRate = (double) successCount / totalMesas * 100;
            String message = String.format("Configuración de departamento completada: %d/%d mesas (%.1f%%)",
                    successCount, totalMesas, successRate);

            logger.info("📊 {}", message);

            return ElectionResult.success(message,
                    Map.of("departmentId", departmentId, "totalMesas", totalMesas,
                            "successCount", successCount, "successRate", successRate));

        } catch (Exception e) {
            logger.error(" Error enviando configuraciones a departamento {} para elección {}", departmentId, electionId, e);
            return ElectionResult.error("Error configurando departamento: " + e.getMessage());
        }
    }

    /**
     * Obtiene el estado de configuración de una mesa
     */
    public ElectionResult getMesaConfigurationStatus(int mesaId) {
        try {
            if (configurationSender == null) {
                return ElectionResult.error("ConfigurationSender no está disponible");
            }

            // Obtener información de la mesa
            Map<String, Object> mesaInfo = connectionDB.getMesaConfiguration(mesaId);
            if (mesaInfo == null) {
                return ElectionResult.error("Mesa no encontrada: " + mesaId);
            }

            // Verificar estado de configuración remota (simulado por ahora)
            String configStatus = "UNKNOWN";
            boolean isConnected = false;

            try {
                // Aquí se podría hacer un ping real a la mesa
                configStatus = "CONFIGURED";
                isConnected = true;
            } catch (Exception e) {
                configStatus = "NOT_CONNECTED";
                isConnected = false;
            }

            Map<String, Object> statusInfo = new HashMap<>(mesaInfo);
            statusInfo.put("configurationStatus", configStatus);
            statusInfo.put("isConnected", isConnected);
            statusInfo.put("lastCheck", new Date());

            return ElectionResult.success("Estado de mesa obtenido", statusInfo);

        } catch (Exception e) {
            logger.error(" Error obteniendo estado de mesa {}", mesaId, e);
            return ElectionResult.error("Error obteniendo estado: " + e.getMessage());
        }
    }


    public ElectionResult startVoting(int electionId) {
        return changeElectionStatus(electionId, ELECTION_STATUS.DURING);
    }


    public ElectionResult stopVoting(int electionId) {
        return changeElectionStatus(electionId, ELECTION_STATUS.CLOSED);
    }


    public ElectionResult resetVoting(int electionId) {
        return changeElectionStatus(electionId, ELECTION_STATUS.PRE);
    }


    public ElectionResult registerVote(ReliableMessage voteMessage) {
        try {
            Vote vote = voteMessage.getMessage();
            logger.info("🗳️ Registrando voto desde máquina: {}", vote.getMachineId());

            // Validar que la elección está activa
            if (!currentElection.isElectionActive()) {
                return ElectionResult.error("La elección no está activa para recibir votos");
            }

            // Registrar voto en el sistema
            int candidateId = Integer.parseInt(vote.getVote());
            currentElection.addVoteToCandidate(candidateId, vote);

            // Persistir en base de datos
            connectionDB.storeVote(vote);

            // Notificar observers si está configurado
            if (voteNotifier != null) {
                try {
                    String candidateName = connectionDB.getCandidateNameById(candidateId);
                    String voteInfo = formatVoteNotification(candidateName, vote);
                    voteNotifier.notifyVoteReceived(voteInfo, vote.getElection());
                } catch (Exception e) {
                    logger.warn("⚠️ Error enviando notificación de voto: {}", e.getMessage());
                }
            }

            logger.info(" Voto registrado exitosamente para candidato ID: {}", candidateId);

            return ElectionResult.success("Voto registrado exitosamente",
                    Map.of("candidateId", candidateId, "machineId", vote.getMachineId(),
                            //  CAMBIO: usar getTimeInMillis() para Calendar
                            "timestamp", vote.getDate()));

        } catch (Exception e) {
            logger.error(" Error registrando voto", e);
            return ElectionResult.error("Error registrando voto: " + e.getMessage());
        }
    }


    public ElectionResult getCitizenReport(String documento, int electionId) {
        try {
            logger.info("📋 Generando reporte para ciudadano: {}", documento);

            String reportData = reportsManager.generateCitizenReportString(documento, electionId);

            if (reportData.startsWith("ERROR")) {
                return ElectionResult.error("Error generando reporte: " + reportData);
            }

            return ElectionResult.success("Reporte de ciudadano generado",
                    Map.of("documento", documento, "reportData", reportData));

        } catch (Exception e) {
            logger.error(" Error generando reporte de ciudadano: {}", documento, e);
            return ElectionResult.error("Error generando reporte: " + e.getMessage());
        }
    }


    public ElectionResult searchCitizens(String nombre, String apellido, int limit) {
        try {
            logger.info("🔍 Buscando ciudadanos: {} {} (límite: {})", nombre, apellido, limit);

            List<Map<String, Object>> results = connectionDB.searchCitizensByName(nombre, apellido, limit);

            return ElectionResult.success("Búsqueda completada",
                    Map.of("results", results, "count", results.size(), "limit", limit));

        } catch (Exception e) {
            logger.error(" Error buscando ciudadanos: {} {}", nombre, apellido, e);
            return ElectionResult.error("Error en búsqueda: " + e.getMessage());
        }
    }


    public ElectionResult getElectionResults(int electionId) {
        try {
            logger.info("📊 Generando reporte de resultados para elección: {}", electionId);

            Map<String, Object> results = connectionDB.getElectionResultsSummary(electionId);

            if (results == null || results.containsKey("error")) {
                return ElectionResult.error("Error obteniendo resultados de elección");
            }

            return ElectionResult.success("Resultados de elección obtenidos", results);

        } catch (Exception e) {
            logger.error(" Error obteniendo resultados de elección: {}", electionId, e);
            return ElectionResult.error("Error obteniendo resultados: " + e.getMessage());
        }
    }

    public ElectionResult getDepartmentReport(int departmentId, int electionId) {
        try {
            logger.info("🏛️ Generando reporte de departamento: {} para elección: {}", departmentId, electionId);

            String reportData = reportsManager.generateDepartmentReportString(departmentId, electionId);

            if (reportData.startsWith("ERROR")) {
                return ElectionResult.error("Error generando reporte: " + reportData);
            }

            // También obtener estadísticas de votación
            Map<String, Object> stats = connectionDB.getVotingStatsByDepartment(electionId, departmentId);

            return ElectionResult.success("Reporte de departamento generado",
                    Map.of("departmentId", departmentId, "reportData", reportData, "statistics", stats));

        } catch (Exception e) {
            logger.error(" Error generando reporte de departamento: {}", departmentId, e);
            return ElectionResult.error("Error generando reporte: " + e.getMessage());
        }
    }


    public ElectionResult getSystemStatus() {
        try {
            Map<String, Object> status = new HashMap<>();

            // Estado básico del sistema
            status.put("systemInitialized", systemInitialized);
            status.put("systemStatus", systemStatus);
            status.put("lastCheck", lastSystemCheck);

            // Estado de la base de datos
            boolean dbHealthy = connectionDB.isHealthy();
            status.put("databaseHealthy", dbHealthy);
            status.put("poolStats", connectionDB.getPoolStats());

            // Métricas de rendimiento
            Map<String, Object> metrics = connectionDB.getPerformanceMetrics();
            status.put("performanceMetrics", metrics);

            // Estado de los componentes
            status.put("components", Map.of(
                    "reportsManager", reportsManager != null ? "ACTIVE" : "INACTIVE",
                    "votingManager", votingManager != null ? "ACTIVE" : "INACTIVE",
                    "voteNotifier", voteNotifier != null ? "ACTIVE" : "INACTIVE",
                    "configurationSender", configurationSender != null ? "ACTIVE" : "INACTIVE"
            ));

            // Estado de la elección actual
            status.put("currentElection", Map.of(
                    "isActive", currentElection.isElectionActive(),
                    "isClosed", currentElection.isElectionClosed(),
                    "status", currentElection.getElectionStatus().name(),
                    "info", currentElection.getElectionInfo()
            ));

            // Estadísticas de observers
            if (voteNotifier != null) {
                status.put("observerStatistics", voteNotifier.getObserverStatistics());
            }

            return ElectionResult.success("Estado del sistema obtenido", status);

        } catch (Exception e) {
            logger.error(" Error obteniendo estado del sistema", e);
            return ElectionResult.error("Error obteniendo estado: " + e.getMessage());
        }
    }


    public ElectionResult runSystemDiagnostic() {
        try {
            logger.info("🔍 Ejecutando diagnóstico completo del sistema...");

            Map<String, Object> diagnostic = new HashMap<>();
            List<String> issues = new ArrayList<>();
            List<String> recommendations = new ArrayList<>();

            // 1. Diagnóstico de base de datos
            boolean dbHealthy = connectionDB.isHealthy();
            diagnostic.put("database", Map.of(
                    "healthy", dbHealthy,
                    "poolStats", connectionDB.getPoolStats(),
                    "metrics", connectionDB.getPerformanceMetrics()
            ));

            if (!dbHealthy) {
                issues.add("Base de datos no está saludable");
                recommendations.add("Verificar conexión a la base de datos");
            }

            // 2. Diagnóstico de elección
            boolean electionReady = connectionDB.validateElectionDataCompleteness(1); // Usar elección de prueba
            diagnostic.put("election", Map.of(
                    "dataComplete", electionReady,
                    "status", currentElection.getElectionStatus().name()
            ));

            if (!electionReady) {
                issues.add("Datos de elección incompletos");
                recommendations.add("Verificar que la elección tenga candidatos y mesas asignadas");
            }

            // 3. Diagnóstico de configuraciones (si está disponible)
            if (configurationSender != null) {
                // Aquí se podría hacer un diagnóstico real de conectividad
                diagnostic.put("configurations", Map.of(
                        "senderActive", true,
                        "note", "Diagnóstico de mesas remotas disponible"
                ));
            } else {
                issues.add("ConfigurationSender no está disponible");
                recommendations.add("Configurar el sistema de envío de configuraciones");
            }

            // 4. Resumen del diagnóstico
            diagnostic.put("summary", Map.of(
                    "timestamp", new Date(),
                    "issuesFound", issues.size(),
                    "issues", issues,
                    "recommendations", recommendations,
                    "overallHealth", issues.isEmpty() ? "HEALTHY" : "NEEDS_ATTENTION"
            ));

            logger.info(" Diagnóstico completado - {} issues encontrados", issues.size());

            return ElectionResult.success("Diagnóstico completado", diagnostic);

        } catch (Exception e) {
            logger.error(" Error ejecutando diagnóstico del sistema", e);
            return ElectionResult.error("Error en diagnóstico: " + e.getMessage());
        }
    }


    public ElectionResult getPerformanceStatistics() {
        try {
            Map<String, Object> stats = new HashMap<>();

            // Estadísticas de base de datos
            Map<String, Object> dbMetrics = connectionDB.getPerformanceMetrics();
            stats.put("database", dbMetrics);

            // Estadísticas del sistema
            Runtime runtime = Runtime.getRuntime();
            stats.put("system", Map.of(
                    "totalMemory", runtime.totalMemory(),
                    "freeMemory", runtime.freeMemory(),
                    "usedMemory", runtime.totalMemory() - runtime.freeMemory(),
                    "maxMemory", runtime.maxMemory(),
                    "availableProcessors", runtime.availableProcessors()
            ));

            // Estadísticas de cache
            stats.put("cache", Map.of(
                    "size", systemCache.size(),
                    "keys", new ArrayList<>(systemCache.keySet())
            ));

            // Timestamp
            stats.put("timestamp", new Date());

            return ElectionResult.success("Estadísticas de rendimiento obtenidas", stats);

        } catch (Exception e) {
            logger.error(" Error obteniendo estadísticas de rendimiento", e);
            return ElectionResult.error("Error obteniendo estadísticas: " + e.getMessage());
        }
    }


    public void setConfigurationSender(ConfigurationSender configurationSender) {
        this.configurationSender = configurationSender;
        logger.info("🔗 ConfigurationSender configurado en el controller");
    }


    public void setVoteNotifier(VoteNotifierImpl voteNotifier) {
        // No es necesario ya que se inicializa en el constructor, pero se deja para compatibilidad
        logger.info(" VoteNotifier configurado en el controller");
    }


    private void initializeTestDataIfNeeded() {
        try {
            // Verificar si ya existen datos de prueba
            Map<String, Object> electionInfo = connectionDB.getElectionInfo(1);

            if (electionInfo == null) {
                logger.info("🧪 Inicializando datos de prueba...");

                // Crear elección de prueba para HOY
                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                Date startDate = cal.getTime();

                cal.set(Calendar.HOUR_OF_DAY, 23);
                cal.set(Calendar.MINUTE, 59);
                Date endDate = cal.getTime();

                createElection("Elección de Prueba 2025", startDate, endDate);

                // Agregar candidatos de prueba
                addCandidate(1, "Juan Pérez", "Partido A");
                addCandidate(1, "María García", "Partido B");
                addCandidate(1, "Carlos López", "Partido C");

                // Cambiar estado a activo
                changeElectionStatus(1, ELECTION_STATUS.DURING);

                logger.info(" Datos de prueba inicializados exitosamente");
            }
        } catch (Exception e) {
            logger.warn(" Error inicializando datos de prueba: {}", e.getMessage());
        }
    }

    private int generateUniqueElectionId() {
        // Generar ID basado en timestamp para asegurar unicidad
        return (int) (System.currentTimeMillis() % 100000);
    }

    private int generateUniqueCandidateId() {
        // Generar ID basado en timestamp para asegurar unicidad
        return (int) (System.currentTimeMillis() % 100000);
    }

    private void clearElectionCache() {
        systemCache.entrySet().removeIf(entry -> entry.getKey().startsWith("election_"));
    }

    private void clearCandidatesCache(int electionId) {
        systemCache.remove("candidates_" + electionId);
    }


    private String formatVoteNotification(String candidateName, Vote vote) {

        return candidateName + "-" + vote.getDate() + "-" + vote.getElection();
    }


    public static class ElectionResult {
        private final boolean success;
        private final String message;
        private final Map<String, Object> data;
        private final Date timestamp;

        private ElectionResult(boolean success, String message, Map<String, Object> data) {
            this.success = success;
            this.message = message;
            this.data = data != null ? data : new HashMap<>();
            this.timestamp = new Date();
        }

        public static ElectionResult success(String message, Map<String, Object> data) {
            return new ElectionResult(true, message, data);
        }

        public static ElectionResult success(String message) {
            return new ElectionResult(true, message, null);
        }

        public static ElectionResult error(String message) {
            return new ElectionResult(false, message, null);
        }

        // Getters
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public Map<String, Object> getData() { return data; }
        public Date getTimestamp() { return timestamp; }

        @Override
        public String toString() {
            return String.format("ElectionResult{success=%s, message='%s', dataSize=%d, timestamp=%s}",
                    success, message, data.size(), timestamp);
        }
    }


    public void shutdown() {
        try {
            logger.info(" Cerrando Controlador del Sistema Electoral");

            // Limpiar cache
            systemCache.clear();

            // Cerrar conexión de base de datos si es necesario
            ConnectionDB.shutdown();

            this.systemStatus = "SHUTDOWN";
            logger.info(" Controlador cerrado exitosamente");

        } catch (Exception e) {
            logger.error(" Error durante shutdown del controller", e);
        }
    }
}