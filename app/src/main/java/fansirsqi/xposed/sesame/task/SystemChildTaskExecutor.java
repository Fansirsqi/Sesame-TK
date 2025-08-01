package fansirsqi.xposed.sesame.task;

import android.os.Handler;

import fansirsqi.xposed.sesame.hook.ApplicationHook;
import fansirsqi.xposed.sesame.util.GlobalThreadPools;
import fansirsqi.xposed.sesame.util.Log;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * SystemChildTaskExecutor 类实现了 ChildTaskExecutor 接口，用于执行和管理子任务，
 * 支持在指定时间延迟后执行子任务，并且支持任务取消和任务组的管理。
 */
public class SystemChildTaskExecutor implements ChildTaskExecutor {
    private static final String TAG = "SystemChildTaskExecutor";
    private final Handler handler;

    private final Map<String, ThreadPoolExecutor> groupChildTaskExecutorMap = new ConcurrentHashMap<>();

    public SystemChildTaskExecutor() {
        handler = ApplicationHook.getMainHandler();
    }

    @Override
    public Boolean addChildTask(ModelTask.ChildModelTask childTask) {
        long execTime = childTask.getExecTime();
        Runnable runnable = () -> {
            if (childTask.getIsCancel()) {
                return;
            }

            ThreadPoolExecutor threadPoolExecutor = getChildGroupHandler(childTask.getGroup());
            Future<?> future = threadPoolExecutor.submit((Callable<Void>) () -> {
                try {
                    long delay = execTime - System.currentTimeMillis();
                    if (delay > 0) {
                        try {
                            Thread.sleep(delay);
                        } catch (InterruptedException e) {
                            Log.runtime(TAG, "延迟中断，任务可能已取消: " + childTask.getId());
                            Thread.currentThread().interrupt(); // 恢复中断状态
                            return null;
                        }
                    }

                    childTask.run();

                } catch (Throwable t) {
                    Log.printStackTrace(TAG, "子任务执行异常: " + childTask.getId(), t);
                } finally {
                    // 可选：根据业务决定是否移除
                    childTask.getModelTask().removeChildTask(childTask.getId());
                }
                return null;
            });

            childTask.setCancelTask(() -> future.cancel(true));
        };

        if (execTime > 0) {
            long delayMillis = execTime - System.currentTimeMillis();
            if (delayMillis > 3000) {
                handler.postDelayed(runnable, delayMillis - 2500);
                childTask.setCancelTask(() -> handler.removeCallbacks(runnable));
            } else {
                // 防止负数或过小 delay
                delayMillis = Math.max(0, delayMillis);
                childTask.setCancelTask(() -> handler.removeCallbacks(runnable));
                handler.postDelayed(runnable, delayMillis);
            }
        } else {
            handler.post(runnable);
        }

        return true;
    }

    @Override
    public void removeChildTask(ModelTask.ChildModelTask childTask) {
        childTask.cancel();
    }

    @Override
    public Boolean clearGroupChildTask(String group) {
        ThreadPoolExecutor pool = groupChildTaskExecutorMap.get(group);
        if (pool != null) {
            GlobalThreadPools.shutdownAndAwaitTermination(pool, 3, group);
            groupChildTaskExecutorMap.remove(group);
        }
        return true;
    }

    @Override
    public void clearAllChildTask() {
        for (Map.Entry<String, ThreadPoolExecutor> entry : groupChildTaskExecutorMap.entrySet()) {
            GlobalThreadPools.shutdownAndAwaitTermination(entry.getValue(), 3, entry.getKey());
        }
        groupChildTaskExecutorMap.clear();
    }

    private ThreadPoolExecutor getChildGroupHandler(String group) {
        return getOrCreateThreadPool(group);
    }

    /**
     * 获取或创建一个线程池
     */
    private synchronized ThreadPoolExecutor getOrCreateThreadPool(String group) {
        ThreadPoolExecutor existing = groupChildTaskExecutorMap.get(group);
        if (existing != null && !existing.isShutdown()) {
            return existing;
        }

        ThreadPoolExecutor newPool = new ThreadPoolExecutor(
                0,
                Integer.MAX_VALUE,
                30L,
                TimeUnit.SECONDS,
                new SynchronousQueue<>(),
                new NamedThreadFactory("TaskGroup-" + group),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );

        groupChildTaskExecutorMap.put(group, newPool);
        return newPool;
    }

    /**
     * 自定义线程工厂，设置线程名前缀
     */
    private static class NamedThreadFactory implements ThreadFactory {
        private final String namePrefix;
        private final AtomicInteger threadNumber = new AtomicInteger(1);

        public NamedThreadFactory(String namePrefix) {
            this.namePrefix = namePrefix;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, namePrefix + "-thread-" + threadNumber.getAndIncrement());
            t.setDaemon(false); // 根据需要调整
            return t;
        }
    }
}