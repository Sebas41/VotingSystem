package org.votaciones;

import ReportsSystem.ReportsService;
import ReportsSystem.ReportsServicePrx;
import com.zeroc.Ice.Current;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ProxyCache completo para Reports (patrón máquina de café)
 * Implementa TODOS los métodos del ReportsService
 */
public class ProxyCacheReports implements ReportsService {

    private static final Logger logger = LoggerFactory.getLogger(ProxyCacheReports.class);

    // =================== CONEXIÓN AL SERVIDOR ===================
    private final ReportsServicePrx reportsServer;

    // =================== CACHE LOCAL (COMO EN MÁQUINA DE CAFÉ) ===================
    private final Map<String, String> citizenReportsCache = new ConcurrentHashMap<>();
    private final Map<String, String[]> searchResultsCache = new ConcurrentHashMap<>();
    private final Map<String, String[]> mesaCitizenCache = new ConcurrentHashMap<>();
    private final Map<String, String> electionReportsCache = new ConcurrentHashMap<>();
    private final Map<String, String> geographicReportsCache = new ConcurrentHashMap<>();
    private final Map<String, String[]> availableElectionsCache = new ConcurrentHashMap<>();

    // =================== TTL SIMPLE ===================
    private final Map<String, Long> cacheTimestamps = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = 5 * 60 * 1000; // 5 minutos
    private static final long ELECTION_CACHE_TTL_MS = 2 * 60 * 1000; // 2 minutos para elecciones

    public ProxyCacheReports(ReportsServicePrx reportsServer) {
        this.reportsServer = reportsServer;
        logger.info("ProxyCacheReports inicializado con cache local completo");
    }

    // =================== IMPLEMENTACIÓN COMPLETA DE MÉTODOS ===================

    @Override
    public String getCitizenReports(String documento, int electionId, Current current) {
        String cacheKey = "citizen_" + documento + "_" + electionId;
        return getFromCacheOrServer(cacheKey, () -> {
            logger.debug("Consultando citizen reports para documento: {}", documento);
            return reportsServer.getCitizenReports(documento, electionId);
        }, citizenReportsCache);
    }

    @Override
    public String[] searchCitizenReports(String nombre, String apellido, int electionId, int limit, Current current) {
        String cacheKey = "search_" + nombre + "_" + apellido + "_" + electionId + "_" + limit;
        return getArrayFromCacheOrServer(cacheKey, () -> {
            logger.debug("Consultando search reports para: {} {}", nombre, apellido);
            return reportsServer.searchCitizenReports(nombre, apellido, electionId, limit);
        }, searchResultsCache);
    }

    @Override
    public String[] getMesaCitizenReports(int mesaId, int electionId, Current current) {
        String cacheKey = "mesa_" + mesaId + "_" + electionId;
        return getArrayFromCacheOrServer(cacheKey, () -> {
            logger.debug("Consultando mesa citizen reports para mesa: {}", mesaId);
            return reportsServer.getMesaCitizenReports(mesaId, electionId);
        }, mesaCitizenCache);
    }

    @Override
    public boolean validateCitizenEligibility(String documento, Current current) {
        // No se cachea por seguridad - siempre consultar servidor
        try {
            logger.debug("Validando elegibilidad para documento: {}", documento);
            return reportsServer.validateCitizenEligibility(documento);
        } catch (Exception e) {
            logger.error("Error validando elegibilidad para {}: {}", documento, e.getMessage());
            return false;
        }
    }

    @Override
    public String getElectionReports(int electionId, Current current) {
        String cacheKey = "election_" + electionId;
        return getFromCacheOrServer(cacheKey, () -> {
            logger.debug("Consultando election reports para elección: {}", electionId);
            return reportsServer.getElectionReports(electionId);
        }, electionReportsCache, ELECTION_CACHE_TTL_MS);
    }

    @Override
    public String[] getAvailableElections(Current current) {
        String cacheKey = "available_elections";
        return getArrayFromCacheOrServer(cacheKey, () -> {
            logger.debug("Consultando elecciones disponibles");
            return reportsServer.getAvailableElections();
        }, availableElectionsCache, ELECTION_CACHE_TTL_MS);
    }

    @Override
    public boolean areReportsReady(int electionId, Current current) {
        // No se cachea - siempre consultar servidor
        try {
            logger.debug("Verificando si reportes están listos para elección: {}", electionId);
            return reportsServer.areReportsReady(electionId);
        } catch (Exception e) {
            logger.error("Error verificando reportes para elección {}: {}", electionId, e.getMessage());
            return false;
        }
    }

    @Override
    public String getGeographicReports(int locationId, String locationType, int electionId, Current current) {
        String cacheKey = "geographic_" + locationType + "_" + locationId + "_" + electionId;
        return getFromCacheOrServer(cacheKey, () -> {
            logger.debug("Consultando geographic reports para {} {}", locationType, locationId);
            return reportsServer.getGeographicReports(locationId, locationType, electionId);
        }, geographicReportsCache);
    }

    @Override
    public void preloadReports(int electionId, Current current) {
        // Precargar reportes principales en cache
        try {
            logger.info("Precargando reportes para elección: {}", electionId);

            // 1. Precargar reporte de elección
            String electionKey = "election_" + electionId;
            String electionReport = reportsServer.getElectionReports(electionId);
            electionReportsCache.put(electionKey, electionReport);
            cacheTimestamps.put(electionKey, System.currentTimeMillis());

            // 2. Precargar elecciones disponibles
            String availableKey = "available_elections";
            String[] availableElections = reportsServer.getAvailableElections();
            availableElectionsCache.put(availableKey, availableElections);
            cacheTimestamps.put(availableKey, System.currentTimeMillis());

            // 3. Delegar al servidor para precarga adicional
            reportsServer.preloadReports(electionId);

            logger.info("Precarga completada para elección: {}", electionId);
        } catch (Exception e) {
            logger.error("Error durante precarga para elección {}: {}", electionId, e.getMessage());
        }
    }

