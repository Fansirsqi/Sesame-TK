package fansirsqi.xposed.sesame.task.otherTask;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import fansirsqi.xposed.sesame.data.Status;
import fansirsqi.xposed.sesame.hook.ApplicationHook;
import fansirsqi.xposed.sesame.util.JsonUtil;
import fansirsqi.xposed.sesame.util.Log;
import fansirsqi.xposed.sesame.util.Maps.UserMap;
import fansirsqi.xposed.sesame.util.TimeUtil;

public class YebExpGold extends BaseCommTask{
    private static final String TAG = "余额宝体验金🌭";
    private int executeIntervalInt;

    private void active(String str, boolean z) throws JSONException {
        StringBuilder stringBuilder;
        StringBuilder stringBuilder2 = new StringBuilder("\"couponId\":\"");
        stringBuilder2.append(str);
        stringBuilder2.append("\",\"type\":\"YEB_TRIAL\"");
        str = stringBuilder2.toString();
        if (z) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(str);
            stringBuilder.append(",\"equityType\":\"voucher\"");
            str = stringBuilder.toString();
        }
        JSONObject requestString = requestString("alipay.yebprod.promo.yebTrial.active", str);
        if (requestString != null) {
            stringBuilder = new StringBuilder("余额宝体验金🌭成功使用[");
            stringBuilder.append(JsonUtil.getValueByPath(requestString, "amount.amount"));
            stringBuilder.append("元]开始计算收益[");
            stringBuilder.append(requestString.getString("confirmDate"));
            stringBuilder.append("]第一笔收益到账[");
            stringBuilder.append(requestString.getString("profitDate"));
            stringBuilder.append("]");
            Log.other(stringBuilder.toString());
        }
    }

    private void doTask(String str, String str2, String str3) {
        String str4 = "\",\"taskId\":\"";
        String str5 = "AP16200213";
        String str6 = "余额宝体验金🍿完成任务[";
        String str7 = "\"appletId\":\"";
        String str8 = "\"params\":{\"appletId\":\"";
        try {
            String str9 = "complete";
            if (str5.equals(str2)) {
                str9 = "trigger";
            }
            StringBuilder stringBuilder = new StringBuilder(str8);
            stringBuilder.append(str);
            stringBuilder.append(str4);
            stringBuilder.append(str2);
            stringBuilder.append("\",\"version\":2},\"path\":\"task.");
            stringBuilder.append(str9);
            stringBuilder.append("\"");
            JSONObject requestString = requestString("com.alipay.yebscenebff.promosdk.index.forward", stringBuilder.toString());
            if (requestString == null) {
                TimeUtil.sleep((long) this.executeIntervalInt);
                return;
            }
            if (str5.equals(str2)) {
                StringBuilder stringBuilder2 = new StringBuilder(str7);
                stringBuilder2.append(str);
                stringBuilder2.append(str4);
                stringBuilder2.append(str2);
                stringBuilder2.append("\",\"activityScene\":\"yuebaotiyanjin\"");
                requestString("com.alipay.fincommonbff.needle.activityTaskReport", stringBuilder2.toString());
            }
            JSONObject jSONObject = (JSONObject) JsonUtil.getValueByPathObject(requestString, "result.prizeSendOrderList.[0].prizeCustomDisplayInfoDTO.extInfo");
            StringBuilder stringBuilder3 = new StringBuilder(str6);
            stringBuilder3.append(str3);
            stringBuilder3.append("]");
            if (jSONObject == null) {
                str = "";
            } else {
                StringBuilder stringBuilder4 = new StringBuilder();
                stringBuilder4.append(jSONObject.getString("PRIZE_AMOUNT"));
                stringBuilder4.append(jSONObject.getString("PRIZE_UNIT"));
                str = stringBuilder4.toString();
            }
            stringBuilder3.append(str);
            Log.other(stringBuilder3.toString());
            TimeUtil.sleep((long) this.executeIntervalInt);
        } catch (Throwable th) {
            TimeUtil.sleep((long) this.executeIntervalInt);
        }
    }

    private void exchange(double d) {
        try {
            StringBuilder stringBuilder = new StringBuilder("\"bizOrderNo\":\"");
            stringBuilder.append(UserMap.getCurrentUid());
            stringBuilder.append(System.currentTimeMillis());
            stringBuilder.append("\",\"campId\":\"CP111609176\",\"exchangeAmount\":\"");
            stringBuilder.append(d);
            stringBuilder.append("\",\"prizeId\":\"PZ129283652\"");
            JSONObject requestString = requestString("com.alipay.yebscenebff.expgold.index.exchange", stringBuilder.toString());
            if (requestString == null) {
                TimeUtil.sleep((long) this.executeIntervalInt);
                return;
            }
            active(JsonUtil.getValueByPath(requestString, "result.equityNo"), true);
            TimeUtil.sleep((long) this.executeIntervalInt);
        } catch (Throwable th) {
            TimeUtil.sleep((long) this.executeIntervalInt);
        }
    }

    @Override
    protected void handle() throws JSONException {

    }

    public JSONObject requestString(String str, String str2) throws JSONException {
        StringBuilder stringBuilder = new StringBuilder("[{");
        stringBuilder.append(str2);
        stringBuilder.append("}]");
        JSONObject stringBuilder2 = new JSONObject(ApplicationHook.requestString(str, stringBuilder.toString()));
        if (stringBuilder2.getBoolean("success")) {
            return stringBuilder2;
        }
        str = TAG;
        stringBuilder = new StringBuilder("requestString err ");
        stringBuilder.append(str2);
        Log.system(str, stringBuilder.toString());
        return null;
    }

    private void signIn(String str, JSONArray jSONArray) throws JSONException {
        if (jSONArray != null) {
            boolean z = false;
            int i = 0;
            while (i < jSONArray.length()) {
                try {
                    JSONObject jSONObject = jSONArray.getJSONObject(i);
                    if ("今天".equals(jSONObject.optString("displayDate"))) {
                        z = "SIGNED".equals(JsonUtil.getValueByPath(jSONObject, "signInfo.signStatus"));
                        break;
                    }
                    i++;
                } catch (Throwable th) {
                    TimeUtil.sleep((long) this.executeIntervalInt);
                }
            }
            if (!z) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("\"signInPlayId\":\"");
                stringBuilder.append(str);
                stringBuilder.append("\"");
                JSONObject requestString = requestString("com.alipay.yebscenebff.needle.yebExpGold.signIn", stringBuilder.toString());
                if (requestString != null) {
                    requestString = (JSONObject) JsonUtil.getValueByPathObject(requestString, "resultData.resultData.prizeOrderDTOList.[0].customMemo");
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("余额宝体验金🍟签到成功[");
                    if (requestString == null) {
                        str = "";
                    } else {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append(requestString.getString("PRIZE_AMOUNT"));
                        stringBuilder.append(requestString.getString("PRIZE_UNIT"));
                        str = stringBuilder.toString();
                    }
                    stringBuilder2.append(str);
                    stringBuilder2.append("]");
                    Log.system(stringBuilder2.toString());
                    TimeUtil.sleep((long) this.executeIntervalInt);
                    return;
                }
            }
        }
        TimeUtil.sleep((long) this.executeIntervalInt);
    }

    private void taskQuery() {
        try {
            StringBuilder stringBuilder = new StringBuilder("\"chInfo\":\"ch_url-https://2021001192699414.hybrid.alipay-eco.com/index.html\",\"signIn\":{\"daysOfQuerySignInData\":21,\"displaySignInTextList\":[{\"value\":\"持\"},{\"value\":\"续\"},{\"value\":\"签\"},{\"value\":\"到\"},{\"value\":\"可\"},{\"value\":\"领\"},{\"value\":\"\"}],\"downgrade\":false,\"todayRedDotText\":\"戳这里\",\"tomorrowRedDotText\":\"\"},\"task\":{\"downgrade\":false,\"queryComplete\":false,\"startTime\":");
            stringBuilder.append(System.currentTimeMillis());
            stringBuilder.append(",\"strategyCode\":\"YEB_TRIAL_ASSET_TASK_BLOCK_REC\"}");
            JSONObject requestString = requestString("com.alipay.yebscenebff.needle.yebExpGold.queryMain", stringBuilder.toString());
            if (requestString != null) {
                requestString = requestString.getJSONObject("resultData");
                double d = requestString.getDouble("balance");
                if (d > 300.0d) {
                    exchange(d);
                }
                signIn("PLAY102253251", (JSONArray) JsonUtil.getValueByPathObject(requestString, "signInData.list"));
                JSONArray jSONArray = (JSONArray) JsonUtil.getValueByPathObject(requestString, "taskData.taskListRes.resultData.taskList");
                if (jSONArray != null) {
                    for (int i = 0; i < jSONArray.length(); i++) {
                        JSONObject jSONObject = jSONArray.getJSONObject(i);
                        doTask(jSONObject.getString("appletId"), jSONObject.getString("taskId"), jSONObject.getString("title"));
                    }
                    Status.setFlagToday(CompletedKeyEnum.YebExpGold.name());
                    TimeUtil.sleep((long) this.executeIntervalInt);
                    return;
                }
            }
            TimeUtil.sleep((long) this.executeIntervalInt);
        } catch (Throwable th) {
            TimeUtil.sleep((long) this.executeIntervalInt);
        }
    }

    private void yebTrialAsset() {
        String str = "yebTrialAsset err ";
        String str2;
        try {
            JSONObject stringBuilder = new JSONObject(ApplicationHook.requestString("alipay.yebprod.promo.yebTrialAsset", "[null]"));
            if (stringBuilder.getBoolean("success")) {
                JSONArray jSONArray = stringBuilder.getJSONArray("trialInfoList");
                for (int i = 0; i < jSONArray.length(); i++) {
                    JSONObject jSONObject = jSONArray.getJSONObject(i);
                    if (!"A".equals(jSONObject.getString("status"))) {
                        active(jSONObject.getString("trialId"), false);
                        TimeUtil.sleep((long) this.executeIntervalInt);
                    }
                }
                TimeUtil.sleep((long) this.executeIntervalInt);
                return;
            }
            str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder(str);
            stringBuilder2.append(stringBuilder.optBoolean("resultDesc"));
            Log.system(str2, stringBuilder2.toString());
            TimeUtil.sleep((long) this.executeIntervalInt);
        } catch (Throwable th) {
            TimeUtil.sleep((long) this.executeIntervalInt);
        }
    }

    public void handle(int i) {
        this.executeIntervalInt = i;
        if (!Status.hasFlagToday(CompletedKeyEnum.YebExpGold.name())) {
            taskQuery();
            yebTrialAsset();
        }
    }
}
