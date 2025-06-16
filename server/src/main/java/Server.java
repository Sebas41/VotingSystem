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

// ✅ IMPORTAR ENUM PARA ESTADOS
import Elections.models.ELECTION_STATUS;

import javax.swing.*;
import com.zeroc.Ice.Exception;

/**
 * Servidor Electoral completo que maneja Reports, Voting, Observer, VotingReceiver y ConfigurationSender
 * ✅ ACTUALIZADO: Incluye soporte para cambio de estado de elecciones en todas las mesas
 * ✅ MODIFICADO: Pruebas específicas para mesa 6823 únicamente
 * Patrón máquina de café con strings formateados + Patrón Observer + Reliable Messaging + Configuración Remota
 */
public class Server {

    // ✅ VARIABLES ESTÁTICAS EXISTENTES
    private static VoteNotifierImpl voteNotifier;
    private static ServerControllerInterface serverController;

    // ✅ VARIABLE ESTÁTICA PARA NUESTRO CONFIGURATION SENDER
    private static ConfigurationSender configurationSender;

    // ✅ CONSTANTES PARA PUERTO ESPECÍFICO DE MESA 6823
    private static final int MESA_6823_ID = 6823;
    private static final int MESA_6823_PORT = 10843; // 10020 + (6823 % 1000)

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

            // ✅ NUESTRO CONFIGURATION SENDER
            System.out.println("📤 Configurando servicio de envío de configuraciones...");
            configurationSender = new ConfigurationSender(votingManager, communicator);

            // ✅ CONECTAR EL CONFIGURATION SENDER CON EL SERVER CONTROLLER
            ((ServerControllerImpl) serverController).setConfigurationSender(configurationSender);
            System.out.println("🔗 ServerController conectado con ConfigurationSender");

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
            System.out.println("   - Mesa objetivo: " + MESA_6823_ID + " (Puerto " + MESA_6823_PORT + ")");
            System.out.println("   - ✅ NUEVO: Control de estado de elecciones");
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
            System.out.println();
            System.out.println("✅ ========== MÉTODOS DE CONTROL PARA MESA " + MESA_6823_ID + " ==========");
            System.out.println("🗳️  testStartElectionMesa6823() - Iniciar elección en mesa " + MESA_6823_ID);
            System.out.println("🔒 testCloseElectionMesa6823() - Cerrar elección en mesa " + MESA_6823_ID);
            System.out.println("⏪ testResetElectionMesa6823() - Resetear elección en mesa " + MESA_6823_ID);
            System.out.println("🔍 testConnectivityMesa6823() - Verificar conectividad con mesa " + MESA_6823_ID);
            System.out.println("📤 sendConfigurationToMesa(mesaId, electionId) - Enviar configuración específica");
            System.out.println("🔍 checkMesaConfigurationStatus(mesaId) - Verificar estado de mesa");
            System.out.println("🧪 testConfiguration() - Prueba de configuración");
            System.out.println();
            System.out.println("⏳ Esperando solicitudes de clientes...");
            System.out.println("====================================================");
            System.out.println();

