package common;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

// Eu atualizei o código para ter o getWhen, locks por chave, e cache LRU

public class KeyValueStore {

    private final Map<String, byte[]> store = new ConcurrentHashMap<>();

    private final Map<String, ReentrantLock> locks = new ConcurrentHashMap<>();
    private final Map<String, Condition> conditions = new ConcurrentHashMap<>();

    private final int cacheSize = 100;
    private final LinkedHashMap<String, byte[]> cache = new LinkedHashMap<>(cacheSize, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, byte[]> eldest) {
            return size() > cacheSize;
        }
    };

    public void put(String key, byte[] value) {
        getLock(key).lock();
        try {
            store.put(key, value);
            conditions.computeIfAbsent(key, k -> getLock(k).newCondition()).signalAll();
            synchronized (cache) {
                cache.put(key, value);
            }
        } finally {
            getLock(key).unlock();
        }
    }

    public byte[] get(String key) {
        getLock(key).lock();
        try {
            synchronized (cache) {
                if (cache.containsKey(key)) {
                    return cache.get(key);
                }
            }
            return store.get(key);
        } finally {
            getLock(key).unlock();
        }
    }

    public void multiPut(Map<String, byte[]> pairs) {
        for (String key : pairs.keySet()) {
            getLock(key).lock();
        }
        try {
            for (Map.Entry<String, byte[]> entry : pairs.entrySet()) {
                store.put(entry.getKey(), entry.getValue());
                synchronized (cache) {
                    cache.put(entry.getKey(), entry.getValue());
                }
                conditions.computeIfAbsent(entry.getKey(), k -> getLock(k).newCondition()).signalAll();
            }
        } finally {
            for (String key : pairs.keySet()) {
                getLock(key).unlock();
            }
        }
    }

    public Map<String, byte[]> multiGet(Iterable<String> keys) {
        Map<String, byte[]> result = new HashMap<>();
        for (String key : keys) {
            getLock(key).lock();
        }
        try {
            for (String key : keys) {
                byte[] value = store.get(key);
                if (value != null) {
                    result.put(key, value);
                }
            }
        } finally {
            for (String key : keys) {
                getLock(key).unlock();
            }
        }
        return result;
    }

    public byte[] getWhen(String key, String keyCond, byte[] valueCond) throws InterruptedException {
        ReentrantLock condLock = getLock(keyCond);
        condLock.lock();
        try {
            Condition condition = conditions.computeIfAbsent(keyCond, k -> condLock.newCondition());
            while (!Arrays.equals(store.get(keyCond), valueCond)) {
                condition.await();
            }
        } finally {
            condLock.unlock();
        }

        return get(key);
    }

    private ReentrantLock getLock(String key) {
        return locks.computeIfAbsent(key, k -> new ReentrantLock());
    }
}