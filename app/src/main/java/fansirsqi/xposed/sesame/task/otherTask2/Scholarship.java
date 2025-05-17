package fansirsqi.xposed.sesame.task.otherTask2;

import org.json.JSONException;
import org.json.JSONObject;

import fansirsqi.xposed.sesame.data.Status;
import fansirsqi.xposed.sesame.hook.RequestManager;
import fansirsqi.xposed.sesame.task.otherTask.BaseCommTask;
import fansirsqi.xposed.sesame.util.Log;
import fansirsqi.xposed.sesame.util.RandomUtil;
import fansirsqi.xposed.sesame.util.TimeUtil;

/**
 * 蚂蚁投资者教育基地--奖学金
 */
public class Scholarship extends BaseCommTask {

    private static final String TAG = "Scholarship";
    @Override
    protected void handle() throws JSONException {
        for (int i = 0; i < 3; i++) {
            processTask();
            TimeUtil.sleep(RandomUtil.nextInt(3000, 5000));
        }

    }

    private void processTask() {
        String result = queryTask();
        TimeUtil.sleep(RandomUtil.nextInt(3000, 5000));
        if (result == null || result.isEmpty()) {
            return;
        }

        try {
            JSONObject res = new JSONObject(result);
            if (res == null || !res.optBoolean("success")) {
                Log.other(TAG + "接口返回失败：" + res.optString("message", "未知错误"));
                return;
            }

            // 安全获取 data 对象
            JSONObject data = res.optJSONObject("data");
            if (data == null) {
                Log.other(TAG + "data 为 null，无法继续执行,响应:"+res);
                return;
            }
            // 安全获取 userInfo 对象
            JSONObject userInfo = data.optJSONObject("userInfo");
            if (userInfo!=null){
                String status = userInfo.optString("status","");
                if (!status.isEmpty() && "FREE".equals(status)){
                }else {
                    Log.other(TAG + "今日已经完成");
                    Status.setFlagToday("ScholarshipTask");
                }
            }
            // 安全获取 prizeInfo 对象
            JSONObject prizeInfo = data.optJSONObject("prizeInfo");
            if (prizeInfo == null) {
                Log.other(TAG + "prizeInfo 为 null，无任务可执行");
                return;
            }

            // 安全获取 drawResult 对象
            JSONObject drawResult = prizeInfo.optJSONObject("drawResult");
            if (drawResult == null) {
                Log.other(TAG + "drawResult 为 null，无法获取任务信息");
                return;
            }

            // 安全获取 taskMorphoDetail 对象
            JSONObject taskMorphoDetail = drawResult.optJSONObject("taskMorphoDetail");
            if (taskMorphoDetail == null) {
                Log.other(TAG + "taskMorphoDetail 为 null，无任务详情");
                return;
            }

            // 安全获取 taskId 和 title
            String taskId = taskMorphoDetail.optString("taskId");
            String title = taskMorphoDetail.optString("title");
            String count = taskMorphoDetail.optString("count");

            if (taskId == null || taskId.isEmpty()) {
                Log.other(TAG + "❌ 无 taskId");
                return;
            }

            String s = doTask(taskId);
            JSONObject s2 = new JSONObject(s);
            if (s2.optBoolean("success")) {
                Log.other(TAG + "完成[" + title + "]✅获得["+count+"]奖学金");
            } else {
                Log.other(TAG + "完成[" + title + "]❌");
            }

        } catch (JSONException e) {
            Log.printStackTrace(TAG, e);
        }
    }



    private String doTask(String taskId){
        String params = "[{\"appletId\":\"AP16171913\",\"stageCode\":\"send\",\"taskId\":\""+taskId+"\"}]";
        return RequestManager.requestString("com.alipay.promobffweb.needle.equity.triggerTask",params);
    }

    private String queryTask(){
        String params = "[{\"jsonArgs\":{\"extInfo\":{\"mode\":\"PURE\",\"source\":\"\"},\"sceneCode\":\"EDUCATION_LUCKYBOX\"},\"methodId\":\"trigger\",\"source\":\"FORTUNE\"}]";
        return RequestManager.requestString("com.alipay.promobffweb.needle.wiki.invokeGzoneReact",params);
    }
}
