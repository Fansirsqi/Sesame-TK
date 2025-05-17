package fansirsqi.xposed.sesame.task.otherTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fansirsqi.xposed.sesame.data.Status;
import fansirsqi.xposed.sesame.hook.RequestManager;
import fansirsqi.xposed.sesame.util.JsonUtil;
import fansirsqi.xposed.sesame.util.Log;
import fansirsqi.xposed.sesame.util.TimeUtil;

public class FundApplication extends OtherTask {
    private static final String TAG = "摇红包💊";
    private int executeIntervalInt = 2000;

    public void handle(int i) throws JSONException {
        JSONObject jSONObject;
        JSONObject jSONObject2;
        try {
            this.executeIntervalInt = i;
            jSONObject = new JSONObject(recommend());
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        if (jSONObject.getBoolean("success") && (jSONObject2 = (JSONObject) JsonUtil.getValueByPathObject(jSONObject, "model.modules.[0].content")) != null) {
            String valueByPath = JsonUtil.getValueByPath(jSONObject2, "lightFireArea.lightTaskId");
            if (!valueByPath.isEmpty()) {
                signIn(valueByPath);
            }
            JSONObject jSONObject3 = jSONObject2.getJSONObject("mainArea");
            if (!Status.hasFlagToday(CompletedKeyEnum.GiftinocenterTask.name())) {
                if (giftinocenterTask(JsonUtil.getValueByPath(jSONObject2, "taskArea.taskCenterIdKey"))) {
                    Status.setFlagToday(CompletedKeyEnum.GiftinocenterTask.name());
                    TimeUtil.sleep(i);
                }
                giftinocenter(jSONObject3.optString("initCampId"));
            }
            //摇红包
            certificate(jSONObject3.getString("certificateTmplId"), jSONObject3.getString("mainActiveId"));
        }
    }

    private void signIn(String str) throws JSONException {
        if (!Status.hasFlagToday(CompletedKeyEnum.FundApplicationSignIn.name())) {

            JSONObject jSONObject = new JSONObject(OtherTaskRpcCall.appletTrigger(new HashMap<String, Object>() {
                final  String val$appletId;

                {
                    this.val$appletId = str;
                    put("appletId", str);
                    put("stageCode", "send");
                    put("source", "giftinocenter");
                }
            }));
            if (!jSONObject.getBoolean("success")) {
                Log.system(TAG, "signIn.appletTrigger" + jSONObject.optString("resultDesc"));
            } else {
                Log.other("摇红包💊签到成功");
                Status.setFlagToday(CompletedKeyEnum.FundApplicationSignIn.name());
            }
        }
    }

    private void giftinocenter(String str) throws JSONException {
        JSONObject json;
        json = new JSONObject(promokernelTrigger(str));

        if (json.optBoolean("success")) {
            JSONObject jSONObject2 = (JSONObject) JsonUtil.getValueByPathObject(json, "prizeSendInfo.[0]");
            if (jSONObject2 != null) {
                String valueByPath = JsonUtil.getValueByPath(jSONObject2, "prizeProperty.activityId");
                if (valueByPath.isEmpty()) {
                    Log.other("摇红包🌶获得[" + jSONObject2.optString("prizeName") + "]");
                } else {
                    JSONObject jSONObject3 = new JSONObject(giftMatch(valueByPath, "", "", "", ""));
                    if (jSONObject3.optBoolean("success")) {
                        JSONObject jSONObject4 = jSONObject3.getJSONObject("bcActivityVO");
                        if (!jSONObject4.getBoolean("activityEnd")) {
                            JSONObject jSONObject5 = jSONObject4.getJSONObject("taskVO");
                            if ("inComplete".equals(jSONObject5.getString("taskState"))) {
                                JSONObject jSONObject6 = jSONObject5.getJSONObject("taskParams");
                                String string = jSONObject6.getString("taskToken");
                                if (!string.isEmpty()) {
                                    String string2 = jSONObject6.getString("url");
                                    Matcher matcher = Pattern.compile("appId=([^&]+)").matcher(string2);
                                    String group = matcher.find() ? matcher.group(1) : "2018081461095002";
                                    JSONObject jSONObject7 = new JSONObject(giftMatch(valueByPath, group, string2, string, "MERCHANT_MINI_APP"));
                                    if (jSONObject7.optBoolean("success")) {
                                        JSONObject jSONObject8 = (JSONObject) JsonUtil.getValueByPathObject(jSONObject7, "bcActivityVO.taskVO");
                                        if (jSONObject8 != null && "complete".equals(jSONObject8.getString("taskState"))) {
                                            JSONObject jSONObject9 = new JSONObject(giftComplete(valueByPath, group, string2, JsonUtil.getValueByPath(jSONObject8, "taskParams.taskToken")));
                                            if (jSONObject9.getBoolean("success")) {
                                                Log.other("摇红包🎁获得[" + JsonUtil.getValueByPath(jSONObject9, "assetVOs.[0].showAmount") + "]元");
                                                return;
                                            }
                                            Log.system(TAG, "giftInoCenter.giftMatch1" + jSONObject9.optString("resultDesc"));
                                        }
                                        Log.other("任务非complete");
                                    } else {
                                        Log.system(TAG, "giftInoCenter.giftMatch1" + jSONObject7.optString("resultDesc"));
                                    }
                                }
                            }
                        }
                    } else {
                        Log.system(TAG, "进行摇红包出错jSONObject3:" + jSONObject3);
                    }
                }
            }
        } else {
            Log.system(TAG, "进行摇红包出错json:" + json);
        }
    }

    private boolean giftinocenterTask(String str) {
        try {
            JSONObject jSONObject = new JSONObject(OtherTaskRpcCall.taskListQuery(str));
            if (!jSONObject.has("success")) {
                Log.system(TAG, "giftinocenterTask.taskListQuery" + jSONObject.optString("resultDesc"));
                return false;
            }
            if (!jSONObject.has("taskDetailList")) {
                Log.system(TAG, "giftinocenterTask.taskListQuery: Missing 'taskDetailList'");
                return false;
            }
            JSONArray jSONArray = jSONObject.getJSONArray("taskDetailList");
            for (int i = 0; i < jSONArray.length(); i++) {
                JSONObject jSONObject2 = jSONArray.getJSONObject(i);
                if (!jSONObject2.has("taskProcessStatus")) {
                    Log.system(TAG, "Missing 'taskProcessStatus' in taskDetailList[" + i + "]");
                    continue;
                }
                String string = jSONObject2.getString("taskProcessStatus");
                String string2 = jSONObject2.getString("sendCampTriggerType");
                if (!"RECEIVE_SUCCESS".equals(string) && !"EVENT_TRIGGER".equals(string2)) {
                    //领取任务
                    String taskId =jSONObject2.getString("taskId");
                    appletTask(taskId ,str);

                    //执行任务
                    JSONObject jSONObject3 = new JSONObject(OtherTaskRpcCall.taskTrigger(new HashMap<String, Object>() {
                        final  String val$appletId;
                        final  String val$taskCenInfo;

                        {
                            this.val$appletId = jSONObject2.getString("taskId");
                            this.val$taskCenInfo = str;
                            put("appletId", jSONObject2.getString("taskId"));
                            put("stageCode", "send");
                            put("source", "giftinocenter");
                            put("taskCenId", str);
                            put("chinfo", "bcyx_dytx");
                            put("outBizNo", jSONObject2.getString("taskId") + System.currentTimeMillis());
                        }
                    }));
                    if (jSONObject3.optBoolean("success")) {
                        Log.other("摇红包💊完成任务[" + JsonUtil.getValueByPath(jSONObject2, "taskMaterial.taskMainTitle") + "]");
                    } else {
                        Log.error(TAG, "摇红包任务执行错误:" + jSONObject3.optString("resultDesc"));
                        break;
                    }
                }
            }
            TimeUtil.sleep(this.executeIntervalInt);
            return true;
        } catch (Throwable th) {
            try {
                String str2 = TAG;
                Log.error(str2, "摇红包任务错误:");
                Log.printStackTrace(str2, th);
                return false;
            } finally {
                TimeUtil.sleep(this.executeIntervalInt);
            }
        }
    }

    //领取任务
    private void appletTask(String taskId, String taskCenInfo) {
        String method = "alipay.promoprod.applet.trigger";
        String s = RequestManager.requestString(method,
                "[{\"appletId\":\""+taskId+"\",\"chinfo\":\"bc_sydoudi\",\"outBizNo\":\""+taskId+ System.currentTimeMillis()+"\"," +
                        "\"source\":\"giftinocenter\",\"stageCode\":\"send\",\"taskCenInfo\":\""+taskCenInfo+"\"}]");
    }
    // FundApplication.java
    private void certificate(String str, String str2) {
        try {
            String response = RequestManager.requestString("alipay.giftinocenter.camp.campActivity.certificateNum", "[{\"certTemplateId\":\"" + str + "\"}]");
            JSONObject jSONObject = new JSONObject(response);
            if (!jSONObject.optBoolean("success", false)) {
                Log.system(TAG, "查询摇红包次数失败: " + jSONObject.optString("resultDesc", "Unknown error"));
                return;
            }
            if (1009 == jSONObject.optInt("error")){
                return;
            }
            int availableNum = jSONObject.optInt("availableNum", 0);
            for (int i = 0; i < availableNum; i++) {
                giftinocenter(str2);
                TimeUtil.sleep(this.executeIntervalInt);
            }
        } catch (JSONException e) {
            Log.error(TAG, "查询摇红包次数JSON错误: " + e.getMessage());
        } catch (Exception e) {
            Log.error(TAG, "查询摇红包次数错误: " + e.getMessage());
        }
    }



    private String recommend() {
        return RequestManager.requestString("alipay.fundapplication.op.module.recommend", "[{\"bizCode\":\"RED_ENVELOPE\",\"factors\":{\"chInfo\":\"bcyx_dytx\"},\"moduleCodes\":[\"INTERACT_PROMO\"],\"system\":\"fundapplication\"}]");
    }

    private String promokernelTrigger(String str) {
        return RequestManager.requestString("alipay.promoprod.camp.promokernel.trigger", "[{\"campInfo\":\"" + str + "\"}]");
    }

    private String giftMatch(String str, String str2, String str3, String str4, String str5) {
        return RequestManager.requestString("alipay.giftinocenter.gift.activity.match", "[{\"activityId\":\"" + str + "\",\"extInfoMap\":{\"checkMode\":\"N\",\"groupInstanceId\":\"\",\"merchantAppId\":\"" + str2 + "\",\"merchantPageUrl\":\"" + str3 + "\",\"taskToken\":\"" + str4 + "\"},\"solutionCode\":\"" + str5 + "\",\"specialCode\":\"\"}]");
    }

    private String giftComplete(String str, String str2, String str3, String str4) {
        return RequestManager.requestString("alipay.giftinocenter.gift.activity.complete", "[{\"activityId\":\"" + str + "\",\"extInfoMap\":{\"checkMode\":\"N\",\"groupInstanceId\":\"\",\"merchantAppId\":\"" + str2 + "\",\"merchantPageUrl\":\"" + str3 + "\",\"taobaoLiveUserId\":\"\",\"taskToken\":\"" + str4 + "\"}        }]");
    }
}
