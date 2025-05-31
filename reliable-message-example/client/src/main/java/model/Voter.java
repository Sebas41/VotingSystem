package model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Representa un votante:
 * - id: cédula o identificador único
 * - name: nombre
 * - password: clave de acceso
 * - alreadyVote: enum YES/NO para saber si ya votó
 */
public class Voter {
    private String id;
    private String name;
    private String password;
    private alreadyVote alreadyVote;

    // Constructor para Jackson
    @JsonCreator
    public Voter(
            @JsonProperty("id") String id,
            @JsonProperty("name") String name,
            @JsonProperty("password") String password,
            @JsonProperty("alreadyVote") alreadyVote alreadyVote) {
        this.id = id;
        this.name = name;
        this.password = password;
        this.alreadyVote = alreadyVote;
    }

    // Getters y setters
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPassword() {
        return password;
    }

    public alreadyVote getAlreadyVote() {
        return alreadyVote;
    }

    public void setAlreadyVote(alreadyVote alreadyVote) {
        this.alreadyVote = alreadyVote;
    }

    // Determina si ya votó
    public boolean isAlreadyVote() {
        System.out.println("Votante " + name + " ya votó: " + alreadyVote);
        return alreadyVote == alreadyVote.YES;
    }

    // Marca como que ya votó
    public void vote() {
        this.alreadyVote = model.alreadyVote.YES;
    }
}
