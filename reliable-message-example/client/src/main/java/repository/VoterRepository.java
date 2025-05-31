package repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import model.Voter;
import model.alreadyVote;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio para gestionar la lista de votantes.
 * Utiliza un archivo JSON llamado "voters.json" en el directorio de trabajo.
 * Si el archivo no existe o está vacío, inicializa 5 votantes básicos.
 */
public class VoterRepository {
    private static final String FILE_PATH = "voters.json";
    private List<Voter> voters;
    private ObjectMapper mapper;

    public VoterRepository() {
        mapper = new ObjectMapper();
        loadOrInitialize();
    }

    private void loadOrInitialize() {
        File file = new File(FILE_PATH);
        if (!file.exists() || file.length() == 0) {
            // Si no existe el archivo o está vacío, creamos 5 votantes por defecto:
            voters = new ArrayList<>();
            for (int i = 1; i <= 5; i++) {
                String id = "voter" + i;         // p.ej. "voter1", "voter2", ...
                String name = "Votante " + i;
                String pass = "pass" + i;       // contraseña por defecto
                voters.add(new Voter(id, name, pass, alreadyVote.NO));
            }
            saveAll();
        } else {
            // Si ya existe, lo leemos:
            try {
                voters = mapper.readValue(file, new TypeReference<List<Voter>>() {});
            } catch (IOException e) {
                e.printStackTrace();
                // En caso de error, inicializar igualmente 5 por defecto:
                voters = new ArrayList<>();
                for (int i = 1; i <= 5; i++) {
                    String id = "voter" + i;
                    String name = "Votante " + i;
                    String pass = "pass" + i;
                    voters.add(new Voter(id, name, pass, alreadyVote.NO));
                }
                saveAll();
            }
        }
    }

    // Guarda la lista completa en voters.json
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
