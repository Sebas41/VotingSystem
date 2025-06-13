package Controller;

import ConnectionDB.ConnectionDB;
import Elections.ElectionImpl;
import Elections.ElectionInterface;
import Elections.models.Candidate;
import Elections.models.ELECTION_STATUS;
import Reports.ReportsManagerImpl;
import ReportsSystem.CitizenReportsConfiguration;
import ReportsSystem.ElectionReportsConfiguration;
import ReportsSystem.GeographicReportsConfiguration;
import ServerUI.ServerUI;
import VotingMachineManager.VotingManagerImpl;
import VotingSystem.VotingConfiguration;
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
    private VotingManagerImpl votingManager;
    private ReportsManagerImpl reportsManager;

    // Configuration storage paths
    // Configuration storage paths
    private static final String CONFIG_BASE_PATH = "server/src/main/java/VotingMachineManager/data/voting_configurations";
    private static final String DEPLOYMENT_LOGS_PATH = "server/src/main/java/VotingMachineManager/data/deployment_logs";
    private static final String REPORTS_BASE_PATH = "server/src/main/java/Reports/data/reports";
    // Threading for parallel processing
    private ExecutorService configExecutor;
    private ExecutorService reportsExecutor;
    private ObjectMapper jsonMapper;

    // Progress tracking
    private volatile boolean isGeneratingConfigs = false;
    private AtomicInteger configProgress = new AtomicInteger(0);
    private int totalConfigsToGenerate = 0;
    private int totalReportsToGenerate = 0; // NEW: Reports count tracking




    public ServerControllerImpl() {
        this.connectionDB = new ConnectionDB();
        this.election = new ElectionImpl(0, new Date(), new Date(), "");
        this.votingManager = new VotingManagerImpl(connectionDB);
        this.reportsManager = new ReportsManagerImpl(connectionDB);

        // Initialize JSON mapper
        this.jsonMapper = new ObjectMapper();
        this.jsonMapper.enable(SerializationFeature.INDENT_OUTPUT);

        // Initialize thread pool for configuration generation
        this.configExecutor = Executors.newFixedThreadPool(
                Math.min(10, Runtime.getRuntime().availableProcessors())
        );

        // Create necessary directories
        initializeDirectories();
        performReportsSystemTest();


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

    // NEW: Test reports system during startup
    private void performReportsSystemTest() {
        try {
            System.out.println("=== Testing Reports System ===");
            Map<String, Object> testResults = reportsManager.performSystemTest(1);

            boolean allTestsPassed = (Boolean) testResults.getOrDefault("allTestsPassed", false);
            if (allTestsPassed) {
                System.out.println("✓ Reports system test PASSED - All systems ready");
            } else {
                System.out.println("⚠ Reports system test FAILED - Check configuration");
                List<String> errors = (List<String>) testResults.get("errors");
                if (errors != null) {
                    errors.forEach(error -> System.out.println("  • " + error));
                }
            }
        } catch (Exception e) {
            System.err.println("Error testing reports system: " + e.getMessage());
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
// =================== FULL CITIZEN REPORTS METHODS ===================

    @Override
    public Map<String, Object> generateDepartmentCitizenReports(int departmentId, int electionId) {
        try {
            System.out.println("=== Generating FULL CITIZEN REPORTS for Department " + departmentId + " ===");

            Map<String, Object> result = reportsManager.generateDepartmentCitizenReports(departmentId, electionId);

            // Log results
            boolean success = (Boolean) result.getOrDefault("success", false);
            int totalCitizens = (Integer) result.getOrDefault("totalCitizens", 0);
            int successCount = (Integer) result.getOrDefault("successCount", 0);
            int errorCount = (Integer) result.getOrDefault("errorCount", 0);

            System.out.printf("Department %d reports: %s - %d/%d citizens processed (%d errors)%n",
                    departmentId,
                    success ? "SUCCESS" : "FAILED",
                    successCount,
                    totalCitizens,
                    errorCount);

            return result;

        } catch (Exception e) {
            System.err.println("Error generating department citizen reports: " + e.getMessage());
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", "Controller error: " + e.getMessage());
            errorResult.put("departmentId", departmentId);
            errorResult.put("electionId", electionId);
            return errorResult;
        }
    }

    @Override
    public Map<String, Object> generateMunicipalityCitizenReports(int municipalityId, int electionId) {
        try {
            System.out.println("=== Generating FULL CITIZEN REPORTS for Municipality " + municipalityId + " ===");

            Map<String, Object> result = reportsManager.generateMunicipalityCitizenReports(municipalityId, electionId);

            // Log results
            boolean success = (Boolean) result.getOrDefault("success", false);
            int totalCitizens = (Integer) result.getOrDefault("totalCitizens", 0);
            int successCount = (Integer) result.getOrDefault("successCount", 0);
            int errorCount = (Integer) result.getOrDefault("errorCount", 0);

            System.out.printf("Municipality %d reports: %s - %d/%d citizens processed (%d errors)%n",
                    municipalityId,
                    success ? "SUCCESS" : "FAILED",
                    successCount,
                    totalCitizens,
                    errorCount);

            return result;

        } catch (Exception e) {
            System.err.println("Error generating municipality citizen reports: " + e.getMessage());
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", "Controller error: " + e.getMessage());
            errorResult.put("municipalityId", municipalityId);
            errorResult.put("electionId", electionId);
            return errorResult;
        }
    }

    @Override
    public Map<String, Object> generatePuestoCitizenReports(int puestoId, int electionId) {
        try {
            System.out.println("=== Generating FULL CITIZEN REPORTS for Puesto " + puestoId + " ===");

            Map<String, Object> result = reportsManager.generatePuestoCitizenReports(puestoId, electionId);

            // Log results
            boolean success = (Boolean) result.getOrDefault("success", false);
            int totalCitizens = (Integer) result.getOrDefault("totalCitizens", 0);
            int successCount = (Integer) result.getOrDefault("successCount", 0);
            int errorCount = (Integer) result.getOrDefault("errorCount", 0);

            System.out.printf("Puesto %d reports: %s - %d/%d citizens processed (%d errors)%n",
                    puestoId,
                    success ? "SUCCESS" : "FAILED",
                    successCount,
                    totalCitizens,
                    errorCount);

            return result;

        } catch (Exception e) {
            System.err.println("Error generating puesto citizen reports: " + e.getMessage());
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", "Controller error: " + e.getMessage());
            errorResult.put("puestoId", puestoId);
            errorResult.put("electionId", electionId);
            return errorResult;
        }
    }



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
                Map<Integer, VotingConfiguration> configs =
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



    private void displayMesaDetails(Map<Integer, VotingConfiguration> configs) {
        System.out.println("\n=== Generated Configuration Details ===");
        configs.forEach((mesaId, config) -> {
            if (config != null && config.mesaInfo != null && config.citizens != null) {
                System.out.printf("Mesa %d: %d citizens, Location: %s%n",
                        mesaId,
                        config.citizens.length,
                        config.mesaInfo.puestoNombre
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

    private void saveConfigurationsToFiles(Map<Integer, VotingConfiguration> configs, String basePath) {
        configs.forEach((mesaId, config) -> {
            try {
                // Save as Ice binary file
                String iceFileName = String.format("mesa_%d_config.ice", mesaId);
                Path iceFilePath = Paths.get(basePath, iceFileName);
                votingManager.saveConfigurationToFile(config, iceFilePath.toString());

                // Also save as JSON for debugging/inspection
                String jsonFileName = String.format("mesa_%d_config.json", mesaId);
                Path jsonFilePath = Paths.get(basePath, jsonFileName);
                String jsonContent = votingManager.exportConfigurationToJson(config);
                Files.writeString(jsonFilePath, jsonContent, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

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


    public void generateDepartmentConfigurations(int departmentId, int electionId) {
        CompletableFuture.runAsync(() -> {
            try {
                System.out.println("=== Generating Configurations for Department " + departmentId + " ===");

                long startTime = System.currentTimeMillis();
                Map<Integer, VotingConfiguration> configs = votingManager.generateDepartmentConfigurations(departmentId, electionId);
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


    public boolean hasVotingMachineConfigurations(int electionId) {
        String electionConfigPath = CONFIG_BASE_PATH + "/election_" + electionId;
        return Files.exists(Paths.get(electionConfigPath)) &&
                getConfigurationFileCount(electionConfigPath) > 0;
    }


    public String getMesaConfiguration(int mesaId, int electionId) {
        try {
            VotingConfiguration config = votingManager.generateMachineConfiguration(mesaId, electionId);
            if (config != null) {
                return votingManager.exportConfigurationToJson(config);
            }
            return "Configuration not found for mesa " + mesaId;
        } catch (Exception e) {
            return "Error retrieving mesa configuration: " + e.getMessage();
        }
    }



    public String validateAllConfigurations(int electionId) {
        try {
            String electionConfigPath = CONFIG_BASE_PATH + "/election_" + electionId;
            if (!Files.exists(Paths.get(electionConfigPath))) {
                return "No configurations found for election " + electionId;
            }

            AtomicInteger validConfigs = new AtomicInteger(0);
            AtomicInteger totalConfigs = new AtomicInteger(0);
            AtomicInteger iceConfigs = new AtomicInteger(0);
            AtomicInteger jsonConfigs = new AtomicInteger(0);
            List<String> validationErrors = new ArrayList<>();

            // Validate Ice files (primary format)
            Files.walk(Paths.get(electionConfigPath))
                    .filter(path -> path.toString().endsWith(".ice"))
                    .forEach(configFile -> {
                        try {
                            totalConfigs.incrementAndGet();
                            iceConfigs.incrementAndGet();

                            // Load Ice configuration
                            VotingConfiguration config = votingManager.loadConfigurationFromFile(configFile.toString());

                            if (votingManager.validateConfiguration(config)) {
                                validConfigs.incrementAndGet();
                            } else {
                                String fileName = configFile.getFileName().toString();
                                validationErrors.add("Ice validation failed: " + fileName);
                            }
                        } catch (Exception e) {
                            String fileName = configFile.getFileName().toString();
                            String error = "Error validating Ice file " + fileName + ": " + e.getMessage();
                            System.err.println(error);
                            validationErrors.add(error);
                        }
                    });

            // Also validate JSON files (secondary format) for completeness
            Files.walk(Paths.get(electionConfigPath))
                    .filter(path -> path.toString().endsWith(".json"))
                    .forEach(configFile -> {
                        try {
                            jsonConfigs.incrementAndGet();
                            String content = Files.readString(configFile);

                            // Basic JSON structure validation
                            Map<String, Object> configMap = jsonMapper.readValue(content, Map.class);

                            // Check for required fields in JSON
                            if (configMap.containsKey("mesaInfo") &&
                                    configMap.containsKey("citizens") &&
                                    configMap.containsKey("candidates")) {
                                // JSON structure is valid
                            } else {
                                String fileName = configFile.getFileName().toString();
                                validationErrors.add("JSON structure validation failed: " + fileName + " - missing required fields");
                            }

                        } catch (Exception e) {
                            String fileName = configFile.getFileName().toString();
                            String error = "Error validating JSON file " + fileName + ": " + e.getMessage();
                            System.err.println(error);
                            validationErrors.add(error);
                        }
                    });

            // Build comprehensive validation report
            StringBuilder report = new StringBuilder();
            report.append("=== Configuration Validation Report ===\n");
            report.append("Election ID: ").append(electionId).append("\n");
            report.append("Validation Date: ").append(new Date()).append("\n");
            report.append("\n=== File Count Summary ===\n");
            report.append("Total Ice Files: ").append(iceConfigs.get()).append("\n");
            report.append("Total JSON Files: ").append(jsonConfigs.get()).append("\n");
            report.append("Total Configurations: ").append(totalConfigs.get()).append(" (based on Ice files)\n");

            report.append("\n=== Validation Results ===\n");
            if (totalConfigs.get() > 0) {
                double validationPercent = (validConfigs.get() * 100.0 / totalConfigs.get());
                report.append("Valid Configurations: ").append(validConfigs.get()).append("/").append(totalConfigs.get())
                        .append(" (").append(String.format("%.1f%%", validationPercent)).append(")\n");

                if (validationPercent == 100.0) {
                    report.append("Status: ✓ ALL CONFIGURATIONS VALID\n");
                } else if (validationPercent >= 95.0) {
                    report.append("Status: ⚠ MOSTLY VALID (some issues detected)\n");
                } else {
                    report.append("Status: ✗ VALIDATION ISSUES DETECTED\n");
                }
            } else {
                report.append("Status: ✗ NO CONFIGURATIONS FOUND\n");
            }

            // Add detailed error information if any
            if (!validationErrors.isEmpty()) {
                report.append("\n=== Validation Errors (").append(validationErrors.size()).append(" total) ===\n");
                validationErrors.stream()
                        .limit(10) // Show first 10 errors to avoid overwhelming output
                        .forEach(error -> report.append("• ").append(error).append("\n"));

                if (validationErrors.size() > 10) {
                    report.append("... and ").append(validationErrors.size() - 10).append(" more errors\n");
                }
            }

            // Add performance information
            report.append("\n=== Format Performance Notes ===\n");
            report.append("• Ice files: Binary format, optimized for production use\n");
            report.append("• JSON files: Text format, for debugging and inspection\n");
            report.append("• Primary validation based on Ice files\n");

            String finalReport = report.toString();

            // Save detailed validation report
            try {
                String reportPath = DEPLOYMENT_LOGS_PATH + "/validation_report_election_" + electionId + "_" +
                        System.currentTimeMillis() + ".txt";
                Files.writeString(Paths.get(reportPath), finalReport);
                System.out.println("Detailed validation report saved to: " + reportPath);
            } catch (Exception e) {
                System.err.println("Could not save validation report: " + e.getMessage());
            }

            return finalReport;

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

            Map<Integer, VotingConfiguration> batchConfigs =
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
                Map<Integer, VotingConfiguration> configs =
                        votingManager.generatePuestoConfigurations(puestoId, electionId);

                if (configs.isEmpty()) {
                    System.out.println("No mesas found in puesto " + puestoId);
                    return;
                }

                totalConfigsToGenerate = configs.size();

                // Get puesto information for better naming - CORRECTED
                Integer firstMesaId = configs.keySet().iterator().next();
                VotingConfiguration firstConfig = configs.get(firstMesaId);
                String puestoName = firstConfig != null && firstConfig.mesaInfo != null ?
                        firstConfig.mesaInfo.puestoNombre : "Unknown";
                String puestoAddress = firstConfig != null && firstConfig.mesaInfo != null ?
                        firstConfig.mesaInfo.puestoDireccion : "";

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


    private void generatePuestoTestSummary(int electionId, int puestoId, String puestoName, String puestoAddress,
                                           Map<Integer, VotingConfiguration> configs, String testName,
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
            for (Map.Entry<Integer, VotingConfiguration> entry : configs.entrySet()) {
                VotingConfiguration config = entry.getValue();
                if (config != null && config.citizens != null) {
                    int citizens = config.citizens.length;
                    totalCitizens += citizens;
                    summary.append("Mesa ").append(entry.getKey()).append(": ").append(citizens).append(" citizens\n");
                }
            }

            summary.append("\nTotal Citizens in Puesto: ").append(totalCitizens).append("\n");
            summary.append("Average Citizens per Mesa: ").append(String.format("%.1f", (double) totalCitizens / configs.size())).append("\n");

            // Add Ice-specific information
            summary.append("\n=== Configuration Format Details ===\n");
            summary.append("Primary Format: Ice Binary (.ice files)\n");
            summary.append("Secondary Format: JSON (.json files for debugging)\n");
            summary.append("Ice Serialization: Optimized for performance and size\n");

            Files.writeString(Paths.get(summaryPath), summary.toString());

        } catch (Exception e) {
            System.err.println("Error generating puesto test summary: " + e.getMessage());
        }
    }

private void displayPuestoDetails(Map<Integer, VotingConfiguration> configs, String puestoName, String puestoAddress) {
    System.out.println("\n=== Puesto Configuration Details ===");
    System.out.println("Puesto: " + puestoName);
    System.out.println("Address: " + puestoAddress);
    System.out.println("Total Mesas: " + configs.size());

    int totalCitizens = 0;
    List<Integer> mesaIds = new ArrayList<>(configs.keySet());
    Collections.sort(mesaIds);

    System.out.println("\nMesa Summary:");
    for (Integer mesaId : mesaIds.subList(0, Math.min(5, mesaIds.size()))) {
        VotingConfiguration config = configs.get(mesaId);
        if (config != null && config.citizens != null) {
            int citizens = config.citizens.length;
            totalCitizens += citizens;
            System.out.printf("  Mesa %d: %d citizens%n", mesaId, citizens);
        }
    }

    if (configs.size() > 5) {
        // Calculate total for remaining mesas
        for (Integer mesaId : mesaIds.subList(5, mesaIds.size())) {
            VotingConfiguration config = configs.get(mesaId);
            if (config != null && config.citizens != null) {
                totalCitizens += config.citizens.length;
            }
        }
        System.out.println("  ... and " + (configs.size() - 5) + " more mesas");
    }

    System.out.println("Total Citizens: " + totalCitizens);
    System.out.println("Average Citizens per Mesa: " + String.format("%.1f", (double) totalCitizens / configs.size()));
}

    @Override
    public String generateCitizenReport(String documento, int electionId) {
        try {
            CitizenReportsConfiguration config = reportsManager.generateCitizenReport(documento, electionId);
            if (config != null) {
                return reportsManager.exportConfigurationToJson(config);
            }
            return "No se encontró información para el documento: " + documento;
        } catch (Exception e) {
            return "Error generando reporte ciudadano: " + e.getMessage();
        }
    }


    @Override
    public List<String> searchCitizenReports(String nombre, String apellido, int electionId, int limit) {
        try {
            List<CitizenReportsConfiguration> configs =
                    reportsManager.searchCitizenReports(nombre, apellido, electionId, limit);

            return configs.stream()
                    .map(config -> reportsManager.exportConfigurationToJson(config))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            return Arrays.asList("Error en búsqueda de ciudadanos: " + e.getMessage());
        }
    }

    @Override
    public boolean validateCitizenEligibility(String documento) {
        try {
            return reportsManager.validateCitizenEligibility(documento);
        } catch (Exception e) {
            System.err.println("Error validando elegibilidad ciudadano: " + e.getMessage());
            return false;
        }
    }


    @Override
    public Map<String, String> generateBatchCitizenReports(List<String> documentos, int electionId) {
        try {
            Map<String, CitizenReportsConfiguration> configs =
                    reportsManager.generateBatchCitizenReports(documentos, electionId);

            Map<String, String> jsonReports = new HashMap<>();
            configs.forEach((documento, config) -> {
                if (config != null) {
                    jsonReports.put(documento, reportsManager.exportConfigurationToJson(config));
                }
            });

            return jsonReports;
        } catch (Exception e) {
            Map<String, String> errorResult = new HashMap<>();
            errorResult.put("error", "Error generando reportes en lote: " + e.getMessage());
            return errorResult;
        }
    }


    @Override
    public List<String> generateMesaCitizenReports(int mesaId, int electionId) {
        try {
            List<CitizenReportsConfiguration> configs =
                    reportsManager.generateMesaCitizenReports(mesaId, electionId);

            return configs.stream()
                    .map(config -> reportsManager.exportConfigurationToJson(config))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            return Arrays.asList("Error generando reportes de mesa: " + e.getMessage());
        }
    }


    @Override
    public String generateElectionResultsReport(int electionId) {
        try {
            ElectionReportsConfiguration config = reportsManager.generateElectionResultsReport(electionId);
            if (config != null) {
                return reportsManager.exportConfigurationToJson(config);
            }
            return "No se encontraron resultados para la elección: " + electionId;
        } catch (Exception e) {
            return "Error generando reporte de resultados: " + e.getMessage();
        }
    }

    @Override
    public String getElectionStatistics(int electionId) {
        try {
            Map<String, Object> stats = reportsManager.getElectionStatistics(electionId);
            return jsonMapper.writeValueAsString(stats);
        } catch (Exception e) {
            return "Error obteniendo estadísticas de elección: " + e.getMessage();
        }
    }





    @Override
    public List<Map<String, Object>> getAvailableElections() {
        try {
            return reportsManager.getAvailableElections();
        } catch (Exception e) {
            System.err.println("Error obteniendo elecciones disponibles: " + e.getMessage());
            return new ArrayList<>();
        }
    }


    @Override
    public String generateDepartmentReport(int departmentId, int electionId) {
        try {
            GeographicReportsConfiguration config =
                    reportsManager.generateDepartmentReport(departmentId, electionId);
            if (config != null) {
                return reportsManager.exportConfigurationToJson(config);
            }
            return "No se encontró información para el departamento: " + departmentId;
        } catch (Exception e) {
            return "Error generando reporte departamental: " + e.getMessage();
        }
    }


    @Override
    public String generateMunicipalityReport(int municipalityId, int electionId) {
        try {
            GeographicReportsConfiguration config =
                    reportsManager.generateMunicipalityReport(municipalityId, electionId);
            if (config != null) {
                return reportsManager.exportConfigurationToJson(config);
            }
            return "No se encontró información para el municipio: " + municipalityId;
        } catch (Exception e) {
            return "Error generando reporte municipal: " + e.getMessage();
        }
    }

    @Override
    public String generatePuestoReport(int puestoId, int electionId) {
        try {
            GeographicReportsConfiguration config =
                    reportsManager.generatePuestoReport(puestoId, electionId);
            if (config != null) {
                return reportsManager.exportConfigurationToJson(config);
            }
            return "No se encontró información para el puesto: " + puestoId;
        } catch (Exception e) {
            return "Error generando reporte de puesto: " + e.getMessage();
        }
    }

    @Override
    public String getReportsStatistics(int electionId) {
        try {
            Map<String, Object> stats = reportsManager.getReportsStatistics(electionId);
            StringBuilder sb = new StringBuilder("=== Estadísticas del Sistema de Reportes ===\n");

            stats.forEach((key, value) -> {
                sb.append(key).append(": ").append(value).append("\n");
            });

            return sb.toString();
        } catch (Exception e) {
            return "Error obteniendo estadísticas de reportes: " + e.getMessage();
        }
    }

    @Override
    public boolean isElectionReadyForReports(int electionId) {
        try {
            return reportsManager.isElectionReadyForReports(electionId);
        } catch (Exception e) {
            System.err.println("Error verificando preparación de elección para reportes: " + e.getMessage());
            return false;
        }
    }
    @Override
    public String validateReportsSystem() {
        try {
            Map<String, Object> testResults = reportsManager.performSystemTest(election.getElectionId());

            StringBuilder report = new StringBuilder("=== Validación del Sistema de Reportes ===\n");
            report.append("Fecha de validación: ").append(new Date()).append("\n");

            boolean allTestsPassed = (Boolean) testResults.getOrDefault("allTestsPassed", false);
            report.append("Estado general: ").append(allTestsPassed ? "✓ PASÓ" : "✗ FALLÓ").append("\n");

            Integer totalTests = (Integer) testResults.get("totalTests");
            if (totalTests != null) {
                report.append("Pruebas ejecutadas: ").append(totalTests).append("\n");
            }

            @SuppressWarnings("unchecked")
            List<String> errors = (List<String>) testResults.get("errors");
            if (errors != null && !errors.isEmpty()) {
                report.append("\nErrores encontrados:\n");
                errors.forEach(error -> report.append("• ").append(error).append("\n"));
            }

            // Add component-specific validations
            report.append("\n=== Validaciones Específicas ===\n");
            report.append("Base de datos: ").append(testResults.get("databaseHealthy")).append("\n");
            report.append("Elección lista: ").append(testResults.get("electionReady")).append("\n");
            report.append("Serialización Ice: ").append(testResults.get("iceSerializationWorks")).append("\n");
            report.append("Validaciones: ").append(testResults.get("validationWorks")).append("\n");

            return report.toString();
        } catch (Exception e) {
            return "Error validando sistema de reportes: " + e.getMessage();
        }
    }





    @Override
    public boolean exportCitizenReport(String documento, int electionId, String filePath) {
        try {
            CitizenReportsConfiguration config = reportsManager.generateCitizenReport(documento, electionId);
            if (config != null) {
                return reportsManager.saveConfigurationToFile(config, filePath);
            }
            return false;
        } catch (Exception e) {
            System.err.println("Error exportando reporte ciudadano: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean exportElectionResultsReport(int electionId, String filePath) {
        try {
            ElectionReportsConfiguration config = reportsManager.generateElectionResultsReport(electionId);
            if (config != null) {
                return reportsManager.saveConfigurationToFile(config, filePath);
            }
            return false;
        } catch (Exception e) {
            System.err.println("Error exportando reporte de resultados: " + e.getMessage());
            return false;
        }
    }


    @Override
    public boolean exportGeographicReport(int locationId, String locationType, int electionId, String filePath) {
        try {
            GeographicReportsConfiguration config = null;

            switch (locationType.toLowerCase()) {
                case "department":
                case "departamento":
                    config = reportsManager.generateDepartmentReport(locationId, electionId);
                    break;
                case "municipality":
                case "municipio":
                    config = reportsManager.generateMunicipalityReport(locationId, electionId);
                    break;
                case "puesto":
                    config = reportsManager.generatePuestoReport(locationId, electionId);
                    break;
                default:
                    System.err.println("Tipo de ubicación no soportado: " + locationType);
                    return false;
            }

            if (config != null) {
                return reportsManager.saveConfigurationToFile(config, filePath);
            }
            return false;
        } catch (Exception e) {
            System.err.println("Error exportando reporte geográfico: " + e.getMessage());
            return false;
        }
    }







}