package org.votaciones;

import ReportsSystem.ReportsService;
import ReportsSystem.ReportsServicePrx;
import com.zeroc.Ice.Current;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * ProxyCache INTELIGENTE completo para Reports
 * Sistema de cache predictivo basado en patrones de uso
 */
public class ProxyCacheReports implements ReportsService {

    private static final Logger logger = LoggerFactory.getLogger(ProxyCacheReports.class);

    // =================== CONEXIÓN AL SERVIDOR ===================
    private final ReportsServicePrx reportsServer;

    // =================== CACHE LOCAL TRADICIONAL ===================
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = 5 * 60 * 1000; // 5 minutos
    private static final int CACHE_TTL_MINUTES = 5;

    // =================== NUEVO: SISTEMA DE CACHE INTELIGENTE ===================

    // 🧠 Análisis de patrones de consulta
    private final Map<String, QueryPattern> queryPatterns = new ConcurrentHashMap<>();
    private final ScheduledExecutorService smartCacheScheduler = Executors.newScheduledThreadPool(2);

    // 📊 Métricas del sistema inteligente
    private final AtomicLong totalQueries = new AtomicLong(0);
    private final AtomicLong predictiveLoads = new AtomicLong(0);
    private final AtomicLong cacheHits = new AtomicLong(0);
    private final AtomicLong cacheMisses = new AtomicLong(0);

    // ⚙️ Configuración del cache inteligente
    private static final long ANALYSIS_WINDOW_MS = 10 * 60 * 1000; // 10 minutos
    private static final long SHORT_BURST_WINDOW_MS = 2 * 60 * 1000; // 2 minutos

    // 🎯 Umbrales de activación
    private static final int PUESTO_THRESHOLD = 3;      // 3+ consultas → precargar puesto
    private static final int MUNICIPALITY_THRESHOLD = 5; // 5+ consultas → precargar municipio
    private static final int DEPARTMENT_THRESHOLD = 8;   // 8+ consultas → precargar departamento

    // 🔥 Umbrales de intensidad (consultas por minuto)
    private static final double HIGH_INTENSITY = 2.0;    // 2+ consultas/min → alta prioridad
    private static final double MEDIUM_INTENSITY = 1.0;  // 1+ consultas/min → media prioridad

    // =================== CLASES AUXILIARES ===================

    /**
     * Entrada del cache tradicional
     */
    private static class CacheEntry {
        private final String data;
        private final long timestamp;

        public CacheEntry(String data, long timestamp) {
            this.data = data;
            this.timestamp = timestamp;
        }

        public String getData() { return data; }
        public long getTimestamp() { return timestamp; }

        public boolean isExpired(long ttlMs) {
            return System.currentTimeMillis() - timestamp > ttlMs;
        }
    }

    /**
     * Patrón de consulta por ubicación geográfica
     */
    private static class QueryPattern {
        private final Queue<Long> timestamps = new LinkedList<>();
        private final AtomicInteger totalQueries = new AtomicInteger(0);
        private final String locationType;
        private final int locationId;
        private volatile long lastPredictiveLoad = 0;
        private volatile boolean isHotspot = false;

        public QueryPattern(String locationType, int locationId) {
            this.locationType = locationType;
            this.locationId = locationId;
        }

        public synchronized void addQuery() {
            long now = System.currentTimeMillis();
            timestamps.offer(now);
            totalQueries.incrementAndGet();

            // Limpiar timestamps antiguos (fuera de ventana de análisis)
            while (!timestamps.isEmpty() &&
                    (now - timestamps.peek()) > ANALYSIS_WINDOW_MS) {
                timestamps.poll();
            }
        }

        public synchronized int getRecentQueries() {
            long now = System.currentTimeMillis();
            // Limpiar timestamps antiguos
            while (!timestamps.isEmpty() &&
                    (now - timestamps.peek()) > ANALYSIS_WINDOW_MS) {
                timestamps.poll();
            }
            return timestamps.size();
        }

        public synchronized int getBurstQueries() {
            long now = System.currentTimeMillis();
            int burstCount = 0;
            for (Long timestamp : timestamps) {
                if ((now - timestamp) <= SHORT_BURST_WINDOW_MS) {
                    burstCount++;
                }
            }
            return burstCount;
        }

        public double getIntensity() {
            int recentQueries = getRecentQueries();
            return recentQueries / (ANALYSIS_WINDOW_MS / 60000.0); // queries per minute
        }

