package fansirsqi.xposed.sesame.task.otherTask2;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

import fansirsqi.xposed.sesame.data.Status;
import fansirsqi.xposed.sesame.hook.RequestManager;
import fansirsqi.xposed.sesame.task.otherTask.BaseCommTask;
import fansirsqi.xposed.sesame.task.otherTask.CompletedKeyEnum;
import fansirsqi.xposed.sesame.util.JsonUtil;
import fansirsqi.xposed.sesame.util.Log;
import fansirsqi.xposed.sesame.util.RandomUtil;
import fansirsqi.xposed.sesame.util.TimeUtil;

public class MemberNew extends BaseCommTask {

    private final String TAG = "会员积分New💎";
    public MemberNew() {
        this.displayName = "会员积分New💎";
    }
    @Override
    protected void handle() {
        try {
            Log.other(this.displayName + "开始执行");
            if (!Status.hasFlagToday(CompletedKeyEnum.MemberSignIn.name())) {
                JSONObject jSONObject = new JSONObject(AntMemberRpcCall.queryMemberSigninCalendar());
                if ("SUCCESS".equalsIgnoreCase(jSONObject.getString("resultCode"))) {
                    Log.other(this.displayName + "签到获得✅[" + jSONObject.getString("signinPoint") + "积分]#已签到" + jSONObject.getString("signinSumDay") + "天");
                    Status.setFlagToday(CompletedKeyEnum.MemberSignIn.name());
                } else {
                    TimeUtil.sleep((long) this.executeIntervalInt);
                    return;
                }
            }
            signPageTaskList();
            queryAllStatusTaskList();
            memTaskListQueryFacade();
            queryPointCert(1, 8, false);
            Log.other(this.displayName + "执行完成");
        } catch (Throwable th) {
            TimeUtil.sleep((long) this.executeIntervalInt);
        }
        TimeUtil.sleep((long) this.executeIntervalInt);
    }
    /**
     * 处理会员任务列表中的 "BROWSE" 类型任务
     *
     * @param taskList JSONArray 任务数组
     * @return 是否有任务被完成
     */
    private boolean doTask(JSONArray taskList) {
        final String TAG = "MemberNew-doTask";
        final String RESULT_CODE = "resultCode";
        final String SUCCESS = "SUCCESS";

        int index = 0;
        boolean hasCompletedAnyTask = false;

        while (index < taskList.length()) {
            try {
                JSONObject taskItem = taskList.getJSONObject(index);
                boolean isHybrid = taskItem.getBoolean("hybrid");

                int currentCount = 0;
                int targetCount = 0;
                int retryCount = 1;

                if (isHybrid) {
                    JSONObject extInfo = taskItem.getJSONObject("extInfo");
                    currentCount = extInfo.getInt("PERIOD_CURRENT_COUNT");
                    targetCount = extInfo.getInt("PERIOD_TARGET_COUNT");
                    retryCount = targetCount > currentCount ? targetCount - currentCount : 1;
                }

                if (retryCount <= 0) {
                    index++;
                    continue;
                }

                // 获取任务配置信息
                JSONObject taskConfig = taskItem.getJSONObject("taskConfigInfo");
                String taskName = taskConfig.getString("name");
                Long taskId = taskConfig.getLong("id");
                String rewardPoints = taskConfig.getJSONObject("awardParam").getString("awardParamPoint");
                String targetBusiness = taskConfig.getJSONArray("targetBusiness").getString(0);

                for (int attempt = 0; attempt < retryCount; attempt++) {
                    try {
                        // 应用任务
                        JSONObject applyResult = new JSONObject(AntMemberRpcCall.applyTask(taskName, taskId));
                        if (!SUCCESS.equalsIgnoreCase(applyResult.getString(RESULT_CODE))) {
                            Log.other(TAG, "applyTask 失败: " + applyResult.optString("resultDesc"));
                            TimeUtil.sleep((long) this.executeIntervalInt);
                            continue;
                        }

                        // 执行任务
                        String[] split = targetBusiness.split("#");
                        String businessType = split.length > 2 ? split[1] : split[0];
                        String businessId = split.length > 2 ? split[2] : split[1];

                        JSONObject executeResult = new JSONObject(AntMemberRpcCall.executeTask(businessId, businessType));

                        if (SUCCESS.equalsIgnoreCase(executeResult.getString(RESULT_CODE))) {
                            String progress = isHybrid ? String.format(Locale.CHINA, "(%d/%d)", currentCount + attempt + 1, targetCount) : "";
                            Log.other(this.displayName + "完成✅[" + taskName + progress + "]#" + rewardPoints + "积分");
                            hasCompletedAnyTask = true;
                        } else {
                            Log.other(TAG, "executeTask 失败: " + executeResult.optString("resultDesc"));
                        }

                        TimeUtil.sleep((long) this.executeIntervalInt);

                    } catch (Exception e) {
                        Log.error(TAG, "for--doTask err: ");
                        TimeUtil.sleep((long) this.executeIntervalInt);
                        return hasCompletedAnyTask;
                    }
                }

                index++;

            } catch (Exception e) {
                Log.error(TAG, "while--doTask err: ");
                TimeUtil.sleep((long) this.executeIntervalInt);
                return hasCompletedAnyTask;
            }
        }

        TimeUtil.sleep((long) this.executeIntervalInt);
        return hasCompletedAnyTask;
    }


