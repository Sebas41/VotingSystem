package configuration;

import Autentication.AlreadyVote;
import Autentication.Voter;
import ConfigurationSystem.ConfigurationReceiver;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zeroc.Ice.Current;
import controller.ControllerVoteUI;
import votation.Candidate;
import votation.Election;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Implementación del servicio de configuración para mesas de votación
 * Recibe configuraciones del servidor y actualiza automáticamente la mesa
 */
public class ConfigurationReceiverImpl implements ConfigurationReceiver {


    private static final String MACHINE_CONFIG_FILE = "machine.properties"; // Buscar en resources
    private static final String VOTERS_JSON_PATH = "data/voters.json";       // Relativo al working directory
    private static final String ELECTION_JSON_PATH = "data/election.json";   // Relativo al working directory

    private static final String FIELD_SEPARATOR = "-";
    private static final String RECORD_SEPARATOR = "#";
    private static final String ARRAY_SEPARATOR = "\\|";
    private static final String CANDIDATE_SEPARATOR = ":";

    private int machineId;
    private ControllerVoteUI controller;
    private ObjectMapper mapper;
    private boolean isConfigured = false;

    public ConfigurationReceiverImpl(ControllerVoteUI controller) {
        this.controller = controller;
        this.mapper = new ObjectMapper();
        this.machineId = loadMachineId();

        System.out.println("📡 ConfigurationReceiver inicializado para mesa: " + machineId);
    }

