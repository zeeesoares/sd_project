package server;

import common.KeyValueStore;
import protocol.TaggedConnection;
import protocol.TaggedFrame;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

//Atualizei o resto

public class ServerMain {
    private static final int MAX_SESSIONS = 5; // Máximo de sessões concorrentes
    private static final int MAX_THREADS = 10;
    private static final ExecutorService threadPool = Executors.newFixedThreadPool(MAX_THREADS);
    private static final AuthManager authManager = new AuthManager();
    private static final SessionManager sessionManager = new SessionManager(MAX_SESSIONS);
    private static final KeyValueStore keyValueStore = new KeyValueStore();

    public static void main(String[] args) throws IOException, InterruptedException {
        ServerConnector connector = new ServerConnector(12345);
        System.out.println("Server is running on port 12345");

        while (true) {
            TaggedConnection conn = connector.accept();

            threadPool.submit(() -> {
                try {
                    sessionManager.acquireSession();
                    handleClient(conn);
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    sessionManager.releaseSession();
                }
            });
        }
    }

    private static void handleClient(TaggedConnection conn) throws IOException {
        while (true) {
            TaggedFrame frame = conn.receive();

            if (frame == null) {
                break;
            }

            int tag = frame.getTag();
            byte[] data = frame.getData();

            switch (tag) {
                case 1: // Registo de utilizador
                    handleRegister(conn, data);
                    break;
                case 2: // Operação PUT
                    handlePut(conn, data);
                    break;
                case 3: // Operação GET
                    handleGet(conn, data);
                    break;
                case 4:// Operação Multiget
                    handleMultiGet(conn, data);
                    break;
                case 5: // Operação Multiput
                    handleMultiPut(conn, data);
                    break;
                case 6: // Operação getWhen
                    handleGetWhen(conn, data);
                    break;
                default:
                    conn.send(new TaggedFrame(0, "Unknown command".getBytes())); // Resposta de comando desconhecido
                    break;
            }
        }
    }

    private static void handlePut(TaggedConnection conn, byte[] data) throws IOException {
        String[] keyValue = new String(data).split(":");
        if (keyValue.length == 2) {
            String key = keyValue[0];
            String value = keyValue[1];

            keyValueStore.put(key, value.getBytes());
            conn.send(new TaggedFrame(0, "Put successful".getBytes()));
        } else {
            conn.send(new TaggedFrame(0, "Invalid PUT format".getBytes()));
        }
    }

    private static void handleGet(TaggedConnection conn, byte[] data) throws IOException {
        String key = new String(data);

        byte[] value = keyValueStore.get(key);
        if (value != null) {
            conn.send(new TaggedFrame(0, value));
        } else {
            conn.send(new TaggedFrame(0, "Key not found".getBytes()));
        }
    }

    private static void handleMultiGet(TaggedConnection conn, byte[] data) throws IOException {
        String[] keysArray = new String(data).split(";");
        Set<String> keys = new HashSet<>(Arrays.asList(keysArray));

        Map<String, byte[]> results = keyValueStore.multiGet(keys);
        if (results.isEmpty()) {
            conn.send(new TaggedFrame(0, "No keys found".getBytes()));
            return;
        }

        StringBuilder serializedResult = new StringBuilder();
        for (Map.Entry<String, byte[]> entry : results.entrySet()) {
            serializedResult.append(entry.getKey())
                    .append(":")
                    .append(new String(entry.getValue()))
                    .append(";");
        }

        conn.send(new TaggedFrame(0, serializedResult.toString().getBytes()));
    }

    private static void handleMultiPut(TaggedConnection conn, byte[] data) throws IOException {
        String[] pairs = new String(data).split(";");
        Map<String, byte[]> keyValuePairs = new HashMap<>();

        for (String pair : pairs) {
            String[] keyValue = pair.split(":");
            if (keyValue.length == 2) {
                keyValuePairs.put(keyValue[0], keyValue[1].getBytes());
            } else {
                conn.send(new TaggedFrame(0, "Invalid MULTIPUT format".getBytes()));
                return;
            }
        }

        keyValueStore.multiPut(keyValuePairs);

        conn.send(new TaggedFrame(0, "multiPut successful".getBytes()));
    }

    private static void handleGetWhen(TaggedConnection conn, byte[] data) throws IOException {
        String[] params = new String(data).split(":");
        if (params.length == 3) {
            String key = params[0];
            String keyCond = params[1];
            byte[] valueCond = params[2].getBytes();

            try {
                byte[] value = keyValueStore.getWhen(key, keyCond, valueCond);
                conn.send(new TaggedFrame(0, value));
            } catch (InterruptedException e) {
                conn.send(new TaggedFrame(0, "Operation interrupted".getBytes()));
            }
        } else {
            conn.send(new TaggedFrame(0, "Invalid GETWHEN format".getBytes()));
        }
    }

    private static void handleRegister(TaggedConnection conn, byte[] data) throws IOException {
        String[] credentials = new String(data).split(":");
        if (credentials.length == 2) {
            String username = credentials[0];
            String password = credentials[1];

            if (authManager.register(username, password)) {
                conn.send(new TaggedFrame(0, "Registration successful".getBytes()));
            } else {
                conn.send(new TaggedFrame(0, "Registration failed".getBytes()));
            }
        } else {
            conn.send(new TaggedFrame(0, "Invalid REGISTER format".getBytes()));
        }
    }
}
