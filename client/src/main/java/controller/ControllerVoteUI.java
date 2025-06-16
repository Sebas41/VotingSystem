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
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Properties;

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

    private static final String CONFIG_STATE_FILE = "client/config/configuration_state.properties";
    private boolean isConfiguredFromServer = false;
    private String lastConfigurationVersion = "";

    public ControllerVoteUI() throws Exception {
        initIceCommunication();
        loadConfigurationState();
        initRepositories();
        initConfigurationService();
        initUI();
    }

    private void initIceCommunication() throws Exception {
        mapper = new ObjectMapper();
        com = Util.initialize();
        rm = RMSourcePrx.checkedCast(com.stringToProxy("Sender:tcp -h 192.168.131.24 -p 10010"));
        dest = RMDestinationPrx.uncheckedCast(com.stringToProxy("Service:tcp -h 192.168.131.21 -p 10012"));
        ip = InetAddress.getLocalHost();
    }

    private void loadConfigurationState() {
        try {
            File stateFile = new File(CONFIG_STATE_FILE);
            if (stateFile.exists()) {
                Properties props = new Properties();
                props.load(new FileInputStream(stateFile));

                isConfiguredFromServer = Boolean.parseBoolean(props.getProperty("configured.from.server", "false"));
                lastConfigurationVersion = props.getProperty("last.configuration.version", "");

                System.out.println("Estado de configuración cargado:");
                System.out.println("   - Configurada desde servidor: " + isConfiguredFromServer);
                System.out.println("   - Última versión: " + lastConfigurationVersion);
            } else {
                System.out.println("No hay estado de configuración previo");
            }
        } catch (Exception e) {
            System.err.println("Error cargando estado de configuración: " + e.getMessage());
        }
    }

    private void saveConfigurationState(String version) {
        try {
            File stateFile = new File(CONFIG_STATE_FILE);
            stateFile.getParentFile().mkdirs();

            Properties props = new Properties();
            props.setProperty("configured.from.server", "true");
            props.setProperty("last.configuration.version", version);
            props.setProperty("last.update.timestamp", String.valueOf(System.currentTimeMillis()));

            props.store(new FileOutputStream(stateFile), "Configuration State - Auto Generated");

            isConfiguredFromServer = true;
            lastConfigurationVersion = version;

            System.out.println("Estado de configuración guardado: " + version);

        } catch (IOException e) {
            System.err.println("Error guardando estado de configuración: " + e.getMessage());
        }
    }

    private void initRepositories() {
        try {
            voteRepo = new VoteRepository();
            System.out.println("Repositorio de votos inicializado");
            reloadDataRepositories();
        } catch (Exception e) {
            System.err.println("Error inicializando repositorios: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void reloadDataRepositories() {
        try {
            authVoter = new AutenticationVoter();
            System.out.println("Repositorio de autenticación recargado");

            electionRepo = new ElectionRepository();
            election = electionRepo.getElection();

            if (election != null && election.getCandidates() != null) {
                System.out.println("Elección cargada: " + election.getCandidates().size() + " candidatos");
                logElectionScheduleInfo();
            } else {
                System.out.println("No hay datos de elección disponibles");
            }

        } catch (Exception e) {
            System.err.println("Error recargando repositorios de datos: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void reloadElectionRepository() {
        try {
            electionRepo = new ElectionRepository();
            election = electionRepo.getElection();

            if (election != null) {
                System.out.println("Repositorio de elección recargado");
            } else {
                System.out.println("No se pudo recargar la elección");
            }

        } catch (Exception e) {
            System.err.println("Error recargando repositorio de elección: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void logElectionScheduleInfo() {
        if (election != null) {
            String votingStatus = election.getVotingStatus();
            System.out.println("Estado de jornada electoral: " + votingStatus);
            System.out.println(election.getFormattedSchedule());

            if (votingStatus.equals("NO_INICIADA")) {
                long timeUntilOpen = election.getTimeUntilOpen();
                System.out.println("Tiempo hasta apertura: " + formatTimeRemaining(timeUntilOpen));
            } else if (votingStatus.equals("ABIERTA")) {
                long timeUntilClose = election.getTimeUntilClose();
                System.out.println("Tiempo hasta cierre: " + formatTimeRemaining(timeUntilClose));
            }
        }
    }

    private void initConfigurationService() {
        try {
            System.out.println("Inicializando servicio de configuración...");

            configurationAdapter = com.createObjectAdapterWithEndpoints(
                    "ConfigurationReceiver",
                    "tcp -h 192.168.131.22 -p 10843"
            );

            configurationReceiver = new ConfigurationReceiverImpl(this);

            configurationAdapter.add(
                    (ConfigurationReceiver) configurationReceiver,
                    Util.stringToIdentity("ConfigurationReceiver")
            );

            configurationAdapter.activate();

            System.out.println("Servicio de configuración activo:");
            System.out.println("   - Puerto: 10843");
            System.out.println("   - Identity: ConfigurationReceiver");
            System.out.println("   - Mesa ID: " + configurationReceiver.getMachineId());

        } catch (Exception e) {
            System.err.println("Error inicializando servicio de configuración: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void initUI() {
        ui = new VotingMachineUI();
        updateUIWithCurrentData();

        ui.addLoginAction(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleLogin();
            }
        });

        ui.addVoteAction(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleVote();
            }
        });

        ui.setVisible(true);
    }

    private void handleLogin() {
        try {
            String id = ui.getVoterId();
            String password = ui.getPassword();

            if (authVoter == null) {
                ui.showLoginMessage("Sistema no configurado. Esperando configuración del servidor...", true);
                return;
            }

            if (election != null && !election.canVote()) {
                String statusMessage = election.getFullVotingStatus();
                ui.showLoginMessage(statusMessage, true);
                System.out.println("Intento de login rechazado - " + statusMessage);
                return;
            }

            if (!authVoter.authenticate(id, password)) {
                ui.showLoginMessage("Credenciales incorrectas.", true);
                return;
            }

            if (authVoter.hasAlreadyVoted(id)) {
                ui.showLoginMessage("Ya has ejercido tu voto. Gracias.", true);
                return;
            }

            if (election == null || election.getCandidates() == null || election.getCandidates().isEmpty()) {
                ui.showLoginMessage("No hay elección configurada. Contacte al administrador.", true);
                return;
            }

            currentVoterId = id;
            ui.showLoginMessage("Autenticación exitosa. Redirigiendo a votación...", false);
            System.out.println("Login exitoso para votante: " + id);
            ui.showVotePanel();

        } catch (Exception ex) {
            ui.showLoginMessage("Error en autenticación: " + ex.getMessage(), true);
            ex.printStackTrace();
        }
    }

    private void handleVote() {
        try {
            String candidateId = ui.getSelectedCandidateId();
            if (candidateId == null) {
                ui.showVoteMessage("Seleccione un candidato válido.", true);
                return;
            }

            if (election != null && !election.canVote()) {
                String statusMessage = election.getFullVotingStatus();
                ui.showVoteMessage("Votación no disponible: " + statusMessage, true);
                System.out.println("Intento de voto rechazado - " + statusMessage);
                ui.resetToLoginAfterVote();
                return;
            }

            if (election == null) {
                ui.showVoteMessage("Error: No hay elección configurada.", true);
                return;
            }

            if (authVoter == null) {
                ui.showVoteMessage("Error: Sistema de autenticación no disponible.", true);
                return;
            }

            long timestamp = System.currentTimeMillis();
            Vote vote = new Vote(ip.getHostAddress(), candidateId, timestamp, election.getElectionId());

            rm.setServerProxy(dest);
            rm.sendMessage(vote);
            voteRepo.save(vote);
            authVoter.markAsVoted(currentVoterId);

            System.out.println("Voto registrado exitosamente - Votante: " + currentVoterId + ", Candidato: " + candidateId);
            ui.showVoteMessage("Gracias por votar. Su elección ha sido registrada.", false);
            ui.resetToLoginAfterVote();

        } catch (Exception ex) {
            ui.showVoteMessage("Error al emitir el voto: " + ex.getMessage(), true);
            ex.printStackTrace();
        }
    }

    private String formatTimeRemaining(long milliseconds) {
        if (milliseconds <= 0) {
            return "0 minutos";
        }

        long hours = milliseconds / (1000 * 60 * 60);
        long minutes = (milliseconds % (1000 * 60 * 60)) / (1000 * 60);

        if (hours > 0) {
            return hours + " horas y " + minutes + " minutos";
        } else {
            return minutes + " minutos";
        }
    }

    private void updateUIWithCurrentData() {
        if (election != null && election.getCandidates() != null && !election.getCandidates().isEmpty()) {
            ui.setCandidates(election.getCandidates());
            System.out.println("UI actualizada con " + election.getCandidates().size() + " candidatos");

            String votingStatus = election.getVotingStatus();
            System.out.println("Estado de jornada electoral: " + votingStatus);
            System.out.println(election.getFormattedSchedule());

            if (isConfiguredFromServer) {
                System.out.println("Mesa configurada desde servidor (versión: " + lastConfigurationVersion + ")");
            } else {
                System.out.println("Usando configuración local por defecto");
            }
        } else {
            System.out.println("No hay elección configurada. Esperando configuración del servidor...");
        }
    }

    public void onConfigurationUpdated() {
        try {
            System.out.println("Procesando nueva configuración del servidor...");

            reloadDataRepositories();

            if (election != null && election.getCandidates() != null && !election.getCandidates().isEmpty()) {
                ui.setCandidates(election.getCandidates());
                System.out.println("UI actualizada con " + election.getCandidates().size() + " candidatos");

                String version = "v" + System.currentTimeMillis();
                saveConfigurationState(version);

                ui.showLoginMessage("Configuración actualizada desde el servidor.", false);

                logElectionScheduleInfo();

                System.out.println("Configuración completada exitosamente");

            } else {
                System.out.println("No se pudo cargar la nueva configuración");
                ui.showLoginMessage("Error cargando nueva configuración.", true);
            }

        } catch (Exception e) {
            System.err.println("Error aplicando nueva configuración: " + e.getMessage());
            e.printStackTrace();
            ui.showLoginMessage("Error al aplicar nueva configuración.", true);
        }
    }

    public void onElectionStatusChanged(int electionId, String newStatus) {
        try {
            System.out.println("Procesando cambio de estado de elección " + electionId + " -> " + newStatus);

            reloadElectionRepository();

            if (election != null && election.getElectionId() == electionId) {
                logElectionStatusChange(newStatus);
                updateUIWithNewElectionStatus(newStatus);
                System.out.println("Estado de elección actualizado exitosamente a: " + newStatus);
            } else {
                System.out.println("La elección no coincide con la configuración actual");
            }

        } catch (Exception e) {
            System.err.println("Error procesando cambio de estado de elección: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void logElectionStatusChange(String newStatus) {
        if (election != null) {
            System.out.println("Estado de elección actualizado:");
            System.out.println("   - Nuevo estado: " + newStatus);
            System.out.println("   - Votación permitida: " + (election.canVote() ? "SÍ" : "NO"));
            System.out.println("   - Estado completo: " + election.getFullVotingStatus());

            if (election.getVotingStartTime() > 0) {
                System.out.println("   - Horarios: " + election.getFormattedSchedule());
            }
        }
    }

    private void updateUIWithNewElectionStatus(String newStatus) {
        if (ui != null && election != null) {
            String message;
            boolean isError;

            switch (newStatus.toUpperCase()) {
                case "PRE":
                    message = "La elección aún no ha iniciado. Votación no disponible.";
                    isError = true;
                    break;
                case "DURING":
                    if (election.canVote()) {
                        message = "Elección activa. Votación disponible.";
                        isError = false;
                    } else {
                        message = "Elección activa pero fuera del horario de votación.";
                        isError = true;
                    }
                    break;
                case "CLOSED":
                    message = "La elección ha terminado. Gracias por participar.";
                    isError = true;
                    break;
                default:
                    message = "Estado de elección actualizado: " + newStatus;
                    isError = false;
                    break;
            }

            ui.showLoginMessage(message, isError);
            System.out.println("UI actualizada con mensaje: " + message);
        }
    }

    public int getMachineId() {
        return configurationReceiver != null ? configurationReceiver.getMachineId() : -1;
    }

    public boolean isVotingAvailable() {
        if (election == null) {
            return false;
        }
        return election.canVote();
    }

    public String getCurrentVotingStatus() {
        if (election == null) {
            return "NO_CONFIGURADA";
        }
        return election.getFullVotingStatus();
    }

    public void shutdown() {
        try {
            System.out.println("Cerrando servicios del controller...");

            if (configurationAdapter != null) {
                configurationAdapter.deactivate();
                System.out.println("Adapter de configuración desactivado");
            }

            if (com != null) {
                com.shutdown();
                System.out.println("Comunicador Ice cerrado");
            }

            System.out.println("Servicios cerrados correctamente");

        } catch (Exception e) {
            System.err.println("Error cerrando servicios: " + e.getMessage());
        }
    }

    public boolean isConfiguredFromServer() {
        return isConfiguredFromServer;
    }

    public String getConfigurationVersion() {
        return lastConfigurationVersion;
    }

    public void forceReloadConfiguration() {
        System.out.println("Forzando recarga de configuración...");
        onConfigurationUpdated();
    }

    public Election getCurrentElection() {
        return election;
    }

    public void debugScheduleInfo() {
        if (election != null) {
            System.out.println("DEBUG - Información de horarios:");
            System.out.println("   - Estado: " + election.getVotingStatus());
            System.out.println("   - Horario: " + election.getFormattedSchedule());
            System.out.println("   - Inicio: " + new java.util.Date(election.getVotingStartTime()));
            System.out.println("   - Fin: " + new java.util.Date(election.getVotingEndTime()));
            System.out.println("   - Ahora: " + new java.util.Date());
        } else {
            System.out.println("DEBUG - No hay elección configurada");
        }
    }
}