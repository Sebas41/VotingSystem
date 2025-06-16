import controller.ControllerVoteUI;
import tools.BulkVoteSender;
import java.util.Scanner;

public class Client {

    private static ControllerVoteUI controller;

    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        boolean salir = false;

        while (!salir) {
            System.out.println("\n=== MENÚ PRINCIPAL ===");
            System.out.println("1. Modo normal (UI de votación)");
            System.out.println("2. Modo de prueba masiva (BulkVoteSender)");
            System.out.println("3. Salir del programa");
            System.out.print("Ingrese su opción (1-3): ");

            int opcion;
            try {
                opcion = Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Error: Por favor ingrese un número válido.");
                continue;
            }

            switch (opcion) {
                case 1:
                    iniciarModoNormal();
                    break;
                case 2:
                    iniciarModoPrueba(scanner);
                    break;
                case 3:
                    System.out.println("Cerrando programa...");
                    salir = true;
                    break;
                default:
                    System.out.println("Error: Opción no válida. Por favor ingrese un número entre 1 y 3.");
            }
        }

        scanner.close();
        System.exit(0);
    }

    private static void iniciarModoPrueba(Scanner scanner) {
        int numVotos = 0;
        boolean entradaValida = false;

        while (!entradaValida) {
            System.out.print("Ingrese el número de votos a enviar (mínimo 1): ");
            try {
                numVotos = Integer.parseInt(scanner.nextLine().trim());
                if (numVotos > 0) {
                    entradaValida = true;
                } else {
                    System.out.println("Error: El número de votos debe ser mayor a 0.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Error: Por favor ingrese un número válido.");
            }
        }

        try {
            System.out.println("Iniciando prueba masiva de votación...");
            BulkVoteSender.runTest(numVotos);
            System.out.println("Prueba completada.");
        } catch (Exception e) {
            System.err.println("Error en modo de prueba masiva: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void iniciarModoNormal() {
        try {

            System.out.println("Iniciando estación de votación...");

            javax.swing.SwingUtilities.invokeLater(() -> {
                try {
                    controller = new ControllerVoteUI();

                    System.out.println("Estación de votación lista");
                    System.out.println("Mesa ID: " + controller.getMachineId());
                    System.out.println("Servicios activos:");
                    System.out.println("   - Votación activa");
                    System.out.println("   - Configuración remota activa (Puerto "
                            + (10020 + (controller.getMachineId() % 1000)) + ")");
                    System.out.println("   - Reliable Messaging activo");
                    System.out.println("Esperando configuraciones del servidor...");

                } catch (Exception e) {
                    System.err.println("Error iniciando controller de votación: " + e.getMessage());
                    e.printStackTrace();
                    System.exit(1);
                }
            });

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Cerrando estación de votación...");

                if (controller != null) {
                    try {
                        controller.shutdown();
                        System.out.println("Controller cerrado correctamente");
                    } catch (Exception e) {
                        System.err.println("Error cerrando controller: " + e.getMessage());
                    }
                }

                try {
                    System.out.println("Deteniendo ReliableServer...");
                    ReliableServer.stopBroker();
                    System.out.println("ReliableServer cerrado correctamente");
                } catch (Exception e) {
                    System.err.println("Error cerrando ReliableServer: " + e.getMessage());
                }

                System.out.println("Estación de votación cerrada completamente");
            }));

            synchronized (Client.class) {
                Client.class.wait();
            }

        } catch (InterruptedException e) {
            System.out.println("Aplicación interrumpida");
            Thread.currentThread().interrupt();

        } catch (Exception e) {
            System.err.println("Error crítico iniciando estación de votación: " + e.getMessage());
            e.printStackTrace();
            cleanup();
            System.exit(1);
        }
    }

    private static void cleanup() {
        System.out.println("Ejecutando cleanup de emergencia...");

        if (controller != null) {
            try {
                controller.shutdown();
            } catch (Exception e) {
                System.err.println("Error en cleanup del controller: " + e.getMessage());
            }
        }

        try {
            ReliableServer.stopBroker();
        } catch (Exception e) {
            System.err.println("Error en cleanup del ReliableServer: " + e.getMessage());
        }
    }

    public static ControllerVoteUI getController() {
        return controller;
    }

    public static void shutdown() {
        synchronized (Client.class) {
            Client.class.notifyAll();
        }
    }
}