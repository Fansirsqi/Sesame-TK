package fansirsqi.xposed.sesame.task.recreation;

import static fansirsqi.xposed.sesame.hook.RequestManager.requestString;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import fansirsqi.xposed.sesame.hook.RequestManager;
import fansirsqi.xposed.sesame.util.JsonUtil;
import fansirsqi.xposed.sesame.util.Log;
import fansirsqi.xposed.sesame.util.Maps.UserMap;
import fansirsqi.xposed.sesame.util.TimeUtil;

public class Fishpond {
    private String TAG;
    private Integer fishCount = null;
    private Integer leftFishTimes = null;
    private String fishData = "";

    private final List<String> notTaskIds = new ArrayList<String>() {
        {
            add("NORMAL_WANYOUXI");
            add("cy25wf_yt_dgwyx30");
            add("ANTFISHPOND_WECHAT_SHARE");
        }
    };

    public Fishpond(String tag) {
        this.TAG = tag;
    }

    public static String displayName = "福气鱼塘🐟";

    private int executeIntervalInt = 2000;

    private String getData() {
        return getData("GameCenter");
    }

    private String getData(String str) {
        return MessageFormat.format("\"requestType\": \"NORMAL\",\n\"sceneCode\": \"{0}\",\n\"source\": \"ch_alipaysearch__chsub_normal\",\n\"version\": \"20240722.01\"", str);
//        return MessageFormat.format("\"requestType\": \"NORMAL\",\n\"sceneCode\": \"{0}\",\n\"source\": \"ch_appid-20001003__chsub_pageid-com.alipay.android.phone.businesscommon.globalsearch.ui.MainSearchActivity\",\n\"version\": \"20240722.01\"", str);
    }

    public void listTask() {
        try {
            JSONObject requestString = requestString("com.alipay.antfishpond.listTask", getData(), true);
            if (requestString == null) {
                return;
            }
            sign(JsonUtil.getValueByPathObject(requestString, "signInfo.list"));
            JSONArray jSONArray = requestString.getJSONArray("taskList");
            for (int i = 0; i < jSONArray.length(); i++) {
                finishTask(jSONArray.getJSONObject(i));
                TimeUtil.sleep(executeIntervalInt);
            }
        } catch (Throwable th) {
            Log.printStackTrace(displayName, th);
        }
    }

    private void sign(Object obj) {
        if (obj == null) {
            return;
        }
        try {
            JSONArray jSONArray = (JSONArray) obj;
            String str = "";
            boolean z = false;
            int i = 0;
            while (true) {
                if (i >= jSONArray.length()) {
                    break;
                }
                JSONObject jSONObject = jSONArray.getJSONObject(i);
                if (jSONObject.getBoolean("today")) {
                    str = jSONObject.getString("signKey");
                    z = jSONObject.getBoolean("signed");
                    break;
                }
                i++;
            }
            if (z || str.isEmpty() || RequestManager.requestString("com.alipay.antfishpond.sign", getData() + ",\"signKey\": \"" + str + "\"",true) == null) {
                return;
            }
            Log.other(displayName + "签到成功");
        } catch (Exception e) {
            Log.printStackTrace(TAG, e);
        }
    }

    private void finishTask(JSONObject jSONObject) {
        try {
            String string = jSONObject.getString("taskId");
            String string2 = jSONObject.getString("taskStatus");
            String string3 = jSONObject.getString("sceneCode");
            int i = jSONObject.getInt("rightsTimes");
            int i2 = jSONObject.getInt("rightsTimesLimit");
            String string4 = jSONObject.getString("actionType");
            int i3 = i2 - i;
            String str = getData(string3) + ",\"taskType\":\"" + string + "\"";
            if ("FINISHED".equals(string2)) {
                receiveTaskAward(str);
                return;
            }
            if ("GOFISH".equals(string4)) {
                fishCount = Integer.valueOf(jSONObject.getInt("taskRequire") - jSONObject.getInt("taskProgress"));
                fishData = str;
                return;
            }
            if (this.notTaskIds.contains(string) || i3 == 0) {
                return;
            } else {
                if (!"TODO".equals(string2) || RequestManager.requestString("com.alipay.antiep.finishTask", str + ",\"outBizNo\":\"" + UserMap.getCurrentUid() + System.currentTimeMillis() + "\"",true) == null || string.contains("GYG_XLIGHT_JX_BUSINEES")) {
                    return;
                }
                TimeUtil.sleep(executeIntervalInt);
                receiveTaskAward(str);
            }
        } catch (Exception e) {
            Log.printStackTrace(TAG, e);
        }
    }

