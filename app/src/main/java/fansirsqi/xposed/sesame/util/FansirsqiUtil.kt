package fansirsqi.xposed.sesame.util

import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.Exception
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

object FansirsqiUtil {
    // 定义一言API的URL
    private const val HITOKOTO_API_URL = "https://v1.hitokoto.cn/"
    private val executorService: ExecutorService = Executors.newSingleThreadExecutor() // 创建单线程执行器
    /**
     * 获取一言句子并格式化输出。
     *
     * @param callback 回调接口，用于返回获取到的句子
     */
    fun getOneWord(callback: OneWordCallback) {
        executorService.execute(
            Runnable {
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
                            response.append(line)
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
                    val formattedSentence =
                        String.format("%s\n\n                    -----Re: %s", hitokoto, from)
                    callback.onSuccess(formattedSentence) // 调用回调返回成功结果
                } catch (e: Exception) {
                    val hitokoto = "忘形雨笠烟蓑，知心牧唱樵歌。\n明月清风共我，闲人三个，从他今古消磨。"
                    val from = "天净沙·渔父"
                    val formattedSentence =
                        String.format("%s\n\n                    -----Re: %s", hitokoto, from)
                    Log.printStackTrace(e)
                    callback.onFailure(formattedSentence) // 调用回调返回失败信息
                }
            })
    }
    // 定义回调接口
    interface OneWordCallback {
        fun onSuccess(result: String?) // 成功回调
        fun onFailure(error: String?) // 失败回调
    }

}
