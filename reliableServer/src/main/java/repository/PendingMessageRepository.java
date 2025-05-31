package repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import model.ReliableMessage;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class PendingMessageRepository {

    private final String storagePath="reliableServer/data/messages.json";
    private final ObjectMapper objectMapper;

    private final Map<String, ReliableMessage> cache = new ConcurrentHashMap<>();

    public PendingMessageRepository() {
        this.objectMapper = new ObjectMapper();
        loadFromDisk();
    }


    public Map<String, ReliableMessage> findAll() {
        return Collections.unmodifiableMap(cache);
    }

    public ReliableMessage findById(String uuid) {
        return cache.get(uuid);
    }


    public void add(ReliableMessage rm) {
        cache.put(rm.getUuid(), rm);
        persistToDisk();
    }

    public void remove(String uuid) {
        if (cache.remove(uuid) != null) {
            persistToDisk();
        }
    }


    public void replaceAll(Map<String, ReliableMessage> all) {
        cache.clear();
        cache.putAll(all);
        persistToDisk();
    }


    private synchronized void persistToDisk() {
        try {
            objectMapper.writerFor(new TypeReference<Map<String, ReliableMessage>>() {})
                        .writeValue(new File(storagePath), cache);
        } catch (IOException e) {
            System.err.println("Error persisting pending messages: " + e.getMessage());
        }
    }


    private void loadFromDisk() {
        File f = new File(storagePath);
        if (!f.exists()) return;

        try {
            Map<String, ReliableMessage> loaded = objectMapper.readValue(
                f, new TypeReference<Map<String, ReliableMessage>>() {}
            );
            if (loaded != null) {
                cache.clear();
                cache.putAll(loaded);
            }
        } catch (IOException e) {
            System.err.println("Error loading pending messages: " + e.getMessage());
        }
    }
}
