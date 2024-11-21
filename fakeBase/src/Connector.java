import java.io.*;
import java.net.Socket;
import java.util.concurrent.locks.ReentrantLock;

public class Connector implements AutoCloseable{
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private ReentrantLock rLock;
    private ReentrantLock wLock;

    public Connector(Socket socket) throws IOException {
        this.socket = socket;
        this.in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        this.out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        this.rLock = new ReentrantLock();
        this.wLock = new ReentrantLock();
    }

    public void send(byte[] data) throws IOException {
        this.wLock.lock();
        try {
            this.out.writeInt(data.length);
            this.out.write(data);
            this.out.flush();
        } finally {
            this.wLock.unlock();
        }
    }

    public byte[] receive() throws IOException {
        this.rLock.lock();
        try {
            int size = this.in.readInt();
            byte[] array = new byte[size];
            this.in.readFully(array);
            return array;
        } finally {
            this.rLock.unlock();
        }
    }

    public void close() throws IOException {
        socket.shutdownInput();
        socket.shutdownOutput();
        socket.close();
    }
}