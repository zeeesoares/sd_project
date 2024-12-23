package tests;

import protocol.Demultiplexer;
import protocol.TaggedConnection;
import protocol.TaggedFrame;

import java.net.Socket;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AutomatedTestClientMultiThreaded {

    private static final int NUM_REQUESTS = 1000; // Número de requisições automáticas
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 12345;
    private static final int NUM_PUT_THREADS = 500;  // Número de threads para operações PUT
    private static final int NUM_GET_THREADS = 500;  // Número de threads para operações GET

    public static void main(String[] args) {
        try {
            ExecutorService executor = Executors.newFixedThreadPool(20);

            Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
            TaggedConnection connector = new TaggedConnection(socket);

            Demultiplexer demux = new Demultiplexer(connector);
            demux.start();

            registerUser(connector, "user123", "password123");

            for (int i = 0; i < NUM_PUT_THREADS; i++) {
                int finalI = i;
                executor.submit((Callable<Void>) () -> {
                    try {
                        // Enviar PUT com chave/valor gerados
                        String key = "key" + finalI;
                        String value = "value" + finalI;
                        performPut(demux, key, value);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return null;
                });
            }

            for (int i = 0; i < NUM_GET_THREADS; i++) {
                int finalI = i;
                executor.submit((Callable<Void>) () -> {
                    try {
                        // Enviar GET para chave gerada
                        String key = "key" + finalI;
                        performGet(demux, key);
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

    private static void performPut(Demultiplexer demux, String key, String value) throws Exception {
        String data = key + ":" + value;
        TaggedFrame putFrame = new TaggedFrame(2, data.getBytes());
        demux.send(putFrame);  // Envia a requisição PUT para o Demultiplexer

        byte[] response = demux.receive(2);
        System.out.println("PUT Response for " + key + ": " + new String(response));
    }

    private static void performGet(Demultiplexer demux, String key) throws Exception {
        TaggedFrame getFrame = new TaggedFrame(3, key.getBytes());
        demux.send(getFrame);  // Envia a requisição GET para o Demultiplexer

        byte[] responseFrame = demux.receive(3);
        System.out.println("GET Response for " + key + ": " + new String(responseFrame));
    }
}
