package fansirsqi.xposed.sesame.task.reserve;
import fansirsqi.xposed.sesame.hook.RequestManager;
public class ReserveRpcCall {
    private static final String VERSION = "20230501";
    private static final String VERSION2 = "20230522";
    public static String queryTreeItemsForExchange() {
        return RequestManager.requestString("alipay.antforest.forest.h5.queryTreeItemsForExchange",
                "[{\"cityCode\":\"370100\",\"itemTypes\":\"\",\"source\":\"chInfo_ch_appcenter__chsub_9patch\",\"version\":\""
                        + VERSION2 + "\"}]");
    }
    public static String queryTreeForExchange(String projectId) {
        return RequestManager.requestString("alipay.antforest.forest.h5.queryTreeForExchange",
                "[{\"projectId\":\"" + projectId + "\",\"version\":\"" + VERSION
                        + "\",\"source\":\"chInfo_ch_appcenter__chsub_9patch\"}]");
    }
    public static String exchangeTree(String projectId) {
        int projectId_num = Integer.parseInt(projectId);
        return RequestManager.requestString("alipay.antmember.forest.h5.exchangeTree",
                "[{\"projectId\":" + projectId_num + ",\"sToken\":\"" + System.currentTimeMillis() + "\",\"version\":\""
                        + VERSION + "\",\"source\":\"chInfo_ch_appcenter__chsub_9patch\"}]");
    }
    /* 查询地图树苗 */
    public static String queryAreaTrees() {
        return RequestManager.requestString("alipay.antmember.forest.h5.queryAreaTrees", "[{}]");
    }
    public static String queryTreeItemsForExchange(String applyActions, String itemTypes) {
        String args = "[{\"applyActions\":\"" + applyActions + "\",\"itemTypes\":\"" + itemTypes + "\"}]";
        return RequestManager.requestString("alipay.antforest.forest.h5.queryTreeItemsForExchange", args);
    }
    public static String queryPost(String method,String data){
        return RequestManager.requestString(method, data);
    }
}
