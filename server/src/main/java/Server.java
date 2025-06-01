import VotingReciever.VotingReceiverImp;
import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.ObjectAdapter;
import com.zeroc.Ice.Util;
import Controller.ServerControllerImpl;
import Controller.ServerControllerInterface;
import ServerUI.ServerUI; // Asegúrate que el package esté bien

import javax.swing.*;

public class Server {

    public static void main(String[] args) {
        try {
            // Crear controlador del servidor
            ServerControllerInterface controller = new ServerControllerImpl();

            // Lanzar la UI en otro hilo para que no bloquee ICE
//            SwingUtilities.invokeLater(() -> {
//                ServerUI.launchUI(controller);
//            });

            // Inicializar Ice y vincular VotingReceiver
            Communicator com = Util.initialize();
            VotingReceiverImp imp = new VotingReceiverImp(controller); // pasa el controller
            ObjectAdapter adapter = com.createObjectAdapterWithEndpoints("Server", "tcp -h localhost -p 10012");
            adapter.add(imp, Util.stringToIdentity("Service"));
            adapter.activate();
            com.waitForShutdown();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
