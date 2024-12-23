package common;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

public class KeyValueStoreGrained {
    private final Map<String, byte[]> store = new HashMap<>();
    private final Map<String, ReentrantLock> locks = new HashMap<>(); // Locks por chave
    private final ReentrantLock globalLock = new ReentrantLock(); // Lock para sincronizar acesso ao mapa de locks

    private ReentrantLock getLockForKey(String key) {
        globalLock.lock(); // Garante que o acesso ao mapa de locks é thread-safe
        try {
            return locks.computeIfAbsent(key, k -> new ReentrantLock());
        } finally {
            globalLock.unlock();
        }
    }

    public void put(String key, byte[] value) {
        ReentrantLock keyLock = getLockForKey(key);
        keyLock.lock();
        try {
            store.put(key, value);
        } finally {
            keyLock.unlock();
        }
    }

    public byte[] get(String key) {
        ReentrantLock keyLock = getLockForKey(key);
        keyLock.lock();
        try {
            return store.get(key);
        } finally {
            keyLock.unlock();
        }
    }

    public void multiPut(Map<String, byte[]> pairs) {
        // Ordena as chaves para evitar deadlocks
        List<String> sortedKeys = new ArrayList<>(pairs.keySet());
        Collections.sort(sortedKeys); // Ordena as chaves em ordem natural
        Map<String, ReentrantLock> acquiredLocks = new HashMap<>();

        try {
            // Adquire locks para todas as chaves na ordem
            for (String key : sortedKeys) {
                ReentrantLock keyLock = getLockForKey(key);
                keyLock.lock();
                acquiredLocks.put(key, keyLock);
            }

            // Realiza o put de todas as chaves e valores
            for (Map.Entry<String, byte[]> entry : pairs.entrySet()) {
                store.put(entry.getKey(), entry.getValue());
            }
        } finally {
            // Libera todos os locks na ordem inversa
            for (int i = sortedKeys.size() - 1; i >= 0; i--) {
                acquiredLocks.get(sortedKeys.get(i)).unlock();
            }
        }
    }

    public Map<String, byte[]> multiGet(Set<String> keys) {
        // Ordena as chaves para evitar deadlocks
        List<String> sortedKeys = new ArrayList<>(keys);
        Collections.sort(sortedKeys); // Ordena as chaves em ordem natural
        Map<String, ReentrantLock> acquiredLocks = new HashMap<>();
        Map<String, byte[]> result = new HashMap<>();

        try {
            // Adquire locks para todas as chaves na ordem
            for (String key : sortedKeys) {
                ReentrantLock keyLock = getLockForKey(key);
                keyLock.lock();
                acquiredLocks.put(key, keyLock);
            }

            // Realiza o get de todas as chaves
            for (String key : keys) {
                if (store.containsKey(key)) {
                    result.put(key, store.get(key));
                }
            }
        } finally {
            // Libera todos os locks na ordem inversa
            for (int i = sortedKeys.size() - 1; i >= 0; i--) {
                acquiredLocks.get(sortedKeys.get(i)).unlock();
            }
        }
        return result;
    }
}
