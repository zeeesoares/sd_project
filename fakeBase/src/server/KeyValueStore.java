package server;

import concurrent.ConcurrentHashMap;

import java.util.List;
import java.util.function.BiConsumer;

public class KeyValueStore {
    private ConcurrentHashMap<String, byte[]> store;

    public KeyValueStore() {
        this.store = new ConcurrentHashMap<>();
    }

    public void put(String key, byte[] value) {
        store.put(key, value);
    }

    public byte[] get(String key) {
        return store.get(key);
    }

    public byte[] remove(String key) {
        return store.remove(key);
    }

    public boolean containsKey(String key) {
        return store.containsKey(key);
    }

    public boolean containsValue(byte[] value) {
        return store.containsValue(value);
    }

    public int size() {
        return store.size();
    }


    public List<byte[]> values() {
        return store.values();
    }

    public void forEach(BiConsumer<String, byte[]> operation) {
        store.forEach(operation);
    }
}