    /**
     * 处理 OTHERS 类型任务
     */
    private void doOtherTask(JSONArray taskList) {
        int index = 0;
        while (index < taskList.length()) {
            JSONObject taskItem = null;
            try {
                taskItem = taskList.getJSONObject(index);
                if (taskItem == null) {
                    Log.other(TAG, "taskItem 为 null，跳过");
                    index++;
                    continue;
                }

                boolean isHybrid = taskItem.optBoolean("hybrid", false);
                int currentCount = 0;
                int targetCount = 0;
                int needExecuteTimes = 1;

                if (isHybrid) {
                    JSONObject extInfo = taskItem.optJSONObject("extInfo");
                    if (extInfo != null) {
                        currentCount = extInfo.optInt("PERIOD_CURRENT_COUNT", 0);
                        targetCount = extInfo.optInt("PERIOD_TARGET_COUNT", 0);
                        needExecuteTimes = targetCount > currentCount ? targetCount - currentCount : 0;
                    }
                }

                if (needExecuteTimes <= 0) {
                    Log.other(TAG + "任务已全部完成，跳过");
                    index++;
                    continue;
                }

                JSONObject config = taskItem.optJSONObject("taskConfigInfo");
                if (config == null) {
                    Log.other(TAG, "任务配置为空，跳过");
                    index++;
                    continue;
                }

                String taskName = config.optString("name", "未知任务");
                Long taskId = config.optLong("id", -1);

                // ✅ 从 taskConfigInfo 中获取 awardParamPoint
                JSONObject awardParam = config.optJSONObject("awardParam");
                String awardPoint = (awardParam != null) ? awardParam.optString("awardParamPoint", "0") : "0";

                JSONArray targetBusinessArray = config.optJSONArray("targetBusiness");
                if (targetBusinessArray == null || targetBusinessArray.length() == 0) {
                    Log.other(TAG + "targetBusinessArray 为空，跳过任务");
                    index++;
                    continue;
                }

                // ✅ 确保从 taskConfigInfo 获取 businessType
                String businessType = config.optString("businessType", "");

                // ✅ 强制只处理 uvChangeBusinessType 类型任务
                if ("uvChangeBusinessType".equalsIgnoreCase(businessType)) {
                    handleGameTask(taskName, taskId, awardPoint, targetBusinessArray);
                } else {
                    //Log.other(TAG + "【跳过】:" + taskName + "，businessType=" + businessType);
                    continue;
                }

            } catch (Exception e) {
                Log.other(displayName + "执行其他任务出错:" + e.getMessage());
                Log.error(displayName + "Others任务原始数据: " + taskItem.toString());
            }

            index++;
            TimeUtil.sleep((long) this.executeIntervalInt);
        }
    }



