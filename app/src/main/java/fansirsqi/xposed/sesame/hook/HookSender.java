package fansirsqi.xposed.sesame.hook;
import androidx.annotation.NonNull;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.util.Map;
import fansirsqi.xposed.sesame.model.BaseModel;
import fansirsqi.xposed.sesame.util.Log;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
/**
 * @author Byseven
 * @date 2025/1/17
 * @apiNote
 */
public class HookSender {
    private static final OkHttpClient client = new OkHttpClient();
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");
    public static void sendHookData(Map<String, Object> hookResponse) {
        try {
            JSONObject jo = new JSONObject();
            for (Map.Entry<String, Object> entry : hookResponse.entrySet()) {
                String k = entry.getKey();
                Object v = entry.getValue();
                if (v instanceof String stringValue) {
                    try {
                        JSONObject valueAsJson = new JSONObject(stringValue);
                        jo.put(k, valueAsJson);
                    } catch (JSONException e) {
                        jo.put(k, v);
                    }
                } else {
                    jo.put(k, v);
                }
            }
            RequestBody body = RequestBody.create(jo.toString(), JSON_MEDIA_TYPE);
            Request request = new Request.Builder()
                    .url(BaseModel.getSendHookDataUrl().getValue())
                    .post(body)
                    .build();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.runtime("Failed to send hook data: " + e.getMessage());
                }
                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) {
                    if (!response.isSuccessful()) {
                        Log.error("Failed to receive response: " + response);
                    } else {
                        Log.runtime("Hook data sent successfully.");
                    }
                }
            });
        } catch (Exception e) {
            Log.error("Failed to send hook data: " + e.getMessage());
        }
    }
}
