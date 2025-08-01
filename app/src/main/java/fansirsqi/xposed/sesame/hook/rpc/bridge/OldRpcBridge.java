package fansirsqi.xposed.sesame.hook.rpc.bridge;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import fansirsqi.xposed.sesame.data.General;
import fansirsqi.xposed.sesame.data.RuntimeInfo;
import fansirsqi.xposed.sesame.entity.RpcEntity;
import fansirsqi.xposed.sesame.hook.ApplicationHook;
import fansirsqi.xposed.sesame.hook.rpc.intervallimit.RpcIntervalLimit;
import fansirsqi.xposed.sesame.model.BaseModel;
import fansirsqi.xposed.sesame.util.Log;
import fansirsqi.xposed.sesame.util.Notify;
import fansirsqi.xposed.sesame.util.StringUtil;
import fansirsqi.xposed.sesame.util.TimeUtil;
public class OldRpcBridge implements RpcBridge {
    private static final String TAG = OldRpcBridge.class.getSimpleName();
    private ClassLoader loader;
    private Class<?> h5PageClazz;
    private Method rpcCallMethod;
    private Method getResponseMethod;
    private Object curH5PageImpl;
    @Override
    public RpcVersion getVersion() {
        return RpcVersion.NEW; // 返回 RPC 的版本
    }
    /**
     * 加载 RPC 所需的类和方法。
     */
    public void load() throws Exception {
        loader = ApplicationHook.getClassLoader();
        try {
            h5PageClazz = loader.loadClass(General.H5PAGE_NAME);
            Log.runtime(TAG, "RPC 类加载成功");
            loadRpcMethods(); // 加载 RPC 方法
        } catch (ClassNotFoundException e) {
            Log.runtime(TAG, "加载 RPC 类时出错：");
            Log.printStackTrace(TAG, e);
            throw new RuntimeException(e);
        } catch (Throwable t) {
            Log.runtime(TAG, "加载 RPC 类时发生意外错误：");
            Log.printStackTrace(TAG, t);
            throw t;
        }
    }
    /**
     * 使用反射加载 RPC 方法。
     */
    private void loadRpcMethods() {
        if (rpcCallMethod == null) {
            try {
                Class<?> rpcUtilClass = loader.loadClass("com.alipay.mobile.nebulaappproxy.api.rpc.H5RpcUtil");
                Class<?> responseClass = loader.loadClass("com.alipay.mobile.nebulaappproxy.api.rpc.H5Response");
                rpcCallMethod = rpcUtilClass.getMethod("rpcCall", String.class, String.class, String.class,
                        boolean.class, loader.loadClass(General.JSON_OBJECT_NAME), String.class,
                        boolean.class, h5PageClazz, int.class, String.class, boolean.class, int.class, String.class);
                getResponseMethod = responseClass.getMethod("getResponse");
                Log.runtime(TAG, "RPC 调用方法加载成功");
            } catch (Exception e) {
                Log.runtime(TAG, "加载 RPC 调用方法时出错：");
                Log.printStackTrace(TAG, e);
            }
        }
    }
    @Override
    public void unload() {
        getResponseMethod = null; // 清空响应方法
        rpcCallMethod = null; // 清空调用方法
        h5PageClazz = null; // 清空 H5 页面类
        loader = null; // 清空类加载器
    }
    /**
     * 向 RPC 实体请求字符串响应。
     *
     * @param rpcEntity     要发送的 RPC 实体。
     * @param tryCount      重试次数。
     * @param retryInterval  重试间隔。
     * @return 响应字符串，如果失败则返回 null。
     */
    public String requestString(RpcEntity rpcEntity, int tryCount, int retryInterval) {
        RpcEntity responseEntity = requestObject(rpcEntity, tryCount, retryInterval);
        return responseEntity != null ? responseEntity.getResponseString() : null; // 返回响应字符串或 null
    }
    @Override
    public RpcEntity requestObject(RpcEntity rpcEntity, int tryCount, int retryInterval) {
        if (ApplicationHook.isOffline()) {
            return null; // 如果离线，直接返回 null
        }
        int id = rpcEntity.hashCode(); // 获取请求 ID
        String method = rpcEntity.getRequestMethod(); // 获取请求方法
        String args = rpcEntity.getRequestData(); // 获取请求参数
        for (int count = 0; count < tryCount; count++) {
            try {
                RpcIntervalLimit.INSTANCE.enterIntervalLimit(method); // 进入 RPC 调用间隔限制
                Object response = invokeRpcCall(method, args); // 调用 RPC 方法
                return processResponse(rpcEntity, response, id, method, args, retryInterval); // 处理响应
            } catch (Throwable t) {
                handleError(rpcEntity, t, method, id, args); // 处理错误
            }
        }
        return null; // 所有尝试失败后返回 null
    }
    /**
     * 使用反射调用 RPC 方法。
     *
     * @param method 请求的方法名。
     * @param args   请求的参数。
     * @return 响应对象。
     * @throws Throwable 如果调用过程中出现错误。
     */
    private Object invokeRpcCall(String method, String args) throws Throwable {
        if (rpcCallMethod.getParameterTypes().length == 12) {
            return rpcCallMethod.invoke(null, method, args, "", true, null, null, false, curH5PageImpl, 0, "", false, -1);
        } else {
            return rpcCallMethod.invoke(null, method, args, "", true, null, null, false, curH5PageImpl, 0, "", false, -1, "");
        }
    }
    /**
     * 处理 RPC 响应。
     *
     * @param rpcEntity   要更新的 RPC 实体。
     * @param response    响应对象。
     * @param id          唯一请求 ID。
     * @param method      请求的方法名。
     * @param args        请求的参数。
     * @param retryInterval 重试间隔。
     * @return 更新后的 RPC 实体。
     * @throws Throwable 如果处理过程中出现错误。
     */
    private RpcEntity processResponse(RpcEntity rpcEntity, Object response, int id, String method, String args, int retryInterval) throws Throwable {
        String resultStr = (String) getResponseMethod.invoke(response); // 获取响应字符串
        JSONObject resultObject = new JSONObject(resultStr);
        rpcEntity.setResponseObject(resultObject, resultStr); // 设置响应对象
        // 检查响应中的 "memo" 字段是否包含 "系统繁忙"
        if (resultObject.optString("memo", "").contains("系统繁忙")) {
            ApplicationHook.setOffline(true); // 设置为离线状态
            Notify.updateStatusText("系统繁忙，可能需要滑动验证");
            Log.record(TAG,"系统繁忙，可能需要滑动验证");
            return null; // 返回 null
        }
        if (!resultObject.optBoolean("success")) {
            rpcEntity.setError(); // 设置为错误状态
            Log.error(TAG,"旧 RPC 响应 | id: " + id + " | method: " + method + " args: " + args + " | data: " + rpcEntity.getResponseString());
        }
        return rpcEntity; // 返回更新后的 RPC 实体
    }
    /**
     * 处理 RPC 请求过程中发生的错误。
     *
     * @param rpcEntity 要更新的 RPC 实体。
     * @param t        发生的异常。
     * @param method   请求的方法名。
     * @param id       唯一请求 ID。
     * @param args     请求的参数。
     */
    private void handleError(RpcEntity rpcEntity, Throwable t, String method, int id, String args) {
        rpcEntity.setError(); // 设置为错误状态
        Log.error(TAG,"旧 RPC 请求 | id: " + id + " | method: " + method + " err:");
        Log.printStackTrace(t); // 打印堆栈跟踪
        if (t instanceof InvocationTargetException) {
            handleInvocationException(rpcEntity, (InvocationTargetException) t, method); // 处理调用异常
        }
    }
    /**
     * 处理调用过程中的特定异常。
     *
     * @param rpcEntity 要更新的 RPC 实体。
     * @param e        发生的 InvocationTargetException。
     * @param method   请求的方法名。
     */
    private void handleInvocationException(RpcEntity rpcEntity, InvocationTargetException e, String method) {
        Throwable cause = e.getCause();
        if (cause != null) {
            String msg = cause.getMessage();
            if (!StringUtil.isEmpty(msg)) {
                handleErrorMessage(rpcEntity, msg, method); // 处理错误消息
            }
        }
    }
    /**
     * 处理特定的错误消息，并根据内容执行相应的操作。
     *
     * @param rpcEntity 要更新的 RPC 实体。
     * @param msg      错误消息。
     * @param method   请求的方法名。
     */
    private void handleErrorMessage(RpcEntity rpcEntity, String msg, String method) {
        if (msg.contains("登录超时")) {
            handleLoginTimeout(); // 处理登录超时
        } else if (msg.contains("[1004]") && "alipay.antmember.forest.h5.collectEnergy".equals(method)) {
            handleEnergyCollectException(); // 处理能量收集异常
        } else if (msg.contains("MMTPException")) {
            handleMmtpException(rpcEntity); // 处理 MMTP 异常
        }
    }
    /**
     * 处理登录超时的情况。
     */
    private void handleLoginTimeout() {
        if (!ApplicationHook.isOffline()) {
            ApplicationHook.setOffline(true);
            Notify.updateStatusText("登录超时");
            if (BaseModel.getTimeoutRestart().getValue()) {
                Log.record(TAG,"尝试重新登录");
                ApplicationHook.reLoginByBroadcast();
            }
        }
    }
    /**
     * 处理能量收集异常的情况。
     */
    private void handleEnergyCollectException() {
        if (BaseModel.getWaitWhenException().getValue() > 0) {
            long waitTime = System.currentTimeMillis() + BaseModel.getWaitWhenException().getValue();
            RuntimeInfo.getInstance().put(RuntimeInfo.RuntimeInfoKey.ForestPauseTime, waitTime);
            Notify.updateStatusText("异常");
            Log.record(TAG,"触发异常, 等待至" + TimeUtil.getCommonDate(waitTime));
        }
    }
    /**
     * 处理 MMTP 异常的情况。
     *
     * @param rpcEntity 要更新的 RPC 实体。
     */
    private void handleMmtpException(RpcEntity rpcEntity) {
        try {
            String jsonString;
            JSONObject jo = new JSONObject();
            jo.put("resultCode", "FAIL");
            jo.put("memo", "MMTPException");
            jo.put("resultDesc", "MMTPException");
            jsonString = jo.toString();
            rpcEntity.setResponseObject(new JSONObject(jsonString), jsonString); // 设置 MMTP 异常响应
        } catch (JSONException e) {
            Log.printStackTrace(e); // 打印异常信息
        }
    }
}
