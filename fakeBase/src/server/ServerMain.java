package server;

import common.KeyValueStore;
import protocol.TaggedConnection;
import protocol.TaggedFrame;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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


            int tag = frame.getTag(); // Identifica o tipo de operação
            byte[] data = frame.getData(); // Dados associados à operação

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
                case 4: // MultiPut
                    handleMultiPut(conn, data);
                    break;
                case 5: // MultiGet
                    handleMultiGet(conn, data);
                    break;
                default:
                    conn.send(new TaggedFrame(0, "Unknown command".getBytes()));
                    break;
            }
        }
    }

    // Handle de Register
    private static void handleRegister(TaggedConnection conn, byte[] data) throws IOException {
        String[] credentials = new String(data).split(":");
        String username = credentials[0];
        String password = credentials[1];

        if (authManager.register(username, password)) {
            conn.send(new TaggedFrame(1, "Registration successful".getBytes()));
        } else {
            conn.send(new TaggedFrame(1, "Registration failed".getBytes()));
        }
    }

    // Handle de PUT
    private static void handlePut(TaggedConnection conn, byte[] data) throws IOException {
        String[] keyValue = new String(data).split(":");
        String key = keyValue[0];
        String value = keyValue[1];

        keyValueStore.put(key, value.getBytes());

        conn.send(new TaggedFrame(2, "Put successful".getBytes()));
    }

    // Handle de GET
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
            String key = keyValue[0];
            String value = keyValue[1];
            pairs.put(key, value.getBytes());
        }
    
        for (Map.Entry<String, byte[]> entry : pairs.entrySet()) {
            keyValueStore.put(entry.getKey(), entry.getValue());
        }
    
        conn.send(new TaggedFrame(4, "MultiPut successful".getBytes()));
    }

    private static void handleMultiGet(TaggedConnection conn, byte[] data) throws IOException {
        String[] keys = new String(data).split(";");
        Map<String, byte[]> result = new HashMap<>();
    
        for (String key : keys) {
            byte[] value = keyValueStore.get(key);
            if (value != null) {
                result.put(key, value);
            }
        }
    
        StringBuilder responseBuilder = new StringBuilder();
        for (Map.Entry<String, byte[]> entry : result.entrySet()) {
            responseBuilder.append(entry.getKey())
                    .append(":")
                    .append(new String(entry.getValue()))
                    .append(";");
        }
    
        conn.send(new TaggedFrame(5, responseBuilder.toString().getBytes()));
    }
    
    

}



//private static void handleClient(Socket clientSocket) {
//    try (Connector connector = new Connector(clientSocket)) {
//        sessionManager.acquireSession();
//
//        try {
//            byte[] usernameBytes = connector.receive();
//            String username = new String(usernameBytes);
//
//            byte[] passwordBytes = connector.receive();
//            String password = new String(passwordBytes);
//
//            if (authManager.authenticate(username, password)) {
//                System.out.println("Authentication successful for user: " + username);
//                connector.send("Welcome!".getBytes());
//            } else {
//                System.out.println("Authentication failed for user: " + username);
//                connector.send("Authentication failed".getBytes());
//            }
//        } finally {
//            // Liberar a sessão após o término
//            //sessionManager.releaseSession();
//        }
//    } catch (Exception e) {
//        e.printStackTrace();
//    }
//}
