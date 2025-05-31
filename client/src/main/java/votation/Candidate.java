
package votation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Candidate {

    private int id;
    private String name;
    private String politicalParty;

    @JsonCreator
    public Candidate(
            @JsonProperty("id") int id,
            @JsonProperty("name") String name,
            @JsonProperty("politicalParty") String politicalParty
    ) {
        this.id = id;
        this.name = name;
        this.politicalParty = politicalParty;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPoliticalParty() {
        return politicalParty;
    }

    @Override
    public String toString() {
        return "[" + id + "] " + name + " (" + politicalParty + ")";
    }
}
