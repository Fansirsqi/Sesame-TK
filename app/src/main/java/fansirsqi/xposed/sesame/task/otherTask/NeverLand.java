package fansirsqi.xposed.sesame.task.otherTask;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import fansirsqi.xposed.sesame.hook.ApplicationHook;
import fansirsqi.xposed.sesame.hook.RequestManager;
import fansirsqi.xposed.sesame.model.modelFieldExt.IntegerModelField;
import fansirsqi.xposed.sesame.util.JsonUtil;
import fansirsqi.xposed.sesame.util.Log;
import fansirsqi.xposed.sesame.util.Notify;
import fansirsqi.xposed.sesame.util.RandomUtil;
import fansirsqi.xposed.sesame.util.TimeUtil;
import fansirsqi.xposed.sesame.data.Status;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class NeverLand extends BaseCommTask {
    private List<Integer> levelIds;
    private String mapId;
    private final String branchId;

    public NeverLand() {
        this.mapId = "MM17";
        this.branchId = "MASTER";
        this.levelIds = new ArrayList<>();
        this.displayName = "悦动健康岛🍰";
        //this.hoursKeyEnum = CompletedKeyEnum.Neverland;
    }

    private void mapStageReward(int i) {
        try {
            JSONObject requestString = requestString("com.alipay.neverland.biz.rpc.mapStageReward", "\"branchId\": \"MASTER\",\"level\": " + i + ",\"source\": \"jkddicon\",\"mapId\": \"" + this.mapId + "\"");
            if (requestString != null) {
                Object valueByPathObject = JsonUtil.getValueByPathObject(requestString, "data.receiveResult.prizes.[0]");
                if (valueByPathObject != null) {
                    requestString = (JSONObject) valueByPathObject;
                    Log.other(this.displayName + "领取地图" + i + "阶段奖励[" + requestString.optString("modifyCount") + requestString.optString("title") + "]");
                }
            }
        } catch (Exception e) {
            Log.printStackTrace(this.TAG, e);
        }
    }

    private void offlineAward() {
        try {
            JSONObject requestString = requestString("com.alipay.neverland.biz.rpc.offlineAward", "\"isAdvertisement\":false,\"source\":\"jkddicon\"");
            if (requestString != null) {
                JSONArray optJSONArray = requestString.getJSONObject("data").optJSONArray("userItems");
                if (optJSONArray != null) {
                    for (int i = 0; i < optJSONArray.length(); i++) {
                        JSONObject jSONObject = optJSONArray.getJSONObject(i);
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append(this.displayName);
                        stringBuilder.append("领取离线奖励[");
                        stringBuilder.append(JsonUtil.getValueByPath(jSONObject, "modifyCount"));
                        stringBuilder.append("]能量");
                        Log.other(stringBuilder.toString());
                    }
                }
            }
        } catch (Exception e) {
            Log.printStackTrace(this.TAG, e);
        }
    }

    private void queryBaseinfo() {
        try {
            JSONObject requestString = requestString("com.alipay.neverland.biz.rpc.queryBaseinfo", "\"branchId\": \"MASTER\",\"source\":\"jkddicon\"");
            if (requestString != null) {
                this.mapId = requestString.getJSONObject("data").getString("mapId");
            }
        } catch (Exception e) {
            Log.printStackTrace(this.TAG, e);
        }
    }

    private void queryBubbleTask() {
        try {
            JSONObject requestString = requestString("com.alipay.neverland.biz.rpc.queryBubbleTask", "\"source\":\"jkddicon\"");
            if (requestString != null && requestString.optBoolean("success")) {
                JSONArray jSONArray = requestString.getJSONObject("data").getJSONArray("bubbleTaskVOS");
                JSONArray jSONArray2 = new JSONArray();

                String title ="未知任务";
                for (int i = 0; i < jSONArray.length(); i++) {
                    JSONObject taskVO = jSONArray.getJSONObject(i);
                    String recordId = taskVO.optString("medEnergyBallInfoRecordId");
                    title = taskVO.optString("title", "未知任务");
                    if (!recordId.isEmpty()) {
                        jSONArray2.put(recordId);
                    }
                }

                if (jSONArray2.length() != 0) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("\"source\": \"jkddicon\",\"medEnergyBallInfoRecordIds\": ");
                    stringBuilder.append(jSONArray2);

                    requestString = requestString("com.alipay.neverland.biz.rpc.pickBubbleTaskEnergy", stringBuilder.toString());

                    if (requestString != null && requestString.optBoolean("success")) {
                        int changeAmount = requestString.optInt("data.changeAmount", 0);
                        Log.other(this.displayName + "领取["+title+"]奖励[" + changeAmount + "]能量");
                    } else {
                        String errorMsg = requestString.optString("errorMsg", "未知错误");
                        Log.error(this.TAG, "领取能量球失败：" + errorMsg);
                    }
                } else {
                    Log.other(this.displayName + "无可领取的能量球");
                }
                TimeUtil.sleep(RandomUtil.nextInt(15000, 20000));
            }
        } catch (Exception e) {
            Log.printStackTrace(this.TAG, e);
        }
    }


    private boolean queryMapStageRewardInfo() {
        boolean z = true;
        try {
            StringBuilder stringBuilder = new StringBuilder("\"branchId\": \"MASTER\",\"source\": \"jkddicon\",\"mapId\": \"");
            stringBuilder.append(this.mapId);
            stringBuilder.append("\"");
            JSONObject requestString = requestString("com.alipay.neverland.biz.rpc.queryMapStageRewardInfo", stringBuilder.toString());
            if (requestString == null) {
                return true;
            }
            JSONArray jSONArray = requestString.getJSONObject("data").getJSONArray("specialActivityQueryResults");
            boolean z2 = false;
            for (int i = 0; i < jSONArray.length(); i++) {
                String valueByPath = JsonUtil.getValueByPath(jSONArray.getJSONObject(i), "functionVO.code");
                if ("TO_RECEIVE".equals(valueByPath)) {
                    int levelId = i + 1;
                    this.levelIds.add(levelId);
                    mapStageReward(levelId);
                    z2 = true;
                } else if (!"RECEIVED".equals(valueByPath)) {
                    break;
                }
            }
            z = z2;
            return z;
        } catch (Exception e) {
            Log.printStackTrace(this.TAG, e);
            return true;
        }
    }

    private void queryMaps() {
        try {
            JSONObject requestString = requestString("com.alipay.neverland.biz.rpc.queryMaps", "\"branchId\": \"MASTER\",\"source\":\"jkddicon\"");
            if (requestString != null) {
                JSONArray jSONArray = requestString.getJSONObject("data").getJSONArray("commonMap");
                for (int i = 0; i < jSONArray.length(); i++) {
                    JSONObject jSONObject = jSONArray.getJSONObject(i);
                    if ("DOING".equals(jSONObject.optString("status"))) {
                        this.mapId = jSONObject.optString("mapId");
                        jSONObject.getJSONArray("reward");
                        break;
                    }
                }
            }
        } catch (Exception e) {
            Log.printStackTrace(this.TAG, e);
        }
    }

    private void queryTaskCenter() {
        try {
            JSONObject requestString = requestString("com.alipay.neverland.biz.rpc.queryTaskCenter", "\"apDid\": \"N/JRfNgEj3Ry133cmrJZBVMIAFW92dVqjEyrrpB7ims=\",\"cityCode\": \"110100\",\"deviceLevel\": \"high\",\"source\": \"jkddicon\"");
            if (requestString != null && requestString.optBoolean("success")) {
                Object valueByPathObject = JsonUtil.getValueByPathObject(requestString, "data.taskCenterTaskVOS");
                if (valueByPathObject != null) {
                    JSONArray jSONArray = (JSONArray) valueByPathObject;
                    for (int i = 0; i < jSONArray.length(); i++) {
                        JSONObject jSONObject = jSONArray.getJSONObject(i);
                        String string = jSONObject.getString("taskStatus");
                        String string2 = jSONObject.getString("taskType");
                        if (!"RECEIVE_SUCCESS".equals(string)&&!"TO_RECEIVE".equals(string)) {
                            if (!"GAME_TASK".equals(string2)) {
                                taskSend(jSONObject);
                                TimeUtil.sleep((long) this.executeIntervalInt);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.printStackTrace(this.TAG, e);
        }
    }

    private void queryTaskInfo() {
        while (true) {
            try {
                JSONObject requestString = requestString("com.alipay.neverland.biz.rpc.queryTaskInfo", "\"source\":\"health-island\",\"type\":\"LIGHT_FEEDS_TASK\"");
                if (requestString != null &&  requestString.optBoolean("success")) {
                    Object valueByPathObject = JsonUtil.getValueByPathObject(requestString, "data.taskInfos.[0]");
                    if (valueByPathObject != null) {
                        requestString = (JSONObject) valueByPathObject;
                        String string = requestString.getString("encryptValue");
                        int i = requestString.getInt("energyNum");
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("\"encryptValue\": \"");
                        stringBuilder.append(string);
                        stringBuilder.append("\",\"energyNum\":\"");
                        stringBuilder.append(i);
                        stringBuilder.append("\",\"source\":\"jkdwodesign\",\"type\":\"LIGHT_FEEDS_TASK\"");
                        if (requestString("com.alipay.neverland.biz.rpc.energyReceive", stringBuilder.toString()) != null) {
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append(this.displayName);
                            stringBuilder2.append("任务获得[");
                            stringBuilder2.append(i);
                            stringBuilder2.append("]能量");
                            Log.other(stringBuilder2.toString());
                        } else {
                            return;
                        }
                    }
                    return;
                }
                return;
            } catch (Exception e) {
                Log.printStackTrace(this.TAG, e);
                return;
            }
        }
    }

    private void sign() {
        try {
            JSONObject requestString = requestString("com.alipay.neverland.biz.rpc.querySign", "\"source\": \"jkdwodesign\"");
            if (requestString != null && requestString.optBoolean("success")) {
                Object valueByPathObject = JsonUtil.getValueByPathObject(requestString, "data.days");
                if (valueByPathObject != null) {
                    JSONArray jSONArray = (JSONArray) valueByPathObject;
                    for (int i = 0; i < jSONArray.length(); i++) {
                        JSONObject jSONObject = jSONArray.getJSONObject(i);
                        if (jSONObject.getBoolean("current")) {
                            if (!jSONObject.getBoolean("signIn")) {
                                requestString = requestString("com.alipay.neverland.biz.rpc.takeSign", "\"source\":\"jkddicon\"");
                                if (requestString != null) {
                                    StringBuilder stringBuilder = new StringBuilder();
                                    stringBuilder.append(this.displayName);
                                    stringBuilder.append("签到成功，获得[");
                                    stringBuilder.append(JsonUtil.getValueByPath(requestString, "data.userItems.[0].modifyCount"));
                                    stringBuilder.append("]");
                                    Log.other(stringBuilder.toString());
                                } else {
                                    return;
                                }
                            }
                            Status.setFlagToday(CompletedKeyEnum.NeverlandSign.name());
                        }
                    }
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
            jSONObject.put("progress", 1);
            jSONObject.put("source", "jkdwodesign");
            jSONObject2 = jSONObject.toString();
            JSONObject requestString = requestString("com.alipay.neverland.biz.rpc.taskReceive", jSONObject2.substring(1, jSONObject2.length() - 1));
            if (requestString != null && requestString.optBoolean("success")) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(this.displayName);
                stringBuilder.append("完成任务[");
                stringBuilder.append(jSONObject.getString("title"));
                stringBuilder.append("]+");
                stringBuilder.append(JsonUtil.getValueByPath(requestString, "data.userItems.[0].modifyCount"));
                Log.other(stringBuilder.toString());
            }
        } catch (Exception e) {
           Log.error(TAG,".taskSend错误:"+e);
        }
    }

    private void walkGrid() {
        try {
            if (!this.mapId.isEmpty()) {
                JSONObject requestString = requestString("com.alipay.neverland.biz.rpc.walkGrid",
                        "\"branchId\": \"MASTER\",\"drilling\": false,\"mapId\": \"" + this.mapId + "\",\"source\":\"jkddicon\"");

                if (requestString != null && requestString.getBoolean("success")) {
                    JSONObject data = requestString.getJSONObject("data");

                    int leftCount = data.optInt("leftCount", 0);

                    // 安全获取userItems[0]
                    JSONArray userItems = data.optJSONArray("userItems");
                    JSONObject userItem = (userItems != null && userItems.length() > 0)
                            ? userItems.optJSONObject(0) : null;

                    // 安全获取starData
                    JSONObject starData = data.optJSONObject("starData");

                    // 构建日志信息
                    String step = JsonUtil.getValueByPath(data, "mapAwards.[0].step");
                    String logMessage = this.displayName + "跳跳跳，前进[" + step + "]步]剩余能量：" + leftCount;

                    if (userItem != null) {
                        String modifyCount = userItem.optString("modifyCount", "0");
                        String name = userItem.optString("name", "");
                        logMessage += "，获得[" + modifyCount + name + "]";
                    }

                    Log.other(logMessage);

                    // 安全获取starData中的值
                    int curr = starData != null ? starData.optInt("curr", 0) : 0;
                    int count = starData != null ? starData.optInt("count", 0) : 0;
                    int rewardLevel = starData != null ? starData.optInt("rewardLevel", 0) : 0;

                    // 安全获取count值
                    int itemCount = 0;
                    if (userItem != null) {
                        itemCount = userItem.optInt("count", 0);
                    }

                    Log.other(displayName + "当前奖励等级[" + rewardLevel + "]--红包碎片[" + itemCount + "]--星星数量[" + curr + "/" + count + "⭐]");

                    if (leftCount >= 5) {
                        TimeUtil.sleep((long) this.executeIntervalInt);
                    }
                }
            }
        } catch (Exception e) {
            Log.printStackTrace(this.TAG, e);
        }
    }

    private void xlightPlugin(JSONObject jSONObject) {
        try {
            JSONObject logExtMap = jSONObject.getJSONObject("logExtMap");
            String title = jSONObject.optString("title", "未知任务");
            String bizId = logExtMap.getString("bizId");

            StringBuilder stringBuilder = new StringBuilder("\"bizId\": \"");
            stringBuilder.append(bizId).append("\"");

            JSONObject response = requestString("com.alipay.adtask.biz.mobilegw.service.task.finish", stringBuilder.toString());

            if (response != null && response.getBoolean("success")) {
                String prizeCount = JsonUtil.getValueByPath(jSONObject, "prizes.[0].prizeCount");
                queryBubbleTask();
                Log.other(this.displayName + "完成任务[" + title + "]+[" + prizeCount + "]");
            } else {
               Log.other(this.displayName+"失败["+title+"]");
            }
        } catch (Exception e) {
            Log.printStackTrace(TAG, e);
        }
    }

    private void exchangePrize() {
        try {
            JSONObject requestString = requestString("com.alipay.neverland.biz.rpc.queryExchangeModule", "\"assetType\": \"RED_PACKAGE_PIECE\",\"source\": \"jkddicon\"");
            if (requestString != null && requestString.getBoolean("success")) {
                requestString = requestString.getJSONObject("data");
                int parseInt = Integer.parseInt(requestString.getJSONObject("mediumModule").optString("expiringAmount"));
                if (parseInt > 0) {
                    requestString = requestString.getJSONObject("exchangePrizeModule");
                    String string = requestString.getString("campId");
                    JSONArray jSONArray = requestString.getJSONArray("exchangePrizes");
                    for (int length = jSONArray.length() - 1; length >= 0; length--) {
                        JSONObject jSONObject = jSONArray.getJSONObject(length);
                        String string2 = jSONObject.getString("statusCode");
                        int i = jSONObject.getInt("consumeMediumAmount");
                        if (!"POINT_NOT_ENOUGH".equals(string2)) {
                            if (i - parseInt <= 500) {
                                string2 = jSONObject.getString("prizeId");
                                String string3 = jSONObject.getString("prizeName");
                                if (requestString("com.alipay.neverland.biz.rpc.doMediumExchangePrize", "\"assetType\": \"RED_PACKAGE_PIECE\",\"prizeId\": \"" + string2 + "\",\"campId\": \"" + string + "\",\"source\": \"jkddicon\"") != null) {
                                    String str = this.displayName + "快过期红包碎片兑换[" + string3 + "]，请及时使用~";
                                    Log.other(str);
                                    String title = "健康岛兑换奖励：";
                                    int newNotificationId = 114708;
                                    Notify.sendNewNotification(ApplicationHook.getContext(),title, str,newNotificationId);
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.printStackTrace(this.TAG, e);
        }
    }
    private void receiveSpecialPrize() {
        try {
            if (!Status.hasFlagToday(CompletedKeyEnum.NeverLandSpecial.name())) {
                int i = 0;
                while (i < 2) {
                    JSONObject requestString = requestString("com.alipay.neverland.biz.rpc.receiveSpecialPrize", "\"medPrizeIds\":[\"benefitCenterActivityPrize1\"],\"sceneType\":\"BENEFIT_CENTER\"");
                    if (requestString != null) {
                        Log.other(this.displayName + "获得特别奖[" + requestString.getJSONObject("data").optInt("modifyCount") + "]能量");
                        i++;
                    }
                }
                Status.setFlagToday(CompletedKeyEnum.NeverLandSpecial.name());
                return;
            }
            Status.setFlagToday(CompletedKeyEnum.NeverLandSpecial.name());
        } catch (Exception e) {
            Log.printStackTrace(this.TAG, e);
        } catch (Throwable th) {
            Status.setFlagToday(CompletedKeyEnum.NeverLandSpecial.name());
        }
    }
    private void viewDailyAds() {
        try {
            if (!Status.hasFlagToday(CompletedKeyEnum.NeverLandDailyAds.name())) {
                int i = 0;
                while (i < 7) {
                    JSONObject requestString = requestString("com.alipay.neverland.biz.rpc.viewDailyAds", "\"source\":\"ch_appid-20001003__chsub_pageid-com.alipay.android.phone.businesscommon.globalsearch.ui.MainSearchActivity\"");
                    if (requestString != null &&  requestString.getBoolean("success")) {
                        Object valueByPathObject = JsonUtil.getValueByPathObject(requestString, "data.userItems");
                        if (valueByPathObject != null) {
                            JSONArray jSONArray = (JSONArray) valueByPathObject;
                            for (int i2 = 0; i2 < jSONArray.length(); i2++) {
                                JSONObject jSONObject = jSONArray.getJSONObject(i2);
                                Log.other(this.displayName + "每日广告[" + jSONObject.optInt("modifyCount") + "]" + jSONObject.optString("name"));
                            }
                        }
                        i++;
                    }
                }
                Status.setFlagToday(CompletedKeyEnum.NeverLandDailyAds.name());
                return;
            }
            Status.setFlagToday(CompletedKeyEnum.NeverLandDailyAds.name());
        } catch (Exception e) {
            Log.printStackTrace(this.TAG, e);
        } catch (Throwable th) {
            Status.setFlagToday(CompletedKeyEnum.NeverLandDailyAds.name());
        }
    }

    // 假设你有获取时间的方法，这里假设你获取到的时间是 9:30
    private static final String FLAG_MORNING = "neverLandOfflineAward_morning";
    private static final String FLAG_AFTERNOON = "neverLandOfflineAward_afternoon";
    private static final String FLAG_NIGHT = "neverLandOfflineAward_night";

    private String getCurrentTimePeriod() {
        int hour = TimeUtil.getHourOfDay();
        if (hour >= 6 && hour < 12) {
            return FLAG_MORNING;
        } else if (hour >= 12 && hour < 18) {
            return FLAG_AFTERNOON;
        } else {
            return FLAG_NIGHT;
        }
    }
    /**
     * 查询所有可以领取奖励的任务（bubbleTaskStatus = TO_RECEIVE），并收集它们的 medEnergyBallInfoRecordId
     *
     * @return 返回可领取的任务记录ID列表
     */
    private List<String> collectReceivableTasks() {
        List<String> recordIds = new ArrayList<>();
        Set<String> processedTitles = new HashSet<>();

        try {
            // 请求参数格式必须是 [{}]
            String params = "[{\"source\":\"ch_appid-20001003__chsub_pageid-com.alipay.android.phone.businesscommon.globalsearch.ui.MainSearchActivity\",\"sportsAuthed\":true}]";

            String responseStr = RequestManager.requestString("com.alipay.neverland.biz.rpc.queryBubbleTask", params);
            JSONObject response = new JSONObject(responseStr);

            if (response != null && response.optBoolean("success")) {
                JSONArray bubbleTaskVOS = response.getJSONObject("data").getJSONArray("bubbleTaskVOS");

                for (int i = 0; i < bubbleTaskVOS.length(); i++) {
                    JSONObject taskVO = bubbleTaskVOS.getJSONObject(i);

                    String title = taskVO.optString("title", "未知任务");
                    String status = taskVO.optString("bubbleTaskStatus");
                    String recordId = taskVO.optString("medEnergyBallInfoRecordId");

                    // 防止重复处理相同标题的任务（除“逛一逛”外）
                    if (processedTitles.contains(title)) {
                        if (!"逛一逛".equals(title)) {
                            continue;
                        }
                    }

                    // 只处理状态为 TO_RECEIVE 的任务
                    if ("TO_RECEIVE".equals(status)) {
                        if (!recordId.isEmpty()) {
                            recordIds.add(recordId);
                            processedTitles.add(title);
                        }
                    } else {
                        //Log.other(this.displayName + "跳过非 TO_RECEIVE 状态任务[" + title + "]，当前状态：" + status);
                        continue;
                    }
                }
            } else {
                String errorMsg = response != null ? response.optString("errorMsg", "无错误信息") : "空响应";
                Log.error(TAG, "collectReceivableTasks 请求失败：" + errorMsg);
            }
        } catch (Exception e) {
            Log.printStackTrace(TAG, e);
        }

        return recordIds;
    }

    /**
     * 一键领取多个能量球任务奖励
     *
     * @param recordIds 能量球记录ID列表
     */
    private void pickAllBubbleTaskEnergy(List<String> recordIds) {
        if (recordIds == null || recordIds.isEmpty()) {
            Log.other(displayName + "无可领取的能量球");
            return;
        }

        try {
            // 构造 JSON 参数，格式为 [{}]
            StringBuilder jsonBuilder = new StringBuilder();
            jsonBuilder.append("[{");
            jsonBuilder.append("\"pickAllEnergyBall\":true,");
            jsonBuilder.append("\"source\":\"ch_appid-20001003__chsub_pageid-com.alipay.android.phone.businesscommon.globalsearch.ui.MainSearchActivity\",");
            jsonBuilder.append("\"medEnergyBallInfoRecordIds\":[");
            for (int i = 0; i < recordIds.size(); i++) {
                jsonBuilder.append("\"").append(recordIds.get(i)).append("\"");
                if (i < recordIds.size() - 1) {
                    jsonBuilder.append(",");
                }
            }
            jsonBuilder.append("]");
            jsonBuilder.append("}]");

            String responseStr = RequestManager.requestString("com.alipay.neverland.biz.rpc.pickBubbleTaskEnergy", jsonBuilder.toString());
            JSONObject response = new JSONObject(responseStr);

            if (response != null && response.optBoolean("success")) {
                int changeAmount = response.optInt("data.changeAmount", 0);
                Log.other(displayName + "成功领取能量: " + changeAmount + " 单位");
            } else {
                String errorMsg = response != null ? response.optString("errorMsg", "未知错误") : "空响应";
                Log.error(TAG, displayName + "领取能量失败: " + errorMsg);
            }
        } catch (JSONException e) {
            Log.error(TAG, "构建领取请求JSON异常: " + e.getMessage());
        } catch (Exception e) {
            Log.printStackTrace(TAG, e);
        }
    }

    @Override
    protected void handle() {
        if (!Status.hasFlagToday(CompletedKeyEnum.NeverlandSign.name())) {
            sign();
            exchangePrize();
        }
        receiveSpecialPrize();
        viewDailyAds();
        queryBaseinfo();

        for (int i = 0; i < 4; i++) {
            queryTaskCenter();
            TimeUtil.sleep((long) this.executeIntervalInt);
        }

        queryTaskInfo();
        queryBubbleTask();
        queryMaps();

        // 查询可领取的任务
        List<String> recordIds = collectReceivableTasks();
        if (!recordIds.isEmpty()) {
            // 一键领取奖励
            pickAllBubbleTaskEnergy(recordIds);
        }


        String flagKey = getCurrentTimePeriod();

        if (!Status.hasFlagToday(flagKey)) {
            offlineAward();
            Status.setFlagToday(flagKey);
        }

        if (!Status.hasFlagToday(CompletedKeyEnum.NeverlandJump.name())){
            IntegerModelField neverLandJumpTIme = OtherTask.getneverLandJumpTIme();
            int jumpTimes = neverLandJumpTIme.getValue();

            IntegerModelField integerModelField = OtherTask.getneverLandJumpTImes();
            int times = integerModelField.getValue();
            if (Boolean.TRUE.equals(this.mapHandler.get("neverLandJump"))) {
                if (!queryMapStageRewardInfo()) {
                    Log.other(this.displayName + "地图阶段奖励已领完，注意手动切换地图");
                }

                for (int i = 0; i < times; i++) {
                    for (int j = 0; j < jumpTimes; j++) {
                        walkGrid(); // 执行跳跃任务
                        TimeUtil.sleep((long) this.executeIntervalInt);
                    }
                    if (i==times-1){
                        Status.setFlagToday(CompletedKeyEnum.NeverlandJump.name());
                    }
                }

            }
        }


    }
}