    private void receiveTaskAward(String str) {
        try {
            RequestManager.requestString("com.alipay.antiep.receiveTaskAward", str + ",\"ignoreLimit\":false",true);
        } catch (Exception e) {
            Log.printStackTrace(TAG, e);
        }
    }

    public void fishpondAngle() throws JSONException {
        String data = "";
        JSONObject requestString = null;
        if (Recreation.getFishpondAngle().getValue()) {
            String value = Recreation.getFishpondToken().getValue();
            if (fishpondSyncIndex()) {
                return;
            }
            int i = 0;
            int i2 = 0;
            do {
                try {
                    data = getData();
                    if (!value.isEmpty()) {
                        data = data + ",\"riskToken\":" + new JSONObject(value);
                    }
                    requestString = requestString("com.alipay.antfishpond.fishpondAngle", data,true);
                } catch (Throwable th) {
                    Log.printStackTrace(TAG, th);
                    i++;
                }
                if (requestString == null) {
                    return;
                }
                String fishpondAngle = fishpondAngle(requestString);
                if (fishpondAngle != null && !"1".equals(fishpondAngle)) {
                    JSONObject requestString2 = RequestManager.requestString("com.alipay.antfishpond.fishpondAngleRodPositioning", "\"areaType\": \"SPECIAL_BIG_ZONE\",\"bizNo\": \"" + fishpondAngle + "\"," + data,true);
                    if (requestString2 == null) {
                        return;
                    } else {
                        fishpondAngle = fishpondAngle(requestString2);
                    }
                }
                i2++;
                Integer num = fishCount;
                if (num != null && i2 == num.intValue()) {
                    receiveTaskAward(fishData);
                }
                Integer num2 = leftFishTimes;
                if (num2 != null && i2 == num2.intValue()) {
                    return;
                }
                if (fishpondAngle == null) {
                    return;
                } else {
                    TimeUtil.sleep(executeIntervalInt);
                }
            } while (i <= 3);
        }else{
            Log.runtime(TAG,"未启用自动钓鱼");
        }
    }

    private String fishpondAngle(JSONObject jSONObject) {
        try {
            int i = jSONObject.getInt("rodSumCount");
            JSONObject jSONObject2 = jSONObject.getJSONObject("angleResultInfo");
            JSONObject optJSONObject = jSONObject2.optJSONObject("angleAdInfo");
            String string = jSONObject2.getString("fishWeight");
            String string2 = jSONObject2.getString("fishType");
            String string3 = jSONObject2.getString("bizNo");
            if (!string.isEmpty() && "0.01".equals(string)) {
                Log.other(displayName + "你个大黑鬼，0.01钓个毛啊，罢工不钓了，手动钓一次鱼后将自动更新token（tips:需打开抓包功能）");
                Log.error(displayName + "你个大黑鬼，0.01钓个毛啊，罢工不钓了，手动钓一次鱼后将自动更新token（tips:需打开抓包功能）");
                Recreation.getFishpondToken().setValue("");
                Recreation.getFishpondAngle().setValue(false);
                return null;
            }
            if ("WELFARE_FISH".equals(string2)) {
                return string3;
            }
            if (optJSONObject != null) {
                "ADFISH".equals(optJSONObject.optString("awardType"));
            }
            Log.other(displayName + "钓鱼获得[" + jSONObject2.getString("fishName") + "]+" + string + "剩余" + i + "次");
            if (i == 0) {
                return null;
            }
            return "1";
        } catch (Throwable th) {
            Log.printStackTrace(this.TAG, th);
            return null;
        }
    }

    private boolean fishpondSyncIndex() {
        try {
            JSONObject requestString = RequestManager.requestString("com.alipay.antfishpond.fishpondSyncIndex", getData() + ",\"syncTypeList\":[\"FISH_ACTIVITY\",\"TASK_DISPLAY\"]",true);
            if (requestString == null) {
                return false;
            }
            int i = requestString.getInt("rodSumCount");
            String valueByPath = JsonUtil.getValueByPath(requestString, "fishActivity.leftFishTimes");
            if (!valueByPath.isEmpty()) {
                leftFishTimes = Integer.valueOf(Integer.parseInt(valueByPath));
            }
            Object valueByPathObject = JsonUtil.getValueByPathObject(requestString, "roundInfo.fishAssetInfo");
            if (valueByPathObject != null) {
                JSONObject jSONObject = (JSONObject) valueByPathObject;
                Log.other(displayName + "目标[" + jSONObject.getString("targetFishWeight") + "]当前[" + jSONObject.getString("currentFishWeight") + "]剩余[" + jSONObject.getString("diffFishWeight") + "]");
            }
            return i == 0;
        } catch (Throwable th) {
            Log.printStackTrace(TAG, th);
            return false;

        }
    }

}
