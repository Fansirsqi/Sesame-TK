package fansirsqi.xposed.sesame.task.otherTask;

import android.os.Build;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import fansirsqi.xposed.sesame.hook.RequestManager;
import fansirsqi.xposed.sesame.util.Maps.UserMap;
import fansirsqi.xposed.sesame.util.RandomUtil;
import fansirsqi.xposed.sesame.util.StringUtil;

public class OtherTaskRpcCall extends BaseTaskRpcCall {
    private static final String clientVersion = "6.5.0";

    public static String goldBillIndex() {
        return RequestManager.requestString("com.alipay.wealthgoldtwa.needle.goldbill.index", "[{\"pageTemplateCode\":\"H5_GOLDBILL\",\"params\":{\"client_pkg_version\":\"0.0.5\"},\"url\":\"https://68687437.h5app.alipay.com/www/index.html\"}]");
    }

    public static String goldBillCollect(String str) {
        return RequestManager.requestString("com.alipay.wealthgoldtwa.goldbill.v2.index.collect", "[{" + str + "\"trigger\":\"Y\"}]");
    }

    public static String goldBillTrigger(String str) {
        return RequestManager.requestString("com.alipay.wealthgoldtwa.goldbill.v4.task.trigger", "[{\"goldBillTaskTransferVersion\":\"v2\",\"taskId\":\"" + str + "\"}]");
    }

    public static String taskQueryPush(String str) {
        return RequestManager.requestString("com.alipay.wealthgoldtwa.needle.taskQueryPush", "[{\"mode\":1,\"taskId\":\"" + str + "\"}]");
    }

    public static String v1benefitQuery() {
        return RequestManager.requestString("com.alipay.pcreditbfweb.drpc.cargodcard.v1benefitQuery", "[{\"args\":{\"productCode\":\"CAR_MASTER_CARD\"}}]");
    }

    public static String v1benefitTrigger(JSONObject jSONObject) {
        return RequestManager.requestString("com.alipay.pcreditbfweb.drpc.cargodcard.v1benefitTrigger", "[" + jSONObject + "]");
    }

    public static String queryTaskList() {
        return RequestManager.requestString("alipay.promoprod.task.query.queryTaskList", "[      {\n            \"consultAccessFlag\": true,\n            \"planId\": \"AP17187348\"\n        }]");
    }

    public static String signup(String str, String str2) {
        String str3 = "\"taskCenId\":\"AP17187348\",\"taskId\":\"" + str2 + "\"";
        if (!str.isEmpty()) {
            str3 = str3 + ",\"extInfo\":{\"gplusItem\":\"" + str + "\"}";
        }
        return RequestManager.requestString("alipay.promoprod.task.query.signup", "[{" + str3 + "}]");
    }

    public static String complete(String str) {
        return RequestManager.requestString("alipay.promoprod.applet.complete", "[{\"appletId\":\"" + str + "\"}]");
    }

    public static String taskListQuery(String str) throws JSONException {
        JSONObject jSONObject = new JSONObject();
        jSONObject.put("ALIPAY_APP_VERSION", "10.5.70.8000");
        jSONObject.put("MOBILE_OS", "Android");
        jSONObject.put("MOBILE_OS_VERSION", Build.VERSION.RELEASE);
        return RequestManager.requestString("alipay.promoprod.task.listQuery", StringUtil.getJsonString(new HashMap<String, Object>() {
            final  JSONObject val$jsonObject;
            final  String val$taskCenInfo;

            {
                this.val$taskCenInfo = str;
                this.val$jsonObject = jSONObject;
                put("taskCenInfo", str);
                put("consultAccessFlag", true);
                put("extInfo", jSONObject);
            }
        }));
    }

    public static String taskTrigger(Map<String, Object> map) {
        return RequestManager.requestString("com.alipay.loanpromoweb.promo.task.taskTrigger", StringUtil.getJsonString(map));
    }

    public static String appletTrigger(Map<String, Object> map) {
        return RequestManager.requestString("alipay.promoprod.applet.trigger", StringUtil.getJsonString(map));
    }

    public static String consumeGoldIndex() throws JSONException {
        return RequestManager.requestString("alipay.mobile.ipsponsorprod.consume.gold.taskV2.index", StringUtil.getJsonString(new HashMap<String, Object>() { // from class: fansirsqi.xposed.sesame.model.task.otherTask.OtherTaskRpcCall.2
            {
                put("alipayAppVersion", "10.5.70");
                put("appClient", "Android");
                put("appSource", "consumeGold");
                put("clientVersion", "6.2.0");
                put("favoriteStatus", "Favorite");
                put("taskSceneCode", "CG_SIGNIN_AD_FEEDS");
                put("userType", "");
                put("cacheMap", new JSONObject() {
                    {
                        put("FAVORITE_CONSUME_GOLD", UserMap.getCurrentUid());
                    }
                });
            }
        }));
    }

    public static String consumeGoldTrigger(Map<String, Object> map) {
        return RequestManager.requestString("alipay.mobile.ipsponsorprod.consume.gold.taskV2.trigger", StringUtil.getJsonString(map));
    }

    public static String openBoxAward() throws JSONException {
        return RequestManager.requestString("alipay.mobile.ipsponsorprod.consume.gold.task.openBoxAward", StringUtil.getJsonString(new HashMap<String, Object>() { // from class: fansirsqi.xposed.sesame.model.task.otherTask.OtherTaskRpcCall.3
            {
                put("bizType", "CONSUME_GOLD");
                put("boxType", "CONSUME_GOLD_SIGN_DATE");
                put("clientVersion", OtherTaskRpcCall.clientVersion);
                put("timeScaleType", 0);
                put("userType", "");
                put("actionAwardDetails", new JSONArray() { // from class: fansirsqi.xposed.sesame.model.task.otherTask.OtherTaskRpcCall.3.1
                    {
                        put(new JSONObject() { // from class: fansirsqi.xposed.sesame.model.task.otherTask.OtherTaskRpcCall.3.1.1
                            {
                                put("actionType", "date_sign_start");
                            }
                        });
                    }
                });
            }
        }));
    }

    public static JSONObject trigger() throws JSONException {
        return new JSONObject(RequestManager.requestString("alipay.mobile.ipsponsorprod.consume.gold.index.promo.trigger", "[{\"appSource\":\"consumeGold\",\"cacheMap\":{\"FAVORITE_CONSUME_GOLD\":\"" + UserMap.getCurrentUid() + "\"},\"alipayAppVersion\":\"10.6.20.9000\",\"appClient\":\"Android\",\"clientTraceId\":\"" + UUID.randomUUID().toString() + "\",\"clientVersion\":\"6.5.0\",\"favoriteStatus\":\"Favorite\",\"requestId\":\"" + RandomUtil.getRandomString(16).toUpperCase() + System.currentTimeMillis() + "\"}]"));
    }
}
