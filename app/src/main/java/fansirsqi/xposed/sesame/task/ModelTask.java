package fansirsqi.xposed.sesame.task;

import android.os.Build;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import fansirsqi.xposed.sesame.model.BaseModel;
import fansirsqi.xposed.sesame.model.Model;
import fansirsqi.xposed.sesame.model.ModelFields;
import fansirsqi.xposed.sesame.model.ModelType;
import fansirsqi.xposed.sesame.util.GlobalThreadPools;
import fansirsqi.xposed.sesame.util.Log;
import fansirsqi.xposed.sesame.util.Notify;
import fansirsqi.xposed.sesame.util.StringUtil;
import lombok.Getter;
import lombok.Setter;

public abstract class ModelTask extends Model {
    private static final Map<ModelTask, Thread> MAIN_TASK_MAP = new ConcurrentHashMap<>();
    private static final ThreadPoolExecutor MAIN_THREAD_POOL = new ThreadPoolExecutor(getModelArray().length, Integer.MAX_VALUE, 30L, TimeUnit.SECONDS, new SynchronousQueue<>(), new ThreadPoolExecutor.CallerRunsPolicy());
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
                childTask.setModelTask(this);
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
                childTask.setModelTask(this);
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
        for (Model model : getModelArray()) {
            if (model != null) {
                if (ModelType.TASK == model.getType()) {
                    if (((ModelTask) model).startTask(force)) {
                        GlobalThreadPools.sleep(750);
                        Notify.updateNextExecText(-1);
                    }
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

    @Getter
    public static class ChildModelTask implements Runnable {
        @Setter
        private ModelTask modelTask;
        private final String id;
        private final String group;
        private final Runnable runnable;
        private final Long execTime;
        private CancelTask cancelTask;
        private Boolean isCancel = false;

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
            getRunnable().run();
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
            if (getCancelTask() != null) {
                try {
                    getCancelTask().cancel();
                    setCancel(true);
                } catch (Exception e) {
                    Log.printStackTrace(e);
                }
            }
        }

        public Boolean getCancel() {
            return isCancel;
        }

        public void setCancel(Boolean cancel) {
            isCancel = cancel;
        }
    }

    public interface CancelTask {
        void cancel();
    }
}
