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

    public BaseTask() {
        this.thread = null;
    }

    public String getId() {
        return toString();
    }

    public abstract Boolean check();

    public abstract void run();

    public synchronized Boolean hasChildTask(String childId) {
        return childTaskMap.containsKey(childId);
    }

    public synchronized BaseTask getChildTask(String childId) {
        return childTaskMap.get(childId);
    }

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

    public synchronized Integer countChildTask() {
        return childTaskMap.size();
    }

    public void startTask() {
        startTask(false);
    }

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
