package org.votaciones;

import java.util.ArrayList;
import java.util.List;
// import ServerUI.ServerUI;

import Reports.VoteNotifierImpl;
import com.zeroc.Ice.*;
import ConnectionDB.ConnectionDB;
import ConnectionDB.ConnectionDBinterface;

import Reports.ReportsManagerImpl;
import ReportsSystem.ReportsService;

import VoteNotification.VoteNotifier;
import VotingMachineManager.VotingManagerImpl;
import VotingsSystem.ConfigurationService;

// ✅ CAMBIO PRINCIPAL: Usar el nuevo controller integrado
import Controller.ServerControllerImpl; // Tu ElectoralSystemController renombrado
import Controller.ServerControllerImpl.ElectionResult;

import VotingReciever.VotingReceiverImp;
import com.zeroc.Ice.Exception;
import reliableMessage.RMDestination;
import configuration.ConfigurationSender;
import Elections.models.ELECTION_STATUS;

/**
 * 🏛️ SERVIDOR ELECTORAL MODERNIZADO – VERSIÓN SILENT
 *
 * Eliminadas todas las salidas a consola (System.out / System.err) para que la
 * clase no imprima nada durante su ejecución. La lógica original y la API
 * pública se mantienen sin cambios visibles para los consumidores.
 */
public class Server {

    // =================== COMPONENTES PRINCIPALES ===================
    private static ServerControllerImpl electoralController; // Tu ElectoralSystemController
    private static ConfigurationSender configurationSender;

    // Componentes para compatibilidad Ice
    private static ReportsManagerImpl reportsManager;
    private static VotingManagerImpl votingManager;
    private static VoteNotifierImpl voteNotifier;

    // =================== CONSTANTES ===================
    private static final int MESA_6823_ID = 6823;
    private static final int MESA_6823_PORT = 10843; // 10020 + (6823 % 1000)

    // =================== GETTERS PARA COMPATIBILIDAD ===================

    public static ServerControllerImpl getElectoralController() {
        return electoralController;
    }

    public static ConfigurationSender getConfigurationSender() {
        return configurationSender;
    }

    public static VoteNotifierImpl getVoteNotifier() {
        return voteNotifier;
    }

    // =================== MÉTODO PRINCIPAL ===================

    public static void main(String[] args) {
        List<String> params = new ArrayList<>();

        try (Communicator communicator = Util.initialize(args, "electoralserver.cfg", params)) {

            // =================== INICIALIZACIÓN DEL CONTROLLER PRINCIPAL
            // ===================

            electoralController = new ServerControllerImpl();

            // =================== CONFIGURACIÓN DE ADAPTERS ICE ===================

            ObjectAdapter reportsAdapter = communicator.createObjectAdapter("ReportsServer");
            ObjectAdapter votingAdapter = communicator.createObjectAdapter("VotingServer");
            ObjectAdapter notifierAdapter = communicator.createObjectAdapter("VoteNotifierServer");
            ObjectAdapter votingReceiverAdapter = communicator.createObjectAdapterWithEndpoints(
                    "VotingReceiverServer", "tcp -h localhost -p 10012");

            // =================== SERVICIO DE REPORTES ===================

            ConnectionDBinterface connectionDB = new ConnectionDB();
            reportsManager = new ReportsManagerImpl(connectionDB);
            reportsAdapter.add((ReportsService) reportsManager, Util.stringToIdentity("ReportsManager"));

            // =================== SERVICIO DE VOTACIÓN ===================

            votingManager = new VotingManagerImpl(connectionDB);
            votingAdapter.add((ConfigurationService) votingManager, Util.stringToIdentity("ConfigurationManager"));

            // =================== CONFIGURATION SENDER ===================

            configurationSender = new ConfigurationSender(votingManager, communicator);
            electoralController.setConfigurationSender(configurationSender);

            // =================== SERVICIO DE OBSERVER ===================

            voteNotifier = new VoteNotifierImpl();
            notifierAdapter.add((VoteNotifier) voteNotifier, Util.stringToIdentity("VoteNotifier"));
            electoralController.setVoteNotifier(voteNotifier);

            // =================== SERVICIO DE VOTING RECEIVER ===================

            VotingReceiverImp votingReceiver = new VotingReceiverImp(electoralController);
            votingReceiverAdapter.add((RMDestination) votingReceiver, Util.stringToIdentity("Service"));

            // =================== ACTIVACIÓN DE SERVICIOS ===================

            reportsAdapter.activate();
            votingAdapter.activate();
            notifierAdapter.activate();
            votingReceiverAdapter.activate();

            // =================== ESPERA Y SHUTDOWN ===================

            communicator.waitForShutdown();
            electoralController.shutdown();

        } catch (LocalException e) {
            // Manejo silencioso: relanzar como Runtime para no imprimir.
            throw new RuntimeException("Error de Ice", e);
        } catch (Exception e) {
            throw new RuntimeException("Error general", e);
        }
    }

    // =================== MÉTODOS DE CONTROL SIMPLIFICADOS ===================

