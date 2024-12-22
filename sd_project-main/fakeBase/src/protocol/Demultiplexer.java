package protocol;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

// Adicionei o resto do Demultiplexer

public class Demultiplexer {
    private final Map<Integer, BlockingQueue<TaggedFrame>> frameQueues = new ConcurrentHashMap<>();

    public void send(int tag, TaggedFrame frame) {
        frameQueues.computeIfAbsent(tag, t -> new LinkedBlockingQueue<>()).add(frame);
    }

    public TaggedFrame receive(int tag) throws InterruptedException {
        return frameQueues.computeIfAbsent(tag, t -> new LinkedBlockingQueue<>()).take();
    }
}