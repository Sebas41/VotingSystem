package VotingReciever;

import Controller.ServerControllerInterface;
import com.zeroc.Ice.Current;

import model.ReliableMessage;
import reliableMessage.ACKServicePrx;
import reliableMessage.RMDestination;
import model.Vote;

public class VotingReceiverImp implements  RMDestination{

    private ServerControllerInterface controller;


    public VotingReceiverImp(ServerControllerInterface controller) {
        this.controller = controller;
    }




    @Override
    public void reciveMessage(ReliableMessage rmessage, ACKServicePrx prx, Current current) {
;
        controller.registerVote(rmessage);
        //String payload = rmessage.getMessage().message;
        //ObjectMapper mapper = new ObjectMapper();
        
        //try {
            //Vote vote = mapper.readValue(payload, Vote.class);
            //System.out.println("Received vote: " + vote);
            //controller.registerVote(rmessage);
       // } //catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            //System.err.println("Error processing JSON: " + e.getMessage());
           // return;
        //}
        
        //System.out.println(rmessage.getMessage().message);
        prx.ack(rmessage.getUuid());

    }

}