    @Override
    public boolean updateConfiguration(int mesaId, String configurationData, Current current) {
        System.out.println("🔄 Recibiendo configuración para mesa " + mesaId);

        try {
            // Validar que la configuración es para esta mesa
            if (mesaId != this.machineId) {
                System.out.println("❌ Configuración rechazada - Mesa ID no coincide: esperado=" +
                        this.machineId + ", recibido=" + mesaId);
                return false;
            }

            // Validar formato del string de configuración
            if (configurationData == null || configurationData.trim().isEmpty()) {
                System.out.println("❌ Configuración rechazada - Datos vacíos");
                return false;
            }

            // Verificar si es un error del servidor
            if (configurationData.startsWith("ERROR")) {
                System.out.println("❌ Error del servidor: " + configurationData);
                return false;
            }

            // Parsear y aplicar configuración
            boolean success = parseAndApplyConfiguration(configurationData);

            if (success) {
                isConfigured = true;
                System.out.println("✅ Configuración aplicada exitosamente para mesa " + mesaId);

                // Notificar al controller que la configuración cambió
                if (controller != null) {
                    controller.onConfigurationUpdated();
                }
            } else {
                System.out.println("❌ Error aplicando configuración para mesa " + mesaId);
            }

            return success;

        } catch (Exception e) {
            System.err.println("❌ Error procesando configuración: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean isReady(int mesaId, Current current) {
        boolean ready = mesaId == this.machineId && controller != null;
        System.out.println("🔍 Mesa " + mesaId + " ready check: " + ready);
        return ready;
    }

    @Override
    public String getConfigurationStatus(int mesaId, Current current) {
        if (mesaId != this.machineId) {
            return "ERROR-Wrong mesa ID";
        }

        if (isConfigured) {
            return "CONFIGURED";
        } else {
            return "NOT_CONFIGURED";
        }
    }

    /**
     * Parsea el string de configuración y actualiza los archivos locales
     */
    private boolean parseAndApplyConfiguration(String configurationData) {
        try {
            System.out.println("🔍 Parseando configuración...");
            System.out.println("📦 Datos recibidos: " + configurationData.length() + " caracteres");

            // Dividir el string por secciones
            String[] sections = configurationData.split(RECORD_SEPARATOR);

            if (sections.length < 5) {
                System.out.println("❌ Formato de configuración inválido - secciones insuficientes: " + sections.length);
                return false;
            }

            // Parsear cada sección
            String mesaInfo = sections[0];        // MESA_INFO
            String electionInfo = sections[1];    // ELECTION_INFO
            String candidatesInfo = sections[2];  // CANDIDATES
            String citizensInfo = sections[3];    // CITIZENS
            String metadata = sections[4];        // METADATA

            System.out.println("📊 Secciones parseadas:");
            System.out.println("   - Mesa: " + mesaInfo);
            System.out.println("   - Elección: " + electionInfo);
            System.out.println("   - Candidatos: " + candidatesInfo.length() + " chars");
            System.out.println("   - Ciudadanos: " + citizensInfo.length() + " chars");
            System.out.println("   - Metadata: " + metadata);

            // 1. Procesar información de la mesa (opcional, para logs)
            processMesaInfo(mesaInfo);

            // 2. Procesar y actualizar elección y candidatos
            boolean electionSuccess = processElectionAndCandidates(electionInfo, candidatesInfo);

            // 3. Procesar y actualizar ciudadanos/votantes
            boolean votersSuccess = processVoters(citizensInfo);

            // 4. Procesar metadata (opcional)
            processMetadata(metadata);

            return electionSuccess && votersSuccess;

        } catch (Exception e) {
            System.err.println("❌ Error parseando configuración: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Procesa información de la mesa
     */
    private void processMesaInfo(String mesaInfo) {
        try {
            String[] parts = mesaInfo.split(FIELD_SEPARATOR);
            if (parts.length >= 10) {
                System.out.println("🏢 Mesa configurada:");
                System.out.println("   - ID: " + parts[0]);
                System.out.println("   - Consecutivo: " + parts[1]);
                System.out.println("   - Puesto: " + parts[3] + " (" + parts[4] + ")");
                System.out.println("   - Municipio: " + parts[6]);
                System.out.println("   - Departamento: " + parts[8]);
                System.out.println("   - Total ciudadanos: " + parts[9]);
            }
        } catch (Exception e) {
            System.err.println("⚠️ Error procesando info de mesa: " + e.getMessage());
        }
    }

    /**
     * Procesa información de elección y candidatos, actualiza election.json
     */
    private boolean processElectionAndCandidates(String electionInfo, String candidatesInfo) {
        try {
            // Parsear información de la elección
            String[] electionParts = electionInfo.split(FIELD_SEPARATOR);
            if (electionParts.length < 5) {
                System.out.println("❌ Formato de elección inválido");
                return false;
            }

            int electionId = Integer.parseInt(electionParts[0]);
            String electionName = electionParts[1];

            System.out.println("🗳️ Procesando elección: " + electionName + " (ID: " + electionId + ")");

            // Parsear candidatos
            List<Candidate> candidates = new ArrayList<>();

            if (!candidatesInfo.trim().isEmpty()) {
                String[] candidateStrings = candidatesInfo.split(ARRAY_SEPARATOR);

                for (String candidateStr : candidateStrings) {
                    String[] candidateParts = candidateStr.split(CANDIDATE_SEPARATOR);
                    if (candidateParts.length >= 3) {
                        int candidateId = Integer.parseInt(candidateParts[0]);
                        String candidateName = candidateParts[1];
                        String candidateParty = candidateParts[2];

                        candidates.add(new Candidate(candidateId, candidateName, candidateParty));
                    }
                }
            }

            System.out.println("👥 Candidatos procesados: " + candidates.size());
            for (Candidate c : candidates) {
                System.out.println("   - " + c.toString());
            }

            // Crear objeto Election y guardarlo
            Election election = new Election(electionId, candidates);

            // Asegurar que el directorio existe
            File file = new File(ELECTION_JSON_PATH);
            file.getParentFile().mkdirs();

            // Guardar como JSON
            mapper.writerWithDefaultPrettyPrinter().writeValue(file, election);

            System.out.println("✅ Archivo election.json actualizado en: " + file.getAbsolutePath());
            return true;

        } catch (Exception e) {
            System.err.println("❌ Error procesando elección y candidatos: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Procesa ciudadanos/votantes, actualiza voters.json
     */
    private boolean processVoters(String citizensInfo) {
        try {
            List<Voter> voters = new ArrayList<>();

            if (!citizensInfo.trim().isEmpty()) {
                String[] citizenStrings = citizensInfo.split(ARRAY_SEPARATOR);

                for (String citizenStr : citizenStrings) {
                    String[] citizenParts = citizenStr.split(CANDIDATE_SEPARATOR);
                    if (citizenParts.length >= 4) {
                        String citizenId = citizenParts[1]; // documento
                        String firstName = citizenParts[2];
                        String lastName = citizenParts[3];
                        String fullName = firstName + " " + lastName;

                        // Generar contraseña por defecto (puedes cambiar esta lógica)
                        String password = "pass" + citizenId.substring(Math.max(0, citizenId.length() - 4));

                        voters.add(new Voter(citizenId, fullName, password, AlreadyVote.NO));
                    }
                }
            }

            System.out.println("👤 Votantes procesados: " + voters.size());

            // Asegurar que el directorio existe
            File file = new File(VOTERS_JSON_PATH);
            file.getParentFile().mkdirs();

            // Guardar como JSON
            mapper.writerWithDefaultPrettyPrinter().writeValue(file, voters);

            System.out.println("✅ Archivo voters.json actualizado en: " + file.getAbsolutePath());
            return true;

        } catch (Exception e) {
            System.err.println("❌ Error procesando votantes: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Procesa metadata de la configuración
     */
    private void processMetadata(String metadata) {
        try {
            String[] parts = metadata.split(FIELD_SEPARATOR);
            if (parts.length >= 2) {
                String version = parts[0];
                long timestamp = Long.parseLong(parts[1]);

                System.out.println("ℹ️ Metadata:");
                System.out.println("   - Versión: " + version);
                System.out.println("   - Timestamp: " + timestamp + " (" + new java.util.Date(timestamp) + ")");
            }
        } catch (Exception e) {
            System.err.println("⚠️ Error procesando metadata: " + e.getMessage());
        }
    }

    /**
     * ✅ MÉTODO MEJORADO: Carga el ID de la máquina desde resources o archivos
     */
    private int loadMachineId() {
        try {
            Properties props = new Properties();

            // 1. Intentar cargar desde resources primero
            try (InputStream is = getClass().getClassLoader().getResourceAsStream(MACHINE_CONFIG_FILE)) {
                if (is != null) {
                    props.load(is);
                    int machineId = Integer.parseInt(props.getProperty("machine.id", "6823"));
                    System.out.println("📁 Configuración cargada desde resources: machine.id=" + machineId);
                    return machineId;
                }
            }

            // 2. Si no está en resources, buscar en el directorio de trabajo
            File configFile = new File("client/config/" + MACHINE_CONFIG_FILE);
            if (configFile.exists()) {
                props.load(new FileInputStream(configFile));
                int machineId = Integer.parseInt(props.getProperty("machine.id", "6823"));
                System.out.println("📁 Configuración cargada desde archivo: machine.id=" + machineId);
                return machineId;
            } else {
                // 3. Crear archivo con ID por defecto
                return createDefaultMachineConfig();
            }

        } catch (Exception e) {
            System.err.println("⚠️ Error cargando ID de máquina, usando ID por defecto: " + e.getMessage());
            return 6823; // ✅ ID por defecto para mesa de prueba
        }
    }

    /**
     * ✅ MÉTODO MEJORADO: Crea archivo de configuración por defecto
     */
    private int createDefaultMachineConfig() {
        try {
            int defaultId = 6823; // ✅ ID de mesa para prueba

            Properties props = new Properties();
            props.setProperty("machine.id", String.valueOf(defaultId));
            props.setProperty("machine.name", "Mesa Votacion " + defaultId);
            props.setProperty("created.timestamp", String.valueOf(System.currentTimeMillis()));

            File configFile = new File("client/config/" + MACHINE_CONFIG_FILE);
            configFile.getParentFile().mkdirs();

            props.store(new FileOutputStream(configFile), "Machine Configuration - Auto Generated");

            System.out.println("📁 Archivo de configuración creado: " + configFile.getAbsolutePath());
            System.out.println("🆔 ID de máquina asignado: " + defaultId);

            return defaultId;

        } catch (IOException e) {
            System.err.println("❌ Error creando configuración por defecto: " + e.getMessage());
            return 6823;
        }
    }

    /**
     * Obtiene el ID de la máquina
     */
    public int getMachineId() {
        return machineId;
    }
}