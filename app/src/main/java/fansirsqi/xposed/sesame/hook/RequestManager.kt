package fansirsqi.xposed.sesame.hook

import android.Manifest
import androidx.annotation.RequiresPermission
import fansirsqi.xposed.sesame.entity.RpcEntity
import fansirsqi.xposed.sesame.hook.rpc.bridge.RpcBridge
import fansirsqi.xposed.sesame.util.Log
import fansirsqi.xposed.sesame.util.NetworkUtils
import org.json.JSONObject

/**
 * @author Byseven
 * @date 2025/1/6
 * @apiNote
 */
object RequestManager {
    private const val TAG = "RequestManager"
    
    /**
     * 检查并处理RPC响应结果
     * 集成RpcErrorHandler进行错误统计和退避控制
     */
    private fun checkResult(result: String?, method: String?): String {
        val methodName = method ?: "unknown"
        
        // 检查接口是否被暂停（1009错误退避）
        if (RpcErrorHandler.isApiSuspended(methodName)) {
            Log.debug(TAG, "接口[$methodName]被暂停中，跳过调用")
            return ""
        }
        
        // 处理 null 返回值
        if (result == null) {
            Log.runtime(TAG, "RPC 返回 null: $methodName")
            RpcErrorHandler.recordApiFailure(methodName)
            return ""
        }
        
        // 检查是否为空字符串
        if (result.trim { it <= ' ' }.isEmpty()) {
            Log.runtime(TAG, "RPC 返回空字符串: $methodName")
            RpcErrorHandler.recordApiFailure(methodName)
            return ""
        }
        
        // 尝试解析响应，检查错误码
        try {
            val jo = JSONObject(result)
            if (jo.has("error")) {
                val errorCode = jo.optString("error")
                when (errorCode) {
                    "1009" -> {
                        Log.error(TAG, "接口[$methodName]触发1009错误，暂停10分钟")
                        RpcErrorHandler.recordApiFailure(methodName, 1009)
                        return ""
                    }
                    "1004" -> {
                        Log.error(TAG, "接口[$methodName]触发1004错误（系统繁忙）")
                        RpcErrorHandler.recordApiFailure(methodName, 1004)
                        return result // 1004返回结果，由上层处理
                    }
                    else -> {
                        if (errorCode.isNotEmpty()) {
                            Log.error(TAG, "接口[$methodName]返回错误: $errorCode")
                            RpcErrorHandler.recordApiFailure(methodName)
                        }
                    }
                }
            } else {
                // 无错误，记录成功
                RpcErrorHandler.recordApiSuccess(methodName)
            }
        } catch (e: Exception) {
            // JSON解析失败，可能不是JSON格式或格式不标准，视为成功
            RpcErrorHandler.recordApiSuccess(methodName)
        }
        
        return result
    }

    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    private fun getRpcBridge(): RpcBridge? {
        if (!NetworkUtils.isNetworkAvailable()) {
            Log.record("网络未连接，等待5秒")
            fansirsqi.xposed.sesame.util.CoroutineUtils.sleepCompat(5000)
            if (!NetworkUtils.isNetworkAvailable()) {
                val networkType = NetworkUtils.getNetworkType()
                Log.record("网络仍未连接，当前网络类型: $networkType，放弃本次请求...")
                return null
            }
        }
        var rpcBridge = ApplicationHook.rpcBridge
        if (rpcBridge == null) {
            Log.record("ApplicationHook.rpcBridge 为空，等待5秒")
            fansirsqi.xposed.sesame.util.CoroutineUtils.sleepCompat(5000)
            rpcBridge = ApplicationHook.rpcBridge
        }
        return rpcBridge
    }

    @JvmStatic
    fun requestString(rpcEntity: RpcEntity): String {
        val rpcBridge = getRpcBridge() ?: return ""
        val result = rpcBridge.requestString(rpcEntity, 3, 1200)
        return checkResult(result, rpcEntity.methodName)
    }

    @JvmStatic
    fun requestString(rpcEntity: RpcEntity, tryCount: Int, retryInterval: Int): String {
        val rpcBridge = getRpcBridge() ?: return ""
        val result = rpcBridge.requestString(rpcEntity, tryCount, retryInterval)
        return checkResult(result, rpcEntity.methodName)
    }

    @JvmStatic
    fun requestString(method: String?, data: String?): String {
        val rpcBridge = getRpcBridge() ?: return ""
        val result = rpcBridge.requestString(method, data)
        return checkResult(result, method)
    }

    @JvmStatic
    fun requestString(method: String?, data: String?, relation: String?): String {
        val rpcBridge = getRpcBridge() ?: return ""
        val result = rpcBridge.requestString(method, data, relation)
        return checkResult(result, method)
    }

    @JvmStatic
    fun requestString(method: String?, data: String?, appName: String?, methodName: String?, facadeName: String?): String {
        val rpcBridge = getRpcBridge() ?: return ""
        val result = rpcBridge.requestString(method, data, appName, methodName, facadeName)
        return checkResult(result, method)
    }

    @JvmStatic
    fun requestString(method: String?, data: String?, tryCount: Int, retryInterval: Int): String {
        val rpcBridge = getRpcBridge() ?: return ""
        val result = rpcBridge.requestString(method, data, tryCount, retryInterval)
        return checkResult(result, method)
    }

    fun requestString(method: String?, data: String?, relation: String?, tryCount: Int, retryInterval: Int): String {
        val rpcBridge = getRpcBridge() ?: return ""
        val result = rpcBridge.requestString(method, data, relation, tryCount, retryInterval)
        return checkResult(result, method)
    }

    @JvmStatic
    fun requestObject(rpcEntity: RpcEntity?, tryCount: Int, retryInterval: Int) {
        val rpcBridge = getRpcBridge() ?: return
        rpcBridge.requestObject(rpcEntity, tryCount, retryInterval)
    }

}
