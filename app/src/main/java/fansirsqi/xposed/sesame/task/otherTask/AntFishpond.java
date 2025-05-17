package fansirsqi.xposed.sesame.task.otherTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fansirsqi.xposed.sesame.data.Status;
import fansirsqi.xposed.sesame.hook.ApplicationHook;
import fansirsqi.xposed.sesame.hook.RequestManager;
import fansirsqi.xposed.sesame.util.JsonUtil;
import fansirsqi.xposed.sesame.util.Log;
import fansirsqi.xposed.sesame.util.Notify;
import fansirsqi.xposed.sesame.util.TimeUtil;
import fansirsqi.xposed.sesame.util.Maps.UserMap;

/** @noinspection unchecked*/
public class AntFishpond extends BaseCommTask {
    private Integer fishCount = 0;
    private Integer leftFishTimes = 0;//还剩需要捕鱼次数才可以领取鱼竿
    private Integer initialRodCount = 0; // 初始鱼竿总数

    private String fishData = "";
    private final List<String> notTaskIds = new ArrayList<>() {
        {
            add("NORMAL_WANYOUXI");
            add("cy25wf_yt_dgwyx30");
            add("ANTFISHPOND_WECHAT_SHARE");
            add("FISHPOND_NCLY_GAME_XJCSPX_PLAY");
            add("FISHPOND_NCLY_GAME_KDYYL_PLAY");
            // 新增需要过滤的任务ID

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
        //this.hoursKeyEnum = CompletedKeyEnum.AntFishpond;
    }

    @Override
    protected void handle() throws JSONException {
        if(Status.hasFlagToday(CompletedKeyEnum.FinshTask.name())) {
            return;
        }
        fishpondExchangeReward();
        listTask();
        TimeUtil.sleep(this.executeIntervalInt);
        triggerSubplotsActivity();
        fishpondAngle();
    }

    private void listTask() {
        try {
            JSONObject requestString = requestString("com.alipay.antfishpond.listTask", getData());
            if (requestString == null) {
                return;
            }
            if (!Status.hasFlagToday(CompletedKeyEnum.AntFishpondSign.name())) {
                sign(JsonUtil.getValueByPathObject(requestString, "signInfo.list"));
            }
            JSONArray jSONArray = requestString.getJSONArray("taskList");
            for (int i = 0; i < jSONArray.length(); i++) {
                finishTask(jSONArray.getJSONObject(i));
                TimeUtil.sleep(this.executeIntervalInt);
            }
        } catch (Throwable th) {
            Log.printStackTrace(this.TAG, th);
        }
    }
    private void triggerSubplotsActivity() {
        try {
            // 定义常量避免魔法字符串
            final String ACTIVITY_TYPE_TOMORROW_ROD = "TOMORROW_ROD";
            final String ACTION_RECEIVE_AWARD = "receiveAward";
            final String EXTEND_AWARD_COUNT = "awardCount";
            final String EXTEND_RECEIVED_ROD_COUNT = "receivedRodCount";

            String result = RequestManager.requestString("com.alipay.antfishpond.querySubplotsActivity",
                    "[{\"requestType\":\"NORMAL\",\"sceneCode\":\"GameCenter\",\"source\":\"ch_searchbox_qudiaoyu\",\"version\":\"20240722.01\"}]");
            JSONObject requestString = new JSONObject(result);
            if (requestString == null) return;

            JSONArray jSONArray = requestString.getJSONArray("subplotsActivityList");
            if (jSONArray == null || jSONArray.length() == 0){
                return;
            }
            for (int i = 0; i < jSONArray.length(); i++) {
                JSONObject jSONObject = jSONArray.getJSONObject(i);
                String activityType = jSONObject.getString("activityType");
                String status = jSONObject.optString("status");

                // 只处理待办状态
                if (!"TODO".equals(status) && !"TODAY_TODO".equals(status)) continue;

                // 使用 switch 简化类型判断
                switch (activityType) {
                    case ACTIVITY_TYPE_TOMORROW_ROD: {
                        Object obj = 2; // 固定类型标识
                        String set = "";

                        // 嵌套逻辑扁平化
                        if (obj != null) {
                            if (obj.equals(2)) {
                                set = EXTEND_RECEIVED_ROD_COUNT;
                                String optString = "FINISH";

                                // 核心业务逻辑
                                String reward = JsonUtil.getValueByPath(
                                        requestString("com.alipay.antfishpond.triggerSubplotsActivity",
                                                getData() + ",\"activityType\": \"" + activityType + "\",\"actionType\": \"" + optString + "\""),
                                        "triggerSubplotsActivity.extend." + set);

                                if (reward != null && !reward.isEmpty()) {
                                    Log.other(displayName + "领取奖励[" + reward + "根钓竿]");
                                }
                            }
                        }
                        break;
                    }

                    // 其他类型预留扩展
                    case "FISH_ACTIVITY":
                    case "GIFT_BOX":
                        // 当前版本无具体操作
                        break;

                    default:
                        Log.other(displayName + "未知活动类型: " + activityType);
                }
            }
        } catch (Throwable th) {
            Log.error(this.TAG+"钓鱼trigger方法出错:"+th);
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
            Status.setFlagToday(CompletedKeyEnum.AntFishpondSign.name());
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
            } else if ("TODO".equals(string2)){
                JSONObject json = requestString("com.alipay.antiep.finishTask", str + ",\"outBizNo\":\"" + UserMap.getCurrentUid() + System.currentTimeMillis() + "\"");
                TimeUtil.sleep(500L);
                if (json != null && json.optBoolean("success")) {
                    receiveTaskAward(str);
                }
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
        //邀请
        try {
            Set<String> set = (Set<String>) this.mapHandler.get("antFishpondList");
            if (set != null && !set.isEmpty()) {
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
        } catch (Exception e) {
           Log.error(this.displayName+"钓鱼邀请任务出错:"+e);
        }

    }

    private void fishpondAngle() {
        try {
            String str = "";
            JSONObject requestString = null;
            if (Boolean.TRUE.equals(this.mapHandler.get("fishpondAngle"))) {
                String value = OtherTask.getFishpondToken().getValue();
                if (fishpondSyncIndex()) {
                    return;
                }
                int i = 0;
                int i2 = 0;
                do {
                    try {
                        str = getData() + ",\"riskToken\":" + value;
                        requestString = requestString("com.alipay.antfishpond.fishpondAngle", str);
                    } catch (Throwable th) {
                        Log.printStackTrace(this.TAG, th);
                        i++;
                    }
                    if (requestString == null) {
                        return;
                    }
                    String fishpondAngle = fishpondAngle(requestString);
                    if (fishpondAngle != null && !"1".equals(fishpondAngle)) {
                        JSONObject requestString2 = requestString("com.alipay.antfishpond.fishpondAngleRodPositioning", "\"areaType\": \"SPECIAL_BIG_ZONE\",\"bizNo\": \"" + fishpondAngle + "\"," + str);
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
        }catch (Exception e){
            Log.error(this.TAG, "钓鱼出错 err: ");
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
                Log.other(this.displayName + "token失效，停止自动钓鱼，手动钓一次鱼后将自动更新token");
                OtherTask.getFishpondToken().setValue("");
                OtherTask.getFishpondAngle().setValue(false);
                return null;
            }
            if ("WELFARE_FISH".equals(string2)) {
                return string3;
            }
            if (optJSONObject != null && "ADFISH".equals(optJSONObject.optString("awardType"))) {
                Matcher matcher = Pattern.compile("bizId=([A-Za-z0-9]+)").matcher(optJSONObject.getString("targetUrl"));
                if (matcher.find() && requestString("com.alipay.adtask.biz.mobilegw.service.task.finish", "\"bizId\": \"" + matcher.group(1) + "\"") != null) {
                    string = string + "X" + optJSONObject.getString("investBubble");
                }
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


    private void fishpondExchangeReward() {
        JSONObject requestString;
        try {
            JSONObject requestString2 = requestString("com.alipay.antfishpond.fishpondIndex", getData());
            if (requestString2 == null || !String.valueOf(true).equals(JsonUtil.getValueByPath(requestString2, "roundInfo.canExchange")) || (requestString = requestString("com.alipay.antfishpond.fishpondExchangeReward", getData())) == null) {
                return;
            }
            JSONObject jSONObject = requestString.getJSONObject("exchangeRewardResult");
            String str = this.displayName + jSONObject.optString("title") + jSONObject.optString("targetRewardCount");
            String title = "\uD83E\uDDE7鱼塘兑换奖励：";
            int newNotificationId = 114707; // 你可以根据需要生成唯一的通知ID
            Log.other(str);
            Notify.sendNewNotification(ApplicationHook.getContext(),title, str,newNotificationId);
            Log.other(displayName+"鱼塘兑换成功\uD83E\uDDE7");
        } catch (Throwable th) {
            Log.printStackTrace(this.TAG, th);
        }
    }
}
