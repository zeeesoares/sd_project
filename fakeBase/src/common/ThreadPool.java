package common;

public class ThreadPool {
    private final Thread[] workers; 
    private final RequestQueue requestQueue; 
    private volatile boolean isRunning = true;

    public ThreadPool(int threadCount) {
        this.requestQueue = new RequestQueue(); 
        workers = new Thread[threadCount];

        for (int i = 0; i < threadCount; i++) {
            workers[i] = new Thread(() -> {
                try {
                    while (isRunning) {
                        Runnable task = requestQueue.dequeue(); 
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
        if (isRunning) {
            requestQueue.enqueue(task); 
        }
    }

    public void shutdown() {
        isRunning = false;
        for (Thread worker : workers) {
            worker.interrupt(); 
        }
        for (Thread worker : workers) {
            try {
                worker.join(); 
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
