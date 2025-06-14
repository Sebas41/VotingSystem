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


public class ProxyCacheServer {

    private static final Logger logger = LoggerFactory.getLogger(ProxyCacheServer.class);

    public static void main(String[] args) {
        List<String> params = new ArrayList<>();

        try (Communicator communicator = Util.initialize(args, "proxycache.cfg", params)) {

            System.out.println("🚀 Iniciando ProxyCache de Reports...");


            System.out.println("🔌 Conectando al servidor principal de Reports...");

            // Conectar al servidor de Reports
            ObjectPrx base = communicator.stringToProxy("ReportsManager:tcp -h localhost -p 9001");
            ReportsServicePrx reportsServer = ReportsServicePrx.checkedCast(base);

            if (reportsServer == null) {
                throw new Error("Proxy inválido para ReportsManager");
            }

            System.out.println("✅ Conectado al servidor principal de Reports");


            System.out.println("💾 Inicializando cache local...");
            ProxyCacheReports proxyCacheReports = new ProxyCacheReports(reportsServer);


            ObjectAdapter adapter = communicator.createObjectAdapter("ProxyCacheServer");
            adapter.add((ReportsService) proxyCacheReports, Util.stringToIdentity("ReportsCacheManager"));
            adapter.activate();


            ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
            scheduler.scheduleAtFixedRate(() -> {
                try {
                    proxyCacheReports.cleanExpiredCache();
                } catch (Exception e) {
                    logger.error("Error durante limpieza automática del cache", e);
                }
            }, 5, 5, TimeUnit.MINUTES); // Limpiar cada 5 minutos


            System.out.println();
            System.out.println("✅ ========== PROXY CACHE REPORTS INICIADO ==========");
            System.out.println("💾 Cache local: ACTIVO");
            System.out.println("🔄 TTL: 5 minutos");
            System.out.println("🧹 Limpieza automática: cada 5 minutos");
            System.out.println("🌐 Puerto: 9003");
            System.out.println("🔗 Identity: ReportsCacheManager");
            System.out.println();
            System.out.println("📡 Servidor principal: localhost:9001");
            System.out.println("⏳ Esperando solicitudes de clientes...");
            System.out.println("=====================================================");
            System.out.println();


            communicator.waitForShutdown();

            System.out.println("🛑 Cerrando ProxyCache de Reports...");
            scheduler.shutdown();

        } catch (LocalException e) {
            System.err.println("❌ Error de Ice en ProxyCache: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } catch (Exception e) {
            System.err.println("❌ Error general en ProxyCache: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}