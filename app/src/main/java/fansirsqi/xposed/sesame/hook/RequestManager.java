package fansirsqi.xposed.sesame.hook;
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
}
