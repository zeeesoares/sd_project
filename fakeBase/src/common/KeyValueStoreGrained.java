package common;

import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class KeyValueStoreGrained {

    private final Map<String, byte[]> store = new HashMap<>();
    private final Map<String, ReentrantReadWriteLock> locks = new HashMap<>();
    private final ReentrantReadWriteLock globalLock = new ReentrantReadWriteLock();

    private ReentrantReadWriteLock getLockForKey(String key) {
        globalLock.writeLock().lock();
        try {
            return locks.computeIfAbsent(key, k -> new ReentrantReadWriteLock());
        } finally {
            globalLock.writeLock().unlock();
        }
    }

    public void put(String key, byte[] value) {
        ReentrantReadWriteLock keyLock = getLockForKey(key);
        keyLock.writeLock().lock();
        try {
            store.put(key, value);
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
        Collections.sort(sortedKeys);
        Map<String, ReentrantReadWriteLock> acquiredLocks = new HashMap<>();

        try {
            for (String key : sortedKeys) {
                ReentrantReadWriteLock keyLock = getLockForKey(key);
                keyLock.writeLock().lock();
                acquiredLocks.put(key, keyLock);
            }
            for (Map.Entry<String, byte[]> entry : pairs.entrySet()) {
                store.put(entry.getKey(), entry.getValue());
            }
        } finally {
            for (int i = sortedKeys.size() - 1; i >= 0; i--) {
                acquiredLocks.get(sortedKeys.get(i)).writeLock().unlock();
            }
        }
    }

    public Map<String, byte[]> multiGet(Set<String> keys) {
        Map<String, byte[]> snapshot;

        globalLock.readLock().lock();
        try {
            snapshot = new HashMap<>(store);
        } finally {
            globalLock.readLock().unlock();
        }

        Map<String, byte[]> result = new HashMap<>();
        for (String key : keys) {
            if (snapshot.containsKey(key)) {
                result.put(key, snapshot.get(key));
            }
        }
        return result;
    }

    public void preload(Map<String, byte[]> initialData) {
        globalLock.writeLock().lock();
        try {
            for (Map.Entry<String, byte[]> entry : initialData.entrySet()) {
                store.put(entry.getKey(), entry.getValue());
                locks.putIfAbsent(entry.getKey(), new ReentrantReadWriteLock());
            }
        } finally {
            globalLock.writeLock().unlock();
        }
    }
}
