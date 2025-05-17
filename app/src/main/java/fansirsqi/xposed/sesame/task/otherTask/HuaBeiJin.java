package fansirsqi.xposed.sesame.task.otherTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import fansirsqi.xposed.sesame.hook.RequestManager;
import fansirsqi.xposed.sesame.util.JsonUtil;
import fansirsqi.xposed.sesame.util.Log;
import fansirsqi.xposed.sesame.util.RandomUtil;
import fansirsqi.xposed.sesame.util.TimeUtil;

public class HuaBeiJin extends BaseCommTask {
    private final String productCode = "";
    private final String sceneCode = "NUGGETS_HUABEI_GOLDEN_POINT";

    public HuaBeiJin() {
        this.displayName = "花呗金💴";
        //this.hoursKeyEnum = CompletedKeyEnum.HuaBeiJin;
    }

    @Override
    protected void handle() {
        queryV2();
    }

    private void queryV2() {
        TimeUtil.sleep(RandomUtil.nextInt(1000,3000));
        JSONObject requestString2;
        JSONArray jSONArray;
        try {
            String res = RequestManager.requestString("com.alipay.pcreditbfweb.sdk.task.queryV2",
                    "[{\"requestFrom\": \"pccp\",\"scene\":\"NUGGETS_HUABEI_GOLDEN_POINT\"}]");
            JSONObject requestString = new JSONObject(res);
            if (requestString != null|| !requestString.getBoolean("success")) {
                JSONArray jSONArray2 = requestString.getJSONArray("data");
                JSONArray jSONArray3 = new JSONArray();
                JSONArray jSONArray4 = new JSONArray();
                for (int i = 0; i < jSONArray2.length(); i++) {
                    JSONObject jSONObject = jSONArray2.getJSONObject(i);
                    String string = jSONObject.getString("taskId");
                    String string2 = jSONObject.getString("taskCenId");
                    jSONObject.getString("taskStatus");
                    if (!"CLICK_JUMP".equals(jSONObject.getString("taskTriggerType"))) {
                        String valueByPath = JsonUtil.getValueByPath(jSONObject, "taskShowInfo.title");
                        trigger(string2, string, "signup");
                        if (trigger(string2, string, "send")) {
                            Log.other(this.displayName + "完成任务[" + valueByPath + "]");
                            jSONArray3.put(string2);
                            jSONArray4.put(string);
                        }
                    }
                    TimeUtil.sleep(RandomUtil.nextInt(15000,20000));
                }
                if (jSONArray3.length() != 0 && (requestString2 = requestString("com.alipay.pcreditbfweb.sdk.task.award", "\"taskCenIds\":" + jSONArray3 + ",\"taskIds\":" + jSONArray4)) != null && (jSONArray = (JSONArray) JsonUtil.getValueByPathObject(requestString2, "data.resultData")) != null) {
                    for (int i2 = 0; i2 < jSONArray.length(); i2++) {
                        Log.other(this.displayName + "获得[" + JsonUtil.getValueByPath(jSONArray.getJSONObject(i2), "prizeSendOrderList.[0].price") + "花呗金]");
                    }
                }
            }
        } catch (JSONException e) {
            Log.error("dubug--花呗金JSON解析异常:"+e);
            throw new RuntimeException(e);
        }
    }

    private boolean trigger(String str, String str2, String str3) {
        try {
            return requestString("com.alipay.pcreditbfweb.sdk.task.trigger", new StringBuilder("\"appletId\": \"").append(str2).append("\",\"outBizNo\": \"").append(str2).append(TimeUtil.getMinuteTimestamp()).append("\",\"taskCenId\": \"").append(str).append("\",\"retryFlag\": true,\"stageCode\":\"").append(str3).append("\"").toString()) != null;
        } catch (Throwable th) {
            try {
                Log.printStackTrace(this.TAG, th);
                return false;
            } finally {
                TimeUtil.sleep(this.executeIntervalInt);
            }
        }
    }
}
