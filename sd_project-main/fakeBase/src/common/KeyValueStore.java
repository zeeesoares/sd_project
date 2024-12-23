package common;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class KeyValueStore {
    private final Map<String, byte[]> store = new HashMap<>();
    private final Map<String, ReentrantLock> locks = new HashMap<>(); // Locks por chave
    private final Map<String,Condition> conditions = new HashMap<>();
    private final ReentrantLock lock = new ReentrantLock(); // Lock para sincron

    public void put(String key, byte[] value) {
        ReentrantLock keyLock = getLockForKey(key);
        keyLock.lock();
        try {
            store.put(key, value);
            // Sinaliza a condição, se existir
            Condition condition = conditions.get(key);
            if (condition != null) {
                condition.signal();
            }
        } finally {
            keyLock.unlock();
        }
    }

    private ReentrantLock getLockForKey(String key) {
        lock.lock(); // Garante que o acesso ao mapa de locks é thread-safe
        try {
            return locks.computeIfAbsent(key, k -> new ReentrantLock());
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

    public byte[] getWhen(String key, String keyCond, byte[] valueCond) throws InterruptedException {
        ReentrantLock keyCondLock = getLockForKey(keyCond);
        keyCondLock.lock();
        try {
            // Obtém ou cria a condição associada à chave condicional
            Condition condition = conditions.computeIfAbsent(keyCond, k-> keyCondLock.newCondition());

            // Espera até que o valor da chave condicional seja igual a `valueCond`
            if (!Arrays.equals(store.get(keyCond), valueCond)) {
                condition.await();
                System.out.println("hhhha");
            }
        } finally {
            keyCondLock.unlock();
        }

        // Retorna o valor associado à chave
        return get(key);
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
