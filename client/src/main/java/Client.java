import controller.ControllerVoteUI;
import tools.BulkVoteSender;

public class Client {

    public static void main(String[] args) throws Exception {

        // Ejecutar prueba automática de votos
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

    }
}

