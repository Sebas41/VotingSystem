package org.votaciones;

import ReportsSystem.ReportsServicePrx;
import com.zeroc.Ice.*;
import com.zeroc.Ice.Exception;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Scanner;

/**
 * Cliente de prueba completo para ProxyCache de Reports
 * Prueba TODAS las funcionalidades del sistema de cache
 */
public class ProxyCacheTestClient {

    private static final Logger logger = LoggerFactory.getLogger(ProxyCacheTestClient.class);

    public static void main(String[] args) {
        try (Communicator communicator = Util.initialize(args)) {

            System.out.println("🚀 ========== CLIENTE DE PRUEBA PROXY CACHE REPORTS ==========");
            System.out.println("🔌 Conectando al proxy cache...");

            // Conectar al proxy cache (no directamente al servidor)
            ObjectPrx base = communicator.stringToProxy("ProxyCacheReports:default -h localhost -p 9999");
            ReportsServicePrx proxyCache = ReportsServicePrx.checkedCast(base);

            if (proxyCache == null) {
                System.err.println("❌ Error: No se pudo conectar al proxy cache");
                return;
            }

            System.out.println("✅ Conectado al proxy cache exitosamente");
            System.out.println("📊 Todas las consultas pasarán por el cache local");
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
                    case "10":
                        testCacheStats(proxyCache);
                        break;
                    case "11":
                        testAuxiliaryMethods(proxyCache, scanner);
                        break;
                    case "12":
                        testHotspotGenerator(proxyCache, scanner);
                        break;
                    case "0":
                        running = false;
                        break;
                    default:
                        System.out.println("❌ Opción inválida");
                }

                if (running) {
                    System.out.println("\nPresiona Enter para continuar...");
                    scanner.nextLine();
                }
            }

