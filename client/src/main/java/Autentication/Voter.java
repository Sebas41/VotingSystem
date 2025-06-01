package Autentication;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;


public class Voter {
    private String id;
    private String name;
    private String password;
    private AlreadyVote alreadyVote;

    @JsonCreator
    public Voter(
            @JsonProperty("id") String id,
            @JsonProperty("name") String name,
            @JsonProperty("password") String password,
            @JsonProperty("alreadyVote") AlreadyVote alreadyVote) {
        this.id = id;
        this.name = name;
        this.password = password;
        this.alreadyVote = alreadyVote;
    }


    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPassword() {
        return password;
    }

    public AlreadyVote getAlreadyVote() {
        return alreadyVote;
    }

    public void setAlreadyVote(AlreadyVote alreadyVote) {
        this.alreadyVote = alreadyVote;
    }


    public boolean isAlreadyVote() {
        return alreadyVote == Autentication.AlreadyVote.YES;
    }

    public void vote() {
        this.alreadyVote = Autentication.AlreadyVote.YES;
    }
}
