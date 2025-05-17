package fansirsqi.xposed.sesame.task.otherTask;

import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import fansirsqi.xposed.sesame.data.Status;
import fansirsqi.xposed.sesame.hook.ApplicationHook;
import fansirsqi.xposed.sesame.hook.RequestManager;
import fansirsqi.xposed.sesame.util.JsonUtil;
import fansirsqi.xposed.sesame.util.Log;
import fansirsqi.xposed.sesame.util.RandomUtil;
import fansirsqi.xposed.sesame.data.Status;
import fansirsqi.xposed.sesame.util.StringUtil;
import fansirsqi.xposed.sesame.util.TimeUtil;
import fansirsqi.xposed.sesame.util.Maps.UserMap;

public class BaseTaskRpcCall {
    public static String taskQuery(String str) {
        return RequestManager.requestString("com.alipay.loanpromoweb.promo.task.taskQuery", "[{\"appletId\":\"" + str +"}]");
    }

    public static String taskTrigger(String str, String str2, String str3) {
        return RequestManager.requestString("com.alipay.loanpromoweb.promo.task.taskTrigger", "[{\"appletId\":\"" + str + "\",\"stageCode\":\"" + str2 + "\",\"taskCenId\":\"" + str3 + "\"}]");
    }

    public static String signInTrigger(String str) {
        return RequestManager.requestString("com.alipay.loanpromoweb.promo.signin.trigger", "[{\"extInfo\":{},\"sceneId\":\"" + str + "\"}]");
    }

    public static void doTask(String str, String str2, String str3) {
        String str4 = "TO_RECEIVE";
        String str5 = "success";
        try {
            JSONObject stringBuilder = new JSONObject(taskQuery(str));
            String str6 = "resultDesc";
            if (stringBuilder.getBoolean(str5)) {
                JSONArray jSONArray = stringBuilder.getJSONObject("result").getJSONArray("taskDetailList");
                for (int i = 0; i < jSONArray.length(); i++) {
                    JSONObject jSONObject = jSONArray.getJSONObject(i);
                    if (Arrays.asList(new String[]{"USER_TRIGGER"}).contains(jSONObject.getString("sendCampTriggerType"))) {
                        String string = jSONObject.getString("taskProcessStatus");
                        String string2 = jSONObject.getString("taskId");
                        String str7 = "NONE_SIGNUP";
                        JSONObject stringBuilder2;
                        if (str4.equals(string)) {
                            stringBuilder2 = new JSONObject(taskTrigger(string2, "receive", str));
                            if (!stringBuilder2.getBoolean(str5)) {
                                Log.error(str2 + ".doTask.receive", stringBuilder2.optString(str6));
                            }
                        } else if (str7.equals(string)) {
                            stringBuilder2 = new JSONObject(taskTrigger(string2, "signup", str));
                            if (!stringBuilder2.getBoolean(str5)) {
                                Log.error(str2 + ".doTask.signup", stringBuilder2.optString(str6));
                            }
                        }
                        if (!"SIGNUP_COMPLETE".equals(string)) {
                            if (!str7.equals(string)) {
                                if (!str4.equals(string)) {
                                }
                                Log.other(str3 + "[" + JsonUtil.getValueByPath(jSONObject, "taskExtProps.TASK_MORPHO_DETAIL.title") + "]任务完成");
                            }
                        }
                        JSONObject stringBuilder3 = new JSONObject(taskTrigger(string2, "send", str));
                        if (!stringBuilder3.getBoolean(str5)) {
                            Log.error(str2 + ".doTask.send", stringBuilder3.optString(str6));
                        }
                        Log.other(str3 + "[" + JsonUtil.getValueByPath(jSONObject, "taskExtProps.TASK_MORPHO_DETAIL.title") + "]任务完成");
                    }
                }
                return;
            }
            Log.error(str2 + ".doTask.taskQuery err " + stringBuilder.optString(str6));
        } catch (Throwable th) {
            Log.error(str2, "doTask err:");
            Log.printStackTrace(str2, th);
        }
    }

