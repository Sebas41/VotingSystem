
import controller.ControllerVote;
public class Client {

    public static void main(String[] args) throws Exception {

        new Thread(() -> {
            System.out.println("Iniciando ReliableServer...");
            ReliableServer.main(new String[0]);
        }).start();

        Thread.sleep(3000);

        System.out.println("Iniciando estacion de votacion...");
        ControllerVote controller = new ControllerVote();
        controller.run();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Deteniendo ReliableServer...");
            ReliableServer.stopBroker();
        }));
    }
}
