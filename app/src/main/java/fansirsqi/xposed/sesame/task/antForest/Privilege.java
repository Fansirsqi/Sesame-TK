package fansirsqi.xposed.sesame.task.antForest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import fansirsqi.xposed.sesame.task.otherTask.CompletedKeyEnum;
import fansirsqi.xposed.sesame.util.Log;
import fansirsqi.xposed.sesame.data.Status;
import fansirsqi.xposed.sesame.util.StringUtil;

public class Privilege {
    private static final String TAG = "青春特权🌸";
    private static final String YOUTH_PRIVILEGE_PREFIX = "青春特权🌸";
    private static final String STUDENT_SIGN_PREFIX = "青春特权🧧";

    // 任务状态常量
    private static final String TASK_RECEIVED = "RECEIVED";
    private static final String TASK_FINISHED = "FINISHED";
    private static final String RPC_SUCCESS = "SUCCESS";

    // 签到时间常量
    private static final int SIGN_IN_START_HOUR = 5;
    private static final int SIGN_IN_END_HOUR = 10;

    // 青春特权任务配置
    private static final List<List<String>> YOUTH_TASKS = Arrays.asList(
            Arrays.asList("DNHZ_SL_college", "DAXUESHENG_SJK", "双击卡"),
            Arrays.asList("DXS_BHZ", "NENGLIANGZHAO_20230807", "保护罩"),
            Arrays.asList("DXS_JSQ", "JIASUQI_20230808", "加速器")
    );

    public static boolean youthPrivilege() {
        try {
            if (!Status.canYouthPrivilegeToday()) return false;

            List<String> processResults = new ArrayList<>();
            for (List<String> task : YOUTH_TASKS) {
                processResults.addAll(processYouthPrivilegeTask(task));
            }

            boolean allSuccess = true;
            for (String result : processResults) {
                if (!"处理成功".equals(result)) {
                    allSuccess = false;
                    break;
                }
            }

            if (allSuccess) Status.setYouthPrivilegeToday();
            return allSuccess;
        } catch (Exception e) {
            Log.printStackTrace(TAG + "青春特权领取异常", e);
            return false;
        }
    }


    private static List<String> processYouthPrivilegeTask(List<String> taskConfig) throws JSONException {
        String queryParam = taskConfig.get(0);
        String receiveParam = taskConfig.get(1);
        String taskName = taskConfig.get(2);

        JSONArray taskList = getTaskList(queryParam);
        if (taskList == null || taskList.length()==0){
            return null;
        }
        return handleTaskList(taskList, receiveParam, taskName);
    }

    private static JSONArray getTaskList(String queryParam) throws JSONException {
        String response = AntForestRpcCall.queryTaskListV2(queryParam);
        JSONObject result = new JSONObject(response);
        if (!result.has("forestTasksNew")) {
            return null;
        }
        return result.getJSONArray("forestTasksNew")
                .getJSONObject(0)
                .getJSONArray("taskInfoList");
    }

    private static List<String> handleTaskList(JSONArray taskInfoList, String taskType, String taskName) {
        List<String> results = new ArrayList<>();
        for (int i = 0; i < taskInfoList.length(); i++) {
            JSONObject task = taskInfoList.optJSONObject(i);
            if (task == null) continue;

            JSONObject baseInfo = task.optJSONObject("taskBaseInfo");
            if (baseInfo == null) continue;

            String currentTaskType = baseInfo.optString("taskType");
            if (!taskType.equals(currentTaskType)) continue;

            processSingleTask(baseInfo, taskType, taskName, results);
        }
        return results;
    }

    private static void processSingleTask(JSONObject baseInfo, String taskType, String taskName, List<String> results) {
        String taskStatus = baseInfo.optString("taskStatus");
        if (TASK_RECEIVED.equals(taskStatus)) {
            Log.forest(YOUTH_PRIVILEGE_PREFIX + "[%s]已领取", taskName);
            return;
        }

        if (TASK_FINISHED.equals(taskStatus)) {
            handleFinishedTask(taskType, taskName, results);
        }
    }

    private static void handleFinishedTask(String taskType, String taskName, List<String> results) {
        try {
            JSONObject response = new JSONObject(AntForestRpcCall.receiveTaskAwardV2(taskType));
            String resultDesc = response.optString("desc");
            results.add(resultDesc);
            String logMessage = "处理成功".equals(resultDesc) ? "领取成功" : "领取结果：" + resultDesc;
            Log.forest(YOUTH_PRIVILEGE_PREFIX + "["+taskName+"]" + logMessage);
        } catch (JSONException e) {
            Log.printStackTrace(TAG + "奖励领取结果解析失败", e);
            results.add("处理异常");
        }
    }

