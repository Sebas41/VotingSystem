import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.ObjectAdapter;
import com.zeroc.Ice.ObjectPrx;
import com.zeroc.Ice.Util;

import communication.Notification;
import reliableMessage.ACKServicePrx;
import services.RMReciever;
import services.RMSender;
import threads.RMJob;

public class ReliableServer {

    private static RMJob job;
    private static Communicator communicator;

    public static void main(String[] args) {
        startBroker(args);
        communicator.waitForShutdown();
    }

    public static void startBroker(String[] iceArgs){
        communicator = Util.initialize(iceArgs, "rmservice.config");
        Notification notification = new Notification();
        job = new RMJob(notification);
        RMReciever rec = new RMReciever(job);
        RMSender sender = new RMSender(job, notification);

        ObjectAdapter adapter = communicator.createObjectAdapter("RMService");
        adapter.add(sender, Util.stringToIdentity("Sender"));
        ObjectPrx prx = adapter.add(rec, Util.stringToIdentity("AckCallback"));
        notification.setAckService(ACKServicePrx.checkedCast(prx));
        adapter.activate();
        job.start();

        System.out.println("========== RELIABLE SERVER INICIADO ==========");
        System.out.println("Escuchando en: 192.168.131.24:10010");
        System.out.println("Servidor destino: 192.168.131.21:10012");
        System.out.println("Estado: ACTIVO");
        System.out.println("==============================================");
    }

    public static void stopBroker() {
        if (job != null) {
            job.setEnable(false);
        }
        if (communicator != null) {
            communicator.shutdown();
        }
        System.out.println("Reliable Server detenido");
    }
}