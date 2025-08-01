package fansirsqi.xposed.sesame.task;

import android.os.Build;

import fansirsqi.xposed.sesame.util.Log;
import fansirsqi.xposed.sesame.util.GlobalThreadPools;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * ProgramChildTaskExecutor 类实现了 ChildTaskExecutor 接口，用于管理和执行子任务。
 * 它现在使用全局线程池来执行任务。
 */
public class ProgramChildTaskExecutor implements ChildTaskExecutor {
    private static final String TAG = "ProgramChildTaskExecutor";
    private final Map<String, ThreadPoolExecutor> groupChildTaskExecutorMap = new ConcurrentHashMap<>();

    @Override
    public Boolean addChildTask(ModelTask.ChildModelTask childTask) {
        ThreadPoolExecutor threadPoolExecutor = getChildGroupThreadPool(childTask.getGroup());
        Future<?> future;
        long execTime = childTask.getExecTime();
        if (execTime > 0) {
            future = threadPoolExecutor.submit(() -> {
                if (childTask.getIsCancel()) {
                    return;
                }
                try {
                    long delay = childTask.getExecTime() - System.currentTimeMillis();
                    if (delay > 0) {
                        try {
                            Thread.sleep(delay);
                        } catch (Exception e) {
                            return;
                        }
                    }
                    childTask.run();
                } catch (Exception e) {
                    Log.printStackTrace(e);
                } finally {
                    childTask.getModelTask().removeChildTask(childTask.getId());
                }
            });
        } else {
            future = threadPoolExecutor.submit(() -> {
                try {
                    childTask.run();
                } catch (Exception e) {
                    Log.printStackTrace(e);
                } finally {
                    childTask.getModelTask().removeChildTask(childTask.getId());
                }
            });
        }
        childTask.setCancelTask(() -> future.cancel(true));
        return true;
    }

    @Override
    public void removeChildTask(ModelTask.ChildModelTask childTask) {
        childTask.cancel();
    }

    @Override
    public Boolean clearGroupChildTask(String group) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            groupChildTaskExecutorMap.compute(group, (keyInner, valueInner) -> {
                if (valueInner != null) {
                    GlobalThreadPools.shutdownAndAwaitTermination(valueInner, 3, group);
                }
                return null;
            });
        } else {
            synchronized (groupChildTaskExecutorMap) {
                ThreadPoolExecutor groupThreadPool = groupChildTaskExecutorMap.get(group);
                if (groupThreadPool != null) {
                    GlobalThreadPools.shutdownAndAwaitTermination(groupThreadPool, 3, group);
                    groupChildTaskExecutorMap.remove(group);
                }
            }
        }
        return true;
    }

    @Override
    public void clearAllChildTask() {
        for (ThreadPoolExecutor pool : groupChildTaskExecutorMap.values()) {
            if (pool != null && !pool.isShutdown()) {
                pool.shutdownNow();
            }
        }
        groupChildTaskExecutorMap.clear();
    }

    private ThreadPoolExecutor getChildGroupThreadPool(String group) {
        ThreadPoolExecutor threadPoolExecutor = groupChildTaskExecutorMap.get(group);
        if (threadPoolExecutor != null) {
            return threadPoolExecutor;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            threadPoolExecutor = groupChildTaskExecutorMap.compute(group, (keyInner, valueInner) -> {
                if (valueInner == null) {
                    valueInner = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 30L, TimeUnit.SECONDS, new SynchronousQueue<>(), new ThreadPoolExecutor.CallerRunsPolicy());
                }
                return valueInner;
            });
        } else {
            synchronized (groupChildTaskExecutorMap) {
                threadPoolExecutor = groupChildTaskExecutorMap.get(group);
                if (threadPoolExecutor == null) {
                    threadPoolExecutor = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 30L, TimeUnit.SECONDS, new SynchronousQueue<>(), new ThreadPoolExecutor.CallerRunsPolicy());
                    groupChildTaskExecutorMap.put(group, threadPoolExecutor);
                }
            }
        }
        return threadPoolExecutor;
    }
}
