package votation;

import java.util.Calendar;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;


public class Vote {
    private String machineId;
    private String vote;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    private Calendar date;
    private int electionId = 1;

    public Vote() {
    }

    @JsonCreator
    public Vote(
            @JsonProperty("machineId") String machineId,
            @JsonProperty("vote") String vote,
            @JsonProperty("date") Calendar date,
            @JsonProperty("electionId") int electionId) {
        this.machineId = machineId;
        this.vote = vote;
        this.date = date;
        this.electionId = electionId;
    }

    public Vote(String machineId, String vote) {
        this.machineId = machineId;
        this.vote = vote;
        this.date = Calendar.getInstance();
    }

    // Getters y setters
    public String getMachineId() {
        return machineId;
    }

    public String getVote() {
        return vote;
    }

    public Calendar getDate() {
        return date;
    }

    public int getElection() {
        return electionId;
    }

    @Override
    public String toString() {
        return "Vote{" +
                "machineId='" + machineId + '\'' +
                ", vote='" + vote + '\'' +
                ", date=" + date.getTime() +
                ", electionId=" + electionId +
                '}';
    }
}
