package votation;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import model.Vote;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * VoteRepository que mantiene un ArrayList<Vote> en memoria
 * y lo persiste entero en un archivo binario usando Kryo.
 */
public class VoteRepository implements VotationInterface {
    private static final String FILE_PATH = "client/data/votes_list.kryo";
    private final File file = new File(FILE_PATH);

    // Lista en memoria que guarda todos los Vote en orden de inserción
    private final List<Vote> votes = new ArrayList<>();

    private final Kryo kryo = new Kryo();

    public VoteRepository() {
        kryo.register(ArrayList.class);
        kryo.register(Vote.class);
        loadFromDisk();
    }

    @SuppressWarnings("unchecked")
    private void loadFromDisk() {
        // Asegurarse de que la carpeta padre exista
        file.getParentFile().mkdirs();

        if (!file.exists()) {
            // No existe el archivo aún => nada que cargar
            return;
        }

        try (Input input = new Input(new FileInputStream(file))) {
            Object obj = kryo.readObject(input, ArrayList.class);
            if (obj instanceof ArrayList) {
                votes.clear();
                votes.addAll((ArrayList<Vote>) obj);
            }
        } catch (IOException e) {
            System.err.println("Error cargando votos con Kryo: " + e.getMessage());
        }
    }


    private  void saveToDisk() {
        try (Output output = new Output(new FileOutputStream(file))) {
            kryo.writeObject(output, votes);
        } catch (IOException e) {
            System.err.println("Error guardando votos con Kryo: " + e.getMessage());
        }
    }

    public List<Vote> findAll() {
        synchronized (votes) {
            return new ArrayList<>(votes);
        }
    }

    @Override
    public void save(Vote vote) {
            votes.add(vote);
            saveToDisk();
    }
}
