import java.io.*;
import java.net.Socket;
import java.util.Arrays;

public class ClientHandler implements Runnable {
    private Socket socket;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (
                DataInputStream in = new DataInputStream(socket.getInputStream());
                DataOutputStream out = new DataOutputStream(socket.getOutputStream());
        ) {
            while (true) {
                String key = in.readUTF();
                int length = in.readInt();
                byte[] value = new byte[length];
                in.readFully(value);

                System.out.println("Recebido: " + key + " = " + Arrays.toString(value));

                out.writeUTF(key);
                out.writeInt(length);
                out.write(value);
            }
        } catch (IOException e) {
            System.out.println("Conexão encerrada pelo cliente");
        }
    }
}

