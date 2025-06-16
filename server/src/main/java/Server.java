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

    // =================== MÉTODOS GENERALIZADOS PARA TODAS LAS MESAS ===================

    /**
     * Prueba conectividad con todas las mesas registradas y activas
     */
    public static boolean testConnectivityAllMesas() {
        if (configurationSender == null) {
            System.out.println("ConfigurationSender no disponible");
            return false;
        }

        List<Integer> activeMesas = getActiveMesaIds();
        if (activeMesas.isEmpty()) {
            System.out.println("No hay mesas activas registradas");
            return false;
        }

        int successCount = 0;
        System.out.println("Probando conectividad con " + activeMesas.size() + " mesas registradas:");

        for (Integer mesaId : activeMesas) {
            try {
                ElectionResult result = electoralController.sendConfigurationToMesa(mesaId, 1);
                if (result.isSuccess()) {
                    System.out.println("✓ Mesa " + mesaId + " conectada y configurada");
                    successCount++;
                } else {
                    System.out.println("✗ Mesa " + mesaId + " no disponible: " + result.getMessage());
                }
            } catch (Exception e) {
                System.out.println("✗ Mesa " + mesaId + " error: " + e.getMessage());
            }

            // Pausa pequeña entre pruebas
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        System.out.println("Conectividad: " + successCount + "/" + activeMesas.size() + " mesas");
        return successCount > 0;
    }

    /**
     * Inicia elección en todas las máquinas registradas
     */
    public static boolean testStartElectionAllMachines() {
        System.out.println("Iniciando elección en todas las mesas registradas...");
        return configurationSender != null && configurationSender.startElectionInAllMachines(1);
    }

    /**
     * Cierra elección en todas las máquinas registradas
     */
    public static boolean testCloseElectionAllMachines() {
        System.out.println("Cerrando elección en todas las mesas registradas...");
        return configurationSender != null && configurationSender.closeElectionInAllMachines(1);
    }

    /**
     * Resetea elección en todas las máquinas registradas
     */
    public static boolean testResetElectionAllMachines() {
        System.out.println("Reseteando elección en todas las mesas registradas...");
        return configurationSender != null && configurationSender.resetElectionInAllMachines(1);
    }

    /**
     * Obtiene lista de IDs de mesas activas
     */
    public static List<Integer> getActiveMesaIds() {
        if (configurationSender != null) {
            return configurationSender.getActiveMesaIds();
        }
        return new ArrayList<>();
    }

    /**
     * Obtiene número de mesas activas registradas
     */
    public static int getActiveMesaCount() {
        if (configurationSender != null) {
            return configurationSender.getActiveMesaCount();
        }
        return 0;
    }

    // =================== MÉTODOS LEGACY (MANTENER COMPATIBILIDAD) ===================

    /**
     * @deprecated Usar testConnectivityAllMesas() en su lugar
     */
    @Deprecated
    public static boolean testConnectivityMesa6823() {
        System.out.println("ADVERTENCIA: Método legacy - use testConnectivityAllMesas()");
        List<Integer> activeMesas = getActiveMesaIds();

        // Buscar si la mesa 6823 está en la lista
        for (Integer mesaId : activeMesas) {
            if (mesaId == 6823) {
                ElectionResult result = electoralController.sendConfigurationToMesa(6823, 1);
                if (result.isSuccess()) {
                    System.out.println("Mesa 6823 conectada y configurada");
                    return true;
                } else {
                    System.out.println("Mesa 6823 no disponible: " + result.getMessage());
                    return false;
                }
            }
        }

        System.out.println("Mesa 6823 no está registrada como activa");
        return false;
    }

    /**
     * @deprecated Usar testStartElectionAllMachines() en su lugar
     */
    @Deprecated
    public static boolean testStartElectionMesa6823() {
        System.out.println("ADVERTENCIA: Método legacy - use testStartElectionAllMachines()");
        return testStartElectionAllMachines();
    }

    /**
     * @deprecated Usar testCloseElectionAllMachines() en su lugar
     */
    @Deprecated
    public static boolean testCloseElectionMesa6823() {
        System.out.println("ADVERTENCIA: Método legacy - use testCloseElectionAllMachines()");
        return testCloseElectionAllMachines();
    }

    /**
     * @deprecated Usar testResetElectionAllMachines() en su lugar
     */
    @Deprecated
    public static boolean testResetElectionMesa6823() {
        System.out.println("ADVERTENCIA: Método legacy - use testResetElectionAllMachines()");
        return testResetElectionAllMachines();
    }

    // =================== MÉTODOS DE INICIALIZACIÓN Y ESTADO ===================

    private static void showSystemInitializationStatus() {
        System.out.println("\n=================== ESTADO DE INICIALIZACIÓN ===================");

        try {
            ElectionResult status = electoralController.getSystemStatus();
            if (status.isSuccess()) {
                System.out.println("✓ Base de datos: CONECTADA");
                System.out.println("✓ Pool de conexiones: ACTIVO");
                System.out.println("✓ Elección de prueba: CONFIGURADA");
                System.out.println("✓ Candidatos de prueba: CARGADOS");
            } else {
                System.out.println("⚠ Advertencias durante inicialización: " + status.getMessage());
            }
        } catch (Exception e) {
            System.out.println("✗ No se pudo obtener estado inicial: " + e.getMessage());
        }

        System.out.println("===============================================================");
    }

    private static void runAutomaticTests() {
        System.out.println("\n=================== INICIANDO PRUEBAS AUTOMÁTICAS ===================");

        try {
            // Mostrar información de mesas registradas
            int mesaCount = getActiveMesaCount();
            System.out.println("Mesas activas registradas: " + mesaCount);

            if (mesaCount == 0) {
                System.out.println("⚠ No hay mesas registradas - saltando pruebas de conectividad");
                return;
            }

            // Prueba de conectividad generalizada
            System.out.println("\n--- Prueba de conectividad con mesas registradas ---");
            boolean connectivity = testConnectivityAllMesas();
            System.out.println("Resultado general: " + (connectivity ? "ALGUNAS CONECTADAS" : "NINGUNA CONECTADA"));

            // Diagnóstico del sistema
            System.out.println("\n--- Diagnóstico del sistema ---");
            ElectionResult diagnostic = electoralController.runSystemDiagnostic();
            System.out.println("Estado: " + (diagnostic.isSuccess() ? "✓ SALUDABLE" : "✗ CON PROBLEMAS"));
            if (!diagnostic.isSuccess()) {
                System.out.println("Mensaje: " + diagnostic.getMessage());
            }

            // Estado del sistema
            System.out.println("\n--- Estado del sistema ---");
            ElectionResult systemStatus = electoralController.getSystemStatus();
            if (systemStatus.isSuccess()) {
                System.out.println("✓ Sistema operativo y listo");
                System.out.println("  Métricas disponibles en systemStatus.getData()");
            }

            // Pruebas de cambio de estado (solo si hay conectividad)
            if (connectivity) {
                System.out.println("\n--- Pruebas de cambio de estado en mesas conectadas ---");

                boolean startResult = testStartElectionAllMachines();
                System.out.println("Iniciar elección: " + (startResult ? "✓ ÉXITO" : "✗ ERROR"));
                Thread.sleep(3000);

                boolean stopResult = testCloseElectionAllMachines();
                System.out.println("Cerrar elección: " + (stopResult ? "✓ ÉXITO" : "✗ ERROR"));
                Thread.sleep(3000);

                boolean resetResult = testResetElectionAllMachines();
                System.out.println("Reset elección: " + (resetResult ? "✓ ÉXITO" : "✗ ERROR"));

                // Resumen
                int successCount = (startResult ? 1 : 0) + (stopResult ? 1 : 0) + (resetResult ? 1 : 0);
                System.out.println("\nResumen cambios de estado: " + successCount + "/3 pruebas exitosas");

                if (successCount == 3) {
                    System.out.println("✓ Comunicación servidor-cliente funcionando perfectamente");
                } else if (successCount > 0) {
                    System.out.println("⚠ Funcionamiento parcial - revisar logs de mesas");
                } else {
                    System.out.println("✗ Comunicación fallida - verificar clientes");
                }
            } else {
                System.out.println("\n⚠ No se ejecutaron pruebas de cambio de estado (sin conectividad)");
            }

        } catch (InterruptedException e) {
            System.out.println("✗ Pruebas interrumpidas");
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            System.err.println("✗ Error en pruebas automáticas: " + e.getMessage());
            e.printStackTrace();
        } finally {
            System.out.println("\n=================== PRUEBAS AUTOMÁTICAS COMPLETADAS ===================");
            System.out.println("Para más pruebas manuales, usa los métodos estáticos del Server:");
            System.out.println("  - Server.testConnectivityAllMesas()");
            System.out.println("  - Server.testStartElectionAllMachines()");
            System.out.println("  - Server.getSystemStatus()");
            System.out.println("=================================================================");
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
        System.out.println("\n=================== INFORMACIÓN DEL SISTEMA ===================");

        ElectionResult status = getSystemStatus();
        if (status.isSuccess()) {
            System.out.println("✓ Sistema funcionando correctamente");

            int mesaCount = getActiveMesaCount();
            List<Integer> mesaIds = getActiveMesaIds();

            System.out.println("📊 Estadísticas de mesas:");
            System.out.println("  - Total mesas activas: " + mesaCount);
            System.out.println("  - IDs registrados: " + mesaIds);

        } else {
            System.out.println("✗ Sistema con problemas: " + status.getMessage());
        }
        System.out.println("============================================================");
    }

    public static void showRegisteredMesas() {
        System.out.println("\n=================== MESAS REGISTRADAS ===================");

        if (configurationSender != null) {
            try {
                configurationSender.showRegisteredMesasInfo();

                // Información adicional
                int activeMesas = getActiveMesaCount();
                List<Integer> mesaIds = getActiveMesaIds();

                System.out.println("\n📈 Resumen:");
                System.out.println("  - Mesas activas: " + activeMesas);
                System.out.println("  - IDs: " + mesaIds);

            } catch (Exception e) {
                System.out.println("✗ Error obteniendo información de mesas: " + e.getMessage());
            }
        } else {
            System.out.println("✗ ConfigurationSender no disponible");
        }

        System.out.println("======================================================");
    }
}