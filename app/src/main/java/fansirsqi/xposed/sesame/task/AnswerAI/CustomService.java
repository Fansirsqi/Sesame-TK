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

public class CustomService implements AnswerAIInterface {
    private static final String TAG = CustomService.class.getSimpleName();
    private static final String CONTENT_TYPE = "application/json";
    private static final String JSON_PATH = "choices.[0].message.content";
    private static final String SYSTEM_MESSAGE = "你是一个拥有丰富的知识，并且能根据知识回答问题的专家。";
    private static final String AUTH_HEADER_PREFIX = "Bearer ";
    private static final Integer TIME_OUT_SECONDS = 180;

    private final String apiKey;
    private final String baseUrl;
    @Setter
    @Getter
    private String modelName = "gpt-3.5-turbo"; // 默认模型

    public CustomService(String apiKey, String baseUrl) {
        this.apiKey = apiKey != null && !apiKey.isEmpty() ? apiKey : "";
        this.baseUrl = baseUrl != null && !baseUrl.isEmpty() ? baseUrl : "https://api.openai.com/v1";
    }

    private JSONObject buildRequestJson(String text) throws JSONException {
        JSONObject requestJson = new JSONObject();
        requestJson.put("model", modelName);

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

    private String sendRequest(JSONObject requestJson) throws IOException {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(TIME_OUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(TIME_OUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(TIME_OUT_SECONDS, TimeUnit.SECONDS)
                .build();

        String url = baseUrl + "/chat/completions";
        MediaType mediaType = MediaType.parse(CONTENT_TYPE);
        RequestBody body = RequestBody.create(requestJson.toString(), mediaType);
        Request request = new Request.Builder()
                .url(url)
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
                Log.other("CustomService请求失败");
                Log.runtime("CustomService接口异常：" + json);
                return "";
            }
            return json;
        }
    }

    @Override
    public String getAnswerStr(String text) {
        String result = "";
        try {
            JSONObject requestJson = buildRequestJson(text);
            String jsonResponse = sendRequest(requestJson);
            if (!jsonResponse.isEmpty()) {
                JSONObject jsonObject = new JSONObject(jsonResponse);
                result = JsonUtil.getValueByPath(jsonObject, JSON_PATH);
            }
        } catch (IOException | JSONException e) {
            Log.printStackTrace(TAG, e);
        }
        return result;
    }

    @Override
    public String getAnswerStr(String text, String model) {
        setModelName(model);
        return getAnswerStr(text);
    }

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