        public boolean needsPredictiveLoad() {
            long now = System.currentTimeMillis();

            // No precargar si ya se hizo recientemente (últimos 3 minutos)
            if ((now - lastPredictiveLoad) < (3 * 60 * 1000)) {
                return false;
            }

            int recentQueries = getRecentQueries();
            int burstQueries = getBurstQueries();
            double intensity = getIntensity();

            // Detectar hotspot por tipo de ubicación
            switch (locationType.toLowerCase()) {
                case "puesto":
                    return burstQueries >= PUESTO_THRESHOLD || intensity >= HIGH_INTENSITY;
                case "municipality":
                case "municipio":
                    return recentQueries >= MUNICIPALITY_THRESHOLD || intensity >= MEDIUM_INTENSITY;
                case "department":
                case "departamento":
                    return recentQueries >= DEPARTMENT_THRESHOLD;
                case "mesa":
                    return burstQueries >= 2; // Mesa más sensible
                default:
                    return false;
            }
        }

        public void markPredictiveLoad() {
            this.lastPredictiveLoad = System.currentTimeMillis();
            this.isHotspot = true;
        }

        // Getters
        public String getLocationType() { return locationType; }
        public int getLocationId() { return locationId; }
        public int getTotalQueries() { return totalQueries.get(); }
        public boolean isHotspot() { return isHotspot; }
    }

    // =================== CONSTRUCTOR MEJORADO ===================
    public ProxyCacheReports(ReportsServicePrx reportsServer) {
        this.reportsServer = reportsServer;
        logger.info("🧠 ProxyCacheReports INTELIGENTE inicializado");

        // Iniciar análisis inteligente cada 30 segundos
        smartCacheScheduler.scheduleAtFixedRate(this::analyzeAndPreload, 30, 30, TimeUnit.SECONDS);

        // Limpieza de patrones antiguos cada 5 minutos
        smartCacheScheduler.scheduleAtFixedRate(this::cleanOldPatterns, 5, 5, TimeUnit.MINUTES);

        logger.info("⚡ Sistema de cache inteligente ACTIVO - Análisis cada 30s");
    }

    // =================== MÉTODOS PRINCIPALES CON ANÁLISIS ===================

    @Override
    public String getCitizenReports(String documento, int electionId, Current current) {
        totalQueries.incrementAndGet();

        // 🔍 Analizar patrón de consulta de ciudadano
        analyzeCitizenQuery(documento, electionId);

        String cacheKey = generateCacheKey("citizen", documento, String.valueOf(electionId));
        return getFromCacheWithStats(cacheKey, () -> {
            logger.debug("Consultando citizen reports para documento: {}", documento);
            return reportsServer.getCitizenReports(documento, electionId);
        });
    }

    @Override
    public String[] searchCitizenReports(String nombre, String apellido, int electionId, int limit, Current current) {
        totalQueries.incrementAndGet();

        String cacheKey = generateCacheKey("search", nombre + "_" + apellido, electionId + "_" + limit);
        String cachedResult = getFromCacheWithStats(cacheKey, () -> {
            logger.debug("Consultando search reports para: {} {}", nombre, apellido);
            String[] results = reportsServer.searchCitizenReports(nombre, apellido, electionId, limit);
            return String.join("###", results);
        });

        if (cachedResult.startsWith("ERROR-")) {
            return new String[]{cachedResult};
        }
        return cachedResult.isEmpty() ? new String[0] : cachedResult.split("###");
    }

    @Override
    public String[] getMesaCitizenReports(int mesaId, int electionId, Current current) {
        totalQueries.incrementAndGet();

        // 🔍 Analizar patrón de consulta de mesa
        analyzeLocationQuery("mesa", mesaId, electionId);

        String cacheKey = generateCacheKey("mesa", String.valueOf(mesaId), String.valueOf(electionId));
        String cachedResult = getFromCacheWithStats(cacheKey, () -> {
            logger.debug("Consultando mesa citizen reports para mesa: {}", mesaId);
            String[] results = reportsServer.getMesaCitizenReports(mesaId, electionId);
            return String.join("###", results);
        });

        if (cachedResult.startsWith("ERROR-")) {
            return new String[]{cachedResult};
        }
        return cachedResult.isEmpty() ? new String[0] : cachedResult.split("###");
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
        totalQueries.incrementAndGet();

        String cacheKey = generateCacheKey("election", String.valueOf(electionId), "");
        return getFromCacheWithStats(cacheKey, () -> {
            logger.debug("Consultando election reports para elección: {}", electionId);
            return reportsServer.getElectionReports(electionId);
        });
    }

