package fansirsqi.xposed.sesame.task.antFarm;

import fansirsqi.xposed.sesame.hook.RequestManager;

/**
 * @author Constanline
 * @since 2023/08/04
 */
public class DadaDailyRpcCall {
    public static String home(String activityId) {
        return RequestManager.requestString("com.alipay.reading.game.dadaDaily.home",
                "[{\"activityId\":" + activityId + ",\"dadaVersion\":\"1.3.0\",\"version\":1}]");
    }

    public static String submit(String activityId, String answer, Long questionId) {
        return RequestManager.requestString("com.alipay.reading.game.dadaDaily.submit",
                "[{\"activityId\":" + activityId + ",\"answer\":\"" + answer + "\",\"dadaVersion\":\"1.3.0\",\"questionId\":" + questionId + ",\"version\":1}]");
    }
}
