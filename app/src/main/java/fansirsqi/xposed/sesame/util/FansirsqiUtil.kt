package fansirsqi.xposed.sesame.util

import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.Exception
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import fansirsqi.xposed.sesame.util.GlobalThreadPools

object FansirsqiUtil {
    // 定义一言API的URL
    private const val HITOKOTO_API_URL = "https://v1.hitokoto.cn/"

    /**
     * 获取一言句子并格式化输出。
     *
     * @param callback 回调接口，用于返回获取到的句子
     */
    fun getOneWord(callback: OneWordCallback) {
        Thread {
            val response = StringBuilder()
            try {
                // 创建 URL 对象并打开连接
                val connection = URL(HITOKOTO_API_URL).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000 // 设置连接超时时间
                connection.readTimeout = 5000 // 设置读取超时时间

                BufferedReader(InputStreamReader(connection.getInputStream())).use { reader ->
                    var line: String?
                    while ((reader.readLine().also { line = it }) != null) {
                        line?.let { response.append(it) }
                    }
                }

                // 解析 JSON 数据
                val jsonObject = JSONObject(response.toString())
                val hitokoto = jsonObject.optString(
                    "hitokoto",
                    "忘形雨笠烟蓑，知心牧唱樵歌。\n明月清风共我，闲人三个，从他今古消磨。"
                )
                val from = jsonObject.optString("from", "天净沙·渔父")

                // 格式化输出句子
                val formattedSentence = "$hitokoto\n\n                    -----Re: $from"

                // 回调成功结果（注意线程切换）
                Handler(Looper.getMainLooper()).post {
                    callback.onSuccess(formattedSentence)
                }

            } catch (e: Exception) {
                val hitokoto = "忘形雨笠烟蓑，知心牧唱樵歌。\n明月清风共我，闲人三个，从他今古消磨。"
                val from = "天净沙·渔父"
                val formattedSentence = "$hitokoto\n\n                    -----Re: $from"

                Log.printStackTrace(e)
                // 回调失败结果（注意线程切换）
                Handler(Looper.getMainLooper()).post {
                    callback.onFailure(formattedSentence)
                }
            }
        }.start()
    }

    // 定义回调接口
    interface OneWordCallback {
        fun onSuccess(result: String?) // 成功回调
        fun onFailure(error: String?) // 失败回调
    }

    /**
     * 生成随机字符串
     * @param length 字符串长度
     */
    fun getRandomString(length: Int): String {
        val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        return (1..length)
            .map { kotlin.random.Random.nextInt(0, charPool.size) }
            .map(charPool::get)
            .joinToString("")
    }

}
