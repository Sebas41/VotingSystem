package controller;

import com.zeroc.Ice.Current;
import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.Util;
import com.zeroc.Ice.InputStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Imports específicos para las clases generadas por Ice
import VotingSystem.ConfigurationCache;
import VotingSystem.VotingConfiguration;

public class ConfigurationCacheProxyImpl implements ConfigurationCache {
    private final Map<String, VotingConfiguration> cache = new ConcurrentHashMap<>();
    private final String basePath = "server/src/main/java/VotingMachineManager/data/voting_configurations";
    private final Communicator communicator;

    public ConfigurationCacheProxyImpl(Communicator communicator) {
        this.communicator = communicator;
    }

    private String buildKey(int mesaId, int electionId) {
        return mesaId + "-" + electionId;
    }

    @Override
    public VotingConfiguration getConfiguration(int mesaId, int electionId, Current current) {
        String key = buildKey(mesaId, electionId);

        if (cache.containsKey(key)) {
            return cache.get(key);
        }

        // Ruta esperada: election_1/department_29/mesa_XXXX_config.ice
        String departmentId = "department_" + (mesaId / 1000); // Ejemplo heurístico
        String path = String.format("%s/election_%d/%s/mesa_%d_config.ice", basePath, electionId, departmentId, mesaId);

        File file = new File(path);
        if (!file.exists()) {
            System.err.println("Archivo no encontrado: " + path);
            return null;
        }

        try {
            VotingConfiguration config = loadConfigurationFromFile(file);
            if (config != null) {
                cache.put(key, config);
            }
            return config;
        } catch (Exception e) {
            System.err.println("Error al cargar configuración desde " + path + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private VotingConfiguration loadConfigurationFromFile(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] data = fis.readAllBytes();
            
            // Crear InputStream de Ice para deserializar
            InputStream inputStream = new InputStream(communicator, data);
            inputStream.startEncapsulation();
            
            // Leer el objeto VotingConfiguration
            VotingConfiguration config = new VotingConfiguration();
            config.ice_read(inputStream);
            
            inputStream.endEncapsulation();
            return config;
        }
    }

    @Override
    public boolean isConfigurationReady(int mesaId, int electionId, Current current) {
        String key = buildKey(mesaId, electionId);
        return cache.containsKey(key);
    }

    @Override
    public void preloadConfigurations(java.util.List<Integer> mesaIds, int electionId, Current current) {
        for (int mesaId : mesaIds) {
            getConfiguration(mesaId, electionId, current); // Carga en caché
        }
    }
}