package configuration;

import ConfigurationSystem.ConfigurationReceiverPrx;
import VotingMachineManager.VotingManagerImpl;
import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.ObjectPrx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servicio para enviar configuraciones a mesas de votación remotas
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
}