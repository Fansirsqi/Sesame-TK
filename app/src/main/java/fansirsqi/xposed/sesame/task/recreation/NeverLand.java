package fansirsqi.xposed.sesame.task.recreation;
import androidx.core.app.NotificationCompat;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import ch.qos.logback.core.joran.action.Action;
import fansirsqi.xposed.sesame.data.CompletedKeyEnum;
import fansirsqi.xposed.sesame.hook.ApplicationHook;
import fansirsqi.xposed.sesame.model.BaseCommTask;
import fansirsqi.xposed.sesame.util.JsonUtil;
import fansirsqi.xposed.sesame.util.Log;
import fansirsqi.xposed.sesame.util.Maps.Status;
import fansirsqi.xposed.sesame.util.NotificationUtil;
import fansirsqi.xposed.sesame.util.TimeUtil;

public class NeverLand extends BaseCommTask {
    private String mapId = "MM11";
    private final String branchId = "MASTER";
    private List<Integer> levelIds = new ArrayList<>();

    public NeverLand() {
        this.displayName = "悦动健康岛🍰";
        this.hoursKeyEnum = CompletedKeyEnum.Neverland;
    }

    @Override // leo.xposed.sesameX.model.common.base.BaseCommTask
    protected void handle() {
        if (!Status.getCompletedDay(CompletedKeyEnum.NeverlandSign)) {
            sign();
            exchangePrize();
        }
        receiveSpecialPrize();
        viewDailyAds();
        queryBaseinfo();
        offlineAward();
        for (int i = 0; i < 4; i++) {
            queryTaskCenter();
            TimeUtil.sleep(this.executeIntervalInt);
        }
        queryTaskInfo();
        queryBubbleTask();
        if (Recreation.getNeverLandJump().getValue()) {
            if (!queryMapStageRewardInfo()) {
                Log.other(this.displayName + "地图阶段奖励已领完，注意手动切换地图");
            }
            walkGrid();
        }else{
            Log.other("悦动健康岛🍰跳一跳未启用");
        }
    }

    private void sign() {
        Object valueByPathObject;
        try {
            JSONObject requestString = requestString("com.alipay.neverland.biz.rpc.querySign", "\"source\": \"jkdwodesign\"");
            if (requestString == null || (valueByPathObject = JsonUtil.getValueByPathObject(requestString, "data.days")) == null) {
                return;
            }
            JSONArray jSONArray = (JSONArray) valueByPathObject;
            for (int i = 0; i < jSONArray.length(); i++) {
                JSONObject jSONObject = jSONArray.getJSONObject(i);
                if (jSONObject.getBoolean("current")) {
                    if (!jSONObject.getBoolean("signIn")) {
                        JSONObject requestString2 = requestString("com.alipay.neverland.biz.rpc.takeSign", "\"source\":\"jkddicon\"");
                        if (requestString2 == null) {
                            return;
                        } else {
                            Log.other(this.displayName + "签到成功，获得[" + JsonUtil.getValueByPath(requestString2, "data.userItems.[0].modifyCount") + "]");
                        }
                    }
                    Status.setCompletedDay(CompletedKeyEnum.NeverlandSign);
                    return;
                }
            }
        } catch (Exception e) {
            Log.printStackTrace(this.TAG, e);
        }
    }

    private void queryTaskCenter() {
        Object valueByPathObject;
        try {
            JSONObject requestString = requestString("com.alipay.neverland.biz.rpc.queryTaskCenter", "\"apDid\": \"EVUwAztLcPFUtPV3WFm5V0NFa5oE6DJ1eXm96Ue0kOo=\",\"cityCode\": \"110100\",\"deviceLevel\": \"high\",\"source\": \"jkddicon\"");
            if (requestString == null || (valueByPathObject = JsonUtil.getValueByPathObject(requestString, "data.taskCenterTaskVOS")) == null) {
                return;
            }
            JSONArray jSONArray = (JSONArray) valueByPathObject;
            for (int i = 0; i < jSONArray.length(); i++) {
                JSONObject jSONObject = jSONArray.getJSONObject(i);
                String string = jSONObject.getString("taskStatus");
                String string2 = jSONObject.getString("taskType");
                if (!"RECEIVE_SUCCESS".equals(string) && !"GAME_TASK".equals(string2)) {
                    taskSend(jSONObject);
                    TimeUtil.sleep(this.executeIntervalInt);
                }
            }
        } catch (Exception e) {
            Log.printStackTrace(this.TAG, e);
        }
    }

