package fansirsqi.xposed.sesame.task.otherTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import fansirsqi.xposed.sesame.task.antOrchard.AntOrchardRpcCall;
import fansirsqi.xposed.sesame.util.Log;
import fansirsqi.xposed.sesame.util.ThreadUtil;


public class AntFarms extends BaseCommTask {
    private static String displayName = "芭芭农场任务🧾";

    @Override
    protected void handle() throws JSONException {
        try {
            doTask();
        } catch (JSONException e) {
            Log.other(displayName+"任务处理异常: " + e.getMessage());
        }
    }

    private void doTask() throws JSONException {
            JSONObject joo = new JSONObject(AntOrchardRpcCall.mowGrassInfo());
            String userId = joo.getString("userId");
            ThreadUtil.sleep(Math.max(15000, 20000)); // 睡眠
            doOrchardDailyTask(userId);
        }


    private void doOrchardDailyTask(String userId) {
        try {
            String s = AntOrchardRpcCall.orchardListTask();
            JSONObject jo = new JSONObject(s);
            if ("100".equals(jo.getString("resultCode"))) {
                JSONArray jaTaskList = jo.getJSONArray("taskList");
                for (int i = 0; i < jaTaskList.length(); i++) {
                    jo = jaTaskList.getJSONObject(i);
                    if (!"TODO".equals(jo.getString("taskStatus"))) continue;
                    String title = jo.getJSONObject("taskDisplayConfig").getString("title");
                    if ("TRIGGER".equals(jo.getString("actionType")) || "ADD_HOME".equals(jo.getString("actionType")) || "PUSH_SUBSCRIBE".equals(jo.getString("actionType"))) {
                        String taskId = jo.getString("taskId");
                        String sceneCode = jo.getString("sceneCode");
                        jo = new JSONObject(AntOrchardRpcCall.finishTask(userId, sceneCode, taskId));
                        if (jo.optBoolean("success")) {
                            Log.farm("芭芭农场任务🧾[" + title + "]");
                        } else {
                            Log.record(jo.getString("desc"));
                            Log.runtime(jo.toString());
                        }
                    }
                }
            } else {
                Log.record(jo.getString("resultCode"));
                Log.runtime(s);
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "doOrchardDailyTask err:");
            Log.printStackTrace(TAG, t);
        }
    }

}
