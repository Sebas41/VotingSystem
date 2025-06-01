package votation;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Election {

    private int electionId;
    private List<Candidate> candidates;

    public Election() {
    }

    @JsonCreator
    public Election(
            @JsonProperty("electionId") int electionId,
            @JsonProperty("candidates") List<Candidate> candidates) {
        this.electionId = electionId;
        this.candidates = candidates;
    }

    public int getElectionId() {
        return electionId;
    }

    public List<Candidate> getCandidates() {
        return candidates;
    }

    public void showCandidates() {
        System.out.println("Candidatos disponibles para elección #" + electionId + ":");
        for (Candidate c : candidates) {
            System.out.println(c.toString());
        }
    }
}