    private void handleGameTask(String taskName, Long taskId, String awardPoint, JSONArray targetBusinessArray) throws JSONException {
        String[] split = targetBusinessArray.getString(0).split("#");
        String ngfeKey = split.length > 1 ? split[0] : "";
        for (int i = 0; i < 1; i++) {
            JSONObject applyResult = new JSONObject(AntMemberRpcCall.applyTask(taskName, taskId));
            TimeUtil.sleep((long) this.executeIntervalInt);

            if (!"SUCCESS".equalsIgnoreCase(applyResult.optString("resultCode"))) {
                Log.other(this.TAG, "申请任务失败：" + applyResult.optString("resultDesc"));
            } else {
                Log.other(this.TAG,"使用applyTask2申请任务");
                JSONObject applyResult2 = new JSONObject(AntMemberRpcCall.applyTask2(taskId));
                TimeUtil.sleep((long) this.executeIntervalInt);
            }

            JSONObject executeResult = new JSONObject(AntMemberRpcCall.ngfeUpdate(ngfeKey));
            TimeUtil.sleep((long) this.executeIntervalInt);

            if (executeResult.optBoolean("success")) {
                Log.other(this.displayName + "完成任务✅[" + taskName + "]#" + awardPoint + "积分");
            } else {
                Log.other(this.TAG, "执行 NGFE 更新失败：" + executeResult);
            }
        }
    }



    /**
     * 查询签到页任务列表并执行
     */
    private void signPageTaskList() {
        final String CATEGORY_TASK_LIST = "categoryTaskList";
        final int MAX_RETRY_TIMES = 3;

        int retryCount = 0;
        while (retryCount < MAX_RETRY_TIMES) {
            try {
                TimeUtil.sleep(RandomUtil.nextInt(5000, 7000));
                JSONObject response = new JSONObject(AntMemberRpcCall.signPageTaskList());

                TimeUtil.sleep(RandomUtil.nextInt(3000, 5000));
                if (!response.optBoolean("success")) {
                    Log.other(TAG, "获取任务列表1失败❌:"+response.optString("errorMessage"));
                    JSONObject response2 = new JSONObject(AntMemberRpcCall.signPageTaskListNew());
                    if (response2.optBoolean("success")){
                        response = response2;
                        Log.other(TAG, "signPageTaskListNew获取任务列表成功✅:");
                    }else{
                        Log.other(TAG, "获取任务列表2失败❌:"+response2.optString("errorMessage"));
                        break;
                    }
                }

                if (!response.has(CATEGORY_TASK_LIST)) {
                    Log.other(TAG, "无可用任务列表❌");
                    break;
                }

                JSONArray categoryList = response.getJSONArray(CATEGORY_TASK_LIST);
                boolean hasCompletedAnyTask = false;

                for (int i = 0; i < categoryList.length(); i++) {
                    JSONObject category = categoryList.getJSONObject(i);
                    JSONArray taskArray = category.getJSONArray("taskList");
                    String type = category.getString("type");
                    if (taskArray == null || taskArray.length() == 0) {
                        Log.other(TAG, "[" + type + "] 类型任务为空❌，跳过");
                        continue;
                    }
                    if ("OTHERS".equalsIgnoreCase(type)) {
                        if (taskArray.length() == 0) {
                            Log.other(TAG, "已完成其他任务");
                            continue;
                        }
                        doOtherTask(taskArray);
                        hasCompletedAnyTask = true;
                    }else if ("BROWSE".equalsIgnoreCase(type)) {
                        if (taskArray.length() == 0) {
                            Log.other(TAG, "已完成日常任务");
                            continue;
                        }
                        if (doTask(taskArray)) {
                            hasCompletedAnyTask = true;
                        }
                    }
                }
                if (!hasCompletedAnyTask) {
                    Log.other(TAG, "本次没有可执行的任务❌");
                    break;
                }
                TimeUtil.sleep(RandomUtil.nextInt(1000, 2000));
                // 成功处理完一次任务，退出循环
                return;

            } catch (Exception e) {
               Log.error(TAG,"sign任务列表出错❌:"+e);
                retryCount++;
                TimeUtil.sleep((long) this.executeIntervalInt);
            }
        }
        // 最终随机休眠一段时间
        TimeUtil.sleep(RandomUtil.nextInt(15000, 20000));
    }