    /**
     * ✅ NUEVO: Crea una elección usando la API integrada
     */
    public static ElectionResult createElection(String name, java.util.Date startDate, java.util.Date endDate) {
        if (electoralController == null) {
            return ServerControllerImpl.ElectionResult.error("Controller no inicializado");
        }
        return electoralController.createElection(name, startDate, endDate);
    }

    /**
     * ✅ NUEVO: Agrega un candidato usando la API integrada
     */
    public static ElectionResult addCandidate(int electionId, String name, String party) {
        if (electoralController == null) {
            return ServerControllerImpl.ElectionResult.error("Controller no inicializado");
        }
        return electoralController.addCandidate(electionId, name, party);
    }

    /**
     * ✅ NUEVO: Inicia votación usando la API integrada
     */
    public static ElectionResult startVoting(int electionId) {
        if (electoralController == null) {
            return ServerControllerImpl.ElectionResult.error("Controller no inicializado");
        }
        return electoralController.startVoting(electionId);
    }

    /**
     * ✅ NUEVO: Detiene votación usando la API integrada
     */
    public static ElectionResult stopVoting(int electionId) {
        if (electoralController == null) {
            return ServerControllerImpl.ElectionResult.error("Controller no inicializado");
        }
        return electoralController.stopVoting(electionId);
    }

    /**
     * ✅ NUEVO: Obtiene estado del sistema usando la API integrada
     */
    public static ElectionResult getSystemStatus() {
        if (electoralController == null) {
            return ServerControllerImpl.ElectionResult.error("Controller no inicializado");
        }
        return electoralController.getSystemStatus();
    }

    /**
     * ✅ NUEVO: Ejecuta diagnóstico del sistema usando la API integrada
     */
    public static ElectionResult runSystemDiagnostic() {
        if (electoralController == null) {
            return ServerControllerImpl.ElectionResult.error("Controller no inicializado");
        }
        return electoralController.runSystemDiagnostic();
    }

    /**
     * ✅ NUEVO: Envía configuración a mesa usando la API integrada
     */
    public static ElectionResult sendConfigurationToMesa(int mesaId, int electionId) {
        if (electoralController == null) {
            return ServerControllerImpl.ElectionResult.error("Controller no inicializado");
        }
        return electoralController.sendConfigurationToMesa(mesaId, electionId);
    }

    /**
     * ✅ NUEVO: Obtiene reporte de ciudadano usando la API integrada
     */
    public static ElectionResult getCitizenReport(String documento, int electionId) {
        if (electoralController == null) {
            return ServerControllerImpl.ElectionResult.error("Controller no inicializado");
        }
        return electoralController.getCitizenReport(documento, electionId);
    }

    /**
     * ✅ NUEVO: Obtiene resultados de elección usando la API integrada
     */
    public static ElectionResult getElectionResults(int electionId) {
        if (electoralController == null) {
            return ServerControllerImpl.ElectionResult.error("Controller no inicializado");
        }
        return electoralController.getElectionResults(electionId);
    }

    // =================== MÉTODOS DE COMPATIBILIDAD (LEGACY) ===================

    public static boolean startElectionInAllMachines() {
        ElectionResult result = startVoting(1);
        return result.isSuccess();
    }

    public static boolean closeElectionInAllMachines() {
        ElectionResult result = stopVoting(1);
        return result.isSuccess();
    }

    public static boolean resetElectionInAllMachines() {
        ElectionResult result = electoralController.resetVoting(1);
        return result.isSuccess();
    }

    /**
     * Métodos auxiliares de prueba – conservados sin salidas a consola
     */

    public static boolean testConnectivityMesa6823() {
        if (configurationSender == null) {
            return false;
        }
        try {
            ElectionResult result = electoralController.sendConfigurationToMesa(MESA_6823_ID, 1);
            return result.isSuccess();
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean testStartElectionMesa6823() {
        return configurationSender != null && configurationSender.startElectionInAllMachines(1);
    }

    public static boolean testCloseElectionMesa6823() {
        return configurationSender != null && configurationSender.closeElectionInAllMachines(1);
    }

    public static boolean testResetElectionMesa6823() {
        return configurationSender != null && configurationSender.resetElectionInAllMachines(1);
    }

    // =================== MÉTODOS PRIVADOS ===================

    private static boolean sendElectionStatusToMesa6823(String newStatus) {
        if (configurationSender == null) {
            return false;
        }

        try {
            String endpoint = "ConfigurationReceiver:default -h localhost -p " + MESA_6823_PORT;
            ObjectPrx base = configurationSender.communicator.stringToProxy(endpoint);
            ConfigurationSystem.ConfigurationReceiverPrx receiver = ConfigurationSystem.ConfigurationReceiverPrx
                    .checkedCast(base);

            if (receiver != null && receiver.isReady(MESA_6823_ID)) {
                return receiver.updateElectionStatus(1, newStatus);
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Muestra información del sistema – ahora silencioso
     */
    public static void showSystemInfo() {
        // Método conservado por compatibilidad, sin impresión.
    }

    public static void showRegisteredMesas() {
        if (configurationSender != null) {
            configurationSender.showRegisteredMesasInfo();
        }
    }
}
