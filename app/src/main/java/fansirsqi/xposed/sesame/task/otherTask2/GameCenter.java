package fansirsqi.xposed.sesame.task.otherTask2;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import fansirsqi.xposed.sesame.data.Status;
import fansirsqi.xposed.sesame.hook.RequestManager;
import fansirsqi.xposed.sesame.task.otherTask.BaseCommTask;
import fansirsqi.xposed.sesame.task.otherTask.CompletedKeyEnum;
import fansirsqi.xposed.sesame.util.Log;
import fansirsqi.xposed.sesame.util.TimeUtil;

public class GameCenter extends BaseCommTask {
    private static final String displayName = "游戏中心💼";
    @Override
    protected void handle() throws JSONException {
        //查询任务列表
        String s = queryTaskList();
        if (s == null || s.isEmpty()){
            Log.other(displayName+"查询任务列表失败");
            return;
        }
        try{
            JSONObject json = new JSONObject(s);
            if (json==null||!json.optBoolean("success")){
                return;
            }
            JSONObject data = json.getJSONObject("data");
            JSONArray taskModuleList = data.getJSONArray("taskModuleList");
            for (int i = 0; i < taskModuleList.length(); i++) {
                JSONObject taskModule = taskModuleList.getJSONObject(i);
                JSONArray taskList = taskModule.getJSONArray("taskList");
                for (int j = 0; j < taskList.length(); j++) {
                    JSONObject task = taskList.getJSONObject(j);
                    String actionType = task.optString("actionType", "未知任务类型");
                    if (actionType.equals("VIEW")){
                        String taskId = task.optString("taskId");
                        String subTitle = task.optString("subTitle");
                        Integer prizeAmount = task.optInt("prizeAmount",0);
                        signUp(taskId);
                        TimeUtil.sleep(15000);
                        doTask(taskId);
                        Log.other(displayName+"["+subTitle+"]任务完成,获得["+prizeAmount+"]玩乐豆");
                    }
                }
            }

            //收取玩乐豆
            try {
                String received = receiveTask();
                if (received == null || received.isEmpty()) {
                    return;
                }
                JSONObject reData = new JSONObject(received);
                if (reData.getBoolean("success")) {
                    JSONObject ReData = reData.getJSONObject("data");
                    String totalAmount = ReData.getString("totalAmount");
                    Log.other(displayName+"收取["+totalAmount+"]玩乐豆");
                    Status.setFlagToday(CompletedKeyEnum.GameCenterTask.name());
                }
            }  catch (Exception e) {
                Log.other(displayName+"收取任务异常: " + e.getMessage());
            }
        }catch (Exception e){
            Log.other(displayName+"任务处理异常: " + e.getMessage());
        }
    }

    private String queryTaskList() {
        return RequestManager.requestString("com.alipay.gamecenteruprod.biz.rpc.v3.queryModularTaskList",
                "[{\"deviceLevel\":\"high\",\"source\":\"ch_appid-20001003__chsub_pageid-com.alipay.android.phone.businesscommon.globalsearch.ui.MainSearchActivity\",\"sourceTab\":\"luckydraw\",\"unityDeviceLevel\":\"high\"}]");
    }

    private String signUp(String taskId){
        return RequestManager.requestString("com.alipay.gamecenteruprod.biz.rpc.v3.doTaskSignup",
                "[{\"source\":\"ch_appid-20001003__chsub_pageid-com.alipay.android.phone.businesscommon.globalsearch.ui.MainSearchActivity\"," +
                        "\"taskId\":\""+taskId+"\"}]");
    }

    private String doTask(String taskId){
        return RequestManager.requestString("com.alipay.gamecenteruprod.biz.rpc.v3.doTaskSend",
                "[{\"taskId\":\""+taskId+"\"}]");
    }

    private String receiveTask(){
        return RequestManager.requestString("com.alipay.gamecenteruprod.biz.rpc.v3.batchReceivePointBall",
                "[{}]"
        );
    }
}
