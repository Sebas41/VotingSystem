package VotingMachineManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


public class MesaConfigurationManager {

    private static final Logger logger = LoggerFactory.getLogger(MesaConfigurationManager.class);
    private static final String CONFIG_FILE = "mesas-config.properties";

    private final Properties mesaProperties;
    private final Map<Integer, MesaInfo> mesasCache;
    private final String configFilePath;

    public MesaConfigurationManager() {
        this.mesaProperties = new Properties();
        this.mesasCache = new ConcurrentHashMap<>();
        this.configFilePath = CONFIG_FILE;

        loadConfiguration();
    }

    public MesaConfigurationManager(String customConfigPath) {
        this.mesaProperties = new Properties();
        this.mesasCache = new ConcurrentHashMap<>();
        this.configFilePath = customConfigPath;

        loadConfiguration();
    }


    private void loadConfiguration() {
        try {
            logger.info(" Cargando configuración de mesas desde: {}", configFilePath);

            // Intentar cargar desde el classpath primero
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream(configFilePath);

            // Si no está en classpath, intentar desde sistema de archivos
            if (inputStream == null) {
                File configFile = new File(configFilePath);
                if (configFile.exists()) {
                    inputStream = new FileInputStream(configFile);
                } else {
                    logger.warn(" Archivo de configuración no encontrado: {}", configFilePath);
                    createDefaultConfigFile();
                    return;
                }
            }

            mesaProperties.load(inputStream);
            inputStream.close();

            // Parsear y cachear la información de las mesas
            parseAndCacheMesaInfo();

            logger.info(" Configuración cargada exitosamente - {} mesas registradas", mesasCache.size());

        } catch (Exception e) {
            logger.error(" Error cargando configuración de mesas: {}", e.getMessage(), e);
            createDefaultConfigFile();
        }
    }


    private void parseAndCacheMesaInfo() {
        mesasCache.clear();

        Set<String> mesaIds = new HashSet<>();

        // Encontrar todos los IDs de mesa únicos
        for (String key : mesaProperties.stringPropertyNames()) {
            if (key.startsWith("mesa.") && key.contains(".")) {
                String[] parts = key.split("\\.");
                if (parts.length >= 3) {
                    mesaIds.add(parts[1]); // El ID está en la segunda posición
                }
            }
        }

        // Crear objetos MesaInfo para cada mesa
        for (String mesaIdStr : mesaIds) {
            try {
                int mesaId = Integer.parseInt(mesaIdStr);
                MesaInfo mesaInfo = createMesaInfoFromProperties(mesaId);

                if (mesaInfo != null) {
                    mesasCache.put(mesaId, mesaInfo);
                    logger.debug(" Mesa {} registrada: {}:{}", mesaId, mesaInfo.getIp(), mesaInfo.getPort());
                }

            } catch (NumberFormatException e) {
                logger.warn(" ID de mesa inválido: {}", mesaIdStr);
            }
        }
    }

    /**
     * Crea un objeto MesaInfo desde las propiedades
     */
    private MesaInfo createMesaInfoFromProperties(int mesaId) {
        String prefix = "mesa." + mesaId + ".";

        String ip = mesaProperties.getProperty(prefix + "ip");
        String portStr = mesaProperties.getProperty(prefix + "port");
        String name = mesaProperties.getProperty(prefix + "name", "Mesa " + mesaId);
        String departmentStr = mesaProperties.getProperty(prefix + "department", "1");
        String activeStr = mesaProperties.getProperty(prefix + "active", "true");

        if (ip == null || portStr == null) {
            logger.warn(" Configuración incompleta para mesa {}", mesaId);
            return null;
        }

        try {
            int port = Integer.parseInt(portStr);
            int department = Integer.parseInt(departmentStr);
            boolean active = Boolean.parseBoolean(activeStr);

            return new MesaInfo(mesaId, ip, port, name, department, active);

        } catch (NumberFormatException e) {
            logger.error(" Error parseando configuración de mesa {}: {}", mesaId, e.getMessage());
            return null;
        }
    }

