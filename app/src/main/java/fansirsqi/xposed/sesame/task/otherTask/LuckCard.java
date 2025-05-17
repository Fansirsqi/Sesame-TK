package fansirsqi.xposed.sesame.task.otherTask;

import org.json.JSONArray;
import org.json.JSONObject;

import fansirsqi.xposed.sesame.data.Status;
import fansirsqi.xposed.sesame.util.JsonUtil;
import fansirsqi.xposed.sesame.util.Log;
import fansirsqi.xposed.sesame.util.TimeUtil;

public class LuckCard extends BaseCommTask {

    public LuckCard() {
        this.displayName = "好运卡 🎯";
        //this.hoursKeyEnum = CompletedKeyEnum.LuckCard;
    }

    @Override
    protected void handle() {
        try {
            consult();
        } catch (Exception e) {
            Log.printStackTrace(e);
        } finally {
            // 确保无论是否成功，都执行一次延时
            TimeUtil.sleep((long) this.executeIntervalInt);
        }
    }

    private void consult() throws Exception {
        JSONObject request = requestString("com.alipay.pcreditcardweb.activity.LuckCard.consult", "");
        if (request == null) {
            Log.error(displayName+"debug--"+request);
            return;
        }
        int errorCode = request.optInt("error");
        if (errorCode == 1009 || errorCode == 48 || errorCode == 6004) {
            Log.other(displayName + "错误 error --原因: " + request);
            return;
        }
        String taskCenterId = JsonUtil.getValueByPath(request, "result.taskInfo.taskCenterId");
        if (taskCenterId.isEmpty()) {
            return;
        }

        sleep();

        JSONObject taskListResponse = requestStringAllNew("com.alipay.pcreditcardweb.activity.LuckCard.queryTaskList", "[]");
        if (!taskListResponse.getBoolean("success")) {
            Log.error(displayName+"debug--"+taskListResponse);
            return;
        }

        JSONArray taskArray = taskListResponse.getJSONArray("result");
        if (taskArray == null || taskArray.length() == 0) {
            return;
        }

        for (int i = 0; i < taskArray.length(); i++) {
            JSONObject task = taskArray.getJSONObject(i);
            String status = task.getString("taskProcessStatus");
            if ("RECEIVE_SUCCESS".equals(status)) {
                continue;
            }

            JSONObject extProps = task.getJSONObject("taskExtProps");
            //跳过不是浏览的任务
            if ("TRANSFORMER".equals(JsonUtil.getValueByPath(extProps, "TASK_TYPE"))) {
                continue;
            }
            //完成任务中心的任务
            if ("WAITING_TIME".equals(JsonUtil.getValueByPath(extProps, "TASK_TYPE"))){
                if (!Status.hasFlagToday(CompletedKeyEnum.GameCenterTaskGameCenter.name())) {
                    boolean gameCenter = gameCenter();
                    if (gameCenter) {
                        Status.setFlagToday(CompletedKeyEnum.GameCenterTaskGameCenter.name());
                    }
                }
            }

            String taskId = task.getString("taskId");
            String subTitle = JsonUtil.getValueByPath(extProps, "TASK_MORPHO_DETAIL.subTitle");

            String stageCode = "NONE_SIGNUP".equals(status) ? "receive" : "send";
            String userCategory = "NONE_SIGNUP".equals(status) ? "toDayNewUser" : "tomorrowUser";

            String requestBody = String.format(
                    "\"pzConfig\":{\"name\":\"任务奖励\"},\"taskCamp\":{\"appletId\":\"%s\",\"stageCode\":\"%s\",\"taskCenId\":\"%s\"},\"userCategory\":\"%s\"",
                    taskId, stageCode, taskCenterId, userCategory);

            JSONObject triggerResponse = requestString(
                    "com.alipay.pcreditcardweb.activity.LuckCard.taskTrigger", requestBody);

            if (triggerResponse != null) {
                Log.other(this.displayName + "完成任务[" + subTitle + "]");
                sleep();
            }
        }

        sleep();
    }

    //浏览15s游戏中心
    private boolean gameCenter() throws Exception {
        boolean isSuccess = false;
        JSONObject request = requestString("com.alipay.pcreditbfweb.sdk.task.trigger",
                "[{\"appletId\":\"AP13283002\"," +
                        "\"bizScene\":\"HAOYUNKA_DAILY\",\"bizSceneFrom\":\"creditCard\",\"extInfo\":{\"name\":\"任务奖励\"," +
                        "\"ticketCampId\":\"CP182329534\"},\"needleParam\":{},\"outBizNo\":\"AP1328300229106083\"," +
                        "\"pccpId\":\"PCCP_2024120204226387059\",\"retryFlag\":true,\"stageCode\":\"send\"," +
                        "\"taskCentId\":\"AP16247630\",\"taskType\":\"WAITING_TIME\",\"triggerRule\":\"TASK_LUCKY_TICKET_TRIGGER\"}]");
        if (request == null) {
            Log.error(displayName+"debug--"+request);
            return false;
        }
        int errorCode = request.optInt("error");
        if (errorCode == 1009 || errorCode == 48 || errorCode == 6004) {
            Log.other(displayName + "错误 error --原因: " +request);
        }
        if (request.getBoolean("success")){
            Log.other(displayName + "完成[浏览15s游戏中心]");
            isSuccess = true;
        }
        return isSuccess;
    }
    /**
     * 统一延时方法
     */
    private void sleep() {
        TimeUtil.sleep((long) this.executeIntervalInt);
    }
}