    public static void studentSignInRedEnvelope() {
        try {
//            if (!isSignInTimeValid()) {
//                Log.record(STUDENT_SIGN_PREFIX + "5点前不执行签到");
//                return;
//            }

            if (!Status.canStudentTask()) {
                Log.record(STUDENT_SIGN_PREFIX + "今日已完成签到");
                return;
            }

            processStudentSignIn();
        } catch (Exception e) {
            Log.printStackTrace(TAG + "学生签到异常", e);
        }
    }

    private static boolean isSignInTimeValid() {
        int currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        return currentHour >= SIGN_IN_START_HOUR;
    }

    private static void processStudentSignIn() throws JSONException {
        String response = AntForestRpcCall.studentQqueryCheckInModel();
        JSONObject result = new JSONObject(response);

        if (!RPC_SUCCESS.equals(result.optString("resultCode"))) {
            Log.record(STUDENT_SIGN_PREFIX + "查询失败：" + result.optString("resultDesc"));
            return;
        }

        JSONObject checkInInfo = result.optJSONObject("studentCheckInInfo");
        if (checkInInfo == null || "DO_TASK".equals(checkInInfo.optString("action"))) {
            Status.setStudentTaskToday();
            return;
        }

        executeStudentSignIn();
    }

    private static void executeStudentSignIn() {
        try {
            String tag = Calendar.getInstance().get(Calendar.HOUR_OF_DAY) < SIGN_IN_END_HOUR
                    ? "double" : "single";

            JSONObject result = new JSONObject(AntForestRpcCall.studentCheckin());
            handleSignInResult(result, tag);
        } catch (JSONException e) {
            Log.printStackTrace(TAG + "签到结果解析失败", e);
        }
    }

    private static void handleSignInResult(JSONObject result, String tag) {
        String resultCode = result.optString("resultCode");
        String resultDesc = result.optString("resultDesc", "签到成功");

        if (RPC_SUCCESS.equals(resultCode)) {
            Status.setStudentTaskToday();
            String logMessage = STUDENT_SIGN_PREFIX + tag + resultDesc;
            Log.forest(logMessage);
        } else {
            String errorMsg = resultDesc.contains("不匹配") ? resultDesc + "可能账户不符合条件" : resultDesc;
            String logMessage = STUDENT_SIGN_PREFIX + tag + "失败：" + errorMsg;
            Log.error(TAG, logMessage);
        }
    }

