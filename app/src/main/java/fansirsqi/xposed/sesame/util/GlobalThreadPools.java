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

    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    // 用于执行普通异步任务的线程池
    private static final ExecutorService GENERAL_PURPOSE_EXECUTOR;
    // 用于执行定时或周期性任务的线程池
    private static final ScheduledExecutorService SCHEDULED_EXECUTOR;

    static {
        GENERAL_PURPOSE_EXECUTOR = new ThreadPoolExecutor(Math.max(2, Math.min(CPU_COUNT - 1, 4)), // 核心线程数
                CPU_COUNT * 8 + 1, // 最大线程数
                30L, // 空闲线程存活时间
                TimeUnit.SECONDS, new SynchronousQueue<>(), // 工作队列
                Executors.defaultThreadFactory(), // 线程工厂
                new ThreadPoolExecutor.CallerRunsPolicy() // 拒绝策略
        );

        SCHEDULED_EXECUTOR = Executors.newScheduledThreadPool(Math.max(1, Math.min(CPU_COUNT - 1, 2)), // 核心线程数，至少为1
                Executors.defaultThreadFactory());

        // 确保在JVM关闭时优雅地关闭线程池
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            shutdownAndAwaitTermination(GENERAL_PURPOSE_EXECUTOR,30, "GeneralPurposeExecutor");
            shutdownAndAwaitTermination(SCHEDULED_EXECUTOR, 30,"ScheduledExecutor");
        }));
    }

    /**
     * 获取通用任务执行器
     *
     * @return ExecutorService
     */
    public static ExecutorService getGeneralPurposeExecutor() {
        return GENERAL_PURPOSE_EXECUTOR;
    }

    /**
     * 获取定时任务执行器
     *
     * @return ScheduledExecutorService
     */
    public static ScheduledExecutorService getScheduledExecutor() {
        return SCHEDULED_EXECUTOR;
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
            Log.error(TAG, "Thread sleep interrupted " + e.getMessage());
//            Thread.currentThread().interrupt();
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