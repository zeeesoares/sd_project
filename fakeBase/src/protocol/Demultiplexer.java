package protocol;

import java.io.IOException;
import java.util.HashMap;
import java.util.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;


public class Demultiplexer implements AutoCloseable {
    private TaggedConnection conn;
    private Map<Integer, Entry> buf = new HashMap<>();
    private ReentrantLock readLock;
    private IOException exception;

    private class Entry{
        int waiters = 0;
        final Condition cond = readLock.newCondition();
        final ArrayDeque<byte[]> queue = new ArrayDeque<>();
    }

    public Entry get(int tag) {
        Entry e = buf.get(tag);
        if (e == null){
            e = new Entry();
            this.buf.put(tag, e);
        }
        return e;
    }

    public Demultiplexer(TaggedConnection conn) {
        this.conn = conn;
        this.readLock = new ReentrantLock();
    }

    public void start() {
        new Thread(() -> {
            try {
                for(;;) {
                    TaggedFrame frame = this.conn.receive();
                    readLock.lock();
                    try {
                        Entry e = get(frame.getTag());
                        e.queue.add(frame.getData());
                        e.cond.signalAll();
                    }
                    finally {
                        readLock.unlock();
                    }
                }
            } catch (IOException e) {
                readLock.lock();
                try{
                    exception = e;
                }
                finally {
                    readLock.unlock();
                }

            }
        }).start();
    }

    public void send(TaggedFrame frame) throws IOException {
       this.conn.send(frame);
    }

    public void send(int tag, byte[] data) throws IOException {
        this.conn.send(tag, data);
    }

    public byte[] receive(int tag) throws IOException, InterruptedException {
       readLock.lock();
       try{
          Entry e = get(tag);
          e.waiters++;

          while(e.queue.isEmpty() && exception == null){
              e.cond.await();
          }

          byte[] res = e.queue.poll();
          e.waiters--;

          if(e.queue.isEmpty() && e.waiters == 0){
              buf.remove(tag);
          }

          if(res != null){
              return res;
          }
          else {
              throw exception;
          }
       }
       finally {
           readLock.unlock();
       }
    }

    public void close() throws IOException {
        this.conn.close();
    }
}