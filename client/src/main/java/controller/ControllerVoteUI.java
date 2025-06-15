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

/**
 * ✅ CONTROLADOR COMPLETO CON:
 * - Configuración remota
 * - Persistencia del estado de configuración
 * - Recarga automática de repositorios
 * - Manejo robusto de errores
 */
public class ControllerVoteUI {
    // =================== COMPONENTES PRINCIPALES ===================
    private VotationInterface voteRepo;
    private AutenticationVoterInterface authVoter;
    private ElectionInterface electionRepo;
    private Election election;

    // =================== COMUNICACIÓN ICE ===================
    private Communicator com;
    private RMSourcePrx rm;
    private RMDestinationPrx dest;
    private ObjectMapper mapper;

    // =================== UI Y DATOS ===================
    private InetAddress ip;
    private VotingMachineUI ui;
    private String currentVoterId;

    // =================== CONFIGURACIÓN REMOTA ===================
    private ConfigurationReceiverImpl configurationReceiver;
    private ObjectAdapter configurationAdapter;

    // ✅ NUEVO: Estado de configuración persistente
    private static final String CONFIG_STATE_FILE = "client/config/configuration_state.properties";
    private boolean isConfiguredFromServer = false;
    private String lastConfigurationVersion = "";

    public ControllerVoteUI() throws Exception {
        // ✅ 1. Inicializar comunicación Ice primero
        initIceCommunication();

        // ✅ 2. Cargar estado de configuración persistente
        loadConfigurationState();

        // ✅ 3. Inicializar repositorios (pueden estar vacíos al inicio)
        initRepositories();

        // ✅ 4. Configurar servicio de configuración remota
        initConfigurationService();

        // ✅ 5. Inicializar UI
        initUI();
    }

    /**
     * ✅ NUEVO: Inicializa la comunicación Ice por separado
     */
    private void initIceCommunication() throws Exception {
        mapper = new ObjectMapper();
        com = Util.initialize();
        rm = RMSourcePrx.checkedCast(com.stringToProxy("Sender:tcp -h localhost -p 10010"));
        dest = RMDestinationPrx.uncheckedCast(com.stringToProxy("Service:tcp -h localhost -p 10012"));
        ip = InetAddress.getLocalHost();
    }

    /**
     * ✅ NUEVO: Carga el estado de configuración persistente
     */
    private void loadConfigurationState() {
        try {
            File stateFile = new File(CONFIG_STATE_FILE);
            if (stateFile.exists()) {
                Properties props = new Properties();
                props.load(new FileInputStream(stateFile));

                isConfiguredFromServer = Boolean.parseBoolean(props.getProperty("configured.from.server", "false"));
                lastConfigurationVersion = props.getProperty("last.configuration.version", "");

                System.out.println("📁 Estado de configuración cargado:");
                System.out.println("   - Configurada desde servidor: " + isConfiguredFromServer);
                System.out.println("   - Última versión: " + lastConfigurationVersion);
            } else {
                System.out.println("📁 No hay estado de configuración previo");
            }
        } catch (Exception e) {
            System.err.println("⚠️ Error cargando estado de configuración: " + e.getMessage());
        }
    }

    /**
     * ✅ NUEVO: Guarda el estado de configuración
     */
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

