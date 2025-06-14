package org.votaciones.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Configuración completa para el ProxyCache del sistema de reportes electorales
 * Maneja argumentos de línea de comandos, archivos de configuración y validaciones
 */
public class ProxyCacheConfig {

    private static final Logger logger = LoggerFactory.getLogger(ProxyCacheConfig.class);

    // =================== CONFIGURACIÓN POR DEFECTO ===================

    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 9091;
    private static final String DEFAULT_CACHE_DIR = "org/votaciones/cache";
    private static final String DEFAULT_MAIN_SERVER = "tcp -h localhost -p 9090";
    private static final String DEFAULT_MODE = "department";
    private static final int DEFAULT_LOCATION_ID = 1;

    // =================== CAMPOS DE CONFIGURACIÓN ===================

    // Configuración de red
    private String host;
    private int port;
    private String mainServerEndpoint;

    // Configuración operacional
    private String mode; // department, municipality, puesto
    private int locationId;
    private String cacheDirectory;
    private boolean interactiveMode;

    // Configuración avanzada
    private int maxCacheSize;
    private long cacheExpirationMs;
    private int connectionTimeoutMs;
    private int threadPoolSize;
    private boolean enableLogging;
    private String logLevel;

    // Configuración de archivos
    private String configFile;
    private Properties configProperties;

    // =================== CONSTRUCTOR ===================

    public ProxyCacheConfig(String[] args) {
        // Inicializar con valores por defecto
        initializeDefaults();

        // Cargar desde archivo de configuración si existe
        loadConfigurationFile();

        // Procesar argumentos de línea de comandos (tienen prioridad)
        parseCommandLineArguments(args);

        logger.info("ProxyCacheConfig initialized");
    }

    /**
     * Inicializa valores por defecto
     */
    private void initializeDefaults() {
        this.host = DEFAULT_HOST;
        this.port = DEFAULT_PORT;
        this.mainServerEndpoint = DEFAULT_MAIN_SERVER;
        this.mode = DEFAULT_MODE;
        this.locationId = DEFAULT_LOCATION_ID;
        this.cacheDirectory = DEFAULT_CACHE_DIR;
        this.interactiveMode = false;

        // Configuración avanzada por defecto
        this.maxCacheSize = 1000;
        this.cacheExpirationMs = 30 * 60 * 1000; // 30 minutos
        this.connectionTimeoutMs = 5000;
        this.threadPoolSize = 4;
        this.enableLogging = true;
        this.logLevel = "INFO";

        this.configFile = "proxycache.properties";
        this.configProperties = new Properties();
    }

    /**
     * Carga configuración desde archivo properties si existe
     */
    private void loadConfigurationFile() {
        Path configPath = Paths.get(configFile);

        if (Files.exists(configPath)) {
            try (FileInputStream fis = new FileInputStream(configPath.toFile())) {
                configProperties.load(fis);
                applyPropertiesConfiguration();
                logger.info("Configuration loaded from file: {}", configFile);
            } catch (IOException e) {
                logger.warn("Could not load configuration file: {}", configFile, e);
            }
        } else {
            logger.info("No configuration file found at: {} - using defaults", configFile);
        }
    }

    /**
     * Aplica configuración desde el archivo properties
     */
    private void applyPropertiesConfiguration() {
        // Configuración de red
        host = configProperties.getProperty("proxy.host", host);
        port = getIntProperty("proxy.port", port);
        mainServerEndpoint = configProperties.getProperty("proxy.mainServer", mainServerEndpoint);

        // Configuración operacional
        mode = configProperties.getProperty("proxy.mode", mode);
        locationId = getIntProperty("proxy.locationId", locationId);
        cacheDirectory = configProperties.getProperty("proxy.cacheDirectory", cacheDirectory);
        interactiveMode = getBooleanProperty("proxy.interactive", interactiveMode);

        // Configuración avanzada
        maxCacheSize = getIntProperty("proxy.cache.maxSize", maxCacheSize);
        cacheExpirationMs = getLongProperty("proxy.cache.expirationMs", cacheExpirationMs);
        connectionTimeoutMs = getIntProperty("proxy.connection.timeoutMs", connectionTimeoutMs);
        threadPoolSize = getIntProperty("proxy.threadPool.size", threadPoolSize);
        enableLogging = getBooleanProperty("proxy.logging.enabled", enableLogging);
        logLevel = configProperties.getProperty("proxy.logging.level", logLevel);
    }

