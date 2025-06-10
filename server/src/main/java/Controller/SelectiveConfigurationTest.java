package Controller;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Interactive test class for selective voting machine configuration
 * Allows operators to select specific departments, municipalities, puestos, or mesas for testing
 */
public class SelectiveConfigurationTest {

    private static ServerControllerImpl serverController;
    private static Scanner scanner;
    private static final int ELECTION_ID = 1; // Test election ID

    public static void main(String[] args) {
        System.out.println("=== Selective Voting Machine Configuration Test ===");

        // Initialize components
        serverController = new ServerControllerImpl();
        scanner = new Scanner(System.in);

        try {
            // Main interactive loop
            boolean running = true;
            while (running) {
                displayMainMenu();
                int choice = getMenuChoice(1, 9);

                switch (choice) {
                    case 1:
                        testDepartmentSelection();
                        break;
                    case 2:
                        testMunicipalitySelection();
                        break;
                    case 3:
                        testPuestoSelection();
                        break;
                    case 4:
                        testSpecificMesaSelection();
                        break;
                    case 5:
                        testDepartmentSample();
                        break;
                    case 6:
                        viewTestHistory();
                        break;
                    case 7:
                        viewSystemStatus();
                        break;
                    case 8:
                        previewMesaDetails();
                        break;
                    case 9:
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
        System.out.println("\n" + "=".repeat(60));
        System.out.println("SELECTIVE VOTING MACHINE CONFIGURATION MENU");
        System.out.println("=".repeat(60));
        System.out.println("1. Configure by Department");
        System.out.println("2. Configure by Municipality");
        System.out.println("3. Configure by Puesto de Votación");
        System.out.println("4. Configure Specific Mesas");
        System.out.println("5. Generate Department Sample Test");
        System.out.println("6. View Test History");
        System.out.println("7. View System Status");
        System.out.println("8. Preview Mesa Details");
        System.out.println("9. Exit");
        System.out.println("=".repeat(60));
        System.out.print("Select option (1-9): ");
    }

    private static void testDepartmentSelection() {
        System.out.println("\n=== DEPARTMENT SELECTION ===");

        // Get available departments
        List<Map<String, Object>> departments = serverController.getAvailableDepartments();
        if (departments.isEmpty()) {
            System.out.println("No departments found in system.");
            return;
        }

        // Display departments
        System.out.println("Available Departments:");
        for (int i = 0; i < departments.size(); i++) {
            Map<String, Object> dept = departments.get(i);
            System.out.printf("%d. %s (ID: %d)%n",
                    i + 1, dept.get("nombre"), dept.get("id"));
        }

        // Get user selection
        System.out.print("Select department (1-" + departments.size() + "): ");
        int choice = getMenuChoice(1, departments.size());
        Map<String, Object> selectedDept = departments.get(choice - 1);

        int departmentId = (Integer) selectedDept.get("id");
        String departmentName = (String) selectedDept.get("nombre");

        // Ask for test name
        System.out.print("Enter test name for this department configuration: ");
        String testName = scanner.nextLine().trim();
        if (testName.isEmpty()) {
            testName = "DepartmentTest_" + departmentName + "_" + System.currentTimeMillis();
        }

        System.out.println("Generating configurations for department: " + departmentName);
        serverController.generateDepartmentConfigurations(departmentId, ELECTION_ID);

        System.out.println("Configuration generation started. Check status with option 7.");
    }

    private static void testMunicipalitySelection() {
        System.out.println("\n=== MUNICIPALITY SELECTION ===");

        // First select department
        List<Map<String, Object>> departments = serverController.getAvailableDepartments();
        if (departments.isEmpty()) {
            System.out.println("No departments found in system.");
            return;
        }

        System.out.println("First, select a department:");
        for (int i = 0; i < Math.min(10, departments.size()); i++) {
            Map<String, Object> dept = departments.get(i);
            System.out.printf("%d. %s%n", i + 1, dept.get("nombre"));
        }

        System.out.print("Select department (1-" + Math.min(10, departments.size()) + "): ");
        int deptChoice = getMenuChoice(1, Math.min(10, departments.size()));
        int departmentId = (Integer) departments.get(deptChoice - 1).get("id");

        // Get municipalities in selected department
        List<Map<String, Object>> municipalities = serverController.getMunicipalitiesByDepartment(departmentId);
        if (municipalities.isEmpty()) {
            System.out.println("No municipalities found in selected department.");
            return;
        }

        System.out.println("\nMunicipalities in " + departments.get(deptChoice - 1).get("nombre") + ":");
        for (int i = 0; i < Math.min(10, municipalities.size()); i++) {
            Map<String, Object> muni = municipalities.get(i);
            System.out.printf("%d. %s%n", i + 1, muni.get("nombre"));
        }

        System.out.print("Select municipality (1-" + Math.min(10, municipalities.size()) + "): ");
        int muniChoice = getMenuChoice(1, Math.min(10, municipalities.size()));
        Map<String, Object> selectedMuni = municipalities.get(muniChoice - 1);

        int municipalityId = (Integer) selectedMuni.get("id");
        String municipalityName = (String) selectedMuni.get("nombre");

        // Get puestos in municipality and generate configs for all
        List<Map<String, Object>> puestos = serverController.getPuestosByMunicipality(municipalityId);

        if (puestos.isEmpty()) {
            System.out.println("No puestos found in selected municipality.");
            return;
        }

        System.out.println("Found " + puestos.size() + " puestos in " + municipalityName);

        // Generate configurations for each puesto in the municipality
        String testName = "MunicipalityTest_" + municipalityName + "_" + System.currentTimeMillis();

        for (Map<String, Object> puesto : puestos) {
            int puestoId = (Integer) puesto.get("id");
            String puestoTestName = testName + "_Puesto_" + puestoId;
            serverController.generatePuestoConfiguration(puestoId, ELECTION_ID, puestoTestName);
        }

        System.out.println("Configuration generation started for all puestos in " + municipalityName);
    }

    private static void testPuestoSelection() {
        System.out.println("\n=== PUESTO DE VOTACIÓN SELECTION ===");

        // Navigate: Department -> Municipality -> Puesto
        List<Map<String, Object>> departments = serverController.getAvailableDepartments();

        System.out.println("Select Department:");
        for (int i = 0; i < Math.min(5, departments.size()); i++) {
            Map<String, Object> dept = departments.get(i);
            System.out.printf("%d. %s%n", i + 1, dept.get("nombre"));
        }

        System.out.print("Department choice (1-" + Math.min(5, departments.size()) + "): ");
        int deptChoice = getMenuChoice(1, Math.min(5, departments.size()));
        int departmentId = (Integer) departments.get(deptChoice - 1).get("id");

        List<Map<String, Object>> municipalities = serverController.getMunicipalitiesByDepartment(departmentId);

        System.out.println("\nSelect Municipality:");
        for (int i = 0; i < Math.min(5, municipalities.size()); i++) {
            Map<String, Object> muni = municipalities.get(i);
            System.out.printf("%d. %s%n", i + 1, muni.get("nombre"));
        }

        System.out.print("Municipality choice (1-" + Math.min(5, municipalities.size()) + "): ");
        int muniChoice = getMenuChoice(1, Math.min(5, municipalities.size()));
        int municipalityId = (Integer) municipalities.get(muniChoice - 1).get("id");

        List<Map<String, Object>> puestos = serverController.getPuestosByMunicipality(municipalityId);

        if (puestos.isEmpty()) {
            System.out.println("No puestos found in selected municipality.");
            return;
        }

        System.out.println("\nSelect Puesto de Votación:");
        for (int i = 0; i < Math.min(10, puestos.size()); i++) {
            Map<String, Object> puesto = puestos.get(i);
            System.out.printf("%d. %s - %s%n",
                    i + 1, puesto.get("nombre"), puesto.get("direccion"));
        }

        System.out.print("Puesto choice (1-" + Math.min(10, puestos.size()) + "): ");
        int puestoChoice = getMenuChoice(1, Math.min(10, puestos.size()));
        Map<String, Object> selectedPuesto = puestos.get(puestoChoice - 1);

        int puestoId = (Integer) selectedPuesto.get("id");
        String puestoName = (String) selectedPuesto.get("nombre");

        // Show mesas in this puesto
        List<Map<String, Object>> mesas = serverController.getMesasByPuesto(puestoId);
        System.out.println("\nFound " + mesas.size() + " mesas in " + puestoName);

        // Generate configuration for this puesto
        String testName = "PuestoTest_" + puestoName.replaceAll("[^a-zA-Z0-9]", "_");
        serverController.generatePuestoConfiguration(puestoId, ELECTION_ID, testName);

        System.out.println("Configuration generation started for puesto: " + puestoName);
    }

    private static void testSpecificMesaSelection() {
        System.out.println("\n=== SPECIFIC MESA SELECTION ===");

        System.out.print("Enter mesa IDs (comma-separated, e.g., 1,2,3,10,25): ");
        String mesaIdsInput = scanner.nextLine().trim();

        try {
            List<Integer> mesaIds = Arrays.stream(mesaIdsInput.split(","))
                    .map(String::trim)
                    .map(Integer::parseInt)
                    .collect(Collectors.toList());

            if (mesaIds.isEmpty()) {
                System.out.println("No valid mesa IDs provided.");
                return;
            }

            // Preview selected mesas
            System.out.println("\nSelected Mesas Preview:");
            for (Integer mesaId : mesaIds.subList(0, Math.min(3, mesaIds.size()))) {
                String preview = serverController.previewMesaConfiguration(mesaId);
                System.out.println(preview);
                System.out.println("-".repeat(40));
            }

            if (mesaIds.size() > 3) {
                System.out.println("... and " + (mesaIds.size() - 3) + " more mesas");
            }

            System.out.print("Enter test name for this configuration: ");
            String testName = scanner.nextLine().trim();
            if (testName.isEmpty()) {
                testName = "SpecificMesasTest_" + System.currentTimeMillis();
            }

            serverController.generateSelectiveMesaConfigurations(mesaIds, ELECTION_ID, testName);
            System.out.println("Configuration generation started for " + mesaIds.size() + " mesas");

        } catch (NumberFormatException e) {
            System.out.println("Invalid mesa ID format. Please use comma-separated numbers.");
        }
    }

    private static void testDepartmentSample() {
        System.out.println("\n=== DEPARTMENT SAMPLE TEST ===");

        List<Map<String, Object>> departments = serverController.getAvailableDepartments();

        System.out.println("Select Department for Sample Test:");
        for (int i = 0; i < Math.min(5, departments.size()); i++) {
            Map<String, Object> dept = departments.get(i);
            System.out.printf("%d. %s%n", i + 1, dept.get("nombre"));
        }

        System.out.print("Department choice (1-" + Math.min(5, departments.size()) + "): ");
        int deptChoice = getMenuChoice(1, Math.min(5, departments.size()));

        int departmentId = (Integer) departments.get(deptChoice - 1).get("id");
        String departmentName = (String) departments.get(deptChoice - 1).get("nombre");

        System.out.print("Enter sample size (number of mesas to test, e.g., 10): ");
        int sampleSize = getMenuChoice(1, 1000); // Max 1000 for safety

        String testName = "SampleTest_" + departmentName + "_" + sampleSize + "mesas";

        serverController.generateDepartmentSampleConfiguration(departmentId, sampleSize, ELECTION_ID, testName);
        System.out.println("Sample configuration generation started for " + sampleSize + " mesas in " + departmentName);
    }

    private static void viewTestHistory() {
        System.out.println("\n=== TEST HISTORY ===");

        List<String> testHistory = serverController.getSelectiveTestHistory();

        if (testHistory.isEmpty()) {
            System.out.println("No previous selective tests found.");
            return;
        }

        System.out.println("Previous Selective Tests:");
        for (int i = 0; i < testHistory.size(); i++) {
            System.out.printf("%d. %s%n", i + 1, testHistory.get(i));
        }

        if (!testHistory.isEmpty()) {
            System.out.print("Select test to view summary (1-" + testHistory.size() + "), or 0 to return: ");
            int choice = getMenuChoice(0, testHistory.size());

            if (choice > 0) {
                String testName = testHistory.get(choice - 1);
                String summary = serverController.getSelectiveTestSummary(testName);
                System.out.println("\n=== Test Summary ===");
                System.out.println(summary);
            }
        }
    }

    private static void viewSystemStatus() {
        System.out.println("\n=== SYSTEM STATUS ===");

        System.out.println("Configuration Status:");
        System.out.println(serverController.getConfigurationStatus());

        System.out.println("\nSystem Statistics:");
        System.out.println(serverController.getConfigurationStatistics(ELECTION_ID));

        System.out.println("Configurations Available:");
        boolean hasConfigs = serverController.hasVotingMachineConfigurations(ELECTION_ID);
        System.out.println("Election " + ELECTION_ID + " configurations exist: " + hasConfigs);
    }

    private static void previewMesaDetails() {
        System.out.println("\n=== MESA PREVIEW ===");

        System.out.print("Enter mesa ID to preview (e.g., 1): ");
        int mesaId = getMenuChoice(1, 999999);

        String preview = serverController.previewMesaConfiguration(mesaId);
        System.out.println("\n" + preview);
    }

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
        System.out.println("=== Selective Configuration Test Complete ===");
    }
}