package fansirsqi.xposed.sesame.hook

import fansirsqi.xposed.sesame.entity.RpcEntity

/**
 * @author Byseven
 * @date 2025/1/6
 * @apiNote
 */
object RequestManager {
    private fun checkResult(result: String, method: String?): String {
        check(!(result.trim { it <= ' ' }.isEmpty())) { "Empty response from RPC method: $method" }
        return result
    }

    @JvmStatic
    fun requestString(rpcEntity: RpcEntity): String {
        val result = ApplicationHook.rpcBridge.requestString(rpcEntity, 3, -1)
        return checkResult(result, rpcEntity.methodName)
    }

    @JvmStatic
    fun requestString(rpcEntity: RpcEntity, tryCount: Int, retryInterval: Int): String {
        val result = ApplicationHook.rpcBridge.requestString(rpcEntity, tryCount, retryInterval)
        return checkResult(result, rpcEntity.methodName)
    }

    @JvmStatic
    fun requestString(method: String?, data: String?): String {
        val result = ApplicationHook.rpcBridge.requestString(method, data)
        return checkResult(result, method)
    }

    @JvmStatic
    fun requestString(method: String?, data: String?, relation: String?): String {
        val result = ApplicationHook.rpcBridge.requestString(method, data, relation)
        return checkResult(result, method)
    }

    @JvmStatic
    fun requestString(method: String?, data: String?, appName: String?, methodName: String?, facadeName: String?): String {
        val result = ApplicationHook.rpcBridge.requestString(method, data, appName, methodName, facadeName)
        return checkResult(result, method)
    }

    @JvmStatic
    fun requestString(method: String?, data: String?, tryCount: Int, retryInterval: Int): String {
        val result = ApplicationHook.rpcBridge.requestString(method, data, tryCount, retryInterval)
        return checkResult(result, method)
    }

    fun requestString(method: String?, data: String?, relation: String?, tryCount: Int, retryInterval: Int): String {
        val result = ApplicationHook.rpcBridge.requestString(method, data, relation, tryCount, retryInterval)
        return checkResult(result, method)
    }

    @JvmStatic
    fun requestObject(rpcEntity: RpcEntity?, tryCount: Int, retryInterval: Int) {
        ApplicationHook.rpcBridge.requestObject(rpcEntity, tryCount, retryInterval)
    }

}
