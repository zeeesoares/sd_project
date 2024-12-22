package client;

import protocol.TaggedFrame;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

// Atualizado com o resto

public class ClientInterface {
    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.println("Enter server address and port (e.g., localhost 12345): ");
            String[] serverInfo = scanner.nextLine().split(" ");
            String host = serverInfo[0];
            int port = Integer.parseInt(serverInfo[1]);

            ClientAPI client = new ClientAPI(host, port);

            while (true) {
                System.out.println("\nChoose an operation:");
                System.out.println("1 - PUT");
                System.out.println("2 - GET");
                System.out.println("3 - MULTIPUT");
                System.out.println("4 - MULTIGET");
                System.out.println("5 - GETWHEN");
                System.out.println("6 - EXIT");
                int choice = scanner.nextInt();
                scanner.nextLine();

                switch (choice) {
                    case 1: // PUT
                        System.out.print("Enter key:value for PUT: ");
                        String[] putData = scanner.nextLine().split(":");
                        client.put(putData[0], putData[1].getBytes());
                        break;

                    case 2: // GET
                        System.out.print("Enter key for GET: ");
                        String getKey = scanner.nextLine();
                        System.out.println("Value: " + new String(client.get(getKey)));
                        break;

                    case 3: // MULTIPUT
                        System.out.print("Enter keys:values for MULTIPUT (separated with ';'): ");
                        String[] multiPutData = scanner.nextLine().split(";");
                        Map<String, byte[]> pairs = new HashMap<>();
                        for (String pair : multiPutData) {
                            String[] keyValue = pair.split(":");
                            pairs.put(keyValue[0], keyValue[1].getBytes());
                        }
                        client.multiPut(pairs);
                        break;

                    case 4: // MULTIGET
                        System.out.print("Enter keys for MULTIGET (separated with ';'): ");
                        String[] multiGetKeys = scanner.nextLine().split(";");
                        System.out.println("Values: " + client.multiGet(Set.of(multiGetKeys)));
                        break;

                    case 5: // GETWHEN
                        System.out.print("Enter key:keyCond:valueCond for GETWHEN: ");
                        String[] getWhenData = scanner.nextLine().split(":");
                        byte[] result = client.getWhen(getWhenData[0], getWhenData[1], getWhenData[2].getBytes());
                        System.out.println("Value: " + new String(result));
                        break;

                    case 6: // EXIT
                        client.close();
                        System.out.println("Goodbye!");
                        return;

                    default:
                        System.out.println("Invalid option. Try again.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}