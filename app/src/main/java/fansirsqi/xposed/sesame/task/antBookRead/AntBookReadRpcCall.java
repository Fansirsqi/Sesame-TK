package fansirsqi.xposed.sesame.task.antBookRead;
import fansirsqi.xposed.sesame.hook.RequestManager;
import fansirsqi.xposed.sesame.util.RandomUtil;
/**
 * @author cwj851
 * @since 2024/01/18
 */
public class AntBookReadRpcCall {
    private static final String VERSION = "1.5.0";
    /* 读书 */
    public static String queryTaskCenterPage() {
        return RequestManager.requestString("com.alipay.antbookpromo.taskcenter.queryTaskCenterPage",
                "[{\"bannerId\":\"\",\"chInfo\":\"ch_alipaysearch__chsub_normal\",\"hasAddHome\":false,\"miniClientVersion\":\"1.0.7\",\"queryAction\":\"\",\"supportFeatures\":[\"prize_task_20230831\",\"auto_sign_in_20231211\",\"podcast_version_20240801\"],\"yuyanVersion\":\""
                        +VERSION+"\"}]");
    }
    public static String queryMiniTaskCenterInfo() {
        return RequestManager.requestString("com.alipay.antbookpromo.minitaskcenter.queryMiniTaskCenterInfo",
                "[{\"chInfo\":\"ch_appcenter__chsub_9patch\",\"hasAddHome\":false,\"isFromSync\":false,\"miniClientVersion\":\"1.0.0\",\"needInfos\":\"\",\"yuyanVersion\":\""
                        + VERSION + "\"}]");
    }
    public static String syncUserReadInfo(String bookId, String chapterId) {
        int readCount = RandomUtil.nextInt(40, 200);
        int readTime = RandomUtil.nextInt(160, 170) * 10000;
        return RequestManager.requestString("com.alipay.antbookread.biz.mgw.syncUserReadInfo",
                "[{\"bookId\":\"" + bookId
                        + "\",\"chInfo\":\"ch_appcenter__chsub_9patch\",\"chapterId\":\""
                        + chapterId
                        + "\",\"miniClientVersion\":\"1.0.0\",\"readCount\":" + readCount
                        + ",\"readTime\":" + readTime
                        + ",\"timeStamp\":" + System.currentTimeMillis()
                        + ",\"volumeId\":\"\",\"yuyanVersion\":\""
                        + VERSION + "\"}]");
    }
    public static String queryReaderForestEnergyInfo(String bookId) {
        return RequestManager.requestString("com.alipay.antbookread.biz.mgw.queryReaderForestEnergyInfo",
                "[{\"bookId\":\"" + bookId
                        + "\",\"chInfo\":\"ch_appcenter__chsub_9patch\",\"miniClientVersion\":\"1.0.0\",\"yuyanVersion\":\""
                        + VERSION + "\"}]");
    }
    public static String queryHomePage() {
        return RequestManager.requestString("com.alipay.antbookread.biz.mgw.queryHomePage",
                "[{\"chInfo\":\"ch_appcenter__chsub_9patch\",\"miniClientVersion\":\"1.0.0\",\"yuyanVersion\":\""
                        + VERSION + "\"}]");
    }
    public static String queryBookCatalogueInfo(String bookId) {
        return RequestManager.requestString("com.alipay.antbookread.biz.mgw.queryBookCatalogueInfo",
                "[{\"bookId\":\"" + bookId
                        + "\",\"chInfo\":\"ch_appcenter__chsub_9patch\",\"isInit\":true,\"miniClientVersion\":\"1.0.0\",\"order\":1,\"yuyanVersion\":\""
                        + VERSION + "\"}]");
    }
    public static String queryReaderContent(String bookId) {
        return RequestManager.requestString("com.alipay.antbookread.biz.mgw.queryReaderContent",
                "[{\"bookId\":\"" + bookId
                        + "\",\"chInfo\":\"ch_appcenter__chsub_9patch\",\"isInit\":true,\"miniClientVersion\":\"1.0.0\",\"queryRecommend\":false,\"yuyanVersion\":\""
                        + VERSION + "\"}]");
    }
    /* 任务 */
    public static String queryTreasureBox() {
        return RequestManager.requestString("com.alipay.antbookpromo.taskcenter.queryTreasureBox",
                "[{\"chInfo\":\"ch_appcenter__chsub_9patch\",\"miniClientVersion\":\"1.0.0\",\"yuyanVersion\":\"1.0.1397\"}]");
    }
    public static String taskFinish(String taskId, String taskType) {
        return RequestManager.requestString("com.alipay.antbookpromo.taskcenter.taskFinish",
                "[{\"chInfo\":\"ch_appcenter__chsub_9patch\",\"miniClientVersion\":\"1.0.0\",\"taskId\":\""
                        + taskId
                        + "\",\"taskType\":\"" + taskType
                        + "\",\"yuyanVersion\":\"1.0.1397\"}]");
    }
    public static String collectTaskPrize(String taskId, String taskType) {
        return RequestManager.requestString("com.alipay.antbookpromo.taskcenter.collectTaskPrize",
                "[{\"chInfo\":\"ch_appcenter__chsub_9patch\",\"miniClientVersion\":\"1.0.0\",\"taskId\":\""
                        + taskId
                        + "\",\"taskType\":\"" + taskType
                        + "\",\"yuyanVersion\":\"1.0.1397\"}]");
    }
    public static String queryApplayer() {
        return RequestManager.requestString("com.alipay.adtask.biz.mobilegw.service.applayer.query",
                "[{\"spaceCode\":\"adPosId#2023112024200071171##sceneCode#null##mediaScene#42##rewardNum#1##spaceCode#READ_LISTEN_BOOK_TREASURE_FEEDS_FUSION##expCode#\"}]");
    }
    public static String serviceTaskFinish(String bizId) {
        return RequestManager.requestString("com.alipay.adtask.biz.mobilegw.service.task.finish",
                "[{\"bizId\":\"" + bizId + "\"}]");
    }
    public static String serviceTaskQuery(String bizId) {
        return RequestManager.requestString("com.alipay.adtask.biz.mobilegw.service.task.query",
                "[{\"bizId\":\"" + bizId + "\"}]");
    }
    public static String openTreasureBox() {
        return RequestManager.requestString("com.alipay.antbookpromo.taskcenter.openTreasureBox",
                "[{\"chInfo\":\"ch_appcenter__chsub_9patch\",\"miniClientVersion\":\"1.0.0\",\"yuyanVersion\":\"1.0.1397\"}]");
    }
    /* 听书 */
    public static String queryEveningForestMainPage() {
        return RequestManager.requestString("com.alipay.antbooks.biz.mgw.queryEveningForestMainPage",
                "[{\"chInfo\":\"sy_wanansenlin_shouye\",\"miniClientVersion\":\"1.0.0\",\"yuyanVersion\":\"1.0.1397\"}]");
    }
    public static String queryAlbumDetailPage(String albumId) {
        return RequestManager.requestString("com.alipay.antbooks.biz.mgw.queryAlbumDetailPage",
                "[{\"albumId\":" + albumId
                        + ",\"chInfo\":\"sy_wanansenlin_shouye\",\"miniClientVersion\":\"1.0.0\",\"yuyanVersion\":\"1.0.1397\"}]");
    }
    public static String querySoundUrl(String albumId, String soundId) {
        return RequestManager.requestString("com.alipay.antbooks.biz.mgw.querySoundUrl",
                "[{\"albumId\":" + albumId
                        + ",\"chInfo\":\"sy_wanansenlin_shouye\",\"miniClientVersion\":\"1.0.0\",\"sceneId\":\"EVENING_FOREST\",\"soundId\":"
                        + soundId + ",\"yuyanVersion\":\"1.0.1397\"}]");
    }
    public static String syncUserPlayData(String albumId, String soundId) {
        return RequestManager.requestString("com.alipay.antbooks.biz.mgw.syncUserPlayData",
                "[{\"chInfo\":\"sy_wanansenlin_shouye\",\"miniClientVersion\":\"1.0.0\",\"syncingPlayRecordRequestList\":[{\"albumId\":"
                        + albumId + ",\"position\":720,\"soundId\":" + soundId
                        + ",\"timestamp\":"
                        + System.currentTimeMillis() + "}],\"yuyanVersion\":\"1.0.1397\"}]");
    }
    public static String queryPlayPage(String albumId, String soundId) {
        return RequestManager.requestString("com.alipay.antbooks.biz.mgw.queryPlayPage",
                "[{\"albumId\":" + albumId
                        + ",\"chInfo\":\"sy_wanansenlin_shouye\",\"miniClientVersion\":\"1.0.0\",\"sceneId\":\"EVENING_FOREST\",\"soundId\":"
                        + soundId + ",\"yuyanVersion\":\"1.0.1397\"}]");
    }
}