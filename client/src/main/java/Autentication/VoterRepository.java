package Autentication;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


public class VoterRepository {
    private static final String FILE_PATH = "client/data/voters.json";
    private List<Voter> voters;
    private ObjectMapper mapper;

    public VoterRepository() {
        mapper = new ObjectMapper();
        loadOrInitialize();
    }

    private void loadOrInitialize() {
        File file = new File(FILE_PATH);
        if (!file.exists() || file.length() == 0) {
            
            voters = new ArrayList<>();
            for (int i = 1; i <= 5; i++) {
                String id = "voter" + i;
                String name = "Votante " + i;
                String pass = "pass" + i;
                voters.add(new Voter(id, name, pass, AlreadyVote.NO));
            }
            saveAll();
        } else {
            
            try {
                voters = mapper.readValue(file, new TypeReference<List<Voter>>() {});
            } catch (IOException e) {
                e.printStackTrace();
                
                voters = new ArrayList<>();
                for (int i = 1; i <= 5; i++) {
                    String id = "voter" + i;
                    String name = "Votante " + i;
                    String pass = "pass" + i;
                    voters.add(new Voter(id, name, pass, AlreadyVote.NO));
                }
                saveAll();
            }
        }
    }


    private void saveAll() {
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(FILE_PATH), voters);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<Voter> findAll() {
        return new ArrayList<>(voters);
    }

    public Optional<Voter> findById(String id) {
        return voters.stream().filter(v -> v.getId().equals(id)).findFirst();
    }

    public void update(Voter updated) {
        for (int i = 0; i < voters.size(); i++) {
            if (voters.get(i).getId().equals(updated.getId())) {
                voters.set(i, updated);
                saveAll();
                return;
            }
        }
    }
}
