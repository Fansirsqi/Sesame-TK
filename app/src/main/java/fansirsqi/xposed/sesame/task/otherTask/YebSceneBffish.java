package fansirsqi.xposed.sesame.task.otherTask;


import org.json.JSONArray;
import org.json.JSONObject;

import fansirsqi.xposed.sesame.util.JsonUtil;
import fansirsqi.xposed.sesame.util.Log;
import fansirsqi.xposed.sesame.util.TimeUtil;

public class YebSceneBffish extends BaseCommTask {

    @Override
    protected void handle() {
        sign();
        incomePlusFeedTaskList();
        index();
        queryPrizeRedemptionInfo();
    }

    public YebSceneBffish() {
        this.displayName = "余额宝养鱼🐟";
        this.hoursKeyEnum = CompletedKeyEnum.YebSceneBff;
    }
    private void incomePlusFeedTaskList() {
        try {
            JSONObject requestString = requestString("com.alipay.yebscenebff.needle.incomePlusFeedTaskList", "");
            if (requestString != null) {
                Object valueByPathObject = JsonUtil.getValueByPathObject(requestString, "result.taskListInfo.uncompletedList");
                if (valueByPathObject != null) {
                    JSONArray jSONArray = (JSONArray) valueByPathObject;
                    String str = "\"appName\": \"yebscenebff\",\"withJson\": false,";
                    for (int i = 0; i < jSONArray.length(); i++) {
                        JSONObject jSONObject = jSONArray.getJSONObject(i);
                        String string = jSONObject.getString("taskId");
                        String string2 = jSONObject.getString("appletId");
                        String str2 = "\",\"taskId\": \"";
                        String str3 = "\",\"version\": 2}";
                        String str4 = "com.alipay.yebscenebff.promosdk.index.forward";
                        if ("NONE_SIGNUP".equals(jSONObject.getString("taskProcessStatus"))) {
                            requestString(str4, str + "\"path\": \"task.trigger\",\"extParams\": {\"appletId\": \"" + string2 + str2 + string + str3);
                        } else {
                            requestString(str4, str + "\"path\": \"task.complete\",\"extParams\": {\"appletId\": \"" + string2 + "\",\"expectSendStatus\": \"RECOMMEND\",\"taskId\": \"" + string + str3);
                        }
                        if (requestString(str4, str + "\"path\": \"task.receive\",\"extParams\": {\"appletId\": \"" + string2 + str2 + string + str3) != null) {
                            Object valueByPathObject2 = JsonUtil.getValueByPathObject(jSONObject, "taskExtProps.TASK_MORPHO_DETAIL");
                            if (valueByPathObject2 != null) {
                                jSONObject = (JSONObject) valueByPathObject2;
                                Log.other(this.displayName + "完成任务[" + jSONObject.optString("title") + "]" + jSONObject.optString("subTitle"));
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

    private void queryPrizeRedemptionInfo() {
        try {
            JSONObject requestStringAllNew = requestStringAllNew("com.alipay.yebscenebff.needle.incomePlus.queryPrizeRedemptionInfo", "[null]");
            if (requestStringAllNew.getBoolean("success")) {
                requestStringAllNew = requestStringAllNew.getJSONObject("result");
                int parseInt = Integer.parseInt(requestStringAllNew.getString("currentGoldAmount"));
                JSONArray jSONArray = requestStringAllNew.getJSONArray("prizeList");
                for (int i = 0; i < jSONArray.length(); i++) {
                    JSONObject jSONObject = jSONArray.getJSONObject(i);
                    if ("VALID".equals(jSONObject.getString("status"))) {
                        int parseInt2 = Integer.parseInt(jSONObject.getString("amount"));
                        if (parseInt >= parseInt2) {
                            String string = jSONObject.getString("title");
                            String string2 = jSONObject.getString("prizeId");
                            String string3 = jSONObject.getString("campId");
                            int i2 = jSONObject.getInt("prizeValue");
                            if (requestString("com.alipay.yebscenebff.needle.incomePlus.redeemPrize", "\"amount\": " + parseInt2 + ",\"campId\": \"" + string3 + "\",\"prizeId\": \"" + string2 + "\"") != null) {
                                Log.other(this.displayName + "金币兑换[" + i2 + string + "]");
                            }
                        }
                    }
                }
                TimeUtil.sleep((long) this.executeIntervalInt);
                return;
            }
            TimeUtil.sleep((long) this.executeIntervalInt);
        } catch (Throwable th) {
            TimeUtil.sleep((long) this.executeIntervalInt);
        }
    }

    private void sign() {
        try {
            String str = "INCOME_PLUS_SIGN_IN_AWARD";
            JSONObject requestString = requestString("com.alipay.yebscenebff.needle.registration.query", "\"playActionCode\":\"SIGN_IN_CALENDAR_RECALL\",\"playEntrance\":\"INCOME_PLUS_SIGN_IN_AWARD\"");
            if (requestString != null) {
                Object valueByPathObject = JsonUtil.getValueByPathObject(requestString, "result.prizeDetailList");
                if (valueByPathObject != null) {
                    JSONArray jSONArray = (JSONArray) valueByPathObject;
                    for (int i = 0; i < jSONArray.length(); i++) {
                        JSONObject jSONObject = jSONArray.getJSONObject(i);
                        String string = jSONObject.getString("signStatus");
                        if (!"NOT_STARTED".equals(string)) {
                            if (!"SIGNED_IN".equals(string)) {
                                string = jSONObject.getString("prizeDayText");
                                String string2 = jSONObject.getString("prizeName");
                                String string3 = jSONObject.getString("prizeAmountText");
                                if (requestString("com.alipay.yebscenebff.needle.registration.trigger", "\"playActionCode\": \"SIGNIN_TRIGGER\",\"playEntrance\": \"" + str + "\",\"prizeId\": \"" + jSONObject.getString("prizeId") + "\"") != null) {
                                    Log.other(this.displayName + "签到成功[" + string + "]获得[" + string2 + string3 + "]");
                                }
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

    private void index() {
        String str = "\"amount\": \"";
        try {
            JSONObject requestString = requestString("com.alipay.yebscenebff.needle.incomePlus.index", "\"bizScenario\":\"YEB_HOME\",\"newScene\":\"otherTask\",\"version\":\"V1\"");
            if (requestString != null) {
                requestString = requestString.getJSONObject("result");
                String string = requestString.getString("contractId");
                receiveGold(requestString.getJSONArray("coinOrderList"), string);
                String valueByPath = JsonUtil.getValueByPath(requestString, "foodAmount.amount");
                if (!valueByPath.isEmpty()) {
                    if (!"0".equals(valueByPath)) {
                        if (requestString("com.alipay.yebscenebff.needle.incomePlus.feedingFish", valueByPath + "\",\"contractId\": \"" + string + "\"") != null) {
                            Log.other(this.displayName + "喂鱼成功[" + valueByPath + "]");
                            TimeUtil.sleep((long) this.executeIntervalInt);
                            return;
                        }
                    }
                }
            }
            TimeUtil.sleep((long) this.executeIntervalInt);
        } catch (Throwable th) {
            TimeUtil.sleep((long) this.executeIntervalInt);
        }
    }

    private void receiveGold(JSONArray jSONArray, String str) {
        int i = 0;
        while (i < jSONArray.length()) {
            try {
                JSONObject jSONObject = jSONArray.getJSONObject(i);
                String string = jSONObject.getString("orderStatus");
                String optString = jSONObject.optString("whenReceiveCoinDate");
                if ("I".equals(string) && !optString.isEmpty()) {
                    if (TimeUtil.isAfter(optString)) {
                        string = jSONObject.getString("amount");
                        if (requestString("com.alipay.yebscenebff.needle.incomePlus.receiveGold", "\"contractId\": \"" + str + "\",\"orderId\": \"" + jSONObject.getString("orderId") + "\"") != null) {
                            Log.other(this.displayName + "领取[" + string + "个金泡泡]");
                        }
                    }
                }
                i++;
            } catch (Throwable th) {
                TimeUtil.sleep((long) this.executeIntervalInt);
            }
        }
        TimeUtil.sleep((long) this.executeIntervalInt);
    }


}