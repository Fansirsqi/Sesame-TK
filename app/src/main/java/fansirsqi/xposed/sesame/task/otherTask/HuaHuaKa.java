package fansirsqi.xposed.sesame.task.otherTask;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import fansirsqi.xposed.sesame.data.Status;
import fansirsqi.xposed.sesame.hook.RequestManager;
import fansirsqi.xposed.sesame.util.JsonUtil;
import fansirsqi.xposed.sesame.util.Log;
import fansirsqi.xposed.sesame.util.TimeUtil;

public class HuaHuaKa extends BaseCommTask {
    String certId;
    private final String productCode;
    private final String sceneCode;

    private void queryV2() {
        try {
            JSONObject requestString = requestString("com.alipay.pcreditbfweb.sdk.task.queryV2", "\"requestFrom\": \"pccp\",\"scene\":\"HUA_HUA_CARD\"");
            if (requestString != null && requestString.getBoolean("success")) {
                JSONArray jSONArray = requestString.getJSONArray("data");

                // 用于存储满足条件的任务ID和任务中心ID
                JSONArray jSONArray2 = new JSONArray();
                JSONArray jSONArray3 = new JSONArray();

                boolean hasUncompletedTask = false; // 是否存在未完成的任务
                int i2 = 0;
                while (i2 < jSONArray.length()) {
                    JSONObject task = jSONArray.getJSONObject(i2);

                    // 获取任务类型，判断是否是 look 类型
                    String taskType = task.optString("taskType");
                    if ("look".equals(taskType)) {

                        String str2 = this.certId;
                        if (str2 == null || str2.isEmpty()) {
                            this.certId = JsonUtil.getValueByPath(task, "taskBaseInfo.prizeInfos.[0].extInfo.CERT_TEMPLATE_ID");
                        }

                        String taskId = task.getString("taskId");
                        String taskCenId = task.getString("taskCenId");
                        String taskStatus = task.getString("taskStatus");

                        String title = JsonUtil.getValueByPath(task, "taskShowInfo.title");

                        // 如果未完成报名，则先报名
                        if ("NONE_SIGNUP".equals(taskStatus)) {
                            trigger(taskCenId, taskId, "signup");
                        }

                        // 提交完成任务
                        trigger(taskCenId, taskId, "send");
                        Log.other(this.displayName + "完成任务[" + title + "]");
                        hasUncompletedTask = true; // 存在至少一个未完成任务

                        jSONArray2.put(taskCenId);
                        jSONArray3.put(taskId);
                    }
                    i2++;
                }
                // 如果没有需要处理的 look 类型任务，说明全部已完成
                if (!hasUncompletedTask) {
                    Status.setFlagToday("HuaHuaKa_TaskCompleted"); // 设置已完成标记
                    TimeUtil.sleep((long) this.executeIntervalInt);
                    return; // 直接返回，不再领取奖励
                }

                // 领取奖励
                if (jSONArray2.length() > 0) {
                    JSONObject awardResponse = requestString("com.alipay.pcreditbfweb.sdk.task.award", "\"taskCenIds\":" + jSONArray2 + ",\"taskIds\":" + jSONArray3);
                    if (awardResponse != null) {
                        JSONArray resultData = (JSONArray) JsonUtil.getValueByPathObject(awardResponse, "data.resultData");
                        if (resultData != null) {
                            for (int i = 0; i < resultData.length(); i++) {
                                JSONObject prizeObj = resultData.getJSONObject(i);
                                String prizeName = JsonUtil.getValueByPath(prizeObj, "prizeSendOrderList.[0].prizeName");
                                Log.other(this.displayName + "获得[" + prizeName + "]");
                            }
                        }
                    }
                }
                TimeUtil.sleep((long) this.executeIntervalInt);
            }
        } catch (Throwable th) {
            Log.error(displayName+"queryV2 error: "+th);
            TimeUtil.sleep((long) this.executeIntervalInt);
        }
    }

