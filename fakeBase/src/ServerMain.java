import java.net.ServerSocket;
import java.net.Socket;

public class ServerMain {
    private static final int MAX_SESSIONS = 5; // Máximo de sessões concorrentes
    private static final AuthManager authManager = new AuthManager();
    private static final SessionManager sessionManager = new SessionManager(MAX_SESSIONS);

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(12345)) {
            System.out.println("Server is running on port 12345...");

            authManager.register("Alice", "password1");
            authManager.register("Bob", "password2");
            authManager.register("1", "1");
            authManager.register("2", "2");
            authManager.register("3", "3");
            authManager.register("4", "4");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket.getInetAddress());

                new Thread(() -> handleClient(clientSocket)).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void handleClient(Socket clientSocket) {
        try (Connector connector = new Connector(clientSocket)) {
            sessionManager.acquireSession();

            try {
                byte[] usernameBytes = connector.receive();
                String username = new String(usernameBytes);

                byte[] passwordBytes = connector.receive();
                String password = new String(passwordBytes);

                if (authManager.authenticate(username, password)) {
                    System.out.println("Authentication successful for user: " + username);
                    connector.send("Welcome!".getBytes());
                } else {
                    System.out.println("Authentication failed for user: " + username);
                    connector.send("Authentication failed".getBytes());
                }
            } finally {
                // Liberar a sessão após o término
                //sessionManager.releaseSession();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
