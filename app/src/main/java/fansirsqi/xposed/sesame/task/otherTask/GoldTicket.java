package fansirsqi.xposed.sesame.task.otherTask;

import fansirsqi.xposed.sesame.data.Status;
import fansirsqi.xposed.sesame.util.JsonUtil;
import fansirsqi.xposed.sesame.util.Log;
import fansirsqi.xposed.sesame.util.TimeUtil;
import org.json.JSONArray;
import org.json.JSONObject;

public class GoldTicket extends BaseCommTask {
    public GoldTicket() {
        this.displayName = "黄金票🕌";
        //this.hoursKeyEnum = CompletedKeyEnum.GoldTicket;
    }

    private void getRankTasks() {
        int i = 0;
        while (i <= 3) {
            try {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("\"holdingBoardBaseInfo\": {\n                \"activityBizDate\": \"");
                stringBuilder.append(TimeUtil.DATE_FORMAT_THREAD_LOCAL);
                stringBuilder.append("\",\n                \"holdLevel\": \"TEN\",\n                \"holdStyle\": \"ALL\",\n                \"profitType\": \"AMOUNT\",\n                \"timeRange\": \"MONTH\",\n                \"type\": \"ACTIVITY\"\n            },");
                String stringBuilder2 = stringBuilder.toString();
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append(stringBuilder2);
                stringBuilder3.append("\"iphoneSystem\": \"14\"");
                JSONObject requestString = requestString("com.alipay.promobffweb.needle.getRankTasks", stringBuilder3.toString());
                if (requestString == null) {
                    break;
                }
                String string = requestString.getString("groupType");
                if ("TODAY_DONE".equals(string)) {
                    break;
                }
                String optString = requestString.optString("unlockTaskCertCampId");
                String string2 = requestString.getString("taskId");
                String string3 = requestString.getString("taskCenId");
                String string4 = requestString.getString("taskType");
                String string5 = requestString.getString("buttonDesc");
                StringBuilder stringBuilder4 = new StringBuilder();
                stringBuilder4.append(stringBuilder2);
                stringBuilder4.append("\"campId\": \"");
                stringBuilder4.append(optString);
                stringBuilder4.append("\",\"groupType\": \"");
                stringBuilder4.append(string);
                stringBuilder4.append("\",\"taskCenId\": \"");
                stringBuilder4.append(string3);
                stringBuilder4.append("\",\"taskId\": \"");
                stringBuilder4.append(string2);
                stringBuilder4.append("\",\"taskType\": \"");
                stringBuilder4.append(string4);
                stringBuilder4.append("\"");
                JSONObject requestString2 = requestString("com.alipay.promobffweb.needle.rankCompletTask", stringBuilder4.toString());
                if (requestString2 != null && requestString2.optBoolean("success")) {
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append(this.displayName);
                    stringBuilder3.append("完成任务[");
                    stringBuilder3.append(string5);
                    stringBuilder3.append("]获得");
                    stringBuilder3.append(requestString2.optString("prizeNumber"));
                    stringBuilder3.append(requestString2.optString("prizeTitleSubfix"));
                    Log.other(stringBuilder3.toString());
                }
                TimeUtil.sleep((long) this.executeIntervalInt);
            } catch (Throwable th) {
                TimeUtil.sleep((long) this.executeIntervalInt);
            }
        }
        TimeUtil.sleep((long) this.executeIntervalInt);
    }

    private void goldBillCollect(String str) {
        try {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(str);
            stringBuilder.append("\"trigger\":\"Y\"");
            JSONObject requestString = requestString("com.alipay.wealthgoldtwa.goldbill.v2.index.collect", stringBuilder.toString());
            if (requestString != null && requestString.optBoolean("success")) {
                JSONArray jSONArray = requestString.getJSONObject("result").getJSONArray("collectedList");
                int length = jSONArray.length();
                if (length != 0) {
                    for (int i = 0; i < length; i++) {
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append(this.displayName);
                        stringBuilder2.append("[");
                        stringBuilder2.append(jSONArray.getString(i));
                        stringBuilder2.append("]");
                        Log.other(stringBuilder2.toString());
                    }
                    TimeUtil.sleep((long) this.executeIntervalInt);
                    return;
                }
            }
            TimeUtil.sleep((long) this.executeIntervalInt);
        } catch (Throwable th) {
            TimeUtil.sleep((long) this.executeIntervalInt);
        }
    }

