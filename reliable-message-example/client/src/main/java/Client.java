
import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.Util;

import model.Message;
import reliableMessage.RMDestinationPrx;
import reliableMessage.RMSourcePrx;

public class Client {
    
    public static void main(String[] args)throws Exception {
        Communicator com = Util.initialize();
        RMSourcePrx rm = RMSourcePrx.checkedCast(com.stringToProxy("Sender:tcp -h localhost -p 10010"));
        RMDestinationPrx dest = RMDestinationPrx.uncheckedCast(com.stringToProxy("Service:tcp -h localhost -p 10012"));

        rm.setServerProxy(dest);
        Message msg = new Message();
        for (int i = 0; i < 10; i++) {
            msg.message = "Send with RM "+i;
            rm.sendMessage(msg);
            System.out.println("sended "+i);
            Thread.sleep(5000);
        }
        com.shutdown();
        
    }
}
