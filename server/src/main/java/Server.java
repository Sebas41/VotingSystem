import ConnectionDB.ConnectionDBinterface;
// ✅ CORRECCIÓN: Solo importar la implementación correcta
import ConnectionDB.ConnectionDB;  // Esta es tu implementación actual
import VotingReciever.VotingReceiverImp;
import Reports.ReportsManagerImpl;

import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.ObjectAdapter;
import com.zeroc.Ice.Util;
import Controller.ServerControllerImpl;
import Controller.ServerControllerInterface;
import ServerUI.ServerUI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;

/**
 * Main server class for Electoral System
 * Provides both VotingReceiver and ReportsManager services
 * Uses separate adapters for better service isolation and scalability
 */
public class Server {

    private static final Logger logger = LoggerFactory.getLogger(Server.class);

    // Service instances for cleanup
    private static ReportsManagerImpl reportsManager;
    private static ConnectionDBinterface connectionDB;
    private static Communicator communicator;

    public static void main(String[] args) {
        try {
            logger.info("Starting Electoral Server...");
            System.out.println("Starting Electoral Server...");

            // =================== INITIALIZATION ===================

            // ✅ CORRECCIÓN: Instanciar correctamente la base de datos
            logger.info("Initializing database connection...");
            connectionDB = new ConnectionDB(); // Tu implementación actual

            // ✅ MEJORADO: Test de conectividad más robusto
            if (!connectionDB.isHealthy()) {
                logger.error("Database connection failed!");
                System.err.println("ERROR: Database connection failed!");

                // ✅ AGREGAR: Mostrar estadísticas de pool para debugging
                if (connectionDB instanceof ConnectionDB) {
                    String poolStats = ((ConnectionDB) connectionDB).getPoolStats();
                    logger.error("Pool status: {}", poolStats);
                    System.err.println("Pool status: " + poolStats);
                }

                System.exit(1);
            }

            logger.info("Database connection established successfully");
            System.out.println("✅ Database connection established");

            // ✅ AGREGAR: Mostrar métricas de la base de datos
            try {
                java.util.Map<String, Object> metrics = connectionDB.getPerformanceMetrics();
                logger.info("Database metrics: {}", metrics);
                System.out.println("📊 Database metrics loaded: " + metrics.get("total_citizens") + " citizens, " +
                        metrics.get("total_mesas") + " mesas");
            } catch (Exception e) {
                logger.warn("Could not load database metrics: {}", e.getMessage());
            }

            // 2. Create server controller
            ServerControllerInterface controller = new ServerControllerImpl();
            logger.info("Server controller initialized");

            // 3. Initialize ReportsManager
            logger.info("Initializing ReportsManager...");
            reportsManager = new ReportsManagerImpl(connectionDB);

            // Perform system test
            java.util.Map<String, Object> testResults = reportsManager.performSystemTest(1);
            boolean allTestsPassed = (Boolean) testResults.getOrDefault("allTestsPassed", false);

            if (!allTestsPassed) {
                logger.warn("ReportsManager system test failed: {}", testResults.get("errors"));
                System.out.println("⚠️  ReportsManager tests failed - check logs");
                // ✅ MEJORADO: Mostrar errores específicos
                @SuppressWarnings("unchecked")
                java.util.List<String> errors = (java.util.List<String>) testResults.get("errors");
                if (errors != null) {
                    for (String error : errors) {
                        System.out.println("   ❌ " + error);
                    }
                }
            } else {
                logger.info("ReportsManager system test passed");
                System.out.println("✅ ReportsManager initialized and tested");
            }

            // 4. Launch UI in separate thread
            SwingUtilities.invokeLater(() -> {
                try {
                    ServerUI.launchUI(controller);
                    logger.info("Server UI launched successfully");
                } catch (Exception e) {
                    logger.error("Failed to launch server UI", e);
                }
            });

            // =================== ICE INITIALIZATION ===================

            logger.info("Initializing Ice middleware...");
            communicator = Util.initialize(args);

            // =================== SERVICE ADAPTERS ===================

            // 1. VotingReceiver Service (original service)
            logger.info("Creating VotingReceiver adapter on port 10012...");
            ObjectAdapter votingAdapter = communicator.createObjectAdapterWithEndpoints(
                    "VotingReceiverAdapter", "tcp -h localhost -p 10012"
            );

            VotingReceiverImp votingReceiver = new VotingReceiverImp(controller);
            votingAdapter.add(votingReceiver, Util.stringToIdentity("Service"));
            votingAdapter.activate();

            logger.info("VotingReceiver service activated on port 10012");
            System.out.println("🗳️  VotingReceiver service: tcp://localhost:10012");

            // 2. ReportsManager Service (new service for proxy cache)
            logger.info("Creating ReportsManager adapter on port 9090...");
            ObjectAdapter reportsAdapter = communicator.createObjectAdapterWithEndpoints(
                    "ReportsManagerAdapter", "tcp -h localhost -p 9090"
            );

            reportsAdapter.add(reportsManager, Util.stringToIdentity("ReportsManager"));
            reportsAdapter.activate();

            logger.info("ReportsManager service activated on port 9090");
            System.out.println("📊 ReportsManager service: tcp://localhost:9090");

            // =================== SHUTDOWN HOOK ===================

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Shutdown signal received, cleaning up...");
                System.out.println("\nShutting down Electoral Server...");

                try {
                    if (reportsManager != null) {
                        reportsManager.shutdown();
                        logger.info("ReportsManager shutdown completed");
                    }

                    // ✅ AGREGAR: Cerrar correctamente la conexión DB
                    if (connectionDB != null) {
                        // Si tu ConnectionDB tiene un método close, úsalo
                        if (connectionDB instanceof ConnectionDB) {
                            // ConnectionDB.shutdown() para cerrar el pool HikariCP
                            ConnectionDB.shutdown();
                            logger.info("Database connection pool closed");
                        }
                    }

                    System.out.println("✅ Cleanup completed successfully");

                } catch (Exception e) {
                    logger.error("Error during shutdown cleanup", e);
                    System.err.println("❌ Error during cleanup: " + e.getMessage());
                }
            }));

            // =================== SERVER READY ===================

            System.out.println();
            System.out.println("===============================================");
            System.out.println("        Electoral Server - READY");
            System.out.println("===============================================");
            System.out.println("  VotingReceiver:  tcp://localhost:10012");
            System.out.println("    Service ID:    Service");
            System.out.println();
            System.out.println("  ReportsManager:  tcp://localhost:9090");
            System.out.println("    Service ID:    ReportsManager");
            System.out.println();
            System.out.println("  Database:        Connected ✅");

            // ✅ AGREGAR: Mostrar información adicional de la BD
            try {
                if (connectionDB instanceof ConnectionDB) {
                    String poolInfo = ((ConnectionDB) connectionDB).getPoolStats();
                    System.out.println("  Pool Status:     " + poolInfo);
                }
            } catch (Exception e) {
                System.out.println("  Pool Status:     " + e.getMessage());
            }

            System.out.println("  UI:              Running ✅");
            System.out.println("===============================================");
            System.out.println();
            System.out.println("🚀 Server is ready to accept connections");
            System.out.println("📋 Check logs for detailed information");
            System.out.println("🛑 Press Ctrl+C to stop the server");
            System.out.println();

            logger.info("Electoral Server fully operational - waiting for connections");

            // =================== WAIT FOR SHUTDOWN ===================

            communicator.waitForShutdown();

        } catch (Exception e) {
            logger.error("FATAL ERROR starting Electoral Server", e);
            System.err.println("💥 FATAL ERROR starting Electoral Server:");
            e.printStackTrace();
            System.exit(1);

        } finally {
            // Final cleanup
            if (communicator != null) {
                try {
                    communicator.destroy();
                    logger.info("Ice communicator destroyed");
                } catch (Exception e) {
                    logger.error("Error destroying Ice communicator", e);
                    System.err.println("Error destroying Ice communicator: " + e.getMessage());
                }
            }
        }

        logger.info("Electoral Server stopped gracefully");
        System.out.println("Electoral Server stopped");
    }

    /**
     * ✅ MEJORADO: Mejor método de status con información de BD
     */
    public static java.util.Map<String, Object> getServerStatus() {
        java.util.Map<String, Object> status = new java.util.HashMap<>();

        try {
            status.put("timestamp", new java.util.Date());
            status.put("communicatorActive", communicator != null && !communicator.isShutdown());
            status.put("databaseHealthy", connectionDB != null && connectionDB.isHealthy());
            status.put("reportsManagerActive", reportsManager != null);

            if (reportsManager != null) {
                status.put("reportsStatistics", reportsManager.getReportsStatistics(1));
            }

            // ✅ AGREGAR: Métricas de base de datos
            if (connectionDB != null) {
                try {
                    status.put("databaseMetrics", connectionDB.getPerformanceMetrics());

                    if (connectionDB instanceof ConnectionDB) {
                        status.put("poolStatus", ((ConnectionDB) connectionDB).getPoolStats());
                    }
                } catch (Exception e) {
                    status.put("databaseError", e.getMessage());
                }
            }

        } catch (Exception e) {
            status.put("error", "Failed to get status: " + e.getMessage());
        }

        return status;
    }

    /**
     * Force a graceful shutdown (for testing or administrative purposes)
     */
    public static void forceShutdown() {
        if (communicator != null) {
            communicator.shutdown();
        }
    }

    /**
     * ✅ AGREGAR: Método para obtener estadísticas de la base de datos
     */
    public static java.util.Map<String, Object> getDatabaseStatus() {
        java.util.Map<String, Object> dbStatus = new java.util.HashMap<>();

        try {
            if (connectionDB != null) {
                dbStatus.put("healthy", connectionDB.isHealthy());
                dbStatus.put("metrics", connectionDB.getPerformanceMetrics());

                if (connectionDB instanceof ConnectionDB) {
                    dbStatus.put("poolStats", ((ConnectionDB) connectionDB).getPoolStats());
                }
            } else {
                dbStatus.put("error", "Database connection not initialized");
            }
        } catch (Exception e) {
            dbStatus.put("error", "Failed to get database status: " + e.getMessage());
        }

        return dbStatus;
    }
}