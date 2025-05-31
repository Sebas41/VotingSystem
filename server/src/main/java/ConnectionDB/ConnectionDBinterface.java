package ConnectionDB;

import Elections.models.Vote;

public interface ConnectionDBinterface {

    void storeVote(Vote newVote);

}
