package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    public static void main(String[] args) {
        try {
            ServerSocket ss = new ServerSocket(12345);

            while (true) {
                Socket socket = ss.accept();

                Thread handleClient = new Thread(new ClientHandler(socket));
                handleClient.start();
            }


        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
