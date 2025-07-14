package fansirsqi.xposed.sesame.task;
import android.os.Build;
import android.os.Handler;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import fansirsqi.xposed.sesame.hook.ApplicationHook;
import fansirsqi.xposed.sesame.util.Log;
import fansirsqi.xposed.sesame.util.ThreadUtil;
/**
 * SystemChildTaskExecutor 类实现了 ChildTaskExecutor 接口，用于执行和管理子任务，
 * 支持在指定时间延迟后执行子任务，并且支持任务取消和任务组的管理。
 */
public class SystemChildTaskExecutor implements ChildTaskExecutor {
    /**
     * 用于延时执行任务的主线程处理器
     */
    private final Handler handler;
    /**
     * 存储每个任务组对应的线程池执行器
     */
    private final Map<String, ThreadPoolExecutor> groupChildTaskExecutorMap = new ConcurrentHashMap<>();
    /**
     * 构造函数，初始化 Handler
     */
    public SystemChildTaskExecutor() {
        handler = ApplicationHook.getMainHandler();
    }
    /**
     * 向任务组添加子任务。若子任务有延迟执行时间，则会在指定时间后执行任务。
     *
     * @param childTask 要添加的子任务
     * @return 是否添加成功
     */
    @Override
    public Boolean addChildTask(ModelTask.ChildModelTask childTask) {
        // 获取子任务所属的任务组的线程池
        ThreadPoolExecutor threadPoolExecutor = getChildGroupHandler(childTask.getGroup());
        long execTime = childTask.getExecTime();
        // 如果有延迟执行时间
        if (execTime > 0) {
            Runnable runnable = () -> {
                if (childTask.getIsCancel()) {
                    return; // 如果任务被取消则不执行
                }
                // 提交子任务到线程池
                Future<?> future = threadPoolExecutor.submit(() -> {
                    try {
                        long delay = childTask.getExecTime() - System.currentTimeMillis();
                        if (delay > 0) {
                            try {
                                ThreadUtil.sleep(delay); // 延迟执行子任务
                            } catch (Exception e) {
                                return; // 如果睡眠中被中断则直接返回
                            }
                        }
                        childTask.run(); // 执行子任务
                    } catch (Exception e) {
                        Log.printStackTrace(e);
                    } finally {
                        childTask.getModelTask().removeChildTask(childTask.getId()); // 完成后移除子任务
                    }
                });
                // 设置取消任务的操作
                childTask.setCancelTask(() -> future.cancel(true));
            };
            long delayMillis = execTime - System.currentTimeMillis();
            // 如果延迟时间大于3秒，设定延迟执行
            if (delayMillis > 3000) {
                handler.postDelayed(runnable, delayMillis - 2500); // 提前2500ms执行
                childTask.setCancelTask(() -> handler.removeCallbacks(runnable));
            } else {
                handler.post(runnable); // 立即执行
                childTask.setCancelTask(() -> handler.removeCallbacks(runnable));
            }
        } else {
            // 如果没有延迟，直接提交任务到线程池
            Future<?> future = threadPoolExecutor.submit(() -> {
                try {
                    childTask.run(); // 执行子任务
                } catch (Exception e) {
                    Log.printStackTrace(e);
                } finally {
                    childTask.getModelTask().removeChildTask(childTask.getId()); // 完成后移除子任务
                }
            });
            childTask.setCancelTask(() -> future.cancel(true)); // 设置取消任务的操作
        }
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
     * @return 是否清除成功
     */
    @Override
    public Boolean clearGroupChildTask(String group) {
        // 对于 Android 7.0 及以上版本，使用 compute 方法处理
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            groupChildTaskExecutorMap.compute(group, (keyInner, valueInner) -> {
                if (valueInner != null) {
                    // 等待线程池中任务结束并关闭线程池
                    ThreadUtil.shutdownAndAwaitTermination(valueInner, 3, TimeUnit.SECONDS);
                }
                return null; // 返回 null，表示移除该组的线程池
            });
        } else {
            // 对于低版本 Android，使用同步块处理
            synchronized (groupChildTaskExecutorMap) {
                ThreadPoolExecutor threadPoolExecutor = groupChildTaskExecutorMap.get(group);
                if (threadPoolExecutor != null) {
                    // 等待线程池任务结束并关闭线程池
                    ThreadUtil.shutdownAndAwaitTermination(threadPoolExecutor, 3, TimeUnit.SECONDS);
                    groupChildTaskExecutorMap.remove(group); // 移除该任务组的线程池
                }
            }
        }
        return true;
    }
    /**
     * 清除所有任务组中的所有子任务
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
     * 获取指定任务组的线程池执行器，如果不存在则创建一个新的线程池
     *
     * @param group 任务组
     * @return 该任务组的线程池执行器
     */
    private ThreadPoolExecutor getChildGroupHandler(String group) {
        ThreadPoolExecutor threadPoolExecutor = groupChildTaskExecutorMap.get(group);
        // 如果线程池已存在，直接返回
        if (threadPoolExecutor != null) {
            return threadPoolExecutor;
        }
        // Android 7.0 及以上版本使用 compute 方法创建线程池
        threadPoolExecutor = groupChildTaskExecutorMap.compute(group, (keyInner, valueInner) -> {
            if (valueInner == null) {
                // 创建一个新的线程池，最大线程数无穷大，采用调用者运行策略
                valueInner = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 30L, TimeUnit.SECONDS,
                        new SynchronousQueue<>(), new ThreadPoolExecutor.CallerRunsPolicy());
            }
            return valueInner;
        });
        return threadPoolExecutor;
    }
}
