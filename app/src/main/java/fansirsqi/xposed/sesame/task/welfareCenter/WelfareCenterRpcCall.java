package fansirsqi.xposed.sesame.task.welfareCenter;


import org.json.JSONArray;

import fansirsqi.xposed.sesame.hook.ApplicationHook;
import fansirsqi.xposed.sesame.task.otherTask.BaseTaskRpcCall;
import fansirsqi.xposed.sesame.util.JsonUtil;

public class WelfareCenterRpcCall extends BaseTaskRpcCall {
    public static String campTrigger(String str, String str2) {
        return ApplicationHook.requestString("com.alipay.loanpromoweb.promo.camp.trigger", "[{" + (!str2.isEmpty() ? "\"campId\": \"CP15205657\"," + str2 : "\"campId\": \"CP15205657\"") + "}]");
    }

    public static String queryCert(String[] strArr) {
        return ApplicationHook.requestString("com.alipay.loanpromoweb.promo.cert.query", "[{\"certTemplateIdSet\":" + JsonUtil.formatJson(strArr) + "}]");
    }

    public static String batchUseVirtualProfit(JSONArray jSONArray) {
        return ApplicationHook.requestString("com.alipay.loanpromoweb.promo.virtualProfit.batchUseVirtualProfit", "[{\"virtualProfitIdList\":" + jSONArray + "}]");
    }

    public static String playTrigger(String str) {
        return ApplicationHook.requestString("com.alipay.loanpromoweb.promo.playcenter.playTrigger.trigger", "[{\"extInfo\":{},\"operation\":\"MYBK_DACU_INTERACTIVE_ZHB\",\"playId\":\"" + str + "\"}]");
    }

    public static String pointBanlance(String str) {
        return ApplicationHook.requestString("com.alipay.loanpromoweb.promo.group.point.pointBanlance", "[{\"queryExpireEndDate\": \"" + str + "\",\"sceneCode\": \"SUPER930\"}]");
    }

    public static String queryEnableVirtualProfitV2(String str) {
        return ApplicationHook.requestString("com.alipay.loanpromoweb.promo.virtualProfit.queryEnableVirtualProfitV2", "[{\"firstSceneCode\":[],\"profitType\":\"ANTBANK_WELFARE_POINT\",\"sceneCode\":[\"FULICenter_JKJML\",\"FULICenter_JZN\",\"BC3_BC3V1\",\"BC3_BC3V2\",\"BC3_BC3V3\",\"SQB_SQBV0\",\"SQB_SQBV1\",\"SQB_SQBV2\",\"SQB_SQBV3\",\"SQB_SQBV4\",\"SQB_SQBV5\",\"SQB_SQBV6\",\"SQB_SQBV7\",\"SQB_SQBV8\",\"SQB_SQBV9\",\"SQB_SQBV10\",\"SQB_SQBV11\",\"SQB_SQBSIGN\",\"FULICenter_JKJQW\",\"FULICenter_WSWF\",\"FULICenter_FLKZS\",\"FULICenter_KGJXBBF\",\"FULICenter_AXHZXB\",\"FULICenter_BBF\",\"FULICenter_V1\",\"FULICenter_V2\",\"FULICenter_V3\",\"FULICenter_V4\",\"FULICenter_V5\",\"FULICenter_V6\",\"FULICenter_V7\",\"FULICenter_YulibaoAUM\",\"FULICenter_PayByMybank\",\"FULICenter_DepositAUM\",\"FULICenter_YYYYH\",\"FULICenter_QYZ\",\"FULICenter_V7PLUS\",\"FULICenter_V6PLUS\",\"FULICenter_V5PLUS\",\"FULICenter_V8\",\"FULICenter_V9\",\"FULICenter_V10\"],\"signInSceneId\":\"" + str + "\"}]");
    }

    public static String signinPlay() {
        return ApplicationHook.requestString("com.alipay.loanpromoweb.member.play.signinPlay", "[{\"channel\": \"miniApp\",\"needMultiple\": false,\"operation\": \"signApply\",\"playId\": \"PLAY100177545\"}]");
    }

    public static String trigger() {
        return ApplicationHook.requestString("com.alipay.loanpromoweb.promo.camp.trigger", "[{\"campId\": \"CP15205657\",\"extParams\": {\"bkPointUseMemo\": \"抽奖消耗\",\"pcbfcCertMemo\": \"FULICenterUSE\"}}]");
    }
}