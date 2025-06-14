package Controller;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Collectors;

/**
 * Interactive test class for Reports Manager functionality
 * Tests citizen lookup, election results, and geographic reports
 */
public class ReportsManagerTest {

    private static ServerControllerImpl serverController;
    private static Scanner scanner;
    private static final int ELECTION_ID = 1; // Test election ID

   public static void main(String[] args) {
    System.out.println("=== REPORTS MANAGER COMPREHENSIVE TEST ===");

    // Initialize components
    serverController = new ServerControllerImpl();
    scanner = new Scanner(System.in);

    try {
        // Main interactive loop
        boolean running = true;
        while (running) {
            displayMainMenu();
            int choice = getMenuChoice(1, 15); // ✅ CAMBIAR DE 12 a 15

            switch (choice) {
                case 1:
                    testSystemValidation();
                    break;
                case 2:
                    testCitizenLookup();
                    break;
                case 3:
                    testCitizenSearch();
                    break;
                case 4:
                    testBatchCitizenReports();
                    break;
                case 5:
                    testElectionResults();
                    break;
                case 6:
                    testGeographicReports();
                    break;
                case 7:
                    testMesaCitizenReports();
                    break;
                case 8:
                    testExportFunctionality();
                    break;
                case 9:
                    viewReportsStatistics();
                    break;
                case 10:
                    testCitizenEligibility();
                    break;
                case 11:
                    testFullDepartmentCitizenReports();  
                    break;
                case 12:
                    testFullMunicipalityCitizenReports(); 
                    break;
                case 13:
                    testFullPuestoCitizenReports();       
                    break;
                case 14:
                    performComprehensiveTest();           
                    break;
                case 15:
                    running = false;                     
                    break;
            }
        }

    } catch (Exception e) {
        System.err.println("Error during testing: " + e.getMessage());
        e.printStackTrace();
    } finally {
        cleanup();
    }
}

