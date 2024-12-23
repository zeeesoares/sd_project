package protocol;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.locks.ReentrantLock;

public class TaggedConnection implements AutoCloseable {

    private TaggedFrame frame;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private ReentrantLock rLock;
    private ReentrantLock wLock;

    public TaggedConnection(Socket socket) throws IOException {
        this.socket = socket;
        this.in =  new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        this.out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        this.rLock = new ReentrantLock();
        this.wLock = new ReentrantLock();
    }

    public void send(TaggedFrame frame) throws IOException {
        this.wLock.lock();
        try {
            frame.serialize(out);
            out.flush();
        } finally {
            this.wLock.unlock();
        }
    }

    public void send(int tag, byte[] data) throws IOException {
        this.wLock.lock();
        try {
            TaggedFrame frame = new TaggedFrame(tag,data);
            frame.serialize(out);
            out.flush();
        } finally {
            this.wLock.unlock();
        }
    }

    public TaggedFrame receive() throws IOException {
        this.rLock.lock();
        try {
            return TaggedFrame.deserialize(in);
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