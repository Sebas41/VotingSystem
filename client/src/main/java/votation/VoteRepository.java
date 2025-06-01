package votation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class VoteRepository  implements VotationInterface {
    private static final String FILE_PATH = "client/data/votes.json";
    private List<Vote> votes;
    private ObjectMapper mapper;

    public VoteRepository() {
        mapper = new ObjectMapper();
        loadOrInitialize();
    }

    private void loadOrInitialize() {
        File file = new File(FILE_PATH);
        if (!file.exists() || file.length() == 0) {
            
            votes = new ArrayList<>();
            saveAll();
        } else {
            
            try {
                votes = mapper.readValue(file, new TypeReference<List<Vote>>() {});
            } catch (IOException e) {
                e.printStackTrace();
                votes = new ArrayList<>();
                saveAll();
            }
        }
    }

    private void saveAll() {
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(FILE_PATH), votes);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<Vote> findAll() {
        return new ArrayList<>(votes);
    }
    @Override
    public void save(Vote vote) {
        votes.add(vote);
        saveAll();
    }
}
