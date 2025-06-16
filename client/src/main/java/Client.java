import controller.ControllerVoteUI;

public class Client {

    private static ControllerVoteUI controller;

    public static void main(String[] args) throws Exception {

        try {
            new Thread(() -> {
                System.out.println(" Iniciando ReliableServer...");
                ReliableServer.main(new String[0]);
            }).start();

            Thread.sleep(3000);

            System.out.println(" Iniciando estación de votación...");

            javax.swing.SwingUtilities.invokeLater(() -> {
                try {
                    controller = new ControllerVoteUI();


                    System.out.println(" Estación de votación lista");
                    System.out.println(" Mesa ID: " + controller.getMachineId());
                    System.out.println(" Servicios activos:");
                    System.out.println("   - Votación: ");
                    System.out.println("   - Configuración remota:  (Puerto " + (10020 + (controller.getMachineId() % 1000)) + ")");
                    System.out.println("   - Reliable Messaging: ");
                    System.out.println(" Esperando configuraciones del servidor...");

                } catch (Exception e) {
                    System.err.println(" Error iniciando controller de votación: " + e.getMessage());
                    e.printStackTrace();
                    System.exit(1);
                }
            });


            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Cerrando estación de votación...");

                if (controller != null) {
                    try {
                        controller.shutdown();
                        System.out.println(" Controller cerrado correctamente");
                    } catch (Exception e) {
                        System.err.println(" Error cerrando controller: " + e.getMessage());
                    }
                }

                try {
                    System.out.println(" Deteniendo ReliableServer...");
                    ReliableServer.stopBroker();
                    System.out.println(" ReliableServer cerrado correctamente");
                } catch (Exception e) {
                    System.err.println(" Error cerrando ReliableServer: " + e.getMessage());
                }

                System.out.println("Estación de votación cerrada completamente");
            }));


            synchronized (Client.class) {
                Client.class.wait();
            }

        } catch (InterruptedException e) {
            System.out.println(" Aplicación interrumpida");
            Thread.currentThread().interrupt();

        } catch (Exception e) {
            System.err.println(" Error crítico iniciando estación de votación: " + e.getMessage());
            e.printStackTrace();

            //  CLEANUP EN CASO DE ERROR
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