            System.out.println("💾 Estado de configuración guardado: " + version);

        } catch (IOException e) {
            System.err.println("❌ Error guardando estado de configuración: " + e.getMessage());
        }
    }

    /**
     * ✅ MEJORADO: Inicializa repositorios con manejo robusto de errores
     */
    private void initRepositories() {
        try {
            // ✅ Inicializar siempre el repositorio de votos
            voteRepo = new VoteRepository();
            System.out.println("✅ Repositorio de votos inicializado");

            // ✅ Intentar cargar datos de elección y votantes
            reloadDataRepositories();

        } catch (Exception e) {
            System.err.println("⚠️ Error inicializando repositorios: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * ✅ NUEVO: Recarga SOLO los repositorios de datos (elección y votantes)
     */
    private void reloadDataRepositories() {
        try {
            // ✅ Recargar repositorio de autenticación
            authVoter = new AutenticationVoter();
            System.out.println("🔄 Repositorio de autenticación recargado");

            // ✅ Recargar repositorio de elección
            electionRepo = new ElectionRepository();
            election = electionRepo.getElection();

            if (election != null && election.getCandidates() != null) {
                System.out.println("🗳️ Elección cargada: " + election.getCandidates().size() + " candidatos");
            } else {
                System.out.println("⚠️ No hay datos de elección disponibles");
            }

        } catch (Exception e) {
            System.err.println("❌ Error recargando repositorios de datos: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * ✅ MEJORADO: Servicio de configuración con puerto correcto
     */
    private void initConfigurationService() {
        try {
            System.out.println("🔧 Inicializando servicio de configuración...");

            // ✅ PUERTO CORREGIDO: 10843 para mesa 6823
            configurationAdapter = com.createObjectAdapterWithEndpoints(
                    "ConfigurationReceiver",
                    "tcp -h localhost -p 10843"
            );

            // Crear implementación del servicio
            configurationReceiver = new ConfigurationReceiverImpl(this);

            // Registrar servicio con el identity correcto
            configurationAdapter.add(
                    (ConfigurationReceiver) configurationReceiver,
                    Util.stringToIdentity("ConfigurationReceiver")
            );

            // Activar adapter
            configurationAdapter.activate();

            System.out.println("✅ Servicio de configuración activo:");
            System.out.println("   - Puerto: 10843");
            System.out.println("   - Identity: ConfigurationReceiver");
            System.out.println("   - Mesa ID: " + configurationReceiver.getMachineId());

        } catch (Exception e) {
            System.err.println("❌ Error inicializando servicio de configuración: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * ✅ MEJORADO: UI con mejor manejo de estados
     */
    private void initUI() {
        ui = new VotingMachineUI();

        // ✅ Verificar estado inicial y mostrar información apropiada
        updateUIWithCurrentData();

        // =================== ACCIÓN DE LOGIN ===================
        ui.addLoginAction(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleLogin();
            }
        });

        // =================== ACCIÓN DE VOTAR ===================
        ui.addVoteAction(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleVote();
            }
        });

        ui.setVisible(true);
    }

    /**
     * ✅ NUEVO: Maneja el proceso de login de forma separada
     */
    private void handleLogin() {
        try {
            String id = ui.getVoterId();
            String password = ui.getPassword();

            // ✅ Verificar que los repositorios estén listos
            if (authVoter == null) {
                ui.showLoginMessage("Sistema no configurado. Esperando configuración del servidor...", true);
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

            // ✅ Verificaciones de seguridad
            if (election == null) {
                ui.showVoteMessage("Error: No hay elección configurada.", true);
                return;
            }

            if (authVoter == null) {
                ui.showVoteMessage("Error: Sistema de autenticación no disponible.", true);
                return;
            }

            // ✅ Enviar voto
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
            ex.printStackTrace();
        }
    }

    /**
     * ✅ NUEVO: Actualiza la UI con los datos actuales
     */
    private void updateUIWithCurrentData() {
        if (election != null && election.getCandidates() != null && !election.getCandidates().isEmpty()) {
            ui.setCandidates(election.getCandidates());
            System.out.println("📊 UI actualizada con " + election.getCandidates().size() + " candidatos");

            if (isConfiguredFromServer) {
                System.out.println("✅ Mesa configurada desde servidor (versión: " + lastConfigurationVersion + ")");
            } else {
                System.out.println("📝 Usando configuración local por defecto");
            }
        } else {
            System.out.println("⚠️ No hay elección configurada. Esperando configuración del servidor...");
        }
    }

    /**
     * ✅ MEJORADO: Método llamado cuando se actualiza la configuración
     * Ahora con persistencia y mejor manejo de errores
     */
    public void onConfigurationUpdated() {
        try {
            System.out.println("🔄 Procesando nueva configuración del servidor...");

            // ✅ 1. Recargar repositorios de datos
            reloadDataRepositories();

            // ✅ 2. Actualizar UI con nuevos datos
            if (election != null && election.getCandidates() != null && !election.getCandidates().isEmpty()) {
                ui.setCandidates(election.getCandidates());
                System.out.println("✅ UI actualizada con " + election.getCandidates().size() + " candidatos");

                // ✅ 3. Guardar estado de configuración
                String version = "v" + System.currentTimeMillis();
                saveConfigurationState(version);

                // ✅ 4. Mostrar mensaje de éxito en la UI
                ui.showLoginMessage("✅ Configuración actualizada desde el servidor.", false);

                System.out.println("🎉 Configuración completada exitosamente");

            } else {
                System.out.println("⚠️ No se pudo cargar la nueva configuración");
                ui.showLoginMessage("❌ Error cargando nueva configuración.", true);
            }

        } catch (Exception e) {
            System.err.println("❌ Error aplicando nueva configuración: " + e.getMessage());
            e.printStackTrace();
            ui.showLoginMessage("❌ Error al aplicar nueva configuración.", true);
        }
    }

    /**
     * ✅ Método para obtener el ID de la mesa
     */
    public int getMachineId() {
        return configurationReceiver != null ? configurationReceiver.getMachineId() : -1;
    }

    /**
     * ✅ MEJORADO: Cleanup completo cuando se cierra la aplicación
     */
    public void shutdown() {
        try {
            System.out.println("🛑 Cerrando servicios del controller...");

            if (configurationAdapter != null) {
                configurationAdapter.deactivate();
                System.out.println("   ✅ Adapter de configuración desactivado");
            }

            if (com != null) {
                com.shutdown();
                System.out.println("   ✅ Comunicador Ice cerrado");
            }

            System.out.println("🛑 Servicios cerrados correctamente");

        } catch (Exception e) {
            System.err.println("⚠️ Error cerrando servicios: " + e.getMessage());
        }
    }


    public boolean isConfiguredFromServer() {
        return isConfiguredFromServer;
    }


    public String getConfigurationVersion() {
        return lastConfigurationVersion;
    }

    public void forceReloadConfiguration() {
        System.out.println("🔄 Forzando recarga de configuración...");
        onConfigurationUpdated();
    }
}