    @Override
    public String[] getAvailableElections(Current current) {
        totalQueries.incrementAndGet();

        String cacheKey = "available_elections";
        String cachedResult = getFromCacheWithStats(cacheKey, () -> {
            logger.debug("Consultando elecciones disponibles");
            String[] results = reportsServer.getAvailableElections();
            return String.join("###", results);
        });

        if (cachedResult.startsWith("ERROR-")) {
            return new String[]{cachedResult};
        }
        return cachedResult.isEmpty() ? new String[0] : cachedResult.split("###");
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
        totalQueries.incrementAndGet();

        // 🔍 Analizar patrón de consulta geográfica
        analyzeLocationQuery(locationType, locationId, electionId);

        String cacheKey = generateCacheKey("geographic", locationType + "_" + locationId, String.valueOf(electionId));
        return getFromCacheWithStats(cacheKey, () -> {
            logger.debug("Consultando geographic reports para {} {}", locationType, locationId);
            return reportsServer.getGeographicReports(locationId, locationType, electionId);
        });
    }

    public void preloadReports(int electionId, Current current) {
        // Método legacy - llama al nuevo método con tipo "basic"
        try {
            String result = preloadReports(electionId, "basic", 0, current);
            logger.info("Precarga legacy completada: {}", result.substring(0, Math.min(result.length(), 100)));
        } catch (Exception e) {
            logger.error("Error en precarga legacy: {}", e.getMessage());
        }
    }

