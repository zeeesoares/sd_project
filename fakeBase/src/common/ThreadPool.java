package common;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;

public class ThreadPool {
    private final Thread[] workers; 
    private final RequestQueue requestQueue; 
    private final Lock lock = new ReentrantLock(); // Lock para proteger o estado
    private final Condition terminationCondition = lock.newCondition(); // Condição para aguardar término
    private boolean isRunning = true;

    public ThreadPool(int threadCount) {
        this.requestQueue = new RequestQueue();
        workers = new Thread[threadCount];

        for (int i = 0; i < threadCount; i++) {
            workers[i] = new Thread(() -> {
                try {
                    while (true) {
                        Runnable task;
                        lock.lock();
                        try {
                            if (!isRunning && requestQueue.isEmpty()) {
                                break;
                            }
                        } finally {
                            lock.unlock();
                        }

                        task = requestQueue.dequeue(); 

                        if (task != null) {
                            task.run();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); 
                }
            });
            workers[i].start();
        }
    }

    public void submit(Runnable task) {
        lock.lock();
        try {
            if (!isRunning) {
                throw new IllegalStateException("ThreadPool has been shut down.");
            }
            requestQueue.enqueue(task); // Enfileira a tarefa (RequestQueue já tem locks)
        } finally {
            lock.unlock();
        }
    }

    public void shutdown() {
        lock.lock();
        try {
            isRunning = false;
            terminationCondition.signalAll(); // Acorda quaisquer threads em espera
        } finally {
            lock.unlock();
        }

        for (Thread worker : workers) {
            worker.interrupt(); // Interrompe os threads
        }

        for (Thread worker : workers) {
            try {
                worker.join(); // Aguarda que todos os threads terminem
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Restaura o estado de interrupção
            }
        }
    }
}
