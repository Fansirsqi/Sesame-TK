package fansirsqi.xposed.sesame.hook

import fansirsqi.xposed.sesame.util.Log
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

/**
 * 用于发送Hook数据到DEBUG服务器
 * @author Byseven
 * @date 2025/1/17
 * @apiNote
 */
object HookSender {
    private const val TAG = "HookSender"
    var sendFlag: Boolean = true
    private val client = OkHttpClient()

    private val JSON_MEDIA_TYPE: MediaType? = "application/json; charset=utf-8".toMediaType()

    fun sendHookData(jo: JSONObject, url: String) {
        try {
            val body: RequestBody = jo.toString().toRequestBody(JSON_MEDIA_TYPE)
            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (!sendFlag) {//避免过多冗余失败记录
                        Log.error(TAG, "Failed to send hook data: ${e.message}")
                        sendFlag = false
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    if (!response.isSuccessful) Log.error(TAG, "Failed to receive response: $response")
//                    } else {
//                        Log.runtime(TAG, "Hook data sent successfully.")
//                    }
                }
            })
        } catch (_: Exception) {
        }
    }
}