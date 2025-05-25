package fansirsqi.xposed.sesame.model;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import fansirsqi.xposed.sesame.data.CompletedKeyEnum;
import fansirsqi.xposed.sesame.hook.RequestManager;
import fansirsqi.xposed.sesame.util.Log;
import fansirsqi.xposed.sesame.util.Maps.Status;

public abstract class BaseCommTask {
    protected String displayName;
    protected CompletedKeyEnum hoursKeyEnum;
    protected CompletedKeyEnum keyEnum;
    protected int executeIntervalInt = 2000;
    protected Map<String, Object> mapHandler = new HashMap<>();
    protected Set<String> friendList = new HashSet<>();
    protected final String TAG = getClass().getSimpleName();

    protected abstract void handle();

    public final void run(int i) {
        run(i, null);
    }

    public final void run(int i, Map<String, Object> map) {
        run(i, map, null);
    }

    public final void run(int i, Map<String, Object> map, Set<String> set) {
        this.mapHandler = map;
        this.executeIntervalInt = i;
        this.friendList = set;
        CompletedKeyEnum completedKeyEnum = this.keyEnum;
        if (completedKeyEnum == null || !Status.getCompletedDay(completedKeyEnum)) {
            CompletedKeyEnum completedKeyEnum2 = this.hoursKeyEnum;
            if (completedKeyEnum2 == null || !Status.getCompletedHours(completedKeyEnum2)) {
                handle();
                CompletedKeyEnum completedKeyEnum3 = this.hoursKeyEnum;
                if (completedKeyEnum3 != null) {
                    Status.setCompletedHours(completedKeyEnum3);
                }
            }
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
        return new JSONObject(RequestManager.requestString(str, str2 == null ? null : "[{" + str2 + "}]"));
    }

    protected JSONObject requestStringAllNew(String str, String str2) throws JSONException {
        return new JSONObject(RequestManager.requestString(str, str2));
    }
}
