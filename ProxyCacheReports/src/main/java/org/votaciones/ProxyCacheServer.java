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

import VoteNotification.VoteNotifier;
import VoteNotification.VoteNotifierPrx;
import VoteNotification.VoteObserver;
import VoteNotification.VoteObserverPrx;

public class ProxyCacheServer {

    private static final Logger logger = LoggerFactory.getLogger(ProxyCacheServer.class);
    private static ScheduledExecutorService scheduler;

    private static VoteObserverImpl voteObserver;
    private static ObjectAdapter observerAdapter;

    public static void main(String[] args) {
        List<String> params = new ArrayList<>();

        try (Communicator communicator = Util.initialize(args, params)) {

            System.out.println("========== SERVIDOR PROXY CACHE + OBSERVER ==========");
            System.out.println("Conectando al servidor principal...");

            ObjectPrx base = communicator.stringToProxy("ReportsManager:default -h 192.168.131.21 -p 9001");
            ReportsServicePrx reportsServer = ReportsServicePrx.checkedCast(base);

            if (reportsServer == null) {
                System.err.println("Error: No se pudo conectar al servidor Reports en 192.168.131.21:9001");
                System.err.println("Asegúrate de que el servidor principal esté ejecutándose");
                return;
            }

            System.out.println("Conectado al servidor Reports principal");

            ObjectAdapter adapter = communicator.createObjectAdapterWithEndpoints(
                    "ProxyCacheAdapter", "default -h 192.168.131.23 -p 9999"
            );

            ProxyCacheReports proxyCache = new ProxyCacheReports(reportsServer);

            adapter.add((ReportsService) proxyCache, Util.stringToIdentity("ProxyCacheReports"));

            System.out.println("Configurando sistema Observer...");

            ObjectPrx notifierBase = communicator.stringToProxy("VoteNotifier:default -h 192.168.131.21 -p 9002");
            VoteNotifierPrx voteNotifier = VoteNotifierPrx.checkedCast(notifierBase);

            if (voteNotifier != null) {
                System.out.println("Conectado al VoteNotifier del servidor central");

                observerAdapter = communicator.createObjectAdapterWithEndpoints(
                        "VoteObserverAdapter", "default -h 192.168.131.23"
                );

                voteObserver = new VoteObserverImpl(proxyCache);
                observerAdapter.add((VoteObserver) voteObserver, Util.stringToIdentity("VoteObserver"));
                observerAdapter.activate();
                VoteObserverPrx observerProxy = VoteObserverPrx.uncheckedCast(
                        observerAdapter.createProxy(Util.stringToIdentity("VoteObserver"))
                );

                int electionId = 1;
                voteNotifier.registerObserver(observerProxy, electionId);

                System.out.println("Registrado como observer para elección " + electionId);
                System.out.println("Ahora recibirás notificaciones de votos en tiempo real!");

            } else {
                System.err.println("No se pudo conectar al VoteNotifier");
                System.err.println("El proxy funcionará sin notificaciones de votos");
            }

            adapter.activate();

            System.out.println("\n========== PROXY CACHE + OBSERVER INICIADO ==========");
            System.out.println("    Proxy Cache: 192.168.131.23:9999");
            System.out.println("    Servidor Backend: 192.168.131.21:9001");
            System.out.println("    VoteNotifier: 192.168.131.21:9002");
            System.out.println("    Cache TTL: 5 minutos");
            System.out.println("    Funcionalidades:");
            System.out.println("   - Cache local inteligente");
            System.out.println("   - Precarga geográfica");
            System.out.println("   - Fallback a cache expirado");
            System.out.println("   - Estadísticas detalladas");
            System.out.println("   - Notificaciones de votos en tiempo real");
            System.out.println();

            scheduler = Executors.newScheduledThreadPool(2);

            scheduler.scheduleAtFixedRate(() -> {
                try {
                    proxyCache.cleanExpiredCache();
                } catch (Exception e) {
                    logger.error("Error durante limpieza automática del cache", e);
                }
            }, 2, 2, TimeUnit.MINUTES);

            if (voteObserver != null) {
                scheduler.scheduleAtFixedRate(() -> {
                    try {
                        if (voteObserver.getTotalVotesReceived() > 0) {
                            System.out.println("\n" + getCurrentTime() + " - Estadísticas actuales:");
                            System.out.println("Total votos recibidos: " + voteObserver.getTotalVotesReceived());
                        }
                    } catch (Exception e) {
                        logger.debug("Error mostrando estadísticas de votos: {}", e.getMessage());
                    }
                }, 30, 30, TimeUnit.SECONDS);
            }

            System.out.println("Limpieza automática del cache: cada 2 minutos");
            System.out.println("Estadísticas de votos: cada 30 segundos");
            System.out.println("Esperando conexiones de clientes en puerto 9999...");
            System.out.println("Esperando notificaciones de votos del servidor...");
            System.out.println("================================================");
            System.out.println();

            try {
                String[] elections = reportsServer.getAvailableElections();
                System.out.println("Prueba de conectividad exitosa");
                System.out.println("Elecciones disponibles: " + elections.length);
            } catch (Exception e) {
                System.err.println("Advertencia: Error en prueba de conectividad: " + e.getMessage());
            }

            System.out.println("\nComandos disponibles mientras el proxy está ejecutándose:");
            System.out.println("   - Ctrl+C: Cerrar proxy");
            System.out.println("   - Los votos aparecerán automáticamente aquí");

            communicator.waitForShutdown();

            System.out.println("\nCerrando Servidor Proxy Cache + Observer...");

            if (voteNotifier != null && voteObserver != null) {
                try {
                    VoteObserverPrx observerProxy = VoteObserverPrx.uncheckedCast(
                            observerAdapter.createProxy(Util.stringToIdentity("VoteObserver"))
                    );
                    voteNotifier.unregisterObserver(observerProxy, 1);
                    System.out.println("Observer desregistrado del servidor central");
                } catch (Exception e) {
                    logger.warn("Error desregistrando observer: {}", e.getMessage());
                }
            }

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

            if (proxyCache != null) {
                proxyCache.shutdown();
            }

            System.out.println("Proxy Cache + Observer finalizado");

        } catch (LocalException e) {
            System.err.println("Error de Ice en proxy cache: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Error general en proxy cache: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } finally {
            if (scheduler != null && !scheduler.isShutdown()) {
                scheduler.shutdownNow();
            }
        }
    }

    private static String getCurrentTime() {
        return new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date());
    }
}