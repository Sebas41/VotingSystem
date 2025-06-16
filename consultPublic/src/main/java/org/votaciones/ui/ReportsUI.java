package org.votaciones.ui;

import org.votaciones.service.ReportsService;
import java.util.Scanner;

public class ReportsUI {
    private final ReportsService service;
    private final Scanner scanner;

    public ReportsUI() {
        this.service = new ReportsService();
        this.scanner = new Scanner(System.in);
    }

    public void start() {
        try {
            System.out.println(" ========== CLIENTE DE CONSULTA PÚBLICA ==========");
            System.out.println("🔌 Conectando al proxy cache...");

            // Conectar al proxy cache
            service.connect("localhost", 9999);

            System.out.println(" Conectado al proxy cache exitosamente");
            System.out.println("📊 Todas las consultas pasarán por el cache local");
            System.out.println();

            boolean running = true;
            while (running) {
                mostrarMenu();
                String opcion = scanner.nextLine().trim();

                switch (opcion) {
                    case "1":
                        consultarReporteCiudadano();
                        break;
                    case "2":
                        buscarCiudadanos();
                        break;
                    case "3":
                        consultarReporteEleccion();
                        break;
                    case "4":
                        consultarReporteGeografico();
                        break;
                    case "5":
                        consultarEleccionesDisponibles();
                        break;
                    case "6":
                        validarElegibilidadCiudadano();
                        break;
                    case "7":
                        consultarCiudadanosMesa();
                        break;
                    case "8":
                        verEstadisticasCache();
                        break;
                    case "0":
                        running = false;
                        break;
                    default:
                        System.out.println(" Opción inválida");
                }

                if (running) {
                    System.out.println("\nPresiona Enter para continuar...");
                    scanner.nextLine();
                }
            }

            System.out.println("👋 Cliente finalizado");
            service.disconnect();

        } catch (Exception e) {
            System.err.println(" Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void mostrarMenu() {
        System.out.println("\n📋 ========== MENÚ DE CONSULTAS ==========");
        System.out.println("1.  Consultar reporte de ciudadano");
        System.out.println("2.  Buscar ciudadanos por nombre");
        System.out.println("3. ️Consultar reporte de elección");
        System.out.println("4.  Consultar reporte geográfico");
        System.out.println("5.  Consultar elecciones disponibles");
        System.out.println("6.  Validar elegibilidad de ciudadano");
        System.out.println("7.  Consultar ciudadanos de mesa");
        System.out.println("8.  Ver estadísticas del cache");
        System.out.println("0.  Salir");
        System.out.println("==========================================");
        System.out.print("Selecciona una opción: ");
    }

    private void consultarReporteCiudadano() {
        try {
            System.out.print(" Ingresa el documento del ciudadano (ej: 12345678): ");
            String documento = scanner.nextLine().trim();

            System.out.print(" Ingresa el ID de la elección (ej: 1): ");
            int electionId = Integer.parseInt(scanner.nextLine().trim());

            System.out.println("⏳ Consultando reporte del ciudadano...");
            long startTime = System.currentTimeMillis();

            String result = service.getCitizenReports(documento, electionId);

            long endTime = System.currentTimeMillis();
            System.out.println(" Consulta completada en " + (endTime - startTime) + " ms");
            System.out.println(" Resultado:");
            System.out.println(result);

        } catch (Exception e) {
            System.err.println(" Error: " + e.getMessage());
        }
    }

    private void buscarCiudadanos() {
        try {
            System.out.print(" Ingresa el nombre (o vacío para omitir): ");
            String nombre = scanner.nextLine().trim();

            System.out.print(" Ingresa el apellido (o vacío para omitir): ");
            String apellido = scanner.nextLine().trim();

            System.out.print(" Ingresa el ID de la elección (ej: 1): ");
            int electionId = Integer.parseInt(scanner.nextLine().trim());

            System.out.print(" Ingresa el límite de resultados (ej: 10): ");
            int limit = Integer.parseInt(scanner.nextLine().trim());

            System.out.println("🔍 Buscando ciudadanos...");
            long startTime = System.currentTimeMillis();

            String[] results = service.searchCitizenReports(nombre, apellido, electionId, limit);

            long endTime = System.currentTimeMillis();
            System.out.println(" Búsqueda completada en " + (endTime - startTime) + " ms");
            System.out.println("📊 Encontrados " + results.length + " resultados:");

            for (int i = 0; i < Math.min(results.length, 5); i++) {
                System.out.println("   " + (i + 1) + ". " + results[i]);
            }

            if (results.length > 5) {
                System.out.println("   ... y " + (results.length - 5) + " más");
            }

        } catch (Exception e) {
            System.err.println(" Error: " + e.getMessage());
        }
    }

    private void consultarReporteEleccion() {
        try {
            System.out.print(" Ingresa el ID de la elección (ej: 1): ");
            int electionId = Integer.parseInt(scanner.nextLine().trim());

            System.out.println(" Consultando reporte de elección...");
            long startTime = System.currentTimeMillis();

            String result = service.getElectionReports(electionId);

            long endTime = System.currentTimeMillis();
            System.out.println(" Consulta completada en " + (endTime - startTime) + " ms");
            System.out.println(" Resultado:");
            System.out.println(result);

        } catch (Exception e) {
            System.err.println(" Error: " + e.getMessage());
        }
    }

    private void consultarReporteGeografico() {
        try {
            System.out.println(" Tipos de ubicación disponibles:");
            System.out.println("   - department (departamento)");
            System.out.println("   - municipality (municipio)");
            System.out.println("   - puesto (puesto de votación)");

            System.out.print(" Ingresa el tipo de ubicación: ");
            String locationType = scanner.nextLine().trim();

            System.out.print(" Ingresa el ID de la ubicación (ej: 1): ");
            int locationId = Integer.parseInt(scanner.nextLine().trim());

            System.out.print(" Ingresa el ID de la elección (ej: 1): ");
            int electionId = Integer.parseInt(scanner.nextLine().trim());

            System.out.println(" Consultando reporte geográfico...");
            long startTime = System.currentTimeMillis();

            String result = service.getGeographicReports(locationId, locationType, electionId);

            long endTime = System.currentTimeMillis();
            System.out.println(" Consulta completada en " + (endTime - startTime) + " ms");
            System.out.println(" Resultado:");
            System.out.println(result);

        } catch (Exception e) {
            System.err.println(" Error: " + e.getMessage());
        }
    }

    private void consultarEleccionesDisponibles() {
        try {
            System.out.println("📊 Consultando elecciones disponibles...");
            long startTime = System.currentTimeMillis();

            String[] elections = service.getAvailableElections();

            long endTime = System.currentTimeMillis();
            System.out.println(" Consulta completada en " + (endTime - startTime) + " ms");
            System.out.println(" Elecciones disponibles (" + elections.length + "):");

            for (int i = 0; i < elections.length; i++) {
                System.out.println("   " + (i + 1) + ". " + elections[i]);
            }

        } catch (Exception e) {
            System.err.println(" Error: " + e.getMessage());
        }
    }

    private void validarElegibilidadCiudadano() {
        try {
            System.out.print(" Ingresa el documento del ciudadano para validar (ej: 12345678): ");
            String documento = scanner.nextLine().trim();

            System.out.println(" Validando elegibilidad del ciudadano...");
            long startTime = System.currentTimeMillis();

            boolean isValid = service.validateCitizenEligibility(documento);

            long endTime = System.currentTimeMillis();
            System.out.println("⚡ Validación completada en " + (endTime - startTime) + " ms");
            System.out.println("📊 Resultado: " + (isValid ? " ELEGIBLE" : " NO ELEGIBLE"));

        } catch (Exception e) {
            System.err.println(" Error: " + e.getMessage());
        }
    }

    private void consultarCiudadanosMesa() {
        try {
            System.out.print(" Ingresa el ID de la mesa (ej: 1): ");
            int mesaId = Integer.parseInt(scanner.nextLine().trim());

            System.out.print(" Ingresa el ID de la elección (ej: 1): ");
            int electionId = Integer.parseInt(scanner.nextLine().trim());

            System.out.println("🏢 Consultando ciudadanos de la mesa...");
            long startTime = System.currentTimeMillis();

            String[] results = service.getMesaCitizenReports(mesaId, electionId);

            long endTime = System.currentTimeMillis();
            System.out.println(" Consulta completada en " + (endTime - startTime) + " ms");
            System.out.println("👥 Ciudadanos en la mesa " + mesaId + " (" + results.length + "):");

            for (int i = 0; i < Math.min(results.length, 3); i++) {
                System.out.println("   " + (i + 1) + ". " + results[i]);
            }

            if (results.length > 3) {
                System.out.println("   ... y " + (results.length - 3) + " más");
            }

        } catch (Exception e) {
            System.err.println(" Error: " + e.getMessage());
        }
    }

    private void verEstadisticasCache() {
        try {
            System.out.println("📊 Obteniendo estadísticas del cache...");
            String stats = service.getCacheStats();
            System.out.println("\n" + "=".repeat(60));
            System.out.println(stats);
            System.out.println("=".repeat(60));

        } catch (Exception e) {
            System.err.println(" Error: " + e.getMessage());
        }
    }
}