    private void goldTicket() {
        String str = "success";
        try {
            JSONObject jSONObject = new JSONObject(OtherTaskRpcCall.goldBillIndex());
            String str2 = "resultDesc";
            if (jSONObject.getBoolean(str)) {
                JSONArray jSONArray = jSONObject.getJSONObject("result").getJSONArray("cardModel");
                for (int i = 0; i < jSONArray.length(); i++) {
                    JSONObject jSONObject2 = jSONArray.getJSONObject(i);
                    String string = jSONObject2.getString("cardTypeId");
                    if (!"H5_GOLDBILL_ASSERT".equals(string)) {
                        if ("H5_GOLDBILL_TASK".equals(string)) {
                            JSONArray jSONArray2 = (JSONArray) JsonUtil.getValueByPathObject(jSONObject2, "dataModel.jsonResult.tasks.todo");
                            if (jSONArray2 != null) {
                                for (int i2 = 0; i2 < jSONArray2.length(); i2++) {
                                    JSONObject jSONObject3 = jSONArray2.getJSONObject(i2);
                                    String string2 = jSONObject3.getString("title");
                                    if (JsonUtil.getValueByPath(jSONObject3, "extInfo.morphoDetail.task_type").contains("TERM_LIFE_INSERANCE")) {
                                        String string3 = jSONObject3.getString("taskId");
                                        JSONObject jSONObject4 = new JSONObject(OtherTaskRpcCall.goldBillTrigger(string3));
                                        StringBuilder stringBuilder;
                                        if (jSONObject4.getBoolean(str)) {
                                            JSONObject jSONObject5 = new JSONObject(OtherTaskRpcCall.taskQueryPush(string3));
                                            if (jSONObject5.getBoolean(str)) {
                                                StringBuilder stringBuilder2 = new StringBuilder();
                                                stringBuilder2.append(this.displayName);
                                                stringBuilder2.append("[");
                                                stringBuilder2.append(string2);
                                                stringBuilder2.append("]");
                                                stringBuilder2.append(jSONObject3.getString("subTitle"));
                                                Log.other(stringBuilder2.toString());
                                                TimeUtil.sleep(500);
                                            } else {
                                                stringBuilder = new StringBuilder();
                                                stringBuilder.append(this.TAG);
                                                stringBuilder.append(".goldTicket.taskQueryPush");
                                               Log.system(stringBuilder.toString(), jSONObject5.optString(str2));
                                            }
                                        } else {
                                            stringBuilder = new StringBuilder();
                                            stringBuilder.append(this.TAG);
                                            stringBuilder.append(".goldTicket.goldBillTrigger");
                                           Log.system(stringBuilder.toString(), jSONObject4.optString(str2));
                                        }
                                    }
                                }
                            }
                        } else {
                            "H5_GOIDBILL_EQUITY".equals(string);
                        }
                    }
                }
                TimeUtil.sleep((long) this.executeIntervalInt);
                return;
            }
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append(this.TAG);
            stringBuilder3.append(".goldTicket.goldBillIndex");
           Log.system(stringBuilder3.toString(), jSONObject.optString(str2));
            TimeUtil.sleep((long) this.executeIntervalInt);
        } catch (Throwable th) {
            TimeUtil.sleep((long) this.executeIntervalInt);
        }
    }

