import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class SessionManager {
    private final int maxSessions;
    private int currentSessions = 0;
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition sessionAvailable = lock.newCondition();

    public SessionManager(int maxSessions) {
        this.maxSessions = maxSessions;
    }

    public void acquireSession() throws InterruptedException {
        lock.lock();
        try {
            while (currentSessions >= maxSessions) {
                sessionAvailable.await();
            }
            currentSessions++;
        } finally {
            lock.unlock();
        }
    }

    public void releaseSession() {
        lock.lock();
        try {
            currentSessions--;
            sessionAvailable.signalAll();
        } finally {
            lock.unlock();
        }
    }
}
