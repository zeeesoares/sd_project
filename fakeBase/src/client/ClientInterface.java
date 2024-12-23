package client;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.Scanner;

public class ClientInterface {
    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            ClientAPI clientAPI = new ClientAPI("localhost", 12345);

            System.out.println("Enter username and password for registration (format: username:password): ");
            String[] registrationData = scanner.nextLine().split(":");
            String registrationResponse = clientAPI.register(registrationData[0], registrationData[1]);
            System.out.println("Registration Response: " + registrationResponse);

            // Loop principal para operações
            while (true) {
                System.out.println("\nChoose an operation:");
                System.out.println("1 - PUT (Store a value)");
                System.out.println("2 - GET (Retrieve a value)");
                System.out.println("3 - MULTIPUT (Store multiple key-value pairs)");
                System.out.println("4 - MULTIGET (Retrieve multiple values by keys)");
                System.out.println("5 - EXIT");
                int choice = scanner.nextInt();
                scanner.nextLine();

                switch (choice) {
                    case 1: // PUT: Armazenar chave e valor
                        System.out.print("Enter key:value for PUT: ");
                        String[] putData = scanner.nextLine().split(":");
                        String putResponse = clientAPI.put(putData[0], putData[1]);
                        System.out.println("PUT Response: " + putResponse);
                        break;

                    case 2: // GET: Recuperar valor pela chave
                        System.out.print("Enter key for GET: ");
                        String key = scanner.nextLine();
                        String getResponse = clientAPI.get(key);
                        System.out.println("GET Response: " + getResponse);
                        break;

                    case 3: // MULTIPUT: Armazenar múltiplos pares chave-valor
                        System.out.println("Enter key:value pairs for MULTIPUT (format: key1:value1;key2:value2): ");
                        String[] multiPutInput = scanner.nextLine().split(";");
                        Map<String, String> multiPutMap = new HashMap<>();
                        for (String pair : multiPutInput) {
                            String[] keyValue = pair.split(":");
                            multiPutMap.put(keyValue[0], keyValue[1]);
                        }
                        String multiPutResponse = clientAPI.multiPut(multiPutMap);
                        System.out.println("MULTIPUT Response: " + multiPutResponse);
                        break;

                    case 4: // MULTIGET: Recuperar valores por múltiplas chaves
                        System.out.println("Enter keys for MULTIGET (format: key1;key2;key3): ");
                        String[] multiGetKeys = scanner.nextLine().split(";");
                        Set<String> keySet = Set.of(multiGetKeys);
                        Map<String, String> multiGetResponse = clientAPI.multiGet(keySet);
                        System.out.println("MULTIGET Response:");
                        multiGetResponse.forEach((k, v) -> System.out.println(k + ": " + v));
                        break;

                    case 5: // EXIT
                        clientAPI.close();
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