    private void submit() {
        String str = "\"";
        String str2 = "\"writeOffNo\": \"";
        String str3 = "\"amount\": ";
        try {
            JSONObject requestString = requestString("com.alipay.wealthgoldtwa.goldbill.consume.query", "\"client_pkg_version\": \"0.0.8\"");
            if (requestString != null && requestString.optBoolean("success")) {
                requestString = requestString.getJSONObject("result");
                JSONObject jSONObject = requestString.getJSONObject("goldbillInfo");
                JSONArray jSONArray = requestString.getJSONArray("goldProducts");
                if (jSONObject.getInt("availableAmount") >= 100 && jSONArray.length() != 0) {
                    int i = jSONObject.getInt("exchangeAmount");
                    String string = jSONObject.getString("exchangeMoney");
                    String string2 = jSONArray.getJSONObject(0).getString("productId");
                    StringBuilder jSONObject2 = new StringBuilder(str3);
                    jSONObject2.append(i);
                    jSONObject2.append(",\"money\": \"");
                    jSONObject2.append(string);
                    jSONObject2.append("\",\"prizeName\": \"黄金\",\"prizeType\": \"GOLD\",\"productId\": \"");
                    jSONObject2.append(string2);
                    jSONObject2.append(str);
                    JSONObject requestString2 = requestString("com.alipay.wealthgoldtwa.goldbill.consume.submit", jSONObject2.toString());
                    if (requestString2 != null && requestString2.optBoolean("success")) {
                        str3 = JsonUtil.getValueByPath(requestString2, "result.writeOffNo");
                        if (!str3.isEmpty()) {
                            StringBuilder jSONObject3 = new StringBuilder(str2);
                            jSONObject3.append(str3);
                            jSONObject3.append(str);
                            if (requestString("com.alipay.wealthgoldtwa.goldbill.v4.consume.result", jSONObject3.toString()) != null) {
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append(this.displayName);
                                stringBuilder.append("提取成功[");
                                stringBuilder.append(string);
                                stringBuilder.append("元]");
                                Log.other(stringBuilder.toString());
                            }
                        }
                    }
                }
            }
        } catch (Throwable th) {
            Log.printStackTrace(this.TAG, th);
        }
    }

