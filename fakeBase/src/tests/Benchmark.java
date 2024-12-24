package tests;

import protocol.TaggedConnection;
import protocol.TaggedFrame;

import java.net.Socket;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class Benchmark {

    public static void main(String[] args) throws Exception {
        final int NUM_CLIENTS = 8; // Número de clientes simulados
        final int THREADS_PER_CLIENT = 5; // Threads internas por cliente
        final int NUM_KEYS = 1000; // Número de chaves para operações
        final int DURATION_SECONDS = 10; // Duração do teste
        final float WRITE_FRACTION = 0.5f; // Fração de operações de escrita
        final String SERVER_HOST = "localhost";
        final int SERVER_PORT = 12345;

        ExecutorService clientExecutor = Executors.newFixedThreadPool(NUM_CLIENTS);
        AtomicInteger totalOps = new AtomicInteger(0); // Contador de operações
        AtomicLong totalLatency = new AtomicLong(0); // Soma de latências

        System.out.println("Filling initial data...");
        fillInitialData(NUM_KEYS, SERVER_HOST, SERVER_PORT);
        System.out.println("Initial data filled. Starting benchmark...");

        long endTime = System.nanoTime() + TimeUnit.SECONDS.toNanos(DURATION_SECONDS);

        for (int i = 0; i < NUM_CLIENTS; i++) {
            clientExecutor.submit(() -> {
                try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT)) {
                    TaggedConnection connection = new TaggedConnection(socket);

                    ExecutorService clientThreads = Executors.newFixedThreadPool(THREADS_PER_CLIENT);

                    for (int t = 0; t < THREADS_PER_CLIENT; t++) {
                        clientThreads.submit(() -> {
                            while (System.nanoTime() < endTime) {
                                long start = System.nanoTime();
                                try {
                                    if (Math.random() < WRITE_FRACTION) {
                                        // Operação PUT
                                        String key = "key" + (totalOps.get() % NUM_KEYS);
                                        String value = "value" + totalOps.get() + "-" + Math.random();
                                        performPut(connection, key, value);
                                    } else {
                                        // Operação GET
                                        String key = "key" + (totalOps.get() % NUM_KEYS);
                                        performGet(connection, key);
                                    }
                                    totalOps.incrementAndGet();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                } finally {
                                    totalLatency.addAndGet(System.nanoTime() - start);
                                }
                            }
                        });
                    }

                    clientThreads.shutdown();
                    clientThreads.awaitTermination(DURATION_SECONDS + 1, TimeUnit.SECONDS);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }

        clientExecutor.shutdown();
        clientExecutor.awaitTermination(DURATION_SECONDS + 1, TimeUnit.SECONDS);

        long totalDuration = TimeUnit.NANOSECONDS.toMillis(TimeUnit.SECONDS.toNanos(DURATION_SECONDS));
        double throughput = totalOps.get() / (totalDuration / 1000.0); // Operações por segundo
        double avgLatency = totalLatency.get() / (1_000_000.0 * totalOps.get()); // Latência média em ms

        System.out.println("Benchmark Results:");
        System.out.println("Total Operations: " + totalOps.get());
        System.out.println("Average Latency (ms): " + avgLatency);
        System.out.println("Throughput (ops/sec): " + throughput);
    }

    private static void fillInitialData(int numKeys, String host, int port) {
        try (Socket socket = new Socket(host, port)) {
            TaggedConnection connection = new TaggedConnection(socket);

            for (int i = 0; i < numKeys; i++) {
                String key = "key" + i;
                String value = "initialValue" + i;
                performPut(connection, key, value);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void performPut(TaggedConnection connection, String key, String value) throws Exception {
        String data = key + ":" + value;
        TaggedFrame putFrame = new TaggedFrame(2, data.getBytes());
        connection.send(putFrame);
        connection.receive(); // Aguarda resposta
    }

    private static void performGet(TaggedConnection connection, String key) throws Exception {
        TaggedFrame getFrame = new TaggedFrame(3, key.getBytes());
        connection.send(getFrame);
        connection.receive(); // Aguarda resposta
    }
}
