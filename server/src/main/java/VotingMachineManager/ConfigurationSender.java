package configuration;

import ConfigurationSystem.ConfigurationReceiverPrx;
import VotingMachineManager.VotingManagerImpl;
import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.ObjectPrx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Servicio para enviar configuraciones a mesas de votación remotas
 * ✅ ACTUALIZADO: Incluye soporte para cambio de estado de elecciones
 */
public class ConfigurationSender {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationSender.class);
    private final VotingManagerImpl votingManager;
    public final Communicator communicator; // ✅ Público para uso en Server.java

    public ConfigurationSender(VotingManagerImpl votingManager, Communicator communicator) {
        this.votingManager = votingManager;
        this.communicator = communicator;
        logger.info("🚀 ConfigurationSender inicializado");
    }

    /**
     * Envía configuración a una mesa específica
     */
    public boolean sendConfigurationToMachine(int mesaId, int electionId) {
        logger.info("📤 Enviando configuración a mesa {} para elección {}", mesaId, electionId);

        try {
            // 1. Generar configuración
            String configurationData = votingManager.generateMachineConfigurationString(mesaId, electionId);

            if (configurationData.startsWith("ERROR")) {
                logger.error("❌ Error generando configuración: {}", configurationData);
                return false;
            }

            logger.info("📦 Configuración generada para mesa {} - {} caracteres", mesaId, configurationData.length());

            // 2. Conectar con la mesa remota
            int port = 10020 + (mesaId % 1000); // Puerto basado en mesa ID
            String endpoint = "ConfigurationReceiver:default -h localhost -p " + port;

            logger.info("🔗 Conectando a mesa {} en endpoint: {}", mesaId, endpoint);

            ObjectPrx base = communicator.stringToProxy(endpoint);
            ConfigurationReceiverPrx receiver = ConfigurationReceiverPrx.checkedCast(base);

            if (receiver == null) {
                logger.error("❌ No se pudo conectar con la mesa {} en puerto {}", mesaId, port);
                return false;
            }

            // 3. Verificar que la mesa esté lista
            logger.info("🔍 Verificando si mesa {} está lista...", mesaId);
            if (!receiver.isReady(mesaId)) {
                logger.warn("⚠️ Mesa {} no está lista para recibir configuración", mesaId);
                return false;
            }

            // 4. Enviar configuración
            logger.info("📡 Enviando configuración a mesa {}...", mesaId);
            boolean success = receiver.updateConfiguration(mesaId, configurationData);

            if (success) {
                logger.info("✅ Configuración enviada exitosamente a mesa {}", mesaId);

                // Verificar estado
                String status = receiver.getConfigurationStatus(mesaId);
                logger.info("📊 Estado de configuración mesa {}: {}", mesaId, status);

            } else {
                logger.error("❌ Error enviando configuración a mesa {}", mesaId);
            }

            return success;

        } catch (Exception e) {
            logger.error("❌ Error enviando configuración a mesa {}: {}", mesaId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * ✅ NUEVO: Cambia el estado de una elección en todas las mesas registradas
     */
    public boolean changeElectionStatusForAllMachines(int electionId, String newStatus) {
        logger.info("🚀 Iniciando cambio de estado para elección {} -> {} en TODAS las mesas", electionId, newStatus);

        try {
            // 1. Validar el estado
            if (!isValidElectionStatus(newStatus)) {
                logger.error("❌ Estado de elección inválido: {}", newStatus);
                return false;
            }

            // 2. Obtener todas las mesas del sistema
            List<Integer> allMesaIds = votingManager.getAllMesaIds();

            if (allMesaIds.isEmpty()) {
                logger.warn("⚠️ No hay mesas registradas en el sistema");
                return false;
            }

            logger.info("📋 Enviando cambio de estado a {} mesas", allMesaIds.size());

            // 3. Enviar cambio de estado a cada mesa
            int successCount = 0;
            int failureCount = 0;

            for (Integer mesaId : allMesaIds) {
                boolean success = sendElectionStatusToMachine(mesaId, electionId, newStatus);

                if (success) {
                    successCount++;
                    logger.debug("✅ Mesa {} actualizada exitosamente", mesaId);
                } else {
                    failureCount++;
                    logger.warn("❌ Error actualizando mesa {}", mesaId);
                }

                // Pequeña pausa entre envíos para evitar saturar la red
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            // 4. Reporte final
            logger.info("📊 Cambio de estado completado:");
            logger.info("   - Exitosas: {}/{}", successCount, allMesaIds.size());
            logger.info("   - Fallidas: {}/{}", failureCount, allMesaIds.size());
            logger.info("   - Estado: {}", newStatus);

            // Considerar exitoso si al menos 80% de las mesas fueron actualizadas
            double successRate = (double) successCount / allMesaIds.size();
            boolean overallSuccess = successRate >= 0.8;

            if (overallSuccess) {
                logger.info("🎉 Cambio de estado EXITOSO - {:.1f}% de mesas actualizadas", successRate * 100);
            } else {
                logger.error("💥 Cambio de estado PARCIALMENTE FALLIDO - solo {:.1f}% de mesas actualizadas", successRate * 100);
            }

            return overallSuccess;

        } catch (Exception e) {
            logger.error("❌ Error crítico cambiando estado de elección: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * ✅ MÉTODO HELPER: Envía cambio de estado a una mesa específica
     */
    private boolean sendElectionStatusToMachine(int mesaId, int electionId, String newStatus) {
        try {
            // Calcular puerto dinámico
            int port = 10020 + (mesaId % 1000);
            String endpoint = "ConfigurationReceiver:default -h localhost -p " + port;

            logger.debug("🔗 Conectando a mesa {} en puerto {} para cambio de estado", mesaId, port);

            // Conectar con la mesa
            ObjectPrx base = communicator.stringToProxy(endpoint);
            ConfigurationReceiverPrx receiver = ConfigurationReceiverPrx.checkedCast(base);

            if (receiver == null) {
                logger.warn("❌ No se pudo conectar con mesa {} en puerto {}", mesaId, port);
                return false;
            }

            // Verificar que la mesa esté lista
            if (!receiver.isReady(mesaId)) {
                logger.warn("⚠️ Mesa {} no está lista para recibir cambios", mesaId);
                return false;
            }

            // ✅ ENVIAR CAMBIO DE ESTADO
            boolean success = receiver.updateElectionStatus(electionId, newStatus);

            if (success) {
                logger.debug("✅ Estado enviado exitosamente a mesa {}", mesaId);
            } else {
                logger.warn("❌ Mesa {} rechazó el cambio de estado", mesaId);
            }

            return success;

        } catch (Exception e) {
            logger.error("❌ Error enviando estado a mesa {}: {}", mesaId, e.getMessage());
            return false;
        }
    }

    /**
     * ✅ MÉTODO HELPER: Valida el estado de elección
     */
    private boolean isValidElectionStatus(String status) {
        return status != null && (
                status.equals("PRE") ||
                        status.equals("DURING") ||
                        status.equals("CLOSED")
        );
    }

    /**
     * ✅ NUEVO: Cambio de estado para un conjunto específico de mesas
     */
    public boolean changeElectionStatusForSpecificMachines(int electionId, String newStatus, List<Integer> mesaIds) {
        logger.info("🚀 Iniciando cambio de estado para elección {} -> {} en {} mesas específicas",
                electionId, newStatus, mesaIds.size());

        try {
            // 1. Validar el estado
            if (!isValidElectionStatus(newStatus)) {
                logger.error("❌ Estado de elección inválido: {}", newStatus);
                return false;
            }

            if (mesaIds == null || mesaIds.isEmpty()) {
                logger.warn("⚠️ Lista de mesas vacía");
                return false;
            }

            // 2. Enviar cambio de estado a cada mesa especificada
            int successCount = 0;
            int failureCount = 0;

            for (Integer mesaId : mesaIds) {
                boolean success = sendElectionStatusToMachine(mesaId, electionId, newStatus);

                if (success) {
                    successCount++;
                    logger.debug("✅ Mesa {} actualizada exitosamente", mesaId);
                } else {
                    failureCount++;
                    logger.warn("❌ Error actualizando mesa {}", mesaId);
                }

                // Pequeña pausa entre envíos
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            // 3. Reporte final
            logger.info("📊 Cambio de estado específico completado:");
            logger.info("   - Exitosas: {}/{}", successCount, mesaIds.size());
            logger.info("   - Fallidas: {}/{}", failureCount, mesaIds.size());

            double successRate = (double) successCount / mesaIds.size();
            return successRate >= 0.8; // 80% de éxito mínimo

        } catch (Exception e) {
            logger.error("❌ Error crítico cambiando estado en mesas específicas: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * ✅ MÉTODOS DE CONVENIENCIA: Para estados específicos
     */
    public boolean startElectionInAllMachines(int electionId) {
        logger.info("🗳️ Iniciando elección {} en todas las mesas...", electionId);
        return changeElectionStatusForAllMachines(electionId, "DURING");
    }

    public boolean closeElectionInAllMachines(int electionId) {
        logger.info("🔒 Cerrando elección {} en todas las mesas...", electionId);
        return changeElectionStatusForAllMachines(electionId, "CLOSED");
    }

    public boolean resetElectionInAllMachines(int electionId) {
        logger.info("⏪ Reseteando elección {} a estado PRE en todas las mesas...", electionId);
        return changeElectionStatusForAllMachines(electionId, "PRE");
    }

    /**
     * ✅ MÉTODO DE PRUEBA: Para testing del cambio de estado
     */
    public void testChangeElectionStatusToAllMachines(int electionId, String newStatus) {
        logger.info("🧪 INICIANDO PRUEBA - Cambio de estado de elección {} a {}", electionId, newStatus);

        boolean success = changeElectionStatusForAllMachines(electionId, newStatus);

        if (success) {
            logger.info("🎉 PRUEBA EXITOSA - Estado cambiado a todas las mesas");
        } else {
            logger.error("💥 PRUEBA FALLIDA - Error cambiando estado");
        }
    }

    /**
     * Método de prueba para enviar configuración a mesa 6823
     */
    public void testSendToMesa6823() {
        logger.info("🧪 INICIANDO PRUEBA - Enviando configuración a mesa 6823");

        // Esperar un poco para que el cliente esté listo
        try {
            logger.info("⏳ Esperando 8 segundos para que el cliente esté listo...");
            Thread.sleep(8000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }

        boolean success = sendConfigurationToMachine(6823, 1);

        if (success) {
            logger.info("🎉 PRUEBA EXITOSA - Mesa 6823 configurada correctamente");
        } else {
            logger.error("💥 PRUEBA FALLIDA - Error configurando mesa 6823");

            // Intentar una vez más
            logger.info("🔄 Reintentando en 5 segundos...");
            try {
                Thread.sleep(5000);
                boolean retrySuccess = sendConfigurationToMachine(6823, 1);
                if (retrySuccess) {
                    logger.info("🎉 REINTENTO EXITOSO - Mesa 6823 configurada");
                } else {
                    logger.error("💥 REINTENTO FALLIDO - Mesa 6823 no se pudo configurar");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * ✅ MÉTODO DE DIAGNÓSTICO: Verificar conectividad con todas las mesas
     */
    public void diagnosticCheckAllMachines() {
        logger.info("🔍 DIAGNÓSTICO - Verificando conectividad con todas las mesas...");

        try {
            List<Integer> allMesaIds = votingManager.getAllMesaIds();

            if (allMesaIds.isEmpty()) {
                logger.warn("⚠️ No hay mesas registradas para diagnóstico");
                return;
            }

            int connectedCount = 0;
            int disconnectedCount = 0;

            for (Integer mesaId : allMesaIds) {
                try {
                    int port = 10020 + (mesaId % 1000);
                    String endpoint = "ConfigurationReceiver:default -h localhost -p " + port;

                    ObjectPrx base = communicator.stringToProxy(endpoint);
                    ConfigurationReceiverPrx receiver = ConfigurationReceiverPrx.checkedCast(base);

                    if (receiver != null && receiver.isReady(mesaId)) {
                        connectedCount++;
                        logger.debug("✅ Mesa {} - CONECTADA (puerto {})", mesaId, port);
                    } else {
                        disconnectedCount++;
                        logger.debug("❌ Mesa {} - DESCONECTADA (puerto {})", mesaId, port);
                    }

                } catch (Exception e) {
                    disconnectedCount++;
                    logger.debug("❌ Mesa {} - ERROR: {}", mesaId, e.getMessage());
                }
            }

            logger.info("📊 DIAGNÓSTICO COMPLETADO:");
            logger.info("   - Total mesas: {}", allMesaIds.size());
            logger.info("   - Conectadas: {} ({:.1f}%)", connectedCount,
                    (double) connectedCount / allMesaIds.size() * 100);
            logger.info("   - Desconectadas: {} ({:.1f}%)", disconnectedCount,
                    (double) disconnectedCount / allMesaIds.size() * 100);

        } catch (Exception e) {
            logger.error("❌ Error durante diagnóstico: {}", e.getMessage(), e);
        }
    }
}