package fansirsqi.xposed.sesame.util;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONObject;
public class FansirsqiUtil {
  // 定义一言API的URL
  private static final String HITOKOTO_API_URL = "https://v1.hitokoto.cn/";
  private static final ExecutorService executorService = Executors.newSingleThreadExecutor(); // 创建单线程执行器
  /**
   * 获取一言句子并格式化输出。
   *
   * @param callback 回调接口，用于返回获取到的句子
   */
  public static void getOneWord(final OneWordCallback callback) {
    executorService.execute(
        () -> {
          StringBuilder response = new StringBuilder();
          try {
            // 创建 URL 对象并打开连接
            HttpURLConnection connection = (HttpURLConnection) new URL(HITOKOTO_API_URL).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000); // 设置连接超时时间
            connection.setReadTimeout(5000); // 设置读取超时时间
            // 读取响应数据
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
              String line;
              while ((line = reader.readLine()) != null) {
                response.append(line);
              }
            }
            // 解析 JSON 数据
            JSONObject jsonObject = new JSONObject(response.toString());
            String hitokoto = jsonObject.optString("hitokoto", "忘形雨笠烟蓑，知心牧唱樵歌。\n明月清风共我，闲人三个，从他今古消磨。");
            String from = jsonObject.optString("from", "天净沙·渔父");
            // 格式化输出句子
            String formattedSentence = String.format("%s\n\n                    -----Re: %s", hitokoto, from);
            callback.onSuccess(formattedSentence); // 调用回调返回成功结果
          } catch (Exception e) {
            String hitokoto = "忘形雨笠烟蓑，知心牧唱樵歌。\n明月清风共我，闲人三个，从他今古消磨。";
            String from = "天净沙·渔父";
            String formattedSentence = String.format("%s\n\n                    -----Re: %s", hitokoto, from);
            Log.printStackTrace(e);
            callback.onFailure(formattedSentence); // 调用回调返回失败信息
          }
        });
  }
  // 定义回调接口
  public interface OneWordCallback {
    void onSuccess(String result); // 成功回调
    void onFailure(String error); // 失败回调
  }
}
