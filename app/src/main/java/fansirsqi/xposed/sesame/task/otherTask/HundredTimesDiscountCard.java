package fansirsqi.xposed.sesame.task.otherTask;


import org.json.JSONArray;
import org.json.JSONObject;

import fansirsqi.xposed.sesame.util.Log;

public class HundredTimesDiscountCard extends BaseCommTask {
    public HundredTimesDiscountCard() {
        this.displayName = "百次立减卡🐟";
        this.hoursKeyEnum = CompletedKeyEnum.HundredTimesDiscountCard;
    }

    private void finishTask(JSONObject jSONObject) {
        String str = "\"taskId\": \"";
        String str2 = "\"appletId\": \"";
        try {
            String string = jSONObject.getString("taskCenId");
            String string2 = jSONObject.getString("taskId");
            String string3 = jSONObject.getString("taskStatus");
            jSONObject = jSONObject.getJSONObject("taskExtProps");
            String str3 = "\"";
            if ("NOT_DONE".equals(string3)) {
                StringBuilder stringBuilder = new StringBuilder(str2);
                stringBuilder.append(string2);
                stringBuilder.append("\",\"stageCode\": \"send\",\"taskCenId\": \"");
                stringBuilder.append(string);
                stringBuilder.append(str3);
                if (requestString("alipay.promoprod.applet.trigger", stringBuilder.toString()) == null) {
                    return;
                }
            } else if ("NONE_SIGNUP".equals(string3)) {
                return;
            }
            StringBuilder stringBuilder2 = new StringBuilder(str);
            stringBuilder2.append(string2);
            stringBuilder2.append(str3);
            if (requestString("alipay.ofpgrowth.hundredtimesdiscountcard.task.receive", stringBuilder2.toString()) != null) {
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append(this.displayName);
                stringBuilder3.append("完成任务[");
                stringBuilder3.append(jSONObject.optString("taskTitle"));
                stringBuilder3.append("]获得");
                stringBuilder3.append(jSONObject.optString("prizeCount"));
                stringBuilder3.append("次");
                Log.other(stringBuilder3.toString());
            }
        } catch (Exception e) {
            Log.printStackTrace(this.TAG, e);
        }
    }

    private void listQuery() {
        try {
            JSONObject requestString = requestString("alipay.ofpgrowth.hundredtimesdiscountcard.task.listquery", "\"extInfo\": {\"needFilterTaskTypeList\": [\"\",\"\"]}");
            if (requestString != null) {
                requestString = requestString.getJSONObject("data");
                signIn(requestString.optJSONObject("signInTaskInfo"));
                JSONArray optJSONArray = requestString.optJSONArray("taskList");
                if (optJSONArray != null) {
                    for (int i = 0; i < optJSONArray.length(); i++) {
                        finishTask(optJSONArray.getJSONObject(i));
                    }
                }
            }
        } catch (Throwable th) {
            Log.printStackTrace(this.TAG, th);
        }
    }

    private void signIn(JSONObject jSONObject) {
        String str = "\"";
        if (jSONObject != null) {
            try {
                String string = jSONObject.getString("taskCenterId");
                String str2 = "";
                JSONArray jSONArray = jSONObject.getJSONArray("taskDetailList");
                for (int i = 0; i < jSONArray.length(); i++) {
                    JSONObject jSONObject2 = jSONArray.getJSONObject(i);
                    if (jSONObject2.getBoolean("hasToday")) {
                        if (!"HAS_COMPLETED".equals(jSONObject2.getString("status"))) {
                            str2 = jSONObject2.getString("taskId");
                        } else {
                            return;
                        }
                    }
                }
                if (!str2.isEmpty()) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("\"appletId\": \"");
                    stringBuilder.append(str2);
                    stringBuilder.append("\",\"stageCode\": \"send\",\"taskCenId\": \"");
                    stringBuilder.append(string);
                    stringBuilder.append(str);
                    requestString("alipay.promoprod.applet.trigger", stringBuilder.toString());
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("\"chInfo\": \"signInTask\",\"taskId\": \"");
                    stringBuilder2.append(str2);
                    stringBuilder2.append(str);
                    jSONObject = requestString("alipay.ofpgrowth.hundredtimesdiscountcard.task.receive", stringBuilder2.toString());
                    if (jSONObject != null) {
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append(this.displayName);
                        stringBuilder3.append("签到成功+");
                        stringBuilder3.append(jSONObject.optString("modelAmount"));
                        stringBuilder3.append(jSONObject.optString("modelUnit"));
                        Log.other(stringBuilder3.toString());
                    }
                }
            } catch (Throwable th) {
                Log.printStackTrace(this.TAG, th);
            }
        }
    }

    protected void handle() {
        listQuery();
    }
}