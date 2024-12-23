package tests;

import client.ClientAPI;
import server.ServerMain;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class Benchmark {

    public static void main(String[] args) throws Exception {
        // Configurações do benchmark
        final int clientThreads = 10; // Número de threads do cliente
        final int durationSeconds = 10; // Duração do teste em segundos
        final float writeFraction = 0.5f; // Fração de operações de escrita
        final String serverHost = "localhost";
        final int serverPort = 12345;

        ExecutorService serverExecutor = Executors.newSingleThreadExecutor();
        serverExecutor.submit(() -> {
            try {
                ServerMain.main(new String[0]); // Inicializar servidor
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        Thread.sleep(2000);

        ExecutorService clientExecutor = Executors.newFixedThreadPool(clientThreads);
        AtomicInteger totalOps = new AtomicInteger(0);
        AtomicLong totalLatency = new AtomicLong(0);

        long endTime = System.currentTimeMillis() + (durationSeconds * 1000);

        for (int i = 0; i < clientThreads; i++) {
            clientExecutor.submit(() -> {
                try (ClientAPI clientAPI = new ClientAPI(serverHost, serverPort)) {
                    while (System.currentTimeMillis() < endTime) {
                        long start = System.nanoTime();
                        try {
                            if (Math.random() < writeFraction) {
                                // Operação PUT
                                String key = "key-" + Thread.currentThread().getId() + "-" + Math.random();
                                String value = "value-" + Math.random();
                                clientAPI.put(key, value);
                            } else {
                                // Operação GET
                                String key = "key-" + Thread.currentThread().getId();
                                clientAPI.get(key);
                            }
                            totalOps.incrementAndGet();
                        } finally {
                            totalLatency.addAndGet(System.nanoTime() - start);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }

        clientExecutor.shutdown();
        while (!clientExecutor.isTerminated()) {
            Thread.sleep(100);
        }

        System.out.println("Benchmark Results:");
        System.out.println("Total Operations: " + totalOps.get());
        System.out.println("Average Latency (ms): " + (totalLatency.get() / 1_000_000.0 / totalOps.get()));

        // Parar o servidor
        serverExecutor.shutdownNow();
    }
}
