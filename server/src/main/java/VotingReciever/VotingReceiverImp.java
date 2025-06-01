package VotingReciever;

import Controller.ServerControllerInterface;
import com.zeroc.Ice.Current;

import model.ReliableMessage;
import reliableMessage.ACKServicePrx;
import reliableMessage.RMDestination;
import com.fasterxml.jackson.databind.ObjectMapper;
import Elections.models.Vote;

public class VotingReceiverImp implements  RMDestination{

    private ServerControllerInterface controller;


    public VotingReceiverImp(ServerControllerInterface controller) {
        this.controller = controller;
    }




    @Override
    public void reciveMessage(ReliableMessage rmessage, ACKServicePrx prx, Current current) {

        String payload = rmessage.getMessage().message;
        ObjectMapper mapper = new ObjectMapper();
        
        try {
            Vote vote = mapper.readValue(payload, Vote.class);
            controller.registerVote(rmessage);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            System.err.println("Error processing JSON: " + e.getMessage());
            return;
        }
        

        prx.ack(rmessage.getUuid());

    }

}
