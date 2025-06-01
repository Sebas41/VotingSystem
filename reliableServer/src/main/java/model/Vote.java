package model;

import java.io.Serializable;


public class Vote implements Serializable{

    public String machineId;
    public String vote;
    public long date;
    public int electionId = 1;
    
    public Vote(String machine, String vote, long date, int electionId) {
        this.machineId = machine;
        this.vote = vote;
        this.date = date;
        this.electionId = electionId;
    }

    public Vote() {
        // Default constructor for serialization
    }
}