            System.out.println("👋 Cliente finalizado");

        } catch (LocalException e) {
            System.err.println("❌ Error de Ice: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("❌ Error general: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // También agregar la opción al menú principal:
    private static void mostrarMenu() {
        System.out.println("\n📋 ========== MENÚ DE PRUEBAS ==========");
        System.out.println("1. 👤 Consultar reporte de ciudadano");
        System.out.println("2. 🔍 Buscar ciudadanos por nombre");
        System.out.println("3. 🗳️  Consultar reporte de elección");
        System.out.println("4. 🌍 Consultar reporte geográfico");
        System.out.println("5. 📊 Consultar elecciones disponibles");
        System.out.println("6. ⚡ Probar rendimiento del cache");
        System.out.println("7. ✅ Validar elegibilidad de ciudadano");
        System.out.println("8. 🏢 Consultar ciudadanos de mesa");
        System.out.println("9. 📥 Precargar reportes (con opciones geográficas)");
        System.out.println("10. 📊 Ver estadísticas del cache");
        System.out.println("11. 🔧 Probar métodos auxiliares geográficos");
        System.out.println("12. 🔥 Generar hotspot (probar cache inteligente)"); // ⭐ NUEVA OPCIÓN
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

            System.out.println("⏳ Consultando reporte del ciudadano...");
            long startTime = System.currentTimeMillis();

            String result = proxyCache.getCitizenReports(documento, electionId);

            long endTime = System.currentTimeMillis();

            System.out.println("✅ Consulta completada en " + (endTime - startTime) + " ms");
            System.out.println("📄 Resultado:");
            System.out.println(result);

        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
        }
    }

    private static void testSearchCitizens(ReportsServicePrx proxyCache, Scanner scanner) {
        try {
            System.out.print("📝 Ingresa el nombre (o vacío para omitir): ");
            String nombre = scanner.nextLine().trim();

            System.out.print("📝 Ingresa el apellido (o vacío para omitir): ");
            String apellido = scanner.nextLine().trim();

            System.out.print("📝 Ingresa el ID de la elección (ej: 1): ");
            int electionId = Integer.parseInt(scanner.nextLine().trim());

            System.out.print("📝 Ingresa el límite de resultados (ej: 10): ");
            int limit = Integer.parseInt(scanner.nextLine().trim());

            System.out.println("🔍 Buscando ciudadanos...");
            long startTime = System.currentTimeMillis();

            String[] results = proxyCache.searchCitizenReports(nombre, apellido, electionId, limit);

            long endTime = System.currentTimeMillis();

            System.out.println("✅ Búsqueda completada en " + (endTime - startTime) + " ms");
            System.out.println("📊 Encontrados " + results.length + " resultados:");

            for (int i = 0; i < Math.min(results.length, 5); i++) {
                System.out.println("   " + (i + 1) + ". " + results[i]);
            }

            if (results.length > 5) {
                System.out.println("   ... y " + (results.length - 5) + " más");
            }

        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
        }
    }

    private static void testElectionReports(ReportsServicePrx proxyCache, Scanner scanner) {
        try {
            System.out.print("📝 Ingresa el ID de la elección (ej: 1): ");
            int electionId = Integer.parseInt(scanner.nextLine().trim());

            System.out.println("🗳️ Consultando reporte de elección...");
            long startTime = System.currentTimeMillis();

            String result = proxyCache.getElectionReports(electionId);

            long endTime = System.currentTimeMillis();

            System.out.println("✅ Consulta completada en " + (endTime - startTime) + " ms");
            System.out.println("📄 Resultado:");
            System.out.println(result);

        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
        }
    }

    private static void testGeographicReports(ReportsServicePrx proxyCache, Scanner scanner) {
        try {
            System.out.println("🌍 Tipos de ubicación disponibles:");
            System.out.println("   - department (departamento)");
            System.out.println("   - municipality (municipio)");
            System.out.println("   - puesto (puesto de votación)");

            System.out.print("📝 Ingresa el tipo de ubicación: ");
            String locationType = scanner.nextLine().trim();

            System.out.print("📝 Ingresa el ID de la ubicación (ej: 1): ");
            int locationId = Integer.parseInt(scanner.nextLine().trim());

            System.out.print("📝 Ingresa el ID de la elección (ej: 1): ");
            int electionId = Integer.parseInt(scanner.nextLine().trim());

            System.out.println("🌍 Consultando reporte geográfico...");
            long startTime = System.currentTimeMillis();

            String result = proxyCache.getGeographicReports(locationId, locationType, electionId);

            long endTime = System.currentTimeMillis();

            System.out.println("✅ Consulta completada en " + (endTime - startTime) + " ms");
            System.out.println("📄 Resultado:");
            System.out.println(result);

        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
        }
    }

    private static void testAvailableElections(ReportsServicePrx proxyCache) {
        try {
            System.out.println("📊 Consultando elecciones disponibles...");
            long startTime = System.currentTimeMillis();

            String[] elections = proxyCache.getAvailableElections();

            long endTime = System.currentTimeMillis();

            System.out.println("✅ Consulta completada en " + (endTime - startTime) + " ms");
            System.out.println("🗳️ Elecciones disponibles (" + elections.length + "):");

            for (int i = 0; i < elections.length; i++) {
                System.out.println("   " + (i + 1) + ". " + elections[i]);
            }

        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
        }
    }

    private static void testCachePerformance(ReportsServicePrx proxyCache) {
        try {
            System.out.println("⚡ Probando rendimiento del cache...");
            System.out.println("📋 Realizando 5 consultas del mismo ciudadano para medir cache hits");

            String documento = "12345678";
            int electionId = 1;

            // Primera consulta (cache miss)
            System.out.println("\n🔄 Consulta 1 (cache miss esperado):");
            long start1 = System.currentTimeMillis();
            String result1 = proxyCache.getCitizenReports(documento, electionId);
            long end1 = System.currentTimeMillis();
            System.out.println("   ⏱️ Tiempo: " + (end1 - start1) + " ms");

            // Consultas siguientes (cache hits esperados)
            for (int i = 2; i <= 5; i++) {
                System.out.println("\n🔄 Consulta " + i + " (cache hit esperado):");
                long startI = System.currentTimeMillis();
                String resultI = proxyCache.getCitizenReports(documento, electionId);
                long endI = System.currentTimeMillis();
                System.out.println("   ⏱️ Tiempo: " + (endI - startI) + " ms");
                System.out.println("   📊 Mismo resultado: " + result1.equals(resultI));
            }

            // Prueba de múltiples consultas rápidas
            System.out.println("\n🚀 Prueba de 10 consultas rápidas consecutivas:");
            long rapidStart = System.currentTimeMillis();
            for (int i = 0; i < 10; i++) {
                proxyCache.getCitizenReports(documento, electionId);
            }
            long rapidEnd = System.currentTimeMillis();
            long rapidTotal = rapidEnd - rapidStart;

            System.out.println("⚡ 10 consultas consecutivas: " + rapidTotal + " ms");
            System.out.println("⚡ Promedio por consulta: " + (rapidTotal/10) + " ms");

        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
        }
    }

    private static void testValidateCitizen(ReportsServicePrx proxyCache, Scanner scanner) {
        try {
            System.out.print("📝 Ingresa el documento del ciudadano para validar (ej: 12345678): ");
            String documento = scanner.nextLine().trim();

            System.out.println("✅ Validando elegibilidad del ciudadano...");
            long startTime = System.currentTimeMillis();

            boolean isValid = proxyCache.validateCitizenEligibility(documento);

            long endTime = System.currentTimeMillis();

            System.out.println("⚡ Validación completada en " + (endTime - startTime) + " ms");
            System.out.println("📊 Resultado: " + (isValid ? "✅ ELEGIBLE" : "❌ NO ELEGIBLE"));

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

            System.out.println("🏢 Consultando ciudadanos de la mesa...");
            long startTime = System.currentTimeMillis();

            String[] results = proxyCache.getMesaCitizenReports(mesaId, electionId);

            long endTime = System.currentTimeMillis();

            System.out.println("✅ Consulta completada en " + (endTime - startTime) + " ms");
            System.out.println("👥 Ciudadanos en la mesa " + mesaId + " (" + results.length + "):");

            for (int i = 0; i < Math.min(results.length, 3); i++) {
                System.out.println("   " + (i + 1) + ". " + results[i]);
            }

            if (results.length > 3) {
                System.out.println("   ... y " + (results.length - 3) + " más");
            }

        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
        }
    }

    private static void testPreloadReports(ReportsServicePrx proxyCache, Scanner scanner) {
        try {
            System.out.print("📝 Ingresa el ID de la elección (ej: 1): ");
            int electionId = Integer.parseInt(scanner.nextLine().trim());

            System.out.println("\n📋 Tipos de precarga disponibles:");
            System.out.println("   basic      - Reportes básicos y metadata");
            System.out.println("   department - Todos los ciudadanos de un departamento");
            System.out.println("   municipality - Todos los ciudadanos de un municipio");
            System.out.println("   puesto     - Todos los ciudadanos de un puesto");
            System.out.println("   mesa       - Todos los ciudadanos de una mesa");
            System.out.println("   all        - Precarga completa del sistema");

            System.out.print("\n📝 Selecciona el tipo de precarga: ");
            String locationType = scanner.nextLine().trim().toLowerCase();

            int locationId = 0;
            if (!locationType.equals("basic") && !locationType.equals("all")) {
                System.out.print("📝 Ingresa el ID de la ubicación: ");
                locationId = Integer.parseInt(scanner.nextLine().trim());
            }

            System.out.println("\n⏳ Iniciando precarga...");
            if (locationType.equals("department") || locationType.equals("all")) {
                System.out.println("⚠️ ADVERTENCIA: Esta operación puede tomar varios minutos");
            }

            long startTime = System.currentTimeMillis();

            // ✅ USAR EL NUEVO MÉTODO CON 3 PARÁMETROS
            String result = proxyCache.preloadReports(electionId, locationType, locationId);

            long endTime = System.currentTimeMillis();

            System.out.println("✅ Precarga completada en " + (endTime - startTime) + " ms");
            System.out.println("\n📊 RESULTADO DETALLADO:");
            System.out.println("=" + "=".repeat(50));
            System.out.println(result);
            System.out.println("=" + "=".repeat(50));

        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
        }
    }

    private static void testCacheStats(ReportsServicePrx proxyCache) {
        try {
            System.out.println("📊 Obteniendo estadísticas del cache...");

            String stats = proxyCache.getCacheStats();

            System.out.println("\n" + "=".repeat(60));
            System.out.println(stats);
            System.out.println("=".repeat(60));

        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
        }
    }

    private static void testAuxiliaryMethods(ReportsServicePrx proxyCache, Scanner scanner) {
        try {
            System.out.println("🔧 MÉTODOS AUXILIARES GEOGRÁFICOS");
            System.out.println("1. Documentos de departamento");
            System.out.println("2. Documentos de municipio");
            System.out.println("3. Documentos de puesto");
            System.out.println("4. Documentos de mesa");

            System.out.print("📝 Selecciona una opción (1-4): ");
            int option = Integer.parseInt(scanner.nextLine().trim());

            System.out.print("📝 Ingresa el ID de la elección (ej: 1): ");
            int electionId = Integer.parseInt(scanner.nextLine().trim());

            String[] results = null;
            long startTime = System.currentTimeMillis();

            switch (option) {
                case 1:
                    System.out.print("📝 Ingresa el ID del departamento: ");
                    int deptId = Integer.parseInt(scanner.nextLine().trim());
                    System.out.println("🏛️ Obteniendo documentos del departamento " + deptId + "...");
                    results = proxyCache.getDepartmentCitizenDocuments(deptId, electionId);
                    break;

                case 2:
                    System.out.print("📝 Ingresa el ID del municipio: ");
                    int munId = Integer.parseInt(scanner.nextLine().trim());
                    System.out.println("🏙️ Obteniendo documentos del municipio " + munId + "...");
                    results = proxyCache.getMunicipalityCitizenDocuments(munId, electionId);
                    break;

                case 3:
                    System.out.print("📝 Ingresa el ID del puesto: ");
                    int puestoId = Integer.parseInt(scanner.nextLine().trim());
                    System.out.println("🗳️ Obteniendo documentos del puesto " + puestoId + "...");
                    results = proxyCache.getPuestoCitizenDocuments(puestoId, electionId);
                    break;

                case 4:
                    System.out.print("📝 Ingresa el ID de la mesa: ");
                    int mesaId = Integer.parseInt(scanner.nextLine().trim());
                    System.out.println("📋 Obteniendo documentos de la mesa " + mesaId + "...");
                    results = proxyCache.getMesaCitizenDocuments(mesaId, electionId);
                    break;

                default:
                    System.out.println("❌ Opción inválida");
                    return;
            }

            long endTime = System.currentTimeMillis();

            if (results != null) {
                System.out.println("✅ Consulta completada en " + (endTime - startTime) + " ms");
                System.out.println("📊 Documentos encontrados: " + results.length);

                // Mostrar los primeros 10 documentos
                System.out.println("📋 Muestra de documentos:");
                for (int i = 0; i < Math.min(results.length, 10); i++) {
                    System.out.println("   " + (i + 1) + ". " + results[i]);
                }

                if (results.length > 10) {
                    System.out.println("   ... y " + (results.length - 10) + " más");
                }
            }

        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
        }
    }

    private static void testHotspotGenerator(ReportsServicePrx proxyCache, Scanner scanner) {
        try {
            System.out.println("🔥 ========== GENERADOR DE HOTSPOTS ==========");
            System.out.println("Este test simula actividad intensa para activar la precarga predictiva");

            System.out.print("📝 Ingresa el tipo de ubicación (puesto/mesa/municipality): ");
            String locationType = scanner.nextLine().trim().toLowerCase();

            System.out.print("📝 Ingresa el ID de la ubicación (ej: 1): ");
            int locationId = Integer.parseInt(scanner.nextLine().trim());

            System.out.print("📝 Número de consultas a simular (ej: 5): ");
            int numQueries = Integer.parseInt(scanner.nextLine().trim());

            int electionId = 1;

            System.out.println("\n🚀 Iniciando simulación de hotspot...");
            System.out.println("⏰ Haciendo " + numQueries + " consultas cada 10 segundos");
            System.out.println("👀 Observa los logs del servidor para ver la detección del hotspot");

            for (int round = 1; round <= 3; round++) {
                System.out.println("\n🔄 Ronda " + round + " de consultas:");

                for (int i = 1; i <= numQueries; i++) {
                    long startTime = System.currentTimeMillis();

                    switch (locationType) {
                        case "puesto":
                            String[] puestoResults = proxyCache.getPuestoCitizenDocuments(locationId, electionId);
                            System.out.println("   " + i + ". Puesto " + locationId + ": " + puestoResults.length + " documentos");
                            break;

                        case "mesa":
                            String[] mesaResults = proxyCache.getMesaCitizenReports(locationId, electionId);
                            System.out.println("   " + i + ". Mesa " + locationId + ": " + mesaResults.length + " ciudadanos");
                            break;

                        case "municipality":
                            String geoResult = proxyCache.getGeographicReports(locationId, "municipality", electionId);
                            System.out.println("   " + i + ". Municipio " + locationId + ": " +
                                    (geoResult.length() > 100 ? geoResult.substring(0, 100) + "..." : geoResult));
                            break;
                    }

                    long endTime = System.currentTimeMillis();
                    System.out.println("       ⏱️ Tiempo: " + (endTime - startTime) + " ms");

                    // Pausa corta entre consultas
                    Thread.sleep(500);
                }

                // Mostrar estadísticas después de cada ronda
                System.out.println("\n📊 Estadísticas actuales del cache:");
                String stats = proxyCache.getCacheStats();
                String[] lines = stats.split("\n");
                for (String line : lines) {
                    if (line.contains("Total consultas") ||
                            line.contains("Precarga predictiva") ||
                            line.contains("Hit rate") ||
                            line.contains("Patrones de consulta activos") ||
                            line.contains(locationType)) {
                        System.out.println("   " + line);
                    }
                }

                if (round < 3) {
                    System.out.println("\n⏳ Esperando 10 segundos para la siguiente ronda...");
                    System.out.println("💡 Durante esta pausa, el sistema debería detectar el hotspot y ejecutar precarga predictiva");
                    Thread.sleep(10000);
                }
            }

            System.out.println("\n✅ Simulación completada!");
            System.out.println("🔍 Revisa los logs del servidor para confirmar:");
            System.out.println("   • Detección de hotspot");
            System.out.println("   • Ejecución de precarga predictiva");
            System.out.println("   • Mejora en hit rate");

        } catch (Exception e) {
            System.err.println("❌ Error en simulación: " + e.getMessage());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }










}