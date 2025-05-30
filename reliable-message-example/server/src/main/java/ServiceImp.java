import com.zeroc.Ice.Current;

import model.ReliableMessage;
import reliableMessage.ACKServicePrx;
import reliableMessage.RMDestination;
public class ServiceImp implements  RMDestination{

    @Override
    public void reciveMessage(ReliableMessage rmessage, ACKServicePrx prx, Current current) {
        System.out.println(rmessage.getMessage().message);
        prx.ack(rmessage.getUuid());
    }

    
}
