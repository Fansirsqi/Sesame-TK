package fansirsqi.xposed.sesame.task.welfareCenter;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.Optional;

import org.json.JSONArray;
import org.json.JSONObject;

import fansirsqi.xposed.sesame.data.Status;
import fansirsqi.xposed.sesame.task.otherTask.BaseCommTask;
import fansirsqi.xposed.sesame.task.otherTask.CompletedKeyEnum;
import fansirsqi.xposed.sesame.util.JsonUtil;
import fansirsqi.xposed.sesame.util.Log;
import fansirsqi.xposed.sesame.util.TimeUtil;

public class WenLiBao extends BaseCommTask {
    // 常量定义
    private static final String TAG = "WenLiBao";
    private static final String DISPLAY_NAME = "稳利宝🐟";
    private static final long DEFAULT_INTERVAL = 3000;

    // 线程安全间隔时间存储
    private final AtomicInteger executeIntervalInt = new AtomicInteger((int) DEFAULT_INTERVAL);

    // 接口服务名称常量
    private static final String QUERY_SERVICE = "com.mybank.bksonarpro.biz.mobile.service.FundExperienceService.queryFundExperienceStatusListInfo";
    private static final String DECISION_SERVICE = "com.mybank.bkfundprod.biz.mobile.service.bff.PageDecisionFacade.decisionPageView";
    private static final String WRITEOFF_SERVICE = "com.mybank.bksonarpro.biz.mobile.service.FundExperienceService.fundExperienceWriteOff";

    // 枚举参数常量
    private static final String[] STATUS_ENUMS = {"waitReceive", "waitUse"};
    private static final int VRTL_TYPE = 1004;

    public WenLiBao() {
        super();
        this.displayName = DISPLAY_NAME;
    }

    @Override
    protected void handle() {
        try {
            if (Boolean.TRUE.equals(this.mapHandler.get("wenLiBao"))) {
                if (!Status.hasFlagToday(CompletedKeyEnum.WenLiBao.name())) {
                    executeWithLogging(() -> {
                        WelfareCenterRpcCall.doTask("AP13266408", this.TAG, this.displayName);
                        createRecording();
                    }, "核心任务执行");

                    Status.setFlagToday(CompletedKeyEnum.WenLiBao.name());
                }
            }
        } finally {
            TimeUtil.sleep(executeIntervalInt.get());
        }
    }

    private void createRecording() {
        try {
            JSONObject request = sendRequest(QUERY_SERVICE, buildQueryParams());
            if (request == null) return;

            JSONArray waitUseList = getJSONArrayFromPath(request, "data.waitUseList");
            if (waitUseList == null) return;

            for (int i = 0; i < waitUseList.length(); i++) {
                processBenefit(waitUseList.getJSONObject(i));
            }
        } catch (Exception e) {
            Log.error(TAG + ".createRecording error: ", String.valueOf(e));
        }
    }

    private void processBenefit(JSONObject benefit) {
        try {
            String benefitId = benefit.getString("benefitId");
            String voucherName = benefit.getString("voucherName");
            JSONObject amountInfo = benefit.getJSONObject("fundExperienceAmount");

            JSONObject decisionResponse = sendRequest(DECISION_SERVICE, buildDecisionParams(benefitId));
            if (decisionResponse == null) return;

            String fundCode = JsonUtil.getValueByPath(decisionResponse,
                    "data.themeFundModelView.matchedInfo.invest_decision.templateContent.[0].products.[0].fundCode");

            if (fundCode == null || fundCode.isEmpty()) {
                Log.other(TAG + ".processBenefit 资金代码为空: " + benefitId);
                return;
            }

            String writeOffParams = buildWriteOffParams(benefitId, fundCode);
            if (sendRequest(WRITEOFF_SERVICE, writeOffParams) != null) {
                logSuccess(voucherName, amountInfo.optString("amount"));
            }
        } catch (Exception e) {
            Log.error(TAG + ".processBenefit error: ", String.valueOf(e));
        }
    }

    private JSONObject sendRequest(String service, String params) {
        try {
            JSONObject response = requestString(service, params);
            if (response == null) {
                Log.error(TAG + ".sendRequest 空响应: " + service);
            }
            return response;
        } catch (Exception e) {
            Log.error(TAG + ".sendRequest 失败: " + service, String.valueOf(e));
            return null;
        }
    }

    private String buildQueryParams() {
        return new StringBuilder()
                .append("\"extInfo\": {},")
                .append("\"queryStatusEnumList\": [\"")
                .append(STATUS_ENUMS[0])
                .append("\",\"")
                .append(STATUS_ENUMS[1])
                .append("\"],")
                .append("\"vrtlType\": ")
                .append(VRTL_TYPE)
                .toString();
    }

    private String buildDecisionParams(String benefitId) {
        return String.format(
                "\"busiCmd\": {\"benefitId\": \"%s\",\"vrtlType\": \"%d\"},\"extInfo\": {},\"pageFlag\": \"term_experience_fund_verification_component\"",
                benefitId, VRTL_TYPE);
    }

    private String buildWriteOffParams(String benefitId, String fundCode) {
        return String.format(
                "\"extInfo\": {},\"vrtlType\": \"%d\",\"writeOffInfos\": [{\"benefitIds\": [\"%s\"],\"%s\"}]",
                VRTL_TYPE, benefitId, fundCode);
    }

    private JSONArray getJSONArrayFromPath(JSONObject obj, String path) {
        try {
            Object result = JsonUtil.getValueByPathObject(obj, path);
            return result instanceof JSONArray ? (JSONArray) result : null;
        } catch (Exception e) {
            Log.error(TAG + ".getJSONArrayFromPath error at " + path, String.valueOf(e));
            return null;
        }
    }

    private void logSuccess(String voucherName, String amount) {
        Log.other(this.displayName + "申请[" + voucherName + "]+" + amount);
    }

    private void executeWithLogging(Runnable task, String taskName) {
        try {
            task.run();
        } catch (Exception e) {
            Log.error(TAG + "." + taskName + "执行失败: ", String.valueOf(e));
        }
    }
}
