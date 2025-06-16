package votation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAlias;

public class Candidate {

    private int id;
    private String name;
    private String party;

    @JsonCreator
    public Candidate(
            @JsonProperty("id") int id,
            @JsonProperty("name") String name,
            @JsonProperty("party") String party
    ) {
        this.id = id;
        this.name = name;
        this.party = party;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    @JsonProperty("party")
    @JsonAlias({"politicalParty", "party"})
    public String getParty() {
        return party;
    }

    public String getPoliticalParty() {
        return party;
    }

    @Override
    public String toString() {
        return "[" + id + "] " + name + " (" + party + ")";
    }
}