    public static void assistFriend(Set<String> set, CompletedKeyEnum completedKeyEnum, String str) {
        String str2;
        String str3;
        String str4;
        String str5;
        char c;
        String str6 = "ANTFARM_ORCHARD_SHARE_P2P";
        try {
            if (set.isEmpty()) {
                return;
            }
            String randomString = RandomUtil.getRandomString(5);
            if (CompletedKeyEnum.AntOrchardAssistFriend == completedKeyEnum) {
                str5 = "农场";
                str2 = randomString + "ANTFARM_ORCHARD_SHARE_P2P";
                str3 = "ANTFARM_ORCHARD";
                str4 = "0.1.2407011521.59";
            } else {
                if (CompletedKeyEnum.AntStallAssistFriend != completedKeyEnum) {
                    return;
                }
                str2 = "-" + randomString + "ANUTSALTML_2PA_SHARE";
                str3 = "ANTSTALL";
                str4 = "0.1.2406171355.5";
                str5 = "新村";
                str6 = "ANTSTALL_P2P_SHARER";
            }
            if (Status.hasFlagToday(completedKeyEnum.name())) {
                return;
            }
            String currentUid = UserMap.getCurrentUid();
            if (set.contains("1") && !"1".equals(currentUid)) {
                achieveBeShareP2P("1" + str2, str6, str3, str4);
            }
            for (String str7 : set) {
                if (!str7.equals(currentUid)) {
                    JSONObject jSONObject = new JSONObject(achieveBeShareP2P(str7 + str2, str6, str3, str4));
                    TimeUtil.sleep(5000L);
                    String maskName = UserMap.getMaskName(str7);
                    if (jSONObject.getBoolean("success")) {
                        Log.farm(str5 + "助力🎉成功[" + maskName + "]");
                    } else {
                        String string = jSONObject.getString("code");
                        switch (string.hashCode()) {
                            case 7766651:
                                if (string.equals("600000027")) {
                                    c = 1;
                                    break;
                                }
                                c = 65535;
                                break;
                            case 7766652:
                                if (string.equals("600000028")) {
                                    c = 0;
                                    break;
                                }
                                c = 65535;
                                break;
                            case 7766653:
                                if (string.equals("600000029")) {
                                    c = 2;
                                    break;
                                }
                                c = 65535;
                                break;
                            default:
                                c = 65535;
                                break;
                        }
                        if (c == 0) {
                            Log.record(str5 + "助力🐮被助力次数上限[" + maskName + "]");
                        } else if (c == 1) {
                            Log.record(str5 + "助力💪今日助力他人次数上限");
                            Status.setFlagToday(completedKeyEnum.name());
                            return;
                        } else if (c != 2) {
                            Log.record(str5 + "助力😔失败[" + maskName + "]" + jSONObject.optString("desc"));
                        } else {
                            Log.record(str5 + "助力💪已助力过[" + maskName + "]");
                        }
                    }
                }
            }
            if (completedKeyEnum == CompletedKeyEnum.AntStallAssistFriend) {
                Status.setFlagToday(completedKeyEnum.name());
            }
        } catch (Throwable th) {
            Log.system(str, "assistFriend err:");
            Log.printStackTrace(str, th);
        }
    }

    public static String achieveBeShareP2P(String str, String str2, String str3, String str4) {
        return RequestManager.requestString("com.alipay.antiep.achieveBeShareP2P", "[{\"requestType\":\"RPC\",\"sceneCode\":\"" + str2 + "\",\"shareId\":\"" + Base64.encodeToString(str.getBytes(), 2) + "\",\"source\":\"" + str3 + "\",\"systemType\":\"android\",\"version\":\"" + str4 + "\"}]");
    }

    public static JSONObject programInvoke(Map<String, Object> map) throws JSONException {
        JSONObject jSONObject = new JSONObject(RequestManager.requestString("alipay.imasp.program.programInvoke", StringUtil.getJsonString(map)));
        if (jSONObject.getBoolean("isSuccess")) {
            return jSONObject;
        }
        Log.error("BaseTaskRpcCall.programInvoke err " + map);
        return null;
    }
}
