package fansirsqi.xposed.sesame.task.otherTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

import fansirsqi.xposed.sesame.data.Status;
import fansirsqi.xposed.sesame.hook.RequestManager;
import fansirsqi.xposed.sesame.util.JsonUtil;
import fansirsqi.xposed.sesame.util.Log;
import fansirsqi.xposed.sesame.util.RandomUtil;
import fansirsqi.xposed.sesame.util.TimeUtil;

public class JobRight extends BaseCommTask{
    private static String method = "alipay.imasp.program.programInvoke";
    private String displayName = "工作中心积分红包 💼";

    private Set<String> skippedTasks = new HashSet<>();
    @Override
    protected void handle() throws JSONException {
        if (!Status.hasFlagToday(CompletedKeyEnum.JobRightSign.name())) {
            Sign();
        }
        if(!Status.hasFlagToday("JobRightTask")) {
            doTask();
        }
    }

    private void doTask() {
        // 查询任务列表
        String taskResult = query();
        if (taskResult == null || taskResult.isEmpty()) {
            Log.error(displayName + "查询任务失败: 返回值为空");
            return;
        }

        try {
            // 解析 JSON 响应
            JSONObject jsonObject = new JSONObject(taskResult);

            // 获取任务列表
            JSONArray playTaskOrderInfoList = (JSONArray) JsonUtil.getValueByPathObject(jsonObject,
                    "components.independent_component_task_reward_01961455_independent_component_task_reward_query.content.playTaskOrderInfoList");

            if (playTaskOrderInfoList == null || playTaskOrderInfoList.length() == 0) {
                Log.other(displayName + "未获取到任务列表");
                return;
            }

            // 遍历任务列表
            for (int i = 0; i < playTaskOrderInfoList.length(); i++) {
                JSONObject task = playTaskOrderInfoList.getJSONObject(i);

                // 提取任务 code 和状态
                String taskCode = task.optString("code", "");
                String taskStatus = task.optString("taskStatus", "");
                JSONObject extInfo = task.optJSONObject("extInfo");
                String activityName;
                if (extInfo != null) {
                    // 提取 activityName
                    activityName = extInfo.optString("activityName", "未知任务");
                } else {
                    activityName = "未知任务名";
                }

                if (skippedTasks.contains(taskCode)){
                    continue;
                }
                // 只处理 taskStatus 为 "init" 的任务
                if (!"finish".equals(taskStatus)) {
                    TimeUtil.sleep(RandomUtil.nextInt(20000,32000));
                    // 调用 subTask 完成任务
                    String recordNoNew = apply(taskCode);
                    if (recordNoNew == null || recordNoNew.isEmpty()) {
                        Log.other(displayName + "申请["+activityName+"]任务失败: 返回值为空");
                        continue;
                    }
                    subTask(taskCode,recordNoNew);
                } else {
                    Log.other(displayName + "跳过任务: " + activityName + ", 状态: " + taskStatus);
                }
            }
            Status.setFlagToday("JobRightTask");
        } catch (JSONException e) {
            Log.error(displayName + "解析任务列表失败: " + e.getMessage());
        }
    }
    private void subTask(String taskCode,String recordNo) {
        Long outBizNo = System.currentTimeMillis();
        String s = RequestManager.requestString(method,
                "[{\"components\":{\"independent_component_task_reward_01961455_independent_component_task_reward_process\":" +
                        "{\"code\":\""+taskCode+"\",\"outBizNo\":"+outBizNo+",\"recordNo\":\""+recordNo+"\"}}," +
                        "\"operationParamIdentify\":\"independent_component_program2024121902034600\",\"source\":\"job-right-center\"}]");

        if (s == null || s.isEmpty()) {
            Log.error(displayName + "提交任务失败: 返回值为空");
            return;
        }

        try {
            JSONObject json = new JSONObject(s);

            // 检查 isSuccess 字段
            if (!json.optBoolean("isSuccess", false)) {
                Log.error(displayName + "提交任务失败: isSuccess=false");
                skippedTasks.add(taskCode);
                return;
            }

            // 获取 claimedTask 的 displayInfo
            JSONObject content = json.optJSONObject("components")
                    .optJSONObject("independent_component_task_reward_01961455_independent_component_task_reward_process")
                    .optJSONObject("content");

            if (content == null) {
                Log.error(displayName + "未获取到任务内容");
                return;
            }

            JSONObject processedTask = content.optJSONObject("processedTask");
            if (processedTask == null) {
                Log.error(displayName + "未获取到 processedTask");
                return;
            }

            JSONObject displayInfo = processedTask.optJSONObject("displayInfo");
            if (displayInfo == null) {
                Log.error(displayName + "未获取到 displayInfo");
                return;
            }

            // 提取 activityName 和 activityValue
            String activityName = displayInfo.optString("activityName", "未知任务");
            int activityValue = displayInfo.optInt("activityValue", 0);

            // 打印结果
            Log.other(displayName + "完成[" + activityName + "]获得工分[" + activityValue+"]⭐");

        } catch (JSONException e) {
            Log.error(displayName + "解析任务结果失败: " + e.getMessage());
        }
    }

