package common;

import java.util.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class KeyValueStoreGrained {
    private final Map<String, byte[]> store = new HashMap<>();
    private final Map<String, ReentrantLock> locks = new HashMap<>(); // Locks por chave
    private final Map<String,Condition> conditions = new HashMap<>();
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
            // Sinaliza qualquer thread esperando pela condição associada
            if (conditions.containsKey(key)) {
                conditions.get(key).signalAll();
            }
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

    public byte[] getWhen(String key, String keyCond, byte[] valueCond) throws InterruptedException {
        ReentrantLock keyCondLock = getLockForKey(keyCond);
        keyCondLock.lock();
        try {
            // Obtém ou cria uma condição associada à chave
            Condition condition = conditions.computeIfAbsent(keyCond, k -> keyCondLock.newCondition());
            // Espera até que o valor da chave condicional seja igual a `valueCond`
            while (!Arrays.equals(store.getOrDefault(keyCond, null), valueCond)) {
                condition.await();
            }
        } finally {
            keyCondLock.unlock();
        }

        return get(key);
    }
}
