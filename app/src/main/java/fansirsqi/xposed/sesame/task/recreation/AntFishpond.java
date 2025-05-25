package fansirsqi.xposed.sesame.task.recreation;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import fansirsqi.xposed.sesame.data.CompletedKeyEnum;
import fansirsqi.xposed.sesame.hook.ApplicationHook;
import fansirsqi.xposed.sesame.model.BaseCommTask;
import fansirsqi.xposed.sesame.util.JsonUtil;
import fansirsqi.xposed.sesame.util.Log;
import fansirsqi.xposed.sesame.util.Maps.UserMap;
import fansirsqi.xposed.sesame.util.NotificationUtil;
import fansirsqi.xposed.sesame.util.TimeUtil;

public class AntFishpond extends BaseCommTask {
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

    private String getData() {
        return getData("GameCenter");
    }

    private String getData(String str) {
        return MessageFormat.format("\"requestType\": \"NORMAL\",\n\"sceneCode\": \"{0}\",\n\"source\": \"ch_alipaysearch__chsub_normal\",\n\"version\": \"20240722.01\"", str);
    }

    public AntFishpond() {
        this.displayName = "福气鱼塘🐟";
        this.hoursKeyEnum = CompletedKeyEnum.AntFishpond;
    }

    @Override // leo.xposed.sesameX.model.common.base.BaseCommTask
    protected void handle() {
        fishpondExchangeReward();
        listTask();
        triggerSubplotsActivity();
        fishpondAngle();
    }

