package org.votaciones.cache;

import Reports.*;
import com.zeroc.Ice.Current;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Implementación completa del cache de reportes para ProxyCache
 * Maneja cache local, sincronización con servidor principal y serving de archivos
 */
public class ReportsCacheImpl implements ReportsManagerInterface {

    private static final Logger logger = LoggerFactory.getLogger(ReportsCacheImpl.class);

    // =================== CONFIGURACIÓN ===================

    private final String cacheDirectory;
    private final String serverEndpoint;
    private final int maxCacheSize;
    private final long cacheExpirationMs;

    // =================== CACHE INTERNO ===================

    private final Map<String, CitizenReportsConfiguration> citizenCache = new ConcurrentHashMap<>();
    private final Map<Integer, ElectionReportsConfiguration> electionCache = new ConcurrentHashMap<>();
    private final Map<String, GeographicReportsConfiguration> geographicCache = new ConcurrentHashMap<>();

    // Cache metadata
    private final Map<String, Long> cacheTimestamps = new ConcurrentHashMap<>();
    private final Map<String, Integer> cacheAccessCount = new ConcurrentHashMap<>();

    // Thread safety
    private final ReentrantReadWriteLock cacheLock = new ReentrantReadWriteLock();

    // =================== CONSTRUCTOR ===================

    public ReportsCacheImpl(String cacheDirectory, String serverEndpoint) {
        this.cacheDirectory = cacheDirectory;
        this.serverEndpoint = serverEndpoint;
        this.maxCacheSize = 1000; // Máximo 1000 entradas por tipo
        this.cacheExpirationMs = 30 * 60 * 1000; // 30 minutos

        // Crear directorio de cache si no existe
        createCacheDirectory();

        logger.info("ReportsCacheImpl initialized - Cache: {}, Server: {}",
                cacheDirectory, serverEndpoint);
    }

    private void createCacheDirectory() {
        try {
            Path cachePath = Paths.get(cacheDirectory);
            if (!Files.exists(cachePath)) {
                Files.createDirectories(cachePath);
                logger.info("Created cache directory: {}", cacheDirectory);
            }

            // Crear subdirectorios
            Files.createDirectories(cachePath.resolve("citizen"));
            Files.createDirectories(cachePath.resolve("election"));
            Files.createDirectories(cachePath.resolve("geographic"));

        } catch (IOException e) {
            logger.error("Failed to create cache directory: {}", cacheDirectory, e);
            throw new RuntimeException("Cannot initialize cache directory", e);
        }
    }

    // =================== REPORTS CACHE INTERFACE ===================

    @Override
    public CitizenReportsConfiguration getCitizenReports(String documento, int electionId, Current current) {
        String cacheKey = generateCitizenCacheKey(documento, electionId);

        cacheLock.readLock().lock();
        try {
            // Verificar cache primero
            CitizenReportsConfiguration cached = citizenCache.get(cacheKey);
            if (cached != null && !isCacheExpired(cacheKey)) {
                incrementAccessCount(cacheKey);
                logger.debug("Cache HIT for citizen: {} election: {}", documento, electionId);
                return cached;
            }
        } finally {
            cacheLock.readLock().unlock();
        }

        // Cache miss - obtener del servidor principal
        logger.debug("Cache MISS for citizen: {} election: {}", documento, electionId);
        CitizenReportsConfiguration result = fetchCitizenReportsFromServer(documento, electionId);

        // Almacenar en cache
        cacheLock.writeLock().lock();
        try {
            citizenCache.put(cacheKey, result);
            cacheTimestamps.put(cacheKey, System.currentTimeMillis());
            cacheAccessCount.put(cacheKey, 1);

            // Limpiar cache si está muy lleno
            if (citizenCache.size() > maxCacheSize) {
                evictOldestEntries(citizenCache, cacheKey);
            }
        } finally {
            cacheLock.writeLock().unlock();
        }

        // Guardar en disco de forma asíncrona
        saveCitizenReportToDisk(documento, electionId, result);

        return result;
    }

