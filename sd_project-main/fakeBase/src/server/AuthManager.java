package server;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class AuthManager {
    private final Map<String, String> userDatabase = new HashMap<>();
    private final Lock lock = new ReentrantLock();

    /**
     * Registra um novo usuário no sistema.
     *
     * @param username Nome de usuário.
     * @param password Senha.
     * @return True se o registro foi bem-sucedido, False se o usuário já existe.
     */
    public boolean register(String username, String password) {
        lock.lock();
        try {
            return userDatabase.putIfAbsent(username, password) == null;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Autentica um usuário.
     *
     * @param username Nome de usuário.
     * @param password Senha.
     * @return True se a autenticação for bem-sucedida, False caso contrário.
     */
    public boolean authenticate(String username, String password) {
        lock.lock();
        try {
            return password.equals(userDatabase.get(username));
        } finally {
            lock.unlock();
        }
    }
}