    /**
     * Crea un archivo de configuración por defecto
     */
    private void createDefaultConfigFile() {
        try {
            logger.info(" Creando archivo de configuración por defecto...");

            Properties defaultProps = new Properties();
            defaultProps.setProperty("mesa.6823.ip", "localhost");
            defaultProps.setProperty("mesa.6823.port", "10843");
            defaultProps.setProperty("mesa.6823.name", "Mesa Central de Pruebas");
            defaultProps.setProperty("mesa.6823.department", "1");
            defaultProps.setProperty("mesa.6823.active", "true");

            defaultProps.setProperty("config.default.timeout", "30000");
            defaultProps.setProperty("config.default.retries", "3");

            File configFile = new File(configFilePath);
            try (FileOutputStream out = new FileOutputStream(configFile)) {
                defaultProps.store(out, "Configuración por defecto de mesas de votación");
            }

            // Cargar la configuración recién creada
            this.mesaProperties.putAll(defaultProps);
            parseAndCacheMesaInfo();

            logger.info(" Archivo de configuración por defecto creado: {}", configFilePath);

        } catch (Exception e) {
            logger.error(" Error creando configuración por defecto: {}", e.getMessage(), e);
        }
    }


    public MesaInfo getMesaInfo(int mesaId) {
        return mesasCache.get(mesaId);
    }


    public boolean isMesaRegistered(int mesaId) {
        MesaInfo mesaInfo = mesasCache.get(mesaId);
        return mesaInfo != null && mesaInfo.isActive();
    }


    public Collection<MesaInfo> getAllMesas() {
        return new ArrayList<>(mesasCache.values());
    }


    public List<MesaInfo> getActiveMesas() {
        return mesasCache.values().stream()
                .filter(MesaInfo::isActive)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }


    public List<MesaInfo> getMesasByDepartment(int departmentId) {
        return mesasCache.values().stream()
                .filter(mesa -> mesa.getDepartment() == departmentId && mesa.isActive())
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }


    public List<Integer> getAllMesaIds() {
        return new ArrayList<>(mesasCache.keySet());
    }


    public List<Integer> getActiveMesaIds() {
        return mesasCache.values().stream()
                .filter(MesaInfo::isActive)
                .map(MesaInfo::getId)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }


    public void reloadConfiguration() {
        logger.info(" Recargando configuración de mesas...");
        loadConfiguration();
    }


    public Map<String, Object> getConfigurationStats() {
        Map<String, Object> stats = new HashMap<>();

        long totalMesas = mesasCache.size();
        long activeMesas = mesasCache.values().stream().mapToLong(mesa -> mesa.isActive() ? 1 : 0).sum();

        //  CORRECCIÓN: Usar Collectors.groupingBy() en lugar del collect() manual
        Map<Integer, Long> mesasByDepartment = mesasCache.values().stream()
                .filter(MesaInfo::isActive)
                .collect(Collectors.groupingBy(MesaInfo::getDepartment, Collectors.counting()));

        stats.put("total_mesas", totalMesas);
        stats.put("active_mesas", activeMesas);
        stats.put("inactive_mesas", totalMesas - activeMesas);
        stats.put("mesas_by_department", mesasByDepartment);
        stats.put("config_file", configFilePath);
        stats.put("last_loaded", new Date());

        return stats;
    }




    public static class MesaInfo {
        private final int id;
        private final String ip;
        private final int port;
        private final String name;
        private final int department;
        private final boolean active;

        public MesaInfo(int id, String ip, int port, String name, int department, boolean active) {
            this.id = id;
            this.ip = ip;
            this.port = port;
            this.name = name;
            this.department = department;
            this.active = active;
        }

        // Getters
        public int getId() { return id; }
        public String getIp() { return ip; }
        public int getPort() { return port; }
        public String getName() { return name; }
        public int getDepartment() { return department; }
        public boolean isActive() { return active; }

        public String getEndpoint() {
            return "ConfigurationReceiver:default -h " + ip + " -p " + port;
        }

        @Override
        public String toString() {
            return String.format("Mesa{id=%d, name='%s', ip='%s', port=%d, dept=%d, active=%s}",
                    id, name, ip, port, department, active);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MesaInfo mesaInfo = (MesaInfo) o;
            return id == mesaInfo.id;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }
    }
}