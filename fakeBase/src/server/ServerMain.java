package server;

import common.KeyValueStoreGrained;
import common.ThreadPool;
import protocol.TaggedConnection;
import protocol.TaggedFrame;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ServerMain {
    private static final int MAX_SESSIONS = 64; // Máximo de sessões concorrentes
    private static final int THREAD_POOL_SIZE = 32; // Tamanho do pool de threads
    private static final AuthManager authManager = new AuthManager();
    private static final SessionManager sessionManager = new SessionManager(MAX_SESSIONS);
    private static final KeyValueStoreGrained keyValueStore = new KeyValueStoreGrained();

    private static final ThreadPool threadPool = new ThreadPool(THREAD_POOL_SIZE);

    public static void main(String[] args) throws IOException {
        ServerConnector connector = new ServerConnector(12345);
        System.out.println("Server is running on port 12345");

        while (true) {
            TaggedConnection conn = connector.accept();

            try {
                new Thread(() -> {
                    try {
                        sessionManager.acquireSession();
                        handleClient(conn);
                    } catch (IOException | InterruptedException e) {
                        System.out.println("Cliente desconectado!");
                    } finally {
                        try {
                            conn.close();
                        } catch (IOException e) {
                            System.out.println("Error closing connection: " + e.getMessage());
                        }
                        sessionManager.releaseSession();
                    }
                }).start();

            } finally {
            }
        }
    }

    private static void handleClient(TaggedConnection conn) throws IOException {
        while (true) {
            TaggedFrame frame = conn.receive();

            threadPool.submit(() -> {
                try {
                    handleRequest(conn, frame);
                } catch (IOException e) {
                    System.out.println("Error handling request: " + e.getMessage());
                }
            });
        }
    }

    private static void handleRequest(TaggedConnection conn, TaggedFrame frame) throws IOException {
        int tag = frame.getTag();
        byte[] data = frame.getData();

        switch (tag) {
            case 1: // Registo de utilizador
                handleAuth(conn, data);
                break;
            case 2: // Operação PUT
                handlePut(conn, data);
                break;
            case 3: // Operação GET
                handleGet(conn, data);
                break;
            case 4: // MultiPut
                handleMultiPut(conn, data);
                break;
            case 5: // MultiGet
                handleMultiGet(conn, data);
                break;
            case 6:// GetWhen
                handleGetWhen(conn,data);
            default:
                conn.send(new TaggedFrame(0, "Unknown command".getBytes()));
                break;
        }
    }

    private static void handleAuth(TaggedConnection conn, byte[] data) throws IOException {
        String[] credentials = new String(data).split(":");
        if (credentials.length != 2) {
            conn.send(new TaggedFrame(1, "Invalid format. Use username:password.".getBytes()));
            return;
        }

        String username = credentials[0];
        String password = credentials[1];

        if (authManager.authenticate(username, password)) {
            conn.send(new TaggedFrame(1, "Authentication successful".getBytes()));
        } else if (authManager.register(username, password)) {
            conn.send(new TaggedFrame(1, "Registration successful".getBytes()));
        } else {
            conn.send(new TaggedFrame(1, "Authentication or registration failed".getBytes()));
        }
    }

    private static void handlePut(TaggedConnection conn, byte[] data) throws IOException {
        String[] keyValue = new String(data).split(":");
        if (keyValue.length != 2) {
            conn.send(new TaggedFrame(2, "Invalid PUT format. Use key:value.".getBytes()));
            return;
        }

        String key = keyValue[0];
        String value = keyValue[1];

        keyValueStore.put(key, value.getBytes());
        conn.send(new TaggedFrame(2, "Put successful".getBytes()));
    }

    private static void handleGet(TaggedConnection conn, byte[] data) throws IOException {
        String key = new String(data);

        byte[] value = keyValueStore.get(key);
        if (value != null) {
            conn.send(new TaggedFrame(3, value));
        } else {
            conn.send(new TaggedFrame(3, "Key not found".getBytes()));
        }
    }

    private static void handleMultiPut(TaggedConnection conn, byte[] data) throws IOException {
        String[] keyValuePairs = new String(data).split(";");
        Map<String, byte[]> pairs = new HashMap<>();

        for (String pair : keyValuePairs) {
            String[] keyValue = pair.split(":");
            if (keyValue.length != 2) {
                conn.send(new TaggedFrame(4, "Invalid MultiPut format. Use key1:value1;key2:value2.".getBytes()));
                return;
            }
            pairs.put(keyValue[0], keyValue[1].getBytes());
        }

        keyValueStore.multiPut(pairs);
        conn.send(new TaggedFrame(4, "MultiPut successful".getBytes()));
    }

    private static void handleMultiGet(TaggedConnection conn, byte[] data) throws IOException {
        String[] keysArray = new String(data).split(";");
        Set<String> keys = new HashSet<>(Arrays.asList(keysArray));

        Map<String, byte[]> result = keyValueStore.multiGet(keys);

        StringBuilder responseBuilder = new StringBuilder();
        for (Map.Entry<String, byte[]> entry : result.entrySet()) {
            responseBuilder.append(entry.getKey())
                    .append(":")
                    .append(new String(entry.getValue()))
                    .append(";");
        }

        conn.send(new TaggedFrame(5, responseBuilder.toString().getBytes()));
    }



    private static void handleGetWhen(TaggedConnection conn, byte[] data) throws IOException {
        String[] params = new String(data).split(":");
        if (params.length == 3) {
            String key = params[0];
            String keyCond = params[1];
            byte[] valueCond = params[2].getBytes();

            try {
                byte[] value = keyValueStore.getWhen(key, keyCond, valueCond);
                if(value!=null){
                    conn.send(new TaggedFrame(6, value));
                }else{
                    conn.send(new TaggedFrame(6, "Key not found".getBytes()));
                }
            } catch (InterruptedException e) {
                conn.send(new TaggedFrame(6, "Operation interrupted".getBytes()));
            }
        } else {
            conn.send(new TaggedFrame(6, "Invalid GETWHEN format".getBytes()));
        }
    }
}
