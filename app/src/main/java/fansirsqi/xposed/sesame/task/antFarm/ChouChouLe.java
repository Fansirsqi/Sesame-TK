package fansirsqi.xposed.sesame.task.antFarm;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import fansirsqi.xposed.sesame.util.GlobalThreadPools;
import fansirsqi.xposed.sesame.util.Log;
import fansirsqi.xposed.sesame.util.Maps.UserMap;
import fansirsqi.xposed.sesame.util.ResUtil;

public class ChouChouLe {
    private static final String TAG = ChouChouLe.class.getSimpleName();

    public enum TaskStatus {
        TODO, FINISHED, RECEIVED, DONATION
    }

    // 定义任务结构体
    private static class TaskInfo {
        String taskStatus;
        String title;
        String taskId;
        String innerAction;
        int rightsTimes;
        int rightsTimesLimit;

        int getRemainingTimes() {
            return Math.max(0, rightsTimesLimit - rightsTimes);
        }
    }

    void chouchoule() {
        try {
            String response = AntFarmRpcCall.queryLoveCabin(UserMap.getCurrentUid());
            JSONObject jo = new JSONObject(response);
            if (!"SUCCESS".equals(jo.optString("memo"))) {
                return;
            }

            JSONObject drawMachineInfo = jo.optJSONObject("drawMachineInfo");
            if (drawMachineInfo == null) {
                Log.farm("抽抽乐🎁[获取抽抽乐活动信息失败]");
                return;
            }

            if (drawMachineInfo.has("dailyDrawMachineActivityId")) {
                doChouchoule("dailyDraw");
            }
            if (drawMachineInfo.has("ipDrawMachineActivityId")) {
                doChouchoule("ipDraw");
            }

        } catch (Throwable t) {
            Log.printStackTrace("chouchoule err:", t);
        }
    }

    private void doChouchoule(String drawType) {
        boolean doubleCheck;
        do {
            doubleCheck = false;
            try {
                JSONObject jo = new JSONObject(AntFarmRpcCall.chouchouleListFarmTask(drawType));
                if (!ResUtil.checkResultCode(TAG, jo)) {
                    Log.record(TAG, drawType.equals("ipDraw") ? "IP抽抽乐任务列表获取失败" : "抽抽乐任务列表获取失败");
                    continue;
                }

                JSONArray farmTaskList = jo.getJSONArray("farmTaskList");
                List<TaskInfo> tasks = parseTasks(farmTaskList);

                for (TaskInfo task : tasks) {
                    GlobalThreadPools.sleep(300L);
                    if (TaskStatus.FINISHED.name().equals(task.taskStatus)) {
                        if (receiveTaskAward(drawType, task.taskId)) {
                            doubleCheck = true;
                        }
                        if (task.getRemainingTimes() > 0) {
                            doChouTask(drawType, task);
                        }
                    } else if (TaskStatus.TODO.name().equals(task.taskStatus)) {
                        if (doChouTask(drawType, task)) {
                            doubleCheck = true;
                        }
                    }
                }

            } catch (Throwable t) {
                Log.printStackTrace("doChouchoule err:", t);
            }
        } while (doubleCheck);

        if ("ipDraw".equals(drawType)) {
            handleIpDraw();
        } else {
            handleDailyDraw();
        }
    }

