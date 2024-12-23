package tests;

import client.ClientAPI;
import server.ServerMain;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class Benchmark {

    public static void main(String[] args) throws Exception {
        final int clientThreads = 10; // Número de threads do cliente
        final int durationSeconds = 10; // Duração do teste em segundos
        final float writeFraction = 0.5f; // Fração de operações de escrita
        final String serverHost = "localhost";
        final int serverPort = 12345;

        ExecutorService serverExecutor = Executors.newSingleThreadExecutor();
        serverExecutor.submit(() -> {
            try {
                ServerMain.main(new String[0]); 
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        Thread.sleep(2000);

        ExecutorService clientExecutor = Executors.newFixedThreadPool(clientThreads);
        AtomicInteger totalOps = new AtomicInteger(0); // Contador de operações realizadas
        AtomicLong totalLatency = new AtomicLong(0); // Soma das latências das operações

        long endTime = System.currentTimeMillis() + (durationSeconds * 1000); // Tempo de término do benchmark

        for (int i = 0; i < clientThreads; i++) {
            clientExecutor.submit(() -> {
                try (ClientAPI clientAPI = new ClientAPI(serverHost, serverPort)) {
                    while (System.currentTimeMillis() < endTime) {
                        long start = System.nanoTime(); // Marca o início da operação
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
                            totalOps.incrementAndGet(); // Incrementa o contador de operações
                        } finally {
                            // Adiciona a latência da operação ao total
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
            Thread.sleep(100); // Aguarda até que todos os clientes terminem suas operações
        }

        long totalTimeMillis = (System.currentTimeMillis() - (endTime - durationSeconds * 1000)); // tempo total em milissegundos
        double throughput = totalOps.get() / (totalTimeMillis / 1000.0); // Operações por segundo

        System.out.println("Benchmark Results:");
        System.out.println("Total Operations: " + totalOps.get());
        System.out.println("Average Latency (ms): " + (totalLatency.get() / 1_000_000.0 / totalOps.get()));
        System.out.println("Throughput (ops/sec): " + throughput);

        serverExecutor.shutdownNow();
    }
}
