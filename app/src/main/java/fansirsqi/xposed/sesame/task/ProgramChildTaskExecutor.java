package fansirsqi.xposed.sesame.task;
import android.os.Build;
import fansirsqi.xposed.sesame.util.Log;
import fansirsqi.xposed.sesame.util.ThreadUtil;
import java.util.Map;
import java.util.concurrent.*;
/**
 * ProgramChildTaskExecutor 类实现了 ChildTaskExecutor 接口，用于管理和执行子任务。
 * 它支持按任务组分配线程池，并为每个子任务设定执行时间。
 */
public class ProgramChildTaskExecutor implements ChildTaskExecutor {
    /**
     * 存储每个任务组的线程池执行器
     */
    private final Map<String, ThreadPoolExecutor> groupChildTaskExecutorMap = new ConcurrentHashMap<>();
    /**
     * 向任务组中添加子任务
     *
     * @param childTask 要添加的子任务
     * @return 添加是否成功
     */
    @Override
    public Boolean addChildTask(ModelTask.ChildModelTask childTask) {
        // 获取子任务所在任务组的线程池
        ThreadPoolExecutor threadPoolExecutor = getChildGroupThreadPool(childTask.getGroup());
        Future<?> future;
        long execTime = childTask.getExecTime();
        // 如果子任务有执行时间，进行延时执行
        if (execTime > 0) {
            future = threadPoolExecutor.submit(() -> {
                if (childTask.getIsCancel()) {
                    return;
                }
                try {
                    long delay = childTask.getExecTime() - System.currentTimeMillis();
                    if (delay > 0) {
                        ThreadUtil.sleep(delay); // 延时执行任务
                    }
                    childTask.run(); // 执行子任务
                } catch (Exception e) {
                    Log.printStackTrace(e);
                } finally {
                    childTask.getModelTask().removeChildTask(childTask.getId()); // 移除已完成的子任务
                }
            });
        } else {
            future = threadPoolExecutor.submit(() -> {
                try {
                    childTask.run(); // 执行子任务
                } catch (Exception e) {
                    Log.printStackTrace(e);
                } finally {
                    childTask.getModelTask().removeChildTask(childTask.getId()); // 移除已完成的子任务
                }
            });
        }
        // 设置子任务取消时的操作
        childTask.setCancelTask(() -> future.cancel(true));
        return true;
    }
    /**
     * 移除指定的子任务
     *
     * @param childTask 要移除的子任务
     */
    @Override
    public void removeChildTask(ModelTask.ChildModelTask childTask) {
        childTask.cancel(); // 取消子任务
    }
    /**
     * 清除指定任务组中的所有子任务
     *
     * @param group 子任务所属的任务组
     * @return 清除是否成功
     */
    @Override
    public Boolean clearGroupChildTask(String group) {
        groupChildTaskExecutorMap.compute(group, (keyInner, valueInner) -> {
            if (valueInner != null) {
                // 等待线程池中任务结束并关闭线程池
                ThreadUtil.shutdownAndAwaitTermination(valueInner, 3, TimeUnit.SECONDS);
            }
            return null;
        });
        return true;
    }
    /**
     * 清除所有子任务
     */
    @Override
    public void clearAllChildTask() {
        // 遍历所有任务组，关闭对应的线程池
        for (ThreadPoolExecutor threadPoolExecutor : groupChildTaskExecutorMap.values()) {
            ThreadUtil.shutdownNow(threadPoolExecutor); // 立即关闭线程池
        }
        groupChildTaskExecutorMap.clear(); // 清空所有任务组
    }
    /**
     * 获取指定任务组的线程池执行器，如果没有则创建一个新的线程池
     *
     * @param group 任务组
     * @return 该任务组的线程池执行器
     */
    private ThreadPoolExecutor getChildGroupThreadPool(String group) {
        ThreadPoolExecutor threadPoolExecutor = groupChildTaskExecutorMap.get(group);
        // 如果线程池不存在，则创建一个新的线程池
        if (threadPoolExecutor != null) {
            return threadPoolExecutor;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            threadPoolExecutor = groupChildTaskExecutorMap.compute(group, (keyInner, valueInner) -> {
                if (valueInner == null) {
                    // 创建新的线程池，最大线程数为无穷大，采用调用者运行策略
                    valueInner = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 30L, TimeUnit.SECONDS,
                            new SynchronousQueue<>(), new ThreadPoolExecutor.CallerRunsPolicy());
                }
                return valueInner;
            });
        } else {
            synchronized (groupChildTaskExecutorMap) {
                threadPoolExecutor = groupChildTaskExecutorMap.get(group);
                if (threadPoolExecutor == null) {
                    // 创建新的线程池，最大线程数为无穷大，采用调用者运行策略
                    threadPoolExecutor = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 30L, TimeUnit.SECONDS,
                            new SynchronousQueue<>(), new ThreadPoolExecutor.CallerRunsPolicy());
                    groupChildTaskExecutorMap.put(group, threadPoolExecutor);
                }
            }
        }
        return threadPoolExecutor;
    }
}
