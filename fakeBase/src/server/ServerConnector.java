package server;

import protocol.TaggedConnection;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerConnector {
    private ServerSocket serverSocket;

    public ServerConnector(int port) throws IOException {
        this.serverSocket = new ServerSocket(port);
    }

    public TaggedConnection accept() throws IOException {
        Socket clientSocket = serverSocket.accept();
        return new TaggedConnection(clientSocket);
    }

    public void close() throws IOException {
        serverSocket.close();
    }
}
