package fansirsqi.xposed.sesame.task.otherTask;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import fansirsqi.xposed.sesame.data.Status;
import fansirsqi.xposed.sesame.hook.ApplicationHook;
import fansirsqi.xposed.sesame.util.Log;

public abstract class BaseCommTask {
    protected String displayName;
    protected CompletedKeyEnum hoursKeyEnum;
    protected CompletedKeyEnum keyEnum;
    protected int executeIntervalInt = 2000;
    protected Map<String, Object> mapHandler = new HashMap<>();
    protected Set<String> friendList = new HashSet<>();
    protected final String TAG = getClass().getSimpleName();

    protected abstract void handle() throws JSONException;

    public final void run(int i) throws JSONException {
        run(i, null);
    }

    public final void run(int i, Map<String, Object> map) throws JSONException {
        run(i, map, null);
    }

    public final void run(int i, Map<String, Object> map, Set<String> set) throws JSONException {
        this.mapHandler = map;
        this.executeIntervalInt = i;
        this.friendList = set;

        // 判断是否已完成当天的任务
        if (!isTaskCompletedToday()) {
            handle(); // 执行任务逻辑
            markTaskCompletedToday(); // 标记任务为已完成
        }
    }

    /**
     * 判断任务是否已完成（基于 Status 的状态管理）
     */
    private boolean isTaskCompletedToday() {
        CompletedKeyEnum completedKeyEnum = this.keyEnum;
        if (completedKeyEnum != null && Status.hasFlagToday(completedKeyEnum.name())) {
            return true; // 已完成当天的任务
        }

        CompletedKeyEnum hoursKeyEnum = this.hoursKeyEnum;
        if (hoursKeyEnum != null && Status.hasFlagToday(hoursKeyEnum.name())) {
            return true; // 已完成当前时间段的任务
        }

        return false; // 任务未完成
    }

    /**
     * 标记任务为已完成（基于 Status 的状态管理）
     */
    private void markTaskCompletedToday() {
        CompletedKeyEnum hoursKeyEnum = this.hoursKeyEnum;
        if (hoursKeyEnum != null) {
            Status.setFlagToday(hoursKeyEnum.name()); // 标记时间段任务为已完成
        }
    }





    protected JSONObject requestString(String str, String str2) throws JSONException {
        return requestString(str, str2, true);
    }

    protected JSONObject requestString(String str, String str2, boolean z) throws JSONException {
        JSONObject requestStringAll = requestStringAll(str, str2);
        if (1009 == requestStringAll.optInt("error")) {
            throw new IllegalStateException(requestStringAll.optString("errorMessage"));
        }
        if (requestStringAll.optBoolean("success", false)) {
            return requestStringAll;
        }
        if (z) {
            Log.error(this.TAG + ".requestString err " + str + "\r\n参数：" + (str2 == null ? null : "[{" + str2 + "}]") + "\r\n结果：" + requestStringAll);
        }
        return null;
    }

    protected JSONObject requestStringAll(String str, String str2) throws JSONException {
        return new JSONObject(ApplicationHook.requestString(str, str2 == null ? null : "[{" + str2 + "}]"));
    }

    protected JSONObject requestStringAllNew(String str, String str2) throws JSONException {
        return new JSONObject(ApplicationHook.requestString(str, str2));
    }
}
