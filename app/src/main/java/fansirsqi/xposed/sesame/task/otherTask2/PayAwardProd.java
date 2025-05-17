package fansirsqi.xposed.sesame.task.otherTask2;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import fansirsqi.xposed.sesame.data.Status;
import fansirsqi.xposed.sesame.hook.RequestManager;
import fansirsqi.xposed.sesame.task.otherTask.BaseCommTask;
import fansirsqi.xposed.sesame.task.otherTask.CompletedKeyEnum;
import fansirsqi.xposed.sesame.util.Log;

public class PayAwardProd extends BaseCommTask {
    private static final String displayName = "支付有优惠红包💵";
    @Override
    protected void handle() throws JSONException {
        Log.other(displayName+"开始执行");
        doTask();
        Log.other(displayName + "任务完成");
    }

    private void doTask() {
        String taskList = queryTaskList();
        if (taskList!=null || !taskList.isEmpty()){
            Log.other(displayName+"查询任务列表为空");
            return;
        }
        try {
            Log.other(displayName+"查询任务列表并执行任务");
            JSONObject listJson = new JSONObject(taskList);
            JSONObject dataTaskList = listJson.getJSONObject("data");
            JSONObject payAwardTaskModel = dataTaskList.getJSONObject("payAwardTaskModel");
            JSONArray taskLists = payAwardTaskModel.getJSONArray("taskList");
            for (int i = 0; i < taskLists.length(); i++){
                JSONObject task = taskLists.getJSONObject(i);
                String appletId = task.getString("taskId");
                String taskCenId = task.getString("taskCenId");
                String taskStatus = task.getString("taskStatus");
                JSONObject taskExtProps = task.getJSONObject("taskExtProps");
                String taskType = taskExtProps.getString("TASK_TYPE");

                //只做浏览任务
                if(!"BROWSER".equals(taskType)){
                    return;
                }
                //只做未完成的任务
                if (!"NOT_DONE".equals(taskStatus)){
                    return;
                }
                //完成任务
                String sub = subTask(appletId, taskCenId);
                if (sub!=null || !sub.isEmpty()) {
                    try {
                        JSONObject jsonSub = new JSONObject(sub);
                        if (jsonSub.getBoolean("success")) {
                            JSONObject appletBaseConfigDTO = jsonSub.getJSONObject("appletBaseConfigDTO");
                            String appletName = appletBaseConfigDTO.getString("appletName");
                            Log.other(displayName + "完成任务[" + appletName + "]");
                            //领取奖励
                            String receiveTask = receiveTask(appletId, taskCenId);
                            if (receiveTask != null || !receiveTask.isEmpty()) {
                                try {
                                    JSONObject jsonReceiveTask = new JSONObject(receiveTask);
                                    if (jsonReceiveTask.getBoolean("success")) {
                                        Log.other(displayName + "领取奖励成功");
                                    } else {
                                        Log.other(displayName + "领取奖励失败:" + jsonReceiveTask.optString("resultMsg"));
                                    }
                                } catch (JSONException e) {
                                    Log.other(displayName + "领取奖励错误");
                                }
                            }
                        }
                    } catch (JSONException e) {
                        Log.other(displayName + "完成任务错误");
                    }
                }
            }
            //设置任务完成
            Status.setFlagToday(CompletedKeyEnum.PayAwardProd.name());
        } catch (JSONException e) {
            Log.other(displayName+"查询任务列表错误");
        }




    }

    private String subTask(String appletId,String taskCenId) {
        String method = "alipay.promoprod.applet.trigger";
        String params = "[{\"appletId\":\""+appletId+"\",\"browseTime\":\"15\",\"spmParams\":" +
                "{\"taskCenId\":\""+taskCenId+"\",\"taskId\":\""+appletId+"\",\"taskStatus\":\"NOT_DONE\"}," +
                "\"stageCode\":\"send\",\"taskCenId\":\""+taskCenId+"\"}]";
        return  RequestManager.requestString(method, params);
    }
    private String receiveTask(String appletId,String taskCenId){
        String method = "alipay.promoprod.applet.trigger";
        String params = "[{\"appletId\":\""+appletId+"\",\"extInfo\":{\"bundleVersion\":\"1\"," +
                "\"meetingBenefitBundleVersion\":\"3\"},\"stageCode\":\"receive\",\"taskCenId\":\""+taskCenId+"\"}]";
        return RequestManager.requestString(method, params);
    }

    private String queryTaskList() {
        String method = "alipay.ofpgrowth.payawardprod.home";
        String params = "[{\"extInfo\":{\"bundleVersion\":\"1\",\"meetingBenefitBundleVersion\":\"3\"},\"sign\":\"eyJzb3VyY2UiOiJkZWZhdWx0In0=\"}]";
        return RequestManager.requestString(method, params);
    }


}
