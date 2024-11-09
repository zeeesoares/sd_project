package client;

import common.ByteArrayWrapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ClientApp {
    public static void main(String[] args) throws IOException {
        ClientLibrary client = new ClientLibrary();
        try {
            BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));
            System.out.println("ClientApp");

            while (true) {
                System.out.print("Key (ou 'exit' para sair): ");
                String key = userInput.readLine();
                if ("exit".equalsIgnoreCase(key)) break;

                System.out.print("Value: ");
                String value = userInput.readLine();

                ByteArrayWrapper byteArrayWrapper = new ByteArrayWrapper(value.getBytes());

                String response = client.sendKeyValue(key, byteArrayWrapper);
                System.out.println(response);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
