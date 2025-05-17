package fansirsqi.xposed.sesame.task.otherTask;

import org.json.JSONArray;
import org.json.JSONObject;

import fansirsqi.xposed.sesame.data.Status;
import fansirsqi.xposed.sesame.util.JsonUtil;
import fansirsqi.xposed.sesame.util.Log;
import fansirsqi.xposed.sesame.util.TimeUtil;

public class DayDaySave extends BaseCommTask {

    @Override
    protected void handle() {
        if (!Status.hasFlagToday(CompletedKeyEnum.DayDaySave.name())) {
            index();
            collection();
            Status.setFlagToday(CompletedKeyEnum.DayDaySave.name());
        }
    }

    public DayDaySave() {
        this.displayName = "蛋定生财💸";
    }
    private void index() {
        String str = "\"";
        try {
            JSONObject requestString = requestString("com.alipay.ficcscenepromobff.needle.daydaysave.index", "\"bizScenario\": \"FL\"");
            if (requestString != null) {
                requestString = requestString.getJSONObject("result");
                if (!(requestString.optBoolean("hasSignIn") || requestStringAllNew("com.alipay.ficcscenepromobff.needle.daydaysave.signIn", "[null]") == null)) {
                    Log.other(this.displayName + "签到成功");
                }
                Object valueByPathObject = JsonUtil.getValueByPathObject(requestString, "energyBubbleList.taskEnergy");
                if (valueByPathObject != null) {
                    JSONArray jSONArray = (JSONArray) valueByPathObject;
                    String str2 = "TASK_SIGN";
                    String str3 = "STABLE_INTERACT_TASK_LIST";
                    for (int i = 0; i < jSONArray.length(); i++) {
                        JSONObject jSONObject = jSONArray.getJSONObject(i);
                        String string = jSONObject.getString("taskId");
                        if (!"to_receive".equals(jSONObject.getString("status"))) {
                            String str4 = "\"playEntrance\": \"" + str3 + "\",\"taskId\": \"" + string + "\",\"playActionCode\": \"";
                            requestString("com.alipay.ficcscenepromobff.promosdk2024.task.trigger", str4 + str2 + str);
                            str2 = "TASK_COMPLETE";
                            jSONObject = requestString("com.alipay.ficcscenepromobff.promosdk2024.task.complete", str4 + str2 + str);
                            if (jSONObject != null) {
                                Log.other(this.displayName + "完成任务[" + JsonUtil.getValueByPath(jSONObject, "modalConfig.modalTitle") + "]");
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

    private void collection() {
        try {
            JSONObject requestString = requestString("com.alipay.ficcscenepromobff.needle.daydaysave.index", "\"bizScenario\": \"FL\"");
            if (requestString != null) {
                requestString = requestString.getJSONObject("result");
                Object valueByPathObject = JsonUtil.getValueByPathObject(requestString, "energyBubbleList.normalEnergy");
                if (valueByPathObject != null) {
                    JSONArray jSONArray = new JSONArray();
                    JSONArray jSONArray2 = new JSONArray();
                    JSONArray jSONArray3 = new JSONArray();
                    JSONArray jSONArray4 = (JSONArray) valueByPathObject;
                    for (int i = 0; i < jSONArray4.length(); i++) {
                        JSONObject jSONObject = jSONArray4.getJSONObject(i);
                        jSONArray.put(jSONObject.getString("amount"));
                        jSONArray2.put(jSONObject.getString("id"));
                        jSONArray3.put(jSONObject.getString("name"));
                    }
                    if (jSONArray2.length() != 0) {
                        if (requestString("com.alipay.ficcscenepromobff.needle.daydaysave.collection", "\"amountList\":" + jSONArray + ",\"isAdvancedUser\": true,\"prePhases\":" + requestString.getJSONObject("progress") + ",\"voucherIdList\":" + jSONArray2) != null) {
                            Log.other(this.displayName + "领取奖励[" + jSONArray3 + "]+" + jSONArray);
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

    private void peakIndex() {
        try {
            JSONObject requestString = requestString("com.alipay.ficcscenepromobff.needle.daydaysavePeak.index", "\"bizScenario\": \"Dianfengsai_zhoucubanner\"");
            if (requestString != null) {
                Object valueByPathObject = JsonUtil.getValueByPathObject(requestString.getJSONObject("result"), "taskList");
                if (valueByPathObject != null) {
                    JSONArray jSONArray = (JSONArray) valueByPathObject;
                    String str = "TASK_COMPLETE";
                    String str2 = "STABLE_INTERACT_RANKING_TASK_LIST";
                    for (int i = 0; i < jSONArray.length(); i++) {
                        JSONObject jSONObject = jSONArray.getJSONObject(i);
                        String string = jSONObject.getString("taskId");
                        jSONObject.getString("status");
                        String string2 = jSONObject.getString("title");
                        if (requestString("com.alipay.ficcscenepromobff.promosdk2024.task.complete", ("\"playEntrance\": \"" + str2 + "\",\"taskId\": \"" + string + "\",\"playActionCode\": \"") + str + "\"") != null) {
                            Log.other(this.displayName + "完成巅峰赛任务[" + string2 + "]");
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



}