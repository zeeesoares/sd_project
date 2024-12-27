package common;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class RequestQueue {
    private final Queue<Runnable> queue = new LinkedList<>();
    private final Lock lock = new ReentrantLock();
    private final Condition notEmpty = lock.newCondition();

    public void enqueue(Runnable task) {
        lock.lock();
        try {
            queue.add(task);
            notEmpty.signal(); // Notifica que a fila não está mais vazia
        } finally {
            lock.unlock();
        }
    }

    public Runnable dequeue() throws InterruptedException {
        lock.lock();
        try {
            while (queue.isEmpty()) {
                notEmpty.await(); // Espera até que haja uma tarefa na fila
            }
            return queue.poll(); // Retorna e remove a próxima tarefa da fila
        } finally {
            lock.unlock();
        }
    }

    public boolean isEmpty() {
        lock.lock();
        try {
            return queue.isEmpty(); // Verifica se a fila está vazia
        } finally {
            lock.unlock();
        }
    }
}