    /**
     * Procesa argumentos de línea de comandos
     */
    private void parseCommandLineArguments(String[] args) {
        for (String arg : args) {
            if (arg.startsWith("--")) {
                processCommandLineArgument(arg);
            }
        }
    }

    /**
     * Procesa un argumento individual de línea de comandos
     */
    private void processCommandLineArgument(String arg) {
        try {
            if (arg.equals("--help") || arg.equals("-h")) {
                printUsage();
                System.exit(0);
            } else if (arg.startsWith("--mode=")) {
                mode = extractValue(arg);
            } else if (arg.startsWith("--location=")) {
                locationId = Integer.parseInt(extractValue(arg));
            } else if (arg.startsWith("--port=")) {
                port = Integer.parseInt(extractValue(arg));
            } else if (arg.startsWith("--host=")) {
                host = extractValue(arg);
            } else if (arg.startsWith("--main-server=")) {
                mainServerEndpoint = extractValue(arg);
            } else if (arg.startsWith("--cache-dir=")) {
                cacheDirectory = extractValue(arg);
            } else if (arg.equals("--interactive")) {
                interactiveMode = true;
            } else if (arg.startsWith("--config=")) {
                configFile = extractValue(arg);
                loadConfigurationFile(); // Recargar con nuevo archivo
            } else if (arg.startsWith("--max-cache-size=")) {
                maxCacheSize = Integer.parseInt(extractValue(arg));
            } else if (arg.startsWith("--cache-expiration=")) {
                cacheExpirationMs = Long.parseLong(extractValue(arg)) * 1000; // En segundos
            } else if (arg.startsWith("--timeout=")) {
                connectionTimeoutMs = Integer.parseInt(extractValue(arg));
            } else if (arg.startsWith("--threads=")) {
                threadPoolSize = Integer.parseInt(extractValue(arg));
            } else if (arg.startsWith("--log-level=")) {
                logLevel = extractValue(arg);
            } else {
                logger.warn("Unknown command line argument: {}", arg);
            }
        } catch (NumberFormatException e) {
            logger.error("Invalid number format in argument: {}", arg);
            throw new IllegalArgumentException("Invalid number format in argument: " + arg, e);
        } catch (Exception e) {
            logger.error("Error processing argument: {}", arg, e);
            throw new IllegalArgumentException("Error processing argument: " + arg, e);
        }
    }

    /**
     * Extrae el valor de un argumento --key=value
     */
    private String extractValue(String arg) {
        int equalIndex = arg.indexOf('=');
        if (equalIndex != -1 && equalIndex < arg.length() - 1) {
            return arg.substring(equalIndex + 1);
        }
        throw new IllegalArgumentException("Invalid argument format: " + arg);
    }

    /**
     * Valida la configuración completa
     */
    public boolean validate() {
        boolean isValid = true;

        // Validar modo
        if (!isValidMode(mode)) {
            logger.error("Invalid mode: {}. Must be: department, municipality, or puesto", mode);
            isValid = false;
        }

        // Validar locationId
        if (locationId <= 0) {
            logger.error("Invalid location ID: {}. Must be positive integer", locationId);
            isValid = false;
        }

        // Validar puerto
        if (port <= 0 || port > 65535) {
            logger.error("Invalid port: {}. Must be between 1 and 65535", port);
            isValid = false;
        }

        // Validar host
        if (host == null || host.trim().isEmpty()) {
            logger.error("Host cannot be null or empty");
            isValid = false;
        }

        // Validar mainServerEndpoint
        if (mainServerEndpoint == null || mainServerEndpoint.trim().isEmpty()) {
            logger.error("Main server endpoint cannot be null or empty");
            isValid = false;
        }

        // Validar directorio de cache
        if (cacheDirectory == null || cacheDirectory.trim().isEmpty()) {
            logger.error("Cache directory cannot be null or empty");
            isValid = false;
        } else {
            // Intentar crear directorio si no existe
            try {
                Path cachePath = Paths.get(cacheDirectory);
                if (!Files.exists(cachePath)) {
                    Files.createDirectories(cachePath);
                    logger.info("Created cache directory: {}", cacheDirectory);
                }
            } catch (IOException e) {
                logger.error("Cannot create cache directory: {}", cacheDirectory, e);
                isValid = false;
            }
        }

        // Validar configuración avanzada
        if (maxCacheSize <= 0) {
            logger.error("Max cache size must be positive: {}", maxCacheSize);
            isValid = false;
        }

        if (cacheExpirationMs <= 0) {
            logger.error("Cache expiration time must be positive: {}", cacheExpirationMs);
            isValid = false;
        }

        if (connectionTimeoutMs <= 0) {
            logger.error("Connection timeout must be positive: {}", connectionTimeoutMs);
            isValid = false;
        }

        if (threadPoolSize <= 0) {
            logger.error("Thread pool size must be positive: {}", threadPoolSize);
            isValid = false;
        }

        return isValid;
    }

