package client;

import common.ByteArrayWrapper;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;


public class ClientLibrary {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 12345;

    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    public ClientLibrary() throws IOException {
        this.socket = new Socket(SERVER_HOST, SERVER_PORT);
        this.in = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());
    }

    public String sendKeyValue(String key, ByteArrayWrapper byteArrayWrapper) throws IOException {
        out.writeUTF(key);            // Envia a chave
        byteArrayWrapper.serialize(out);        // Envia o valor

        // Lê a resposta do servidor
        String echoKey = in.readUTF();
        int length = in.readInt();
        byte[] echoValue = new byte[length];
        in.readFully(echoValue);

        return "Echo do servidor: " + echoKey + " = " + new String(echoValue);
    }

    public void close() throws IOException {
        socket.close();
    }
}
