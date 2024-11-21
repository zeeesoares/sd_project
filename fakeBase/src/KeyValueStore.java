import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

public class KeyValueStore {
    private final Map<String, byte[]> store = new HashMap<>();
    private final ReentrantLock lock = new ReentrantLock();

    public void put(String key, byte[] value) {
        lock.lock();
        try {
            store.put(key, value);
        } finally {
            lock.unlock();
        }
    }

    public byte[] get(String key) {
        lock.lock();
        try {
            return store.get(key);
        } finally {
            lock.unlock();
        }
    }

    public void multiPut(Map<String, byte[]> pairs) {
        lock.lock();
        try {
            store.putAll(pairs);
        } finally {
            lock.unlock();
        }
    }

    public Map<String, byte[]> multiGet(Iterable<String> keys) {
        lock.lock();
        try {
            Map<String, byte[]> result = new HashMap<>();
            for (String key : keys) {
                if (store.containsKey(key)) {
                    result.put(key, store.get(key));
                }
            }
            return result;
        } finally {
            lock.unlock();
        }
    }
}