    public static long convertToTimestamp(String isoTime) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC")); // 设置为 UTC 时区
        try {
            Date date = sdf.parse(isoTime);
            return date.getTime(); // 得到 13 位时间戳
        } catch (ParseException e) {
            e.printStackTrace();
            return -1; // 出错返回 -1
        }
    }
    private void campConsult(String startTime,String endTime) {
        long startTimestamp = convertToTimestamp(startTime);
        long endTimestamp = convertToTimestamp(endTime);
        TimeUtil.sleep(1000);
        String str2 = "\"args\":";
        // 定义所有阶段ID
        String[] campIds = {
                "HHK_SINGLE_CAMP_1",
                "HHK_SINGLE_CAMP_2",
                "HHK_SINGLE_CAMP_3"
        };

        for (String campId : campIds) {
            try {
                JSONObject jSONObject = new JSONObject();
                jSONObject.put("endTime", endTimestamp);
                jSONObject.put("playId", campId);
                jSONObject.put("requestFrom", "pccp");
                jSONObject.put("startTime", startTimestamp);

                String requestString0 = RequestManager.requestString(
                        "com.alipay.pcreditbfweb.drpc.collect.campConsult",
                        "[{\"endTime\":" + endTimestamp
                                + ",\"playId\":\"" + campId
                                + "\",\"requestFrom\":\"pccp\""
                                + ",\"startTime\":" + startTimestamp + "}]"
                );
                JSONObject requestString = new JSONObject(requestString0);
                if (requestString != null) {
                    JSONArray jSONArray = (JSONArray) JsonUtil.getValueByPathObject(requestString, "data.result.result.nodeList");
                    if (jSONArray != null) {
                        for (int i = 0; i < jSONArray.length(); i++) {
                            JSONObject jSONObject2 = jSONArray.getJSONObject(i);
                            String title = jSONObject2.getString("title");
                            String status = jSONObject2.getString("status");
                            String nodeId = JsonUtil.getValueByPath(jSONObject2, "config.id");

                            if (!(nodeId.isEmpty() || "RECEIVE".equals(status) || "DISABLE".equals(status))) {
                                jSONObject.put("nodeId", nodeId);
                                requestString("com.alipay.pcreditbfweb.drpc.collect.campTrigger", str2 + jSONObject);
                                Log.other(this.displayName + "任务阶段奖励领取[" + title + "]");
                            }
                        }
                        Status.setFlagToday("HuaHuaKaCollect");
                    }
                }

                TimeUtil.sleep((long) this.executeIntervalInt);

            } catch (Throwable th) {
                Log.error("campConsult error: " + campId+"因为:"+ th);
                TimeUtil.sleep((long) this.executeIntervalInt);
            }
        }
    }


    private void index() {
        try {
            String str = "CARD_HUA_HUA_CARD_23Y06";
            String str2 = "CARD_HUA_HUA_CARD";
            if (!this.certId.isEmpty()) {
                int i = 3;
                do {
                    JSONObject requestString = requestString("com.alipay.pcreditbfweb.promo.hhk.index", "\"certId\":\"" + this.certId + "\",\"productCode\":\"" + "HUA_HUA_CARD_NORMAL_23Y06" + "\",\"productCodeFlop\":\"" + str + "\",\"sceneCode\":\"" + "HUA_HUA_CARD" + "\",\"sceneCodeFlop\":\"" + str2 + "\"");
                    if (requestString != null) {
                        requestString = requestString.getJSONObject("data");
                        int i2 = requestString.getInt("remainingTimes");
                        if (i2 != 0) {
                            Log.other(this.displayName + "还可翻[" + i2 + "]次");
                            String str3 = "合卡失败，停止翻卡";
                            if (merge(requestString.getJSONArray("fragments"))) {
                                JSONArray jSONArray = requestString.getJSONArray("cardPrizes");
                                for (int i3 = 0; i3 < jSONArray.length(); i3++) {
                                    if (i <= 0) {
                                        continue;
                                    }
                                    JSONObject jSONObject = jSONArray.getJSONObject(i3);
                                    if (!jSONObject.getBoolean("isOpen")) {
                                        boolean z;
                                        int flopCard;
                                        int parseInt = Integer.parseInt(JsonUtil.getValueByPath(jSONObject, "position.index"));
                                        if (i3 != 0) {
                                            if (!jSONObject.getBoolean("isNewPageBegin")) {
                                                z = false;
                                                flopCard = flopCard(str, str2, parseInt, z);
                                                if (flopCard != 0) {
                                                    Log.other(this.displayName + str3);
                                                } else {
                                                    if (flopCard == 2) {
                                                        i--;
                                                    }
                                                    i2--;
                                                    if (i2 <= 0) {
                                                    }
                                                }
                                            }
                                        }
                                        z = true;
                                        flopCard = flopCard(str, str2, parseInt, z);
                                        if (flopCard != 0) {
                                        }
                                    }
                                }
                            } else {
                                Log.other(this.displayName + str3);
                            }
                        }
                    }
                } while (i > 0);
                TimeUtil.sleep((long) this.executeIntervalInt);
                return;
            }
            TimeUtil.sleep((long) this.executeIntervalInt);
        } catch (Throwable th) {
            TimeUtil.sleep((long) this.executeIntervalInt);
        }
    }

    private boolean merge(JSONArray jSONArray) throws JSONException {
        if (jSONArray != null) {
            int i = 0;
            while (i < jSONArray.length()) {
                try {
                    int i2 = jSONArray.getJSONObject(i).getInt("number");
                    if (i2 == 0) {
                        break;
                    } else if (i2 > 1) {
                        break;
                    } else {
                        i++;
                    }
                } catch (Throwable th) {
                    TimeUtil.sleep((long) this.executeIntervalInt);
                    throw th;
                }
            }
            JSONObject requestString = requestString("com.alipay.pcreditbfweb.promo.hhk.index.merge",
                    "\"productCode\":\"HUA_HUA_CARD_NORMAL_23Y06\",\"sceneCode\":\"HUA_HUA_CARD\"");

            if (requestString != null) {
                requestString = requestString.getJSONObject("data");
                String string = requestString.getString("campId");
                requestString = requestString("com.alipay.pcreditbfweb.drpc.pageQueryPrizeSendOrderLite", "\"args\":{\"campIds\":[\"" + string + "\"],\"outBizNo\":\"" + requestString.getString("bizNo") + "\",\"pageNum\":1,\"perPageSize\":10}");
                if (requestString != null) {
                    Log.other(this.displayName + "合卡获得[" + JsonUtil.getValueByPath(requestString, "data.result.resultData.dataList.[0].prizeName") + "]");
                }
            }
            TimeUtil.sleep((long) this.executeIntervalInt);
            return false;
        }
        TimeUtil.sleep((long) this.executeIntervalInt);
        return true;
    }

    private int flopCard(String str, String str2, int i, boolean z) throws JSONException {
        try {
            JSONObject requestString = requestString("com.alipay.pcreditbfweb.promo.hhk.index.flopcard", "\"isNewPageBegin\":" + z + ",\"lineIndex\":" + i + ",\"productCode\":\"" + str + "\",\"sceneCode\":\"" + str2 + "\"");
            if (requestString != null) {
                Log.other(this.displayName + "翻卡获得[" + JsonUtil.getValueByPath(requestString, "data.playPrizeList.[0].prizeName") + "]");
                requestString = requestString("com.alipay.pcreditbfweb.promo.hhk.index.refreshFragments", "\"productCode\":\"HUA_HUA_CARD_NORMAL_23Y06\",\"sceneCode\":\"HUA_HUA_CARD\"");
                if (requestString != null) {
                    boolean merge = merge((JSONArray) JsonUtil.getValueByPathObject(requestString, "data.charNumberList"));
                    TimeUtil.sleep((long) this.executeIntervalInt);
                    return 2;
                }
            }
            TimeUtil.sleep((long) this.executeIntervalInt);
            return 2;
        } catch (Throwable th) {
            TimeUtil.sleep((long) this.executeIntervalInt);
            throw th;
        }
    }

    private void indexTrigger() {
        try {
            if (!Status.hasFlagToday(CompletedKeyEnum.HuaHuaKaSign.name())) {
                JSONObject requestString = requestString("com.alipay.pcreditbfweb.promo.index.trigger", "\"campId\": \"CP14460747\"");
                if (requestString != null) {
                    Log.other(this.displayName + "签到成功[" + JsonUtil.getValueByPath(requestString, "data.prizeSendOrderList.[0].prizeName") + "]");
                    Status.setFlagToday(CompletedKeyEnum.HuaHuaKaSign.name());
                    TimeUtil.sleep((long) this.executeIntervalInt);
                    return;
                }
            }
            TimeUtil.sleep((long) this.executeIntervalInt);
        } catch (Throwable th) {
            TimeUtil.sleep((long) this.executeIntervalInt);
        }
    }

    private boolean trigger(String str, String str2, String str3) throws JSONException {
        boolean z = false;
        try {
            if (requestString("com.alipay.pcreditbfweb.sdk.task.trigger", "\"appletId\": \"" + str2 + "\",\"outBizNo\": \"" + str2 + TimeUtil.getMinuteTimestamp() + "\",\"taskCenId\": \"" + str + "\",\"retryFlag\": true,\"stageCode\":\"" + str3 + "\"") != null) {
                z = true;
            }
            TimeUtil.sleep((long) this.executeIntervalInt);
            return z;
        } catch (Throwable th) {
            TimeUtil.sleep((long) this.executeIntervalInt);
            throw th;
        }
    }

    public HuaHuaKa() {
        this.productCode = "HUA_HUA_CARD_NORMAL_23Y06";
        this.sceneCode = "HUA_HUA_CARD";
        this.displayName = "花花卡💴";
        //this.hoursKeyEnum = CompletedKeyEnum.HuaHuaKa;
    }

    protected void handle() {
        indexTrigger();//签到
        queryV2();//查询并执行任务
        index();//翻牌
    }
}