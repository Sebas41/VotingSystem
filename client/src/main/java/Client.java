import controller.ControllerVoteUI;

public class Client {

    public static void main(String[] args) throws Exception {

        new Thread(() -> {
            System.out.println("Iniciando ReliableServer...");
            ReliableServer.main(new String[0]);
        }).start();

        Thread.sleep(3000); // Espera a que inicie el servidor confiable

        System.out.println("Iniciando estación de votación...");

        // Lanza la interfaz gráfica en el hilo de Swing
        javax.swing.SwingUtilities.invokeLater(() -> {
            try {
                new ControllerVoteUI();  // Crea y muestra la UI
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // Hook para detener servidor cuando se cierre
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Deteniendo ReliableServer...");
            ReliableServer.stopBroker();
        }));
    }
}
