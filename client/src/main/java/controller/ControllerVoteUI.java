package controller;

import Autentication.AutenticationVoter;
import Autentication.AutenticationVoterInterface;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.ObjectAdapter;
import com.zeroc.Ice.Util;
import configuration.ConfigurationReceiverImpl;
import ConfigurationSystem.ConfigurationReceiver;
import model.Vote;
import reliableMessage.RMDestinationPrx;
import reliableMessage.RMSourcePrx;
import ui.VotingMachineUI;
import votation.*;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.InetAddress;

/**
 * Controlador actualizado que incluye el servicio de configuración remota
 */
public class ControllerVoteUI {
    private VotationInterface voteRepo;
    private AutenticationVoterInterface authVoter;

    private ElectionInterface electionRepo;
    private Election election;

    private Communicator com;
    private RMSourcePrx rm;
    private RMDestinationPrx dest;
    private ObjectMapper mapper;

    private InetAddress ip;
    private VotingMachineUI ui;
    private String currentVoterId;


    private ConfigurationReceiverImpl configurationReceiver;
    private ObjectAdapter configurationAdapter;

    public ControllerVoteUI() throws Exception {
        voteRepo = new VoteRepository();
        authVoter = new AutenticationVoter();

        electionRepo = new ElectionRepository();
        election = electionRepo.getElection();

        mapper = new ObjectMapper();
        com = Util.initialize();
        rm = RMSourcePrx.checkedCast(com.stringToProxy("Sender:tcp -h localhost -p 10010"));
        dest = RMDestinationPrx.uncheckedCast(com.stringToProxy("Service:tcp -h localhost -p 10012"));
        ip = InetAddress.getLocalHost();


        initConfigurationService();

        initUI();
    }


    private void initConfigurationService() {
        try {
            System.out.println("🔧 Inicializando servicio de configuración...");

            // ✅ PUERTO CORREGIDO: 10843 para mesa 6823
            configurationAdapter = com.createObjectAdapterWithEndpoints(
                    "ConfigurationReceiver",
                    "tcp -h localhost -p 10843"  // ✅ Puerto específico para mesa 6823
            );

            // Crear implementación del servicio
            configurationReceiver = new ConfigurationReceiverImpl(this);

            // Registrar servicio con el identity correcto
            configurationAdapter.add(
                    (ConfigurationReceiver) configurationReceiver,
                    Util.stringToIdentity("ConfigurationReceiver")  // ✅ Identity correcto
            );

            // Activar adapter
            configurationAdapter.activate();

            System.out.println("✅ Servicio de configuración activo:");
            System.out.println("   - Puerto: 10843");  // ✅ Puerto correcto
            System.out.println("   - Identity: ConfigurationReceiver");  // ✅ Identity correcto
            System.out.println("   - Mesa ID: " + configurationReceiver.getMachineId());

        } catch (Exception e) {
            System.err.println("❌ Error inicializando servicio de configuración: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void initUI() {
        ui = new VotingMachineUI();

        // ✅ VERIFICAR SI HAY ELECCIÓN CARGADA
        if (election != null && election.getCandidates() != null) {
            ui.setCandidates(election.getCandidates());
            System.out.println("📊 Elección cargada: " + election.getCandidates().size() + " candidatos");
        } else {
            System.out.println("⚠️ No hay elección configurada. Esperando configuración del servidor...");
        }

        // Acción de login
        ui.addLoginAction(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String id = ui.getVoterId();
                String password = ui.getPassword();

                if (!authVoter.authenticate(id, password)) {
                    ui.showLoginMessage("Credenciales incorrectas.", true);
                    return;
                }

                if (authVoter.hasAlreadyVoted(id)) {
                    ui.showLoginMessage("Ya has ejercido su voto. Gracias.", true);
                    return;
                }

                currentVoterId = id;
                ui.showLoginMessage("Autenticación exitosa. Redirigiendo a votación...", false);
                ui.showVotePanel();
            }
        });

        // Acción de votar
        ui.addVoteAction(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    String candidateId = ui.getSelectedCandidateId();
                    if (candidateId == null) {
                        ui.showVoteMessage("Seleccione un candidato válido.", true);
                        return;
                    }

                    // ✅ VERIFICAR QUE HAY ELECCIÓN CONFIGURADA
                    if (election == null) {
                        ui.showVoteMessage("Error: No hay elección configurada.", true);
                        return;
                    }

                    long timestamp = System.currentTimeMillis();
                    Vote vote = new Vote(ip.getHostAddress(), candidateId, timestamp, election.getElectionId());
                    rm.setServerProxy(dest);
                    rm.sendMessage(vote);
                    voteRepo.save(vote);
                    authVoter.markAsVoted(currentVoterId);
                    ui.showVoteMessage("Gracias por votar. Su elección ha sido registrada.", false);

                    ui.resetToLoginAfterVote();

                } catch (Exception ex) {
                    ui.showVoteMessage("Error al emitir el voto: " + ex.getMessage(), true);
                }
            }
        });

        ui.setVisible(true);
    }

    /**
     * ✅ NUEVO: Método llamado cuando se actualiza la configuración
     * Recarga los datos y actualiza la UI
     */
    public void onConfigurationUpdated() {
        try {
            System.out.println("🔄 Recargando configuración...");

            // Recargar repositorios desde archivos actualizados
            electionRepo = new ElectionRepository();
            election = electionRepo.getElection();

            authVoter = new AutenticationVoter(); // Recarga voters.json

            // Actualizar UI con nuevos candidatos
            if (election != null && election.getCandidates() != null) {
                ui.setCandidates(election.getCandidates());
                System.out.println("✅ UI actualizada con " + election.getCandidates().size() + " candidatos");

                // Mostrar mensaje en la UI
                ui.showLoginMessage("Configuración actualizada desde el servidor.", false);
            } else {
                System.out.println("⚠️ No se pudo cargar la nueva configuración");
                ui.showLoginMessage("Error cargando nueva configuración.", true);
            }

        } catch (Exception e) {
            System.err.println("❌ Error recargando configuración: " + e.getMessage());
            e.printStackTrace();
            ui.showLoginMessage("Error al aplicar nueva configuración.", true);
        }
    }

    /**
     * ✅ NUEVO: Método para obtener el ID de la mesa
     */
    public int getMachineId() {
        return configurationReceiver != null ? configurationReceiver.getMachineId() : -1;
    }

    /**
     * ✅ NUEVO: Cleanup cuando se cierra la aplicación
     */
    public void shutdown() {
        try {
            if (configurationAdapter != null) {
                configurationAdapter.deactivate();
            }
            if (com != null) {
                com.shutdown();
            }
            System.out.println("🛑 Servicios cerrados correctamente");
        } catch (Exception e) {
            System.err.println("⚠️ Error cerrando servicios: " + e.getMessage());
        }
    }
}