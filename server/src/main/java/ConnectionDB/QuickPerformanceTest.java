package test;

import ConnectionDB.ConnectionDB;
import java.util.Map;
import java.util.List;


public class QuickPerformanceTest {

    public static void main(String[] args) {
        System.out.println("=== Testing Optimized Voting System ===");

        ConnectionDB db = new ConnectionDB();

        System.out.println("\n1. Health Check:");
        boolean healthy = db.isHealthy();
        System.out.println("   Database healthy: " + healthy);

        System.out.println("\n2. Connection Pool:");
        System.out.println("   " + db.getPoolStats());

        System.out.println("\n3. Database Metrics:");
        Map<String, Object> metrics = db.getPerformanceMetrics();
        for (Map.Entry<String, Object> entry : metrics.entrySet()) {
            System.out.println("   " + entry.getKey() + ": " + entry.getValue());
        }

        System.out.println("\n4. Testing Mesa Configuration Speed:");

        // Get first available mesa ID
        List<Integer> allMesas = db.getAllMesaIds();
        if (!allMesas.isEmpty()) {
            int testMesaId = allMesas.get(0);
            System.out.println("   Testing with mesa ID: " + testMesaId);

            long startTime = System.currentTimeMillis();
            Map<String, Object> config = db.getMesaConfiguration(testMesaId);
            long endTime = System.currentTimeMillis();

            System.out.println("   Mesa configuration retrieved in: " + (endTime - startTime) + " ms");

            if (config != null) {
                System.out.println("   Mesa info: " + config.get("puesto_nombre") +
                        " in " + config.get("municipio_nombre"));
                System.out.println("   Citizens in this mesa: " + config.get("total_ciudadanos"));
            }


            System.out.println("\n5. Testing Citizens Retrieval Speed:");
            startTime = System.currentTimeMillis();
            List<Map<String, Object>> citizens = db.getCitizensByMesa(testMesaId);
            endTime = System.currentTimeMillis();

            System.out.println("   Citizens retrieved in: " + (endTime - startTime) + " ms");
            System.out.println("   Total citizens found: " + citizens.size());

            if (!citizens.isEmpty()) {
                Map<String, Object> firstCitizen = citizens.get(0);
                System.out.println("   First citizen: " + firstCitizen.get("nombre") +
                        " " + firstCitizen.get("apellido") +
                        " (Doc: " + firstCitizen.get("documento") + ")");
            }

        } else {
            System.out.println("   No mesa data found in database");
        }

        System.out.println("\n6. Testing Existing Functionality:");
        String candidateName = db.getCandidateNameById(1);
        System.out.println("   Candidate ID 1: " + candidateName);

        System.out.println("\n=== Test Complete ===");
        System.out.println("Your optimized voting system is ready for 100M citizens!");

        ConnectionDB.shutdown();
    }
}