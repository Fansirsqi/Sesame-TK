package fansirsqi.xposed.sesame.task.recreation;

import androidx.webkit.internal.ApiFeature;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
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
    private Integer fishCount;
    private Integer leftFishTimes;
    private String fishData;
    private final List<String> notTaskIds;

    public AntFishpond() {
        this.fishCount = null;
        this.leftFishTimes = null;
        this.fishData = "";
        this.notTaskIds = new ArrayList<String>() {
            {
                add("NORMAL_WANYOUXI");
                add("cy25wf_yt_dgwyx30");
                add("ANTFISHPOND_WECHAT_SHARE");
            }
        };
        this.displayName = "福气鱼塘🐟";
        this.hoursKeyEnum = CompletedKeyEnum.AntFishpond;
    }

    private String getData() {
        return getData("GameCenter");
    }

    private String getData(String str) {
        return MessageFormat.format("\"requestType\": \"NORMAL\",\n\"sceneCode\": \"{0}\",\n\"source\": \"ch_alipaysearch__chsub_normal\",\n\"version\": \"20240722.01\"", new Object[]{str});
    }

    @Override
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
                TimeUtil.sleep((long) this.executeIntervalInt);
            }
        } catch (Throwable th) {
            Log.printStackTrace(this.TAG, th);
        }
    }

    private void sign(Object param) {
        if (param == null) return;

        try {
            JSONArray signArray = (JSONArray) param;
            String signKey = "";
            boolean hasSigned = false;

            // 遍历查找今日签到项
            for (int i = 0; i < signArray.length(); i++) {
                JSONObject item = signArray.getJSONObject(i);
                if (item.getBoolean("today")) {
                    signKey = item.getString("signKey");
                    hasSigned = item.getBoolean("signed");
                    break; // 找到今日项后提前退出循环
                }
            }

            // 检查签到条件
            if (hasSigned || signKey.isEmpty()) return;

            // 构造请求数据
            String action = "com.alipay.antfishpond.sign";
            String requestData = getData() + ",\"signKey\": \"" + signKey + "\"";

            // 发送签到请求
            JSONObject result = requestString(action, requestData);
            if (result == null) return;

            // 记录成功日志
            String successMsg = this.displayName + "签到成功"; // displayName为实例字段
            Log.other(successMsg); // 假设日志工具类

        } catch (Exception e) {
            Log.printStackTrace(TAG, e); // 异常处理
        }
    }

    private void finishTask(JSONObject jSONObject) {
        String str = "\"";
        try {
            Log.runtime(this.TAG, "执行finishTask");
            String string = jSONObject.getString("taskId");
            String string2 = jSONObject.getString("taskStatus");
            String string3 = jSONObject.getString("sceneCode");
            int i = jSONObject.getInt("rightsTimes");
            int i2 = jSONObject.getInt("rightsTimesLimit");
            String string4 = jSONObject.getString("actionType");
            i2 -= i;
            string3 = getData(string3) + ",\"taskType\":\"" + string + str;

            Log.runtime(this.TAG, String.format(Locale.ROOT, "taskId:%s,taskStatus:%s,sceneCode:%s,rightsTimes:%s,rightsTimesLimit:%s,actionType:%s",
                    string, string2, string3, i, i2, string4));
            Log.runtime(this.TAG, "antiep string3：" + string3);
            if ("FINISHED".equals(string2)) {
                receiveTaskAward(string3);
                Log.runtime(this.TAG, "FINISHED");
            } else if ("GOFISH".equals(string4)) {
                Log.runtime(this.TAG, "GOFISH");
                this.fishCount = Integer.valueOf(jSONObject.getInt("taskRequire") - jSONObject.getInt("taskProgress"));
                this.fishData = string3;
            } else if (!this.notTaskIds.contains(string) && i2 != 0) {
                if ("SHARE".equals(string4)) {
                    Log.runtime(this.TAG, "SHARE");
                    batchInviteP2P(i2);
                    receiveTaskAward(string3);
                }
                if (!"TODO".equals(string2) || string.contains("GYG_XLIGHT_JX_BUSINEES")) {
                    Log.runtime(this.TAG, "!TODO");
                    if (requestString("com.alipay.antiep.finishTask", string3 + ",\"outBizNo\":\"" + UserMap.getCurrentUid() + System.currentTimeMillis() + str) == null) {
                        Log.runtime(this.TAG, "antiep.finishTask");
                        receiveTaskAward(string3);
                    }
                }
            }
        } catch (Exception e) {
            Log.printStackTrace(this.TAG, e);
        }
    }

    // 收取奖励
    private void receiveTaskAward(String str) {
        int i = 0;
        do {
            try {
                TimeUtil.sleep((long) (i + 1) * (this.executeIntervalInt));
                requestString("com.alipay.antiep.receiveTaskAward", str + ",\"ignoreLimit\":false");
                break;
            } catch (Exception e) {
                i++;
                Log.runtime(this.TAG, "receiveTaskAward收取奖励异常:" + e);
            }
        } while (i < 3);
    }

    private void batchInviteP2P(int i) {
        try {
            Set set = (Set) this.mapHandler.get("antFishpondList");
            if (set != null) {
                if (!set.isEmpty()) {
                    String data = getData("ANTFISHPOND_SHARE_P2P");
                    String str = "";
                    JSONArray jSONArray = new JSONArray();
                    ArrayList<String> arrayList = new ArrayList<>(set);
                    for (int i2 = 0; i2 < i; i2++) {
                        if (i2 < arrayList.size()) {
                            str = arrayList.get(i2);
                        }
                        JSONObject jSONObject = new JSONObject();
                        jSONObject.put("beInvitedUserId", str);
                        jSONArray.put(jSONObject);
                    }
                    requestString("com.alipay.antiep.batchInviteP2P", data + ",\"inviteP2PVOList\":" + jSONArray);
                }
            }
        } catch (Exception e) {
            Log.printStackTrace(this.TAG, e);
        }
    }

    private void fishpondAngle() {
        if (Boolean.TRUE.equals(this.mapHandler.get("fishpondAngle"))) {
            String str = (String) Recreation.getFishpondToken().getValue();
            if (!fishpondSyncIndex()) {
                int i = 0;
                int j = 0;
                int i2 = 0;
                String fishpondAngle = null;
                do {
                    try {
                        String data = getData();
                        if (!str.isEmpty()) {
                            data = data + ",\"riskToken\":" + new JSONObject(str);
                        } else {
                            Log.other(this.TAG, "靓仔,先打开抓包模式，手动钓一次鱼");
                            return;
                        }
                        JSONObject requestString = requestString("com.alipay.antfishpond.fishpondAngle", data);
                        if (requestString != null) {
                            fishpondAngle = fishpondAngle(requestString);
                            if (!(fishpondAngle == null || "1".equals(fishpondAngle))) {
                                JSONObject requestString2 = requestString("com.alipay.antfishpond.fishpondAngleRodPositioning", "\"areaType\": \"SPECIAL_BIG_ZONE\",\"bizNo\": \"" + fishpondAngle + "\"," + data);
                                if (requestString2 != null) {
                                    fishpondAngle = fishpondAngle(requestString2);
                                } else {
                                    return;
                                }
                            }
                            i2++;
                        }
                    } catch (Exception e) {
                        i++;
                        Log.printStackTrace(this.TAG, e);
                    }
                    Integer num = this.fishCount;
                    if (num != null && i2 == num.intValue()) {
                        try {
                            receiveTaskAward(this.fishData);
                        } catch (Exception e) {
                            j++;
                            Log.runtime(this.TAG, "receiveTaskAward异常:" + e);
                        }
                    }
                    num = this.leftFishTimes;
                    if (num != null && i2 == num.intValue()) {
                        try {
                            triggerSubplotsActivity();
                        } catch (Exception e) {
                            i++;
                            Log.runtime(this.TAG, "triggerSubplotsActivity异常:" + e);
                        }
                    }
                    if (fishpondAngle != null) {
                        TimeUtil.sleep((long) this.executeIntervalInt);
                    } else {
                        return;
                    }
                } while (i <= 3 && j < 10);
            }
        }
    }

    private String fishpondAngle(JSONObject jSONObject) {
        String str = "你个大黑鬼，0.01钓个毛啊，罢工不钓了，手动钓一次鱼后将自动更新token（tips:需打开抓包功能）";
        String str2 = null;
        try {
            int i = jSONObject.getInt("rodSumCount");
            jSONObject = jSONObject.getJSONObject("angleResultInfo");
            JSONObject optJSONObject = jSONObject.optJSONObject("angleAdInfo");
            String string = jSONObject.getString("fishWeight");
            String string2 = jSONObject.getString("fishType");
            String string3 = jSONObject.getString("bizNo");
            if (!string.isEmpty() && "0.01".equals(string)) {
                Log.other(this.displayName + str);
                Log.error(this.displayName + str);
                Recreation.getFishpondToken().setValue("");
                Recreation.getFishpondAngle().setValue(Boolean.valueOf(false));
                return null;
            } else if ("WELFARE_FISH".equals(string2)) {
                return string3;
            } else {
                if (optJSONObject != null) {
                    "ADFISH".equals(optJSONObject.optString("awardType"));
                }
                Log.other(this.displayName + "钓鱼获得[" + jSONObject.getString("fishName") + "]+" + string + "剩余" + i + "次");
                if (i != 0) {
                    str2 = "1";
                }
                return str2;
            }
        } catch (Throwable th) {
            Log.other(this.TAG, "钓鱼异常:" + th);
            Log.printStackTrace(this.TAG, th);
            return null;
        }
    }

    private boolean fishpondSyncIndex() {
        boolean z = false;
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
                requestString = (JSONObject) valueByPathObject;
                Log.other(this.displayName + "目标[" + requestString.getString("targetFishWeight") + "]当前[" + requestString.getString("currentFishWeight") + "]剩余[" + requestString.getString("diffFishWeight") + "]");
            }
            if (i == 0) {
                z = true;
            }
            return z;
        } catch (Throwable th) {
            Log.printStackTrace(this.TAG, th);
            return false;
        }
    }

