package concurrent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;

public class ConcurrentHashMap<T,V> {
    private final Map<T, V> delegate;
    private final ReentrantLock lock = new ReentrantLock();

    public ConcurrentHashMap(Map<T, V> delegate) {
        this.delegate = delegate;
    }

    public ConcurrentHashMap() {
        this(new HashMap<>());
    }

    public V put(T key, V value) {
        lock.lock();
        try {
            return this.delegate.put(key, value);
        } finally {
            lock.unlock();
        }
    }

    public V get(T key) {
        lock.lock();
        try {
            return this.delegate.get(key);
        } finally {
            lock.unlock();
        }
    }

    public V remove(T key) {
        lock.lock();
        try {
            return this.delegate.remove(key);
        } finally {
            lock.unlock();
        }
    }

    public boolean containsKey(T key) {
        lock.lock();
        try {
            return this.delegate.containsKey(key);
        } finally {
            lock.unlock();
        }
    }

    public boolean containsValue(V value) {
        lock.lock();
        try {
            return this.delegate.containsValue(value);
        } finally {
            lock.unlock();
        }
    }

    public int size() {
        lock.lock();
        try {
            return this.delegate.size();
        } finally {
            lock.unlock();
        }
    }

    public List<V> values() {
        lock.lock();
        try {
            return List.copyOf(this.delegate.values());
        } finally {
            lock.unlock();
        }
    }

    public void forEach(BiConsumer<T, V> operation) {
        lock.lock();
        try {
            this.delegate.forEach(operation);
        } finally {
            lock.unlock();
        }
    }
}