    private void triggerBigPrize() {
        try {
            JSONObject requestStringAllNew = requestStringAllNew("com.alipay.wealthgoldtwa.needle.task.triggerBigPrize", "[null]");
            if (requestStringAllNew != null && requestStringAllNew.optBoolean("success")) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(this.displayName);
                stringBuilder.append("领礼包[");
                stringBuilder.append(JsonUtil.getValueByPath(requestStringAllNew, "result.bigPrizeInfo.prizeName"));
                stringBuilder.append("]");
                Log.other(stringBuilder.toString());
            }
        } catch (Throwable th) {
            Log.printStackTrace(this.TAG, th);
        }
    }

    private void wealthgoldtwa() {
        String str = "\"";
        int i = 0;
        while (i < 3) {
            i++;
            try {
                JSONObject requestString = requestString("com.alipay.wealthgoldtwa.needle.v2.index", "\"bizScene\": \"gold\",\"chInfo\": \"gold\",\"forceNewVersion\": 0,\"taskId\": \"\"");
                if (requestString != null) {
                    Object valueByPathObject = JsonUtil.getValueByPathObject(requestString, "result.upsertData");
                    if (valueByPathObject != null) {
                        valueByPathObject = JsonUtil.getValueByPathObject((JSONObject) valueByPathObject, "task.tasks.todo");
                        if (valueByPathObject != null) {
                            JSONArray jSONArray = (JSONArray) valueByPathObject;
                            int length = jSONArray.length();
                            if (length > 0) {
                                for (int i2 = 0; i2 < length; i2++) {
                                    JSONObject jSONObject = jSONArray.getJSONObject(i2);
                                    String string = jSONObject.getString("taskId");
                                    String string2 = jSONObject.getString("title");
                                    String string3 = jSONObject.getString("amount");
                                    StringBuilder stringBuilder = new StringBuilder();
                                    stringBuilder.append("\"taskId\":\"");
                                    stringBuilder.append(string);
                                    stringBuilder.append(str);
                                    if (requestString("com.alipay.wealthgoldtwa.goldbill.v4.task.trigger", stringBuilder.toString()) != null) {
                                        stringBuilder = new StringBuilder();
                                        stringBuilder.append("\"mode\": 1,\"taskId\":\"");
                                        stringBuilder.append(string);
                                        stringBuilder.append(str);
                                        JSONObject jsonObject = requestString("com.alipay.wealthgoldtwa.needle.taskQueryPush", stringBuilder.toString());
                                        if (jsonObject != null && jsonObject.optBoolean("success")) {
                                            StringBuilder stringBuilder2 = new StringBuilder();
                                            stringBuilder2.append(this.displayName);
                                            stringBuilder2.append("完成任务[");
                                            stringBuilder2.append(string2);
                                            stringBuilder2.append("]+");
                                            stringBuilder2.append(string3);
                                            Log.other(stringBuilder2.toString());
                                            TimeUtil.sleep((long) this.executeIntervalInt);
                                            triggerBigPrize();
                                        }
                                    }
                                }
                                TimeUtil.sleep((long) this.executeIntervalInt);
                            } else {
                                return;
                            }
                        }
                        return;
                    }
                    return;
                }
            } catch (Throwable th) {
                Log.printStackTrace(this.TAG, th);
            }
        }
    }

    private void weekModeSignIn() {
        try {
            JSONObject requestString = requestString("com.alipay.finaggexpbff.flow.h5Query", "\"options\":{\"applicationCode\":\"EQUITY-FH-V2\",\"workflowCode\":\"weekModeSignIn\"}");
            if (requestString != null && requestString.optBoolean("SUCCESS")) {
                requestString = (JSONObject) JsonUtil.getValueByPathObject(requestString, "result.prizeInfo.basePrize");
                if (requestString != null) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(this.displayName);
                    stringBuilder.append("签到获得[");
                    stringBuilder.append(requestString.getString("price"));
                    stringBuilder.append(requestString.getString("unit"));
                    stringBuilder.append("]");
                    Log.other(stringBuilder.toString());
                    TimeUtil.sleep((long) this.executeIntervalInt);
                    return;
                }
            }
            TimeUtil.sleep((long) this.executeIntervalInt);
        } catch (Throwable th) {
            TimeUtil.sleep((long) this.executeIntervalInt);
        }
    }

    private void weeklyWelfare() {
        try {
            JSONObject requestString = requestString("com.alipay.finaggexpbff.needle.weeklyWelfare.index", "\"chInfo\": \"goldbill\",\"modeBitMask\": 513");
            if (requestString != null) {
                Object valueByPathObject = JsonUtil.getValueByPathObject(requestString, "result.upsertData.sign.timeline");
                if (valueByPathObject != null) {
                    JSONArray jSONArray = (JSONArray) valueByPathObject;
                    int i = 0;
                    while (i < jSONArray.length()) {
                        JSONObject jSONObject = jSONArray.getJSONObject(i);
                        if (!jSONObject.optBoolean("isToday")) {
                            i++;
                        } else if (!jSONObject.optBoolean("signed")) {
                            String string = jSONObject.getString("prizeNum");
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("\"basePrize\": ");
                            stringBuilder.append(jSONObject.getString("basePrizeNum"));
                            stringBuilder.append(",\"prizeNum\": \"");
                            stringBuilder.append(string);
                            stringBuilder.append("\",\"type\": \"SIGN\"");
                            if (requestString("com.alipay.finaggexpbff.needle.weeklyWelfare.trigger", stringBuilder.toString()) != null) {
                                StringBuilder stringBuilder2 = new StringBuilder();
                                stringBuilder2.append(this.displayName);
                                stringBuilder2.append("每周福利签到获得[");
                                stringBuilder2.append(string);
                                stringBuilder2.append("]");
                                Log.other(stringBuilder2.toString());
                                TimeUtil.sleep((long) this.executeIntervalInt);
                                return;
                            }
                        }
                    }
                    TimeUtil.sleep((long) this.executeIntervalInt);
                    return;
                }
            }
            TimeUtil.sleep((long) this.executeIntervalInt);
        } catch (Throwable th) {
            TimeUtil.sleep((long) this.executeIntervalInt);
        }
    }

    protected void handle() {
        Log.other(displayName + "开始执行");
        try {
                weekModeSignIn();
                //getRankTasks();
                weeklyWelfare();
                goldBillCollect("\"campId\":\"CP1417744\",\"directModeDisableCollect\":true,\"from\":\"antfarm\",");
                goldTicket();
                wealthgoldtwa();
                goldBillCollect("");
                submit();
        } catch (Throwable th){
            Log.printStackTrace(displayName, th);
        }finally {
            Status.setFlagToday("GoldTicket_TaskCompleted");;
            Log.other(displayName+"执行完毕");
        }
    }
}