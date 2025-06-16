ConfigurationSender

        package configuration;

import ConfigurationSystem.ConfigurationReceiverPrx;
import VotingMachineManager.VotingManagerImpl;
import VotingMachineManager.MesaConfigurationManager;
import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.ObjectPrx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;


public class ConfigurationSender {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationSender.class);
    private final VotingManagerImpl votingManager;
    public final Communicator communicator;


    private final MesaConfigurationManager mesaConfigManager;

    public ConfigurationSender(VotingManagerImpl votingManager, Communicator communicator) {
        this.votingManager = votingManager;
        this.communicator = communicator;
        this.mesaConfigManager = new MesaConfigurationManager();

        logger.info("🚀 ConfigurationSender inicializado");
        logRegisteredMesas();
    }


    public boolean sendConfigurationToMachine(int mesaId, int electionId) {


        try {

            if (!mesaConfigManager.isMesaRegistered(mesaId)) {

                return false;
            }


            MesaConfigurationManager.MesaInfo mesaInfo = mesaConfigManager.getMesaInfo(mesaId);


            // 1. Generar configuración de la elección
            String configurationData = votingManager.generateMachineConfigurationString(mesaId, electionId);

            if (configurationData.startsWith("ERROR")) {

                return false;
            }



            // 2. Conectar usando la información del archivo
            String endpoint = mesaInfo.getEndpoint(); // "ConfigurationReceiver:default -h localhost -p 10843"


            ObjectPrx base = communicator.stringToProxy(endpoint);
            ConfigurationReceiverPrx receiver = ConfigurationReceiverPrx.checkedCast(base);

            if (receiver == null) {

                return false;
            }

            // 3. Verificar que la mesa esté lista

            if (!receiver.isReady(mesaId)) {

                return false;
            }


            boolean success = receiver.updateConfiguration(mesaId, configurationData);

            if (success) {



                String status = receiver.getConfigurationStatus(mesaId);

            } else {

            }

            return success;

        } catch (Exception e) {

            return false;
        }
    }

    public boolean changeElectionStatusForAllMachines(int electionId, String newStatus) {


        // Obtener solo mesas activas del archivo
        List<MesaConfigurationManager.MesaInfo> activeMesas = mesaConfigManager.getActiveMesas();

        if (activeMesas.isEmpty()) {

            return false;
        }



        int successCount = 0;
        for (MesaConfigurationManager.MesaInfo mesaInfo : activeMesas) {
            boolean success = sendElectionStatusToMachine(mesaInfo, electionId, newStatus);
            if (success) {
                successCount++;

            } else {

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


        return success;
    }


    private boolean sendElectionStatusToMachine(MesaConfigurationManager.MesaInfo mesaInfo, int electionId, String newStatus) {
        try {
            String endpoint = mesaInfo.getEndpoint();
            ObjectPrx base = communicator.stringToProxy(endpoint);
            ConfigurationReceiverPrx receiver = ConfigurationReceiverPrx.checkedCast(base);

            if (receiver == null) {

                return false;
            }

            if (!receiver.isReady(mesaInfo.getId())) {

                return false;
            }

            return receiver.updateElectionStatus(electionId, newStatus);

        } catch (Exception e) {

            return false;
        }
    }




    public boolean startElectionInAllMachines(int electionId) {

        return changeElectionStatusForAllMachines(electionId, "DURING");
    }

    public boolean closeElectionInAllMachines(int electionId) {

        return changeElectionStatusForAllMachines(electionId, "CLOSED");
    }

    public boolean resetElectionInAllMachines(int electionId) {

        return changeElectionStatusForAllMachines(electionId, "PRE");
    }


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