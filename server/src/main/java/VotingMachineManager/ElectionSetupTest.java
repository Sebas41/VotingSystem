package VotingMachineManager;
import ConnectionDB.ConnectionDB;
import model.Vote;
import java.util.Date;
import java.util.List;
import java.util.Map;


public class ElectionSetupTest {

    public static void main(String[] args) {
        System.out.println("=== Election Setup Diagnostic & Fix ===");

        ConnectionDB db = new ConnectionDB();
        int testElectionId = 1;

        // Step 1: Check database health
        System.out.println("\n1. Database Health Check:");
        boolean healthy = db.isHealthy();
        System.out.println("   Database healthy: " + healthy);

        if (!healthy) {
            System.err.println("ERROR: Database connection failed!");
            return;
        }

        // Step 2: Check what tables exist and have data
        System.out.println("\n2. Data Availability Check:");

        // Check elections
        Map<String, Object> electionInfo = db.getElectionInfo(testElectionId);
        System.out.println("   Election " + testElectionId + " exists: " + (electionInfo != null));
        if (electionInfo != null) {
            System.out.println("   Election name: " + electionInfo.get("nombre"));
            System.out.println("   Election status: " + electionInfo.get("estado"));
        }

        // Check candidates
        List<Map<String, Object>> candidates = db.getCandidatesByElection(testElectionId);
        System.out.println("   Candidates for election " + testElectionId + ": " + candidates.size());
        for (Map<String, Object> candidate : candidates) {
            System.out.println("     - " + candidate.get("nombre") + " (" + candidate.get("partido") + ")");
        }

        // Check mesas
        List<Integer> allMesas = db.getAllMesaIds();
        System.out.println("   Total mesas in system: " + allMesas.size());

        // Check citizens
        Map<String, Object> metrics = db.getPerformanceMetrics();
        System.out.println("   Total citizens: " + metrics.get("total_citizens"));
        System.out.println("   Total puestos: " + metrics.get("total_puestos"));

        // Step 3: Setup missing data if needed
        System.out.println("\n3. Setting Up Missing Data:");

        // Create election if it doesn't exist
        if (electionInfo == null) {
            System.out.println("   Creating test election...");

            java.util.Calendar cal = java.util.Calendar.getInstance();


            cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
            cal.set(java.util.Calendar.MINUTE, 0);
            cal.set(java.util.Calendar.SECOND, 0);
            cal.set(java.util.Calendar.MILLISECOND, 0);
            Date startDate = cal.getTime();

            cal.set(java.util.Calendar.HOUR_OF_DAY, 23);
            cal.set(java.util.Calendar.MINUTE, 59);
            cal.set(java.util.Calendar.SECOND, 59);
            Date endDate = cal.getTime();


            System.out.println("    Configurando elección para HOY:");
            System.out.println("     - Inicio elección: " + startDate);
            System.out.println("     - Fin elección: " + endDate);
            System.out.println("     - Jornada será: 08:00 - 18:00 (calculado por servidor)");

            db.storeElection(testElectionId, "Elección de Prueba 2025", startDate, endDate, "ACTIVE");

            electionInfo = db.getElectionInfo(testElectionId);
            System.out.println("   Election created: " + (electionInfo != null));

            if (electionInfo != null) {
                System.out.println("    Elección creada exitosamente");
                System.out.println("     - El servidor calculará jornada: 08:00 - 18:00");
                System.out.println("     - Estado actual: ACTIVE");
            }
        }

        // Create candidates if they don't exist
        if (candidates.isEmpty()) {
            System.out.println("   Creating test candidates...");
            db.storeCandidate(1, "Juan Pérez", "Partido A", testElectionId);
            db.storeCandidate(2, "María García", "Partido B", testElectionId);
            db.storeCandidate(3, "Carlos López", "Partido C", testElectionId);

            candidates = db.getCandidatesByElection(testElectionId);
            System.out.println("   Candidates created: " + candidates.size());
        }

        // Step 4: Test mesa configuration
        System.out.println("\n4. Mesa Configuration Test:");

        if (!allMesas.isEmpty()) {
            int testMesaId = allMesas.get(0);
            System.out.println("   Testing mesa ID: " + testMesaId);

            // Get mesa configuration
            Map<String, Object> mesaConfig = db.getMesaConfiguration(testMesaId);
            System.out.println("   Mesa config exists: " + (mesaConfig != null));

            if (mesaConfig != null) {
                System.out.println("   Mesa location: " + mesaConfig.get("puesto_nombre"));
                System.out.println("   Municipality: " + mesaConfig.get("municipio_nombre"));
                System.out.println("   Department: " + mesaConfig.get("departamento_nombre"));
                System.out.println("   Citizens assigned: " + mesaConfig.get("total_ciudadanos"));
            } else {
                System.err.println("   ERROR: Mesa configuration not found!");
                System.out.println("   This suggests the vista_configuracion_mesa view is missing.");
            }

            // Get citizens for this mesa
            List<Map<String, Object>> citizens = db.getCitizensByMesa(testMesaId);
            System.out.println("   Citizens found for mesa: " + citizens.size());

            if (citizens.size() > 0) {
                Map<String, Object> firstCitizen = citizens.get(0);
                System.out.println("   Sample citizen: " + firstCitizen.get("nombre") + " " + firstCitizen.get("apellido"));
                System.out.println("   Document: " + firstCitizen.get("documento"));
            }

        } else {
            System.err.println("   ERROR: No mesas found in database!");
        }

        // Step 5: Check required database views/tables
        System.out.println("\n5. Database Schema Check:");
        try {
            // Try to query the vista_configuracion_mesa view
            if (!allMesas.isEmpty()) {
                Map<String, Object> viewTest = db.getMesaConfiguration(allMesas.get(0));
                System.out.println("   vista_configuracion_mesa view: " + (viewTest != null ? "EXISTS" : "MISSING"));
            }

            // Check departments
            List<Map<String, Object>> departments = db.getAllDepartments();
            System.out.println("   Departments table: " + (departments.size() > 0 ? "EXISTS (" + departments.size() + " records)" : "EMPTY"));

        } catch (Exception e) {
            System.err.println("   Schema check error: " + e.getMessage());
        }

        // Step 6: Final validation
        System.out.println("\n6. Final Validation:");
        boolean dataComplete = db.validateElectionDataCompleteness(testElectionId);
        System.out.println("   Election data complete: " + dataComplete);

        // Step 7: Recommendations
        System.out.println("\n7. Recommendations:");

        if (!dataComplete) {
            System.out.println("    Election data is incomplete. Issues found:");

            if (electionInfo == null) {
                System.out.println("     - Election " + testElectionId + " not found");
            }
            if (candidates.size() == 0) {
                System.out.println("     - No candidates for election " + testElectionId);
            }
            if (allMesas.size() == 0) {
                System.out.println("     - No voting tables (mesas) in system");
            }
            if (metrics.get("total_citizens").equals(0L)) {
                System.out.println("     - No citizens in system");
            }

            System.out.println("\n    To fix these issues:");
            System.out.println("     1. Run your data population scripts");
            System.out.println("     2. Ensure vista_configuracion_mesa view exists");
            System.out.println("     3. Verify all foreign key relationships");

        } else {
            System.out.println("   Election data is complete!");
            System.out.println("    Ready to run VotingMachineManager tests");
        }

        // Step 8: Create SQL script for missing view (if needed)
        if (allMesas.size() > 0) {
            Map<String, Object> mesaConfig = db.getMesaConfiguration(allMesas.get(0));
            if (mesaConfig == null) {
                System.out.println("\n8. Missing View SQL Script:");
                System.out.println("   Run this SQL to create the missing view:");
                System.out.println();
                System.out.println("   CREATE OR REPLACE VIEW vista_configuracion_mesa AS");
                System.out.println("   SELECT ");
                System.out.println("       mv.id as mesa_id,");
                System.out.println("       mv.consecutive as mesa_consecutive,");
                System.out.println("       pv.id as puesto_id,");
                System.out.println("       pv.nombre as puesto_nombre,");
                System.out.println("       pv.direccion as puesto_direccion,");
                System.out.println("       pv.consecutive as puesto_consecutive,");
                System.out.println("       m.id as municipio_id,");
                System.out.println("       m.nombre as municipio_nombre,");
                System.out.println("       d.id as departamento_id,");
                System.out.println("       d.nombre as departamento_nombre,");
                System.out.println("       COUNT(c.id) as total_ciudadanos");
                System.out.println("   FROM mesa_votacion mv");
                System.out.println("   JOIN puesto_votacion pv ON mv.puesto_id = pv.id");
                System.out.println("   JOIN municipio m ON pv.municipio_id = m.id");
                System.out.println("   JOIN departamento d ON m.departamento_id = d.id");
                System.out.println("   LEFT JOIN ciudadano c ON c.mesa_id = mv.id");
                System.out.println("   GROUP BY mv.id, mv.consecutive, pv.id, pv.nombre, pv.direccion, pv.consecutive,");
                System.out.println("            m.id, m.nombre, d.id, d.nombre;");
            }
        }

        System.out.println("\n=== Election Setup Diagnostic Complete ===");

        // Cleanup
        ConnectionDB.shutdown();
    }
}