    private void listTask() {
        try {
            JSONObject requestString = requestString("com.alipay.antfishpond.listTask", getData());
            if (requestString == null) {
                return;
            }
            sign(JsonUtil.getValueByPathObject(requestString, "signInfo.list"));
            JSONArray jSONArray = requestString.getJSONArray("taskList");
            for (int i = 0; i < jSONArray.length(); i++) {
                finishTask(jSONArray.getJSONObject(i));
                TimeUtil.sleep(this.executeIntervalInt);
            }
        } catch (Throwable th) {
            Log.printStackTrace(this.TAG, th);
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
            if (z || str.isEmpty() || requestString("com.alipay.antfishpond.sign", getData() + ",\"signKey\": \"" + str + "\"") == null) {
                return;
            }
            Log.other(this.displayName + "签到成功");
        } catch (Exception e) {
            Log.printStackTrace(this.TAG, e);
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
                this.fishCount = Integer.valueOf(jSONObject.getInt("taskRequire") - jSONObject.getInt("taskProgress"));
                this.fishData = str;
                return;
            }
            if (this.notTaskIds.contains(string) || i3 == 0) {
                return;
            }
            if ("SHARE".equals(string4)) {
                batchInviteP2P(i3);
                receiveTaskAward(str);
            } else {
                if (!"TODO".equals(string2) || requestString("com.alipay.antiep.finishTask", str + ",\"outBizNo\":\"" + UserMap.getCurrentUid() + System.currentTimeMillis() + "\"") == null || string.contains("GYG_XLIGHT_JX_BUSINEES")) {
                    return;
                }
                TimeUtil.sleep(500L);
                receiveTaskAward(str);
            }
        } catch (Exception e) {
            Log.printStackTrace(this.TAG, e);
        }
    }

    private void receiveTaskAward(String str) {
        try {
            requestString("com.alipay.antiep.receiveTaskAward", str + ",\"ignoreLimit\":false");
        } catch (Exception e) {
            Log.printStackTrace(this.TAG, e);
        }
    }

    private void batchInviteP2P(int i) {
        try {
            Set set = (Set) this.mapHandler.get("antFishpondList");
            if (set != null && !set.isEmpty()) {
                String data = getData("ANTFISHPOND_SHARE_P2P");
                String str = "";
                JSONArray jSONArray = new JSONArray();
                ArrayList<Object> arrayList = new ArrayList<Object>(set);
                for (int i2 = 0; i2 < i; i2++) {
                    if (i2 < arrayList.size()) {
                        str = (String) arrayList.get(i2);
                    }
                    JSONObject jSONObject = new JSONObject();
                    jSONObject.put("beInvitedUserId", str);
                    jSONArray.put(jSONObject);
                }
                requestString("com.alipay.antiep.batchInviteP2P", data + ",\"inviteP2PVOList\":" + jSONArray);
            }
        } catch (Exception e) {
            Log.printStackTrace(this.TAG, e);
        }
    }

    private void fishpondAngle() {
        String data = "";
        JSONObject requestString = null;
        try {
            if (Boolean.TRUE.equals(this.mapHandler.get("fishpondAngle"))) {
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
                        requestString = requestString("com.alipay.antfishpond.fishpondAngle", data);
                    } catch (Throwable th) {
                        Log.printStackTrace(this.TAG, th);
                        i++;
                    }
                    if (requestString == null) {
                        return;
                    }
                    String fishpondAngle = fishpondAngle(requestString);
                    if (fishpondAngle != null && !"1".equals(fishpondAngle)) {
                        JSONObject requestString2 = requestString("com.alipay.antfishpond.fishpondAngleRodPositioning", "\"areaType\": \"SPECIAL_BIG_ZONE\",\"bizNo\": \"" + fishpondAngle + "\"," + data);
                        if (requestString2 == null) {
                            return;
                        } else {
                            fishpondAngle = fishpondAngle(requestString2);
                        }
                    }
                    i2++;
                    Integer num = this.fishCount;
                    if (num != null && i2 == num.intValue()) {
                        receiveTaskAward(this.fishData);
                    }
                    Integer num2 = this.leftFishTimes;
                    if (num2 != null && i2 == num2.intValue()) {
                        triggerSubplotsActivity();
                    }
                    if (fishpondAngle == null) {
                        return;
                    } else {
                        TimeUtil.sleep(this.executeIntervalInt);
                    }
                } while (i <= 3);
            }
        } catch (RuntimeException e) {
            throw new RuntimeException(e);
        } catch (JSONException e) {
            throw new RuntimeException(e);
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
                Log.other(this.displayName + "你个大黑鬼，0.01钓个毛啊，罢工不钓了，手动钓一次鱼后将自动更新token（tips:需打开抓包功能）");
                Log.error(this.displayName + "你个大黑鬼，0.01钓个毛啊，罢工不钓了，手动钓一次鱼后将自动更新token（tips:需打开抓包功能）");
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
            Log.other(this.displayName + "钓鱼获得[" + jSONObject2.getString("fishName") + "]+" + string + "剩余" + i + "次");
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
            JSONObject requestString = requestString("com.alipay.antfishpond.fishpondSyncIndex", getData() + ",\"syncTypeList\":[\"FISH_ACTIVITY\",\"TASK_DISPLAY\"]");
            if (requestString == null) {
                return false;
            }
            int i = requestString.getInt("rodSumCount");
            String valueByPath = JsonUtil.getValueByPath(requestString, "fishActivity.leftFishTimes");
            if (!valueByPath.isEmpty()) {
                this.leftFishTimes = Integer.valueOf(Integer.parseInt(valueByPath));
            }
            Object valueByPathObject = JsonUtil.getValueByPathObject(requestString, "roundInfo.fishAssetInfo");
            if (valueByPathObject != null) {
                JSONObject jSONObject = (JSONObject) valueByPathObject;
                Log.other(this.displayName + "目标[" + jSONObject.getString("targetFishWeight") + "]当前[" + jSONObject.getString("currentFishWeight") + "]剩余[" + jSONObject.getString("diffFishWeight") + "]");
            }
            return i == 0;
        } catch (Throwable th) {
            Log.printStackTrace(this.TAG, th);
            return false;
        }
    }

    private void triggerSubplotsActivity() {
        throw new UnsupportedOperationException("Method not decompiled: fansirsqi.xposed.sesame.model.task.recreation.AntFishpond.triggerSubplotsActivity():void");
    }

    private void fishpondExchangeReward() {
        JSONObject requestString;
        try {
            JSONObject requestString2 = requestString("com.alipay.antfishpond.fishpondIndex", getData());
            if (requestString2 == null || !String.valueOf(true).equals(JsonUtil.getValueByPath(requestString2, "roundInfo.canExchange")) || (requestString = requestString("com.alipay.antfishpond.fishpondExchangeReward", getData())) == null) {
                return;
            }
            JSONObject jSONObject = requestString.getJSONObject("exchangeRewardResult");
            String str = this.displayName + jSONObject.optString("title") + jSONObject.optString("targetRewardCount");
            Log.other(str);
            NotificationUtil.showNotification(ApplicationHook.getContext(), str);
        } catch (Throwable th) {
            Log.printStackTrace(this.TAG, th);
        }
    }
}
