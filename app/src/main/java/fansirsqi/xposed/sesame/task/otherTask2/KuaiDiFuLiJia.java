package fansirsqi.xposed.sesame.task.otherTask2;

import java.util.HashMap;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import fansirsqi.xposed.sesame.data.Status;
import fansirsqi.xposed.sesame.hook.ApplicationHook;
import fansirsqi.xposed.sesame.hook.RequestManager;
import fansirsqi.xposed.sesame.task.antMember.AntMember;
import fansirsqi.xposed.sesame.task.otherTask.CompletedKeyEnum;
import fansirsqi.xposed.sesame.util.JsonUtil;
import fansirsqi.xposed.sesame.util.Log;
import fansirsqi.xposed.sesame.util.RandomUtil;
import fansirsqi.xposed.sesame.util.TimeUtil;

public class KuaiDiFuLiJia extends AntMember {
    private static final String TAG = "快递积分任务🎁";
    private int executeIntervalInt = 2000;

    private void listQuery(String str) {
        String str2 = "listQuery err ";
        try {
            JSONObject stringBuilder = new JSONObject(ApplicationHook.requestString("alipay.promoprod.task.listQuery", "[{\"consultAccessFlag\":true,\"taskCenInfo\":\"" + str + "\"}]"));
            if (stringBuilder.getBoolean("success")) {
                JSONArray jSONArray = stringBuilder.getJSONArray("taskDetailList");
                for (int i = 0; i < jSONArray.length(); i++) {
                    stringBuilder = jSONArray.getJSONObject(i);
                    String optString = stringBuilder.optString("taskId");
                    String optString2 = stringBuilder.optString("taskProcessStatus");
                    stringBuilder.optString("sendCampTriggerType");
                    String valueByPath = JsonUtil.getValueByPath(stringBuilder, "taskMaterial.taskCenInfo");
                    if (!valueByPath.isEmpty()) {
                        str = valueByPath;
                    }
                    if (!"RECEIVE_SUCCESS".equals(optString2)) {
                        trigger(optString, "signup", str);
                        stringBuilder = trigger(optString, "send", str);
                        if (stringBuilder != null || stringBuilder.getBoolean("success")) {
                            Log.other(TAG+"完成[" + JsonUtil.getValueByPath(stringBuilder, "prizeSendInfo.[0].prizeName") + "🎉]");
                        }
                    }
                }
                TimeUtil.sleep(RandomUtil.nextInt(10000,15000));
                return;
            }
            Log.error(TAG, stringBuilder.optString("errorMessage"));
            TimeUtil.sleep((long) this.executeIntervalInt);
        } catch (Throwable th) {
            Log.error(TAG, str2+th);
            TimeUtil.sleep((long) this.executeIntervalInt);
        }
    }
    private void listQuery2(String str) {
        String str2 = "listQuery2 err ";
        try {
            JSONObject stringBuilder = new JSONObject(ApplicationHook.requestString("alipay.promoprod.task.listQuery",
                    "[{\"consultAccessFlag\":true,\"extInfo\":{\"componentCode\":\"musi_test\"},\"taskCenInfo\":\""+str+"\"}]"));
            if (stringBuilder.getBoolean("success")) {
                JSONArray jSONArray = stringBuilder.getJSONArray("taskDetailList");
                for (int i = 0; i < jSONArray.length(); i++) {
                    stringBuilder = jSONArray.getJSONObject(i);
                    String optString = stringBuilder.optString("taskId");
                    String optString2 = stringBuilder.optString("taskProcessStatus");
                    stringBuilder.optString("sendCampTriggerType");
                    String valueByPath = JsonUtil.getValueByPath(stringBuilder, "taskMaterial.taskCenInfo");
                    if (!valueByPath.isEmpty()) {
                        str = valueByPath;
                    }
                    if (!"RECEIVE_SUCCESS".equals(optString2)) {
                        trigger(optString, "signup", str);
                        stringBuilder = trigger(optString, "send", str);
                        if (stringBuilder != null || stringBuilder.getBoolean("success")) {
                            Log.other(TAG+"完成✅[" + JsonUtil.getValueByPath(stringBuilder, "prizeSendInfo.[0].prizeName") + "🎉]");
                        }
                    }
                }
                TimeUtil.sleep(RandomUtil.nextInt(10000,15000));
                return;
            }
            Log.error(TAG, stringBuilder.optString("errorMessage"));
            TimeUtil.sleep((long) this.executeIntervalInt);
        } catch (Throwable th) {
            Log.error(TAG, "listQuery2 err:"+th);
            TimeUtil.sleep((long) this.executeIntervalInt);
        }
    }

    private JSONObject trigger(String str, String str2, String str3) throws JSONException {
        JSONObject stringBuilder = new JSONObject(RequestManager.requestString("alipay.promoprod.applet.trigger",
                "[{\"appletId\":\""+str+"\"," +
                        "\"stageCode\":\""+str2+"\"," +
                        "\"taskCenInfo\":\""+str3+"\"}]"));
        if (stringBuilder.getBoolean("success")) {
            return stringBuilder;
        }
        Log.error(TAG, "❌快递积分["+str+"]任务 err :" + stringBuilder.getString("errorMessage"));
        return null;
    }

    public void handle(int i) {
        try {
            if (Status.hasFlagToday(CompletedKeyEnum.KuaiDiFuLiJia.name())) {
                TimeUtil.sleep((long) i);
                return;
            }
            this.executeIntervalInt = i;
            listQuery("MZVPQ0DScvD6NjaPJzk8iNRgSSvWpCuA");
            listQuery2("MZVPQ0DScvD6NjaPJzk8iCCWtq%2FRt4kh");
            Status.setFlagToday(CompletedKeyEnum.KuaiDiFuLiJia.name());
            TimeUtil.sleep((long) i);
        } catch (Throwable th) {
            TimeUtil.sleep((long) i);
        }
    }
}