    /**
     * 青春特权--任务
     */
    public static void processStudentTasks() {
        try {
            // 使用一个Map来记录每个任务的异常次数
            java.util.Map<String, Integer> taskErrorCounts = new java.util.HashMap<>();
            boolean allTasksCompleted = true;

            // Step 1: 查询任务模型
            String queryResponse = AntForestRpcCall.queryTaskModel("searchxsth", false);
            JSONObject queryResult = new JSONObject(queryResponse);

            // 检查查询是否成功
            if (!"SUCCESS".equals(queryResult.optString("resultCode"))) {
                Log.error(TAG, "任务查询失败：" + queryResult);
                Log.forest(TAG, "任务查询失败：" + queryResult.optString("resultDesc"));
                Log.other(TAG, "任务查询失败：" + queryResult.optString("resultDesc"));
                return;
            }

            // 提取任务列表
            JSONObject feedsTaskVO = queryResult.optJSONObject("studentTaskModule");
            if (feedsTaskVO == null) {
                Log.error(TAG, "未找到任务模块");
                Log.forest(TAG, "未找到任务模块");
                Log.other(TAG, "未找到任务模块");
                return;
            }

            JSONArray taskList = feedsTaskVO.optJSONArray("taskGroupList")
                    .optJSONObject(0)
                    .optJSONArray("taskList");

            // Step 2: 检查所有任务是否已完成
            for (int i = 0; i < taskList.length(); i++) {
                JSONObject task = taskList.optJSONObject(i);
                String taskStatus = task.optString("taskStatus");

                if (!"COMPLETE".equals(taskStatus)) {
                    allTasksCompleted = false;
                    break;
                }
            }

            // 如果所有任务已完成，设置任务状态为完成今日不再执行
            if (allTasksCompleted) {
                Status.setFlagToday(CompletedKeyEnum.privilegeTask.name());
                Log.other(STUDENT_SIGN_PREFIX + "所有任务已完成🏆");
                Log.forest(STUDENT_SIGN_PREFIX + "所有任务已完成🏆");
                return;
            }

            // Step 3: 遍历任务列表并处理任务
            for (int i = 0; i < taskList.length(); i++) {
                JSONObject task = taskList.optJSONObject(i);
                String taskCode = task.optString("taskCode");
                String taskSource = task.optString("taskSource");
                String taskType = task.optString("taskType");
                String taskName = task.optString("taskName");
                int currentCount = task.optInt("currentCount", 0); // 使用默认值0
                int totalCount = task.optInt("totalCount", 1); // 使用默认值1
                String taskStatus = task.optString("taskStatus");
                String taskBizId = task.optString("taskBizId");
                String prizeAmount = task.optString("prizeAmount", "0"); // 获取奖励金额，默认为 "0"

                // 如果某个任务异常次数达到3次，则跳过该任务
                int errorCount = taskErrorCounts.containsKey(taskName) ? taskErrorCounts.get(taskName) : 0;
                if (errorCount >= 2) {
                    Log.other(STUDENT_SIGN_PREFIX + "--跳过异常次数过多的任务：" + taskName);
                    Log.forest(STUDENT_SIGN_PREFIX + "--跳过异常次数过多的任务：" + taskName);
                    continue;
                }

                try {
                    if ("COMPLETE".equals(taskStatus)) {
                        //Log.other(STUDENT_SIGN_PREFIX + "任务已完成：" + taskName);
                        //Log.forest(STUDENT_SIGN_PREFIX + "任务已完成：" + taskName);
                        continue;
                    }
                    // 进行领取报名
                    taskSignUp(taskBizId, taskCode, taskSource, taskType);
                    // 模拟等待
                    waitForDuration(16000);
                    // 处理任务进度
                    if ("TO_APPLY".equals(taskStatus) || "PROCESSING".equals(taskStatus)) {
                        String completeResponse = AntForestRpcCall.taskComplete(taskBizId, taskCode, taskSource, taskType);
                        JSONObject completeResult = new JSONObject(completeResponse);

                        if ("SUCCESS".equals(completeResult.optString("resultCode"))) {
                            Log.other(STUDENT_SIGN_PREFIX + "完成[" + taskName + "]获得:" + prizeAmount + "豆子");
                            Log.forest(STUDENT_SIGN_PREFIX + "完成[" + taskName + "]获得:" + prizeAmount + "豆子");
                        } else {
                            Log.error(TAG, "任务[" + taskName + "]失败：" + completeResult.optString("resultDesc"));
                            throw new Exception("任务失败");
                        }
                    }

                } catch (Exception e) {
                    // 记录异常次数
                    taskErrorCounts.put(taskName, errorCount + 1);
                    Log.error(TAG, "任务异常：" + taskName + ", 异常次数：" + taskErrorCounts.get(taskName));

                    // 如果异常次数达到3次，跳过该任务
                    if (taskErrorCounts.get(taskName) >= 3) {
                        Log.other(STUDENT_SIGN_PREFIX + "--跳过异常次数过多的任务：" + taskName);
                        Log.forest(STUDENT_SIGN_PREFIX + "--跳过异常次数过多的任务：" + taskName);
                    }
                }

            }
        } catch (JSONException e) {
            Log.printStackTrace(TAG + "青春特权--任务处理异常", e);
        }
    }




    public static String taskSignUp(String taskBizId, String taskCode, String taskSource, String taskType) {
        try {
            // 发送请求
            String response = AntForestRpcCall.taskSignUp(taskBizId, taskCode, taskSource, taskType);
            JSONObject result = new JSONObject(response);

            // 检查结果
            String resultCode = result.optString("resultCode");
            String resultDesc = result.optString("resultDesc", "报名成功");

            if ("SUCCESS".equals(resultCode)) {
                Log.other(STUDENT_SIGN_PREFIX + "任务报名成功：" + taskCode);
                //Log.forest(STUDENT_SIGN_PREFIX + "任务报名成功：" + taskCode);
            } else {
                Log.error(TAG, "任务报名失败：" + resultDesc);
            }

            return response;
        } catch (Exception e) {
            Log.error(TAG, "任务报名异常：" + taskCode + ", 异常信息：" + e.getMessage());
            return "";
        }
    }




    // 等待方法
    private static void waitForDuration(long duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Log.error(TAG, "等待被中断");
        }
    }


}