    private void taskSend(JSONObject jSONObject) {
        try {
            if ("LIGHT_TASK".equals(jSONObject.getString("taskType")) && jSONObject.has("logExtMap")) {
                xlightPlugin(jSONObject);
                return;
            }
            jSONObject.put("scene", "MED_TASK_HALL");
            String jSONObject2 = jSONObject.toString();
            requestString("com.alipay.neverland.biz.rpc.taskSend", jSONObject2.substring(1, jSONObject2.length() - 1));
            jSONObject.put(NotificationCompat.CATEGORY_PROGRESS, 1);
            jSONObject.put("source", "jkdwodesign");
            String jSONObject3 = jSONObject.toString();
            JSONObject requestString = requestString("com.alipay.neverland.biz.rpc.taskReceive", jSONObject3.substring(1, jSONObject3.length() - 1));
            if (requestString == null) {
                return;
            }
            Log.other(this.displayName + "完成任务[" + jSONObject.getString("title") + "]+" + JsonUtil.getValueByPath(requestString, "data.userItems.[0].modifyCount"));
        } catch (Exception e) {
            Log.printStackTrace(this.TAG, e);
        }
    }

    private void xlightPlugin(JSONObject jSONObject) {
        try {
            if (requestString("com.alipay.adtask.biz.mobilegw.service.task.finish", "\"bizId\": \"" + jSONObject.getJSONObject("logExtMap").getString("bizId") + "\",\"extendInfo\":{}") == null) {
                return;
            }
            Log.other(this.displayName + "完成任务[" + jSONObject.getString("title") + "]+" + JsonUtil.getValueByPath(jSONObject, "prizes.[0].prizeCount"));
        } catch (Exception e) {
            Log.printStackTrace(this.TAG, e);
        }
    }

    private void queryTaskInfo() {
        Object valueByPathObject;
        while (true) {
            try {
                JSONObject requestString = requestString("com.alipay.neverland.biz.rpc.queryTaskInfo", "\"source\":\"health-island\",\"type\":\"LIGHT_FEEDS_TASK\"");
                if (requestString == null || (valueByPathObject = JsonUtil.getValueByPathObject(requestString, "data.taskInfos.[0]")) == null) {
                    return;
                }
                JSONObject jSONObject = (JSONObject) valueByPathObject;
                String string = jSONObject.getString("encryptValue");
                int i = jSONObject.getInt("energyNum");
                if (requestString("com.alipay.neverland.biz.rpc.energyReceive", "\"encryptValue\": \"" + string + "\",\"energyNum\":\"" + i + "\",\"source\":\"jkdwodesign\",\"type\":\"LIGHT_FEEDS_TASK\"") == null) {
                    return;
                } else {
                    Log.other(this.displayName + "任务获得[" + i + "]能量");
                }
            } catch (Exception e) {
                Log.printStackTrace(this.TAG, e);
                return;
            }
        }
    }

    private void walkGrid() {
        try {
            if (this.mapId.isEmpty()) {
                return;
            }
            while (true) {
                JSONObject requestString = requestString("com.alipay.neverland.biz.rpc.walkGrid", "\"branchId\": \"MASTER\",\"drilling\": false,\"mapId\": \"" + this.mapId + "\",\"source\":\"jkddicon\"");
                if (requestString == null) {
                    return;
                }
                JSONObject jSONObject = requestString.getJSONObject(ApplicationHook.AlipayBroadcastReceiver.EXTRA_DATA);
                int i = jSONObject.getInt("leftCount");
                Object valueByPathObject = JsonUtil.getValueByPathObject(jSONObject, "userItems.[0]");
                JSONObject jSONObject2 = jSONObject.getJSONObject("starData");
                String str = this.displayName + "跳跳跳，前进[" + JsonUtil.getValueByPath(jSONObject, "mapAwards.[0].step") + "步]剩余能量：" + i;
                if (valueByPathObject != null) {
                    JSONObject jSONObject3 = (JSONObject) valueByPathObject;
                    str = str + "，获得[" + jSONObject3.getString("modifyCount") + jSONObject3.getString(Action.NAME_ATTRIBUTE) + "]";
                }
                Log.other(str);
                jSONObject2.getInt("curr");
                jSONObject2.getInt("count");
                jSONObject2.getInt("rewardLevel");
                jSONObject2.getJSONArray("stageRewardRecord").length();
                if (i < 5) {
                    return;
                } else {
                    TimeUtil.sleep(this.executeIntervalInt);
                }
            }
        } catch (Exception e) {
            Log.printStackTrace(this.TAG, e);
        }
    }

    private void queryBaseinfo() {
        try {
            JSONObject requestString = requestString("com.alipay.neverland.biz.rpc.queryBaseinfo", "\"branchId\": \"MASTER\",\"source\":\"jkddicon\"");
            if (requestString == null) {
                return;
            }
            this.mapId = requestString.getJSONObject(ApplicationHook.AlipayBroadcastReceiver.EXTRA_DATA).getString("mapId");
        } catch (Exception e) {
            Log.printStackTrace(this.TAG, e);
        }
    }

