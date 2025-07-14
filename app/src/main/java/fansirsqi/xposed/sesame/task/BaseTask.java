package fansirsqi.xposed.sesame.task;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import fansirsqi.xposed.sesame.util.Log;
import fansirsqi.xposed.sesame.util.ThreadUtil;
import lombok.Getter;
public abstract class BaseTask {
    @Getter
    private volatile Thread thread;
    private final Map<String, BaseTask> childTaskMap = new ConcurrentHashMap<>();
    public BaseTask() {
        this.thread = null;
    }
    public String getId() {
        return toString();
    }
    public abstract Boolean check();
    public abstract void run();
    public void startTask() {
        startTask(false);
    }
    private void startChildTasks() {
        for (BaseTask childTask : childTaskMap.values()) {
            if (childTask != null) {
                childTask.startTask();
            }
        }
    }
    private void stopChildTasks() {
        for (BaseTask childTask : childTaskMap.values()) {
            if (childTask != null) {
                ThreadUtil.shutdownAndWait(childTask.getThread(), -1, TimeUnit.SECONDS);
            }
        }
        childTaskMap.clear();
    }
    public synchronized void startTask(Boolean force) {
        try {
            if (thread != null && thread.isAlive()) {
                if (!force) return;
                stopTask();
            }
            thread = new Thread(this::run);
            if (check()) {
                thread.start();
                startChildTasks();
            }
        } catch (Exception e) {
            Log.printStackTrace(e);
        }
    }
    public synchronized void stopTask() {
        if (thread != null && thread.isAlive()) {
            ThreadUtil.shutdownAndWait(thread, 5, TimeUnit.SECONDS);
        }
        for (BaseTask childTask : childTaskMap.values()) {
            if (childTask != null) {
                ThreadUtil.shutdownAndWait(childTask.getThread(), -1, TimeUnit.SECONDS);
            }
        }
        stopChildTasks();
        thread = null;
    }
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
