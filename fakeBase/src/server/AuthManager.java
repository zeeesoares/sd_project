package server;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class AuthManager {
    private final Map<String, String> userDatabase = new HashMap<>();
    private final Lock lock = new ReentrantLock();

    public boolean register(String username, String password) {
        lock.lock();
        try {
            if (userDatabase.containsKey(username)) {
                return false;
            }
            userDatabase.put(username, password);
            return true;
        } finally {
            lock.unlock();
        }
    }

    public boolean authenticate(String username, String password) {
        lock.lock();
        try {
            String storedPassword = userDatabase.get(username);
            return storedPassword != null && storedPassword.equals(password);
        } finally {
            lock.unlock();
        }
    }
}