    // =================== MÉTODOS HELPER PARA CACHE ===================

    private String getFromCacheOrServer(String cacheKey, ServerCall<String> serverCall,
                                        Map<String, String> cache) {
        return getFromCacheOrServer(cacheKey, serverCall, cache, CACHE_TTL_MS);
    }

    private String getFromCacheOrServer(String cacheKey, ServerCall<String> serverCall,
                                        Map<String, String> cache, long ttl) {
        // 1. Verificar cache local primero
        if (isCacheValid(cacheKey, ttl)) {
            String cachedResult = cache.get(cacheKey);
            if (cachedResult != null) {
                logger.debug("Cache HIT para: {}", cacheKey);
                return cachedResult;
            }
        }

        // 2. Cache miss - consultar servidor
        try {
            logger.debug("Cache MISS para: {} - consultando servidor", cacheKey);
            String result = serverCall.call();

            // 3. Guardar en cache local
            cache.put(cacheKey, result);
            cacheTimestamps.put(cacheKey, System.currentTimeMillis());

            logger.info("Resultado cacheado para: {}", cacheKey);
            return result;

        } catch (Exception e) {
            logger.error("Error consultando servidor para {}: {}", cacheKey, e.getMessage());

            // 4. Fallback: devolver cache expirado si existe
            String fallbackResult = cache.get(cacheKey);
            if (fallbackResult != null) {
                logger.warn("Usando cache expirado como fallback para: {}", cacheKey);
                return fallbackResult;
            }

            return "ERROR-No se pudo obtener el reporte-" + System.currentTimeMillis();
        }
    }

    private String[] getArrayFromCacheOrServer(String cacheKey, ServerCall<String[]> serverCall,
                                               Map<String, String[]> cache) {
        return getArrayFromCacheOrServer(cacheKey, serverCall, cache, CACHE_TTL_MS);
    }

    private String[] getArrayFromCacheOrServer(String cacheKey, ServerCall<String[]> serverCall,
                                               Map<String, String[]> cache, long ttl) {
        // 1. Verificar cache local
        if (isCacheValid(cacheKey, ttl)) {
            String[] cachedResult = cache.get(cacheKey);
            if (cachedResult != null) {
                logger.debug("Cache HIT para array: {}", cacheKey);
                return cachedResult;
            }
        }

        // 2. Cache miss - consultar servidor
        try {
            logger.debug("Cache MISS para array: {} - consultando servidor", cacheKey);
            String[] result = serverCall.call();

            // 3. Guardar en cache local
            cache.put(cacheKey, result);
            cacheTimestamps.put(cacheKey, System.currentTimeMillis());

            logger.info("Array cacheado para: {}", cacheKey);
            return result;

        } catch (Exception e) {
            logger.error("Error consultando servidor para array {}: {}", cacheKey, e.getMessage());

            // 4. Fallback
            String[] fallbackResult = cache.get(cacheKey);
            if (fallbackResult != null) {
                logger.warn("Usando cache expirado como fallback para array: {}", cacheKey);
                return fallbackResult;
            }

            return new String[]{"ERROR-No se pudo obtener el reporte-" + System.currentTimeMillis()};
        }
    }

    private boolean isCacheValid(String cacheKey, long ttlMs) {
        Long timestamp = cacheTimestamps.get(cacheKey);
        if (timestamp == null) {
            return false;
        }

        long age = System.currentTimeMillis() - timestamp;
        return age < ttlMs;
    }

    // =================== MÉTODOS DE MANTENIMIENTO ===================

    /**
     * Limpia el cache expirado (como respaldarMaq() en máquina de café)
     */
    public void cleanExpiredCache() {
        long now = System.currentTimeMillis();
        int cleaned = 0;

        for (Map.Entry<String, Long> entry : cacheTimestamps.entrySet()) {
            if (now - entry.getValue() > CACHE_TTL_MS) {
                String key = entry.getKey();
                cacheTimestamps.remove(key);

                // Limpiar de todos los caches
                citizenReportsCache.remove(key);
                searchResultsCache.remove(key);
                mesaCitizenCache.remove(key);
                electionReportsCache.remove(key);
                geographicReportsCache.remove(key);
                availableElectionsCache.remove(key);

                cleaned++;
            }
        }

        if (cleaned > 0) {
            logger.info("Cache limpiado: {} entradas expiradas removidas", cleaned);
        }
    }

    /**
     * Estadísticas del cache (como la interfaz de la máquina)
     */
    public String getCacheStats() {
        int totalEntries = citizenReportsCache.size() + searchResultsCache.size() +
                mesaCitizenCache.size() + electionReportsCache.size() +
                geographicReportsCache.size() + availableElectionsCache.size();

        return String.format("CACHE_STATS-%d-%d-%d-%d-%d-%d-%d",
                totalEntries,
                citizenReportsCache.size(),
                searchResultsCache.size(),
                mesaCitizenCache.size(),
                electionReportsCache.size(),
                geographicReportsCache.size(),
                availableElectionsCache.size()
        );
    }

    /**
     * Limpiar todo el cache
     */
    public void clearCache() {
        citizenReportsCache.clear();
        searchResultsCache.clear();
        mesaCitizenCache.clear();
        electionReportsCache.clear();
        geographicReportsCache.clear();
        availableElectionsCache.clear();
        cacheTimestamps.clear();
        logger.info("Cache completamente limpiado");
    }

    // =================== INTERFAZ FUNCIONAL HELPER ===================
    @FunctionalInterface
    private interface ServerCall<T> {
        T call() throws Exception;
    }
}