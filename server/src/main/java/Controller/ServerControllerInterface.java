package Controller;

import model.ReliableMessage;

public interface ServerControllerInterface {

    void registerVote(ReliableMessage newVote);


    String getElectionInfo();
}
