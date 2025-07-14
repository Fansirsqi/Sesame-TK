package fansirsqi.xposed.sesame.task;
import android.os.Build;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import fansirsqi.xposed.sesame.model.BaseModel;
import fansirsqi.xposed.sesame.model.Model;
import fansirsqi.xposed.sesame.model.ModelFields;
import fansirsqi.xposed.sesame.model.ModelType;
import fansirsqi.xposed.sesame.util.Log;
import fansirsqi.xposed.sesame.util.Notify;
import fansirsqi.xposed.sesame.util.StringUtil;
import fansirsqi.xposed.sesame.util.ThreadUtil;
import lombok.Getter;
public abstract class ModelTask extends Model {
    // 存储所有主任务与线程的映射
    private static final Map<ModelTask, Thread> MAIN_TASK_MAP = new ConcurrentHashMap<>();

    // 线程同步计数
    private static CountDownLatch taskCompletionLatch;

    // 主任务线程池，线程池大小为模型数组长度，最大线程数无限制，空闲时间30秒
    private static final ThreadPoolExecutor MAIN_THREAD_POOL = new ThreadPoolExecutor(
            Math.max(1, getModelArray().length)// 核心线程数，至少为 1
            , Math.min(Integer.MAX_VALUE, getModelArray().length * 2)// 最大线程数
            , 30L// 空闲线程存活时间
            , TimeUnit.SECONDS
//            , new SynchronousQueue<>()
            , new LinkedBlockingQueue<>(getModelArray().length * 2)// 队列容量
            , new ThreadPoolExecutor.CallerRunsPolicy()
    );
    // 存储子任务的映射
    private final Map<String, ChildModelTask> childTaskMap = new ConcurrentHashMap<>();
    private ChildTaskExecutor childTaskExecutor;
    @Getter
    private final Runnable mainRunnable = new Runnable() {
        private final ModelTask task = ModelTask.this;
        @Override
        public void run() {
            if (MAIN_TASK_MAP.get(task) != null) {
                return;
            }
            MAIN_TASK_MAP.put(task, Thread.currentThread());
            try {
                Notify.setStatusTextExec(task.getName());
                task.run();
            } catch (Exception e) {
                Log.printStackTrace(e);
            } finally {
                MAIN_TASK_MAP.remove(task);
                taskCompletionLatch.countDown();
                Notify.updateNextExecText(-1);
            }
        }
    };
    public ModelTask() {
    }
    /**
     * 准备任务执行环境
     */
    @Override
    public final void prepare() {
        childTaskExecutor = newTimedTaskExecutor();
    }
    /**
     * 获取任务ID
     *
     * @return 任务ID
     */
    public String getId() {
        return toString();
    }
    /**
     * 获取任务类型
     *
     * @return 任务类型为TASK
     */
    public ModelType getType() {
        return ModelType.TASK;
    }
    /**
     * 获取任务名称
     *
     * @return 任务名称
     */
    public abstract String getName();
    /**
     * 获取任务的字段
     *
     * @return 任务字段
     */
    public abstract ModelFields getFields();
    /**
     * 检查任务是否可执行
     *
     * @return Boolean值，表示是否通过检查
     */
    public abstract Boolean check();
    /**
     * 是否为同步任务
     *
     * @return Boolean值，表示是否为同步任务
     */
    public Boolean isSync() {
        return false;
    }
    /**
     * 执行任务
     */
    public abstract void run();
    /**
     * 检查任务是否包含指定的子任务
     *
     * @param childId 子任务ID
     * @return 是否包含该子任务
     */
    public Boolean hasChildTask(String childId) {
        return childTaskMap.containsKey(childId);
    }
    /**
     * 获取指定ID的子任务
     *
     * @param childId 子任务ID
     * @return 子任务对象
     */
    public ChildModelTask getChildTask(String childId) {
        return childTaskMap.get(childId);
    }
    /**
     * 添加子任务
     *
     * @param childTask 子任务对象
     */
    public void addChildTask(ChildModelTask childTask) {
        String childId = childTask.getId();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            childTaskMap.compute(childId, (key, value) -> {
                if (value != null) {
                    value.cancel();
                }
                childTask.modelTask = this;
                if (childTaskExecutor.addChildTask(childTask)) {
                    return childTask;
                }
                return null;
            });
        } else {
            synchronized (childTaskMap) {
                ChildModelTask oldTask = childTaskMap.get(childId);
                if (oldTask != null) {
                    oldTask.cancel();
                }
                childTask.modelTask = this;
                if (childTaskExecutor.addChildTask(childTask)) {
                    childTaskMap.put(childId, childTask);
                }
            }
        }
    }
    /**
     * 移除指定ID的子任务
     *
     * @param childId 子任务ID
     */
    public void removeChildTask(String childId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            childTaskMap.compute(childId, (key, value) -> {
                if (value != null) {
                    childTaskExecutor.removeChildTask(value);
                }
                return null;
            });
        } else {
            synchronized (childTaskMap) {
                ChildModelTask childTask = childTaskMap.get(childId);
                if (childTask != null) {
                    childTaskExecutor.removeChildTask(childTask);
                }
                childTaskMap.remove(childId);
            }
        }
    }
    /**
     * 获取当前任务的子任务数量
     *
     * @return 子任务数量
     */
    public Integer countChildTask() {
        return childTaskMap.size();
    }
    /**
     * 启动任务
     *
     * @return 是否成功启动任务
     */
    public Boolean startTask() {
        return startTask(false);
    }
    /**
     * 启动任务
     *
     * @param force 是否强制启动
     * @return 是否成功启动任务
     */
    public synchronized Boolean startTask(Boolean force) {
        if (MAIN_TASK_MAP.containsKey(this)) {
            if (!force) {
                return false;
            }
            stopTask();
        }
        try {
            if (isEnable() && check()) {
                if (isSync()) {
                    mainRunnable.run();
                    taskCompletionLatch.countDown();
                } else {
                    MAIN_THREAD_POOL.execute(mainRunnable);
                }
                return true;
            }
        } catch (Exception e) {
            Log.printStackTrace(e);
        }
        return false;
    }
    /**
     * 停止当前任务及其所有子任务
     */
    public synchronized void stopTask() {
        for (ChildModelTask childModelTask : childTaskMap.values()) {
            try {
                childModelTask.cancel();
            } catch (Exception e) {
                Log.printStackTrace(e);
            }
        }
        if (childTaskExecutor != null) {
            childTaskExecutor.clearAllChildTask();
        }
        childTaskMap.clear();
        MAIN_THREAD_POOL.remove(mainRunnable);
        MAIN_TASK_MAP.remove(this);
    }
    /**
     * 启动所有任务
     */
    public static void startAllTask() {
        startAllTask(false);
    }
    /**
     * 启动所有任务
     *
     * @param force 是否强制启动
     */
    public static void startAllTask(Boolean force) {
        Notify.setStatusTextExec();
        taskCompletionLatch = new CountDownLatch(getModelArray().length);
        // 启动一个线程等待所有任务完成
        new Thread(() -> {
            try {
                taskCompletionLatch.await(); // 等待所有任务完成
                Notify.forceUpdateText();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
        for (Model model : getModelArray()) {
            if (model != null) {
                if (ModelType.TASK == model.getType()) {
                    if (((ModelTask) model).startTask(force)) {
                        ThreadUtil.sleep(750);
                    } else {
                        taskCompletionLatch.countDown();
                        Notify.updateNextExecText(-1);
                    }
                } else {
                    // normal模块直接标记已完成
                    taskCompletionLatch.countDown();
                    Notify.updateNextExecText(-1);
                }
            }
        }
    }
    /**
     * 停止所有任务
     */
    public static void stopAllTask() {
        for (Model model : getModelArray()) {
            if (model != null) {
                try {
                    if (ModelType.TASK == model.getType()) {
                        ((ModelTask) model).stopTask();
                    }
                } catch (Exception e) {
                    Log.printStackTrace(e);
                }
            }
        }
    }
    /**
     * 创建一个新的子任务执行器
     *
     * @return 子任务执行器
     */
    private ChildTaskExecutor newTimedTaskExecutor() {
        ChildTaskExecutor childTaskExecutor;
        Integer timedTaskModel = BaseModel.getTimedTaskModel().getValue();
        if (timedTaskModel == BaseModel.TimedTaskModel.SYSTEM) {
            childTaskExecutor = new SystemChildTaskExecutor();
        } else if (timedTaskModel == BaseModel.TimedTaskModel.PROGRAM) {
            childTaskExecutor = new ProgramChildTaskExecutor();
        } else {
            throw new RuntimeException("not found childTaskExecutor");
        }
        return childTaskExecutor;
    }
    public static class ChildModelTask implements Runnable {
        @Getter
        private ModelTask modelTask;
        @Getter
        private final String id;
        @Getter
        private final String group;
        private final Runnable runnable;
        @Getter
        private final Long execTime;
        private CancelTask cancelTask;
        @Getter
        private Boolean isCancel = false;
        public ChildModelTask() {
            this(null, null, () -> {
            }, 0L);
        }
        public ChildModelTask(String id) {
            this(id, null, () -> {
            }, 0L);
        }
        public ChildModelTask(String id, String group) {
            this(id, group, () -> {
            }, 0L);
        }
        protected ChildModelTask(String id, long execTime) {
            this(id, null, null, execTime);
        }
        public ChildModelTask(String id, Runnable runnable) {
            this(id, null, runnable, 0L);
        }
        public ChildModelTask(String id, String group, Runnable runnable) {
            this(id, group, runnable, 0L);
        }
        public ChildModelTask(String id, String group, Runnable runnable, Long execTime) {
            if (StringUtil.isEmpty(id)) {
                id = toString();
            }
            if (StringUtil.isEmpty(group)) {
                group = "DEFAULT";
            }
            if (runnable == null) {
                runnable = setRunnable();
            }
            this.id = id;
            this.group = group;
            this.runnable = runnable;
            this.execTime = execTime;
        }
        /**
         * 设置子任务的运行逻辑
         *
         * @return 子任务的运行逻辑
         */
        public Runnable setRunnable() {
            return null;
        }
        /**
         * 执行子任务
         */
        public final void run() {
            runnable.run();
        }
        /**
         * 设置取消任务的逻辑
         *
         * @param cancelTask 取消任务的逻辑
         */
        protected void setCancelTask(CancelTask cancelTask) {
            this.cancelTask = cancelTask;
        }
        /**
         * 取消子任务
         */
        public final void cancel() {
            if (cancelTask != null) {
                try {
                    cancelTask.cancel();
                    isCancel = true;
                } catch (Exception e) {
                    Log.printStackTrace(e);
                }
            }
        }
    }
    /**
     * 返回是否还有未结束的任务
     */
    public static boolean isAllTaskFinished() {
        return taskCompletionLatch == null || taskCompletionLatch.getCount() == 0;
    }

    /**
     * 获取总任务执行进度 100%
     */
    public static int completedTaskPercentage() {
        if (taskCompletionLatch == null) {
            return 100;
        }
        int totalTaskCount = getModelArray().length;
        return (int) ((totalTaskCount - taskCompletionLatch.getCount()) * 100 / totalTaskCount);
    }
    public interface CancelTask {
        void cancel();
    }
}