    @Override
    public ElectionReportsConfiguration getElectionReports(int electionId, Current current) {
        cacheLock.readLock().lock();
        try {
            // Verificar cache primero
            ElectionReportsConfiguration cached = electionCache.get(electionId);
            if (cached != null && !isCacheExpired("election_" + electionId)) {
                incrementAccessCount("election_" + electionId);
                logger.debug("Cache HIT for election: {}", electionId);
                return cached;
            }
        } finally {
            cacheLock.readLock().unlock();
        }

        // Cache miss
        logger.debug("Cache MISS for election: {}", electionId);
        ElectionReportsConfiguration result = fetchElectionReportsFromServer(electionId);

        // Almacenar en cache
        cacheLock.writeLock().lock();
        try {
            electionCache.put(electionId, result);
            String cacheKey = "election_" + electionId;
            cacheTimestamps.put(cacheKey, System.currentTimeMillis());
            cacheAccessCount.put(cacheKey, 1);

            if (electionCache.size() > maxCacheSize) {
                evictOldestElectionEntries(electionId);
            }
        } finally {
            cacheLock.writeLock().unlock();
        }

        // Guardar en disco
        saveElectionReportToDisk(electionId, result);

        return result;
    }

    @Override
    public GeographicReportsConfiguration getGeographicReports(int locationId, String locationType,
                                                               int electionId, Current current) {
        String cacheKey = generateGeographicCacheKey(locationId, locationType, electionId);

        cacheLock.readLock().lock();
        try {
            GeographicReportsConfiguration cached = geographicCache.get(cacheKey);
            if (cached != null && !isCacheExpired(cacheKey)) {
                incrementAccessCount(cacheKey);
                logger.debug("Cache HIT for geographic: {} {} election: {}",
                        locationId, locationType, electionId);
                return cached;
            }
        } finally {
            cacheLock.readLock().unlock();
        }

        // Cache miss
        logger.debug("Cache MISS for geographic: {} {} election: {}",
                locationId, locationType, electionId);
        GeographicReportsConfiguration result = fetchGeographicReportsFromServer(locationId, locationType, electionId);

        // Almacenar en cache
        cacheLock.writeLock().lock();
        try {
            geographicCache.put(cacheKey, result);
            cacheTimestamps.put(cacheKey, System.currentTimeMillis());
            cacheAccessCount.put(cacheKey, 1);

            if (geographicCache.size() > maxCacheSize) {
                evictOldestGeographicEntries(cacheKey);
            }
        } finally {
            cacheLock.writeLock().unlock();
        }

        // Guardar en disco
        saveGeographicReportToDisk(locationId, locationType, electionId, result);

        return result;
    }

    @Override
    public boolean areReportsReady(int electionId, Current current) {
        try {
            // Verificar si tenemos reportes en cache o en disco
            String electionKey = "election_" + electionId;
            if (electionCache.containsKey(electionId) && !isCacheExpired(electionKey)) {
                return true;
            }

            // Verificar en disco
            Path electionFile = getElectionReportPath(electionId);
            if (Files.exists(electionFile)) {
                return true;
            }

            // Consultar al servidor principal
            return checkReportsReadyOnServer(electionId);

        } catch (Exception e) {
            logger.error("Error checking if reports are ready for election: {}", electionId, e);
            return false;
        }
    }

    @Override
    public void preloadReports(int electionId, Current current) {
        logger.info("Preloading reports for election: {}", electionId);

        // Ejecutar en background para no bloquear la llamada
        new Thread(() -> {
            try {
                // Precargar reportes de elección
                getElectionReports(electionId, current);

                // Precargar algunos reportes geográficos comunes
                preloadCommonGeographicReports(electionId);

                logger.info("Preload completed for election: {}", electionId);

            } catch (Exception e) {
                logger.error("Error during preload for election: {}", electionId, e);
            }
        }, "ReportsPreloader-" + electionId).start();
    }