    private List<TaskInfo> parseTasks(JSONArray array) throws Exception {
        List<TaskInfo> list = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            JSONObject item = array.getJSONObject(i);
            TaskInfo info = new TaskInfo();
            info.taskStatus = item.getString("taskStatus");
            info.title = item.getString("title");
            info.taskId = item.getString("bizKey");
            info.innerAction = item.optString("innerAction");
            info.rightsTimes = item.optInt("rightsTimes", 0);
            info.rightsTimesLimit = item.optInt("rightsTimesLimit", 0);
            list.add(info);
        }
        return list;
    }

    private boolean doChouTask(String drawType, TaskInfo task) {
        try {
            GlobalThreadPools.sleep(800L);
            String s = AntFarmRpcCall.chouchouleDoFarmTask(drawType, task.taskId);
            JSONObject jo = new JSONObject(s);
            if (ResUtil.checkResultCode(TAG, jo)) {
                Log.farm((drawType.equals("ipDraw") ? "IP抽抽乐" : "抽抽乐") + "🧾️[任务: " + task.title + "]");
                GlobalThreadPools.sleep(100L);
                receiveTaskAward(drawType, task.taskId);
                return true;
            }
        } catch (Throwable t) {
            Log.printStackTrace("执行抽抽乐任务 err:", t);
        }
        return false;
    }

    /**
     * 领取任务奖励
     *
     * @param drawType "dailyDraw" or "ipDraw" 普通装扮或者IP装扮
     * @param taskId   任务ID
     * @return 是否领取成功
     */
    private boolean receiveTaskAward(String drawType, String taskId) {
        try {
            String s = AntFarmRpcCall.chouchouleReceiveFarmTaskAward(drawType, taskId);
            JSONObject jo = new JSONObject(s);
            return ResUtil.checkResultCode(TAG, jo);
        } catch (Throwable t) {
            Log.printStackTrace("receiveFarmTaskAward err:", t);
        }
        return false;
    }

    /**
     * 执行IP抽抽乐
     */
    private void handleIpDraw() {
        try {
            JSONObject jo = new JSONObject(AntFarmRpcCall.queryDrawMachineActivity());
            if (!ResUtil.checkResultCode(TAG, jo)) {
                return;
            }

            JSONObject activity = jo.getJSONObject("drawMachineActivity");
            long endTime = activity.getLong("endTime");
            if (System.currentTimeMillis() > endTime) {
                Log.record(TAG, "该[" + activity.optString("activityId") + "]抽奖活动已结束");
                return;
            }

            int drawTimes = jo.optInt("drawTimes", 0);
            for (int i = 0; i < drawTimes; i++) {
                drawPrize("IP抽抽乐", AntFarmRpcCall.drawMachine());
                GlobalThreadPools.sleep(1500L);
            }

        } catch (Throwable t) {
            Log.printStackTrace("handleIpDraw err:", t);
        }
    }

    /**
     * 执行正常抽抽乐
     */
    private void handleDailyDraw() {
        try {
            JSONObject jo = new JSONObject(AntFarmRpcCall.enterDrawMachine());
            if (!ResUtil.checkResultCode(TAG, jo)) {
                Log.record(TAG, "抽奖活动进入失败");
                return;
            }

            JSONObject userInfo = jo.getJSONObject("userInfo");
            JSONObject drawActivityInfo = jo.getJSONObject("drawActivityInfo");
            long endTime = drawActivityInfo.getLong("endTime");
            if (System.currentTimeMillis() > endTime) {
                Log.record(TAG, "该[" + drawActivityInfo.optString("activityId") + "]抽奖活动已结束");
                return;
            }

            int leftDrawTimes = userInfo.optInt("leftDrawTimes", 0);
            String activityId = drawActivityInfo.optString("activityId");

            for (int i = 0; i < leftDrawTimes; i++) {
                String call = activityId.equals("null") ? AntFarmRpcCall.DrawPrize() : AntFarmRpcCall.DrawPrize(activityId);
                drawPrize("抽抽乐", call);
                GlobalThreadPools.sleep(1500L);
            }

        } catch (Throwable t) {
            Log.printStackTrace("handleDailyDraw err:", t);
        }
    }

    /**
     * 领取抽抽乐奖品
     *
     * @param prefix   抽奖类型
     * @param response 服务器返回的结果
     */
    private void drawPrize(String prefix, String response) {
        try {
            JSONObject jo = new JSONObject(response);
            if (ResUtil.checkResultCode(TAG, jo)) {
                String title = jo.getString("title");
                int prizeNum = jo.optInt("prizeNum", 1);
                Log.farm(prefix + "🎁[领取: " + title + "*" + prizeNum + "]");
            }else {
                Log.record(TAG, prefix + "🎁[领取失败]");
            }
        } catch (Exception ignored) {
        }
    }

}