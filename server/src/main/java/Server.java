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
import VotingsSystem.ConfigurationService;
import Controller.ServerControllerImpl;
import Controller.ServerControllerInterface;

import VotingReciever.VotingReceiverImp;
import reliableMessage.RMDestination;

// ✅ IMPORTAR NUESTRO ConfigurationSender
import configuration.ConfigurationSender;

import javax.swing.*;
import com.zeroc.Ice.Exception;

/**
 * Servidor Electoral completo que maneja Reports, Voting, Observer, VotingReceiver y ConfigurationSender
 * Patrón máquina de café con strings formateados + Patrón Observer + Reliable Messaging + Configuración Remota
 */
public class Server {

    // ✅ VARIABLES ESTÁTICAS EXISTENTES
    private static VoteNotifierImpl voteNotifier;
    private static ServerControllerInterface serverController;

    // ✅ VARIABLE ESTÁTICA PARA NUESTRO CONFIGURATION SENDER
    private static ConfigurationSender configurationSender;

    public static VoteNotifierImpl getVoteNotifier() {
        return voteNotifier;
    }

    public static ServerControllerInterface getServerController() {
        return serverController;
    }

    // ✅ GETTER PARA CONFIGURATION SENDER
    public static ConfigurationSender getConfigurationSender() {
        return configurationSender;
    }

    public static void main(String[] args) {
        List<String> params = new ArrayList<>();

        try (Communicator communicator = Util.initialize(args, "electoralserver.cfg", params)) {

            System.out.println("🏛️  Iniciando Servidor Electoral...");
            System.out.println("📊 Configurando servicios Reports, Voting, Observer, VotingReceiver y ConfigurationSender...");

            // =================== CONFIGURACIÓN DE ADAPTERS ===================

            ObjectAdapter reportsAdapter = communicator.createObjectAdapter("ReportsServer");
            ObjectAdapter votingAdapter = communicator.createObjectAdapter("VotingServer");
            ObjectAdapter notifierAdapter = communicator.createObjectAdapter("VoteNotifierServer");

            // ✅ Adapter para VotingReceiver
            ObjectAdapter votingReceiverAdapter = communicator.createObjectAdapterWithEndpoints(
                    "VotingReceiverServer", "tcp -h localhost -p 10012"
            );

            // =================== INICIALIZACIÓN DE DATABASE Y CONTROLLER ===================

            System.out.println("🔌 Conectando a la base de datos...");
            ConnectionDBinterface connectionDB = new ConnectionDB();

            // ✅ Crear el controller del servidor
            System.out.println("🎮 Inicializando Controller del servidor...");
            serverController = new ServerControllerImpl();

            // =================== SERVICIO DE REPORTES ===================

            System.out.println("📈 Configurando servicio de Reports...");
            ReportsManagerImpl reportsManager = new ReportsManagerImpl(connectionDB);
            reportsAdapter.add((ReportsService) reportsManager, Util.stringToIdentity("ReportsManager"));

            // =================== SERVICIO DE VOTACIÓN ===================

            System.out.println("🗳️  Configurando servicio de Voting...");
            VotingManagerImpl votingManager = new VotingManagerImpl(connectionDB);
            votingAdapter.add((ConfigurationService) votingManager, Util.stringToIdentity("ConfigurationManager"));

            // ✅ NUESTRO CONFIGURATION SENDER (SIMPLE)
            System.out.println("📤 Configurando servicio de envío de configuraciones...");
            configurationSender = new ConfigurationSender(votingManager, communicator);

            // =================== SERVICIO DE OBSERVER ===================

            System.out.println("🔔 Configurando servicio de Observer (Notificaciones)...");
            voteNotifier = new VoteNotifierImpl();
            notifierAdapter.add((VoteNotifier) voteNotifier, Util.stringToIdentity("VoteNotifier"));

            ServerControllerImpl.setVoteNotifier(voteNotifier);
            System.out.println("🔗 Controller conectado con VoteNotifier");

            // =================== SERVICIO DE VOTING RECEIVER ===================

            System.out.println("📥 Configurando servicio de VotingReceiver...");
            VotingReceiverImp votingReceiver = new VotingReceiverImp(serverController);
            votingReceiverAdapter.add((RMDestination) votingReceiver, Util.stringToIdentity("Service"));

            // =================== ACTIVACIÓN DE SERVICIOS ===================

            System.out.println("🚀 Activando servicios...");
            reportsAdapter.activate();
            votingAdapter.activate();
            notifierAdapter.activate();
            votingReceiverAdapter.activate();

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
            System.out.println("📤 Servicio ConfigurationSender: ACTIVO");
            System.out.println("   - Función: Envío de configuraciones a mesas de votación");
            System.out.println("   - Mesa objetivo: 6823 (Puerto 10843)");
            System.out.println();
            System.out.println("🔔 Servicio Observer: ACTIVO");
            System.out.println("   - Identity: VoteNotifier");
            System.out.println("   - Función: Notificaciones de votos en tiempo real");
            System.out.println();
            System.out.println("📥 Servicio VotingReceiver: ACTIVO");
            System.out.println("   - Identity: Service");
            System.out.println("   - Puerto: 10012 (tcp -h localhost -p 10012)");
            System.out.println("   - Función: Recepción de votos mediante Reliable Messaging");
            System.out.println();
            System.out.println("🔌 Base de datos: CONECTADA");
            System.out.println("⏳ Esperando solicitudes de clientes...");
            System.out.println("====================================================");
            System.out.println();

            // ✅ NUEVA PRUEBA AUTOMÁTICA DE CONFIGURACIÓN
            System.out.println("🧪 Iniciando prueba automática de configuración...");

            // Lanzar prueba en hilo separado
            new Thread(() -> {
                configurationSender.testSendToMesa6823();
            }).start();

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

    // ✅ MÉTODOS SIMPLIFICADOS PARA USO EXTERNO

    /**
     * Envía configuración a una mesa específica
     */
    public static boolean sendConfigurationToMesa(int mesaId, int electionId) {
        if (configurationSender == null) {
            System.err.println("❌ ConfigurationSender no está inicializado");
            return false;
        }

        System.out.println("📤 Enviando configuración a mesa " + mesaId + " para elección " + electionId);
        return configurationSender.sendConfigurationToMachine(mesaId, electionId);
    }

    /**
     * Verifica el estado de configuración de una mesa
     */
    public static String checkMesaConfigurationStatus(int mesaId) {
        if (configurationSender == null) {
            return "ERROR-ConfigurationSender no inicializado";
        }

        System.out.println("🔍 Verificando estado de mesa " + mesaId);

        try {
            int port = 10020 + (mesaId % 1000);
            String endpoint = "ConfigurationReceiver:default -h localhost -p " + port;

            com.zeroc.Ice.ObjectPrx base = configurationSender.communicator.stringToProxy(endpoint);
            ConfigurationSystem.ConfigurationReceiverPrx receiver =
                    ConfigurationSystem.ConfigurationReceiverPrx.checkedCast(base);

            if (receiver != null) {
                return receiver.getConfigurationStatus(mesaId);
            } else {
                return "ERROR-No se pudo conectar";
            }

        } catch (Exception e) {
            return "ERROR-" + e.getMessage();
        }
    }

    /**
     * Método para pruebas manuales
     */
    public static void testConfiguration() {
        if (configurationSender != null) {
            new Thread(() -> {
                configurationSender.testSendToMesa6823();
            }).start();
        }
    }
}