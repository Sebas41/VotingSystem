package Controller;

import ConnectionDB.ConnectionDB;
import Elections.ElectionImpl;
import Elections.ElectionInterface;
import Elections.models.Candidate;
import Elections.models.ELECTION_STATUS;
import Reports.ReportsInterface;
import Reports.ReportsImplementation;
import ServerUI.ServerUI;
import VotingMachineManager.VotingManagerInterface;
import VotingMachineManager.VotingManagerImpl;
import model.ReliableMessage;
import model.Vote;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class ServerControllerImpl implements ServerControllerInterface {

    private ElectionInterface election;
    private ConnectionDB connectionDB;
    private ReportsInterface reports;
    private VotingManagerInterface votingManager;

    // Configuration storage paths
    private static final String CONFIG_BASE_PATH = "voting_configurations";
    private static final String DEPLOYMENT_LOGS_PATH = "deployment_logs";

    // Threading for parallel processing
    private ExecutorService configExecutor;
    private ObjectMapper jsonMapper;

    // Progress tracking
    private volatile boolean isGeneratingConfigs = false;
    private AtomicInteger configProgress = new AtomicInteger(0);
    private int totalConfigsToGenerate = 0;

    public ServerControllerImpl() {
        this.connectionDB = new ConnectionDB();
        this.election = new ElectionImpl(0, new Date(), new Date(), "");
        this.reports = new ReportsImplementation(connectionDB);
        this.votingManager = new VotingManagerImpl(connectionDB);

        // Initialize JSON mapper
        this.jsonMapper = new ObjectMapper();
        this.jsonMapper.enable(SerializationFeature.INDENT_OUTPUT);

        // Initialize thread pool for configuration generation
        this.configExecutor = Executors.newFixedThreadPool(
                Math.min(10, Runtime.getRuntime().availableProcessors())
        );

        // Create necessary directories
        initializeDirectories();

        cargarDatosPrueba();
    }

    private void initializeDirectories() {
        try {
            Files.createDirectories(Paths.get(CONFIG_BASE_PATH));
            Files.createDirectories(Paths.get(DEPLOYMENT_LOGS_PATH));
            System.out.println("Configuration directories initialized successfully");
        } catch (IOException e) {
            System.err.println("Error creating configuration directories: " + e.getMessage());
        }
    }

    private void cargarDatosPrueba() {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm");
            Date start = sdf.parse("30-05-2025 00:00");
            Date end = sdf.parse("31-12-2025 23:59");

            createElection(1, "Elección de Prueba", start, end);
            changeElectionStatus(ELECTION_STATUS.DURING);

            addCandidate(1, "Candidato A", "Partido A");
            addCandidate(2, "Candidato B", "Partido B");
            addCandidate(3, "Candidato C", "Partido C");

            System.out.println("=== Datos de prueba cargados exitosamente ===");
        } catch (Exception e) {
            System.err.println("Error cargando datos de prueba: " + e.getMessage());
        }
    }

    // =================== SELECTIVE CONFIGURATION METHODS ===================

    /**
     * Get all available departments for selection
     */
    public List<Map<String, Object>> getAvailableDepartments() {
        try {
            List<Map<String, Object>> departments = connectionDB.getAllDepartments();
            System.out.println("Found " + departments.size() + " departments");
            return departments;
        } catch (Exception e) {
            System.err.println("Error retrieving departments: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Get all municipalities in a specific department
     */
    public List<Map<String, Object>> getMunicipalitiesByDepartment(int departmentId) {
        try {
            List<Map<String, Object>> municipalities = connectionDB.getMunicipalitiesByDepartment(departmentId);
            System.out.println("Found " + municipalities.size() + " municipalities in department " + departmentId);
            return municipalities;
        } catch (Exception e) {
            System.err.println("Error retrieving municipalities: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Get all puestos de votación in a specific municipality
     */
    public List<Map<String, Object>> getPuestosByMunicipality(int municipalityId) {
        try {
            List<Map<String, Object>> puestos = connectionDB.getPuestosByMunicipality(municipalityId);
            System.out.println("Found " + puestos.size() + " puestos in municipality " + municipalityId);
            return puestos;
        } catch (Exception e) {
            System.err.println("Error retrieving puestos: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Get all mesas in a specific puesto de votación
     */
    public List<Map<String, Object>> getMesasByPuesto(int puestoId) {
        try {
            // We need to get mesa IDs and their info for a specific puesto
            List<Integer> allMesaIds = connectionDB.getAllMesaIds();
            List<Map<String, Object>> mesasInPuesto = new ArrayList<>();

            for (Integer mesaId : allMesaIds) {
                Map<String, Object> mesaConfig = connectionDB.getMesaConfiguration(mesaId);
                if (mesaConfig != null && puestoId == (Integer) mesaConfig.get("puesto_id")) {
                    Map<String, Object> mesaInfo = new HashMap<>();
                    mesaInfo.put("mesa_id", mesaConfig.get("mesa_id"));
                    mesaInfo.put("mesa_consecutive", mesaConfig.get("mesa_consecutive"));
                    mesaInfo.put("total_ciudadanos", mesaConfig.get("total_ciudadanos"));
                    mesaInfo.put("puesto_nombre", mesaConfig.get("puesto_nombre"));
                    mesasInPuesto.add(mesaInfo);
                }
            }

            System.out.println("Found " + mesasInPuesto.size() + " mesas in puesto " + puestoId);
            return mesasInPuesto;
        } catch (Exception e) {
            System.err.println("Error retrieving mesas for puesto: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Generate configuration for a specific list of mesas
     */
    public void generateSelectiveMesaConfigurations(List<Integer> mesaIds, int electionId, String testName) {
        if (isGeneratingConfigs) {
            System.out.println("Configuration generation already in progress...");
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                isGeneratingConfigs = true;
                configProgress.set(0);
                totalConfigsToGenerate = mesaIds.size();

                System.out.println("=== Starting Selective Mesa Configuration Generation ===");
                System.out.println("Test Name: " + testName);
                System.out.println("Mesas to configure: " + mesaIds);

                // Check if election is ready
                if (!votingManager.isElectionReadyForConfiguration(electionId)) {
                    System.err.println("Election " + electionId + " is not ready for configuration!");
                    return;
                }

                // Create test-specific directory
                String testConfigPath = CONFIG_BASE_PATH + "/election_" + electionId + "/selective_tests/" +
                        sanitizeFileName(testName) + "_" + System.currentTimeMillis();
                Files.createDirectories(Paths.get(testConfigPath));

                long startTime = System.currentTimeMillis();

                // Generate configurations for selected mesas
                Map<Integer, Map<String, Object>> configs =
                        votingManager.generateBatchMachineConfigurations(mesaIds, electionId);

                // Save configurations to files
                saveConfigurationsToFiles(configs, testConfigPath);

                long endTime = System.currentTimeMillis();
                double totalTimeSeconds = (endTime - startTime) / 1000.0;

                // Generate test summary
                generateSelectiveTestSummary(electionId, mesaIds, testName, totalTimeSeconds, testConfigPath);

                System.out.println("=== Selective Configuration Generation Complete ===");
                System.out.printf("Generated %d configurations in %.2f seconds%n",
                        configs.size(), totalTimeSeconds);

                // Display mesa details
                displayMesaDetails(configs);

            } catch (Exception e) {
                System.err.println("Error during selective configuration generation: " + e.getMessage());
                e.printStackTrace();
            } finally {
                isGeneratingConfigs = false;
                configProgress.set(0);
                totalConfigsToGenerate = 0;
            }
        });
    }

    /**
     * Generate configuration for a specific puesto de votación
     */
    public void generatePuestoConfiguration(int puestoId, int electionId, String testName) {
        try {
            System.out.println("=== Generating Configuration for Puesto " + puestoId + " ===");

            // Get all mesas in this puesto
            List<Map<String, Object>> mesasInPuesto = getMesasByPuesto(puestoId);
            List<Integer> mesaIds = mesasInPuesto.stream()
                    .map(mesa -> (Integer) mesa.get("mesa_id"))
                    .collect(Collectors.toList());

            if (mesaIds.isEmpty()) {
                System.out.println("No mesas found in puesto " + puestoId);
                return;
            }

            // Get puesto information for better naming
            String puestoName = mesasInPuesto.get(0).get("puesto_nombre").toString();
            String enhancedTestName = testName + "_Puesto_" + puestoId + "_" + sanitizeFileName(puestoName);

            generateSelectiveMesaConfigurations(mesaIds, electionId, enhancedTestName);

        } catch (Exception e) {
            System.err.println("Error generating puesto configuration: " + e.getMessage());
        }
    }

    /**
     * Generate configuration for specific mesas in a department (for testing sample)
     */
    public void generateDepartmentSampleConfiguration(int departmentId, int sampleSize, int electionId, String testName) {
        try {
            System.out.println("=== Generating Sample Configuration for Department " + departmentId + " ===");

            // Get all mesas in department
            List<Integer> departmentMesaIds = connectionDB.getMesaIdsByDepartment(departmentId);

            if (departmentMesaIds.isEmpty()) {
                System.out.println("No mesas found in department " + departmentId);
                return;
            }

            // Take a sample
            Collections.shuffle(departmentMesaIds);
            List<Integer> sampleMesaIds = departmentMesaIds.stream()
                    .limit(sampleSize)
                    .collect(Collectors.toList());

            String enhancedTestName = testName + "_DeptSample_" + departmentId + "_" + sampleSize + "mesas";

            generateSelectiveMesaConfigurations(sampleMesaIds, electionId, enhancedTestName);

        } catch (Exception e) {
            System.err.println("Error generating department sample configuration: " + e.getMessage());
        }
    }

    /**
     * Get configuration details for a specific mesa (for preview before generation)
     */
    public String previewMesaConfiguration(int mesaId) {
        try {
            Map<String, Object> mesaConfig = connectionDB.getMesaConfiguration(mesaId);
            if (mesaConfig == null) {
                return "Mesa " + mesaId + " not found";
            }

            List<Map<String, Object>> citizens = connectionDB.getCitizensByMesa(mesaId);

            StringBuilder preview = new StringBuilder();
            preview.append("=== Mesa ").append(mesaId).append(" Preview ===\n");
            preview.append("Consecutive: ").append(mesaConfig.get("mesa_consecutive")).append("\n");
            preview.append("Puesto: ").append(mesaConfig.get("puesto_nombre")).append("\n");
            preview.append("Address: ").append(mesaConfig.get("puesto_direccion")).append("\n");
            preview.append("Municipality: ").append(mesaConfig.get("municipio_nombre")).append("\n");
            preview.append("Department: ").append(mesaConfig.get("departamento_nombre")).append("\n");
            preview.append("Total Citizens: ").append(citizens.size()).append("\n");

            if (!citizens.isEmpty()) {
                preview.append("Sample Citizens:\n");
                citizens.stream().limit(3).forEach(citizen ->
                        preview.append("  - ").append(citizen.get("nombre")).append(" ")
                                .append(citizen.get("apellido")).append(" (").append(citizen.get("documento")).append(")\n")
                );
                if (citizens.size() > 3) {
                    preview.append("  ... and ").append(citizens.size() - 3).append(" more\n");
                }
            }

            return preview.toString();

        } catch (Exception e) {
            return "Error previewing mesa " + mesaId + ": " + e.getMessage();
        }
    }

    /**
     * List all previous selective tests
     */
    public List<String> getSelectiveTestHistory() {
        try {
            String selectiveTestsPath = CONFIG_BASE_PATH + "/election_" + election.getElectionId() + "/selective_tests";
            if (!Files.exists(Paths.get(selectiveTestsPath))) {
                return new ArrayList<>();
            }

            return Files.list(Paths.get(selectiveTestsPath))
                    .filter(Files::isDirectory)
                    .map(path -> path.getFileName().toString())
                    .sorted(Collections.reverseOrder()) // Most recent first
                    .collect(Collectors.toList());

        } catch (Exception e) {
            System.err.println("Error retrieving test history: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Get summary of a specific selective test
     */
    public String getSelectiveTestSummary(String testName) {
        try {
            String summaryPath = DEPLOYMENT_LOGS_PATH + "/selective_test_" + sanitizeFileName(testName) + ".txt";
            if (Files.exists(Paths.get(summaryPath))) {
                return Files.readString(Paths.get(summaryPath));
            }
            return "Test summary not found for: " + testName;
        } catch (Exception e) {
            return "Error reading test summary: " + e.getMessage();
        }
    }

    // =================== HELPER METHODS ===================

    private void displayMesaDetails(Map<Integer, Map<String, Object>> configs) {
        System.out.println("\n=== Generated Configuration Details ===");
        configs.forEach((mesaId, config) -> {
            Map<String, Object> summary = (Map<String, Object>) config.get("summary");
            Map<String, Object> mesaInfo = (Map<String, Object>) config.get("mesaInfo");

            if (summary != null && mesaInfo != null) {
                System.out.printf("Mesa %d: %d citizens, Location: %s%n",
                        mesaId,
                        summary.get("totalAssignedCitizens"),
                        mesaInfo.get("puesto_nombre")
                );
            }
        });
    }

    private void generateSelectiveTestSummary(int electionId, List<Integer> mesaIds, String testName,
                                              double timeSeconds, String configPath) {
        try {
            String summaryPath = DEPLOYMENT_LOGS_PATH + "/selective_test_" + sanitizeFileName(testName) + ".txt";
            StringBuilder summary = new StringBuilder();

            summary.append("=== Selective Configuration Test Summary ===\n");
            summary.append("Test Name: ").append(testName).append("\n");
            summary.append("Election ID: ").append(electionId).append("\n");
            summary.append("Test Date: ").append(new Date()).append("\n");
            summary.append("Mesas Configured: ").append(mesaIds.size()).append("\n");
            summary.append("Mesa IDs: ").append(mesaIds).append("\n");
            summary.append("Generation Time: ").append(String.format("%.2f seconds", timeSeconds)).append("\n");
            summary.append("Average Time per Mesa: ").append(String.format("%.2f ms", (timeSeconds * 1000) / mesaIds.size())).append("\n");
            summary.append("Configuration Path: ").append(configPath).append("\n");

            // Add mesa details
            summary.append("\n=== Mesa Details ===\n");
            for (Integer mesaId : mesaIds) {
                Map<String, Object> mesaConfig = connectionDB.getMesaConfiguration(mesaId);
                if (mesaConfig != null) {
                    summary.append("Mesa ").append(mesaId).append(": ")
                            .append(mesaConfig.get("puesto_nombre")).append(" - ")
                            .append(mesaConfig.get("total_ciudadanos")).append(" citizens\n");
                }
            }

            Files.writeString(Paths.get(summaryPath), summary.toString());

        } catch (Exception e) {
            System.err.println("Error generating selective test summary: " + e.getMessage());
        }
    }

    private String sanitizeFileName(String fileName) {
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private void saveConfigurationsToFiles(Map<Integer, Map<String, Object>> configs, String basePath) {
        configs.forEach((mesaId, config) -> {
            try {
                String fileName = String.format("mesa_%d_config.json", mesaId);
                Path filePath = Paths.get(basePath, fileName);

                String jsonContent = votingManager.exportConfigurationToJson(config);
                Files.writeString(filePath, jsonContent, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

                configProgress.incrementAndGet();

            } catch (Exception e) {
                System.err.println("Error saving config for mesa " + mesaId + ": " + e.getMessage());
            }
        });
    }

    // =================== ORIGINAL VOTING MACHINE CONFIGURATION METHODS ===================

    /**
     * Generate configuration files for all voting machines
     */
    public void generateAllVotingMachineConfigurations(int electionId) {
        if (isGeneratingConfigs) {
            System.out.println("Configuration generation already in progress...");
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                isGeneratingConfigs = true;
                configProgress.set(0);

                System.out.println("=== Starting Full Voting Machine Configuration Generation ===");

                // Check if election is ready
                if (!votingManager.isElectionReadyForConfiguration(electionId)) {
                    System.err.println("Election " + electionId + " is not ready for configuration!");
                    return;
                }

                // Get all mesa IDs
                List<Integer> allMesaIds = connectionDB.getAllMesaIds();
                totalConfigsToGenerate = allMesaIds.size();

                System.out.println("Generating configurations for " + totalConfigsToGenerate + " voting machines...");

                // Create election-specific directory
                String electionConfigPath = CONFIG_BASE_PATH + "/election_" + electionId + "/full_deployment";
                Files.createDirectories(Paths.get(electionConfigPath));

                // Process in batches for better performance
                int batchSize = 100;
                List<List<Integer>> batches = createBatches(allMesaIds, batchSize);

                long startTime = System.currentTimeMillis();

                // Process batches in parallel
                List<CompletableFuture<Void>> batchFutures = new ArrayList<>();

                for (int i = 0; i < batches.size(); i++) {
                    final int batchIndex = i;
                    final List<Integer> batch = batches.get(i);

                    CompletableFuture<Void> batchFuture = CompletableFuture.runAsync(() -> {
                        processBatch(batch, electionId, electionConfigPath, batchIndex);
                    }, configExecutor);

                    batchFutures.add(batchFuture);
                }

                // Wait for all batches to complete
                CompletableFuture.allOf(batchFutures.toArray(new CompletableFuture[0])).join();

                long endTime = System.currentTimeMillis();
                double totalTimeSeconds = (endTime - startTime) / 1000.0;

                // Generate deployment summary
                generateDeploymentSummary(electionId, totalConfigsToGenerate, totalTimeSeconds);

                System.out.println("=== Configuration Generation Complete ===");
                System.out.printf("Generated %d configurations in %.2f seconds (%.2f configs/sec)%n",
                        totalConfigsToGenerate, totalTimeSeconds, totalConfigsToGenerate / totalTimeSeconds);

            } catch (Exception e) {
                System.err.println("Error during configuration generation: " + e.getMessage());
                e.printStackTrace();
            } finally {
                isGeneratingConfigs = false;
            }
        });
    }

    /**
     * Generate configurations for a specific department
     */
    public void generateDepartmentConfigurations(int departmentId, int electionId) {
        CompletableFuture.runAsync(() -> {
            try {
                System.out.println("=== Generating Configurations for Department " + departmentId + " ===");

                long startTime = System.currentTimeMillis();
                Map<Integer, Map<String, Object>> configs = votingManager.generateDepartmentConfigurations(departmentId, electionId);
                long endTime = System.currentTimeMillis();

                // Save configurations to files
                String deptConfigPath = CONFIG_BASE_PATH + "/election_" + electionId + "/department_" + departmentId;
                Files.createDirectories(Paths.get(deptConfigPath));

                saveConfigurationsToFiles(configs, deptConfigPath);

                double timeSeconds = (endTime - startTime) / 1000.0;
                System.out.printf("Department %d: Generated %d configurations in %.2f seconds%n",
                        departmentId, configs.size(), timeSeconds);

            } catch (Exception e) {
                System.err.println("Error generating department configurations: " + e.getMessage());
            }
        });
    }

    /**
     * Get configuration deployment status
     */
    public String getConfigurationStatus() {
        if (!isGeneratingConfigs) {
            return "Configuration generation: IDLE";
        }

        int progress = configProgress.get();
        double progressPercent = totalConfigsToGenerate > 0 ?
                (progress * 100.0 / totalConfigsToGenerate) : 0;

        return String.format("Configuration generation: IN PROGRESS - %d/%d (%.1f%%)",
                progress, totalConfigsToGenerate, progressPercent);
    }

    /**
     * Get configuration statistics for an election
     */
    public String getConfigurationStatistics(int electionId) {
        try {
            Map<String, Object> stats = votingManager.getConfigurationStatistics(electionId);
            StringBuilder sb = new StringBuilder("=== Configuration Statistics ===\n");

            stats.forEach((key, value) -> {
                sb.append(key).append(": ").append(value).append("\n");
            });

            return sb.toString();
        } catch (Exception e) {
            return "Error retrieving configuration statistics: " + e.getMessage();
        }
    }

    /**
     * Check if voting machine configurations exist for an election
     */
    public boolean hasVotingMachineConfigurations(int electionId) {
        String electionConfigPath = CONFIG_BASE_PATH + "/election_" + electionId;
        return Files.exists(Paths.get(electionConfigPath)) &&
                getConfigurationFileCount(electionConfigPath) > 0;
    }

    /**
     * Get configuration for a specific mesa (for debugging/testing)
     */
    public String getMesaConfiguration(int mesaId, int electionId) {
        try {
            Map<String, Object> config = votingManager.generateMachineConfiguration(mesaId, electionId);
            if (config != null) {
                return votingManager.exportConfigurationToJson(config);
            }
            return "Configuration not found for mesa " + mesaId;
        } catch (Exception e) {
            return "Error retrieving mesa configuration: " + e.getMessage();
        }
    }

    /**
     * Validate all generated configurations
     */
    public String validateAllConfigurations(int electionId) {
        try {
            String electionConfigPath = CONFIG_BASE_PATH + "/election_" + electionId;
            if (!Files.exists(Paths.get(electionConfigPath))) {
                return "No configurations found for election " + electionId;
            }

            AtomicInteger validConfigs = new AtomicInteger(0);
            AtomicInteger totalConfigs = new AtomicInteger(0);

            Files.walk(Paths.get(electionConfigPath))
                    .filter(path -> path.toString().endsWith(".json"))
                    .forEach(configFile -> {
                        try {
                            totalConfigs.incrementAndGet();
                            String content = Files.readString(configFile);
                            Map<String, Object> config = jsonMapper.readValue(content, Map.class);

                            if (votingManager.validateConfiguration(config)) {
                                validConfigs.incrementAndGet();
                            }
                        } catch (Exception e) {
                            System.err.println("Error validating " + configFile + ": " + e.getMessage());
                        }
                    });

            return String.format("Configuration Validation: %d/%d valid (%.1f%%)",
                    validConfigs.get(), totalConfigs.get(),
                    totalConfigs.get() > 0 ? (validConfigs.get() * 100.0 / totalConfigs.get()) : 0);

        } catch (Exception e) {
            return "Error during validation: " + e.getMessage();
        }
    }


    private List<List<Integer>> createBatches(List<Integer> items, int batchSize) {
        List<List<Integer>> batches = new ArrayList<>();
        for (int i = 0; i < items.size(); i += batchSize) {
            batches.add(items.subList(i, Math.min(i + batchSize, items.size())));
        }
        return batches;
    }

    private void processBatch(List<Integer> mesaIds, int electionId, String basePath, int batchIndex) {
        try {
            Map<Integer, Map<String, Object>> batchConfigs =
                    votingManager.generateBatchMachineConfigurations(mesaIds, electionId);

            saveConfigurationsToFiles(batchConfigs, basePath);

            int batchProgress = configProgress.addAndGet(mesaIds.size());

            if (batchIndex % 10 == 0) { // Log every 10th batch
                System.out.printf("Batch %d complete. Progress: %d/%d%n",
                        batchIndex, batchProgress, totalConfigsToGenerate);
            }

        } catch (Exception e) {
            System.err.println("Error processing batch " + batchIndex + ": " + e.getMessage());
        }
    }

    private void generateDeploymentSummary(int electionId, int totalConfigs, double timeSeconds) {
        try {
            String summaryPath = DEPLOYMENT_LOGS_PATH + "/deployment_summary_election_" + electionId + ".txt";
            StringBuilder summary = new StringBuilder();

            summary.append("=== Voting Machine Configuration Deployment Summary ===\n");
            summary.append("Election ID: ").append(electionId).append("\n");
            summary.append("Generation Date: ").append(new Date()).append("\n");
            summary.append("Total Configurations Generated: ").append(totalConfigs).append("\n");
            summary.append("Total Generation Time: ").append(String.format("%.2f seconds", timeSeconds)).append("\n");
            summary.append("Average Time per Configuration: ").append(String.format("%.2f ms", (timeSeconds * 1000) / totalConfigs)).append("\n");
            summary.append("Generation Rate: ").append(String.format("%.2f configs/second", totalConfigs / timeSeconds)).append("\n");
            summary.append("Storage Location: ").append(CONFIG_BASE_PATH + "/election_" + electionId).append("\n");

            // Add system info
            summary.append("\n=== System Information ===\n");
            summary.append("Available Processors: ").append(Runtime.getRuntime().availableProcessors()).append("\n");
            summary.append("Max Memory: ").append(Runtime.getRuntime().maxMemory() / (1024 * 1024)).append(" MB\n");
            summary.append("Database Pool Status: ").append(connectionDB.getPoolStats()).append("\n");

            Files.writeString(Paths.get(summaryPath), summary.toString());

        } catch (Exception e) {
            System.err.println("Error generating deployment summary: " + e.getMessage());
        }
    }

    private int getConfigurationFileCount(String path) {
        try {
            return (int) Files.walk(Paths.get(path))
                    .filter(p -> p.toString().endsWith(".json"))
                    .count();
        } catch (Exception e) {
            return 0;
        }
    }


    @Override
    public void registerVote(ReliableMessage newVote) {
        try {
            Vote vote = newVote.getMessage();
            int candidateId = Integer.parseInt(vote.vote);

            if (!election.isElectionActive()) {
                System.out.println("La elección no está activa. No se puede registrar el voto.");
                return;
            }

            election.addVoteToCandidate(candidateId, vote);
            connectionDB.storeVote(vote);
            System.out.println("Voto registrado exitosamente para candidato ID: " + candidateId);

            ServerUI ui = ServerUI.getInstance();
            if (ui != null) {
                // ui.showVoteInfo("Voto recibido para candidato ID: " + candidateId);
            }

        } catch (Exception e) {
            System.err.println("Error al registrar el voto: " + e.getMessage());
        }
    }

    @Override
    public String getElectionInfo() {
        return election.getElectionInfo();
    }

    @Override
    public void createElection(int id, String name, Date start, Date end) {
        election.registerElection(id, name, start, end);
        connectionDB.storeElection(id, name, start, end, ELECTION_STATUS.PRE.name());
        System.out.println("Elección creada correctamente: " + name);
    }

    @Override
    public void changeElectionStatus(ELECTION_STATUS status) {
        election.changeElectionStatus(status);
        System.out.println("Estado de la elección cambiado a: " + status);
    }

    @Override
    public void addCandidate(int id, String name, String party) {
        election.addCandidate(id, name, party);
        connectionDB.storeCandidate(id, name, party, election.getElectionId());
        System.out.println("Candidato añadido: " + name);
    }

    @Override
    public void editCandidate(int id, String newName, String newParty) {
        boolean success = election.editCandidate(id, newName, newParty);
        if (success) {
            System.out.println("Candidato editado con éxito.");
        } else {
            System.out.println("No se encontró el candidato con ID: " + id);
        }
    }

    @Override
    public void removeCandidate(int id) {
        boolean success = election.removeCandidate(id);
        if (success) {
            System.out.println("Candidato eliminado con éxito.");
        } else {
            System.out.println("No se encontró el candidato con ID: " + id);
        }
    }

    @Override
    public void loadCandidatesFromCSV(String filepath) {
        election.loadCandidatesFromCSV(filepath);
        System.out.println("Candidatos cargados desde: " + filepath);
    }

    @Override
    public List<Candidate> getCandidates() {
        return election.getCandidates();
    }

    public void showVotesPerCandidateReport(int electionId) {
        var result = reports.getTotalVotesPerCandidate(electionId);
        System.out.println("=== Total de votos por candidato ===");
        result.forEach((candidate, votes) -> System.out.println(candidate + ": " + votes));
    }

    public void showVotesPerCandidateByMachine(int electionId) {
        var result = reports.getVotesPerCandidateByMachine(electionId);
        System.out.println("=== Votos por candidato por máquina ===");
        result.forEach((machineId, map) -> {
            System.out.println("Máquina: " + machineId);
            map.forEach((candidate, votes) -> System.out.println("  " + candidate + ": " + votes));
        });
    }

    public void exportVotesPerMachineCSV(int electionId, String path) {
        var file = reports.exportVotesPerMachineCSV(electionId, path);
        System.out.println("Reporte por mesa exportado en: " + file.getAbsolutePath());
    }

    public void exportElectionResultsCSV(int electionId, String path) {
        var file = reports.exportElectionResultsCSV(electionId, path);
        System.out.println("Resultados de elecciones exportados en: " + file.getAbsolutePath());
    }

    @Override
    public String getTotalVotesPerCandidate(int electionId) {
        var result = reports.getTotalVotesPerCandidate(electionId);
        StringBuilder sb = new StringBuilder("=== Total de votos por candidato ===\n");
        result.forEach((candidate, votes) -> sb.append(candidate).append(": ").append(votes).append("\n"));
        return sb.toString();
    }

    @Override
    public String getVotesPerCandidateByMachine(int electionId) {
        var result = reports.getVotesPerCandidateByMachine(electionId);
        StringBuilder sb = new StringBuilder("=== Votos por candidato por máquina ===\n");
        result.forEach((machineId, map) -> {
            sb.append("Máquina: ").append(machineId).append("\n");
            map.forEach((candidate, votes) -> sb.append("  ").append(candidate).append(": ").append(votes).append("\n"));
        });
        return sb.toString();
    }

    public void shutdown() {
        if (configExecutor != null && !configExecutor.isShutdown()) {
            configExecutor.shutdown();
            try {
                if (!configExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                    configExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                configExecutor.shutdownNow();
            }
        }
        ConnectionDB.shutdown();
    }



    // =================== REPLACE THIS METHOD IN YOUR ServerControllerImpl.java ===================

    /**
     * Generate configuration for a specific puesto de votación (OPTIMIZED VERSION)
     */
    public void generatePuestoConfiguration(int puestoId, int electionId, String testName) {
        if (isGeneratingConfigs) {
            System.out.println("Configuration generation already in progress...");
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                isGeneratingConfigs = true;
                configProgress.set(0);

                System.out.println("=== Generating Configuration for Puesto " + puestoId + " ===");

                // Check if election is ready
                if (!votingManager.isElectionReadyForConfiguration(electionId)) {
                    System.err.println("Election " + electionId + " is not ready for configuration!");
                    return;
                }

                long startTime = System.currentTimeMillis();

                // Use the new optimized method
                Map<Integer, Map<String, Object>> configs =
                        votingManager.generatePuestoConfigurations(puestoId, electionId);

                if (configs.isEmpty()) {
                    System.out.println("No mesas found in puesto " + puestoId);
                    return;
                }

                totalConfigsToGenerate = configs.size();

                // Get puesto information for better naming
                Integer firstMesaId = configs.keySet().iterator().next();
                Map<String, Object> mesaInfo = (Map<String, Object>) configs.get(firstMesaId).get("mesaInfo");
                String puestoName = mesaInfo != null ? (String) mesaInfo.get("puesto_nombre") : "Unknown";
                String puestoAddress = mesaInfo != null ? (String) mesaInfo.get("puesto_direccion") : "";

                String enhancedTestName = testName + "_Puesto_" + puestoId + "_" + sanitizeFileName(puestoName);

                // Create test-specific directory
                String testConfigPath = CONFIG_BASE_PATH + "/election_" + electionId + "/selective_tests/" +
                        sanitizeFileName(enhancedTestName) + "_" + System.currentTimeMillis();
                Files.createDirectories(Paths.get(testConfigPath));

                // Save configurations to files
                saveConfigurationsToFiles(configs, testConfigPath);

                long endTime = System.currentTimeMillis();
                double totalTimeSeconds = (endTime - startTime) / 1000.0;

                // Generate enhanced test summary with puesto details
                generatePuestoTestSummary(electionId, puestoId, puestoName, puestoAddress,
                        configs, enhancedTestName, totalTimeSeconds, testConfigPath);

                System.out.println("=== Puesto Configuration Generation Complete ===");
                System.out.printf("Generated %d configurations for puesto '%s' in %.2f seconds%n",
                        configs.size(), puestoName, totalTimeSeconds);

                // Display puesto details
                displayPuestoDetails(configs, puestoName, puestoAddress);

            } catch (Exception e) {
                System.err.println("Error generating puesto configuration: " + e.getMessage());
                e.printStackTrace();
            } finally {
                isGeneratingConfigs = false;
                configProgress.set(0);
                totalConfigsToGenerate = 0;
            }
        });
    }

// =================== ADD THESE HELPER METHODS TO YOUR ServerControllerImpl.java ===================

    private void generatePuestoTestSummary(int electionId, int puestoId, String puestoName, String puestoAddress,
                                           Map<Integer, Map<String, Object>> configs, String testName,
                                           double timeSeconds, String configPath) {
        try {
            String summaryPath = DEPLOYMENT_LOGS_PATH + "/puesto_test_" + sanitizeFileName(testName) + ".txt";
            StringBuilder summary = new StringBuilder();

            summary.append("=== Puesto Configuration Test Summary ===\n");
            summary.append("Test Name: ").append(testName).append("\n");
            summary.append("Election ID: ").append(electionId).append("\n");
            summary.append("Puesto ID: ").append(puestoId).append("\n");
            summary.append("Puesto Name: ").append(puestoName).append("\n");
            summary.append("Puesto Address: ").append(puestoAddress).append("\n");
            summary.append("Test Date: ").append(new Date()).append("\n");
            summary.append("Mesas Configured: ").append(configs.size()).append("\n");
            summary.append("Generation Time: ").append(String.format("%.2f seconds", timeSeconds)).append("\n");
            summary.append("Average Time per Mesa: ").append(String.format("%.2f ms", (timeSeconds * 1000) / configs.size())).append("\n");
            summary.append("Configuration Path: ").append(configPath).append("\n");

            // Add mesa details
            summary.append("\n=== Mesa Details ===\n");
            int totalCitizens = 0;
            for (Map.Entry<Integer, Map<String, Object>> entry : configs.entrySet()) {
                Map<String, Object> config = entry.getValue();
                Map<String, Object> summaryInfo = (Map<String, Object>) config.get("summary");
                if (summaryInfo != null) {
                    int citizens = (Integer) summaryInfo.get("totalAssignedCitizens");
                    totalCitizens += citizens;
                    summary.append("Mesa ").append(entry.getKey()).append(": ").append(citizens).append(" citizens\n");
                }
            }

            summary.append("\nTotal Citizens in Puesto: ").append(totalCitizens).append("\n");
            summary.append("Average Citizens per Mesa: ").append(String.format("%.1f", (double) totalCitizens / configs.size())).append("\n");

            Files.writeString(Paths.get(summaryPath), summary.toString());

        } catch (Exception e) {
            System.err.println("Error generating puesto test summary: " + e.getMessage());
        }
    }

    private void displayPuestoDetails(Map<Integer, Map<String, Object>> configs, String puestoName, String puestoAddress) {
        System.out.println("\n=== Puesto Configuration Details ===");
        System.out.println("Puesto: " + puestoName);
        System.out.println("Address: " + puestoAddress);
        System.out.println("Total Mesas: " + configs.size());

        int totalCitizens = 0;
        List<Integer> mesaIds = new ArrayList<>(configs.keySet());
        Collections.sort(mesaIds);

        System.out.println("\nMesa Summary:");
        for (Integer mesaId : mesaIds.subList(0, Math.min(5, mesaIds.size()))) {
            Map<String, Object> config = configs.get(mesaId);
            Map<String, Object> summary = (Map<String, Object>) config.get("summary");
            if (summary != null) {
                int citizens = (Integer) summary.get("totalAssignedCitizens");
                totalCitizens += citizens;
                System.out.printf("  Mesa %d: %d citizens%n", mesaId, citizens);
            }
        }

        if (configs.size() > 5) {
            // Calculate total for remaining mesas
            for (Integer mesaId : mesaIds.subList(5, mesaIds.size())) {
                Map<String, Object> config = configs.get(mesaId);
                Map<String, Object> summary = (Map<String, Object>) config.get("summary");
                if (summary != null) {
                    totalCitizens += (Integer) summary.get("totalAssignedCitizens");
                }
            }
            System.out.println("  ... and " + (configs.size() - 5) + " more mesas");
        }

        System.out.println("Total Citizens: " + totalCitizens);
        System.out.println("Average Citizens per Mesa: " + String.format("%.1f", (double) totalCitizens / configs.size()));
    }








}