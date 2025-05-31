import com.zeroc.Ice.Current;

import model.ReliableMessage;
import reliableMessage.ACKServicePrx;
import reliableMessage.RMDestination;
import com.fasterxml.jackson.databind.ObjectMapper;
import model.Vote;

public class ServiceImp implements  RMDestination{

    @Override
    public void reciveMessage(ReliableMessage rmessage, ACKServicePrx prx, Current current) {
        String payload = rmessage.getMessage().message;
        ObjectMapper mapper = new ObjectMapper();   
        
        try {
            Vote vote = mapper.readValue(payload, Vote.class);
            System.out.println("Received vote: " + vote);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            System.err.println("Error processing JSON: " + e.getMessage());
            return;
        }
        
        System.out.println(rmessage.getMessage().message);
        prx.ack(rmessage.getUuid());
    }


    
}