    @Override
    public void syncWithMainServer(Current current) {
        logger.info("Starting synchronization with main server: {}", serverEndpoint);

        try {
            // Obtener lista de archivos disponibles del servidor
            List<String> availableFiles = getAvailableFilesFromServer();

            // Sincronizar archivos faltantes o desactualizados
            int syncedFiles = 0;
            for (String fileName : availableFiles) {
                if (needsSync(fileName)) {
                    syncFileFromServer(fileName);
                    syncedFiles++;
                }
            }

            logger.info("Synchronization completed. Synced {} files", syncedFiles);

        } catch (Exception e) {
            logger.error("Error during synchronization with main server", e);
            throw new RuntimeException("Synchronization failed", e);
        }
    }

    // =================== FILE SERVING METHODS ===================

    @Override
    public byte[] getCitizenReportFile(String documento, int electionId, Current current) {
        try {
            Path filePath = getCitizenReportPath(documento, electionId);

            if (Files.exists(filePath)) {
                logger.debug("Serving citizen report file: {}", filePath);
                return Files.readAllBytes(filePath);
            }

            // Si no existe localmente, intentar obtener del servidor
            byte[] data = fetchCitizenReportFileFromServer(documento, electionId);
            if (data != null && data.length > 0) {
                // Guardar para futuras consultas
                Files.createDirectories(filePath.getParent());
                Files.write(filePath, data);
                return data;
            }

            logger.warn("Citizen report file not found: documento={}, electionId={}", documento, electionId);
            return new byte[0];

        } catch (IOException e) {
            logger.error("Error reading citizen report file: documento={}, electionId={}",
                    documento, electionId, e);
            return new byte[0];
        }
    }

    @Override
    public byte[] getElectionReportFile(int electionId, Current current) {
        try {
            Path filePath = getElectionReportPath(electionId);

            if (Files.exists(filePath)) {
                logger.debug("Serving election report file: {}", filePath);
                return Files.readAllBytes(filePath);
            }

            // Obtener del servidor si no existe
            byte[] data = fetchElectionReportFileFromServer(electionId);
            if (data != null && data.length > 0) {
                Files.createDirectories(filePath.getParent());
                Files.write(filePath, data);
                return data;
            }

            logger.warn("Election report file not found: electionId={}", electionId);
            return new byte[0];

        } catch (IOException e) {
            logger.error("Error reading election report file: electionId={}", electionId, e);
            return new byte[0];
        }
    }

    @Override
    public byte[] getGeographicReportFile(int locationId, String locationType,
                                          int electionId, Current current) {
        try {
            Path filePath = getGeographicReportPath(locationId, locationType, electionId);

            if (Files.exists(filePath)) {
                logger.debug("Serving geographic report file: {}", filePath);
                return Files.readAllBytes(filePath);
            }

            // Obtener del servidor
            byte[] data = fetchGeographicReportFileFromServer(locationId, locationType, electionId);
            if (data != null && data.length > 0) {
                Files.createDirectories(filePath.getParent());
                Files.write(filePath, data);
                return data;
            }

            logger.warn("Geographic report file not found: locationId={}, locationType={}, electionId={}",
                    locationId, locationType, electionId);
            return new byte[0];

        } catch (IOException e) {
            logger.error("Error reading geographic report file: locationId={}, locationType={}, electionId={}",
                    locationId, locationType, electionId, e);
            return new byte[0];
        }
    }

