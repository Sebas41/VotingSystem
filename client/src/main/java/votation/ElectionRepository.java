package votation;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;

public class ElectionRepository implements ElectionInterface {

    private static final String FILE_PATH = "client/data/election.json";
    private Election election;

    public ElectionRepository() {
        load();
    }

    private void load() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            File file = new File(FILE_PATH);
            if (!file.exists()) {
                throw new IOException("Archivo de elección no encontrado: " + FILE_PATH);
            }
            this.election = mapper.readValue(file, Election.class);
        } catch (IOException e) {
            System.err.println("Error al cargar la elección desde JSON: " + e.getMessage());
            this.election = null;
        }
    }


    @Override
    public Election getElection() {
        return election;
    }


}
