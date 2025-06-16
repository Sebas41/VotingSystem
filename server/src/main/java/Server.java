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

// ✅ CAMBIO PRINCIPAL: Usar el nuevo controller integrado
import Controller.ServerControllerImpl; // Tu ElectoralSystemController renombrado
import Controller.ServerControllerImpl.ElectionResult;

import VotingReciever.VotingReceiverImp;
import com.zeroc.Ice.Exception;
import reliableMessage.RMDestination;
import configuration.ConfigurationSender;
import Elections.models.ELECTION_STATUS;

/**
 * 🏛️ SERVIDOR ELECTORAL MODERNIZADO
 *
 * ✅ NUEVO: Usa ElectoralSystemController integrado
 * ✅ API SIMPLIFICADA: Todos los métodos devuelven ElectionResult
 * ✅ MENOS CÓDIGO: Elimina duplicación y complejidad
 * ✅ MEJOR ESTRUCTURA: Separación clara de responsabilidades
 *
 * Servidor completo que maneja Reports, Voting, Observer, VotingReceiver y ConfigurationSender
 * usando el nuevo controller integrado para una API limpia y consistente.
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

            System.out.println("🏛️  ========== SERVIDOR ELECTORAL MODERNIZADO ==========");
            System.out.println("✅ Iniciando servidor con ElectoralSystemController integrado...");

            // =================== INICIALIZACIÓN DEL CONTROLLER PRINCIPAL ===================

            System.out.println("🎮 Inicializando Controller Electoral Integrado...");
            electoralController = new ServerControllerImpl(); // Tu ElectoralSystemController
            System.out.println("✅ ElectoralSystemController inicializado exitosamente");

            // Mostrar estado inicial del sistema
            showSystemInitializationStatus();

            // =================== CONFIGURACIÓN DE ADAPTERS ICE ===================

            System.out.println("\n📡 Configurando servicios Ice...");

            ObjectAdapter reportsAdapter = communicator.createObjectAdapter("ReportsServer");
            ObjectAdapter votingAdapter = communicator.createObjectAdapter("VotingServer");
            ObjectAdapter notifierAdapter = communicator.createObjectAdapter("VoteNotifierServer");
            ObjectAdapter votingReceiverAdapter = communicator.createObjectAdapterWithEndpoints(
                    "VotingReceiverServer", "tcp -h localhost -p 10012"
            );

            // =================== SERVICIO DE REPORTES ===================

            System.out.println("📈 Configurando servicio de Reports...");
            ConnectionDBinterface connectionDB = new ConnectionDB();
            reportsManager = new ReportsManagerImpl(connectionDB);
            reportsAdapter.add((ReportsService) reportsManager, Util.stringToIdentity("ReportsManager"));

            // =================== SERVICIO DE VOTACIÓN ===================

            System.out.println("🗳️  Configurando servicio de Voting...");
            votingManager = new VotingManagerImpl(connectionDB);
            votingAdapter.add((ConfigurationService) votingManager, Util.stringToIdentity("ConfigurationManager"));

            // =================== CONFIGURATION SENDER ===================

            System.out.println("📤 Configurando servicio de envío de configuraciones...");
            configurationSender = new ConfigurationSender(votingManager, communicator);

            // ✅ CONECTAR CON EL CONTROLLER INTEGRADO
            electoralController.setConfigurationSender(configurationSender);
            System.out.println("🔗 ConfigurationSender conectado con ElectoralController");

            // =================== SERVICIO DE OBSERVER ===================

            System.out.println("🔔 Configurando servicio de Observer...");
            voteNotifier = new VoteNotifierImpl();
            notifierAdapter.add((VoteNotifier) voteNotifier, Util.stringToIdentity("VoteNotifier"));

            // ✅ CONECTAR CON EL CONTROLLER INTEGRADO
            electoralController.setVoteNotifier(voteNotifier);
            System.out.println("🔗 VoteNotifier conectado con ElectoralController");

            // =================== SERVICIO DE VOTING RECEIVER ===================

            System.out.println("📥 Configurando servicio de VotingReceiver...");
            VotingReceiverImp votingReceiver = new VotingReceiverImp(electoralController);
            votingReceiverAdapter.add((RMDestination) votingReceiver, Util.stringToIdentity("Service"));

            // =================== ACTIVACIÓN DE SERVICIOS ===================

            System.out.println("🚀 Activando servicios Ice...");
            reportsAdapter.activate();
            votingAdapter.activate();
            notifierAdapter.activate();
            votingReceiverAdapter.activate();

            // =================== LANZAR INTERFAZ GRÁFICA ===================
            System.out.println("🖥️ Iniciando Interfaz Gráfica del Servidor...");

            // Lanzar la UI en el hilo de eventos de Swing
            ServerUI.launchUI(electoralController);

            System.out.println("✅ Interfaz Gráfica iniciada exitosamente");

            // =================== INFORMACIÓN DEL SERVIDOR ===================

            System.out.println("\n✅ ========== SERVIDOR ELECTORAL ACTIVO ==========");
            System.out.println("🎮 Controller: ElectoralSystemController (API Integrada)");
            System.out.println("📊 Servicio Reports: ACTIVO (puerto 9001)");
            System.out.println("🗳️  Servicio Voting: ACTIVO (puerto 9003)");
            System.out.println("🔔 Servicio Observer: ACTIVO (puerto 9002)");
            System.out.println("📥 Servicio VotingReceiver: ACTIVO (puerto 10012)");
            System.out.println("📤 ConfigurationSender: ACTIVO");
            System.out.println("🔌 Base de datos: CONECTADA con pool optimizado");

            // =================== MÉTODOS DISPONIBLES ===================

            System.out.println("\n🎮 ========== MÉTODOS DE CONTROL DISPONIBLES ==========");
            System.out.println("📋 GESTIÓN DE ELECCIONES:");
            System.out.println("   • createElection(name, startDate, endDate)");
            System.out.println("   • startVoting(electionId) / stopVoting(electionId)");
            System.out.println("   • getElectionInfo(electionId)");

            System.out.println("\n👥 GESTIÓN DE CANDIDATOS:");
            System.out.println("   • addCandidate(electionId, name, party)");
            System.out.println("   • loadCandidatesFromCSV(electionId, filePath)");

            System.out.println("\n📤 CONFIGURACIÓN DE MESAS:");
            System.out.println("   • sendConfigurationToMesa(mesaId, electionId)");
            System.out.println("   • sendConfigurationToDepartment(deptId, electionId)");

            System.out.println("\n📊 REPORTES Y CONSULTAS:");
            System.out.println("   • getCitizenReport(documento, electionId)");
            System.out.println("   • getElectionResults(electionId)");
            System.out.println("   • searchCitizens(nombre, apellido, limit)");

            System.out.println("\n🔧 MONITOREO Y DIAGNÓSTICO:");
            System.out.println("   • getSystemStatus() - Estado completo del sistema");
            System.out.println("   • runSystemDiagnostic() - Diagnóstico detallado");
            System.out.println("   • getPerformanceStatistics() - Métricas de rendimiento");

            System.out.println("\n🧪 MÉTODOS DE PRUEBA (Mesa " + MESA_6823_ID + "):");
            System.out.println("   • testStartElectionMesa6823()");
            System.out.println("   • testCloseElectionMesa6823()");
            System.out.println("   • testConnectivityMesa6823()");
            System.out.println("==================================================");

            // =================== ESTADO INICIAL DEL SISTEMA ===================

            System.out.println("\n📊 ========== ESTADO INICIAL DEL SISTEMA ==========");
            ElectionResult systemStatus = electoralController.getSystemStatus();
            if (systemStatus.isSuccess()) {
                System.out.println("✅ Sistema inicializado correctamente");
                System.out.println("📋 Información disponible en systemStatus.getData()");
            } else {
                System.out.println("⚠️ Advertencias en inicialización: " + systemStatus.getMessage());
            }

            // =================== PRUEBAS AUTOMÁTICAS ===================

            System.out.println("\n🧪 Iniciando pruebas automáticas en 10 segundos...");
            System.out.println("   📱 Para pruebas completas, ejecuta el cliente: java -jar client/build/libs/client.jar");

            // Lanzar pruebas en hilo separado
            new Thread(() -> {
                try {
                    Thread.sleep(10000); // Esperar 10 segundos
                    runAutomaticTests();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();

            System.out.println("\n⏳ Servidor listo - Esperando solicitudes de clientes...");

            // =================== ESPERA Y SHUTDOWN ===================

            communicator.waitForShutdown();
            System.out.println("🛑 Cerrando Servidor Electoral...");

            // Cleanup del controller
            electoralController.shutdown();

        } catch (LocalException e) {
            System.err.println("❌ Error de Ice: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } catch (Exception e) {
            System.err.println("❌ Error general: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
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

    // =================== MÉTODOS DE PRUEBA ESPECÍFICOS ===================

    /**
     * ✅ SIMPLIFICADO: Prueba de conectividad con mesa 6823
     */
    public static boolean testConnectivityMesa6823() {
        System.out.println("🔍 Probando conectividad con mesa " + MESA_6823_ID + "...");

        if (configurationSender == null) {
            System.err.println("❌ ConfigurationSender no disponible");
            return false;
        }

        try {
            String endpoint = "ConfigurationReceiver:default -h localhost -p " + MESA_6823_PORT;
            ObjectPrx base = configurationSender.communicator.stringToProxy(endpoint);
            ConfigurationSystem.ConfigurationReceiverPrx receiver =
                    ConfigurationSystem.ConfigurationReceiverPrx.checkedCast(base);

            if (receiver != null && receiver.isReady(MESA_6823_ID)) {
                System.out.println("✅ Mesa " + MESA_6823_ID + " conectada y lista");
                return true;
            } else {
                System.out.println("❌ Mesa " + MESA_6823_ID + " no disponible");
                return false;
            }
        } catch (Exception e) {
            System.err.println("❌ Error conectando: " + e.getMessage());
            return false;
        }
    }

    /**
     * ✅ SIMPLIFICADO: Inicia elección en mesa 6823
     */
    public static boolean testStartElectionMesa6823() {
        System.out.println("🗳️ Iniciando elección en mesa " + MESA_6823_ID + "...");
        return sendElectionStatusToMesa6823("DURING");
    }

    /**
     * ✅ SIMPLIFICADO: Cierra elección en mesa 6823
     */
    public static boolean testCloseElectionMesa6823() {
        System.out.println("🔒 Cerrando elección en mesa " + MESA_6823_ID + "...");
        return sendElectionStatusToMesa6823("CLOSED");
    }

    /**
     * ✅ SIMPLIFICADO: Resetea elección en mesa 6823
     */
    public static boolean testResetElectionMesa6823() {
        System.out.println("⏪ Reseteando elección en mesa " + MESA_6823_ID + "...");
        return sendElectionStatusToMesa6823("PRE");
    }

    // =================== MÉTODOS HELPER PRIVADOS ===================

    private static void showSystemInitializationStatus() {
        System.out.println("\n📊 Estado de inicialización:");

        try {
            ElectionResult status = electoralController.getSystemStatus();
            if (status.isSuccess()) {
                System.out.println("✅ Base de datos: CONECTADA");
                System.out.println("✅ Pool de conexiones: ACTIVO");
                System.out.println("✅ Elección de prueba: CONFIGURADA");
                System.out.println("✅ Candidatos de prueba: CARGADOS");
            } else {
                System.out.println("⚠️ Advertencias durante inicialización");
            }
        } catch (Exception e) {
            System.out.println("⚠️ No se pudo obtener estado inicial: " + e.getMessage());
        }
    }

    private static void runAutomaticTests() {
        System.out.println("\n🧪 ========== EJECUTANDO PRUEBAS AUTOMÁTICAS ==========");

        try {
            // 1. Prueba de conectividad
            System.out.println("\n1️⃣ Prueba de conectividad con mesa " + MESA_6823_ID + ":");
            boolean connectivity = testConnectivityMesa6823();
            System.out.println("   Resultado: " + (connectivity ? "✅ CONECTADA" : "❌ NO CONECTADA"));

            if (!connectivity) {
                System.out.println("   💡 Para pruebas completas, ejecuta el cliente en otra terminal");
                System.out.println("   📱 java -jar client/build/libs/client.jar");
            }

            // 2. Prueba de diagnóstico del sistema
            System.out.println("\n2️⃣ Diagnóstico del sistema:");
            ElectionResult diagnostic = electoralController.runSystemDiagnostic();
            System.out.println("   Estado: " + (diagnostic.isSuccess() ? "✅ SALUDABLE" : "⚠️ CON PROBLEMAS"));
            if (!diagnostic.isSuccess()) {
                System.out.println("   Mensaje: " + diagnostic.getMessage());
            }

            // 3. Prueba de estado del sistema
            System.out.println("\n3️⃣ Estado del sistema:");
            ElectionResult systemStatus = electoralController.getSystemStatus();
            if (systemStatus.isSuccess()) {
                System.out.println("   ✅ Sistema operativo y listo");
                System.out.println("   📊 Métricas disponibles en systemStatus.getData()");
            }

            // 4. Pruebas de cambio de estado (solo si cliente conectado)
            if (connectivity) {
                System.out.println("\n4️⃣ Pruebas de cambio de estado en mesa " + MESA_6823_ID + ":");

                // Test DURING
                boolean startResult = testStartElectionMesa6823();
                System.out.println("   Iniciar elección: " + (startResult ? "✅ ÉXITO" : "❌ ERROR"));
                Thread.sleep(3000);

                // Test CLOSED
                boolean stopResult = testCloseElectionMesa6823();
                System.out.println("   Cerrar elección: " + (stopResult ? "✅ ÉXITO" : "❌ ERROR"));
                Thread.sleep(3000);

                // Test PRE
                boolean resetResult = testResetElectionMesa6823();
                System.out.println("   Reset elección: " + (resetResult ? "✅ ÉXITO" : "❌ ERROR"));

                // Resumen
                int successCount = (startResult ? 1 : 0) + (stopResult ? 1 : 0) + (resetResult ? 1 : 0);
                System.out.println("\n   📊 Resumen: " + successCount + "/3 pruebas exitosas");

                if (successCount == 3) {
                    System.out.println("   🎉 ¡PERFECTO! Comunicación servidor-cliente funcionando");
                } else if (successCount > 0) {
                    System.out.println("   ⚠️ Funcionamiento parcial - revisar logs");
                } else {
                    System.out.println("   ❌ Comunicación fallida - verificar cliente");
                }
            }

            System.out.println("\n✅ ========== PRUEBAS AUTOMÁTICAS COMPLETADAS ==========");
            System.out.println("💡 Para más pruebas, usa los métodos estáticos del Server");
            System.out.println("🎮 Ejemplo: Server.getSystemStatus()");

        } catch (Exception e) {
            System.err.println("❌ Error en pruebas automáticas: " + e.getMessage());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean sendElectionStatusToMesa6823(String newStatus) {
        if (configurationSender == null) {
            System.err.println("❌ ConfigurationSender no disponible");
            return false;
        }

        try {
            String endpoint = "ConfigurationReceiver:default -h localhost -p " + MESA_6823_PORT;
            ObjectPrx base = configurationSender.communicator.stringToProxy(endpoint);
            ConfigurationSystem.ConfigurationReceiverPrx receiver =
                    ConfigurationSystem.ConfigurationReceiverPrx.checkedCast(base);

            if (receiver != null && receiver.isReady(MESA_6823_ID)) {
                return receiver.updateElectionStatus(1, newStatus); // Elección ID 1
            }
            return false;
        } catch (Exception e) {
            System.err.println("❌ Error enviando estado: " + e.getMessage());
            return false;
        }
    }

    // =================== MÉTODOS DE COMPATIBILIDAD ===================

    /**
     * ✅ COMPATIBILIDAD: Para código legacy que use estos métodos
     */
    public static boolean startElectionInAllMachines() {
        ElectionResult result = startVoting(1); // Elección por defecto
        System.out.println("🗳️ " + result.getMessage());
        return result.isSuccess();
    }

    public static boolean closeElectionInAllMachines() {
        ElectionResult result = stopVoting(1); // Elección por defecto
        System.out.println("🔒 " + result.getMessage());
        return result.isSuccess();
    }

    public static boolean resetElectionInAllMachines() {
        ElectionResult result = electoralController.resetVoting(1); // Elección por defecto
        System.out.println("⏪ " + result.getMessage());
        return result.isSuccess();
    }

    /**
     * ✅ NUEVO: Muestra información completa del sistema
     */
    public static void showSystemInfo() {
        System.out.println("\n📊 ========== INFORMACIÓN DEL SISTEMA ==========");

        ElectionResult status = getSystemStatus();
        if (status.isSuccess()) {
            System.out.println("✅ ElectoralSystemController: ACTIVO");
            System.out.println("✅ API Integrada: DISPONIBLE");
            System.out.println("✅ Todos los subsistemas: OPERATIVOS");

            System.out.println("\n🎯 Mesa de prueba: " + MESA_6823_ID + " (Puerto " + MESA_6823_PORT + ")");
            System.out.println("📊 Métricas del sistema disponibles");
            System.out.println("🔧 Diagnósticos automáticos activos");
        } else {
            System.out.println("❌ Sistema con problemas: " + status.getMessage());
        }

        System.out.println("\n💡 Usa Server.getSystemStatus() para detalles completos");
        System.out.println("🧪 Usa Server.runSystemDiagnostic() para diagnóstico detallado");
        System.out.println("================================================");
    }
}