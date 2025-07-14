package fansirsqi.xposed.sesame.hook;
import fansirsqi.xposed.sesame.entity.RpcEntity;
/**
 * @author Byseven
 * @date 2025/1/6
 * @apiNote
 */
public class RequestManager {
    public static String requestString(RpcEntity rpcEntity) {
        return ApplicationHook.rpcBridge.requestString(rpcEntity, 3, -1);
    }
    public static String requestString(RpcEntity rpcEntity, int tryCount, int retryInterval) {
        return ApplicationHook.rpcBridge.requestString(rpcEntity, tryCount, retryInterval);
    }
    public static String requestString(String method, String data) {
        return ApplicationHook.rpcBridge.requestString(method, data);
    }
    public static String requestString(String method, String data, String relation) {
        return ApplicationHook.rpcBridge.requestString(method, data, relation);
    }
    public static String requestString(String method, String data, String appName, String methodName, String facadeName) {
        return ApplicationHook.rpcBridge.requestString(method, data, appName, methodName, facadeName);
    }
    public static String requestString(String method, String data, int tryCount, int retryInterval) {
        return ApplicationHook.rpcBridge.requestString(method, data, tryCount, retryInterval);
    }
    public static String requestString(String method, String data, String relation, int tryCount, int retryInterval) {
        return ApplicationHook.rpcBridge.requestString(method, data, relation, tryCount, retryInterval);
    }
    public static RpcEntity requestObject(RpcEntity rpcEntity) {
        return ApplicationHook.rpcBridge.requestObject(rpcEntity, 3, -1);
    }
    public static void requestObject(RpcEntity rpcEntity, int tryCount, int retryInterval) {
        ApplicationHook.rpcBridge.requestObject(rpcEntity, tryCount, retryInterval);
    }
    public static RpcEntity requestObject(String method, String data) {
        return ApplicationHook.rpcBridge.requestObject(method, data);
    }
    public static RpcEntity requestObject(String method, String data, String relation) {
        return ApplicationHook.rpcBridge.requestObject(method, data, relation);
    }
    public static RpcEntity requestObject(String method, String data, int tryCount, int retryInterval) {
        return ApplicationHook.rpcBridge.requestObject(method, data, tryCount, retryInterval);
    }
    public static RpcEntity requestObject(String method, String data, String relation, int tryCount, int retryInterval) {
        return ApplicationHook.rpcBridge.requestObject(method, data, relation, tryCount, retryInterval);
    }
}
