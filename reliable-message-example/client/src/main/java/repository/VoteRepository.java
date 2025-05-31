package repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import model.Vote;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Repositorio para gestionar la lista de votos emitidos.
 * Utiliza un archivo JSON llamado "votes.json" en el directorio de trabajo.
 */
public class VoteRepository {
    private static final String FILE_PATH = "votes.json";
    private List<Vote> votes;
    private ObjectMapper mapper;

    public VoteRepository() {
        mapper = new ObjectMapper();
        loadOrInitialize();
    }

    private void loadOrInitialize() {
        File file = new File(FILE_PATH);
        if (!file.exists() || file.length() == 0) {
            // Si no hay archivo, empezamos con lista vacía
            votes = new ArrayList<>();
            saveAll();
        } else {
            // Si existe, cargamos
            try {
                votes = mapper.readValue(file, new TypeReference<List<Vote>>() {});
            } catch (IOException e) {
                e.printStackTrace();
                votes = new ArrayList<>();
                saveAll();
            }
        }
    }

    // Guarda lista completa en votes.json
    private void saveAll() {
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(FILE_PATH), votes);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Devuelve todos los votos
    public List<Vote> findAll() {
        return new ArrayList<>(votes);
    }

    // Agrega un nuevo voto y escribe en archivo
    public void save(Vote vote) {
        votes.add(vote);
        saveAll();
    }
}
