package common;

import java.util.*;
import java.util.concurrent.locks.*;

public class KeyValueStoreGrained {
    private final Map<String, byte[]> store = new HashMap<>();
    private final Map<String, ReentrantReadWriteLock> locks = new HashMap<>(); // Locks por chave
    private final Map<String, Condition> conditions = new HashMap<>();
    private final ReentrantLock globalLock = new ReentrantLock();

    private ReentrantReadWriteLock getLockForKey(String key) {
        globalLock.lock();
        try {
            return locks.computeIfAbsent(key, k -> new ReentrantReadWriteLock());
        } finally {
            globalLock.unlock();
        }
    }

    public void put(String key, byte[] value) {
        ReentrantReadWriteLock keyLock = getLockForKey(key);
        keyLock.writeLock().lock();
        try {
            store.put(key, value);
            if (conditions.containsKey(key)) {
                conditions.get(key).signalAll();
            }
        } finally {
            keyLock.writeLock().unlock();
        }
    }

    public byte[] get(String key) {
        ReentrantReadWriteLock keyLock = getLockForKey(key);
        keyLock.readLock().lock();
        try {
            return store.get(key);
        } finally {
            keyLock.readLock().unlock();
        }
    }

    public void multiPut(Map<String, byte[]> pairs) {
        List<String> sortedKeys = new ArrayList<>(pairs.keySet());
        Collections.sort(sortedKeys); // Ordena as chaves em ordem natural
        Map<String, ReentrantReadWriteLock> acquiredLocks = new HashMap<>();

        try {
            for (String key : sortedKeys) {
                ReentrantReadWriteLock keyLock = getLockForKey(key);
                keyLock.writeLock().lock();
                acquiredLocks.put(key, keyLock);
            }

            // Realiza o put de todas as chaves e valores
            for (Map.Entry<String, byte[]> entry : pairs.entrySet()) {
                store.put(entry.getKey(), entry.getValue());
                if (conditions.containsKey(entry.getKey())) {
                    conditions.get(entry.getKey()).signalAll();
                }
            }
        } finally {
            for (int i = sortedKeys.size() - 1; i >= 0; i--) {
                acquiredLocks.get(sortedKeys.get(i)).writeLock().unlock();
            }
        }
    }

    public Map<String, byte[]> multiGet(Set<String> keys) {
        List<String> sortedKeys = new ArrayList<>(keys);
        Collections.sort(sortedKeys);
        Map<String, ReentrantReadWriteLock> acquiredLocks = new HashMap<>();
        Map<String, byte[]> result = new HashMap<>();

        try {
            for (String key : sortedKeys) {
                ReentrantReadWriteLock keyLock = getLockForKey(key);
                keyLock.readLock().lock();
                acquiredLocks.put(key, keyLock);
            }

            for (String key : keys) {
                if (store.containsKey(key)) {
                    result.put(key, store.get(key));
                }
            }
        } finally {
            for (int i = sortedKeys.size() - 1; i >= 0; i--) {
                acquiredLocks.get(sortedKeys.get(i)).readLock().unlock();
            }
        }
        return result;
    }

    public byte[] getWhen(String key, String keyCond, byte[] valueCond) throws InterruptedException {
        ReentrantReadWriteLock keyCondLock = getLockForKey(keyCond);
        keyCondLock.writeLock().lock(); // Utiliza writeLock para manipular condições
        try {
            Condition condition = conditions.computeIfAbsent(keyCond, k -> keyCondLock.writeLock().newCondition());
            while (!Arrays.equals(store.getOrDefault(keyCond, null), valueCond)) {
                condition.await();
            }
        } finally {
            keyCondLock.writeLock().unlock();
        }

        return get(key); // Lê o valor da chave principal
    }
}