    private void receiveSpecialPrize() {
        try {
            try {
            } catch (Exception e) {
                Log.printStackTrace(this.TAG, e);
            }
            if (!Status.getCompletedDay(CompletedKeyEnum.NeverLandSpecial)) {
                for (int i = 0; i < 20; i++) {
                    JSONObject requestString = requestString("com.alipay.neverland.biz.rpc.receiveSpecialPrize", "\"medPrizeIds\":[\"benefitCenterActivityPrize1\"],\"sceneType\":\"BENEFIT_CENTER\"");
                    if (requestString != null) {
                        Log.other(this.displayName + "获得特别奖[" + requestString.getJSONObject(ApplicationHook.AlipayBroadcastReceiver.EXTRA_DATA).optInt("modifyCount") + "]能量");
                    }
                }
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        } finally {
            Status.setCompletedDay(CompletedKeyEnum.NeverLandSpecial);
        }
    }

    private void viewDailyAds() {
        try {
            try {
            } catch (Exception e) {
                Log.printStackTrace(this.TAG, e);
            }
            if (!Status.getCompletedDay(CompletedKeyEnum.NeverLandDailyAds)) {
                for (int i = 0; i < 7; i++) {
                    JSONObject requestString = requestString("com.alipay.neverland.biz.rpc.viewDailyAds", "\"source\":\"ch_appid-20001003__chsub_pageid-com.alipay.android.phone.businesscommon.globalsearch.ui.MainSearchActivity\"");
                    if (requestString != null) {
                        Object valueByPathObject = JsonUtil.getValueByPathObject(requestString, "data.userItems");
                        if (valueByPathObject != null) {
                            JSONArray jSONArray = (JSONArray) valueByPathObject;
                            for (int i2 = 0; i2 < jSONArray.length(); i2++) {
                                JSONObject jSONObject = jSONArray.getJSONObject(i2);
                                Log.other(this.displayName + "每日广告[" + jSONObject.optInt("modifyCount") + "]" + jSONObject.optString(Action.NAME_ATTRIBUTE));
                            }
                        }
                    }
                }
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        } finally {
            Status.setCompletedDay(CompletedKeyEnum.NeverLandDailyAds);
        }
    }

    private void queryBubbleTask() {
        JSONObject requestString;
        try {
            JSONObject requestString2 = requestString("com.alipay.neverland.biz.rpc.queryBubbleTask", "\"source\":\"jkddicon\",\"sportsAuthed\": true");
            if (requestString2 == null) {
                return;
            }
            JSONArray jSONArray = requestString2.getJSONObject(ApplicationHook.AlipayBroadcastReceiver.EXTRA_DATA).getJSONArray("bubbleTaskVOS");
            JSONArray jSONArray2 = new JSONArray();
            for (int i = 0; i < jSONArray.length(); i++) {
                JSONObject jSONObject = jSONArray.getJSONObject(i);
                String optString = jSONObject.optString("medEnergyBallInfoRecordId");
                if (!optString.isEmpty() && "TO_RECEIVE".equals(jSONObject.optString("bubbleTaskStatus"))) {
                    jSONArray2.put(optString);
                }
            }
            if (jSONArray2.length() == 0 || (requestString = requestString("com.alipay.neverland.biz.rpc.pickBubbleTaskEnergy", "\"source\": \"jkddicon\",\"pickAllEnergyBall\": true,\"medEnergyBallInfoRecordIds\": " + jSONArray2)) == null) {
                return;
            }
            Log.other(this.displayName + "领取奖励[" + JsonUtil.getValueByPath(requestString, "data.changeAmount") + "]能量");
        } catch (Exception e) {
            Log.printStackTrace(this.TAG, e);
        }
    }

    private void offlineAward() {
        JSONArray optJSONArray;
        try {
            JSONObject requestString = requestString("com.alipay.neverland.biz.rpc.offlineAward", "\"isAdvertisement\":false,\"source\":\"jkddicon\"");
            if (requestString == null || (optJSONArray = requestString.getJSONObject(ApplicationHook.AlipayBroadcastReceiver.EXTRA_DATA).optJSONArray("userItems")) == null) {
                return;
            }
            for (int i = 0; i < optJSONArray.length(); i++) {
                Log.other(this.displayName + "领取离线奖励[" + JsonUtil.getValueByPath(optJSONArray.getJSONObject(i), "modifyCount") + "]能量");
            }
        } catch (Exception e) {
            Log.printStackTrace(this.TAG, e);
        }
    }

    private void queryMaps() {
        try {
            JSONObject requestString = requestString("com.alipay.neverland.biz.rpc.queryMaps", "\"branchId\": \"MASTER\",\"source\":\"jkddicon\"");
            if (requestString == null) {
                return;
            }
            JSONArray jSONArray = requestString.getJSONObject(ApplicationHook.AlipayBroadcastReceiver.EXTRA_DATA).getJSONArray("commonMap");
            for (int i = 0; i < jSONArray.length(); i++) {
                JSONObject jSONObject = jSONArray.getJSONObject(i);
                if ("DOING".equals(jSONObject.optString(NotificationCompat.CATEGORY_STATUS))) {
                    this.mapId = jSONObject.optString("mapId");
                    jSONObject.getJSONArray("reward");
                    return;
                }
            }
        } catch (Exception e) {
            Log.printStackTrace(this.TAG, e);
        }
    }

    private boolean queryMapStageRewardInfo() {
        try {
            JSONObject requestString = requestString("com.alipay.neverland.biz.rpc.queryMapStageRewardInfo", "\"branchId\": \"MASTER\",\"source\": \"jkddicon\",\"mapId\": \"" + this.mapId + "\"");
            if (requestString == null) {
                return true;
            }
            JSONArray jSONArray = requestString.getJSONObject(ApplicationHook.AlipayBroadcastReceiver.EXTRA_DATA).getJSONArray("specialActivityQueryResults");
            boolean z = false;
            for (int i = 0; i < jSONArray.length(); i++) {
                String valueByPath = JsonUtil.getValueByPath(jSONArray.getJSONObject(i), "functionVO.code");
                if ("TO_RECEIVE".equals(valueByPath)) {
                    this.levelIds.add(Integer.valueOf(i + 1));
                    z = true;
                } else if (!"RECEIVED".equals(valueByPath)) {
                    return true;
                }
            }
            return z;
        } catch (Exception e) {
            Log.printStackTrace(this.TAG, e);
            return true;
        }
    }

    private void mapStageReward(int i) {
        Object valueByPathObject;
        try {
            JSONObject requestString = requestString("com.alipay.neverland.biz.rpc.mapStageReward", "\"branchId\": \"MASTER\",\"level\": " + i + ",\"source\": \"jkddicon\",\"mapId\": \"" + this.mapId + "\"");
            if (requestString == null || (valueByPathObject = JsonUtil.getValueByPathObject(requestString, "data.receiveResult.prizes.[0]")) == null) {
                return;
            }
            JSONObject jSONObject = (JSONObject) valueByPathObject;
            Log.other(this.displayName + "领取地图" + i + "阶段奖励[" + jSONObject.optString("modifyCount") + jSONObject.optString("title") + "]");
        } catch (Exception e) {
            Log.printStackTrace(this.TAG, e);
        }
    }

    private void exchangePrize() {
        try {
            JSONObject requestString = requestString("com.alipay.neverland.biz.rpc.queryExchangeModule", "\"assetType\": \"RED_PACKAGE_PIECE\",\"source\": \"jkddicon\"");
            if (requestString == null) {
                return;
            }
            JSONObject jSONObject = requestString.getJSONObject(ApplicationHook.AlipayBroadcastReceiver.EXTRA_DATA);
            int parseInt = Integer.parseInt(jSONObject.getJSONObject("mediumModule").optString("expiringAmount"));
            if (parseInt <= 0) {
                return;
            }
            JSONObject jSONObject2 = jSONObject.getJSONObject("exchangePrizeModule");
            String string = jSONObject2.getString("campId");
            JSONArray jSONArray = jSONObject2.getJSONArray("exchangePrizes");
            for (int length = jSONArray.length() - 1; length >= 0; length--) {
                JSONObject jSONObject3 = jSONArray.getJSONObject(length);
                String string2 = jSONObject3.getString("statusCode");
                int i = jSONObject3.getInt("consumeMediumAmount");
                if (!"POINT_NOT_ENOUGH".equals(string2) && i - parseInt <= 500) {
                    String string3 = jSONObject3.getString("prizeId");
                    String string4 = jSONObject3.getString("prizeName");
                    if (requestString("com.alipay.neverland.biz.rpc.doMediumExchangePrize", "\"assetType\": \"RED_PACKAGE_PIECE\",\"prizeId\": \"" + string3 + "\",\"campId\": \"" + string + "\",\"source\": \"jkddicon\"") != null) {
                        String str = this.displayName + "快过期红包碎片兑换[" + string4 + "]，请及时使用~";
                        Log.other(str);
                        NotificationUtil.showNotification(ApplicationHook.getContext(), str);
                        return;
                    }
                }
            }
        } catch (Exception e) {
            Log.printStackTrace(this.TAG, e);
        }
    }
}