    private static void displayMainMenu() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("REPORTS MANAGER TEST MENU");
        System.out.println("=".repeat(70));
        System.out.println("1.   System Validation & Health Check");
        System.out.println("2.   Test Citizen Lookup (by Document)");
        System.out.println("3.   Test Citizen Search (by Name)");
        System.out.println("4.   Test Batch Citizen Reports");
        System.out.println("5.   Test Election Results Reports");
        System.out.println("6.   Test Geographic Reports");
        System.out.println("7.   Test Mesa Citizen Reports");
        System.out.println("8.   Test Export Functionality");
        System.out.println("9.   View Reports Statistics");
        System.out.println("10.  Test Citizen Eligibility");
        System.out.println("11.  🆕 Test FULL Department Citizen Reports");  // NUEVO
        System.out.println("12.  🆕 Test FULL Municipality Citizen Reports"); // NUEVO
        System.out.println("13.  🆕 Test FULL Puesto Citizen Reports");       // NUEVO
        System.out.println("14.  Comprehensive Test Suite");
        System.out.println("15.  Exit");
        System.out.println("=".repeat(70));
        System.out.print("Select option (1-15): ");
    }

    private static void testSystemValidation() {
        System.out.println("\n=== 🔧 SYSTEM VALIDATION & HEALTH CHECK ===");

        System.out.println("Running comprehensive system validation...");
        String validationReport = serverController.validateReportsSystem();
        System.out.println(validationReport);

        System.out.println("\nChecking election readiness...");
        boolean isReady = serverController.isElectionReadyForReports(ELECTION_ID);
        System.out.println("Election " + ELECTION_ID + " ready for reports: " +
                (isReady ? " YES" : " NO"));

        System.out.println("\nGetting available elections...");
        List<Map<String, Object>> elections = serverController.getAvailableElections();
        System.out.println("Available elections: " + elections.size());
        elections.forEach(election ->
                System.out.println("  - " + election.get("nombre") + " (ID: " + election.get("id") +
                        ", Status: " + election.get("estado") + ")")
        );

        System.out.println("\n System validation complete!");
    }

    private static void testCitizenLookup() {
        System.out.println("\n===  CITIZEN LOOKUP TEST ===");

        System.out.print("Enter citizen document ID (e.g., 12345678): ");
        String documento = scanner.nextLine().trim();

        if (documento.isEmpty()) {
            System.out.println("Using test document: 1000000001");
            documento = "1000000001";
        }

        System.out.println("Searching for citizen with document: " + documento);

        // Test eligibility first
        boolean isEligible = serverController.validateCitizenEligibility(documento);
        System.out.println("Citizen eligibility: " + (isEligible ? " ELIGIBLE" : " NOT ELIGIBLE"));

        if (isEligible) {
            // Generate citizen report
            String citizenReport = serverController.generateCitizenReport(documento, ELECTION_ID);

            System.out.println("\n📋 CITIZEN REPORT:");
            System.out.println("=".repeat(50));

            if (citizenReport.contains("Error") || citizenReport.contains("No se encontró")) {
                System.out.println(" " + citizenReport);
            } else {
                // Parse and display key information (simplified)
                System.out.println(" Report generated successfully!");
                System.out.println(" Report preview (JSON format):");

                // Show first 500 characters for preview
                String preview = citizenReport.length() > 500 ?
                        citizenReport.substring(0, 500) + "..." : citizenReport;
                System.out.println(preview);

                System.out.print("\nShow full report? (y/n): ");
                if (scanner.nextLine().trim().toLowerCase().startsWith("y")) {
                    System.out.println("\n FULL CITIZEN REPORT:");
                    System.out.println(citizenReport);
                }
            }
        } else {
            System.out.println(" Cannot generate report for ineligible citizen");
        }
    }

    private static void testFullDepartmentCitizenReports() {
        System.out.println("\n=== 🏛️ FULL DEPARTMENT CITIZEN REPORTS TEST ===");

        System.out.print("Enter department ID (e.g., 1): ");
        int departmentId = getMenuChoice(1, 999);

        System.out.println("⚠️  WARNING: This will generate INDIVIDUAL ICE files for ALL citizens in department " + departmentId);
        System.out.println("This could take a significant amount of time and disk space.");
        System.out.print("Continue? (y/n): ");

        if (!scanner.nextLine().trim().toLowerCase().startsWith("y")) {
            System.out.println("Operation cancelled.");
            return;
        }

        System.out.println("🚀 Starting FULL citizen reports generation for department " + departmentId + "...");
        long startTime = System.currentTimeMillis();

        Map<String, Object> result = serverController.generateDepartmentCitizenReports(departmentId, ELECTION_ID);

        long endTime = System.currentTimeMillis();
        double totalTimeSeconds = (endTime - startTime) / 1000.0;

        System.out.println("\n📊 DEPARTMENT CITIZEN REPORTS RESULTS:");
        System.out.println("=".repeat(60));

        boolean success = (Boolean) result.getOrDefault("success", false);
        System.out.println("🎯 Status: " + (success ? "✅ SUCCESS" : "❌ FAILED"));

        if (success) {
            int totalCitizens = (Integer) result.getOrDefault("totalCitizens", 0);
            int successCount = (Integer) result.getOrDefault("successCount", 0);
            int errorCount = (Integer) result.getOrDefault("errorCount", 0);
            String reportDirectory = (String) result.getOrDefault("reportDirectory", "N/A");

            System.out.println("👥 Total Citizens: " + totalCitizens);
            System.out.println("✅ Successful Reports: " + successCount);
            System.out.println("❌ Failed Reports: " + errorCount);
            System.out.println("⏱️  Generation Time: " + String.format("%.2f seconds", totalTimeSeconds));
            System.out.println("📈 Success Rate: " + String.format("%.1f%%", (successCount * 100.0 / totalCitizens)));
            System.out.println("📁 Report Directory: " + reportDirectory);

            if (totalCitizens > 0) {
                System.out.println("⚡ Average Time per Report: " +
                        String.format("%.2f ms", (totalTimeSeconds * 1000) / totalCitizens));
            }

            // Show errors if any
            @SuppressWarnings("unchecked")
            List<String> errors = (List<String>) result.get("errors");
            if (errors != null && !errors.isEmpty()) {
                System.out.println("\n⚠️  ERRORS ENCOUNTERED:");
                errors.stream().limit(5).forEach(error -> System.out.println("  • " + error));
                if (errors.size() > 5) {
                    System.out.println("  ... and " + (errors.size() - 5) + " more errors");
                }
            }
        } else {
            String error = (String) result.get("error");
            System.out.println("❌ Error: " + error);
        }
    }

    private static void testFullMunicipalityCitizenReports() {
        System.out.println("\n=== 🏘️ FULL MUNICIPALITY CITIZEN REPORTS TEST ===");

        System.out.print("Enter municipality ID (e.g., 1): ");
        int municipalityId = getMenuChoice(1, 999);

        System.out.println("⚠️  WARNING: This will generate INDIVIDUAL ICE files for ALL citizens in municipality " + municipalityId);
        System.out.println("This could take a significant amount of time and disk space.");
        System.out.print("Continue? (y/n): ");

        if (!scanner.nextLine().trim().toLowerCase().startsWith("y")) {
            System.out.println("Operation cancelled.");
            return;
        }

        System.out.println("🚀 Starting FULL citizen reports generation for municipality " + municipalityId + "...");
        long startTime = System.currentTimeMillis();

        Map<String, Object> result = serverController.generateMunicipalityCitizenReports(municipalityId, ELECTION_ID);

        long endTime = System.currentTimeMillis();
        double totalTimeSeconds = (endTime - startTime) / 1000.0;

        System.out.println("\n📊 MUNICIPALITY CITIZEN REPORTS RESULTS:");
        System.out.println("=".repeat(60));

        boolean success = (Boolean) result.getOrDefault("success", false);
        System.out.println("🎯 Status: " + (success ? "✅ SUCCESS" : "❌ FAILED"));

        if (success) {
            int totalCitizens = (Integer) result.getOrDefault("totalCitizens", 0);
            int successCount = (Integer) result.getOrDefault("successCount", 0);
            int errorCount = (Integer) result.getOrDefault("errorCount", 0);
            String reportDirectory = (String) result.getOrDefault("reportDirectory", "N/A");

            System.out.println("👥 Total Citizens: " + totalCitizens);
            System.out.println("✅ Successful Reports: " + successCount);
            System.out.println("❌ Failed Reports: " + errorCount);
            System.out.println("⏱️  Generation Time: " + String.format("%.2f seconds", totalTimeSeconds));
            System.out.println("📈 Success Rate: " + String.format("%.1f%%", (successCount * 100.0 / totalCitizens)));
            System.out.println("📁 Report Directory: " + reportDirectory);

            if (totalCitizens > 0) {
                System.out.println("⚡ Average Time per Report: " +
                        String.format("%.2f ms", (totalTimeSeconds * 1000) / totalCitizens));
            }

            // Show errors if any
            @SuppressWarnings("unchecked")
            List<String> errors = (List<String>) result.get("errors");
            if (errors != null && !errors.isEmpty()) {
                System.out.println("\n⚠️  ERRORS ENCOUNTERED:");
                errors.stream().limit(5).forEach(error -> System.out.println("  • " + error));
                if (errors.size() > 5) {
                    System.out.println("  ... and " + (errors.size() - 5) + " more errors");
                }
            }
        } else {
            String error = (String) result.get("error");
            System.out.println("❌ Error: " + error);
        }
    }

    private static void testFullPuestoCitizenReports() {
        System.out.println("\n=== 🗳️ FULL PUESTO CITIZEN REPORTS TEST ===");

        System.out.print("Enter puesto ID (e.g., 1): ");
        int puestoId = getMenuChoice(1, 999999);

        System.out.println("⚠️  WARNING: This will generate INDIVIDUAL ICE files for ALL citizens in puesto " + puestoId);
        System.out.println("This could take some time depending on the number of citizens.");
        System.out.print("Continue? (y/n): ");

        if (!scanner.nextLine().trim().toLowerCase().startsWith("y")) {
            System.out.println("Operation cancelled.");
            return;
        }

        System.out.println("🚀 Starting FULL citizen reports generation for puesto " + puestoId + "...");
        long startTime = System.currentTimeMillis();

        Map<String, Object> result = serverController.generatePuestoCitizenReports(puestoId, ELECTION_ID);

        long endTime = System.currentTimeMillis();
        double totalTimeSeconds = (endTime - startTime) / 1000.0;

        System.out.println("\n📊 PUESTO CITIZEN REPORTS RESULTS:");
        System.out.println("=".repeat(60));

        boolean success = (Boolean) result.getOrDefault("success", false);
        System.out.println("🎯 Status: " + (success ? "✅ SUCCESS" : "❌ FAILED"));

        if (success) {
            int totalCitizens = (Integer) result.getOrDefault("totalCitizens", 0);
            int successCount = (Integer) result.getOrDefault("successCount", 0);
            int errorCount = (Integer) result.getOrDefault("errorCount", 0);
            String reportDirectory = (String) result.getOrDefault("reportDirectory", "N/A");

            System.out.println("👥 Total Citizens: " + totalCitizens);
            System.out.println("✅ Successful Reports: " + successCount);
            System.out.println("❌ Failed Reports: " + errorCount);
            System.out.println("⏱️  Generation Time: " + String.format("%.2f seconds", totalTimeSeconds));

            if (totalCitizens > 0) {
                System.out.println("📈 Success Rate: " + String.format("%.1f%%", (successCount * 100.0 / totalCitizens)));
                System.out.println("⚡ Average Time per Report: " +
                        String.format("%.2f ms", (totalTimeSeconds * 1000) / totalCitizens));
            }

            System.out.println("📁 Report Directory: " + reportDirectory);

            // Show errors if any
            @SuppressWarnings("unchecked")
            List<String> errors = (List<String>) result.get("errors");
            if (errors != null && !errors.isEmpty()) {
                System.out.println("\n⚠️  ERRORS ENCOUNTERED:");
                errors.stream().limit(5).forEach(error -> System.out.println("  • " + error));
                if (errors.size() > 5) {
                    System.out.println("  ... and " + (errors.size() - 5) + " more errors");
                }
            }
        } else {
            String error = (String) result.get("error");
            System.out.println("❌ Error: " + error);
        }
    }

    // También actualizar el método getMenuChoice para el nuevo rango

    private static void testCitizenSearch() {
        System.out.println("\n=== 🔍 CITIZEN SEARCH TEST ===");

        System.out.print("Enter first name (or partial, e.g., 'Juan'): ");
        String nombre = scanner.nextLine().trim();

        System.out.print("Enter last name (or partial, e.g., 'Garcia'): ");
        String apellido = scanner.nextLine().trim();

        if (nombre.isEmpty() && apellido.isEmpty()) {
            System.out.println("Using test search: nombre='Carlos', apellido='Rodriguez'");
            nombre = "Carlos";
            apellido = "Rodriguez";
        }

        System.out.print("Enter search limit (default 10): ");
        String limitStr = scanner.nextLine().trim();
        int limit = limitStr.isEmpty() ? 10 : Integer.parseInt(limitStr);

        System.out.println("Searching for citizens: '" + nombre + "' '" + apellido + "' (limit: " + limit + ")");

        List<String> searchResults = serverController.searchCitizenReports(nombre, apellido, ELECTION_ID, limit);

        System.out.println("\n🔍 SEARCH RESULTS:");
        System.out.println("=".repeat(50));
        System.out.println("Found " + searchResults.size() + " matching citizens");

        if (!searchResults.isEmpty()) {
            for (int i = 0; i < Math.min(3, searchResults.size()); i++) {
                System.out.println("\n📋 Result " + (i + 1) + ":");
                String result = searchResults.get(i);
                if (result.contains("Error")) {
                    System.out.println(" " + result);
                } else {
                    // Show preview
                    String preview = result.length() > 300 ? result.substring(0, 300) + "..." : result;
                    System.out.println(preview);
                }
            }

            if (searchResults.size() > 3) {
                System.out.println("\n... and " + (searchResults.size() - 3) + " more results");
            }

            System.out.print("\nShow detailed results? (y/n): ");
            if (scanner.nextLine().trim().toLowerCase().startsWith("y")) {
                searchResults.forEach(result -> {
                    System.out.println("\n" + "─".repeat(40));
                    System.out.println(result);
                });
            }
        } else {
            System.out.println(" No citizens found with the specified criteria");
        }
    }

    private static void testBatchCitizenReports() {
        System.out.println("\n=== 📋 BATCH CITIZEN REPORTS TEST ===");

        System.out.print("Enter document IDs (comma-separated, e.g., 1000000001,1000000002,1000000003): ");
        String documentsInput = scanner.nextLine().trim();

        List<String> documentos;
        if (documentsInput.isEmpty()) {
            System.out.println("Using test documents: 1000000001, 1000000002, 1000000003");
            documentos = Arrays.asList("1000000001", "1000000002", "1000000003");
        } else {
            documentos = Arrays.stream(documentsInput.split(","))
                    .map(String::trim)
                    .collect(Collectors.toList());
        }

        System.out.println("Generating batch reports for " + documentos.size() + " citizens...");

        Map<String, String> batchResults = serverController.generateBatchCitizenReports(documentos, ELECTION_ID);

        System.out.println("\n📋 BATCH RESULTS:");
        System.out.println("=".repeat(50));
        System.out.println("Generated " + batchResults.size() + " reports");

        int successCount = 0;
        int errorCount = 0;

        for (Map.Entry<String, String> entry : batchResults.entrySet()) {
            String documento = entry.getKey();
            String report = entry.getValue();

            if (report.contains("Error") || report.contains("error")) {
                errorCount++;
                System.out.println(" " + documento + ": " +
                        (report.length() > 100 ? report.substring(0, 100) + "..." : report));
            } else {
                successCount++;
                System.out.println(" " + documento + ": Report generated (" + report.length() + " chars)");
            }
        }

        System.out.println("\n📊 BATCH SUMMARY:");
        System.out.println(" Successful: " + successCount);
        System.out.println(" Errors: " + errorCount);
        System.out.println("📈 Success Rate: " + String.format("%.1f%%", (successCount * 100.0 / documentos.size())));
    }

    private static void testElectionResults() {
        System.out.println("\n=== 📊 ELECTION RESULTS TEST ===");

        System.out.println("Generating election results report for election " + ELECTION_ID + "...");

        String electionReport = serverController.generateElectionResultsReport(ELECTION_ID);

        System.out.println("\n📊 ELECTION RESULTS:");
        System.out.println("=".repeat(50));

        if (electionReport.contains("Error") || electionReport.contains("No se encontraron")) {
            System.out.println(" " + electionReport);
        } else {
            System.out.println(" Election results generated successfully!");

            // Show preview
            String preview = electionReport.length() > 800 ?
                    electionReport.substring(0, 800) + "..." : electionReport;
            System.out.println(" Results preview:");
            System.out.println(preview);

            System.out.print("\nShow full results? (y/n): ");
            if (scanner.nextLine().trim().toLowerCase().startsWith("y")) {
                System.out.println("\n📊 FULL ELECTION RESULTS:");
                System.out.println(electionReport);
            }
        }

        // Also test election statistics
        System.out.println("\n📈 Getting election statistics...");
        String statsReport = serverController.getElectionStatistics(ELECTION_ID);
        System.out.println("📈 ELECTION STATISTICS:");
        System.out.println(statsReport);
    }

    private static void testGeographicReports() {
        System.out.println("\n===  GEOGRAPHIC REPORTS TEST ===");

        System.out.println("Select geographic level:");
        System.out.println("1. Department Report");
        System.out.println("2. Municipality Report");
        System.out.println("3. Puesto Report");
        System.out.print("Choice (1-3): ");

        int choice = getMenuChoice(1, 3);
        String reportType;
        String report;

        switch (choice) {
            case 1:
                reportType = "Department";
                System.out.print("Enter department ID (e.g., 1): ");
                int deptId = getMenuChoice(1, 999);
                report = serverController.generateDepartmentReport(deptId, ELECTION_ID);
                break;
            case 2:
                reportType = "Municipality";
                System.out.print("Enter municipality ID (e.g., 1): ");
                int muniId = getMenuChoice(1, 999);
                report = serverController.generateMunicipalityReport(muniId, ELECTION_ID);
                break;
            case 3:
                reportType = "Puesto";
                System.out.print("Enter puesto ID (e.g., 1): ");
                int puestoId = getMenuChoice(1, 999);
                report = serverController.generatePuestoReport(puestoId, ELECTION_ID);
                break;
            default:
                return;
        }

        System.out.println("\n " + reportType.toUpperCase() + " REPORT:");
        System.out.println("=".repeat(50));

        if (report.contains("Error") || report.contains("No se encontró")) {
            System.out.println(" " + report);
        } else {
            System.out.println(" Geographic report generated successfully!");

            // Show preview
            String preview = report.length() > 600 ? report.substring(0, 600) + "..." : report;
            System.out.println(" Report preview:");
            System.out.println(preview);

            System.out.print("\nShow full report? (y/n): ");
            if (scanner.nextLine().trim().toLowerCase().startsWith("y")) {
                System.out.println("\n FULL " + reportType.toUpperCase() + " REPORT:");
                System.out.println(report);
            }
        }
    }

    private static void testMesaCitizenReports() {
        System.out.println("\n=== MESA CITIZEN REPORTS TEST ===");

        System.out.print("Enter mesa ID (e.g., 1): ");
        int mesaId = getMenuChoice(1, 999999);

        System.out.println("Generating citizen reports for all citizens in mesa " + mesaId + "...");

        List<String> mesaReports = serverController.generateMesaCitizenReports(mesaId, ELECTION_ID);

        System.out.println("\nMESA CITIZEN REPORTS:");
        System.out.println("=".repeat(50));
        System.out.println("Generated " + mesaReports.size() + " citizen reports for mesa " + mesaId);

        if (!mesaReports.isEmpty()) {
            System.out.println("\n📋 Sample reports (first 3):");
            for (int i = 0; i < Math.min(3, mesaReports.size()); i++) {
                String report = mesaReports.get(i);
                System.out.println("\n Citizen " + (i + 1) + ":");
                if (report.contains("Error")) {
                    System.out.println(" " + report);
                } else {
                    String preview = report.length() > 200 ? report.substring(0, 200) + "..." : report;
                    System.out.println(" " + preview);
                }
            }

            if (mesaReports.size() > 3) {
                System.out.println("\n... and " + (mesaReports.size() - 3) + " more citizen reports");
            }

            System.out.print("\nShow all reports? (y/n): ");
            if (scanner.nextLine().trim().toLowerCase().startsWith("y")) {
                mesaReports.forEach(report -> {
                    System.out.println("\n" + "─".repeat(40));
                    System.out.println(report);
                });
            }
        } else {
            System.out.println(" No citizens found in mesa " + mesaId);
        }
    }

    private static void testExportFunctionality() {
        System.out.println("\n===  EXPORT FUNCTIONALITY TEST ===");

        System.out.println("Select export type:");
        System.out.println("1. Export Citizen Report");
        System.out.println("2. Export Election Results");
        System.out.println("3. Export Geographic Report");
        System.out.print("Choice (1-3): ");

        int choice = getMenuChoice(1, 3);
        boolean exportResult = false;
        String filename = "reports_test_" + System.currentTimeMillis();

        switch (choice) {
            case 1:
                System.out.print("Enter document ID: ");
                String documento = scanner.nextLine().trim();
                if (documento.isEmpty()) documento = "1000000001";

                String citizenPath = "server/src/main/java/Reports/data/" + filename + "_citizen.ice";
                exportResult = serverController.exportCitizenReport(documento, ELECTION_ID, citizenPath);
                System.out.println("Citizen report export: " + (exportResult ? " SUCCESS" : " FAILED"));
                if (exportResult) System.out.println(" Saved to: " + citizenPath);
                break;

            case 2:
                String electionPath = "server/src/main/java/Reports/data/" + filename + "_election.ice";
                exportResult = serverController.exportElectionResultsReport(ELECTION_ID, electionPath);
                System.out.println("Election results export: " + (exportResult ? " SUCCESS" : " FAILED"));
                if (exportResult) System.out.println(" Saved to: " + electionPath);
                break;

            case 3:
                System.out.print("Enter location ID: ");
                int locationId = getMenuChoice(1, 999);
                System.out.print("Enter location type (department/municipality/puesto): ");
                String locationType = scanner.nextLine().trim();
                if (locationType.isEmpty()) locationType = "department";

                String geoPath = "server/src/main/java/Reports/data/" + filename + "_geographic.ice";
                exportResult = serverController.exportGeographicReport(locationId, locationType, ELECTION_ID, geoPath);
                System.out.println("Geographic report export: " + (exportResult ? " SUCCESS" : " FAILED"));
                if (exportResult) System.out.println(" Saved to: " + geoPath);
                break;
        }

        System.out.println("\n Export test complete!");
    }

    private static void viewReportsStatistics() {
        System.out.println("\n=== 📈 REPORTS STATISTICS ===");

        String reportsStats = serverController.getReportsStatistics(ELECTION_ID);
        System.out.println(reportsStats);
    }

    private static void testCitizenEligibility() {
        System.out.println("\n===  CITIZEN ELIGIBILITY TEST ===");

        System.out.print("Enter document IDs to test (comma-separated): ");
        String input = scanner.nextLine().trim();

        List<String> documentos;
        if (input.isEmpty()) {
            documentos = Arrays.asList("1000000001", "1000000002", "9999999999", "invalid_doc");
            System.out.println("Using test documents: " + documentos);
        } else {
            documentos = Arrays.stream(input.split(","))
                    .map(String::trim)
                    .collect(Collectors.toList());
        }

        System.out.println("\n ELIGIBILITY RESULTS:");
        System.out.println("=".repeat(50));

        int eligible = 0;
        int notEligible = 0;

        for (String documento : documentos) {
            boolean isEligible = serverController.validateCitizenEligibility(documento);
            System.out.println(documento + ": " + (isEligible ? " ELIGIBLE" : " NOT ELIGIBLE"));

            if (isEligible) eligible++;
            else notEligible++;
        }

        System.out.println("\n📊 ELIGIBILITY SUMMARY:");
        System.out.println(" Eligible: " + eligible);
        System.out.println(" Not Eligible: " + notEligible);
        System.out.println("📈 Eligibility Rate: " + String.format("%.1f%%", (eligible * 100.0 / documentos.size())));
    }

    private static void performComprehensiveTest() {
        System.out.println("\n===  COMPREHENSIVE TEST SUITE ===");
        System.out.println("Running all tests automatically...\n");

        // Test 1: System Validation
        System.out.println(" Running system validation...");
        testSystemValidation();
        waitForUser();

        // Test 2: Sample Citizen Lookup
        System.out.println("\n Testing citizen lookup with sample data...");
        System.out.println("Testing document: 1000000001");
        String citizenReport = serverController.generateCitizenReport("1000000001", ELECTION_ID);
        System.out.println("Result: " + (citizenReport.contains("Error") ? " FAILED" : " SUCCESS"));

        // Test 3: Eligibility Check
        System.out.println("\n Testing citizen eligibility...");
        boolean eligible = serverController.validateCitizenEligibility("1000000001");
        System.out.println("Eligibility check: " + (eligible ? " ELIGIBLE" : " NOT ELIGIBLE"));

        // Test 4: Election Results
        System.out.println("\n📊 Testing election results...");
        String electionResults = serverController.generateElectionResultsReport(ELECTION_ID);
        System.out.println("Election results: " + (electionResults.contains("Error") ? " FAILED" : " SUCCESS"));

        // Test 5: Geographic Report
        System.out.println("\n️ Testing geographic report...");
        String geoReport = serverController.generateDepartmentReport(1, ELECTION_ID);
        System.out.println("Geographic report: " + (geoReport.contains("Error") ? " FAILED" : " SUCCESS"));

        // Test 6: Statistics
        System.out.println("\n Getting reports statistics...");
        String stats = serverController.getReportsStatistics(ELECTION_ID);
        System.out.println("Statistics: " + (stats.contains("Error") ? " FAILED" : " SUCCESS"));

        System.out.println("\n COMPREHENSIVE TEST COMPLETE!");
        System.out.println("Check individual test results above for detailed information.");
    }

    private static void waitForUser() {
        System.out.print("\nPress Enter to continue...");
        scanner.nextLine();
    }

    // También actualizar el método getMenuChoice para el nuevo rango
    private static int getMenuChoice(int min, int max) {
        while (true) {
            try {
                String input = scanner.nextLine().trim();
                int choice = Integer.parseInt(input);
                if (choice >= min && choice <= max) {
                    return choice;
                } else {
                    System.out.print("Please enter a number between " + min + " and " + max + ": ");
                }
            } catch (NumberFormatException e) {
                System.out.print("Please enter a valid number: ");
            }
        }
    }

    private static void cleanup() {
        System.out.println("\nCleaning up...");
        if (serverController != null) {
            serverController.shutdown();
        }
        if (scanner != null) {
            scanner.close();
        }
        System.out.println("=== Reports Manager Test Complete ===");
    }
}