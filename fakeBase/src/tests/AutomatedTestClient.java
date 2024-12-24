package tests;

import protocol.TaggedConnection;
import protocol.TaggedFrame;

import java.net.Socket;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class AutomatedTestClient {

    private static final int NUM_CLIENTS = 10; // Número de clientes simulados
    private static final int THREADS_PER_CLIENT = 5; // Threads internas por cliente
    private static final int NUM_KEYS = 1000; // Número de chaves para preenchimento inicial
    private static final int TEST_DURATION_SECONDS = 10; // Duração máxima do teste em segundos
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 12345;

    public static void main(String[] args) {
        ExecutorService clientExecutor = Executors.newFixedThreadPool(NUM_CLIENTS);
        AtomicInteger totalOperations = new AtomicInteger(0); 

        try {
        
            System.out.println("Filling server with initial keys...");
            fillInitialData(NUM_KEYS);
            System.out.println("Initial data filled. Starting test...");

            long startTime = System.nanoTime(); 
            long testDurationNanos = TimeUnit.SECONDS.toNanos(TEST_DURATION_SECONDS); 

            for (int i = 0; i < NUM_CLIENTS; i++) {
                clientExecutor.submit(() -> {
                    try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT)) {
                        TaggedConnection connector = new TaggedConnection(socket);

                        String username = "user" + Thread.currentThread().getId();
                        String password = "password" + Thread.currentThread().getId();
                        registerUser(connector, username, password);

                        ExecutorService clientThreads = Executors.newFixedThreadPool(THREADS_PER_CLIENT);

                        for (int t = 0; t < THREADS_PER_CLIENT; t++) {
                            clientThreads.submit(() -> {
                                while (System.nanoTime() - startTime < testDurationNanos) {
                                    try {
                                        int requestId = totalOperations.get(); 
                                        boolean isPutOperation = requestId % 2 == 0;

                                        if (isPutOperation) {
                                            String key = "key" + (requestId % NUM_KEYS);
                                            performPut(connector, key, "value" + requestId + "-" + Math.random(), totalOperations);
                                        } else {
                                            String key = "key" + (requestId % NUM_KEYS);
                                            performGet(connector, key, totalOperations);
                                        }
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                        }

                        clientThreads.shutdown();
                        clientThreads.awaitTermination(TEST_DURATION_SECONDS, TimeUnit.SECONDS);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }

            clientExecutor.shutdown();
            clientExecutor.awaitTermination(TEST_DURATION_SECONDS + 1, TimeUnit.SECONDS);

            long endTime = System.nanoTime(); // Tempo final
            double durationInSeconds = (endTime - startTime) / 1_000_000_000.0; // Duração em segundos

            int totalOps = totalOperations.get();
            double opsPerSecond = totalOps / durationInSeconds;

            System.out.println("Total Operations: " + totalOps);
            System.out.println("Duration: " + durationInSeconds + " seconds");
            System.out.println("Operations Per Second: " + opsPerSecond + " ops/s");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void fillInitialData(int numKeys) throws Exception {
        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT)) {
            TaggedConnection connector = new TaggedConnection(socket);

            for (int i = 0; i < numKeys; i++) {
                String key = "key" + i;
                String value = "initialValue" + i;
                performPut(connector, key, value, null);
            }
        }
    }

    private static void registerUser(TaggedConnection connector, String username, String password) throws Exception {
        String regData = username + ":" + password;
        TaggedFrame registrationFrame = new TaggedFrame(1, regData.getBytes());
        connector.send(registrationFrame);

        TaggedFrame responseFrame = connector.receive();
        System.out.println("Registration Response: " + new String(responseFrame.getData()));
    }

    private static void performPut(TaggedConnection connector, String key, String value, AtomicInteger counter) throws Exception {
        String data = key + ":" + value;
        TaggedFrame putFrame = new TaggedFrame(2, data.getBytes());
        connector.send(putFrame);

        TaggedFrame responseFrame = connector.receive();
        if (counter != null) {
            counter.incrementAndGet(); // Incrementa o contador quando a resposta é recebida
        }
    }

    private static void performGet(TaggedConnection connector, String key, AtomicInteger counter) throws Exception {
        TaggedFrame getFrame = new TaggedFrame(3, key.getBytes());
        connector.send(getFrame);

        TaggedFrame responseFrame = connector.receive();
        if (counter != null) {
            counter.incrementAndGet(); // Incrementa o contador quando a resposta é recebida
        }
    }
}
