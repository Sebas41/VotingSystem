package org.votaciones;

import ReportsSystem.ReportsService;
import ReportsSystem.ReportsServicePrx;
import com.zeroc.Ice.*;
import com.zeroc.Ice.Exception;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Servidor Proxy Cache para Reports
 * Se conecta al servidor principal en 9001 y expone el proxy en 9999
 */
public class ProxyCacheServer {

    private static final Logger logger = LoggerFactory.getLogger(ProxyCacheServer.class);
    private static ScheduledExecutorService scheduler;

    public static void main(String[] args) {
        List<String> params = new ArrayList<>();

        try (Communicator communicator = Util.initialize(args, params)) {

            System.out.println("🚀 ========== SERVIDOR PROXY CACHE REPORTS ==========");
            System.out.println("🔌 Conectando al servidor principal...");

            // =================== CONEXIÓN AL SERVIDOR PRINCIPAL ===================

            // Conectar al servidor Reports principal en puerto 9001
            ObjectPrx base = communicator.stringToProxy("ReportsManager:default -h localhost -p 9001");
            ReportsServicePrx reportsServer = ReportsServicePrx.checkedCast(base);

            if (reportsServer == null) {
                System.err.println("❌ Error: No se pudo conectar al servidor Reports en puerto 9001");
                System.err.println("💡 Asegúrate de que el servidor principal esté ejecutándose");
                return;
            }

            System.out.println("✅ Conectado al servidor Reports principal");

            // =================== CONFIGURACIÓN DEL PROXY CACHE ===================

            // Crear adapter para el proxy cache en puerto 9999
            ObjectAdapter adapter = communicator.createObjectAdapterWithEndpoints(
                    "ProxyCacheAdapter", "default -h localhost -p 9999"
            );

            // Crear instancia del proxy cache
            ProxyCacheReports proxyCache = new ProxyCacheReports(reportsServer);

            // Registrar el proxy cache
            adapter.add((ReportsService) proxyCache, Util.stringToIdentity("ProxyCacheReports"));

            // =================== ACTIVACIÓN DEL PROXY ===================

            adapter.activate();

            System.out.println("✅ ========== PROXY CACHE INICIADO ==========");
            System.out.println("🔧 Proxy Cache: localhost:9999");
            System.out.println("🔗 Servidor Backend: localhost:9001");
            System.out.println("💾 Cache TTL: 5 minutos");
            System.out.println("⚡ Funcionalidades:");
            System.out.println("   • Cache local inteligente");
            System.out.println("   • Precarga geográfica");
            System.out.println("   • Fallback a cache expirado");
            System.out.println("   • Estadísticas detalladas");
            System.out.println();

            // =================== LIMPIEZA AUTOMÁTICA DEL CACHE ===================

            scheduler = Executors.newScheduledThreadPool(1);

            // Limpiar cache expirado cada 2 minutos
            scheduler.scheduleAtFixedRate(() -> {
                try {
                    proxyCache.cleanExpiredCache();
                } catch (Exception e) {
                    logger.error("Error durante limpieza automática del cache", e);
                }
            }, 2, 2, TimeUnit.MINUTES);

            System.out.println("🧹 Limpieza automática del cache: cada 2 minutos");
            System.out.println("⏳ Esperando conexiones de clientes en puerto 9999...");
            System.out.println("================================================");
            System.out.println();

            // =================== PRUEBA DE CONECTIVIDAD ===================

            // Hacer una prueba de conectividad al servidor principal
            try {
                String[] elections = reportsServer.getAvailableElections();
                System.out.println("🔍 Prueba de conectividad exitosa");
                System.out.println("📊 Elecciones disponibles: " + elections.length);
            } catch (Exception e) {
                System.err.println("⚠️ Advertencia: Error en prueba de conectividad: " + e.getMessage());
            }

            // =================== ESPERA Y SHUTDOWN ===================

            communicator.waitForShutdown();

            System.out.println("\n🛑 Cerrando Servidor Proxy Cache...");

            // Limpiar scheduler
            if (scheduler != null && !scheduler.isShutdown()) {
                scheduler.shutdown();
                try {
                    if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                        scheduler.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    scheduler.shutdownNow();
                }
            }

            System.out.println("👋 Proxy Cache finalizado");

        } catch (LocalException e) {
            System.err.println("❌ Error de Ice en proxy cache: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } catch (Exception e) {
            System.err.println("❌ Error general en proxy cache: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } finally {
            // Cleanup final del scheduler
            if (scheduler != null && !scheduler.isShutdown()) {
                scheduler.shutdownNow();
            }
        }
    }
}