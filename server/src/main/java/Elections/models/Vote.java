package Elections.models;

import java.util.Calendar;

public class Vote  {
    
    private String machineId;
    private String vote;
    private Calendar date;
    private int election=1;

    public Vote(){}
    public Vote(String machineId, String vote) {
        this.machineId = machineId;
        this.vote = vote;
        this.date = Calendar.getInstance();
    }
    
    public String getmachineId() {
        return machineId;
    }

    public String getMachineId() {
        return machineId;
    }


    public String getVote() {
        return vote;
    }

    public void setVote(String vote) {
        this.vote = vote;
    }

    public void setMachineId(String id) {
        this.machineId = id;
    }
    public Calendar getDate() {
        return date;
    }
    public void setDate(Calendar date) {
        this.date = date;
    }
    public int getElection() {
        return election;
    }
    public void setElection(int election) {
        this.election = election;
    }
    
    public String toString() {
        return "Vote{" +
                "id='" + machineId + '\'' +
                ", vote='" + vote + '\'' +
                '}';
    }
    
}