    @Override
    public String[] getAvailableReportFiles(String reportType, int electionId, Current current) {
        try {
            List<String> files = new ArrayList<>();
            Path typeDir = Paths.get(cacheDirectory, reportType);

            if (Files.exists(typeDir)) {
                Files.walk(typeDir)
                        .filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().contains("_" + electionId + "_"))
                        .forEach(path -> files.add(path.getFileName().toString()));
            }

            // También consultar al servidor por archivos adicionales
            String[] serverFiles = getAvailableReportFilesFromServer(reportType, electionId);
            for (String serverFile : serverFiles) {
                if (!files.contains(serverFile)) {
                    files.add(serverFile);
                }
            }

            return files.toArray(new String[0]);

        } catch (Exception e) {
            logger.error("Error getting available report files: reportType={}, electionId={}",
                    reportType, electionId, e);
            return new String[0];
        }
    }

    @Override
    public Map<String, String> getReportFileMetadata(String fileName, Current current) {
        Map<String, String> metadata = new HashMap<>();

        try {
            // Buscar archivo en cache local
            Path filePath = findFileInCache(fileName);

            if (filePath != null && Files.exists(filePath)) {
                metadata.put("size", String.valueOf(Files.size(filePath)));
                metadata.put("lastModified", String.valueOf(Files.getLastModifiedTime(filePath).toMillis()));
                metadata.put("location", "org/votaciones/cache");
                metadata.put("path", filePath.toString());
            } else {
                // Obtener metadata del servidor
                metadata = getReportFileMetadataFromServer(fileName);
                metadata.put("location", "server");
            }

            metadata.put("fileName", fileName);
            metadata.put("timestamp", String.valueOf(System.currentTimeMillis()));

        } catch (Exception e) {
            logger.error("Error getting metadata for file: {}", fileName, e);
            metadata.put("error", e.getMessage());
        }

        return metadata;
    }

    @Override
    public boolean reportFileExists(String fileName, Current current) {
        try {
            // Verificar en cache local primero
            Path filePath = findFileInCache(fileName);
            if (filePath != null && Files.exists(filePath)) {
                return true;
            }

            // Verificar en servidor
            return reportFileExistsOnServer(fileName);

        } catch (Exception e) {
            logger.error("Error checking if report file exists: {}", fileName, e);
            return false;
        }
    }

    @Override
    public Map<String, byte[]> getBulkReportFiles(String[] fileNames, Current current) {
        Map<String, byte[]> result = new HashMap<>();

        for (String fileName : fileNames) {
            try {
                Path filePath = findFileInCache(fileName);

                if (filePath != null && Files.exists(filePath)) {
                    // Leer desde cache local
                    result.put(fileName, Files.readAllBytes(filePath));
                } else {
                    // Obtener del servidor
                    byte[] data = getReportFileFromServer(fileName);
                    if (data != null && data.length > 0) {
                        result.put(fileName, data);

                        // Guardar en cache para futuro uso
                        saveFileToCache(fileName, data);
                    }
                }

            } catch (Exception e) {
                logger.error("Error getting bulk file: {}", fileName, e);
                result.put(fileName, new byte[0]); // Archivo vacío en caso de error
            }
        }

        logger.info("Bulk file request completed: {} files requested, {} files returned",
                fileNames.length, result.size());

        return result;
    }

    // =================== CACHE MANAGEMENT ===================

    @Override
    public void refreshCache(int electionId, Current current) {
        logger.info("Refreshing cache for election: {}", electionId);

        cacheLock.writeLock().lock();
        try {
            // Remover entradas de cache para esta elección
            citizenCache.entrySet().removeIf(entry -> entry.getKey().contains("_" + electionId + "_"));
            electionCache.remove(electionId);
            geographicCache.entrySet().removeIf(entry -> entry.getKey().contains("_" + electionId + "_"));

            // Limpiar timestamps y contadores
            cacheTimestamps.entrySet().removeIf(entry -> entry.getKey().contains("_" + electionId + "_"));
            cacheAccessCount.entrySet().removeIf(entry -> entry.getKey().contains("_" + electionId + "_"));

        } finally {
            cacheLock.writeLock().unlock();
        }

        // Forzar recarga desde servidor
        preloadReports(electionId, current);
    }

    @Override
    public Map<String, String> getCacheStats(Current current) {
        Map<String, String> stats = new HashMap<>();

        cacheLock.readLock().lock();
        try {
            stats.put("citizenCacheSize", String.valueOf(citizenCache.size()));
            stats.put("electionCacheSize", String.valueOf(electionCache.size()));
            stats.put("geographicCacheSize", String.valueOf(geographicCache.size()));
            stats.put("totalCacheEntries", String.valueOf(
                    citizenCache.size() + electionCache.size() + geographicCache.size()));

            // Estadísticas de hits/misses (se podrían implementar contadores)
            stats.put("cacheHitRate", "N/A"); // TODO: Implementar contadores de hits/misses

            // Información de memoria
            Runtime runtime = Runtime.getRuntime();
            stats.put("memoryUsed", String.valueOf((runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)));
            stats.put("memoryTotal", String.valueOf(runtime.totalMemory() / (1024 * 1024)));

            stats.put("cacheDirectory", cacheDirectory);
            stats.put("serverEndpoint", serverEndpoint);
            stats.put("uptime", String.valueOf(System.currentTimeMillis()));

        } finally {
            cacheLock.readLock().unlock();
        }

        return stats;
    }

    @Override
    public void clearCache(int electionId, Current current) {
        logger.info("Clearing cache for election: {}", electionId);

        cacheLock.writeLock().lock();
        try {
            // Limpiar cache en memoria
            String electionIdStr = "_" + electionId + "_";
            citizenCache.entrySet().removeIf(entry -> entry.getKey().contains(electionIdStr));
            electionCache.remove(electionId);
            geographicCache.entrySet().removeIf(entry -> entry.getKey().contains(electionIdStr));

            // Limpiar metadata
            cacheTimestamps.entrySet().removeIf(entry -> entry.getKey().contains(electionIdStr));
            cacheAccessCount.entrySet().removeIf(entry -> entry.getKey().contains(electionIdStr));

        } finally {
            cacheLock.writeLock().unlock();
        }

        // Limpiar archivos de disco (opcional)
        clearDiskCacheForElection(electionId);
    }

    // =================== MÉTODOS PRIVADOS DE UTILIDAD ===================

    private String generateCitizenCacheKey(String documento, int electionId) {
        return "citizen_" + documento + "_" + electionId;
    }

    private String generateGeographicCacheKey(int locationId, String locationType, int electionId) {
        return "geographic_" + locationId + "_" + locationType + "_" + electionId;
    }

    private boolean isCacheExpired(String cacheKey) {
        Long timestamp = cacheTimestamps.get(cacheKey);
        if (timestamp == null) {
            return true;
        }
        return (System.currentTimeMillis() - timestamp) > cacheExpirationMs;
    }

    private void incrementAccessCount(String cacheKey) {
        cacheAccessCount.merge(cacheKey, 1, Integer::sum);
    }

    private Path getCitizenReportPath(String documento, int electionId) {
        return Paths.get(cacheDirectory, "citizen", "citizen_" + documento + "_" + electionId + ".json");
    }

    private Path getElectionReportPath(int electionId) {
        return Paths.get(cacheDirectory, "election", "election_" + electionId + ".json");
    }

    private Path getGeographicReportPath(int locationId, String locationType, int electionId) {
        return Paths.get(cacheDirectory, "geographic",
                "geographic_" + locationId + "_" + locationType + "_" + electionId + ".json");
    }

    // =================== STUBS PARA COMUNICACIÓN CON SERVIDOR ===================
    // Estos métodos necesitan implementarse según tu protocolo de comunicación

    private CitizenReportsConfiguration fetchCitizenReportsFromServer(String documento, int electionId) {
        // TODO: Implementar comunicación con servidor principal
        logger.warn("fetchCitizenReportsFromServer not implemented - returning mock data");
        return createMockCitizenReports(documento, electionId);
    }

    private ElectionReportsConfiguration fetchElectionReportsFromServer(int electionId) {
        // TODO: Implementar comunicación con servidor principal
        logger.warn("fetchElectionReportsFromServer not implemented - returning mock data");
        return createMockElectionReports(electionId);
    }

    private GeographicReportsConfiguration fetchGeographicReportsFromServer(int locationId, String locationType, int electionId) {
        // TODO: Implementar comunicación con servidor principal
        logger.warn("fetchGeographicReportsFromServer not implemented - returning mock data");
        return createMockGeographicReports(locationId, locationType, electionId);
    }

    // Métodos adicionales que necesitan implementación
    private boolean checkReportsReadyOnServer(int electionId) { return true; }
    private void preloadCommonGeographicReports(int electionId) { }
    private List<String> getAvailableFilesFromServer() { return new ArrayList<>(); }
    private boolean needsSync(String fileName) { return false; }
    private void syncFileFromServer(String fileName) { }
    private byte[] fetchCitizenReportFileFromServer(String documento, int electionId) { return new byte[0]; }
    private byte[] fetchElectionReportFileFromServer(int electionId) { return new byte[0]; }
    private byte[] fetchGeographicReportFileFromServer(int locationId, String locationType, int electionId) { return new byte[0]; }
    private String[] getAvailableReportFilesFromServer(String reportType, int electionId) { return new String[0]; }
    private Map<String, String> getReportFileMetadataFromServer(String fileName) { return new HashMap<>(); }
    private boolean reportFileExistsOnServer(String fileName) { return false; }
    private byte[] getReportFileFromServer(String fileName) { return new byte[0]; }

    // Métodos auxiliares
    private void saveCitizenReportToDisk(String documento, int electionId, CitizenReportsConfiguration config) { }
    private void saveElectionReportToDisk(int electionId, ElectionReportsConfiguration config) { }
    private void saveGeographicReportToDisk(int locationId, String locationType, int electionId, GeographicReportsConfiguration config) { }
    private void evictOldestEntries(Map<String, ?> cache, String newKey) { }
    private void evictOldestElectionEntries(int electionId) { }
    private void evictOldestGeographicEntries(String cacheKey) { }
    private Path findFileInCache(String fileName) { return null; }
    private void saveFileToCache(String fileName, byte[] data) { }
    private void clearDiskCacheForElection(int electionId) { }

    // =================== MÉTODOS MOCK PARA TESTING ===================

    private CitizenReportsConfiguration createMockCitizenReports(String documento, int electionId) {
        CitizenReportsConfiguration config = new CitizenReportsConfiguration();

        // Crear citizen info mock
        CitizenInfo citizen = new CitizenInfo();
        citizen.id = 1;
        citizen.documento = documento;
        citizen.nombre = "Juan";
        citizen.apellido = "Pérez";

        // Crear location info mock
        LocationInfo location = new LocationInfo();
        location.departamentoId = 5;
        location.departamentoNombre = "Antioquia";
        location.municipioId = 1;
        location.municipioNombre = "Medellín";

        // Crear election info mock
        ElectionInfo election = new ElectionInfo();
        election.id = electionId;
        election.nombre = "Elección Presidencial 2024";
        election.estado = "active";

        // Crear assignment
        CitizenVotingAssignment assignment = new CitizenVotingAssignment();
        assignment.citizen = citizen;
        assignment.location = location;
        assignment.election = election;
        assignment.generationTimestamp = System.currentTimeMillis();

        config.assignment = assignment;
        config.availableElections = new ElectionInfo[]{election};
        config.packageVersion = "1.0.0";
        config.generationTimestamp = System.currentTimeMillis();

        return config;
    }

    private ElectionReportsConfiguration createMockElectionReports(int electionId) {
        ElectionReportsConfiguration config = new ElectionReportsConfiguration();

        // Mock election results
        ElectionResults results = new ElectionResults();
        ElectionInfo election = new ElectionInfo();
        election.id = electionId;
        election.nombre = "Elección Presidencial 2024";
        results.election = election;
        results.totalVotes = 1000000;
        results.totalEligibleVoters = 1500000;
        results.participationPercentage = 66.67;
        results.generationTimestamp = System.currentTimeMillis();

        config.results = results;
        config.packageVersion = "1.0.0";
        config.generationTimestamp = System.currentTimeMillis();

        return config;
    }

    private GeographicReportsConfiguration createMockGeographicReports(int locationId, String locationType, int electionId) {
        GeographicReportsConfiguration config = new GeographicReportsConfiguration();

        // Mock geographic stats
        GeographicStats stats = new GeographicStats();

        LocationInfo location = new LocationInfo();
        location.departamentoId = locationId;
        location.departamentoNombre = "Antioquia";

        ElectionInfo election = new ElectionInfo();
        election.id = electionId;
        election.nombre = "Elección Presidencial 2024";

        stats.location = location;
        stats.election = election;
        stats.totalMesas = 100;
        stats.totalPuestos = 20;
        stats.totalCitizens = 50000;
        stats.totalVotes = 35000;
        stats.participationPercentage = 70.0;
        stats.generationTimestamp = System.currentTimeMillis();

        config.stats = stats;
        config.packageVersion = "1.0.0";
        config.generationTimestamp = System.currentTimeMillis();

        return config;
    }
}