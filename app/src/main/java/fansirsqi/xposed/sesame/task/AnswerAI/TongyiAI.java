package fansirsqi.xposed.sesame.task.AnswerAI;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import fansirsqi.xposed.sesame.util.JsonUtil;
import fansirsqi.xposed.sesame.util.Log;
import lombok.Getter;
import lombok.Setter;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * @author Byseven
 * @date 2025/1/30
 * @apiNote
 */
public class TongyiAI implements AnswerAIInterface {

    private final String TAG = TongyiAI.class.getSimpleName();

    private static final String URL = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";
    private static final String CONTENT_TYPE = "application/json";
    @Getter
    @Setter
    private String modelName = "qwen-turbo";
    private static final String JSON_PATH = "choices.[0].message.content";
    private final String token;

    public TongyiAI(String token) {
        if (token != null && !token.isEmpty()) {
            this.token = token;
        } else {
            this.token = "";
        }
    }

    /**
     * 获取AI回答结果
     *
     * @param text 问题内容
     * @return AI回答结果
     */
    @Override
    public String getAnswerStr(String text) {
        String result = "";
        Response response = null;
        try {
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS) // 设置连接超时时间为 30 秒
                    .writeTimeout(30, TimeUnit.SECONDS)   // 设置写超时时间为 30 秒
                    .readTimeout(30, TimeUnit.SECONDS)    // 设置读超时时间为 30 秒
                    .build();
            JSONObject contentObject = new JSONObject();
            contentObject.put("role", "user");
            contentObject.put("content", text);
            JSONArray messageArray = new JSONArray();
            messageArray.put(contentObject);
            JSONObject bodyObject = new JSONObject();
            bodyObject.put("model", this.modelName);
            bodyObject.put("messages", messageArray);
            RequestBody body = RequestBody.create(bodyObject.toString(), MediaType.parse(CONTENT_TYPE));
            Request request = new Request.Builder()
                    .url(URL)
                    .method("POST", body)
                    .addHeader("Authorization", "Bearer " + token)
                    .addHeader("Content-Type", CONTENT_TYPE)
                    .build();
            response = client.newCall(request).execute();
            if (response.body() == null) {
                return result;
            }
            String json = response.body().string();
            if (!response.isSuccessful()) {
                Log.other("Tongyi请求失败");
                Log.record("Tongyi接口异常：" + json);
                return result;
            }
            JSONObject jsonObject = new JSONObject(json);
            result = JsonUtil.getValueByPath(jsonObject, JSON_PATH);
        } catch (JSONException | IOException e) {
            Log.printStackTrace(TAG, e);
        } finally {
            if (response != null) {
                response.close();
            }
        }
        return result;
    }


    @Override
    public String getAnswerStr(String text, String model) {
        setModelName(model);
        return getAnswerStr(text);
    }

    /**
     * 获取答案
     *
     * @param title     问题
     * @param answerList 答案集合
     * @return 空没有获取到
     */
    @Override
    public Integer getAnswer(String title, List<String> answerList) {
        int size = answerList.size();
        StringBuilder answerStr = new StringBuilder();
        for (int i = 0; i < size; i++) {
            answerStr.append(i + 1).append(".[").append(answerList.get(i)).append("]\n");
        }
        String answerResult = getAnswerStr("问题：" + title + "\n\n" + "答案列表：\n\n" + answerStr + "\n\n" + "请只返回答案列表中的序号");
        if (answerResult != null && !answerResult.isEmpty()) {
            try {
                int index = Integer.parseInt(answerResult) - 1;
                if (index >= 0 && index < size) {
                    return index;
                }
            } catch (Exception e) {
                Log.record("AI🧠回答，返回数据：" + answerResult);
            }
            for (int i = 0; i < size; i++) {
                if (answerResult.contains(answerList.get(i))) {
                    return i;
                }
            }
        }
        return -1;
    }

}

