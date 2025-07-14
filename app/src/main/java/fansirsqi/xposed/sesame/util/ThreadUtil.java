package fansirsqi.xposed.sesame.util;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
public class ThreadUtil {
    private static final String TAG = ThreadUtil.class.getSimpleName();
    public static void shutdownAndWait(Thread thread, long timeout, TimeUnit unit) {
        if (thread != null) {
            thread.interrupt();
            if (timeout > -1L) {
                try {
                    thread.join(unit.toMillis(timeout));
                } catch (InterruptedException e) {
                    Log.runtime(TAG, "thread shutdownAndWait err:");
                    Log.printStackTrace(TAG, e);
                }
            }
        }
    }
    public static void shutdownNow(ExecutorService pool) {
        if (pool != null && !pool.isShutdown()) {
            pool.shutdownNow();
        }
    }
    /**
     * 使当前线程暂停指定的毫秒数。
     *
     * @param millis 毫秒数。
     */
    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
    public boolean shutdownAndAwaitTermination(ExecutorService pool) {
        try {
            shutdownAndAwaitTermination(pool, 30, TimeUnit.SECONDS);
        } catch (Exception e) {
            Log.runtime(TAG, "thread shutdownAndWait err:");
            Log.printStackTrace(TAG, e);
            return false;
        }
        return true;
    }
    public static void shutdownAndAwaitTermination(ExecutorService pool, long timeout, TimeUnit unit) {
        if (pool != null && !pool.isShutdown()) {
            pool.shutdown();
            try {
                if (!pool.awaitTermination(1, TimeUnit.SECONDS)) {
                    pool.shutdownNow();
                    if (!pool.awaitTermination(timeout, unit)) {
                        Log.runtime(TAG, "thread pool can't close");
                    }
                }
            } catch (InterruptedException ie) {
                pool.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}
