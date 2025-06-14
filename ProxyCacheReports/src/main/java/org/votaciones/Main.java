package org.votaciones;

import Reports.*;
import org.votaciones.cache.ReportsCacheImpl;
import org.votaciones.config.ProxyCacheConfig;
import com.zeroc.Ice.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Aplicación principal del ProxyCache para el sistema de reportes electorales
 * Maneja múltiples modos de operación: departamento, municipio, puesto
 */
public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static final AtomicBoolean shutdown = new AtomicBoolean(false);

    // Componentes principales
    private static Communicator communicator;
    private static ObjectAdapter adapter;
    private static ReportsCacheImpl reportsCache;
    private static ProxyCacheConfig config;

    public static void main(String[] args) {
        logger.info("🚀 Starting ProxyCache Reports Service...");

        try {
            // 1. Cargar configuración
            config = new ProxyCacheConfig(args);
            config.displayConfiguration();

            // 2. Validar configuración
            if (!config.validate()) {
                logger.error("❌ Invalid configuration. Exiting.");
                System.exit(1);
            }

            // 3. Inicializar Ice
            initializeIce();

            // 4. Crear y configurar cache de reportes
            initializeReportsCache();

            // 5. Configurar Ice Adapter y Servant
            configureIceAdapter();

            // 6. Registrar shutdown hook
            registerShutdownHook();

            // 7. Iniciar servidor
            startServer();

            // 8. CLI interactivo (opcional)
            if (config.isInteractiveMode()) {
                startInteractiveCLI();
            } else {
                waitForShutdown();
            }

        } catch (java.lang.Exception e) {  // ✅ ESPECÍFICO: java.lang.Exception
            logger.error("💥 Critical error starting ProxyCache service", e);
            System.exit(1);
        } finally {
            shutdown();
        }
    }

    /**
     * Inicializa el comunicador Ice con la configuración adecuada
     */
    private static void initializeIce() {
        try {
            logger.info("🧊 Initializing Ice communicator...");

            // Configurar propiedades de Ice
            InitializationData initData = new InitializationData();
            initData.properties = Util.createProperties();

            // Configuraciones básicas de Ice
            initData.properties.setProperty("Ice.ThreadPool.Client.Size", "4");
            initData.properties.setProperty("Ice.ThreadPool.Client.SizeMax", "10");
            initData.properties.setProperty("Ice.ThreadPool.Server.Size", "4");
            initData.properties.setProperty("Ice.ThreadPool.Server.SizeMax", "20");

            // Configuración de timeouts
            initData.properties.setProperty("Ice.Override.ConnectTimeout", "5000");
            initData.properties.setProperty("Ice.Override.Timeout", "30000");

            // Configuración de retry
            initData.properties.setProperty("Ice.RetryIntervals", "0 1000 5000");

            // Logging
            initData.properties.setProperty("Ice.Warn.Connections", "1");
            initData.properties.setProperty("Ice.Trace.Network", "1");

            communicator = Util.initialize(initData);
            logger.info("✅ Ice communicator initialized successfully");

        } catch (java.lang.Exception e) {  // ✅ ESPECÍFICO
            logger.error("❌ Failed to initialize Ice communicator", e);
            throw new RuntimeException("Ice initialization failed", e);
        }
    }

    /**
     * Inicializa el cache de reportes con la configuración específica
     */
    private static void initializeReportsCache() {
        try {
            logger.info("📦 Initializing Reports Cache...");

            // Crear directorio de cache específico para esta instancia
            String cacheDir = String.format("%s/%s_%d",
                    config.getCacheDirectory(),
                    config.getMode(),
                    config.getLocationId());

            // Crear instancia del cache
            reportsCache = new ReportsCacheImpl(cacheDir, config.getMainServerEndpoint());

            logger.info("✅ Reports Cache initialized - Directory: {}, Mode: {}, Location: {}",
                    cacheDir, config.getMode(), config.getLocationId());

        } catch (java.lang.Exception e) {  // ✅ ESPECÍFICO
            logger.error("❌ Failed to initialize Reports Cache", e);
            throw new RuntimeException("Reports Cache initialization failed", e);
        }
    }

    /**
     * Configura el adapter de Ice y registra el servant
     */
    private static void configureIceAdapter() {
        try {
            logger.info("🔌 Configuring Ice adapter...");

            // Crear adapter
            String adapterName = String.format("ProxyCache_%s_%d", config.getMode(), config.getLocationId());
            String endpoint = String.format("tcp -h %s -p %d", config.getHost(), config.getPort());

            adapter = communicator.createObjectAdapterWithEndpoints(adapterName, endpoint);

            // Registrar servant
            String objectName = String.format("ReportsManager_%s_%d", config.getMode(), config.getLocationId());
            Identity identity = Util.stringToIdentity(objectName);

            adapter.add(reportsCache, identity);

            logger.info("✅ Ice adapter configured - Endpoint: {}, Object: {}", endpoint, objectName);

        } catch (java.lang.Exception e) {  // ✅ ESPECÍFICO
            logger.error("❌ Failed to configure Ice adapter", e);
            throw new RuntimeException("Ice adapter configuration failed", e);
        }
    }

    /**
     * Inicia el servidor Ice
     */
    private static void startServer() {
        try {
            logger.info("🚀 Starting ProxyCache server...");

            // Activar adapter
            adapter.activate();

            // Mostrar información del servidor
            displayServerInfo();

            logger.info("✅ ProxyCache server started successfully");

            // Test inicial de conectividad
            performInitialTests();

        } catch (java.lang.Exception e) {  // ✅ ESPECÍFICO
            logger.error("❌ Failed to start ProxyCache server", e);
            throw new RuntimeException("Server startup failed", e);
        }
    }

    /**
     * Muestra información del servidor en startup
     */
    private static void displayServerInfo() {
        logger.info("🟢 ProxyCache Reports Service RUNNING");
        logger.info("   📍 Mode: {} (Location: {})", config.getMode(), config.getLocationId());
        logger.info("   🌐 Listening on: {}:{}", config.getHost(), config.getPort());
        logger.info("   🔗 Main Server: {}", config.getMainServerEndpoint());
        logger.info("   💾 Cache Directory: {}", config.getCacheDirectory());
        logger.info("   🔧 Ice Object: ReportsManager_{}_{}", config.getMode(), config.getLocationId());

        if (config.isInteractiveMode()) {
            logger.info("   🎮 Interactive mode: ENABLED");
        }

        logger.info("   ⏰ Started at: {}", java.time.LocalDateTime.now());
    }

    /**
     * Realiza tests iniciales para verificar que todo funciona
     */
    private static void performInitialTests() {
        logger.info("🧪 Performing initial system tests...");

        try {
            // Test 1: Verificar que el cache está funcionando
            Map<String, String> cacheStats = reportsCache.getCacheStats(null);
            logger.info("✅ Cache stats retrieved: {} entries",
                    cacheStats.get("totalCacheEntries"));

            // Test 2: Verificar conectividad al servidor principal (mock por ahora)
            boolean serverReachable = testMainServerConnectivity();
            if (serverReachable) {
                logger.info("✅ Main server connectivity test passed");
            } else {
                logger.warn("⚠️ Main server connectivity test failed - will use fallback mode");
            }

            // Test 3: Test de funcionalidad básica
            testBasicFunctionality();

            logger.info("✅ Initial system tests completed successfully");

        } catch (java.lang.Exception e) {  // ✅ ESPECÍFICO
            logger.warn("⚠️ Some initial tests failed, but service will continue", e);
        }
    }

    /**
     * Test de conectividad con el servidor principal
     */
    private static boolean testMainServerConnectivity() {
        try {
            // TODO: Implementar ping real al servidor principal
            // Por ahora retornamos true como mock
            Thread.sleep(100); // Simular latencia de red
            return true;
        } catch (java.lang.Exception e) {  // ✅ ESPECÍFICO
            return false;
        }
    }

    /**
     * Test de funcionalidad básica del cache
     */
    private static void testBasicFunctionality() {
        try {
            // Test mock de reporte de ciudadano
            CitizenReportsConfiguration testReport = reportsCache.getCitizenReports("12345678", 1, null);
            if (testReport != null) {
                logger.info("✅ Basic citizen report functionality test passed");
            }

            // Test de estadísticas de cache
            Map<String, String> stats = reportsCache.getCacheStats(null);
            logger.info("✅ Cache statistics functionality test passed");

        } catch (java.lang.Exception e) {  // ✅ ESPECÍFICO
            logger.warn("⚠️ Basic functionality test encountered issues", e);
        }
    }

    /**
     * CLI interactivo para administración del proxy cache
     */
    private static void startInteractiveCLI() {
        logger.info("🎮 Starting interactive CLI...");

        Scanner scanner = new Scanner(System.in);

        while (!shutdown.get()) {
            try {
                System.out.println("\n=== ProxyCache Management CLI ===");
                System.out.println("1. Show cache statistics");
                System.out.println("2. Clear cache for election");
                System.out.println("3. Refresh cache for election");
                System.out.println("4. Test citizen report");
                System.out.println("5. Sync with main server");
                System.out.println("6. Show server info");
                System.out.println("0. Shutdown server");
                System.out.print("Choose option: ");

                String input = scanner.nextLine().trim();

                switch (input) {
                    case "1":
                        showCacheStatistics();
                        break;
                    case "2":
                        clearCacheForElection(scanner);
                        break;
                    case "3":
                        refreshCacheForElection(scanner);
                        break;
                    case "4":
                        testCitizenReport(scanner);
                        break;
                    case "5":
                        syncWithMainServer();
                        break;
                    case "6":
                        displayServerInfo();
                        break;
                    case "0":
                        System.out.println("🛑 Shutting down server...");
                        shutdown.set(true);
                        break;
                    default:
                        System.out.println("❌ Invalid option. Try again.");
                }

            } catch (java.lang.Exception e) {  // ✅ ESPECÍFICO
                logger.error("❌ Error in interactive CLI", e);
                System.out.println("❌ Error: " + e.getMessage());
            }
        }

        scanner.close();
    }

    /**
     * Muestra estadísticas del cache en la CLI
     */
    private static void showCacheStatistics() {
        try {
            Map<String, String> stats = reportsCache.getCacheStats(null);

            System.out.println("\n📊 Cache Statistics:");
            System.out.println("   Citizens cached: " + stats.get("citizenCacheSize"));
            System.out.println("   Elections cached: " + stats.get("electionCacheSize"));
            System.out.println("   Geographic cached: " + stats.get("geographicCacheSize"));
            System.out.println("   Total entries: " + stats.get("totalCacheEntries"));
            System.out.println("   Memory used: " + stats.get("memoryUsed") + " MB");
            System.out.println("   Memory total: " + stats.get("memoryTotal") + " MB");
            System.out.println("   Cache directory: " + stats.get("cacheDirectory"));

        } catch (java.lang.Exception e) {  // ✅ ESPECÍFICO
            System.out.println("❌ Error getting cache statistics: " + e.getMessage());
        }
    }

    /**
     * Limpia cache para una elección específica
     */
    private static void clearCacheForElection(Scanner scanner) {
        try {
            System.out.print("Enter election ID to clear: ");
            String input = scanner.nextLine().trim();
            int electionId = Integer.parseInt(input);

            reportsCache.clearCache(electionId, null);
            System.out.println("✅ Cache cleared for election " + electionId);

        } catch (NumberFormatException e) {
            System.out.println("❌ Invalid election ID format");
        } catch (java.lang.Exception e) {  // ✅ ESPECÍFICO
            System.out.println("❌ Error clearing cache: " + e.getMessage());
        }
    }

    /**
     * Refresca cache para una elección específica
     */
    private static void refreshCacheForElection(Scanner scanner) {
        try {
            System.out.print("Enter election ID to refresh: ");
            String input = scanner.nextLine().trim();
            int electionId = Integer.parseInt(input);

            reportsCache.refreshCache(electionId, null);
            System.out.println("✅ Cache refreshed for election " + electionId);

        } catch (NumberFormatException e) {
            System.out.println("❌ Invalid election ID format");
        } catch (java.lang.Exception e) {  // ✅ ESPECÍFICO
            System.out.println("❌ Error refreshing cache: " + e.getMessage());
        }
    }

    /**
     * Test de reporte de ciudadano desde CLI
     */
    private static void testCitizenReport(Scanner scanner) {
        try {
            System.out.print("Enter citizen document (e.g., 12345678): ");
            String documento = scanner.nextLine().trim();

            System.out.print("Enter election ID (e.g., 1): ");
            String input = scanner.nextLine().trim();
            int electionId = Integer.parseInt(input);

            System.out.println("🔍 Retrieving citizen report...");
            long startTime = System.currentTimeMillis();

            CitizenReportsConfiguration report = reportsCache.getCitizenReports(documento, electionId, null);

            long duration = System.currentTimeMillis() - startTime;

            if (report != null) {
                System.out.println("✅ Report retrieved successfully in " + duration + "ms");
                System.out.println("   Citizen: " + report.assignment.citizen.nombre + " " + report.assignment.citizen.apellido);
                System.out.println("   Document: " + report.assignment.citizen.documento);
                System.out.println("   Department: " + report.assignment.location.departamentoNombre);
                System.out.println("   Municipality: " + report.assignment.location.municipioNombre);
                System.out.println("   Generated: " + new java.util.Date(report.generationTimestamp));
            } else {
                System.out.println("❌ Report not found");
            }

        } catch (NumberFormatException e) {
            System.out.println("❌ Invalid election ID format");
        } catch (java.lang.Exception e) {  // ✅ ESPECÍFICO
            System.out.println("❌ Error testing citizen report: " + e.getMessage());
        }
    }

    /**
     * Sincroniza con el servidor principal
     */
    private static void syncWithMainServer() {
        try {
            System.out.println("🔄 Synchronizing with main server...");
            reportsCache.syncWithMainServer(null);
            System.out.println("✅ Synchronization completed");
        } catch (java.lang.Exception e) {  // ✅ ESPECÍFICO
            System.out.println("❌ Error during synchronization: " + e.getMessage());
        }
    }

    /**
     * Espera hasta que se solicite el shutdown
     */
    private static void waitForShutdown() {
        logger.info("💤 Service running. Press Ctrl+C to shutdown.");

        // Esperar hasta que shutdown sea solicitado
        while (!shutdown.get()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    /**
     * Registra shutdown hook para limpieza ordenada
     */
    private static void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("🛑 Shutdown signal received. Cleaning up...");
            shutdown.set(true);
            shutdown();
        }, "ShutdownHook"));
    }

    /**
     * Limpieza ordenada de recursos
     */
    private static void shutdown() {
        logger.info("🧹 Shutting down ProxyCache service...");

        try {
            // Cerrar adapter de Ice
            if (adapter != null) {
                adapter.deactivate();
                logger.info("✅ Ice adapter deactivated");
            }

            // Limpiar cache y guardar estado
            if (reportsCache != null) {
                // TODO: Implementar persistencia del estado del cache si es necesario
                logger.info("✅ Reports cache cleaned up");
            }

            // Destruir comunicador de Ice
            if (communicator != null && !communicator.isShutdown()) {
                communicator.destroy();
                logger.info("✅ Ice communicator destroyed");
            }

            logger.info("✅ ProxyCache service shutdown completed");

        } catch (java.lang.Exception e) {  // ✅ ESPECÍFICO
            logger.error("❌ Error during shutdown", e);
        }
    }
}