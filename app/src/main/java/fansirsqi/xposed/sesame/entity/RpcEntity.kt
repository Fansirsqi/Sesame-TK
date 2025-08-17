package fansirsqi.xposed.sesame.entity

import lombok.Getter
import org.json.JSONException
import org.json.JSONObject
import kotlin.concurrent.Volatile

/**
 * 表示一个 RPC（远程过程调用）实体，用于封装请求和响应数据。
 * 提供线程安全的响应和错误标识。
 */
@Getter
class RpcEntity @JvmOverloads constructor(
    val requestMethod: String? = null,
    val requestData: String? = null,
    val requestRelation: String? = null,
    val appName: String? = null,
    val methodName: String? = "taskFeedback",
    val facadeName: String? = null
) {
    @Volatile
    var hasResult = false
    @Volatile
    var hasError = false
    @Volatile
    var responseObject: Any? = null
    @Volatile
    var responseString: String? = null
    /**
     * 设置响应结果并标记请求已完成。
     *
     * @param result    响应的对象
     * @param resultStr 响应的字符串形式
     */
    fun setResponseObject(result: Any?, resultStr: String?) {
        this.hasResult = true // 标记请求有结果
        this.responseObject = result
        this.responseString = resultStr
    }

    /**
     * 标记请求为错误状态。
     */
    fun setError() {
        this.hasError = true // 标记请求发生错误
    }

    @get:Throws(JSONException::class)
    val rpcFullRequestData: String
        /**
         * 获取Rpc请求字符串
         *
         * @return Rpc请求字符串
         * @throws JSONException json解析错误，需要处理
         */
        get() {
            val jo = JSONObject()
            jo.put("__apiCallStartTime", System.currentTimeMillis())
            // [__apiNativeCallId]不传是否有影响，取值又如何获取
            jo.put("apiCallLink", "XRiverNotFound")
            jo.put("appName", this.appName)
            jo.put("execEngine", "XRiver")
            jo.put("facadeName", this.facadeName)
            jo.put("methodName", this.methodName)
            jo.put("operationType", this.requestMethod)
            jo.put("requestData", this.requestData)
            jo.put("relationLocal", this.requestRelation)
            return jo.toString()
        }
}
