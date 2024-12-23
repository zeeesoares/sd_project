package tests;

import protocol.TaggedConnection;
import protocol.TaggedFrame;

import java.net.Socket;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AutomatedTestClient {

    private static final int NUM_REQUESTS = 1000; // Número de requisições automáticas
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 12345;

    public static void main(String[] args) {
        try {
            ExecutorService executor = Executors.newFixedThreadPool(20);

            Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
            TaggedConnection connector = new TaggedConnection(socket);

            registerUser(connector, "user123", "password123");

            for (int i = 0; i < NUM_REQUESTS; i++) {
                int finalI = i;
                executor.submit((Callable<Void>) () -> {
                    try {
                        if (finalI % 2 != 0) {
                            String key = "key" + finalI;
                            String value = "value" + finalI;
                            performPut(connector, key, value);
                        } else {
                            String key = "key" + finalI;
                            performGet(connector, key);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return null;
                });
            }

            for (int i = 0; i < NUM_REQUESTS; i++) {
                int finalI = i;
                executor.submit((Callable<Void>) () -> {
                    try {
                        String key = "key" + finalI;
                        performGet(connector, key);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return null;
                });
            }


            executor.shutdown();
            while (!executor.isTerminated()) {
            }

            System.out.println("Automated test finished.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void registerUser(TaggedConnection connector, String username, String password) throws Exception {
        String regData = username + ":" + password;
        TaggedFrame registrationFrame = new TaggedFrame(1, regData.getBytes());
        connector.send(registrationFrame);

        TaggedFrame responseFrame = connector.receive();
        System.out.println("Registration Response: " + new String(responseFrame.getData()));
    }

    private static void performPut(TaggedConnection connector, String key, String value) throws Exception {
        String data = key + ":" + value;
        TaggedFrame putFrame = new TaggedFrame(2, data.getBytes());
        connector.send(putFrame);

        TaggedFrame responseFrame = connector.receive();
        System.out.println("PUT Response for " + key + ": " + new String(responseFrame.getData()));
    }

    private static void performGet(TaggedConnection connector, String key) throws Exception {
        TaggedFrame getFrame = new TaggedFrame(3, key.getBytes());
        connector.send(getFrame);

        TaggedFrame responseFrame = connector.receive();
        System.out.println("GET Response for " + key + ": " + new String(responseFrame.getData()));
    }
}