    public void queryPointCert(int i, int i2, boolean z) {
        String str = "resultCode";
        String str2 = "SUCCESS";
        try {
            JSONObject jSONObject = new JSONObject(AntMemberRpcCall.queryPointCert(i, i2));
            TimeUtil.sleep(RandomUtil.nextInt(3000, 5000));
            String str3 = "resultDesc";
            if (str2.equalsIgnoreCase(jSONObject.getString(str))) {
                boolean z2 = jSONObject.getBoolean("hasNextPage");
                JSONArray jSONArray = jSONObject.getJSONArray("certList");
                for (int i3 = 0; i3 < jSONArray.length(); i3++) {
                    JSONObject jSONObject2 = jSONArray.getJSONObject(i3);
                    String string = jSONObject2.getString("bizTitle");
                    String string2 = jSONObject2.getString("id");
                    int i4 = jSONObject2.getInt("pointAmount");
                    TimeUtil.sleep((long) this.executeIntervalInt);
                    JSONObject jSONObject3 = new JSONObject(AntMemberRpcCall.receivePointByUser(string2));
                    if (str2.equalsIgnoreCase(jSONObject3.getString(str))) {
                        Log.other(this.displayName + "领取奖励✅[" + string + "]#" + i4 + "积分");
                    } else {
                        Log.error(this.TAG + ".receivePointByUser err " + jSONObject3.getString(str3));
                    }
                }
                if (z2) {
                    queryPointCert(i + 1, i2, z);
                }
                TimeUtil.sleep((long) this.executeIntervalInt);
                return;
            }else{
                Log.error(this.TAG + ".queryPointCert err " + jSONObject.getString(str3));
            }
            TimeUtil.sleep((long) this.executeIntervalInt);
        } catch (Throwable th) {
            TimeUtil.sleep((long) this.executeIntervalInt);
        }
    }

    public void memTaskListQueryFacade() {
        try {
            JSONObject requestString = requestString("com.alipay.amic.memtask.h5.MemTaskListQueryFacade.signPageTaskList", "\"source\": \"antmember\",\"sourcePassMap\": {\"innerSource\": \"\",\"source\": \"myTab\",\"unid\": \"\"},\"spaceCode\": \"ant_member_xlight_task\",\"taskTopConfigId\": \"\"");
            if (requestString == null || !requestString.optBoolean("success")) {
                Log.error(this.TAG + ".memTaskListQueryFacade err " + requestString.optString("errorMessage"));
                TimeUtil.sleep((long) this.executeIntervalInt);
                return;
            }
            JSONArray jSONArray = requestString.getJSONObject("resultData").getJSONArray("adTaskList");
            for (int i = 0; i < jSONArray.length(); i++) {
                JSONObject jSONObject = jSONArray.getJSONObject(i);
                String valueByPath = JsonUtil.getValueByPath(jSONObject, "lightsAdExtMap.bizId");
                String valueByPath2 = JsonUtil.getValueByPath(jSONObject, "simpleTaskConfig.title");
                if (!valueByPath.isEmpty()) {
                    JSONObject requestString2 = requestString("com.alipay.adtask.biz.mobilegw.service.task.finish", "\"bizId\": \"" + valueByPath + "\",\"extendInfo\": {}");
                    TimeUtil.sleep((long) this.executeIntervalInt);
                    if (requestString2 != null && requestString2.optBoolean("success")) {
                        Object valueByPathObject = JsonUtil.getValueByPathObject(requestString2, "extendInfo.rewardInfo");
                        if (valueByPathObject != null) {
                            requestString2 = (JSONObject) valueByPathObject;
                            Log.other(this.displayName + "完成任务✅[" + valueByPath2 + "]+" + requestString2.getString("rewardAmount") + requestString2.getString("rewardTypeName"));
                        }
                    }
                }
            }
            TimeUtil.sleep((long) this.executeIntervalInt);
        } catch (Throwable th) {
            TimeUtil.sleep((long) this.executeIntervalInt);
        }
    }


    private void queryAllStatusTaskList() {
        String str = "availableTaskList";
        try {
            JSONObject jSONObject = new JSONObject(AntMemberRpcCall.queryAllStatusTaskList());
            if (jSONObject.optBoolean("success")) {
                if (jSONObject.has(str)) {
                    if (doTask(jSONObject.getJSONArray(str))) {
                        queryAllStatusTaskList();
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