            // ✅ PRUEBA INICIAL DE CONECTIVIDAD CON MESA 6823
            System.out.println("🔍 Verificando conectividad inicial con mesa " + MESA_6823_ID + "...");
            new Thread(() -> {
                try {
                    Thread.sleep(5000); // Esperar 5 segundos
                    testConnectivityMesa6823();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();

            // ✅ NUEVA PRUEBA AUTOMÁTICA DE CONFIGURACIÓN
            System.out.println("🧪 Iniciando prueba automática de configuración...");

            // Lanzar prueba en hilo separado
            new Thread(() -> {
                configurationSender.testSendToMesa6823();
            }).start();

            // ✅ EJEMPLO DE USO DE LOS NUEVOS MÉTODOS
            System.out.println("📝 Ejemplo de uso de métodos de control:");
            System.out.println("   Para iniciar elección: Server.testStartElectionMesa6823()");
            System.out.println("   Para cerrar elección: Server.testCloseElectionMesa6823()");
            System.out.println();

            // =================== ✅ PRUEBA AUTOMÁTICA DE CAMBIO DE ESTADO SOLO MESA 6823 ===================
            System.out.println("🧪 Iniciando prueba automática de cambio de estado SOLO MESA " + MESA_6823_ID + " en 25 segundos...");
            System.out.println("   (Esto dará tiempo para que el cliente se conecte)");
            System.out.println("   📱 Asegúrate de tener el cliente ejecutándose: java -jar client/build/libs/client.jar");

            // Lanzar prueba en hilo separado para no bloquear el servidor
            new Thread(() -> {
                try {
                    // Esperar que el cliente esté listo
                    System.out.println("⏳ Esperando 25 segundos para que el cliente se conecte...");
                    Thread.sleep(25000);

                    System.out.println("\n🧪 ========== INICIANDO PRUEBAS AUTOMÁTICAS (SOLO MESA " + MESA_6823_ID + ") ==========");

                    // =================== PRUEBA 1: INICIAR ELECCIÓN ===================
                    System.out.println("\n🧪 PRUEBA 1: Cambiar estado a DURING (Elección activa) en mesa " + MESA_6823_ID);
                    boolean result1 = testStartElectionMesa6823();
                    System.out.println("Resultado: " + (result1 ? "✅ ÉXITO" : "❌ ERROR"));

                    if (result1) {
                        System.out.println("🎯 EN EL CLIENTE DEBERÍAS VER:");
                        System.out.println("   - 'Elección activa. Votación disponible.'");
                        System.out.println("   - Estado de votación: ABIERTA");
                    }

                    // Esperar para ver el cambio
                    System.out.println("⏳ Esperando 10 segundos para observar el cambio...");
                    Thread.sleep(10000);

                    // =================== PRUEBA 2: CERRAR ELECCIÓN ===================
                    System.out.println("\n🧪 PRUEBA 2: Cambiar estado a CLOSED (Elección cerrada) en mesa " + MESA_6823_ID);
                    boolean result2 = testCloseElectionMesa6823();
                    System.out.println("Resultado: " + (result2 ? "✅ ÉXITO" : "❌ ERROR"));

                    if (result2) {
                        System.out.println("🎯 EN EL CLIENTE DEBERÍAS VER:");
                        System.out.println("   - 'La elección ha terminado. Gracias por participar.'");
                        System.out.println("   - Estado de votación: CERRADA");
                    }

                    // Esperar para ver el cambio
                    System.out.println("⏳ Esperando 10 segundos para observar el cambio...");
                    Thread.sleep(10000);

                    // =================== PRUEBA 3: RESETEAR ELECCIÓN ===================
                    System.out.println("\n🧪 PRUEBA 3: Cambiar estado a PRE (Elección no iniciada) en mesa " + MESA_6823_ID);
                    boolean result3 = testResetElectionMesa6823();
                    System.out.println("Resultado: " + (result3 ? "✅ ÉXITO" : "❌ ERROR"));

                    if (result3) {
                        System.out.println("🎯 EN EL CLIENTE DEBERÍAS VER:");
                        System.out.println("   - 'La elección aún no ha iniciado. Votación no disponible.'");
                        System.out.println("   - Estado de votación: CERRADA");
                    }

                    // =================== RESUMEN ===================
                    System.out.println("\n📊 ========== RESUMEN DE PRUEBAS AUTOMÁTICAS (MESA " + MESA_6823_ID + ") ==========");
                    System.out.println("Estado DURING: " + (result1 ? "✅ FUNCIONA" : "❌ ERROR"));
                    System.out.println("Estado CLOSED: " + (result2 ? "✅ FUNCIONA" : "❌ ERROR"));
                    System.out.println("Estado PRE: " + (result3 ? "✅ FUNCIONA" : "❌ ERROR"));

                    int exitosos = (result1 ? 1 : 0) + (result2 ? 1 : 0) + (result3 ? 1 : 0);

                    if (exitosos == 3) {
                        System.out.println("\n🎉🎉🎉 PERFECTO! TODAS LAS PRUEBAS EXITOSAS (MESA " + MESA_6823_ID + ") 🎉🎉🎉");
                        System.out.println("✅ Tu implementación de cambio de estado funciona correctamente");
                        System.out.println("✅ La comunicación servidor-cliente está funcionando en puerto " + MESA_6823_PORT);
                        System.out.println("✅ Los archivos JSON se están actualizando correctamente");
                        System.out.println("✅ El control independiente de horarios está funcionando");
                    } else if (exitosos >= 1) {
                        System.out.println("\n⚠️ Funciona parcialmente (" + exitosos + "/3)");
                        System.out.println("💡 Revisar logs del servidor y cliente para errores específicos");
                    } else {
                        System.out.println("\n❌ No funcionó - Posibles causas:");
                        System.out.println("   1. Cliente no está ejecutándose");
                        System.out.println("   2. Problemas de conexión Ice en puerto " + MESA_6823_PORT);
                        System.out.println("   3. Error en la implementación del método updateElectionStatus");
                        System.out.println("   4. ConfigurationReceiver.ice no tiene el nuevo método");
                        System.out.println("   5. Mesa " + MESA_6823_ID + " no está escuchando correctamente");
                    }

                    System.out.println("=========================================");

                    // Opcionalmente, continuar con pruebas periódicas
                    System.out.println("\n💡 Puedes seguir probando manualmente llamando:");
                    System.out.println("   - Server.testStartElectionMesa6823()");
                    System.out.println("   - Server.testCloseElectionMesa6823()");
                    System.out.println("   - Server.testResetElectionMesa6823()");
                    System.out.println("   - Server.testConnectivityMesa6823()");

                } catch (InterruptedException e) {
                    System.err.println("❌ Pruebas automáticas interrumpidas: " + e.getMessage());
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    System.err.println("❌ Error en pruebas automáticas: " + e.getMessage());
                    e.printStackTrace();
                }
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

    // =================== ✅ NUEVOS MÉTODOS DE CONTROL DE ESTADO ESPECÍFICOS PARA MESA 6823 ===================

    /**
     * ✅ NUEVO: Prueba de conectividad específica con mesa 6823
     */
    public static boolean testConnectivityMesa6823() {
        if (configurationSender == null) {
            System.err.println("❌ ConfigurationSender no está inicializado");
            return false;
        }

        System.out.println("🔍 Probando conectividad con mesa " + MESA_6823_ID + " en puerto " + MESA_6823_PORT + "...");

        try {
            String endpoint = "ConfigurationReceiver:default -h localhost -p " + MESA_6823_PORT;
            System.out.println("🔗 Endpoint: " + endpoint);

            ObjectPrx base = configurationSender.communicator.stringToProxy(endpoint);
            ConfigurationSystem.ConfigurationReceiverPrx receiver =
                    ConfigurationSystem.ConfigurationReceiverPrx.checkedCast(base);

            if (receiver == null) {
                System.out.println("❌ No se pudo conectar con mesa " + MESA_6823_ID + " en puerto " + MESA_6823_PORT);
                System.out.println("💡 Verifica que el cliente esté ejecutándose");
                return false;
            }

            boolean ready = receiver.isReady(MESA_6823_ID);
            if (ready) {
                System.out.println("✅ Mesa " + MESA_6823_ID + " está CONECTADA y LISTA en puerto " + MESA_6823_PORT);

                String status = receiver.getConfigurationStatus(MESA_6823_ID);
                System.out.println("📊 Estado de configuración: " + status);

                return true;
            } else {
                System.out.println("⚠️ Mesa " + MESA_6823_ID + " está conectada pero NO está lista");
                return false;
            }

        } catch (Exception e) {
            System.err.println("❌ Error conectando con mesa " + MESA_6823_ID + ": " + e.getMessage());
            System.out.println("💡 Posibles causas:");
            System.out.println("   - Cliente no está ejecutándose");
            System.out.println("   - Puerto " + MESA_6823_PORT + " no está abierto");
            System.out.println("   - ConfigurationReceiver no está inicializado en el cliente");
            return false;
        }
    }

    /**
     * ✅ NUEVO: Inicia la elección específicamente en mesa 6823
     */
    public static boolean testStartElectionMesa6823() {
        if (configurationSender == null) {
            System.err.println("❌ ConfigurationSender no está inicializado");
            return false;
        }

        System.out.println("🗳️ TEST: Iniciando elección en mesa " + MESA_6823_ID + " (puerto " + MESA_6823_PORT + ")");
        return sendElectionStatusToMesa6823("DURING");
    }

    /**
     * ✅ NUEVO: Cierra la elección específicamente en mesa 6823
     */
    public static boolean testCloseElectionMesa6823() {
        if (configurationSender == null) {
            System.err.println("❌ ConfigurationSender no está inicializado");
            return false;
        }

        System.out.println("🔒 TEST: Cerrando elección en mesa " + MESA_6823_ID + " (puerto " + MESA_6823_PORT + ")");
        return sendElectionStatusToMesa6823("CLOSED");
    }

    /**
     * ✅ NUEVO: Resetea la elección específicamente en mesa 6823
     */
    public static boolean testResetElectionMesa6823() {
        if (configurationSender == null) {
            System.err.println("❌ ConfigurationSender no está inicializado");
            return false;
        }

        System.out.println("⏪ TEST: Reseteando elección en mesa " + MESA_6823_ID + " (puerto " + MESA_6823_PORT + ")");
        return sendElectionStatusToMesa6823("PRE");
    }

    /**
     * ✅ NUEVO MÉTODO HELPER: Envía estado de elección específicamente a mesa 6823
     */
    private static boolean sendElectionStatusToMesa6823(String newStatus) {
        try {
            int electionId = 1; // ID de elección por defecto

            System.out.println("📤 Enviando estado '" + newStatus + "' a mesa " + MESA_6823_ID + "...");

            String endpoint = "ConfigurationReceiver:default -h localhost -p " + MESA_6823_PORT;
            System.out.println("🔗 Conectando a: " + endpoint);

            ObjectPrx base = configurationSender.communicator.stringToProxy(endpoint);
            ConfigurationSystem.ConfigurationReceiverPrx receiver =
                    ConfigurationSystem.ConfigurationReceiverPrx.checkedCast(base);

            if (receiver == null) {
                System.err.println("❌ No se pudo conectar con mesa " + MESA_6823_ID + " en puerto " + MESA_6823_PORT);
                return false;
            }

            // Verificar que la mesa esté lista
            if (!receiver.isReady(MESA_6823_ID)) {
                System.err.println("⚠️ Mesa " + MESA_6823_ID + " no está lista para recibir cambios");
                return false;
            }

            // Enviar cambio de estado
            boolean success = receiver.updateElectionStatus(electionId, newStatus);

            if (success) {
                System.out.println("✅ Estado '" + newStatus + "' enviado exitosamente a mesa " + MESA_6823_ID);
            } else {
                System.err.println("❌ Mesa " + MESA_6823_ID + " rechazó el cambio de estado a '" + newStatus + "'");
            }

            return success;

        } catch (Exception e) {
            System.err.println("❌ Error enviando estado a mesa " + MESA_6823_ID + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // =================== MÉTODOS ORIGINALES MANTENIDOS ===================

    /**
     * ✅ ORIGINAL: Inicia la elección en todas las mesas de votación
     */
    public static boolean startElectionInAllMachines() {
        if (serverController == null) {
            System.err.println("❌ ServerController no está inicializado");
            return false;
        }

        System.out.println("🗳️ Iniciando elección en todas las mesas...");
        return ((ServerControllerImpl) serverController).startElectionInAllMachines();
    }

    /**
     * ✅ ORIGINAL: Cierra la elección en todas las mesas de votación
     */
    public static boolean closeElectionInAllMachines() {
        if (serverController == null) {
            System.err.println("❌ ServerController no está inicializado");
            return false;
        }

        System.out.println("🔒 Cerrando elección en todas las mesas...");
        return ((ServerControllerImpl) serverController).closeElectionInAllMachines();
    }

    /**
     * ✅ ORIGINAL: Resetea la elección a estado PRE en todas las mesas
     */
    public static boolean resetElectionInAllMachines() {
        if (serverController == null) {
            System.err.println("❌ ServerController no está inicializado");
            return false;
        }

        System.out.println("⏪ Reseteando elección a estado PRE en todas las mesas...");
        return ((ServerControllerImpl) serverController).resetElectionInAllMachines();
    }

    /**
     * ✅ ORIGINAL: Cambia el estado de la elección en todas las mesas
     */
    public static boolean changeElectionStatusInAllMachines(ELECTION_STATUS newStatus) {
        if (serverController == null) {
            System.err.println("❌ ServerController no está inicializado");
            return false;
        }

        System.out.println("🔄 Cambiando estado de elección a " + newStatus + " en todas las mesas...");
        return ((ServerControllerImpl) serverController).changeElectionStatusInAllMachines(newStatus);
    }

    /**
     * ✅ ORIGINAL: Cambia el estado de la elección solo en mesas específicas
     */
    public static boolean changeElectionStatusInSpecificMachines(ELECTION_STATUS newStatus, List<Integer> mesaIds) {
        if (configurationSender == null) {
            System.err.println("❌ ConfigurationSender no está inicializado");
            return false;
        }

        System.out.println("🔄 Cambiando estado de elección a " + newStatus + " en " + mesaIds.size() + " mesas específicas...");

        // Asumiendo que tenemos elección ID 1 (puedes hacer esto configurable)
        int electionId = 1;
        return configurationSender.changeElectionStatusForSpecificMachines(electionId, newStatus.name(), mesaIds);
    }

    /**
     * ✅ ORIGINAL: Diagnóstico de conectividad con todas las mesas
     */
    public static void diagnosticCheckAllMachines() {
        if (configurationSender == null) {
            System.err.println("❌ ConfigurationSender no está inicializado");
            return;
        }

        System.out.println("🔍 Ejecutando diagnóstico de conectividad...");
        configurationSender.diagnosticCheckAllMachines();
    }

    // =================== MÉTODOS EXISTENTES MEJORADOS ===================

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
     * ✅ MEJORADO: Método para pruebas manuales con más opciones
     */
    public static void testConfiguration() {
        if (configurationSender != null) {
            new Thread(() -> {
                configurationSender.testSendToMesa6823();
            }).start();
        }
    }

    /**
     * ✅ NUEVO: Prueba de cambio de estado específica para mesa 6823
     */
    public static void testElectionStatusChangeMesa6823() {
        System.out.println("🧪 Iniciando prueba de cambio de estado para mesa " + MESA_6823_ID + "...");

        // Prueba secuencial de estados
        new Thread(() -> {
            try {
                System.out.println("1. Iniciando elección en mesa " + MESA_6823_ID + "...");
                boolean started = testStartElectionMesa6823();
                System.out.println("   Resultado: " + (started ? "✅ ÉXITO" : "❌ ERROR"));

                Thread.sleep(5000); // Esperar 5 segundos

                System.out.println("2. Cerrando elección en mesa " + MESA_6823_ID + "...");
                boolean closed = testCloseElectionMesa6823();
                System.out.println("   Resultado: " + (closed ? "✅ ÉXITO" : "❌ ERROR"));

                Thread.sleep(3000); // Esperar 3 segundos

                System.out.println("3. Reseteando elección en mesa " + MESA_6823_ID + "...");
                boolean reset = testResetElectionMesa6823();
                System.out.println("   Resultado: " + (reset ? "✅ ÉXITO" : "❌ ERROR"));

                System.out.println("🎉 Prueba de cambio de estado completada para mesa " + MESA_6823_ID);

            } catch (InterruptedException e) {
                System.err.println("❌ Prueba interrumpida: " + e.getMessage());
            }
        }).start();
    }

    /**
     * ✅ ORIGINAL: Prueba de cambio de estado para todas las mesas
     */
    public static void testElectionStatusChange() {
        System.out.println("🧪 Iniciando prueba de cambio de estado...");

        // Prueba secuencial de estados
        new Thread(() -> {
            try {
                System.out.println("1. Iniciando elección...");
                boolean started = startElectionInAllMachines();
                System.out.println("   Resultado: " + (started ? "✅ ÉXITO" : "❌ ERROR"));

                Thread.sleep(5000); // Esperar 5 segundos

                System.out.println("2. Cerrando elección...");
                boolean closed = closeElectionInAllMachines();
                System.out.println("   Resultado: " + (closed ? "✅ ÉXITO" : "❌ ERROR"));

                Thread.sleep(3000); // Esperar 3 segundos

                System.out.println("3. Reseteando elección...");
                boolean reset = resetElectionInAllMachines();
                System.out.println("   Resultado: " + (reset ? "✅ ÉXITO" : "❌ ERROR"));

                System.out.println("🎉 Prueba de cambio de estado completada");

            } catch (InterruptedException e) {
                System.err.println("❌ Prueba interrumpida: " + e.getMessage());
            }
        }).start();
    }

    /**
     * ✅ MEJORADO: Información del sistema
     */
    public static void showSystemInfo() {
        System.out.println();
        System.out.println("📊 ========== INFORMACIÓN DEL SISTEMA ==========");
        System.out.println("🎯 Mesa de prueba: " + MESA_6823_ID);
        System.out.println("🔌 Puerto específico: " + MESA_6823_PORT);
        System.out.println("📡 Endpoint: ConfigurationReceiver:default -h localhost -p " + MESA_6823_PORT);

        if (serverController != null) {
            System.out.println("✅ ServerController: INICIALIZADO");
        } else {
            System.out.println("❌ ServerController: NO INICIALIZADO");
        }

        if (configurationSender != null) {
            System.out.println("✅ ConfigurationSender: INICIALIZADO");
        } else {
            System.out.println("❌ ConfigurationSender: NO INICIALIZADO");
        }

        if (voteNotifier != null) {
            System.out.println("✅ VoteNotifier: INICIALIZADO");
        } else {
            System.out.println("❌ VoteNotifier: NO INICIALIZADO");
        }

        System.out.println();
        System.out.println("🎮 MÉTODOS ESPECÍFICOS PARA MESA " + MESA_6823_ID + ":");
        System.out.println("   - testStartElectionMesa6823()");
        System.out.println("   - testCloseElectionMesa6823()");
        System.out.println("   - testResetElectionMesa6823()");
        System.out.println("   - testConnectivityMesa6823()");
        System.out.println("   - testElectionStatusChangeMesa6823()");
        System.out.println();
        System.out.println("🎮 MÉTODOS GLOBALES (TODAS LAS MESAS):");
        System.out.println("   - startElectionInAllMachines()");
        System.out.println("   - closeElectionInAllMachines()");
        System.out.println("   - resetElectionInAllMachines()");
        System.out.println("   - diagnosticCheckAllMachines()");
        System.out.println("   - testElectionStatusChange()");
        System.out.println("   - sendConfigurationToMesa(mesaId, electionId)");
        System.out.println("   - checkMesaConfigurationStatus(mesaId)");
        System.out.println("================================================");
        System.out.println();
    }

    /**
     * ✅ ORIGINAL: Método de utilidad para obtener lista de mesas por defecto
     */
    public static List<Integer> getDefaultMesaList() {
        List<Integer> defaultMesas = new ArrayList<>();
        defaultMesas.add(MESA_6823_ID); // Mesa de prueba
        // Puedes agregar más mesas aquí según tu configuración
        return defaultMesas;
    }

    /**
     * ✅ NUEVO: Calcular puerto para cualquier mesa (método público para verificación)
     */
    public static int calculatePortForMesa(int mesaId) {
        return 10020 + (mesaId % 1000);
    }
}