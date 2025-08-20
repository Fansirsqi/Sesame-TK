package fansirsqi.xposed.sesame.task;

import android.os.Build;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import fansirsqi.xposed.sesame.util.Log;

import lombok.Getter;

public abstract class BaseTask {
    private static final String TAG = "BaseTask";

    @Getter
    private volatile Thread thread;

    private final Map<String, BaseTask> childTaskMap = new ConcurrentHashMap<>();

    /**
     * 构造函数
     */
    public BaseTask() {
        this.thread = null;
    }

    /**
     * 获取任务ID
     *
     * @return 任务ID字符串
     */
    public String getId() {
        return toString();
    }

    /**
     * 检查任务是否可以执行
     *
     * @return 是否可以执行
     */
    public abstract Boolean check();

    /**
     * 执行任务的主要逻辑
     */
    public abstract void run();

    /**
     * 检查是否包含指定的子任务
     *
     * @param childId 子任务ID
     * @return 是否包含该子任务
     */
    public synchronized Boolean hasChildTask(String childId) {
        return childTaskMap.containsKey(childId);
    }

    /**
     * 获取指定ID的子任务
     *
     * @param childId 子任务ID
     * @return 子任务对象，如果不存在则返回null
     */
    public synchronized BaseTask getChildTask(String childId) {
        return childTaskMap.get(childId);
    }

    /**
     * 添加子任务
     *
     * @param childTask 要添加的子任务
     */
    public synchronized void addChildTask(BaseTask childTask) {
        String childId = childTask.getId();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            childTaskMap.compute(childId, (key, value) -> {
                if (value != null) {
                    value.stopTask();
                }
                childTask.startTask();
                return childTask;
            });
        } else {
            BaseTask oldTask = childTaskMap.get(childId);
            if (oldTask != null) {
                oldTask.stopTask();
            }
            childTask.startTask();
            childTaskMap.put(childId, childTask);
        }
    }

    /**
     * 移除指定ID的子任务
     *
     * @param childId 要移除的子任务ID
     */
    public synchronized void removeChildTask(String childId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            childTaskMap.compute(childId, (key, value) -> {
                if (value != null) {
                    shutdownAndWait(value.getThread(), -1, TimeUnit.SECONDS);
                }
                return null;
            });
        } else {
            BaseTask oldTask = childTaskMap.get(childId);
            if (oldTask != null) {
                shutdownAndWait(oldTask.getThread(), -1, TimeUnit.SECONDS);
            }
            childTaskMap.remove(childId);
        }
    }

    /**
     * 获取子任务数量
     *
     * @return 子任务的数量
     */
    public synchronized Integer countChildTask() {
        return childTaskMap.size();
    }

    /**
     * 启动任务（非强制模式）
     */
    public void startTask() {
        startTask(false);
    }

    /**
     * 启动任务
     *
     * @param force 是否强制启动，如果为true则停止当前任务后重新启动
     */
    public synchronized void startTask(Boolean force) {
        if (thread != null && thread.isAlive()) {
            if (!force) {
                return;
            }
            stopTask();
        }
        thread = new Thread(this::run);
        try {
            if (check()) {
                thread.start();
                for (BaseTask childTask : childTaskMap.values()) {
                    if (childTask != null) {
                        childTask.startTask();
                    }
                }
            }
        } catch (Exception e) {
            Log.printStackTrace(e);
        }
    }

    /**
     * 关闭线程并等待其结束
     *
     * @param thread  要关闭的线程
     * @param timeout 等待超时时间
     * @param unit    时间单位
     */
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

    /**
     * 停止当前任务及其所有子任务
     */
    public synchronized void stopTask() {
        if (thread != null && thread.isAlive()) {
            shutdownAndWait(thread, 5, TimeUnit.SECONDS);
        }
        for (BaseTask childTask : childTaskMap.values()) {
            if (childTask != null) {
                shutdownAndWait(childTask.getThread(), -1, TimeUnit.SECONDS);
            }
        }
        thread = null;
        childTaskMap.clear();
    }

    /**
     * 创建一个新的BaseTask实例
     *
     * @return 新的BaseTask实例
     */
    public static BaseTask newInstance() {
        return new BaseTask() {
            @Override
            public void run() {
            }

            @Override
            public Boolean check() {
                return true;
            }
        };
    }

    /**
     * 创建一个指定ID的BaseTask实例
     *
     * @param id 任务ID
     * @return 新的BaseTask实例
     */
    public static BaseTask newInstance(String id) {
        return new BaseTask() {
            @Override
            public String getId() {
                return id;
            }

            @Override
            public void run() {
            }

            @Override
            public Boolean check() {
                return true;
            }
        };
    }

    /**
     * 创建一个指定ID和运行逻辑的BaseTask实例
     *
     * @param id       任务ID
     * @param runnable 运行逻辑
     * @return 新的BaseTask实例
     */
    public static BaseTask newInstance(String id, Runnable runnable) {
        return new BaseTask() {
            @Override
            public String getId() {
                return id;
            }

            @Override
            public void run() {
                runnable.run();
            }

            @Override
            public Boolean check() {
                return true;
            }
        };
    }

}
