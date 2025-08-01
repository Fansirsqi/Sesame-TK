package fansirsqi.xposed.sesame.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 全局线程池管理类
 */
public class GlobalThreadPools {
    private static final String TAG = "GlobalThreadPools";

    /**
     * 使当前线程暂停指定的毫秒数。
     *
     * @param millis 毫秒数。
     */
    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Log.error(TAG, "Thread sleep interrupted " + e.getMessage());
            Thread.currentThread().interrupt();
        } catch (Exception e1) {
            Log.printStackTrace(e1);
//            Thread.currentThread().interrupt();
            Log.error(TAG, "Thread sleep interrupted " + e1.getMessage());
        } catch (Throwable t) {
            Log.printStackTrace(t);
//            Thread.currentThread().interrupt();
            Log.error(TAG, "Thread sleep interrupted " + t.getMessage());
        }
    }

    public static void shutdownAndAwaitTermination(ExecutorService pool, long timeout, String poolName) {
        if (pool != null && !pool.isShutdown()) {
            pool.shutdown();
            try {
                if (!pool.awaitTermination(1, TimeUnit.SECONDS)) {
                    pool.shutdownNow();
                    if (!pool.awaitTermination(timeout, TimeUnit.SECONDS)) {
                        Log.runtime(TAG, "thread " + poolName + " can't close");
                    }
                }
            } catch (InterruptedException ie) {
                pool.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}