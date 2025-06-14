package org.votaciones;

import ReportsSystem.ReportsServicePrx;
import com.zeroc.Ice.*;
import com.zeroc.Ice.Exception;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Cliente de prueba para el ProxyCache de Reports
 * Simula consultas de ciudadanos para probar el cache
 */
public class ProxyCacheTestClient {

    private static final Logger logger = LoggerFactory.getLogger(ProxyCacheTestClient.class);

    public static void main(String[] args) {
        List<String> params = new ArrayList<>();

        try (Communicator communicator = Util.initialize(args, params)) {

            System.out.println("🧪 ========== CLIENTE DE PRUEBA PROXY CACHE ==========");
            System.out.println();

            // Conectar al ProxyCache (no al servidor principal)
            ObjectPrx base = communicator.stringToProxy("ReportsCacheManager:tcp -h localhost -p 9003");
            ReportsServicePrx proxyCache = ReportsServicePrx.checkedCast(base);

            if (proxyCache == null) {
                System.err.println("❌ Error: No se pudo conectar al ProxyCache en puerto 9003");
                System.err.println("💡 Asegúrate de que el ProxyCache esté ejecutándose");
                System.exit(1);
            }

            System.out.println("✅ Conectado al ProxyCache en puerto 9003");
            System.out.println();

            // Menú interactivo
            Scanner scanner = new Scanner(System.in);
            boolean running = true;

            while (running) {
                mostrarMenu();
                String opcion = scanner.nextLine().trim();

                switch (opcion) {
                    case "1":
                        testCitizenReports(proxyCache, scanner);
                        break;
                    case "2":
                        testSearchCitizens(proxyCache, scanner);
                        break;
                    case "3":
                        testElectionReports(proxyCache, scanner);
                        break;
                    case "4":
                        testGeographicReports(proxyCache, scanner);
                        break;
                    case "5":
                        testAvailableElections(proxyCache);
                        break;
                    case "6":
                        testCachePerformance(proxyCache);
                        break;
                    case "7":
                        testValidateCitizen(proxyCache, scanner);
                        break;
                    case "8":
                        testMesaCitizenReports(proxyCache, scanner);
                        break;
                    case "9":
                        testPreloadReports(proxyCache, scanner);
                        break;
                    case "0":
                        running = false;
                        break;
                    default:
                        System.out.println("❌ Opción inválida");
                }
                System.out.println();
            }

            System.out.println("👋 Cerrando cliente de prueba...");

        } catch (LocalException e) {
            System.err.println("❌ Error de Ice: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("❌ Error general: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void mostrarMenu() {
        System.out.println("📋 ========== MENÚ DE PRUEBAS ==========");
        System.out.println("1. 👤 Consultar reporte de ciudadano");
        System.out.println("2. 🔍 Buscar ciudadanos por nombre");
        System.out.println("3. 🗳️  Consultar reporte de elección");
        System.out.println("4. 🌍 Consultar reporte geográfico");
        System.out.println("5. 📊 Consultar elecciones disponibles");
        System.out.println("6. ⚡ Probar rendimiento del cache");
        System.out.println("7. ✅ Validar elegibilidad de ciudadano");
        System.out.println("8. 🏢 Consultar ciudadanos de mesa");
        System.out.println("9. 📥 Precargar reportes");
        System.out.println("0. 🚪 Salir");
        System.out.println("==========================================");
        System.out.print("Selecciona una opción: ");
    }

    private static void testCitizenReports(ReportsServicePrx proxyCache, Scanner scanner) {
        try {
            System.out.print("📝 Ingresa el documento del ciudadano (ej: 12345678): ");
            String documento = scanner.nextLine().trim();

            System.out.print("📝 Ingresa el ID de la elección (ej: 1): ");
            int electionId = Integer.parseInt(scanner.nextLine().trim());

            System.out.println("\n⏳ Primera consulta (sin cache)...");
            long startTime = System.currentTimeMillis();

            String result = proxyCache.getCitizenReports(documento, electionId);

            long endTime = System.currentTimeMillis();

            System.out.println("✅ Reporte obtenido en " + (endTime - startTime) + " ms");
            System.out.println("📄 Resultado: " + result.substring(0, Math.min(result.length(), 100)) + "...");

            // Segunda consulta para probar cache
            System.out.println("\n🔄 Segunda consulta (debería usar cache)...");
            startTime = System.currentTimeMillis();

            result = proxyCache.getCitizenReports(documento, electionId);

            endTime = System.currentTimeMillis();
            System.out.println("⚡ Segunda consulta: " + (endTime - startTime) + " ms (desde cache)");

            // Tercera consulta inmediata
            System.out.println("\n🔄 Tercera consulta inmediata...");
            startTime = System.currentTimeMillis();

            result = proxyCache.getCitizenReports(documento, electionId);

            endTime = System.currentTimeMillis();
            System.out.println("⚡ Tercera consulta: " + (endTime - startTime) + " ms (debería ser <5ms)");

        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
        }
    }

    private static void testSearchCitizens(ReportsServicePrx proxyCache, Scanner scanner) {
        try {
            System.out.print("📝 Ingresa el nombre (ej: Juan): ");
            String nombre = scanner.nextLine().trim();

            System.out.print("📝 Ingresa el apellido (ej: Pérez): ");
            String apellido = scanner.nextLine().trim();

            System.out.print("📝 Ingresa el ID de la elección (ej: 1): ");
            int electionId = Integer.parseInt(scanner.nextLine().trim());

            System.out.println("\n⏳ Buscando ciudadanos...");
            long startTime = System.currentTimeMillis();

            String[] results = proxyCache.searchCitizenReports(nombre, apellido, electionId, 10);

            long endTime = System.currentTimeMillis();

            System.out.println("✅ Búsqueda completada en " + (endTime - startTime) + " ms");
            System.out.println("📊 Resultados encontrados: " + results.length);

            for (int i = 0; i < Math.min(results.length, 3); i++) {
                System.out.println("   " + (i+1) + ". " + results[i].substring(0, Math.min(results[i].length(), 80)) + "...");
            }

            // Segunda búsqueda para cache
            System.out.println("\n🔄 Repitiendo búsqueda (debería usar cache)...");
            startTime = System.currentTimeMillis();

            results = proxyCache.searchCitizenReports(nombre, apellido, electionId, 10);

            endTime = System.currentTimeMillis();
            System.out.println("⚡ Segunda búsqueda: " + (endTime - startTime) + " ms (desde cache)");

        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
        }
    }

    private static void testElectionReports(ReportsServicePrx proxyCache, Scanner scanner) {
        try {
            System.out.print("📝 Ingresa el ID de la elección (ej: 1): ");
            int electionId = Integer.parseInt(scanner.nextLine().trim());

            System.out.println("\n⏳ Consultando reporte de elección...");
            long startTime = System.currentTimeMillis();

            String result = proxyCache.getElectionReports(electionId);

            long endTime = System.currentTimeMillis();

            System.out.println("✅ Reporte obtenido en " + (endTime - startTime) + " ms");
            System.out.println("📄 Resultado: " + result.substring(0, Math.min(result.length(), 100)) + "...");

            // Segunda consulta para cache
            System.out.println("\n🔄 Segunda consulta...");
            startTime = System.currentTimeMillis();

            result = proxyCache.getElectionReports(electionId);

            endTime = System.currentTimeMillis();
            System.out.println("⚡ Segunda consulta: " + (endTime - startTime) + " ms");

        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
        }
    }

    private static void testGeographicReports(ReportsServicePrx proxyCache, Scanner scanner) {
        try {
            System.out.print("📝 Ingresa el ID de la ubicación (ej: 1): ");
            int locationId = Integer.parseInt(scanner.nextLine().trim());

            System.out.print("📝 Ingresa el tipo (department/municipality/puesto): ");
            String locationType = scanner.nextLine().trim();

            System.out.print("📝 Ingresa el ID de la elección (ej: 1): ");
            int electionId = Integer.parseInt(scanner.nextLine().trim());

            System.out.println("\n⏳ Consultando reporte geográfico...");
            long startTime = System.currentTimeMillis();

            String result = proxyCache.getGeographicReports(locationId, locationType, electionId);

            long endTime = System.currentTimeMillis();

            System.out.println("✅ Reporte obtenido en " + (endTime - startTime) + " ms");
            System.out.println("📄 Resultado: " + result.substring(0, Math.min(result.length(), 100)) + "...");

        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
        }
    }

    private static void testAvailableElections(ReportsServicePrx proxyCache) {
        try {
            System.out.println("⏳ Consultando elecciones disponibles...");
            long startTime = System.currentTimeMillis();

            String[] elections = proxyCache.getAvailableElections();

            long endTime = System.currentTimeMillis();

            System.out.println("✅ Consulta completada en " + (endTime - startTime) + " ms");
            System.out.println("📊 Elecciones disponibles: " + elections.length);

            for (int i = 0; i < Math.min(elections.length, 5); i++) {
                System.out.println("   " + (i+1) + ". " + elections[i]);
            }

            // Segunda consulta para cache
            System.out.println("\n🔄 Segunda consulta...");
            startTime = System.currentTimeMillis();

            elections = proxyCache.getAvailableElections();

            endTime = System.currentTimeMillis();
            System.out.println("⚡ Segunda consulta: " + (endTime - startTime) + " ms");

        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
        }
    }

    private static void testValidateCitizen(ReportsServicePrx proxyCache, Scanner scanner) {
        try {
            System.out.print("📝 Ingresa el documento del ciudadano (ej: 12345678): ");
            String documento = scanner.nextLine().trim();

            System.out.println("⏳ Validando elegibilidad...");
            long startTime = System.currentTimeMillis();

            boolean isEligible = proxyCache.validateCitizenEligibility(documento);

            long endTime = System.currentTimeMillis();

            System.out.println("✅ Validación completada en " + (endTime - startTime) + " ms");
            System.out.println("📊 Resultado: " + (isEligible ? "✅ ELEGIBLE" : "❌ NO ELEGIBLE"));

            // Nota: Este método NO usa cache por seguridad
            System.out.println("ℹ️  Nota: validateCitizenEligibility NO usa cache por seguridad");

        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
        }
    }

    private static void testMesaCitizenReports(ReportsServicePrx proxyCache, Scanner scanner) {
        try {
            System.out.print("📝 Ingresa el ID de la mesa (ej: 1): ");
            int mesaId = Integer.parseInt(scanner.nextLine().trim());

            System.out.print("📝 Ingresa el ID de la elección (ej: 1): ");
            int electionId = Integer.parseInt(scanner.nextLine().trim());

            System.out.println("\n⏳ Consultando ciudadanos de la mesa...");
            long startTime = System.currentTimeMillis();

            String[] results = proxyCache.getMesaCitizenReports(mesaId, electionId);

            long endTime = System.currentTimeMillis();

            System.out.println("✅ Consulta completada en " + (endTime - startTime) + " ms");
            System.out.println("📊 Ciudadanos en la mesa: " + results.length);

            for (int i = 0; i < Math.min(results.length, 3); i++) {
                System.out.println("   " + (i+1) + ". " + results[i].substring(0, Math.min(results[i].length(), 80)) + "...");
            }

        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
        }
    }

    private static void testPreloadReports(ReportsServicePrx proxyCache, Scanner scanner) {
        try {
            System.out.print("📝 Ingresa el ID de la elección para precargar (ej: 1): ");
            int electionId = Integer.parseInt(scanner.nextLine().trim());

            System.out.println("⏳ Precargando reportes...");
            long startTime = System.currentTimeMillis();

            proxyCache.preloadReports(electionId);

            long endTime = System.currentTimeMillis();

            System.out.println("✅ Precarga completada en " + (endTime - startTime) + " ms");
            System.out.println("💾 Los reportes principales ahora están en cache");

        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
        }
    }

    private static void testCachePerformance(ReportsServicePrx proxyCache) {
        try {
            System.out.println("⚡ ========== PRUEBA DE RENDIMIENTO ==========");

            // Datos de prueba
            String[] documentos = {"12345678", "87654321", "11111111", "22222222", "33333333"};
            int electionId = 1;

            System.out.println("🚀 Realizando 5 consultas iniciales (sin cache)...");

            // Primera ronda - sin cache
            long totalTime1 = 0;
            for (String doc : documentos) {
                long start = System.currentTimeMillis();
                try {
                    proxyCache.getCitizenReports(doc, electionId);
                    long time = System.currentTimeMillis() - start;
                    totalTime1 += time;
                    System.out.println("   Doc " + doc + ": " + time + " ms");
                } catch (Exception e) {
                    System.out.println("   Doc " + doc + ": ERROR - " + e.getMessage());
                }
            }

            System.out.println("📊 Tiempo total (sin cache): " + totalTime1 + " ms");
            System.out.println("📊 Promedio (sin cache): " + (totalTime1/5) + " ms");

            System.out.println("\n🔄 Realizando las mismas 5 consultas (con cache)...");

            // Segunda ronda - con cache
            long totalTime2 = 0;
            for (String doc : documentos) {
                long start = System.currentTimeMillis();
                try {
                    proxyCache.getCitizenReports(doc, electionId);
                    long time = System.currentTimeMillis() - start;
                    totalTime2 += time;
                    System.out.println("   Doc " + doc + ": " + time + " ms");
                } catch (Exception e) {
                    System.out.println("   Doc " + doc + ": ERROR - " + e.getMessage());
                }
            }

            System.out.println("📊 Tiempo total (con cache): " + totalTime2 + " ms");
            System.out.println("📊 Promedio (con cache): " + (totalTime2/5) + " ms");

            if (totalTime1 > 0) {
                long improvement = totalTime1 - totalTime2;
                double percentage = (improvement * 100.0) / totalTime1;
                System.out.println("🎯 Mejora del cache: " + improvement + " ms (" + String.format("%.1f", percentage) + "%)");
            }

            // Prueba de consultas rápidas consecutivas
            System.out.println("\n🏃 Prueba de 10 consultas consecutivas del mismo documento...");
            String testDoc = "12345678";
            long rapidStart = System.currentTimeMillis();

            for (int i = 0; i < 10; i++) {
                try {
                    proxyCache.getCitizenReports(testDoc, electionId);
                } catch (Exception e) {
                    System.out.println("Error en consulta " + (i+1));
                }
            }

            long rapidEnd = System.currentTimeMillis();
            long rapidTotal = rapidEnd - rapidStart;
            System.out.println("🚀 10 consultas consecutivas: " + rapidTotal + " ms");
            System.out.println("⚡ Promedio por consulta: " + (rapidTotal/10) + " ms");

        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
        }
    }
}