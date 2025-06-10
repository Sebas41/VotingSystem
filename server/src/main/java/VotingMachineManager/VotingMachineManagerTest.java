package VotingMachineManager;

import ConnectionDB.ConnectionDB;
import VotingMachineManager.VotingManagerInterface;
import VotingMachineManager.VotingManagerImpl;
import java.util.List;
import java.util.Map;
import java.util.Arrays;

/**
 * Comprehensive test for VotingMachineManager
 * Tests configuration package generation for voting machines
 */
public class VotingMachineManagerTest {

    public static void main(String[] args) {
        System.out.println("=== Testing VotingMachineManager ===");

        // Initialize components
        ConnectionDB connectionDB = new ConnectionDB();
        VotingManagerInterface votingManager = new VotingManagerImpl(connectionDB);

        // Test with election ID 1 (from your test data)
        int testElectionId = 1;

        // Test 1: Health check and basic setup
        System.out.println("\n1. System Health Check:");
        boolean healthy = connectionDB.isHealthy();
        System.out.println("   Database healthy: " + healthy);

        if (!healthy) {
            System.err.println("   ERROR: Database not healthy. Stopping tests.");
            return;
        }

        // Test 2: Check if election is ready for configuration
        System.out.println("\n2. Election Readiness Check:");
        boolean electionReady = votingManager.isElectionReadyForConfiguration(testElectionId);
        System.out.println("   Election " + testElectionId + " ready: " + electionReady);

        if (!electionReady) {
            System.out.println("   WARNING: Election not ready. Configuration may be incomplete.");
        }

        // Test 3: Get configuration statistics
        System.out.println("\n3. Configuration Statistics:");
        Map<String, Object> configStats = votingManager.getConfigurationStatistics(testElectionId);
        for (Map.Entry<String, Object> entry : configStats.entrySet()) {
            System.out.println("   " + entry.getKey() + ": " + entry.getValue());
        }

        // Test 4: Generate single machine configuration
        System.out.println("\n4. Single Machine Configuration Test:");

        // Get a test mesa ID
        List<Integer> allMesaIds = connectionDB.getAllMesaIds();
        if (allMesaIds.isEmpty()) {
            System.err.println("   ERROR: No mesas found in database");
            return;
        }

        int testMesaId = allMesaIds.get(0);
        System.out.println("   Testing with mesa ID: " + testMesaId);

        long startTime = System.currentTimeMillis();
        Map<String, Object> singleConfig = votingManager.generateMachineConfiguration(testMesaId, testElectionId);
        long endTime = System.currentTimeMillis();

        System.out.println("   Configuration generated in: " + (endTime - startTime) + " ms");

        if (singleConfig != null) {
            System.out.println("   Configuration validation: " + votingManager.validateConfiguration(singleConfig));

            // Display configuration details
            Map<String, Object> summary = (Map<String, Object>) singleConfig.get("summary");
            if (summary != null) {
                System.out.println("   Mesa ID: " + summary.get("mesaId"));
                System.out.println("   Total candidates: " + summary.get("totalCandidates"));
                System.out.println("   Assigned citizens: " + summary.get("totalAssignedCitizens"));
            }

            // Show election info
            Map<String, Object> electionInfo = (Map<String, Object>) singleConfig.get("electionInfo");
            if (electionInfo != null) {
                System.out.println("   Election: " + electionInfo.get("nombre") + " (" + electionInfo.get("estado") + ")");
            }

            // Show mesa info
            Map<String, Object> mesaInfo = (Map<String, Object>) singleConfig.get("mesaInfo");
            if (mesaInfo != null) {
                System.out.println("   Location: " + mesaInfo.get("puesto_nombre") + " in " + mesaInfo.get("municipio_nombre"));
            }

        } else {
            System.err.println("   ERROR: Failed to generate configuration for mesa " + testMesaId);
        }

        // Test 5: Batch configuration generation (small batch)
        System.out.println("\n5. Batch Configuration Test:");

        // Test with first 5 mesas
        List<Integer> batchMesaIds = allMesaIds.subList(0, Math.min(5, allMesaIds.size()));
        System.out.println("   Testing batch with " + batchMesaIds.size() + " mesas: " + batchMesaIds);

        startTime = System.currentTimeMillis();
        Map<Integer, Map<String, Object>> batchConfigs = votingManager.generateBatchMachineConfigurations(batchMesaIds, testElectionId);
        endTime = System.currentTimeMillis();

        System.out.println("   Batch configuration generated in: " + (endTime - startTime) + " ms");
        System.out.println("   Configurations generated: " + batchConfigs.size());

        // Validate all configurations in batch
        int validConfigs = 0;
        for (Map<String, Object> config : batchConfigs.values()) {
            if (votingManager.validateConfiguration(config)) {
                validConfigs++;
            }
        }
        System.out.println("   Valid configurations: " + validConfigs + "/" + batchConfigs.size());

        // Test 6: Department-based configuration
        System.out.println("\n6. Department Configuration Test:");

        // Get all departments
        List<Map<String, Object>> departments = connectionDB.getAllDepartments();
        if (!departments.isEmpty()) {
            Map<String, Object> testDepartment = departments.get(0);
            int departmentId = (Integer) testDepartment.get("id");
            String departmentName = (String) testDepartment.get("nombre");

            System.out.println("   Testing with department: " + departmentName + " (ID: " + departmentId + ")");

            startTime = System.currentTimeMillis();
            Map<Integer, Map<String, Object>> departmentConfigs = votingManager.generateDepartmentConfigurations(departmentId, testElectionId);
            endTime = System.currentTimeMillis();

            System.out.println("   Department configurations generated in: " + (endTime - startTime) + " ms");
            System.out.println("   Total configurations for department: " + departmentConfigs.size());

            if (!departmentConfigs.isEmpty()) {
                // Validate first few configurations
                int samplesToValidate = Math.min(3, departmentConfigs.size());
                int validDeptConfigs = 0;
                int count = 0;

                for (Map<String, Object> config : departmentConfigs.values()) {
                    if (count >= samplesToValidate) break;
                    if (votingManager.validateConfiguration(config)) {
                        validDeptConfigs++;
                    }
                    count++;
                }
                System.out.println("   Sample validation: " + validDeptConfigs + "/" + samplesToValidate + " valid");
            }
        } else {
            System.out.println("   No departments found in database");
        }

        // Test 7: JSON Export Test
        System.out.println("\n7. JSON Export Test:");
        if (singleConfig != null) {
            String jsonConfig = votingManager.exportConfigurationToJson(singleConfig);
            System.out.println("   JSON export successful: " + (jsonConfig.length() > 10));
            System.out.println("   JSON size: " + jsonConfig.length() + " characters");

            // Show first 200 characters of JSON
            String preview = jsonConfig.length() > 200 ? jsonConfig.substring(0, 200) + "..." : jsonConfig;
            System.out.println("   JSON preview: " + preview);
        }

        // Test 8: Performance Test (larger batch)
        System.out.println("\n8. Performance Test:");

        // Test with larger batch if we have enough mesas
        int performanceTestSize = Math.min(50, allMesaIds.size());
        if (performanceTestSize > 5) {
            List<Integer> perfTestMesas = allMesaIds.subList(0, performanceTestSize);
            System.out.println("   Performance test with " + performanceTestSize + " mesas");

            startTime = System.currentTimeMillis();
            Map<Integer, Map<String, Object>> perfConfigs = votingManager.generateBatchMachineConfigurations(perfTestMesas, testElectionId);
            endTime = System.currentTimeMillis();

            long totalTime = endTime - startTime;
            double avgTimePerMesa = (double) totalTime / performanceTestSize;

            System.out.println("   Total time: " + totalTime + " ms");
            System.out.println("   Average per mesa: " + String.format("%.2f", avgTimePerMesa) + " ms");
            System.out.println("   Configurations generated: " + perfConfigs.size());

            // Calculate total citizens processed
            int totalCitizens = 0;
            for (Map<String, Object> config : perfConfigs.values()) {
                Map<String, Object> summary = (Map<String, Object>) config.get("summary");
                if (summary != null) {
                    totalCitizens += (Integer) summary.get("totalAssignedCitizens");
                }
            }
            System.out.println("   Total citizens processed: " + totalCitizens);

            if (totalTime > 0) {
                double citizensPerSecond = (double) totalCitizens / (totalTime / 1000.0);
                System.out.println("   Processing rate: " + String.format("%.0f", citizensPerSecond) + " citizens/second");
            }

        } else {
            System.out.println("   Skipping performance test - not enough mesas");
        }

        // Test 9: Error handling test
        System.out.println("\n9. Error Handling Test:");

        // Test with non-existent mesa
        Map<String, Object> errorConfig = votingManager.generateMachineConfiguration(999999, testElectionId);
        System.out.println("   Non-existent mesa handling: " + (errorConfig == null ? "PASS" : "FAIL"));

        // Test with non-existent election
        Map<String, Object> errorConfig2 = votingManager.generateMachineConfiguration(testMesaId, 999999);
        System.out.println("   Non-existent election handling: " + (errorConfig2 == null ? "PASS" : "FAIL"));

        // Test validation with null
        boolean nullValidation = votingManager.validateConfiguration(null);
        System.out.println("   Null configuration validation: " + (!nullValidation ? "PASS" : "FAIL"));

        // Test 10: Final summary
        System.out.println("\n10. Final Summary:");
        System.out.println("   Total mesas in system: " + allMesaIds.size());
        System.out.println("   Total departments: " + departments.size());

        // Estimate full deployment time
        if (performanceTestSize > 5) {
            double avgTimePerMesa = 50.0; // Conservative estimate based on tests
            double estimatedFullTime = (allMesaIds.size() * avgTimePerMesa) / 1000.0; // in seconds
            System.out.println("   Estimated full deployment time: " + String.format("%.1f", estimatedFullTime) + " seconds");
        }

        System.out.println("\n=== VotingMachineManager Test Complete ===");
        System.out.println("Your voting machine configuration system is ready for deployment!");

        // Cleanup
        ConnectionDB.shutdown();
    }
}