//    private void triggerSubplotsActivity() {
//        throw new UnsupportedOperationException("Method not decompiled: fansirsqi.xposed.sesame.model.task.recreation.AntFishpond.triggerSubplotsActivity():void");
//    }

    private void triggerSubplotsActivity() {
        try {
            String action1 = "com.alipay.antfishpond.querySubplotsActivity";
            String data = getData() + ",\"activityType\":[]"; // 假设getData()是类方法
            JSONObject response = requestString(action1, data); // 假设requestString返回JSONObject

            if (response == null) return;

            JSONArray activityList = response.getJSONArray("subplotsActivityList");
            for (int i = 0; i < activityList.length(); i++) {
                JSONObject activity = activityList.getJSONObject(i);
                String activityType = activity.getString("activityType");
                String status = activity.optString("status");

                if (!"TODO".equals(status) && !"TODAY_TODO".equals(status)) {
                    continue;
                }

                String actionType = "receiveAward";
                String dataKey = "";

                switch (activityType) {
                    case "GIFT_BOX":
                        dataKey = "awardCount";
                        break;
                    case "FISH_ACTIVITY":
                        dataKey = "rodSumCount";
                        break;
                    case "TOMORROW_ROD":
                        actionType = "FINISH";
                        dataKey = "receivedRodCount";
                        break;
                    default:
                        break;
                }

                if (dataKey.isEmpty()) continue;

                String action2 = "com.alipay.antfishpond.triggerSubplotsActivity";
                String requestData = String.format("%s,\"activityType\": \"%s\",\"actionType\": \"%s\"",
                        getData(), activityType, actionType);

                JSONObject result = requestString(action2, requestData);
                if (result != null) {
                    String path = "triggerSubplotsActivity.extend." + dataKey;
                    String value = JsonUtil.getValueByPath(result, path); // 假设JsonUtil工具类
                    String logMsg = String.format("%s领取奖励[%s根钓竿]",
                            this.displayName, value); // 假设displayName字段
                    Log.other(logMsg); // 假设日志工具类
                }
            }
        } catch (Exception e) {
            Log.printStackTrace(TAG, e); // 假设日志方法
        }
    }

    private void fishpondExchangeReward() {
        try {
            JSONObject requestString = requestString("com.alipay.antfishpond.fishpondIndex", getData());
            if (requestString != null && String.valueOf(true).equals(JsonUtil.getValueByPath(requestString, "roundInfo.canExchange"))) {
                requestString = requestString("com.alipay.antfishpond.fishpondExchangeReward", getData());
                if (requestString != null) {
                    requestString = requestString.getJSONObject("exchangeRewardResult");
                    String str = this.displayName + requestString.optString("title") + requestString.optString("targetRewardCount");
                    Log.other(str);
                    NotificationUtil.showNotification(ApplicationHook.getContext(), str);
                }
            }
        } catch (Throwable th) {
            Log.printStackTrace(this.TAG, th);
        }
    }
}
