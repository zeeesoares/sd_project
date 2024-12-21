package client;

import protocol.TaggedConnection;
import protocol.TaggedFrame;

import java.net.Socket;
import java.util.Scanner;

public class ClientInterface {
    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            Socket socket = new Socket("localhost", 12345); // Conexão com o servidor
            TaggedConnection connector = new TaggedConnection(socket);

            System.out.println("Enter username and password for registration (format: username:password): ");
            String registrationData = scanner.nextLine();
            TaggedFrame registrationFrame = new TaggedFrame(1, registrationData.getBytes()); // Tag 1 para registo
            connector.send(registrationFrame);

            TaggedFrame responseFrame = connector.receive();
            System.out.println(new String(responseFrame.getData()));

            while (true) {
                System.out.println("\nChoose an operation:");
                System.out.println("1 - PUT (Store a value)");
                System.out.println("2 - GET (Retrieve a value)");
                System.out.println("3 - MULTIGET (Retrieve multiple values)");
                System.out.println("4 - MULTIPUT (Store multiple values)");
                System.out.println("5 - EXIT");
                int choice = scanner.nextInt();
                scanner.nextLine();

                switch (choice) {
                    case 1: // PUT: Armazenar chave e valor
                        System.out.print("Enter key:value for PUT: ");
                        String putData = scanner.nextLine();
                        TaggedFrame putFrame = new TaggedFrame(2, putData.getBytes()); // Tag 2 para PUT
                        connector.send(putFrame);

                        responseFrame = connector.receive();
                        System.out.println(new String(responseFrame.getData()));
                        break;

                    case 2: // GET: Recuperar um valor por chave
                        System.out.print("Enter key for GET: ");
                        String key = scanner.nextLine();
                        TaggedFrame getFrame = new TaggedFrame(3, key.getBytes()); // Tag 3 para GET
                        connector.send(getFrame);
                        // Aguardar e exibir resposta
                        responseFrame = connector.receive();
                        System.out.println(new String(responseFrame.getData()));
                        break;

                    case 3: // MULTIGET: Recuperar múltiplos valores por chaves
                        System.out.print("Enter keys for MULTIGET (separated by ;): ");
                        String keys = scanner.nextLine().trim();
                        TaggedFrame multiGetFrame = new TaggedFrame(4, keys.getBytes()); // Tag 4 para MULTIGET
                        connector.send(multiGetFrame);

                        responseFrame = connector.receive();
                        System.out.println("MULTIGET Result: " + new String(responseFrame.getData()));

                        break;

                    case 4:
                        System.out.print("Enter keys:values for MULTIPUT (separated by ;): ");
                        String multiPutData = scanner.nextLine().trim();
                        TaggedFrame multiPutFrame = new TaggedFrame(5, multiPutData.getBytes());
                        connector.send(multiPutFrame);

                        responseFrame = connector.receive();
                        System.out.println(new String(responseFrame.getData()));
                        break;

                    case 5:
                        System.out.println("Closing connection...");
                        connector.close();
                        return;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
