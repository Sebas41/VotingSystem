import controller.ControllerVoteUI;
import tools.BulkVoteSender;

public class Client {

    public static void main(String[] args) throws Exception {

        // Inicia el servidor confiable
        new Thread(() -> {
            System.out.println("Iniciando ReliableServer...");
            ReliableServer.main(new String[0]);
        }).start();

        // Esperar a que inicie el servidor
        Thread.sleep(3000);

        // Ejecutar prueba automática de votos
        System.out.println("Ejecutando prueba automática de envío de votos...");
        BulkVoteSender.runTest();

        // Iniciar la estación de votación (UI)
        //System.out.println("Iniciando estación de votación...");
//        javax.swing.SwingUtilities.invokeLater(() -> {
//            try {
//                new ControllerVoteUI();  // UI real
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        });

        // Hook para detener servidor al cerrar cliente
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Deteniendo ReliableServer...");
            ReliableServer.stopBroker();
        }));
    }
}
