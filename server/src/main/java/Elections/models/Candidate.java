package Elections.models;

import java.util.ArrayList;
import java.util.List;

public class Candidate {

    private int id;
    private String name;
    private String politicalParty;
    private String electionId;
    private List<Vote> votes;

    public Candidate(int id, String name, String politicalParty, int electionId) {
        this.id = id;
        this.name = name;
        this.politicalParty = politicalParty;
        this.votes = new ArrayList<>();
    }

    public void addVote(Vote newVote){
        this.votes.add(newVote);
    }

    public List<Vote>  getVotes(){
        return this.votes;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPoliticalParty() {
        return politicalParty;
    }

    public void setPoliticalParty(String politicalParty) {
        this.politicalParty = politicalParty;
    }

    @Override
    public String toString() {
        return "ID: " + id + ", Nombre: " + name + ", Partido: " + politicalParty;
    }
}