    private String apply(String taskCode) {
        String recordNo = "";
        String data = "[{\"components\":{\"independent_component_task_reward_01961455_independent_component_task_reward_apply\":" +
                "{\"code\":\"" + taskCode + "\"}},\"deviceInfo\":{},\"operationParamIdentify\":\"independent_component_program2024121902034600\",\"source\":\"job-right-center\"}]";

        String s = RequestManager.requestString(method, data);
        if (s == null || s.isEmpty()) {
            Log.error(displayName + "申请任务失败: HTTP响应为空");
            return recordNo;
        }

        try {
            JSONObject json = new JSONObject(s);
            if (!json.optBoolean("isSuccess", false)) {
                Log.error(displayName + "申请任务失败:"+json);
                return recordNo;
            }

            JSONObject components = json.optJSONObject("components");
            if (components == null) {
                Log.error(displayName + "申请任务失败: components字段缺失");
                return recordNo;
            }

            JSONObject applyComponent = components.optJSONObject(
                    "independent_component_task_reward_01961455_independent_component_task_reward_apply");
            if (applyComponent == null) {
                Log.error(displayName + "申请任务失败: 组件字段缺失");
                return recordNo;
            }

            JSONObject content = applyComponent.optJSONObject("content");
            if (content == null) {
                Log.error(displayName + "申请任务失败: content字段缺失");
                return recordNo;
            }

            JSONObject claimedTask = content.optJSONObject("claimedTask");
            if (claimedTask == null) {
                Log.error(displayName + "申请任务失败: claimedTask字段缺失");
                return recordNo;
            }

            // 提取核心字段
            recordNo = claimedTask.optString("recordNo", "");
            if (recordNo.isEmpty()) {
                Log.error(displayName + "申请任务失败: recordNo字段缺失");
                return recordNo;
            }
        } catch (JSONException e) {
            Log.error(displayName + "申请任务失败: JSON解析异常 - " + e.getMessage());
            Log.error(displayName + "原始响应: " + s); // 输出原始响应便于调试
        }
        return recordNo;
    }

    private String query() {
        return RequestManager.requestString(method,
                "[{\"channel\":\"job-right-center\",\"components\":{\"independent_component_task_reward_01961455_independent_component_task_reward_query\":{}},\"deviceInfo\":{},\"operationParamIdentify\":\"independent_component_program2024121902034600\",\"source\":\"job-right-center\"}]");
    }

    private void Sign() {
        String code = "";
        String s0 = RequestManager.requestString(method,
                "[{\"components\":{\"independent_component_sign_in_01961456_independent_component_sign_in_recall\":{}},\"deviceInfo\":{},\"operationParamIdentify\":\"independent_component_program2024121902034600\",\"source\":\"job-right-center\"}]");

        if (s0 == null || s0.isEmpty()) {
            Log.other(displayName + "查询签到信息失败: 返回值为空");
            return;
        }

        try {
            // 解析签到模板信息
            JSONObject jsonObject = new JSONObject(s0);
            JSONArray playSignInOrderInfoList = (JSONArray) JsonUtil.getValueByPathObject(jsonObject,
                    "components.independent_component_sign_in_01961456_independent_component_sign_in_recall.content.playSignInOrderInfoList");

            if (playSignInOrderInfoList == null || playSignInOrderInfoList.length() == 0) {
                Log.other(displayName + "未获取到签到模板信息");
                return;
            }

            // 获取第一个签到模板的 code
            JSONObject firstTemplate = playSignInOrderInfoList.getJSONObject(0);
            JSONObject playSignInTemplateInfo = firstTemplate.optJSONObject("playSignInTemplateInfo");

            if (playSignInTemplateInfo == null) {
                Log.other(displayName + "签到模板信息缺失");
                return;
            }

            code = playSignInTemplateInfo.optString("code", "");
            if (code.isEmpty()) {
                Log.other(displayName + "签到模板 code 缺失");
                return;
            }

        } catch (JSONException e) {
            Log.other(displayName + "查询签到信息失败: " + e.getMessage());
            return;
        }

        // 提交签到请求
        String s = RequestManager.requestString(method,
                "[{\"components\":{\"independent_component_sign_in_01961456_independent_component_sign_in\":" +
                        "{\"code\":\"" + code + "\"}},\"deviceInfo\":{},\"operationParamIdentify\":" +
                        "\"independent_component_program2024121902034600\",\"source\":\"job-right-center\"}]");

        if (s == null || s.isEmpty()) {
            Log.other(displayName + "签到提交失败: 返回值为空");
            return;
        }

        try {
            JSONObject json = new JSONObject(s);

            // 检查签到是否成功
            if (!json.optBoolean("isSuccess", false)) {
                Log.other(displayName + "签到提交失败: isSuccess=false");
                return;
            }

            // 获取签到结果信息
            JSONObject playSignInResultInfo = (JSONObject) JsonUtil.getValueByPathObject(json,
                    "components.independent_component_sign_in_01961456_independent_component_sign_in.content.playSignInResultInfo");

            if (playSignInResultInfo == null) {
                Log.other(displayName + "签到结果信息缺失");
                return;
            }

            // 获取签到周期实例信息
            JSONObject playSignInCycleInstanceInfo = playSignInResultInfo.optJSONObject("playSignInCycleInstanceInfo");
            if (playSignInCycleInstanceInfo == null) {
                Log.other(displayName + "签到周期实例信息缺失");
                return;
            }

            // 获取签到统计信息
            String signCount = playSignInCycleInstanceInfo.optString("accumulativeSignInCount", "0");
            String continuousSignCount = playSignInCycleInstanceInfo.optString("continuousSignInCount", "0");

            // 获取签到周期日期
            String cycleStartDate = playSignInCycleInstanceInfo.optString("cycleStartDate", "未知");
            String cycleEndDate = playSignInCycleInstanceInfo.optString("cycleEndDate", "未知");

            // 打印签到结果
            Log.other(displayName + "签到成功: 累计签到次数=" + signCount +
                    ", 连续签到次数=" + continuousSignCount +
                    ", 当前周期=" + cycleStartDate + " 至 " + cycleEndDate);
            Status.setFlagToday(CompletedKeyEnum.JobRightSign.name());
        } catch (JSONException e) {
            Log.other(displayName + "签到失败: " + e.getMessage());
        }
    }
}
