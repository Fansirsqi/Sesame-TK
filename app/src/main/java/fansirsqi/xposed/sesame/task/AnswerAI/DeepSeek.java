/**
 * @author Byseven
 * @date 2025/1/30
 * @apiNote
 */

package fansirsqi.xposed.sesame.task.AnswerAI;

import static fansirsqi.xposed.sesame.util.JsonUtil.getValueByPath;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import fansirsqi.xposed.sesame.util.Log;
import lombok.Getter;
import lombok.Setter;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


/**
 * DeepSeek帮助类，用于与DeepSeek接口交互以获取AI回答
 * 支持单条文本问题及带有候选答案列表的问题请求
 */
public class DeepSeek implements AnswerAIInterface {
    private static final String TAG = DeepSeek.class.getSimpleName();
    private static final String BASE_URL = "https://api.deepseek.com/v1/chat/completions";
    private static final String CONTENT_TYPE = "application/json";
    private static final String JSON_PATH = "choices.[0].message.content";
    private static final String SYSTEM_MESSAGE = "你是一个拥有丰富的知识，并且能根据知识回答问题的专家。";
    private static final String AUTH_HEADER_PREFIX = "Bearer ";
    private static final Integer TIME_OUT_SECONDS = 180;

    @Setter
    @Getter
    private String modelName = "deepseek-reasoner"; //"deepseek-chat";

    private final String apiKey;

    // 构造函数，初始化API Key
    public DeepSeek(String apiKey) {
        this.apiKey = apiKey != null && !apiKey.isEmpty() ? apiKey : "";
    }

    // 移除控制字符
    private String removeControlCharacters(String text) {
        return text.replaceAll("\\p{Cntrl}&&[^\n" + "\t]", "");
    }

    // 构建请求体的JSON对象
    private JSONObject buildRequestJson(String text) throws JSONException {
        text = removeControlCharacters(text);
        JSONObject requestJson = new JSONObject();
        requestJson.put("model", this.modelName);

        JSONArray messages = new JSONArray();
        JSONObject systemMessage = new JSONObject();
        systemMessage.put("role", "system");
        systemMessage.put("content", SYSTEM_MESSAGE);
        messages.put(systemMessage);

        JSONObject userMessage = new JSONObject();
        userMessage.put("role", "user");
        userMessage.put("content", text);
        messages.put(userMessage);

        requestJson.put("messages", messages);
        requestJson.put("stream", false);
        return requestJson;
    }

    // 发送请求并处理响应
    private String sendRequest(JSONObject requestJson) throws IOException {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(TIME_OUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(TIME_OUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(TIME_OUT_SECONDS, TimeUnit.SECONDS)
                .build();
        MediaType mediaType = MediaType.parse(CONTENT_TYPE);
        RequestBody body = RequestBody.create(requestJson.toString(), mediaType);
        Request request = new Request.Builder()
                .url(BASE_URL)
                .method("POST", body)
                .addHeader("Content-Type", CONTENT_TYPE)
                .addHeader("Authorization", AUTH_HEADER_PREFIX + apiKey)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.body() == null) {
                return "";
            }
            String json = response.body().string();
            if (!response.isSuccessful()) {
                Log.other("DeepSeek请求失败");
                Log.runtime("DeepSeek接口异常：" + json);
                return "";
            }
            return json;
        }
    }


    @Override
    public String getAnswerStr(String text, String model) {
        setModelName(model);
        return getAnswerStr(text);
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
        try {
            JSONObject requestJson = buildRequestJson(text);
            String jsonResponse = sendRequest(requestJson);
            if (!jsonResponse.isEmpty()) {
                JSONObject jsonObject = new JSONObject(jsonResponse);
                result = getValueByPath(jsonObject, JSON_PATH);
            }
        } catch (IOException | org.json.JSONException e) {
            Log.printStackTrace(TAG, e);
        }
        return result;
    }

    /**
     * 获取答案
     *
     * @param title      问题
     * @param answerList 答案集合
     * @return 空没有获取到
     */
    @Override
    public Integer getAnswer(String title, List<String> answerList) {
        StringBuilder answerStr = new StringBuilder();
        for (String answer : answerList) {
            answerStr.append("[").append(answer).append("]");
        }
        String answerResult = getAnswerStr(title + "\n" + answerStr);
        if (answerResult != null && !answerResult.isEmpty()) {
            for (int i = 0; i < answerList.size(); i++) {
                if (answerResult.contains(answerList.get(i))) {
                    return i;
                }
            }
        }
        return -1;
    }
}