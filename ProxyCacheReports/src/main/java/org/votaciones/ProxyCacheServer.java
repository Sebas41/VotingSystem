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

// =================== NUEVO: IMPORTS PARA OBSERVER ===================
import VoteNotification.VoteNotifier;
import VoteNotification.VoteNotifierPrx;
import VoteNotification.VoteObserver;
import VoteNotification.VoteObserverPrx;

/**
 * Servidor Proxy Cache para Reports + Observer de Notificaciones de Votos
 * Se conecta al servidor principal en 9001 y expone el proxy en 9999
 * Ahora también recibe notificaciones de votos en tiempo real
 */
public class ProxyCacheServer {

    private static final Logger logger = LoggerFactory.getLogger(ProxyCacheServer.class);
    private static ScheduledExecutorService scheduler;

    // =================== NUEVO: REFERENCIAS PARA OBSERVER ===================
    private static VoteObserverImpl voteObserver;
    private static ObjectAdapter observerAdapter;

    public static void main(String[] args) {
        List<String> params = new ArrayList<>();

        try (Communicator communicator = Util.initialize(args, params)) {

            System.out.println("🚀 ========== SERVIDOR PROXY CACHE + OBSERVER ==========");
            System.out.println("🔌 Conectando al servidor principal...");

            // =================== CONEXIÓN AL SERVIDOR PRINCIPAL ===================

            // Conectar al servidor Reports principal en puerto 9001
            ObjectPrx base = communicator.stringToProxy("ReportsManager:default -h localhost -p 9001");
            ReportsServicePrx reportsServer = ReportsServicePrx.checkedCast(base);

            if (reportsServer == null) {
                System.err.println(" Error: No se pudo conectar al servidor Reports en puerto 9001");
                System.err.println("💡 Asegúrate de que el servidor principal esté ejecutándose");
                return;
            }

            System.out.println(" Conectado al servidor Reports principal");

            // =================== CONFIGURACIÓN DEL PROXY CACHE ===================

            // Crear adapter para el proxy cache en puerto 9999
            ObjectAdapter adapter = communicator.createObjectAdapterWithEndpoints(
                    "ProxyCacheAdapter", "default -h localhost -p 9999"
            );

            // Crear instancia del proxy cache
            ProxyCacheReports proxyCache = new ProxyCacheReports(reportsServer);

            // Registrar el proxy cache
            adapter.add((ReportsService) proxyCache, Util.stringToIdentity("ProxyCacheReports"));

            // =================== NUEVO: CONFIGURACIÓN DEL OBSERVER ===================

            System.out.println(" Configurando sistema Observer...");

            // Conectar al VoteNotifier del servidor central
            ObjectPrx notifierBase = communicator.stringToProxy("VoteNotifier:default -h localhost -p 9002");
            VoteNotifierPrx voteNotifier = VoteNotifierPrx.checkedCast(notifierBase);

            if (voteNotifier != null) {
                System.out.println(" Conectado al VoteNotifier del servidor central");

                // Crear adapter para el observer (puerto dinámico)
                observerAdapter = communicator.createObjectAdapterWithEndpoints(
                        "VoteObserverAdapter", "default -h localhost"
                );

                // Crear instancia del observer
                voteObserver = new VoteObserverImpl(proxyCache);

                // Registrar el observer en su adapter
                observerAdapter.add((VoteObserver) voteObserver, Util.stringToIdentity("VoteObserver"));

                // Activar adapter del observer
                observerAdapter.activate();

                // Crear proxy del observer para registrarlo en el servidor
                VoteObserverPrx observerProxy = VoteObserverPrx.uncheckedCast(
                        observerAdapter.createProxy(Util.stringToIdentity("VoteObserver"))
                );

                // Registrarse como observer para la elección 1 (puedes cambiar esto)
                int electionId = 1;
                voteNotifier.registerObserver(observerProxy, electionId);

                System.out.println(" Registrado como observer para elección " + electionId);
                System.out.println("📊 Ahora recibirás notificaciones de votos en tiempo real!");

            } else {
                System.err.println(" No se pudo conectar al VoteNotifier");
                System.err.println("💡 El proxy funcionará sin notificaciones de votos");
            }

            // =================== ACTIVACIÓN DEL PROXY ===================

            adapter.activate();

            System.out.println("\n ========== PROXY CACHE + OBSERVER INICIADO ==========");
            System.out.println("🔧 Proxy Cache: localhost:9999");
            System.out.println("🔗 Servidor Backend: localhost:9001");
            System.out.println("📡 VoteNotifier: localhost:9002");
            System.out.println("💾 Cache TTL: 5 minutos");
            System.out.println("⚡ Funcionalidades:");
            System.out.println("   • Cache local inteligente");
            System.out.println("   • Precarga geográfica");
            System.out.println("   • Fallback a cache expirado");
            System.out.println("   • Estadísticas detalladas");
            System.out.println("   •  Notificaciones de votos en tiempo real");
            System.out.println();

            // =================== LIMPIEZA AUTOMÁTICA DEL CACHE ===================

            scheduler = Executors.newScheduledThreadPool(2); // Aumentado a 2 threads

            // Limpiar cache expirado cada 2 minutos
            scheduler.scheduleAtFixedRate(() -> {
                try {
                    proxyCache.cleanExpiredCache();
                } catch (Exception e) {
                    logger.error("Error durante limpieza automática del cache", e);
                }
            }, 2, 2, TimeUnit.MINUTES);

            // NUEVO: Mostrar estadísticas de votos cada 30 segundos
            if (voteObserver != null) {
                scheduler.scheduleAtFixedRate(() -> {
                    try {
                        // Solo mostrar si hay votos
                        if (voteObserver.getTotalVotesReceived() > 0) {
                            System.out.println("\n" + getCurrentTime() + " - Estadísticas actuales:");
                            System.out.println("📊 Total votos recibidos: " + voteObserver.getTotalVotesReceived());
                        }
                    } catch (Exception e) {
                        logger.debug("Error mostrando estadísticas de votos: {}", e.getMessage());
                    }
                }, 30, 30, TimeUnit.SECONDS);
            }

            System.out.println(" Limpieza automática del cache: cada 2 minutos");
            System.out.println(" Estadísticas de votos: cada 30 segundos");
            System.out.println(" Esperando conexiones de clientes en puerto 9999...");
            System.out.println(" Esperando notificaciones de votos del servidor...");
            System.out.println("================================================");
            System.out.println();

            // =================== PRUEBA DE CONECTIVIDAD ===================

            // Hacer una prueba de conectividad al servidor principal
            try {
                String[] elections = reportsServer.getAvailableElections();
                System.out.println("🔍 Prueba de conectividad exitosa");
                System.out.println("📊 Elecciones disponibles: " + elections.length);
            } catch (Exception e) {
                System.err.println(" Advertencia: Error en prueba de conectividad: " + e.getMessage());
            }

            // Mostrar comandos disponibles
            System.out.println("\n💡 Comandos disponibles mientras el proxy está ejecutándose:");
            System.out.println("   • Ctrl+C: Cerrar proxy");
            System.out.println("   • Los votos aparecerán automáticamente aquí");

            // =================== ESPERA Y SHUTDOWN ===================

            communicator.waitForShutdown();

            System.out.println("\n🛑 Cerrando Servidor Proxy Cache + Observer...");

            // Desregistrar observer antes de cerrar
            if (voteNotifier != null && voteObserver != null) {
                try {
                    VoteObserverPrx observerProxy = VoteObserverPrx.uncheckedCast(
                            observerAdapter.createProxy(Util.stringToIdentity("VoteObserver"))
                    );
                    voteNotifier.unregisterObserver(observerProxy, 1); // Elección 1
                    System.out.println("👋 Observer desregistrado del servidor central");
                } catch (Exception e) {
                    logger.warn("Error desregistrando observer: {}", e.getMessage());
                }
            }

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

            // Cerrar proxy cache
            if (proxyCache != null) {
                proxyCache.shutdown();
            }

            System.out.println("👋 Proxy Cache + Observer finalizado");

        } catch (LocalException e) {
            System.err.println(" Error de Ice en proxy cache: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } catch (Exception e) {
            System.err.println(" Error general en proxy cache: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } finally {
            // Cleanup final del scheduler
            if (scheduler != null && !scheduler.isShutdown()) {
                scheduler.shutdownNow();
            }
        }
    }

    /**
     * Helper para obtener timestamp actual
     */
    private static String getCurrentTime() {
        return new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date());
    }
}