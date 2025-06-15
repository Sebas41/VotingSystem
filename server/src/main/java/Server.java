package org.votaciones;

import java.util.ArrayList;
import java.util.List;

import Reports.VoteNotifierImpl;
import com.zeroc.Ice.*;
import ConnectionDB.ConnectionDB;
import ConnectionDB.ConnectionDBinterface;

import Reports.ReportsManagerImpl;
import ReportsSystem.ReportsService;

import VoteNotification.VoteNotifier;
import VotingMachineManager.VotingManagerImpl;
import VotingsSystem.ConfigurationService; // ✅ CORREGIDO
import Controller.ServerControllerImpl;    // ✅ AGREGADO

import com.zeroc.Ice.Exception;

/**
 * Servidor Electoral completo que maneja Reports, Voting y Observer
 * Patrón máquina de café con strings formateados + Patrón Observer
 */
public class Server {

    // ✅ AGREGADO: Declaración de la variable estática
    private static VoteNotifierImpl voteNotifier;

    public static VoteNotifierImpl getVoteNotifier() {
        return voteNotifier;
    }

    public static void main(String[] args) {
        List<String> params = new ArrayList<>();

        try (Communicator communicator = Util.initialize(args, "electoralserver.cfg", params)) {

            System.out.println("🏛️  Iniciando Servidor Electoral...");
            System.out.println("📊 Configurando servicios Reports, Voting y Observer...");

            // =================== CONFIGURACIÓN DE ADAPTERS ===================

            ObjectAdapter reportsAdapter = communicator.createObjectAdapter("ReportsServer");
            ObjectAdapter votingAdapter = communicator.createObjectAdapter("VotingServer");
            ObjectAdapter notifierAdapter = communicator.createObjectAdapter("VoteNotifierServer");

            // =================== INICIALIZACIÓN DE DATABASE ===================

            System.out.println("🔌 Conectando a la base de datos...");
            ConnectionDBinterface connectionDB = new ConnectionDB();

            // =================== SERVICIO DE REPORTES ===================

            System.out.println("📈 Configurando servicio de Reports...");
            ReportsManagerImpl reportsManager = new ReportsManagerImpl(connectionDB);
            reportsAdapter.add((ReportsService) reportsManager, Util.stringToIdentity("ReportsManager"));

            // =================== SERVICIO DE VOTACIÓN ===================

            System.out.println("🗳️  Configurando servicio de Voting...");
            VotingManagerImpl votingManager = new VotingManagerImpl(connectionDB);
            votingAdapter.add((ConfigurationService) votingManager, Util.stringToIdentity("ConfigurationManager"));

            // =================== SERVICIO DE OBSERVER ===================

            System.out.println("🔔 Configurando servicio de Observer (Notificaciones)...");
            voteNotifier = new VoteNotifierImpl();
            notifierAdapter.add((VoteNotifier) voteNotifier, Util.stringToIdentity("VoteNotifier"));

            ServerControllerImpl.setVoteNotifier(voteNotifier);
            System.out.println("🔗 Controller conectado con VoteNotifier");

            // =================== ACTIVACIÓN DE SERVICIOS ===================

            System.out.println("🚀 Activando servicios...");
            reportsAdapter.activate();
            votingAdapter.activate();
            notifierAdapter.activate();

            // =================== INFORMACIÓN DEL SERVIDOR ===================

            System.out.println();
            System.out.println("✅ ========== SERVIDOR ELECTORAL INICIADO ==========");
            System.out.println("📊 Servicio Reports: ACTIVO");
            System.out.println("   - Identity: ReportsManager");
            System.out.println("   - Formato: Strings formateados (patrón máquina de café)");
            System.out.println();
            System.out.println("🗳️  Servicio Voting: ACTIVO");
            System.out.println("   - Identity: ConfigurationManager");
            System.out.println("   - Formato: Strings formateados (patrón máquina de café)");
            System.out.println();
            System.out.println("🔔 Servicio Observer: ACTIVO");
            System.out.println("   - Identity: VoteNotifier");
            System.out.println("   - Función: Notificaciones de votos en tiempo real");
            System.out.println();
            System.out.println("🔌 Base de datos: CONECTADA");
            System.out.println("⏳ Esperando solicitudes de clientes...");
            System.out.println("====================================================");
            System.out.println();

            // =================== ESPERA Y SHUTDOWN ===================

            communicator.waitForShutdown();

            System.out.println("🛑 Cerrando Servidor Electoral...");

        } catch (LocalException e) {
            System.err.println("❌ Error de Ice en el servidor electoral: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } catch (Exception e) {
            System.err.println("❌ Error general en el servidor electoral: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}