    /**
     * Verifica si el modo es válido
     */
    private boolean isValidMode(String mode) {
        return "department".equalsIgnoreCase(mode) ||
                "municipality".equalsIgnoreCase(mode) ||
                "puesto".equalsIgnoreCase(mode);
    }

    /**
     * Muestra la configuración actual
     */
    public void displayConfiguration() {
        logger.info("=== ProxyCache Configuration ===");
        logger.info("Network Configuration:");
        logger.info("  Host: {}", host);
        logger.info("  Port: {}", port);
        logger.info("  Main Server: {}", mainServerEndpoint);

        logger.info("Operational Configuration:");
        logger.info("  Mode: {}", mode);
        logger.info("  Location ID: {}", locationId);
        logger.info("  Cache Directory: {}", cacheDirectory);
        logger.info("  Interactive Mode: {}", interactiveMode);

        logger.info("Advanced Configuration:");
        logger.info("  Max Cache Size: {}", maxCacheSize);
        logger.info("  Cache Expiration: {} ms", cacheExpirationMs);
        logger.info("  Connection Timeout: {} ms", connectionTimeoutMs);
        logger.info("  Thread Pool Size: {}", threadPoolSize);
        logger.info("  Logging Enabled: {}", enableLogging);
        logger.info("  Log Level: {}", logLevel);

        logger.info("Configuration File: {}", configFile);
        logger.info("================================");
    }

    /**
     * Genera un resumen de configuración para logs
     */
    public String getConfigurationSummary() {
        return String.format("ProxyCache[%s:%d] Mode=%s Location=%d Cache=%s Server=%s",
                host, port, mode, locationId, cacheDirectory, mainServerEndpoint);
    }

    /**
     * Crea configuración específica para un departamento
     */
    public static ProxyCacheConfig forDepartment(int departmentId, int port, String mainServer) {
        String[] args = {
                "--mode=department",
                "--location=" + departmentId,
                "--port=" + port,
                "--main-server=" + mainServer
        };
        return new ProxyCacheConfig(args);
    }

    /**
     * Crea configuración específica para un municipio
     */
    public static ProxyCacheConfig forMunicipality(int municipalityId, int port, String mainServer) {
        String[] args = {
                "--mode=municipality",
                "--location=" + municipalityId,
                "--port=" + port,
                "--main-server=" + mainServer
        };
        return new ProxyCacheConfig(args);
    }

    /**
     * Crea configuración específica para un puesto
     */
    public static ProxyCacheConfig forPuesto(int puestoId, int port, String mainServer) {
        String[] args = {
                "--mode=puesto",
                "--location=" + puestoId,
                "--port=" + port,
                "--main-server=" + mainServer
        };
        return new ProxyCacheConfig(args);
    }

