package repository;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import model.ReliableMessage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PendingMessageStoreMap {

    private final String filePath = "reliableServer/data/messages_map.kryo";
    private final File file = new File(filePath);

    private final ConcurrentHashMap<String, ReliableMessage> store = new ConcurrentHashMap<>();

    private final Kryo kryo = new Kryo();

    public PendingMessageStoreMap() {
        kryo.register(ConcurrentHashMap.class);
        kryo.register(ReliableMessage.class);
        kryo.register(model.Vote.class);

        loadFromDisk();
    }


    public Map<String, ReliableMessage> findAll() {
        return Collections.unmodifiableMap(store);
    }

    public ReliableMessage findById(String uuid) {
        return store.get(uuid);
    }

    public void add(ReliableMessage msg) {
        store.put(msg.getUuid(), msg);
        saveToDisk();
    }


    public void remove(String uuid) {
        if (store.remove(uuid) != null) {
            saveToDisk();
        }
    }


    public void replaceAll(Map<String, ReliableMessage> all) {
        store.clear();
        store.putAll(all);
        saveToDisk();
    }


    private synchronized void saveToDisk() {
        try (Output output = new Output(new FileOutputStream(file))) {
            kryo.writeObject(output, store);
        } catch (IOException e) {
            System.err.println("Error guardando mensajes con Kryo: " + e.getMessage());
        }
    }


    @SuppressWarnings("unchecked")
    private void loadFromDisk() {
        if (!file.exists()) return;
        try (Input input = new Input(new FileInputStream(file))) {
            Object read = kryo.readObject(input, ConcurrentHashMap.class);
            if (read instanceof ConcurrentHashMap) {
                store.putAll((ConcurrentHashMap<String, ReliableMessage>) read);
            }
        } catch (IOException e) {
            System.err.println("Error cargando mensajes con Kryo: " + e.getMessage());
        }
    }
}
