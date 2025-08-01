package fansirsqi.xposed.sesame.task.antFarm;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import fansirsqi.xposed.sesame.util.GlobalThreadPools;
import fansirsqi.xposed.sesame.util.Log;
import fansirsqi.xposed.sesame.util.maps.UserMap;
import fansirsqi.xposed.sesame.util.ResChecker;

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
            if (!ResChecker.checkRes(TAG, jo)) {
                return;
            }

            JSONObject drawMachineInfo = jo.optJSONObject("drawMachineInfo");
            if (drawMachineInfo == null) {
                Log.error(TAG, "抽抽乐🎁[获取抽抽乐活动信息失败]");
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

    /**
     * 执行抽抽乐
     *
     * @param drawType "dailyDraw" or "ipDraw" 普通装扮或者IP装扮
     */

    private void doChouchoule(String drawType) {
        boolean doubleCheck;
        do {
            doubleCheck = false;
            try {
                JSONObject jo = new JSONObject(AntFarmRpcCall.chouchouleListFarmTask(drawType));
                if (!ResChecker.checkRes(TAG, jo)) {
                    Log.error(TAG, drawType.equals("ipDraw") ? "IP抽抽乐任务列表获取失败" : "抽抽乐任务列表获取失败");
                    continue;
                }
                JSONArray farmTaskList = jo.getJSONArray("farmTaskList");//获取任务列表
                List<TaskInfo> tasks = parseTasks(farmTaskList);
                for (TaskInfo task : tasks) {
                    GlobalThreadPools.sleep(5 * 1000L);
                    if (TaskStatus.FINISHED.name().equals(task.taskStatus)) {
                        if (receiveTaskAward(drawType, task.taskId)) {//领取奖励
                            doubleCheck = true;
                        }
                    } else if (TaskStatus.TODO.name().equals(task.taskStatus)) {
                        if (task.getRemainingTimes() > 0) {
                            if (doChouTask(drawType, task)) {
                                doubleCheck = true;
                            }
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

    private Boolean doChouTask(String drawType, TaskInfo task) {
        try {
            String s = AntFarmRpcCall.chouchouleDoFarmTask(drawType, task.taskId);
            JSONObject jo = new JSONObject(s);
            if (ResChecker.checkRes(TAG, jo)) {
                Log.farm((drawType.equals("ipDraw") ? "IP抽抽乐" : "抽抽乐") + "🧾️[任务: " + task.title + "]");
                return true;
            }
            return false;
        } catch (Throwable t) {
            Log.printStackTrace("执行抽抽乐任务 err:", t);
            return false;
        }
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
            return ResChecker.checkRes(TAG, jo);
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
            if (!ResChecker.checkRes(TAG, jo)) {
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
                GlobalThreadPools.sleep(5 * 1000L);
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
            if (!ResChecker.checkRes(TAG, jo)) {
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
                GlobalThreadPools.sleep(5 * 1000L);
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
            if (ResChecker.checkRes(TAG, jo)) {
                String title = jo.getString("title");
                int prizeNum = jo.optInt("prizeNum", 1);
                Log.farm(prefix + "🎁[领取: " + title + "*" + prizeNum + "]");
            }
        } catch (Exception ignored) {
        }
    }

}