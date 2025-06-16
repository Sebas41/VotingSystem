package configuration;

import ConfigurationSystem.ConfigurationReceiverPrx;
import VotingMachineManager.VotingManagerImpl;
import VotingMachineManager.MesaConfigurationManager;
import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.ObjectPrx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 📤 CONFIGURATION SENDER - SOLO MESAS REGISTRADAS
 *
 * ✅ COMPORTAMIENTO:
 * - Lee archivo "configMachines/mesas-config.properties"
 * - Solo envía configuraciones a mesas que estén en el archivo
 * - Por ahora: solo mesa 6823 (localhost:10843)
 */
public class ConfigurationSender {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationSender.class);
    private final VotingManagerImpl votingManager;
    public final Communicator communicator;

    // ✅ Gestor de configuración de mesas
    private final MesaConfigurationManager mesaConfigManager;

    public ConfigurationSender(VotingManagerImpl votingManager, Communicator communicator) {
        this.votingManager = votingManager;
        this.communicator = communicator;
        this.mesaConfigManager = new MesaConfigurationManager();

        logger.info("🚀 ConfigurationSender inicializado");
        logRegisteredMesas();
    }

    /**
     * ✅ MÉTODO PRINCIPAL: Envía configuración solo si la mesa está registrada
     */
    public boolean sendConfigurationToMachine(int mesaId, int electionId) {
        logger.info("📤 Solicitando envío de configuración a mesa {} para elección {}", mesaId, electionId);

        try {
            // ✅ VALIDACIÓN: Verificar si la mesa está registrada
            if (!mesaConfigManager.isMesaRegistered(mesaId)) {
                logger.error("❌ Mesa {} NO está registrada en el archivo de configuración", mesaId);
                logger.error("   Solo se pueden configurar mesas registradas en: configMachines/mesas-config.properties");
                return false;
            }

            // ✅ Obtener información de la mesa desde el archivo
            MesaConfigurationManager.MesaInfo mesaInfo = mesaConfigManager.getMesaInfo(mesaId);
            logger.info("📋 Mesa {} encontrada en configuración:", mesaId);
            logger.info("   - Nombre: {}", mesaInfo.getName());
            logger.info("   - IP: {}", mesaInfo.getIp());
            logger.info("   - Puerto: {}", mesaInfo.getPort());
            logger.info("   - Activa: {}", mesaInfo.isActive());

            // 1. Generar configuración de la elección
            String configurationData = votingManager.generateMachineConfigurationString(mesaId, electionId);

            if (configurationData.startsWith("ERROR")) {
                logger.error("❌ Error generando configuración: {}", configurationData);
                return false;
            }

            logger.info("📦 Configuración generada - {} caracteres", configurationData.length());

            // 2. Conectar usando la información del archivo
            String endpoint = mesaInfo.getEndpoint(); // "ConfigurationReceiver:default -h localhost -p 10843"
            logger.info("🔗 Conectando a endpoint: {}", endpoint);

            ObjectPrx base = communicator.stringToProxy(endpoint);
            ConfigurationReceiverPrx receiver = ConfigurationReceiverPrx.checkedCast(base);

            if (receiver == null) {
                logger.error("❌ No se pudo conectar con la mesa {} en {}:{}",
                        mesaId, mesaInfo.getIp(), mesaInfo.getPort());
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
                logger.info("✅ Configuración enviada exitosamente a mesa {} ({})", mesaId, mesaInfo.getName());

                // Verificar estado final
                String status = receiver.getConfigurationStatus(mesaId);
                logger.info("📊 Estado final mesa {}: {}", mesaId, status);
            } else {
                logger.error("❌ Error enviando configuración a mesa {}", mesaId);
            }

            return success;

        } catch (Exception e) {
            logger.error("❌ Error crítico enviando configuración a mesa {}: {}", mesaId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * ✅ CAMBIO DE ESTADO: Solo a mesas registradas
     */
    public boolean changeElectionStatusForAllMachines(int electionId, String newStatus) {
        logger.info("🚀 Cambiando estado de elección {} a {} en mesas registradas", electionId, newStatus);

        // Obtener solo mesas activas del archivo
        List<MesaConfigurationManager.MesaInfo> activeMesas = mesaConfigManager.getActiveMesas();

        if (activeMesas.isEmpty()) {
            logger.warn("⚠️ No hay mesas activas registradas en el archivo de configuración");
            return false;
        }

        logger.info("📋 Enviando cambio de estado a {} mesas registradas", activeMesas.size());

        int successCount = 0;
        for (MesaConfigurationManager.MesaInfo mesaInfo : activeMesas) {
            boolean success = sendElectionStatusToMachine(mesaInfo, electionId, newStatus);
            if (success) {
                successCount++;
                logger.info("✅ Mesa {} ({}) actualizada", mesaInfo.getId(), mesaInfo.getName());
            } else {
                logger.warn("❌ Error actualizando mesa {} ({})", mesaInfo.getId(), mesaInfo.getName());
            }

            // Pausa entre envíos
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        boolean success = successCount > 0;
        logger.info("📊 Cambio de estado completado: {}/{} mesas actualizadas",
                successCount, activeMesas.size());

        return success;
    }

    /**
     * ✅ HELPER: Envía cambio de estado a una mesa específica
     */
    private boolean sendElectionStatusToMachine(MesaConfigurationManager.MesaInfo mesaInfo, int electionId, String newStatus) {
        try {
            String endpoint = mesaInfo.getEndpoint();
            ObjectPrx base = communicator.stringToProxy(endpoint);
            ConfigurationReceiverPrx receiver = ConfigurationReceiverPrx.checkedCast(base);

            if (receiver == null) {
                logger.warn("❌ No se pudo conectar con mesa {}", mesaInfo.getId());
                return false;
            }

            if (!receiver.isReady(mesaInfo.getId())) {
                logger.warn("⚠️ Mesa {} no está lista", mesaInfo.getId());
                return false;
            }

            return receiver.updateElectionStatus(electionId, newStatus);

        } catch (Exception e) {
            logger.error("❌ Error enviando estado a mesa {}: {}", mesaInfo.getId(), e.getMessage());
            return false;
        }
    }

    /**
     * ✅ HELPER: Muestra las mesas registradas al iniciar
     */
    private void logRegisteredMesas() {
        List<MesaConfigurationManager.MesaInfo> allMesas =
                (List<MesaConfigurationManager.MesaInfo>) mesaConfigManager.getAllMesas();

        logger.info("📋 ========== MESAS REGISTRADAS ==========");
        if (allMesas.isEmpty()) {
            logger.warn("   ⚠️ No hay mesas registradas en el archivo de configuración");
        } else {
            for (MesaConfigurationManager.MesaInfo mesa : allMesas) {
                logger.info("   Mesa {}: {} - {}:{} (Activa: {})",
                        mesa.getId(), mesa.getName(), mesa.getIp(), mesa.getPort(), mesa.isActive());
            }
        }
        logger.info("==========================================");
    }

    /**
     * ✅ MÉTODOS DE CONVENIENCIA
     */
    public boolean startElectionInAllMachines(int electionId) {
        logger.info("🗳️ Iniciando elección {} en mesas registradas...", electionId);
        return changeElectionStatusForAllMachines(electionId, "DURING");
    }

    public boolean closeElectionInAllMachines(int electionId) {
        logger.info("🔒 Cerrando elección {} en mesas registradas...", electionId);
        return changeElectionStatusForAllMachines(electionId, "CLOSED");
    }

    public boolean resetElectionInAllMachines(int electionId) {
        logger.info("⏪ Reseteando elección {} en mesas registradas...", electionId);
        return changeElectionStatusForAllMachines(electionId, "PRE");
    }

    /**
     * ✅ INFORMACIÓN: Obtiene estadísticas de mesas registradas
     */
    public void showRegisteredMesasInfo() {
        List<MesaConfigurationManager.MesaInfo> allMesas =
                (List<MesaConfigurationManager.MesaInfo>) mesaConfigManager.getAllMesas();

        System.out.println("📊 ========== INFORMACIÓN DE MESAS ==========");
        System.out.println("Total mesas registradas: " + allMesas.size());

        for (MesaConfigurationManager.MesaInfo mesa : allMesas) {
            System.out.println(String.format("Mesa %d (%s):", mesa.getId(), mesa.getName()));
            System.out.println("  - Dirección: " + mesa.getIp() + ":" + mesa.getPort());
            System.out.println("  - Departamento: " + mesa.getDepartment());
            System.out.println("  - Estado: " + (mesa.isActive() ? "ACTIVA" : "INACTIVA"));
            System.out.println("  - Endpoint: " + mesa.getEndpoint());
        }
        System.out.println("============================================");
    }
}