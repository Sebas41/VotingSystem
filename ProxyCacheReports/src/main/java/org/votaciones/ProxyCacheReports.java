package org.votaciones;

import ReportsSystem.ReportsService;
import ReportsSystem.ReportsServicePrx;
import com.zeroc.Ice.Current;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ProxyCache completo para Reports (patrón máquina de café)
 * Implementa TODOS los métodos del ReportsService
 */
public class ProxyCacheReports implements ReportsService {

    private static final Logger logger = LoggerFactory.getLogger(ProxyCacheReports.class);

    // =================== CONEXIÓN AL SERVIDOR ===================
    private final ReportsServicePrx reportsServer;

    // =================== CACHE LOCAL UNIFICADO ===================
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = 5 * 60 * 1000; // 5 minutos
    private static final int CACHE_TTL_MINUTES = 5;

    // =================== CLASE AUXILIAR PARA CACHE ===================
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

    public ProxyCacheReports(ReportsServicePrx reportsServer) {
        this.reportsServer = reportsServer;
        logger.info("ProxyCacheReports inicializado con cache local completo");
    }

    // =================== IMPLEMENTACIÓN COMPLETA DE MÉTODOS ===================

    @Override
    public String getCitizenReports(String documento, int electionId, Current current) {
        String cacheKey = generateCacheKey("citizen", documento, String.valueOf(electionId));
        return getFromCache(cacheKey, () -> {
            logger.debug("Consultando citizen reports para documento: {}", documento);
            return reportsServer.getCitizenReports(documento, electionId);
        });
    }

    @Override
    public String[] searchCitizenReports(String nombre, String apellido, int electionId, int limit, Current current) {
        String cacheKey = generateCacheKey("search", nombre + "_" + apellido, electionId + "_" + limit);
        String cachedResult = getFromCache(cacheKey, () -> {
            logger.debug("Consultando search reports para: {} {}", nombre, apellido);
            String[] results = reportsServer.searchCitizenReports(nombre, apellido, electionId, limit);
            return String.join("###", results); // Convertir array a string para cache
        });

        // Convertir de vuelta a array
        if (cachedResult.startsWith("ERROR-")) {
            return new String[]{cachedResult};
        }
        return cachedResult.isEmpty() ? new String[0] : cachedResult.split("###");
    }

    @Override
    public String[] getMesaCitizenReports(int mesaId, int electionId, Current current) {
        String cacheKey = generateCacheKey("mesa", String.valueOf(mesaId), String.valueOf(electionId));
        String cachedResult = getFromCache(cacheKey, () -> {
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
        String cacheKey = generateCacheKey("election", String.valueOf(electionId), "");
        return getFromCache(cacheKey, () -> {
            logger.debug("Consultando election reports para elección: {}", electionId);
            return reportsServer.getElectionReports(electionId);
        });
    }

    @Override
    public String[] getAvailableElections(Current current) {
        String cacheKey = "available_elections";
        String cachedResult = getFromCache(cacheKey, () -> {
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
        String cacheKey = generateCacheKey("geographic", locationType + "_" + locationId, String.valueOf(electionId));
        return getFromCache(cacheKey, () -> {
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

    /**
     * Precarga reportes de manera inteligente según el tipo especificado
     */
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

    /**
     * Obtiene estadísticas del cache
     */
    @Override
    public String getCacheStats(Current current) {
        StringBuilder stats = new StringBuilder();
        stats.append("📊 ========== ESTADÍSTICAS DEL CACHE ==========\n");

        // Estadísticas básicas
        stats.append(String.format("💾 Total entradas: %d\n", cache.size()));
        stats.append(String.format("🔄 TTL configurado: %d minutos\n", CACHE_TTL_MINUTES));

        // Análisis por tipo
        Map<String, Integer> typeCount = new HashMap<>();
        Map<String, Long> typeSize = new HashMap<>();

        for (Map.Entry<String, CacheEntry> entry : cache.entrySet()) {
            String key = entry.getKey();
            String type = key.split("_")[0];
            long size = entry.getValue().getData().length();

            typeCount.merge(type, 1, Integer::sum);
            typeSize.merge(type, size, Long::sum);
        }

        stats.append("\n📋 Por tipo de contenido:\n");
        for (Map.Entry<String, Integer> entry : typeCount.entrySet()) {
            String type = entry.getKey();
            int count = entry.getValue();
            long size = typeSize.get(type);

            stats.append(String.format("   %s: %d entradas (%.1f KB)\n",
                    type, count, size / 1024.0));
        }

        // Memoria utilizada
        long totalSize = cache.values().stream().mapToLong(e -> e.getData().length()).sum();
        stats.append(String.format("\n💾 Memoria utilizada: %.2f MB\n", totalSize / (1024.0 * 1024.0)));

        return stats.toString();
    }

    // =================== MÉTODOS HELPER PARA CACHE ===================

    private String getFromCache(String cacheKey, ServerCall serverCall) {
        // 1. Verificar cache local primero
        CacheEntry entry = cache.get(cacheKey);
        if (entry != null && !entry.isExpired(CACHE_TTL_MS)) {
            logger.debug("Cache HIT para: {}", cacheKey);
            return entry.getData();
        }

        // 2. Cache miss - consultar servidor
        try {
            logger.debug("Cache MISS para: {} - consultando servidor", cacheKey);
            String result = serverCall.call();

            // 3. Guardar en cache local
            cache.put(cacheKey, new CacheEntry(result, System.currentTimeMillis()));

            logger.info("Resultado cacheado para: {}", cacheKey);
            return result;

        } catch (Exception e) {
            logger.error("Error consultando servidor para {}: {}", cacheKey, e.getMessage());

            // 4. Fallback: devolver cache expirado si existe
            if (entry != null) {
                logger.warn("Usando cache expirado como fallback para: {}", cacheKey);
                return entry.getData();
            }

            return "ERROR-No se pudo obtener el reporte-" + System.currentTimeMillis();
        }
    }

    private String generateCacheKey(String type, String id1, String id2) {
        return type + "_" + id1 + "_" + id2;
    }

    // =================== MÉTODOS DE PRECARGA ===================

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
            logger.info("Cache limpiado: {} entradas expiradas removidas", cleaned);
        }
    }

    /**
     * Limpiar todo el cache
     */
    public void clearCache() {
        cache.clear();
        logger.info("Cache completamente limpiado");
    }



    @Override
    public String[] getDepartmentCitizenDocuments(int departmentId, int electionId, Current current) {
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


}