    @Override
    public String preloadReports(int electionId, String locationType, int locationId, Current current) {
        logger.info("📥 Iniciando precarga tipo '{}' para elección {} (ubicación ID: {})",
                locationType, electionId, locationId);

        long startTime = System.currentTimeMillis();
        StringBuilder result = new StringBuilder();
        result.append("🚀 ========== PRECARGA DE REPORTES ==========\n");
        result.append(String.format("📊 Elección: %d | Tipo: %s | Ubicación: %d\n\n",
                electionId, locationType, locationId));

        try {
            switch (locationType.toLowerCase()) {
                case "basic":
                    return preloadBasicReports(electionId, result, startTime);
                case "department":
                    return preloadDepartmentReports(electionId, locationId, result, startTime);
                case "municipality":
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
            logger.error("❌ Error en precarga tipo '{}': {}", locationType, e.getMessage());
            result.append("❌ ERROR: ").append(e.getMessage()).append("\n");
            return result.toString();
        }
    }

    @Override
    public String getCacheStats(Current current) {
        StringBuilder stats = new StringBuilder();
        stats.append("🧠 ========== ESTADÍSTICAS CACHE INTELIGENTE ==========\n");

        // Estadísticas básicas del cache
        stats.append(String.format("💾 Total entradas en cache: %d\n", cache.size()));
        stats.append(String.format("🔄 TTL configurado: %d minutos\n", CACHE_TTL_MINUTES));

        // Estadísticas del sistema inteligente
        stats.append("\n📊 Sistema Inteligente:\n");
        stats.append(String.format("   🔍 Total consultas: %d\n", totalQueries.get()));
        stats.append(String.format("   ⚡ Precarga predictiva: %d\n", predictiveLoads.get()));
        stats.append(String.format("   📈 Cache hits: %d\n", cacheHits.get()));
        stats.append(String.format("   📉 Cache misses: %d\n", cacheMisses.get()));

        long totalCacheRequests = cacheHits.get() + cacheMisses.get();
        if (totalCacheRequests > 0) {
            double hitRate = (cacheHits.get() * 100.0) / totalCacheRequests;
            stats.append(String.format("   🎯 Hit rate: %.1f%%\n", hitRate));
        }

        // Patrones activos
        stats.append(String.format("\n🔥 Patrones de consulta activos: %d\n", queryPatterns.size()));

        // Top hotspots
        // Top hotspots
        List<QueryPattern> topHotspots = queryPatterns.values().stream()
                .filter(p -> p.getRecentQueries() > 0)
                .sorted((a, b) -> Integer.compare(b.getRecentQueries(), a.getRecentQueries()))
                .limit(5)
                .collect(Collectors.toList()); // ✅ CORRECTO

        if (!topHotspots.isEmpty()) {
            stats.append("\n🏆 Top Hotspots:\n");
            for (int i = 0; i < topHotspots.size(); i++) {
                QueryPattern pattern = topHotspots.get(i);
                stats.append(String.format("   %d. %s %d: %d consultas (%.1f/min)%s\n",
                        i + 1, pattern.getLocationType(), pattern.getLocationId(),
                        pattern.getRecentQueries(), pattern.getIntensity(),
                        pattern.isHotspot() ? " 🔥" : ""));
            }
        }

        // Análisis por tipo de cache
        Map<String, Integer> typeCount = new HashMap<>();
        Map<String, Long> typeSize = new HashMap<>();

        for (Map.Entry<String, CacheEntry> entry : cache.entrySet()) {
            String key = entry.getKey();
            String type = key.split("_")[0];
            long size = entry.getValue().getData().length();

            typeCount.merge(type, 1, Integer::sum);
            typeSize.merge(type, size, Long::sum);
        }

        stats.append("\n📋 Cache por tipo de contenido:\n");
        for (Map.Entry<String, Integer> entry : typeCount.entrySet()) {
            String type = entry.getKey();
            int count = entry.getValue();
            long size = typeSize.get(type);

            stats.append(String.format("   %s: %d entradas (%.1f KB)\n",
                    type, count, size / 1024.0));
        }

        // Memoria utilizada
        long totalSize = cache.values().stream().mapToLong(e -> e.getData().length()).sum();
        stats.append(String.format("\n💾 Memoria total utilizada: %.2f MB\n", totalSize / (1024.0 * 1024.0)));

        return stats.toString();
    }

    // =================== MÉTODOS AUXILIARES GEOGRÁFICOS ===================

    @Override
    public String[] getDepartmentCitizenDocuments(int departmentId, int electionId, Current current) {
        // Analizar consulta de departamento
        analyzeLocationQuery("department", departmentId, electionId);

        logger.debug("Proxy: getDepartmentCitizenDocuments para departamento {} elección {}", departmentId, electionId);

        try {
            String[] results = reportsServer.getDepartmentCitizenDocuments(departmentId, electionId);
            logger.info("Obtenidos {} documentos de ciudadanos para departamento {}", results.length, departmentId);
            return results;
        } catch (Exception e) {
            logger.error("Error obteniendo documentos de departamento {}: {}", departmentId, e.getMessage());
            return new String[]{"ERROR-Error obteniendo documentos de departamento: " + e.getMessage()};
        }
    }

    @Override
    public String[] getMunicipalityCitizenDocuments(int municipalityId, int electionId, Current current) {
        // Analizar consulta de municipio
        analyzeLocationQuery("municipality", municipalityId, electionId);

        logger.debug("Proxy: getMunicipalityCitizenDocuments para municipio {} elección {}", municipalityId, electionId);

        try {
            String[] results = reportsServer.getMunicipalityCitizenDocuments(municipalityId, electionId);
            logger.info("Obtenidos {} documentos de ciudadanos para municipio {}", results.length, municipalityId);
            return results;
        } catch (Exception e) {
            logger.error("Error obteniendo documentos de municipio {}: {}", municipalityId, e.getMessage());
            return new String[]{"ERROR-Error obteniendo documentos de municipio: " + e.getMessage()};
        }
    }

    @Override
    public String[] getPuestoCitizenDocuments(int puestoId, int electionId, Current current) {
        // Analizar consulta de puesto
        analyzeLocationQuery("puesto", puestoId, electionId);

        logger.debug("Proxy: getPuestoCitizenDocuments para puesto {} elección {}", puestoId, electionId);

        try {
            String[] results = reportsServer.getPuestoCitizenDocuments(puestoId, electionId);
            logger.info("Obtenidos {} documentos de ciudadanos para puesto {}", results.length, puestoId);
            return results;
        } catch (Exception e) {
            logger.error("Error obteniendo documentos de puesto {}: {}", puestoId, e.getMessage());
            return new String[]{"ERROR-Error obteniendo documentos de puesto: " + e.getMessage()};
        }
    }

    @Override
    public String[] getMesaCitizenDocuments(int mesaId, int electionId, Current current) {
        // Analizar consulta de mesa
        analyzeLocationQuery("mesa", mesaId, electionId);

        logger.debug("Proxy: getMesaCitizenDocuments para mesa {} elección {}", mesaId, electionId);

        try {
            String[] results = reportsServer.getMesaCitizenDocuments(mesaId, electionId);
            logger.info("Obtenidos {} documentos de ciudadanos para mesa {}", results.length, mesaId);
            return results;
        } catch (Exception e) {
            logger.error("Error obteniendo documentos de mesa {}: {}", mesaId, e.getMessage());
            return new String[]{"ERROR-Error obteniendo documentos de mesa: " + e.getMessage()};
        }
    }

    // =================== NUEVO: MÉTODOS DE ANÁLISIS INTELIGENTE ===================

    /**
     * 🔍 Analiza consultas de ciudadanos para detectar patrones por mesa/puesto
     */
    private void analyzeCitizenQuery(String documento, int electionId) {
        try {
            // Intentar extraer información geográfica del cache
            String citizenKey = generateCacheKey("citizen", documento, String.valueOf(electionId));
            CacheEntry entry = cache.get(citizenKey);

            if (entry != null) {
                // Extraer información geográfica del reporte cacheado
                extractLocationFromCitizenReport(entry.getData(), electionId);
            }

        } catch (Exception e) {
            logger.debug("Error analizando consulta de ciudadano: {}", e.getMessage());
        }
    }

    /**
     * 🗺️ Analiza consultas de ubicaciones geográficas
     */
    private void analyzeLocationQuery(String locationType, int locationId, int electionId) {
        String patternKey = generatePatternKey(locationType, locationId, electionId);

        QueryPattern pattern = queryPatterns.computeIfAbsent(patternKey,
                k -> new QueryPattern(locationType, locationId));

        pattern.addQuery();

        logger.debug("📊 Patrón actualizado: {} {} - {} consultas recientes",
                locationType, locationId, pattern.getRecentQueries());
    }

    /**
     * 🔬 Extrae información geográfica de reportes de ciudadanos
     */
    private void extractLocationFromCitizenReport(String citizenReport, int electionId) {
        try {
            // El reporte tiene formato: CITIZEN_DATA#LOCATION_DATA#ELECTION_DATA#...
            String[] parts = citizenReport.split("#");
            if (parts.length >= 2) {
                String locationData = parts[1];
                String[] locationParts = locationData.split("-");

                if (locationParts.length >= 10) {
                    // Formato: deptId-deptNombre-munId-munNombre-puestoId-puestoNombre-...-mesaId-...
                    int puestoId = Integer.parseInt(locationParts[4]);
                    int mesaId = Integer.parseInt(locationParts[8]);

                    // Analizar patrones de puesto y mesa
                    analyzeLocationQuery("puesto", puestoId, electionId);
                    analyzeLocationQuery("mesa", mesaId, electionId);
                }
            }
        } catch (Exception e) {
            logger.debug("Error extrayendo ubicación de reporte: {}", e.getMessage());
        }
    }

    /**
     * 🧠 Análisis principal: detecta hotspots y ejecuta precarga predictiva
     */
    private void analyzeAndPreload() {
        try {
            logger.debug("🔄 Iniciando análisis de patrones...");

            List<QueryPattern> hotspotsDetected = new ArrayList<>();

            // Analizar cada patrón de consulta
            for (QueryPattern pattern : queryPatterns.values()) {
                if (pattern.needsPredictiveLoad()) {
                    hotspotsDetected.add(pattern);
                    logger.info("🔥 HOTSPOT detectado: {} {} ({} consultas, intensidad: {:.1f})",
                            pattern.getLocationType(), pattern.getLocationId(),
                            pattern.getRecentQueries(), pattern.getIntensity());
                }
            }

            // Ejecutar precarga predictiva para hotspots
            for (QueryPattern hotspot : hotspotsDetected) {
                executePredictivePreload(hotspot);
            }

            if (hotspotsDetected.size() > 0) {
                logger.info("⚡ Análisis completado: {} hotspots procesados", hotspotsDetected.size());
            }

        } catch (Exception e) {
            logger.error("Error durante análisis de patrones: {}", e.getMessage());
        }
    }

    /**
     * 🚀 Ejecuta precarga predictiva para un hotspot detectado
     */
    private void executePredictivePreload(QueryPattern hotspot) {
        try {
            // Obtener la elección más común (usar 1 por defecto)
            int electionId = 1; // TODO: Mejorar para detectar elección más consultada

            logger.info("📥 Ejecutando precarga predictiva: {} {}",
                    hotspot.getLocationType(), hotspot.getLocationId());

            // Ejecutar precarga en background
            smartCacheScheduler.submit(() -> {
                try {
                    String result = preloadReports(electionId, hotspot.getLocationType(),
                            hotspot.getLocationId(), null);

                    hotspot.markPredictiveLoad();
                    predictiveLoads.incrementAndGet();

                    logger.info("✅ Precarga predictiva completada para {} {}",
                            hotspot.getLocationType(), hotspot.getLocationId());

                } catch (Exception e) {
                    logger.error("Error en precarga predictiva: {}", e.getMessage());
                }
            });

        } catch (Exception e) {
            logger.error("Error ejecutando precarga predictiva: {}", e.getMessage());
        }
    }

    /**
     * 🧹 Limpia patrones antiguos para optimizar memoria
     */
    private void cleanOldPatterns() {
        try {
            int initialSize = queryPatterns.size();

            queryPatterns.entrySet().removeIf(entry -> {
                QueryPattern pattern = entry.getValue();
                return pattern.getRecentQueries() == 0 && !pattern.isHotspot();
            });

            int removedPatterns = initialSize - queryPatterns.size();
            if (removedPatterns > 0) {
                logger.debug("🧹 Limpieza de patrones: {} patrones antiguos removidos", removedPatterns);
            }

        } catch (Exception e) {
            logger.error("Error limpiando patrones antiguos: {}", e.getMessage());
        }
    }

    // =================== MÉTODOS HELPER MEJORADOS ===================

    /**
     * Cache con estadísticas mejoradas
     */
    private String getFromCacheWithStats(String cacheKey, ServerCall serverCall) {
        // 1. Verificar cache local primero
        CacheEntry entry = cache.get(cacheKey);
        if (entry != null && !entry.isExpired(CACHE_TTL_MS)) {
            cacheHits.incrementAndGet();
            logger.debug("📈 Cache HIT para: {}", cacheKey);
            return entry.getData();
        }

        // 2. Cache miss - consultar servidor
        cacheMisses.incrementAndGet();
        try {
            logger.debug("📉 Cache MISS para: {} - consultando servidor", cacheKey);
            String result = serverCall.call();

            // 3. Guardar en cache local
            cache.put(cacheKey, new CacheEntry(result, System.currentTimeMillis()));

            logger.info("💾 Resultado cacheado para: {}", cacheKey);
            return result;

        } catch (Exception e) {
            logger.error("❌ Error consultando servidor para {}: {}", cacheKey, e.getMessage());

            // 4. Fallback: devolver cache expirado si existe
            if (entry != null) {
                logger.warn("⚠️ Usando cache expirado como fallback para: {}", cacheKey);
                return entry.getData();
            }

            return "ERROR-No se pudo obtener el reporte-" + System.currentTimeMillis();
        }
    }

    private String getFromCache(String cacheKey, ServerCall serverCall) {
        return getFromCacheWithStats(cacheKey, serverCall);
    }

    private String generatePatternKey(String locationType, int locationId, int electionId) {
        return locationType + "_" + locationId + "_" + electionId;
    }

    private String generateCacheKey(String type, String id1, String id2) {
        return type + "_" + id1 + "_" + id2;
    }

    // =================== MÉTODOS DE PRECARGA ORIGINALES ===================

    private String preloadBasicReports(int electionId, StringBuilder result, long startTime) {
        try {
            result.append("📋 PRECARGA BÁSICA\n");
            int itemsPreloaded = 0;

            // 1. Reporte general de la elección
            result.append("⏳ Precargando reporte de elección...\n");
            String electionReport = reportsServer.getElectionReports(electionId);
            String electionKey = generateCacheKey("election", String.valueOf(electionId), "");
            cache.put(electionKey, new CacheEntry(electionReport, System.currentTimeMillis()));
            itemsPreloaded++;
            result.append("   ✅ Reporte de elección cacheado\n");

            // 2. Elecciones disponibles
            result.append("⏳ Precargando lista de elecciones...\n");
            String[] elections = reportsServer.getAvailableElections();
            String electionsKey = "available_elections";
            cache.put(electionsKey, new CacheEntry(String.join("###", elections), System.currentTimeMillis()));
            itemsPreloaded++;
            result.append("   ✅ Lista de elecciones cacheada\n");

            // 3. Reportes geográficos principales (departamentos)
            result.append("⏳ Precargando reportes de departamentos principales...\n");
            int[] mainDepartments = {1, 2, 3, 5}; // IDs de departamentos principales
            for (int deptId : mainDepartments) {
                try {
                    String geoReport = reportsServer.getGeographicReports(deptId, "department", electionId);
                    String geoKey = generateCacheKey("geographic", "department_" + deptId, String.valueOf(electionId));
                    cache.put(geoKey, new CacheEntry(geoReport, System.currentTimeMillis()));
                    itemsPreloaded++;
                } catch (Exception e) {
                    result.append("   ⚠️ Error con departamento ").append(deptId).append("\n");
                }
            }
            result.append("   ✅ Reportes geográficos principales cacheados\n");

            long duration = System.currentTimeMillis() - startTime;
            result.append(String.format("\n✅ PRECARGA BÁSICA COMPLETADA\n"));
            result.append(String.format("📊 Items precargados: %d\n", itemsPreloaded));
            result.append(String.format("⏱️ Tiempo: %d ms\n", duration));

            return result.toString();

        } catch (Exception e) {
            logger.error("❌ Error en precarga básica: {}", e.getMessage());
            throw e;
        }
    }

    private String preloadDepartmentReports(int electionId, int departmentId, StringBuilder result, long startTime) {
        try {
            result.append(String.format("🏛️ PRECARGA DEPARTAMENTO %d\n", departmentId));

            // 1. Precargar reporte geográfico del departamento
            result.append("⏳ Precargando reporte geográfico del departamento...\n");
            String deptReport = reportsServer.getGeographicReports(departmentId, "department", electionId);
            String deptKey = generateCacheKey("geographic", "department_" + departmentId, String.valueOf(electionId));
            cache.put(deptKey, new CacheEntry(deptReport, System.currentTimeMillis()));
            result.append("   ✅ Reporte geográfico cacheado\n");

            // 2. Obtener todos los ciudadanos del departamento
            result.append("⏳ Obteniendo lista de ciudadanos del departamento...\n");
            String[] citizenDocuments = reportsServer.getDepartmentCitizenDocuments(departmentId, electionId);

            if (citizenDocuments.length > 0 && citizenDocuments[0].startsWith("ERROR")) {
                result.append("   ❌ Error obteniendo ciudadanos: ").append(citizenDocuments[0]).append("\n");
                return result.toString();
            }

            result.append(String.format("   📊 Encontrados %d ciudadanos\n", citizenDocuments.length));

            // 3. Precargar reportes de ciudadanos en lotes
            result.append("⏳ Precargando reportes de ciudadanos...\n");
            int preloadedCitizens = 0;
            int batchSize = 100;

            for (int i = 0; i < citizenDocuments.length; i += batchSize) {
                int batchEnd = Math.min(i + batchSize, citizenDocuments.length);

                for (int j = i; j < batchEnd; j++) {
                    try {
                        String documento = citizenDocuments[j];
                        String citizenKey = generateCacheKey("citizen", documento, String.valueOf(electionId));

                        if (!cache.containsKey(citizenKey)) {
                            String citizenReport = reportsServer.getCitizenReports(documento, electionId);
                            cache.put(citizenKey, new CacheEntry(citizenReport, System.currentTimeMillis()));
                            preloadedCitizens++;
                        }
                    } catch (Exception e) {
                        logger.warn("Error precargando ciudadano {}: {}", citizenDocuments[j], e.getMessage());
                    }
                }

                // Log de progreso cada lote
                if (i % (batchSize * 4) == 0) { // Log cada 4 lotes
                    result.append(String.format("   📈 Progreso: %d/%d ciudadanos\n",
                            Math.min(batchEnd, citizenDocuments.length), citizenDocuments.length));
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            result.append(String.format("\n✅ PRECARGA DEPARTAMENTO %d COMPLETADA\n", departmentId));
            result.append(String.format("📊 Ciudadanos precargados: %d/%d\n", preloadedCitizens, citizenDocuments.length));
            result.append(String.format("⏱️ Tiempo total: %d ms\n", duration));
            result.append(String.format("⚡ Promedio: %.2f ms/ciudadano\n",
                    duration / (double) Math.max(preloadedCitizens, 1)));

            return result.toString();

        } catch (Exception e) {
            logger.error("❌ Error en precarga de departamento {}: {}", departmentId, e.getMessage());
            result.append("❌ ERROR: ").append(e.getMessage()).append("\n");
            return result.toString();
        }
    }

    private String preloadMunicipalityReports(int electionId, int municipalityId, StringBuilder result, long startTime) {
        result.append(String.format("🏙️ PRECARGA MUNICIPIO %d\n", municipalityId));

        try {
            String munReport = reportsServer.getGeographicReports(municipalityId, "municipality", electionId);
            String munKey = generateCacheKey("geographic", "municipality_" + municipalityId, String.valueOf(electionId));
            cache.put(munKey, new CacheEntry(munReport, System.currentTimeMillis()));

            long duration = System.currentTimeMillis() - startTime;
            result.append(String.format("\n✅ PRECARGA MUNICIPIO %d COMPLETADA\n", municipalityId));
            result.append(String.format("⏱️ Tiempo: %d ms\n", duration));

            return result.toString();

        } catch (Exception e) {
            logger.error("❌ Error en precarga de municipio {}: {}", municipalityId, e.getMessage());
            result.append("❌ ERROR: ").append(e.getMessage()).append("\n");
            return result.toString();
        }
    }

    private String preloadPuestoReports(int electionId, int puestoId, StringBuilder result, long startTime) {
        try {
            result.append(String.format("🗳️ PRECARGA PUESTO %d\n", puestoId));

            // Obtener y precargar ciudadanos del puesto
            String[] citizenDocuments = reportsServer.getPuestoCitizenDocuments(puestoId, electionId);
            int preloadedCitizens = preloadCitizensBatch(citizenDocuments, electionId, result);

            long duration = System.currentTimeMillis() - startTime;
            result.append(String.format("\n✅ PRECARGA PUESTO %d COMPLETADA\n", puestoId));
            result.append(String.format("📊 Ciudadanos precargados: %d\n", preloadedCitizens));
            result.append(String.format("⏱️ Tiempo: %d ms\n", duration));

            return result.toString();

        } catch (Exception e) {
            logger.error("❌ Error en precarga de puesto {}: {}", puestoId, e.getMessage());
            result.append("❌ ERROR: ").append(e.getMessage()).append("\n");
            return result.toString();
        }
    }

    private String preloadMesaReports(int electionId, int mesaId, StringBuilder result, long startTime) {
        try {
            result.append(String.format("📋 PRECARGA MESA %d\n", mesaId));

            // Precargar ciudadanos de la mesa
            String[] mesaCitizens = reportsServer.getMesaCitizenReports(mesaId, electionId);
            String mesaKey = generateCacheKey("mesa", String.valueOf(mesaId), String.valueOf(electionId));
            cache.put(mesaKey, new CacheEntry(String.join("###", mesaCitizens), System.currentTimeMillis()));

            result.append(String.format("   📊 Ciudadanos de mesa cacheados: %d\n", mesaCitizens.length));

            long duration = System.currentTimeMillis() - startTime;
            result.append(String.format("\n✅ PRECARGA MESA %d COMPLETADA\n", mesaId));
            result.append(String.format("⏱️ Tiempo: %d ms\n", duration));

            return result.toString();

        } catch (Exception e) {
            logger.error("❌ Error en precarga de mesa {}: {}", mesaId, e.getMessage());
            result.append("❌ ERROR: ").append(e.getMessage()).append("\n");
            return result.toString();
        }
    }

    private String preloadAllReports(int electionId, StringBuilder result, long startTime) {
        result.append("🌐 PRECARGA COMPLETA DEL SISTEMA\n");
        result.append("⚠️ ADVERTENCIA: Esta operación puede tomar mucho tiempo\n\n");

        // Solo precarga básica por ahora
        return preloadBasicReports(electionId, result, startTime);
    }

    /**
     * Precarga ciudadanos en lotes
     */
    private int preloadCitizensBatch(String[] citizenDocuments, int electionId, StringBuilder result) {
        if (citizenDocuments.length > 0 && citizenDocuments[0].startsWith("ERROR")) {
            result.append("   ❌ Error obteniendo ciudadanos: ").append(citizenDocuments[0]).append("\n");
            return 0;
        }

        int preloadedCitizens = 0;
        int batchSize = 50;

        result.append(String.format("⏳ Precargando %d ciudadanos...\n", citizenDocuments.length));

        for (int i = 0; i < citizenDocuments.length; i += batchSize) {
            int batchEnd = Math.min(i + batchSize, citizenDocuments.length);

            for (int j = i; j < batchEnd; j++) {
                try {
                    String documento = citizenDocuments[j];
                    String citizenKey = generateCacheKey("citizen", documento, String.valueOf(electionId));

                    if (!cache.containsKey(citizenKey)) {
                        String citizenReport = reportsServer.getCitizenReports(documento, electionId);
                        cache.put(citizenKey, new CacheEntry(citizenReport, System.currentTimeMillis()));
                        preloadedCitizens++;
                    }
                } catch (Exception e) {
                    logger.warn("Error precargando ciudadano {}: {}", citizenDocuments[j], e.getMessage());
                }
            }

            if (i % (batchSize * 4) == 0) { // Log cada 4 lotes
                result.append(String.format("   📈 Progreso: %d/%d\n",
                        Math.min(batchEnd, citizenDocuments.length), citizenDocuments.length));
            }
        }

        return preloadedCitizens;
    }

    // =================== INTERFAZ FUNCIONAL HELPER ===================
    @FunctionalInterface
    private interface ServerCall {
        String call() throws Exception;
    }

    // =================== MÉTODOS DE MANTENIMIENTO ===================

    /**
     * Limpia el cache expirado
     */
    public void cleanExpiredCache() {
        long now = System.currentTimeMillis();
        AtomicInteger cleaned = new AtomicInteger();

        cache.entrySet().removeIf(entry -> {
            if (entry.getValue().isExpired(CACHE_TTL_MS)) {
                cleaned.getAndIncrement();
                return true;
            }
            return false;
        });

        if (cleaned.get() > 0) {
            logger.info("🧹 Cache limpiado: {} entradas expiradas removidas", cleaned);
        }
    }

    /**
     * Limpiar todo el cache
     */
    public void clearCache() {
        cache.clear();
        logger.info("🗑️ Cache completamente limpiado");
    }

    /**
     * Método para cerrar el sistema inteligente correctamente
     */
    public void shutdown() {
        try {
            logger.info("🛑 Cerrando sistema de cache inteligente...");
            smartCacheScheduler.shutdown();
            if (!smartCacheScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                smartCacheScheduler.shutdownNow();
            }
            logger.info("✅ Sistema de cache inteligente cerrado correctamente");
        } catch (InterruptedException e) {
            smartCacheScheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}