package fansirsqi.xposed.sesame.task.otherTask;

import androidx.core.app.NotificationCompat;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import fansirsqi.xposed.sesame.data.Status;
import fansirsqi.xposed.sesame.hook.ApplicationHook;
import fansirsqi.xposed.sesame.util.JsonUtil;
import fansirsqi.xposed.sesame.util.Log;
import fansirsqi.xposed.sesame.util.Notify;
import fansirsqi.xposed.sesame.util.TimeUtil;

public class PiXiuFood extends BaseCommTask {
    private final String recallMethod = "alipay.ofpgrowth.ttlc.props.task.recall";

    public PiXiuFood() {
        this.displayName = "天天来财⛩";
        this.hoursKeyEnum = CompletedKeyEnum.PiXiuFood;
    }

    @Override
    protected void handle() throws JSONException {
        homepageQuery();
    }

    private void taskRecall(String str) throws JSONException {
        JSONObject requestString;
        JSONArray jSONArray;

        if (!Status.hasFlagToday(CompletedKeyEnum.PiXiuFoodTask.name()) && (requestString = requestString("alipay.ofpgrowth.ttlc.props.task.recall", "\"activityId\":\"" + str + "\"")) != null && (jSONArray = (JSONArray) JsonUtil.getValueByPathObject(requestString, "data.tasks")) != null) {
            HashMap hashMap = new HashMap();
            hashMap.put("activityId", str);
            for (int i = 0; i < jSONArray.length(); i++) {
                JSONObject jSONObject = jSONArray.getJSONObject(i);
                if (!"DONE".equals(jSONObject.getString(NotificationCompat.CATEGORY_STATUS))) {
                    String string = jSONObject.getString("taskId");
                    String optString = jSONObject.optString("taskDetail");
                    hashMap.put("appletId", string);
                    hashMap.put("stageCode", "signup");
                    OtherTaskRpcCall.appletTrigger(hashMap);
                    TimeUtil.sleep(this.executeIntervalInt);
                    hashMap.put("stageCode", "send");
                    JSONObject jSONObject2 = new JSONObject(OtherTaskRpcCall.appletTrigger(hashMap));
                    if (jSONObject2.getBoolean("success")) {
                        Log.other(this.displayName + "任务完成[" + optString + "]");
                    } else {
                        Log.error(this.TAG, "taskRecall.appletTrigger" + jSONObject2.optString("resultDesc"));
                    }
                }
            }
            Status.setFlagToday(CompletedKeyEnum.PiXiuFoodTask.name());
        }
    }

    private void homepageQuery() throws JSONException {
        JSONObject requestString = null;
        JSONArray jSONArray;
        try {
            requestString = requestString("alipay.ofpgrowth.ttlc.homepage.query", "\"init\":true");
        }catch (Exception e){
            Log.error(this.TAG, "homepageQuery err:");
        }
        if (requestString != null) {
            JSONObject jSONObject = requestString.getJSONObject(ApplicationHook.AlipayBroadcastReceiver.EXTRA_DATA);
            String string = jSONObject.getString("activityId");
            pendingActivityInfo(jSONObject);
            taskRecall(string);
            JSONArray optJSONArray = jSONObject.optJSONArray("boxedLuckyNumbers");
            int i = jSONObject.getInt("maxLuckyNumbersLength");
            int length = optJSONArray == null ? 0 : optJSONArray.length();
            if (length != i) {
                JSONArray jSONArray2 = jSONObject.getJSONArray("piXiuFoods");
                if (jSONArray2.length() != 0) {
                    int min = Math.min(jSONArray2.length(), i - length);
                    for (int i2 = 0; i2 < min; i2++) {
                        JSONObject requestString2 = requestString("alipay.ofpgrowth.ttlc.pixiu.food.use", "\"activityId\":\"" + string + "\",\"piXiuFoodIds\":[\"" + jSONArray2.getJSONObject(i2).getString("id") + "\"]");
                        if (requestString2 != null && (jSONArray = (JSONArray) JsonUtil.getValueByPathObject(requestString2, "data.boxedLuckyNumbersAfter")) != null) {
                            StringBuilder sb = new StringBuilder();
                            for (int i3 = 0; i3 < jSONArray.length(); i3++) {
                                sb.append(jSONArray.getJSONObject(i2).getString("luckyNumber"));
                            }
                            Log.other(this.displayName + "集球成功，当前幸运数字[" + ((Object) sb) + "]");
                        }
                    }
                }
            }
        }
    }

    private void pendingActivityInfo(JSONObject jSONObject) throws JSONException {
        try {
        } finally {
            try {
            } finally {
            }
        }
        if (jSONObject.optBoolean("hasPendingActivity") && jSONObject.has("pendingActivityInfo")) {
            JSONObject jSONObject2 = jSONObject.getJSONObject("pendingActivityInfo");
            if (jSONObject2.optBoolean("hasWinning")) {
                String valueByPath = JsonUtil.getValueByPath(jSONObject2, "prizeAmountTotal.amount");
                Log.other(this.displayName + "中奖啦[" + jSONObject2.optString("activityName") + "]" + valueByPath + "元");
                if (requestString("alipay.ofpgrowth.ttlc.props.task.recall", "\"activityId\":\"\"") != null && requestString("alipay.ofpgrowth.ttlc.prize.award", "\"activityId\":\"" + jSONObject2.getString("activityId") + "\"") != null) {
                    String str = this.displayName + "领奖成功" + valueByPath + "元，红包三天有效，请尽快使用哟~";
                    String title = "[天天来财]";
                    int id = 114710;
                    Notify.sendNewNotification(ApplicationHook.getContext(), title,str,id);
                    Log.other(str);
                }
            }
        }
    }
}
