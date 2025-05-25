package fansirsqi.xposed.sesame.hook;
import org.json.JSONException;
import org.json.JSONObject;

import fansirsqi.xposed.sesame.entity.RpcEntity;

/**
 * @author Byseven
 * @date 2025/1/6
 * @apiNote
 */
public class RequestManager {

    private static String checkResult(String result, String method) {
        if (result == null || result.trim().isEmpty()) {
            throw new IllegalStateException("Empty response from RPC method: " + method);
        }
        return result;
    }

    private static JSONObject checkJsonResult(String result, String method) throws JSONException {
        if (result == null || result.trim().isEmpty()) {
            throw new IllegalStateException("Empty response from RPC method: " + method);
        }
        return new JSONObject(result);
    }

    public static String requestString(RpcEntity rpcEntity) {
        String result = ApplicationHook.rpcBridge.requestString(rpcEntity, 3, -1);
        return checkResult(result, rpcEntity.getMethodName());
    }
    public static String requestString(RpcEntity rpcEntity, int tryCount, int retryInterval) {
        String result = ApplicationHook.rpcBridge.requestString(rpcEntity, tryCount, retryInterval);
        return checkResult(result, rpcEntity.getMethodName());
    }
    public static String requestString(String method, String data) {
        String result = ApplicationHook.rpcBridge.requestString(method, data);
        return checkResult(result, method);
    }

    public static JSONObject requestJson(String method, String data) throws JSONException {
        String result = ApplicationHook.rpcBridge.requestString(method, data);
        return checkJsonResult(result, method);
    }

    public static String requestString(String method, String data, String relation) {
        String result = ApplicationHook.rpcBridge.requestString(method, data, relation);
        return checkResult(result, method);
    }
    public static String requestString(String method, String data, String appName, String methodName, String facadeName) {
        String result = ApplicationHook.rpcBridge.requestString(method, data, appName, methodName, facadeName);
        return checkResult(result, method);
    }
    public static String requestString(String method, String data, int tryCount, int retryInterval) {
        String result = ApplicationHook.rpcBridge.requestString(method, data, tryCount, retryInterval);
        return checkResult(result, method);
    }
    public static String requestString(String method, String data, String relation, int tryCount, int retryInterval) {
        String result = ApplicationHook.rpcBridge.requestString(method, data, relation, tryCount, retryInterval);
        return checkResult(result, method);
    }

    public static void requestObject(RpcEntity rpcEntity, int tryCount, int retryInterval) {
        ApplicationHook.rpcBridge.requestObject(rpcEntity, tryCount, retryInterval);
    }

    public static RpcEntity requestObject(String method, String data, int tryCount, int retryInterval) {
        return ApplicationHook.rpcBridge.requestObject(method, data, tryCount, retryInterval);
    }

    /*
    public static JSONObject requestString(String str, String str2, boolean z) throws JSONException {
        JSONObject requestStringAll = requestStringAll(str, str2);
        if (1009 == requestStringAll.optInt("error")) {
            throw new IllegalStateException(requestStringAll.optString("errorMessage"));
        }
        if (requestStringAll.optBoolean("success", false)) {
            return requestStringAll;
        }
        if (z) {
            Log.error(".requestString err " + str + "\r\n参数：" + (str2 == null ? null : "[{" + str2 + "}]") + "\r\n结果：" + requestStringAll);
        }
        return null;
    }

    public static JSONObject requestStringAll(String str, String str2) throws JSONException {
        return new JSONObject(requestString(str, str2 == null ? null : "[{" + str2 + "}]"));
    }

    public static JSONObject requestStringAllNew(String str, String str2) throws JSONException {
        return new JSONObject(requestString(str, str2));
    }

     */

}
