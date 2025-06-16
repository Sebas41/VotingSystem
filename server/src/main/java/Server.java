package org.votaciones;

import java.util.ArrayList;
import java.util.List;
import ServerUI.ServerUI;

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
import Controller.ServerControllerImpl.ElectionResult;

import VotingReciever.VotingReceiverImp;
import com.zeroc.Ice.Exception;
import reliableMessage.RMDestination;
import configuration.ConfigurationSender;
import Elections.models.ELECTION_STATUS;

public class Server {

    private static ServerControllerImpl electoralController;
    private static ConfigurationSender configurationSender;
    private static ReportsManagerImpl reportsManager;
    private static VotingManagerImpl votingManager;
    private static VoteNotifierImpl voteNotifier;

    private static final int MESA_6823_ID = 6823;
    private static final int MESA_6823_PORT = 10843;

    public static ServerControllerImpl getElectoralController() {
        return electoralController;
    }

    public static ConfigurationSender getConfigurationSender() {
        return configurationSender;
    }

    public static VoteNotifierImpl getVoteNotifier() {
        return voteNotifier;
    }

    public static void main(String[] args) {
        List<String> params = new ArrayList<>();

        try (Communicator communicator = Util.initialize(args, "electoralserver.cfg", params)) {

            electoralController = new ServerControllerImpl();

            showSystemInitializationStatus();

            ObjectAdapter reportsAdapter = communicator.createObjectAdapter("ReportsServer");
            ObjectAdapter votingAdapter = communicator.createObjectAdapter("VotingServer");
            ObjectAdapter notifierAdapter = communicator.createObjectAdapter("VoteNotifierServer");
            ObjectAdapter votingReceiverAdapter = communicator.createObjectAdapterWithEndpoints(
                    "VotingReceiverServer", "tcp -h 192.168.131.101 -p 10012"
            );

            ConnectionDBinterface connectionDB = new ConnectionDB();
            reportsManager = new ReportsManagerImpl(connectionDB);
            reportsAdapter.add((ReportsService) reportsManager, Util.stringToIdentity("ReportsManager"));

            votingManager = new VotingManagerImpl(connectionDB);
            votingAdapter.add((ConfigurationService) votingManager, Util.stringToIdentity("ConfigurationManager"));

            configurationSender = new ConfigurationSender(votingManager, communicator);

            electoralController.setConfigurationSender(configurationSender);

            showRegisteredMesas();

            voteNotifier = new VoteNotifierImpl();
            notifierAdapter.add((VoteNotifier) voteNotifier, Util.stringToIdentity("VoteNotifier"));

            electoralController.setVoteNotifier(voteNotifier);

            VotingReceiverImp votingReceiver = new VotingReceiverImp(electoralController);
            votingReceiverAdapter.add((RMDestination) votingReceiver, Util.stringToIdentity("Service"));

            reportsAdapter.activate();
            votingAdapter.activate();
            notifierAdapter.activate();
            votingReceiverAdapter.activate();

            ServerUI.launchUI(electoralController);

            ElectionResult systemStatus = electoralController.getSystemStatus();
            if (systemStatus.isSuccess()) {
                System.out.println("Sistema inicializado correctamente");
                System.out.println("Información disponible en systemStatus.getData()");
            } else {
                System.out.println("Advertencias en inicialización: " + systemStatus.getMessage());
            }

            System.out.println("\nIniciando pruebas automáticas en 10 segundos...");
            System.out.println("Para pruebas completas, ejecuta el cliente: java -jar client/build/libs/client.jar");

            new Thread(() -> {
                try {
                    Thread.sleep(10000);
                    runAutomaticTests();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();

            System.out.println("\nServidor listo - Esperando solicitudes de clientes...");

            communicator.waitForShutdown();

            electoralController.shutdown();

        } catch (LocalException e) {
            e.printStackTrace();
            System.exit(1);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static ElectionResult createElection(String name, java.util.Date startDate, java.util.Date endDate) {
        if (electoralController == null) {
            return ServerControllerImpl.ElectionResult.error("Controller no inicializado");
        }
        return electoralController.createElection(name, startDate, endDate);
    }

    public static ElectionResult addCandidate(int electionId, String name, String party) {
        if (electoralController == null) {
            return ServerControllerImpl.ElectionResult.error("Controller no inicializado");
        }
        return electoralController.addCandidate(electionId, name, party);
    }

    public static ElectionResult startVoting(int electionId) {
        if (electoralController == null) {
            return ServerControllerImpl.ElectionResult.error("Controller no inicializado");
        }
        return electoralController.startVoting(electionId);
    }

    public static ElectionResult stopVoting(int electionId) {
        if (electoralController == null) {
            return ServerControllerImpl.ElectionResult.error("Controller no inicializado");
        }
        return electoralController.stopVoting(electionId);
    }

    public static ElectionResult getSystemStatus() {
        if (electoralController == null) {
            return ServerControllerImpl.ElectionResult.error("Controller no inicializado");
        }
        return electoralController.getSystemStatus();
    }

    public static ElectionResult runSystemDiagnostic() {
        if (electoralController == null) {
            return ServerControllerImpl.ElectionResult.error("Controller no inicializado");
        }
        return electoralController.runSystemDiagnostic();
    }

    public static ElectionResult sendConfigurationToMesa(int mesaId, int electionId) {
        if (electoralController == null) {
            return ServerControllerImpl.ElectionResult.error("Controller no inicializado");
        }
        return electoralController.sendConfigurationToMesa(mesaId, electionId);
    }

    public static ElectionResult getCitizenReport(String documento, int electionId) {
        if (electoralController == null) {
            return ServerControllerImpl.ElectionResult.error("Controller no inicializado");
        }
        return electoralController.getCitizenReport(documento, electionId);
    }

    public static ElectionResult getElectionResults(int electionId) {
        if (electoralController == null) {
            return ServerControllerImpl.ElectionResult.error("Controller no inicializado");
        }
        return electoralController.getElectionResults(electionId);
    }

    public static boolean testConnectivityMesa6823() {
        if (configurationSender == null) {
            return false;
        }

        try {
            ElectionResult result = electoralController.sendConfigurationToMesa(MESA_6823_ID, 1);

            if (result.isSuccess()) {
                System.out.println("Mesa " + MESA_6823_ID + " conectada y configurada");
                return true;
            } else {
                System.out.println("Mesa " + MESA_6823_ID + " no disponible: " + result.getMessage());
                return false;
            }
        } catch (Exception e) {
            System.err.println("Error conectando: " + e.getMessage());
            return false;
        }
    }

    public static boolean testStartElectionMesa6823() {
        System.out.println("Iniciando elección en mesa " + MESA_6823_ID + " (sistema registrado)...");
        return configurationSender != null && configurationSender.startElectionInAllMachines(1);
    }

    public static boolean testCloseElectionMesa6823() {
        System.out.println("Cerrando elección en mesa " + MESA_6823_ID + " (sistema registrado)...");
        return configurationSender != null && configurationSender.closeElectionInAllMachines(1);
    }

    public static boolean testResetElectionMesa6823() {
        System.out.println("Reseteando elección en mesa " + MESA_6823_ID + " (sistema registrado)...");
        return configurationSender != null && configurationSender.resetElectionInAllMachines(1);
    }

    private static void showSystemInitializationStatus() {
        System.out.println("\nEstado de inicialización:");

        try {
            ElectionResult status = electoralController.getSystemStatus();
            if (status.isSuccess()) {
                System.out.println("Base de datos: CONECTADA");
                System.out.println("Pool de conexiones: ACTIVO");
                System.out.println("Elección de prueba: CONFIGURADA");
                System.out.println("Candidatos de prueba: CARGADOS");
            } else {
                System.out.println("Advertencias durante inicialización");
            }
        } catch (Exception e) {
            System.out.println("No se pudo obtener estado inicial: " + e.getMessage());
        }
    }

    private static void runAutomaticTests() {
        System.out.println("\nPRUEBAS AUTOMATICAS");

        try {
            System.out.println("\nPrueba de conectividad con mesa " + MESA_6823_ID + ":");
            boolean connectivity = testConnectivityMesa6823();
            System.out.println("Resultado: " + (connectivity ? "CONECTADA" : "NO CONECTADA"));

            System.out.println("\nDiagnóstico del sistema:");
            ElectionResult diagnostic = electoralController.runSystemDiagnostic();
            System.out.println("Estado: " + (diagnostic.isSuccess() ? "SALUDABLE" : "CON PROBLEMAS"));
            if (!diagnostic.isSuccess()) {
                System.out.println("Mensaje: " + diagnostic.getMessage());
            }

            System.out.println("\nEstado del sistema:");
            ElectionResult systemStatus = electoralController.getSystemStatus();
            if (systemStatus.isSuccess()) {
                System.out.println("Sistema operativo y listo");
                System.out.println("Métricas disponibles en systemStatus.getData()");
            }

            if (connectivity) {
                System.out.println("\nPruebas de cambio de estado en mesa " + MESA_6823_ID + ":");

                boolean startResult = testStartElectionMesa6823();
                System.out.println("Iniciar elección: " + (startResult ? "ÉXITO" : "ERROR"));
                Thread.sleep(3000);

                boolean stopResult = testCloseElectionMesa6823();
                System.out.println("Cerrar elección: " + (stopResult ? "ÉXITO" : "ERROR"));
                Thread.sleep(3000);

                boolean resetResult = testResetElectionMesa6823();
                System.out.println("Reset elección: " + (resetResult ? "ÉXITO" : "ERROR"));

                int successCount = (startResult ? 1 : 0) + (stopResult ? 1 : 0) + (resetResult ? 1 : 0);
                System.out.println("\nResumen: " + successCount + "/3 pruebas exitosas");

                if (successCount == 3) {
                    System.out.println("Comunicación servidor-cliente funcionando");
                } else if (successCount > 0) {
                    System.out.println("Funcionamiento parcial - revisar logs");
                } else {
                    System.out.println("Comunicación fallida - verificar cliente");
                }
            }

            System.out.println("\n------ PRUEBAS AUTOMÁTICAS COMPLETADAS -----------");
            System.out.println("Para más pruebas, usa los métodos estáticos del Server");
            System.out.println("Ejemplo: Server.getSystemStatus()");

        } catch (Exception e) {
            System.err.println("Error en pruebas automáticas: " + e.getMessage());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean sendElectionStatusToMesa6823(String newStatus) {
        if (configurationSender == null) {
            System.err.println("ConfigurationSender no disponible");
            return false;
        }

        try {
            String endpoint = "ConfigurationReceiver:default -h 192.168.131.102 -p " + MESA_6823_PORT;
            ObjectPrx base = configurationSender.communicator.stringToProxy(endpoint);
            ConfigurationSystem.ConfigurationReceiverPrx receiver =
                    ConfigurationSystem.ConfigurationReceiverPrx.checkedCast(base);

            if (receiver != null && receiver.isReady(MESA_6823_ID)) {
                return receiver.updateElectionStatus(1, newStatus);
            }
            return false;
        } catch (Exception e) {
            System.err.println("Error enviando estado: " + e.getMessage());
            return false;
        }
    }

    public static boolean startElectionInAllMachines() {
        ElectionResult result = startVoting(1);
        System.out.println(result.getMessage());
        return result.isSuccess();
    }

    public static boolean closeElectionInAllMachines() {
        ElectionResult result = stopVoting(1);
        System.out.println(result.getMessage());
        return result.isSuccess();
    }

    public static boolean resetElectionInAllMachines() {
        ElectionResult result = electoralController.resetVoting(1);
        System.out.println(result.getMessage());
        return result.isSuccess();
    }

    public static void showSystemInfo() {
        System.out.println("\n-------------- INFORMACIÓN DEL SISTEMA ------------");

        ElectionResult status = getSystemStatus();
        if (status.isSuccess()) {
            System.out.println("\nMesa de prueba: " + MESA_6823_ID + " (Puerto " + MESA_6823_PORT + ")");
        } else {
            System.out.println("Sistema con problemas: " + status.getMessage());
        }
    }

    public static void showRegisteredMesas() {
        System.out.println("\n--------------- MESAS REGISTRADAS -------------------");

        if (configurationSender != null) {
            try {
                configurationSender.showRegisteredMesasInfo();
            } catch (Exception e) {
                System.out.println("Error obteniendo información de mesas: " + e.getMessage());
            }
        } else {
            System.out.println("ConfigurationSender no disponible");
        }

        System.out.println("--------------------------------");
    }
}