    /**
     * Guarda la configuración actual a un archivo properties
     */
    public void saveToFile(String filename) throws IOException {
        Properties props = new Properties();

        // Configuración de red
        props.setProperty("proxy.host", host);
        props.setProperty("proxy.port", String.valueOf(port));
        props.setProperty("proxy.mainServer", mainServerEndpoint);

        // Configuración operacional
        props.setProperty("proxy.mode", mode);
        props.setProperty("proxy.locationId", String.valueOf(locationId));
        props.setProperty("proxy.cacheDirectory", cacheDirectory);
        props.setProperty("proxy.interactive", String.valueOf(interactiveMode));

        // Configuración avanzada
        props.setProperty("proxy.cache.maxSize", String.valueOf(maxCacheSize));
        props.setProperty("proxy.cache.expirationMs", String.valueOf(cacheExpirationMs));
        props.setProperty("proxy.connection.timeoutMs", String.valueOf(connectionTimeoutMs));
        props.setProperty("proxy.threadPool.size", String.valueOf(threadPoolSize));
        props.setProperty("proxy.logging.enabled", String.valueOf(enableLogging));
        props.setProperty("proxy.logging.level", logLevel);

        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(filename)) {
            props.store(fos, "ProxyCache Configuration - Generated at " + new java.util.Date());
            logger.info("Configuration saved to: {}", filename);
        }
    }

    // =================== MÉTODOS DE UTILIDAD PARA PROPERTIES ===================

    private int getIntProperty(String key, int defaultValue) {
        String value = configProperties.getProperty(key);
        if (value != null) {
            try {
                return Integer.parseInt(value.trim());
            } catch (NumberFormatException e) {
                logger.warn("Invalid integer property {}: {}, using default: {}", key, value, defaultValue);
            }
        }
        return defaultValue;
    }

    private long getLongProperty(String key, long defaultValue) {
        String value = configProperties.getProperty(key);
        if (value != null) {
            try {
                return Long.parseLong(value.trim());
            } catch (NumberFormatException e) {
                logger.warn("Invalid long property {}: {}, using default: {}", key, value, defaultValue);
            }
        }
        return defaultValue;
    }

    private boolean getBooleanProperty(String key, boolean defaultValue) {
        String value = configProperties.getProperty(key);
        if (value != null) {
            return Boolean.parseBoolean(value.trim());
        }
        return defaultValue;
    }

    // =================== MÉTODOS DE AYUDA ===================

    private void printUsage() {
        System.out.println("ProxyCache Reports Service Configuration");
        System.out.println("Usage: java -jar proxycache.jar [options]");
        System.out.println();
        System.out.println("Required Options:");
        System.out.println("  --mode=<department|municipality|puesto>  Operating mode");
        System.out.println("  --location=<id>                          Location ID for the mode");
        System.out.println();
        System.out.println("Network Options:");
        System.out.println("  --port=<port>                            Port to listen on (default: 9091)");
        System.out.println("  --host=<host>                            Host to bind to (default: localhost)");
        System.out.println("  --main-server=<endpoint>                 Main server Ice endpoint");
        System.out.println("                                           (default: \"tcp -h localhost -p 9090\")");
        System.out.println();
        System.out.println("Cache Options:");
        System.out.println("  --cache-dir=<directory>                  Cache directory (default: ./cache)");
        System.out.println("  --max-cache-size=<size>                  Maximum cache entries (default: 1000)");
        System.out.println("  --cache-expiration=<seconds>             Cache expiration time (default: 1800)");
        System.out.println();
        System.out.println("Advanced Options:");
        System.out.println("  --config=<file>                          Configuration file (default: proxycache.properties)");
        System.out.println("  --interactive                            Enable interactive CLI mode");
        System.out.println("  --timeout=<ms>                           Connection timeout (default: 5000)");
        System.out.println("  --threads=<count>                        Thread pool size (default: 4)");
        System.out.println("  --log-level=<level>                      Log level (default: INFO)");
        System.out.println("  --help                                   Show this help");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  # Department proxy cache for Antioquia (ID=5)");
        System.out.println("  java -jar proxycache.jar --mode=department --location=5 --port=9091");
        System.out.println();
        System.out.println("  # Municipality proxy cache for Medellín (ID=25) with interactive mode");
        System.out.println("  java -jar proxycache.jar --mode=municipality --location=25 --port=9092 --interactive");
        System.out.println();
        System.out.println("  # Puesto proxy cache with custom main server");
        System.out.println("  java -jar proxycache.jar --mode=puesto --location=150 --port=9093 \\");
        System.out.println("                           --main-server=\"tcp -h 192.168.1.100 -p 9090\"");
        System.out.println();
        System.out.println("  # Using configuration file");
        System.out.println("  java -jar proxycache.jar --config=my-proxy.properties");
        System.out.println();
        System.out.println("Configuration File Format:");
        System.out.println("  proxy.mode=department");
        System.out.println("  proxy.locationId=5");
        System.out.println("  proxy.port=9091");
        System.out.println("  proxy.host=localhost");
        System.out.println("  proxy.mainServer=tcp -h localhost -p 9090");
        System.out.println("  proxy.cacheDirectory=./cache");
        System.out.println("  proxy.interactive=false");
        System.out.println("  proxy.cache.maxSize=1000");
        System.out.println("  proxy.cache.expirationMs=1800000");
        System.out.println("  proxy.connection.timeoutMs=5000");
        System.out.println("  proxy.threadPool.size=4");
        System.out.println("  proxy.logging.enabled=true");
        System.out.println("  proxy.logging.level=INFO");
    }

    // =================== GETTERS Y SETTERS ===================

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }

    public String getMainServerEndpoint() { return mainServerEndpoint; }
    public void setMainServerEndpoint(String mainServerEndpoint) { this.mainServerEndpoint = mainServerEndpoint; }

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }

    public int getLocationId() { return locationId; }
    public void setLocationId(int locationId) { this.locationId = locationId; }

    public String getCacheDirectory() { return cacheDirectory; }
    public void setCacheDirectory(String cacheDirectory) { this.cacheDirectory = cacheDirectory; }

    public boolean isInteractiveMode() { return interactiveMode; }
    public void setInteractiveMode(boolean interactiveMode) { this.interactiveMode = interactiveMode; }

    public int getMaxCacheSize() { return maxCacheSize; }
    public void setMaxCacheSize(int maxCacheSize) { this.maxCacheSize = maxCacheSize; }

    public long getCacheExpirationMs() { return cacheExpirationMs; }
    public void setCacheExpirationMs(long cacheExpirationMs) { this.cacheExpirationMs = cacheExpirationMs; }

    public int getConnectionTimeoutMs() { return connectionTimeoutMs; }
    public void setConnectionTimeoutMs(int connectionTimeoutMs) { this.connectionTimeoutMs = connectionTimeoutMs; }

    public int getThreadPoolSize() { return threadPoolSize; }
    public void setThreadPoolSize(int threadPoolSize) { this.threadPoolSize = threadPoolSize; }

    public boolean isEnableLogging() { return enableLogging; }
    public void setEnableLogging(boolean enableLogging) { this.enableLogging = enableLogging; }

    public String getLogLevel() { return logLevel; }
    public void setLogLevel(String logLevel) { this.logLevel = logLevel; }

    public String getConfigFile() { return configFile; }
    public void setConfigFile(String configFile) { this.configFile = configFile; }

    // =================== MÉTODOS DE CONVENIENCIA ===================

    /**
     * Retorna el endpoint completo para este proxy cache
     */
    public String getProxyEndpoint() {
        return String.format("tcp -h %s -p %d", host, port);
    }

    /**
     * Retorna el nombre del objeto Ice para este proxy
     */
    public String getIceObjectName() {
        return String.format("ReportsManager_%s_%d", mode, locationId);
    }

    /**
     * Retorna el nombre del adapter Ice para este proxy
     */
    public String getIceAdapterName() {
        return String.format("ProxyCache_%s_%d", mode, locationId);
    }

    /**
     * Retorna el directorio de cache específico para esta instancia
     */
    public String getInstanceCacheDirectory() {
        return String.format("%s/%s_%d", cacheDirectory, mode, locationId);
    }

    /**
     * Retorna si esta instancia maneja un departamento específico
     */
    public boolean isDepartmentMode() {
        return "department".equalsIgnoreCase(mode);
    }

    /**
     * Retorna si esta instancia maneja un municipio específico
     */
    public boolean isMunicipalityMode() {
        return "municipality".equalsIgnoreCase(mode);
    }

    /**
     * Retorna si esta instancia maneja un puesto específico
     */
    public boolean isPuestoMode() {
        return "puesto".equalsIgnoreCase(mode);
    }

    /**
     * Retorna una descripción legible de la configuración
     */
    @Override
    public String toString() {
        return String.format("ProxyCacheConfig{mode=%s, locationId=%d, host=%s, port=%d, mainServer=%s, cacheDir=%s}",
                mode, locationId, host, port, mainServerEndpoint, cacheDirectory);
    }

    /**
     * Crea una copia de esta configuración
     */
    public ProxyCacheConfig copy() {
        ProxyCacheConfig copy = new ProxyCacheConfig(new String[0]);

        copy.host = this.host;
        copy.port = this.port;
        copy.mainServerEndpoint = this.mainServerEndpoint;
        copy.mode = this.mode;
        copy.locationId = this.locationId;
        copy.cacheDirectory = this.cacheDirectory;
        copy.interactiveMode = this.interactiveMode;
        copy.maxCacheSize = this.maxCacheSize;
        copy.cacheExpirationMs = this.cacheExpirationMs;
        copy.connectionTimeoutMs = this.connectionTimeoutMs;
        copy.threadPoolSize = this.threadPoolSize;
        copy.enableLogging = this.enableLogging;
        copy.logLevel = this.logLevel;
        copy.configFile = this.configFile;

        return copy;
    }
}