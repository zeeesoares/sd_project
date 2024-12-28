package tests;

import client.ClientAPI;

import java.io.FileReader;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Properties;

public class Benchmark {

    public static void main(String[] args) throws Exception {
        // Load workload configuration
        Properties config = new Properties();
        config.load(new FileReader("workload.properties"));

        final int NUM_CLIENTS = Integer.parseInt(config.getProperty("num_clients", "8"));
        final int THREADS_PER_CLIENT = Integer.parseInt(config.getProperty("threads_per_client", "5"));
        final int NUM_KEYS = Integer.parseInt(config.getProperty("num_keys", "1000"));
        final int DURATION_SECONDS = Integer.parseInt(config.getProperty("duration_seconds", "10"));
        final float WRITE_FRACTION = Float.parseFloat(config.getProperty("write_fraction", "0.5"));
        final String WORKLOAD_DISTRIBUTION = config.getProperty("workload_distribution", "uniform");

        ExecutorService clientExecutor = Executors.newFixedThreadPool(NUM_CLIENTS);
        AtomicInteger totalOps = new AtomicInteger(0);
        AtomicLong totalLatency = new AtomicLong(0);

        long endTime = System.nanoTime() + TimeUnit.SECONDS.toNanos(DURATION_SECONDS);

        for (int i = 0; i < NUM_CLIENTS; i++) {
            clientExecutor.submit(() -> {
                try (ClientAPI clientAPI = new ClientAPI("localhost", 12345)) {
                    ExecutorService clientThreads = Executors.newFixedThreadPool(THREADS_PER_CLIENT);

                    for (int t = 0; t < THREADS_PER_CLIENT; t++) {
                        clientThreads.submit(() -> {
                            while (System.nanoTime() < endTime) {
                                long start = System.nanoTime();
                                try {
                                    String key = generateKey(NUM_KEYS, WORKLOAD_DISTRIBUTION);
                                    //System.out.println(key);
                                    if (Math.random() < WRITE_FRACTION) {
                                        // Write Operation
                                        String value = "value" + totalOps.get() + "-" + ThreadLocalRandom.current().nextInt(NUM_KEYS);
                                        clientAPI.put(key, value);
                                    } else {
                                        // Read Operation
                                        clientAPI.get(key);
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
        double throughput = totalOps.get() / (totalDuration / 1000.0);
        

        System.out.println("Benchmark Results:");
        System.out.println("Total Operations: " + totalOps.get());
        System.out.println("Throughput (ops/sec): " + throughput);
    }

    private static String generateKey(int numKeys, String distribution) {
        switch (distribution) {
            case "zipfian":
                return "key" + zipfian(numKeys);
            default:
                return "key" + ThreadLocalRandom.current().nextInt(numKeys);
        }
    }

    private static int zipfian(int numKeys) {
        double skew = 1.5; // Podemos ajustar
        double rand = Math.random();
        double sum = 0;
        double harmonicSum = 0;

        for (int i = 1; i <= numKeys; i++) {
            harmonicSum += 1.0 / Math.pow(i, skew);
        }

        for (int i = 1; i <= numKeys; i++) {
            sum += (1.0 / Math.pow(i, skew)) / harmonicSum;
            if (rand <= sum) {
                return i - 1;
            }
        }
        return numKeys - 1;
    }
}
