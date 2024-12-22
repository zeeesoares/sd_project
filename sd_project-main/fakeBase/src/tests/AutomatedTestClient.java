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
            // Criar um pool de threads com 20 threads para enviar requisições simultâneas
            ExecutorService executor = Executors.newFixedThreadPool(20);

            // Conectar ao servidor
            Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
            TaggedConnection connector = new TaggedConnection(socket);

            // Registro automático de usuários
            registerUser(connector, "user123", "password123");

            // Automatizar operações PUT e GET
            for (int i = 0; i < NUM_REQUESTS; i++) {
                int finalI = i;
                executor.submit((Callable<Void>) () -> {
                    try {
                        if (finalI % 2 == 0) {
                            // Enviar PUT com chave/valor gerados
                            String key = "key" + finalI;
                            String value = "value" + finalI;
                            performPut(connector, key, value);
                        } else {
                            // Enviar GET para chave gerada
                            String key = "key" + finalI;
                            performGet(connector, key);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return null;
                });
            }

            executor.shutdown();
            while (!executor.isTerminated()) {
                // Aguarda até que todas as requisições terminem
            }

            System.out.println("Automated test finished.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Função para registar um novo usuário
    private static void registerUser(TaggedConnection connector, String username, String password) throws Exception {
        String regData = username + ":" + password;
        TaggedFrame registrationFrame = new TaggedFrame(1, regData.getBytes());
        connector.send(registrationFrame);

        TaggedFrame responseFrame = connector.receive();
        System.out.println("Registration Response: " + new String(responseFrame.getData()));
    }

    // Função para realizar uma operação PUT
    private static void performPut(TaggedConnection connector, String key, String value) throws Exception {
        String data = key + ":" + value;
        TaggedFrame putFrame = new TaggedFrame(2, data.getBytes());
        connector.send(putFrame);

        TaggedFrame responseFrame = connector.receive();
        System.out.println("PUT Response for " + key + ": " + new String(responseFrame.getData()));
    }

    // Função para realizar uma operação GET
    private static void performGet(TaggedConnection connector, String key) throws Exception {
        TaggedFrame getFrame = new TaggedFrame(3, key.getBytes());
        connector.send(getFrame);

        TaggedFrame responseFrame = connector.receive();
        System.out.println("GET Response for " + key + ": " + new String(responseFrame.getData()));
    }
}
