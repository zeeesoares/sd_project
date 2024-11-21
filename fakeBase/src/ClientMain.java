import java.net.Socket;
import java.util.Scanner;

public class ClientMain {
    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            Socket socket = new Socket("localhost", 12345);
            Connector connector = new Connector(socket);

            System.out.println("Enter username: ");
            String username = scanner.nextLine();
            System.out.println("Enter password: ");
            String password = scanner.nextLine();

            connector.send(username.getBytes());
            connector.send(password.getBytes());

            // Receber resposta do servidor
            byte[] response = connector.receive();
            System.out.println("Server response: " + new String(response));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
