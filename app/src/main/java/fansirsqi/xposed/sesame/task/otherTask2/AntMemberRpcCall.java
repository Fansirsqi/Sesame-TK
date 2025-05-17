package fansirsqi.xposed.sesame.task.otherTask2;


import fansirsqi.xposed.sesame.entity.RpcEntity;
import fansirsqi.xposed.sesame.hook.ApplicationHook;
import fansirsqi.xposed.sesame.hook.RequestManager;
import fansirsqi.xposed.sesame.util.RandomUtil;

public class AntMemberRpcCall {
    public static String executeTask(String str, String str2) {
        return ApplicationHook.requestString("alipay.antmember.biz.rpc.membertask.h5.executeTask", "[{\"bizOutNo\":\"" + (System.currentTimeMillis() - 16000) + "\",\"bizParam\":\"" + str + "\",\"bizSubType\":\"" + str2 + "\",\"bizType\":\"BROWSE\"}]");
    }

    public static Boolean check() {
        boolean z = true;
        RpcEntity requestObject = RequestManager.requestObject("alipay.antmember.biz.rpc.member.h5.queryPointCert", "[{\"page\":1,\"pageSize\":8}]", 1, 0);
        if (requestObject == null || requestObject.getHasError().booleanValue()) {
            z = false;
        }
        return Boolean.valueOf(z);
    }

    public static String queryPointCert(int i, int i2) {
        return ApplicationHook.requestString("alipay.antmember.biz.rpc.member.h5.queryPointCert", "[{\"page\":" + i + ",\"pageSize\":" + i2 + "}]");
    }

    public static String receivePointByUser(String str) {
        return ApplicationHook.requestString("alipay.antmember.biz.rpc.member.h5.receivePointByUser", "[{\"certId\":" + str + "}]");
    }

    public static String rpcCall_signIn() {
        return ApplicationHook.requestString("alipay.kbmemberprod.action.signIn", "[{\"sceneCode\":\"KOUBEI_INTEGRAL\",\"source\":\"ALIPAY_TAB\",\"version\":\"2.0\"}]");
    }

    public static String applyTask(String str, Long l) {
        return ApplicationHook.requestString("alipay.antmember.biz.rpc.membertask.h5.applyTask", "[{\"darwinExpParams\":{\"darwinName\":\"" + str + "\"},\"sourcePassMap\":{\"innerSource\":\"\",\"source\":\"myTab\",\"unid\":\"\"},\"taskConfigId\":" + l + "}]");
    }
    //新方法？
    public static String applyTask2(Long l) {
        return ApplicationHook.requestString("alipay.antmember.biz.rpc.membertask.h5.applyTask", "[{\"sourcePassMap\":{\"innerSource\":\"\",\"source\":\"myTab\",\"unid\":\"\"},\"taskConfigId\":\""+l+"\"}]");
    }

    private static String getUniqueId() {
        return String.valueOf(System.currentTimeMillis()) + RandomUtil.nextLong();
    }

    public static String ngfeUpdate(String str) {
        return ApplicationHook.requestString("com.alipay.csprod.prom.camp.ngfe.update", "[{\"tagCode\":\"" + str + "\"}]");
    }

    public static String queryAllStatusTaskList() {
        return ApplicationHook.requestString("alipay.antmember.biz.rpc.membertask.h5.queryAllStatusTaskList", "[{\"sourceBusiness\":\"signInAd\"}]");
    }

    public static String queryMemberSigninCalendar() {
        return ApplicationHook.requestString("com.alipay.amic.biz.rpc.signin.h5.queryMemberSigninCalendar", "[{\"autoSignIn\":true,\"invitorUserId\":\"\",\"sceneCode\":\"QUERY\"}]");
    }

    public static String signPageTaskList() {
        return ApplicationHook.requestString("alipay.antmember.biz.rpc.membertask.h5.signPageTaskList", "[{\"sourceBusiness\":\"antmember\",\"spaceCode\":\"ant_member_xlight_task\",\"sourcePassMap\": {\"innerSource\": \"\",\"source\": \"myTab\",\"unid\": \"\"},}]");
    }
    public static String signPageTaskListNew() {
        return RequestManager.requestString("alipay.antmember.biz.rpc.membertask.h5.signPageTaskList",
                "[{\"sourceBusiness\":\"antmember\",\"spaceCode\":\"ant_member_xlight_task\"}]");
    }
    public static String transcodeCheck() {
        return ApplicationHook.requestString("alipay.mrchservbase.mrchbusiness.sign.transcode.check", "[{}]");
    }
}