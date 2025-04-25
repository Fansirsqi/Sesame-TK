package fansirsqi.xposed.sesame.task.antForest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.UUID;

import fansirsqi.xposed.sesame.entity.AlipayVersion;
import fansirsqi.xposed.sesame.entity.RpcEntity;
import fansirsqi.xposed.sesame.hook.ApplicationHook;
import fansirsqi.xposed.sesame.hook.RequestManager;
import fansirsqi.xposed.sesame.util.RandomUtil;
import fansirsqi.xposed.sesame.util.StringUtil;

/**
 * 森林 RPC 调用类
 */
public class AntForestRpcCall {
    private static String VERSION = "";

    public static void init() {
        AlipayVersion alipayVersion = ApplicationHook.getAlipayVersion();
        if (alipayVersion.compareTo(new AlipayVersion("10.5.88.8000")) > 0) {
            VERSION = "20240403";
        } else if (alipayVersion.compareTo(new AlipayVersion("10.3.96.8100")) > 0) {
            VERSION = "20230501";
        } else {
            VERSION = "20230501";
        }
    }

    private static String getUniqueId() {
        return String.valueOf(System.currentTimeMillis()) + RandomUtil.nextLong();
    }

    public static String queryEnergyRanking() {
        return RequestManager.requestString(
                "alipay.antmember.forest.h5.queryEnergyRanking",
                "[{\"periodType\":\"total\",\"rankType\":\"energyRank\",\"source\":\"chInfo_ch_appcenter__chsub_9patch\",\"version\":\"" + VERSION + "\"}]",
                "{\"pathList\":[\"friendRanking\",\"myself\",\"totalDatas\"]}");
    }

    public static String fillUserRobFlag(String userIdList) {
        return RequestManager.requestString("alipay.antforest.forest.h5.fillUserRobFlag", "[{\"userIdList\":" + userIdList + "}]", "{\"pathList" +
                "\":[\"friendRanking\"]}");
    }

    public static String queryHomePage() throws JSONException {
        JSONObject requestObject = new JSONObject()
                .put("activityParam", new JSONObject())
                .put("configVersionMap", new JSONObject().put("wateringBubbleConfig", "0"))
                .put("skipWhackMole", false)
                .put("source", "chInfo_ch_appcenter__chsub_9patch")
                .put("version", VERSION);
        return RequestManager.requestString(
                "alipay.antforest.forest.h5.queryHomePage",
                new JSONArray().put(requestObject).toString(),
                3,
                1000
        );
    }

    public static String queryFriendHomePage(String userId) {
        return RequestManager.requestString(
                "alipay.antforest.forest.h5.queryFriendHomePage",
                "[{\"canRobFlags\":\"F,F,F,F,F\",\"configVersionMap\":{\"redPacketConfig\":0,\"wateringBubbleConfig\":\"10\"}," +
                        "\"source\":\"chInfo_ch_appcenter__chsub_9patch\",\"userId\":\""
                        + userId
                        + "\",\"version\":\""
                        + VERSION
                        + "\"}]",
                3,
                1000);
    }

    public static RpcEntity getCollectEnergyRpcEntity(String bizType, String userId, long bubbleId) {
        String args1;
        if (StringUtil.isEmpty(bizType)) {
            args1 =
                    "[{\"bizType\":\"\",\"bubbleIds\":[" + bubbleId + "],\"source\":\"chInfo_ch_appcenter__chsub_9patch\",\"userId\":\"" + userId + "\"," +
                            "\"version\":\"" + VERSION + "\"}]";
        } else {
            args1 = "[{\"bizType\":\"" + bizType + "\",\"bubbleIds\":[" + bubbleId + "],\"source\":\"chInfo_ch_appcenter__chsub_9patch\",\"userId\":\"" + userId + "\"}]";
        }
        return new RpcEntity("alipay.antmember.forest.h5.collectEnergy", args1, null);
    }

    public static String collectEnergy(String bizType, String userId, Long bubbleId) {
        return RequestManager.requestString(getCollectEnergyRpcEntity(bizType, userId, bubbleId));
    }

    public static RpcEntity getCollectBatchEnergyRpcEntity(String userId, List<Long> bubbleIdList) {
        return getCollectBatchEnergyRpcEntity(userId, StringUtil.collectionJoinString(",", bubbleIdList));
    }

    public static RpcEntity getCollectBatchEnergyRpcEntity(String userId, String bubbleIds) {
        return new RpcEntity(
                "alipay.antmember.forest.h5.collectEnergy",
                "[{\"bizType\":\"\",\"bubbleIds\":["
                        + bubbleIds
                        + "],\"fromAct\":\"BATCH_ROB_ENERGY\",\"source\":\"chInfo_ch_appcenter__chsub_9patch\",\"userId\":\""
                        + userId
                        + "\",\"version\":\""
                        + VERSION
                        + "\"}]");
    }

    public static String collectBatchEnergy(String userId, List<Long> bubbleId) {
        return RequestManager.requestString(getCollectBatchEnergyRpcEntity(userId, bubbleId));
    }

    public static String collectRebornEnergy() {
        return RequestManager.requestString("alipay.antforest.forest.h5.collectRebornEnergy", "[{\"source\":\"chInfo_ch_appcenter__chsub_9patch\"}]");
    }

    public static String transferEnergy(String targetUser, String bizNo, int energyId) {
        return RequestManager.requestString(
                "alipay.antmember.forest.h5.transferEnergy",
                "[{\"bizNo\":\""
                        + bizNo
                        + UUID.randomUUID().toString()
                        + "\",\"energyId\":"
                        + energyId
                        + ",\"extInfo\":{\"sendChat\":\"N\"},\"from\":\"friendIndex\",\"source\":\"chInfo_ch_appcenter__chsub_9patch\",\"targetUser\":\""
                        + targetUser
                        + "\",\"transferType\":\"WATERING\",\"version\":\""
                        + VERSION
                        + "\"}]");
    }

    public static String forFriendCollectEnergy(String targetUserId, long bubbleId) {
        String args1 = "[{\"bubbleIds\":[" + bubbleId + "],\"targetUserId\":\"" + targetUserId + "\"}]";
        return RequestManager.requestString("alipay.antmember.forest.h5.forFriendCollectEnergy", args1);
    }

    /**
     * 森林签到
     */
    public static String vitalitySign() {
        return RequestManager.requestString("alipay.antforest.forest.h5.vitalitySign", "[{\"source\":\"chInfo_ch_appcenter__chsub_9patch\"}]");
    }

    public static String queryEnergyRainHome() {
        return RequestManager.requestString("alipay.antforest.forest.h5.queryEnergyRainHome", "[{\"version\":\"" + VERSION + "\"}]");
    }

    public static String queryEnergyRainCanGrantList() {
        return RequestManager.requestString("alipay.antforest.forest.h5.queryEnergyRainCanGrantList", "[{}]");
    }

    public static String grantEnergyRainChance(String targetUserId) {
        return RequestManager.requestString("alipay.antforest.forest.h5.grantEnergyRainChance", "[{\"targetUserId\":" + targetUserId + "}]");
    }

    public static String startEnergyRain() {
        return RequestManager.requestString("alipay.antforest.forest.h5.startEnergyRain", "[{\"version\":\"" + VERSION + "\"}]");
    }

    public static String energyRainSettlement(int saveEnergy, String token) {
        return RequestManager.requestString(
                "alipay.antforest.forest.h5.energyRainSettlement",
                "[{\"activityPropNums\":0,\"saveEnergy\":" + saveEnergy + ",\"token\":\"" + token + "\",\"version\":\"" + VERSION + "\"}]");
    }

    public static String queryTaskList() throws JSONException {
        JSONObject jo = new JSONObject();
        jo.put("extend", new JSONObject());
        jo.put("fromAct", "home_task_list");
        jo.put("source", "chInfo_ch_appcenter__chsub_9patch");
        jo.put("version", VERSION);
        return RequestManager.requestString("alipay.antforest.forest.h5.queryTaskList", new JSONArray().put(jo).toString());
    }

    /*青春特权道具任务状态查询🔍*/
    public static String queryTaskListV2(String firstTaskType) throws JSONException {
        JSONObject jo = new JSONObject();
        JSONObject extend = new JSONObject();
        extend.put("firstTaskType", firstTaskType); // DNHZ_SL_college,DXS_BHZ，DXS_JSQ
        jo.put("extend", extend);
        jo.put("fromAct", "home_task_list");
        if (firstTaskType.equals("DNHZ_SL_college")) {
            jo.put("source", firstTaskType);
        }
        if (firstTaskType.equals("DXS_BHZ") || firstTaskType.equals("DXS_JSQ")) {
            jo.put("source", "202212TJBRW");
        }
        jo.put("version", VERSION);
        return RequestManager.requestString("alipay.antforest.forest.h5.queryTaskList", new JSONArray().put(jo).toString());
    }

    public static String receiveTaskAward(String sceneCode, String taskType) throws JSONException {
        JSONObject jo = new JSONObject();
        jo.put("ignoreLimit", false);
        jo.put("requestType", "H5");
        jo.put("sceneCode", sceneCode);
        jo.put("source", "ANTFOREST");
        jo.put("taskType", taskType);
        return RequestManager.requestString("com.alipay.antiep.receiveTaskAward", new JSONArray().put(jo).toString());
    }

    /**
     * 领取青春特权道具
     *
     * @param taskType DAXUESHENG_SJK,NENGLIANGZHAO_20230807,JIASUQI_20230808
     * @return 领取结果
     * @throws JSONException JSON 解析异常
     */
    public static String receiveTaskAwardV2(String taskType) throws JSONException {
        JSONObject jo = new JSONObject();
        jo.put("ignoreLimit", false);
        jo.put("requestType", "H5");
        jo.put("sceneCode", "ANTFOREST_VITALITY_TASK");
        jo.put("source", "ANTFOREST");
        jo.put("taskType", taskType); // DAXUESHENG_SJK,NENGLIANGZHAO_20230807,JIASUQI_20230808
        return RequestManager.requestString("com.alipay.antiep.receiveTaskAward", new JSONArray().put(jo).toString());
    }

    public static String finishTask(String sceneCode, String taskType) throws JSONException {
        String outBizNo = taskType + "_" + RandomUtil.nextDouble();
        JSONObject jo = new JSONObject();
        jo.put("outBizNo", outBizNo);
        jo.put("requestType", "H5");
        jo.put("sceneCode", sceneCode);
        jo.put("source", "ANTFOREST");
        jo.put("taskType", taskType);
        return RequestManager.requestString("com.alipay.antiep.finishTask", new JSONArray().put(jo).toString());
    }

    public static String popupTask() throws JSONException {
        // 创建用于构造 JSON 请求的对象
        JSONObject jo = new JSONObject();
        jo.put("fromAct", "pop_task");
        jo.put("needInitSign", false);
        jo.put("source", "chInfo_ch_appcenter__chsub_9patch");
        jo.put("statusList", new JSONArray().put("TODO").put("FINISHED"));
        jo.put("version", VERSION);
        // 将 JSON 对象转换为字符串请求
        return RequestManager.requestString("alipay.antforest.forest.h5.popupTask", new JSONArray().put(jo).toString());
    }

    public static String antiepSign(String entityId, String userId) throws JSONException {
        // 构造 JSON 对象
        JSONObject jo = new JSONObject();
        jo.put("entityId", entityId);
        jo.put("requestType", "rpc");
        jo.put("sceneCode", "ANTFOREST_ENERGY_SIGN");
        jo.put("source", "ANTFOREST");
        jo.put("userId", userId);
        // 调用请求
        return RequestManager.requestString("com.alipay.antiep.sign", new JSONArray().put(jo).toString());
    }

    public static String queryPropList(boolean onlyGive) throws JSONException {
        JSONObject jo = new JSONObject();
        jo.put("onlyGive", onlyGive ? "Y" : "");
        jo.put("source", "chInfo_ch_appcenter__chsub_9patch");
        jo.put("version", VERSION);
        return RequestManager.requestString("alipay.antforest.forest.h5.queryPropList", new JSONArray().put(jo).toString());
    }

    public static String queryAnimalPropList() throws JSONException {
        JSONObject jo = new JSONObject();
        jo.put("source", "chInfo_ch_appcenter__chsub_9patch");
        return RequestManager.requestString("alipay.antforest.forest.h5.queryAnimalPropList", new JSONArray().put(jo).toString());
    }

    public static String consumeProp(String propGroup, String propType, Boolean replace) throws JSONException {
        JSONObject jo = new JSONObject();
        jo.put("propGroup", propGroup);
        jo.put("propType", propType);
        jo.put("replace", replace.toString());
        jo.put("sToken", System.currentTimeMillis() + "_" + RandomUtil.getRandomString(8));
        jo.put("source", "chInfo_ch_appcenter__chsub_9patch");
        return RequestManager.requestString("alipay.antforest.forest.h5.consumeProp", new JSONArray().put(jo).toString());
    }

    public static String giveProp(String giveConfigId, String propId, String targetUserId) throws JSONException {
        JSONObject jo = new JSONObject();
        jo.put("giveConfigId", giveConfigId);
        jo.put("propId", propId);
        jo.put("source", "self_corner");
        jo.put("targetUserId", targetUserId);
        return RequestManager.requestString("alipay.antforest.forest.h5.giveProp", new JSONArray().put(jo).toString());
    }

    public static String collectProp(String giveConfigId, String giveId) throws JSONException {
        JSONObject jo = new JSONObject();
        jo.put("giveConfigId", giveConfigId);
        jo.put("giveId", giveId);
        jo.put("source", "chInfo_ch_appcenter__chsub_9patch");
        return RequestManager.requestString("alipay.antforest.forest.h5.collectProp", new JSONArray().put(jo).toString());
    }

    public static String consumeProp(String propId, String propType) {
        return RequestManager.requestString(
                "alipay.antforest.forest.h5.consumeProp",
                "[{\"propId\":\""
                        + propId
                        + "\",\"propType\":\""
                        + propType
                        + "\",\"source\":\"chInfo_ch_appcenter__chsub_9patch\",\"timezoneId\":\"Asia/Shanghai\",\"version\":\""
                        + VERSION
                        + "\"}]");
    }

    public static String itemList(String labelType) {
        return RequestManager.requestString(
                "com.alipay.antiep.itemList",
                "[{\"extendInfo\":\"{}\",\"labelType\":\""
                        + labelType
                        + "\",\"pageSize\":20,\"requestType\":\"rpc\",\"sceneCode\":\"ANTFOREST_VITALITY\",\"source\":\"afEntry\",\"startIndex\":0}]");
    }

    public static String itemDetail(String spuId) {
        return RequestManager.requestString(
                "com.alipay.antiep.itemDetail",
                "[{\"requestType\":\"rpc\",\"sceneCode\":\"ANTFOREST_VITALITY\",\"source\":\"afEntry\",\"spuId\":\"" + spuId + "\"}]");
    }

    public static String queryVitalityStoreIndex() {
        return RequestManager.requestString("alipay.antforest.forest.h5.queryVitalityStoreIndex", "[{\"source\":\"afEntry\"}]");
    }

    public static String exchangeBenefit(String spuId, String skuId) throws JSONException {
        JSONObject jo = new JSONObject();
        jo.put("sceneCode", "ANTFOREST_VITALITY");
        jo.put("requestId", System.currentTimeMillis() + "_" + RandomUtil.getRandom(17));
        jo.put("spuId", spuId);
        jo.put("skuId", skuId);
        jo.put("source", "GOOD_DETAIL");
        return RequestManager.requestString("com.alipay.antcommonweal.exchange.h5.exchangeBenefit", new JSONArray().put(jo).toString());
    }

    public static String testH5Rpc(String operationTpye, String requestDate) {
        return RequestManager.requestString(operationTpye, requestDate);
    }

    /**
     * 巡护保护地
     */
    public static String queryUserPatrol() throws JSONException {
        JSONObject jo = new JSONObject();
        jo.put("source", "ant_forest");
        jo.put("timezoneId", "Asia/Shanghai");
        return RequestManager.requestString("alipay.antforest.forest.h5.queryUserPatrol", new JSONArray().put(jo).toString());
    }

    public static String queryMyPatrolRecord() throws JSONException {
        JSONObject jo = new JSONObject();
        jo.put("source", "ant_forest");
        jo.put("timezoneId", "Asia/Shanghai");
        return RequestManager.requestString("alipay.antforest.forest.h5.queryMyPatrolRecord", new JSONArray().put(jo).toString());
    }

    public static String switchUserPatrol(String targetPatrolId) throws JSONException {
        JSONObject jo = new JSONObject();
        jo.put("source", "ant_forest");
        jo.put("targetPatrolId", targetPatrolId);
        jo.put("timezoneId", "Asia/Shanghai");
        return RequestManager.requestString("alipay.antforest.forest.h5.switchUserPatrol", new JSONArray().put(jo).toString());
    }

    public static String patrolGo(int nodeIndex, int patrolId) {
        return RequestManager.requestString(
                "alipay.antforest.forest.h5.patrolGo", "[{\"nodeIndex\":" + nodeIndex + ",\"patrolId\":" + patrolId + ",\"source\":\"ant_forest\"," +
                        "\"timezoneId\":\"Asia/Shanghai\"}]");
    }

    public static String patrolKeepGoing(int nodeIndex, int patrolId, String eventType) {
        String args = null;
        switch (eventType) {
            case "video":
                args = "[{\"nodeIndex\":" + nodeIndex + ",\"patrolId\":" + patrolId + ",\"reactParam\":{\"viewed\":\"Y\"},\"source\":\"ant_forest\"," +
                        "\"timezoneId\":\"Asia/Shanghai\"}]";
                break;
            case "chase":
                args = "[{\"nodeIndex\":" + nodeIndex + ",\"patrolId\":" + patrolId + ",\"reactParam\":{\"sendChat\":\"Y\"},\"source\":\"ant_forest\"," +
                        "\"timezoneId\":\"Asia/Shanghai\"}]";
                break;
            case "quiz":
                args =
                        "[{\"nodeIndex\":" + nodeIndex + ",\"patrolId\":" + patrolId + ",\"reactParam\":{\"answer\":\"correct\"},\"source\":\"ant_forest\"," +
                                "\"timezoneId\":\"Asia/Shanghai\"}]";
                break;
            default:
                args = "[{\"nodeIndex\":" + nodeIndex + ",\"patrolId\":" + patrolId + ",\"reactParam\":{},\"source\":\"ant_forest\"," +
                        "\"timezoneId\":\"Asia/Shanghai\"}]";
                break;
        }
        return RequestManager.requestString("alipay.antforest.forest.h5.patrolKeepGoing", args);
    }

    public static String exchangePatrolChance(int costStep) {
        return RequestManager.requestString(
                "alipay.antforest.forest.h5.exchangePatrolChance", "[{\"costStep\":" + costStep + ",\"source\":\"ant_forest\"," +
                        "\"timezoneId\":\"Asia/Shanghai\"}]");
    }

    public static String queryAnimalAndPiece(int animalId) {
        String args = null;
        if (animalId != 0) {
            args = "[{\"animalId\":" + animalId + ",\"source\":\"ant_forest\",\"timezoneId\":\"Asia/Shanghai\"}]";
        } else {
            args = "[{\"source\":\"ant_forest\",\"timezoneId\":\"Asia/Shanghai\",\"withDetail\":\"N\",\"withGift\":true}]";
        }
        return RequestManager.requestString("alipay.antforest.forest.h5.queryAnimalAndPiece", args);
    }

    public static String combineAnimalPiece(int animalId, String piecePropIds) {
        return RequestManager.requestString(
                "alipay.antforest.forest.h5.combineAnimalPiece",
                "[{\"animalId\":" + animalId + ",\"piecePropIds\":" + piecePropIds + ",\"timezoneId\":\"Asia/Shanghai\",\"source\":\"ant_forest\"}]");
    }

    public static String AnimalConsumeProp(String propGroup, String propId, String propType) {
        return RequestManager.requestString(
                "alipay.antforest.forest.h5.consumeProp",
                "[{\"propGroup\":\"" + propGroup + "\",\"propId\":\"" + propId + "\",\"propType\":\"" + propType + "\",\"source\":\"ant_forest\"," +
                        "\"timezoneId\":\"Asia/Shanghai\"}]");
    }

    public static String collectAnimalRobEnergy(String propId, String propType, String shortDay) {
        return RequestManager.requestString(
                "alipay.antforest.forest.h5.collectAnimalRobEnergy",
                "[{\"propId\":\"" + propId + "\",\"propType\":\"" + propType + "\",\"shortDay\":\"" + shortDay + "\",\"source" +
                        "\":\"chInfo_ch_appcenter__chsub_9patch\"}]");
    }

    /**
     * 复活能量
     */
    public static String protectBubble(String targetUserId) {
        return RequestManager.requestString(
                "alipay.antforest.forest.h5.protectBubble",
                "[{\"source\":\"ANT_FOREST_H5\",\"targetUserId\":\"" + targetUserId + "\",\"version\":\"" + VERSION + "\"}]");
    }

    /**
     * 森林礼盒
     */
    public static String collectFriendGiftBox(String targetId, String targetUserId) {
        return RequestManager.requestString(
                "alipay.antforest.forest.h5.collectFriendGiftBox",
                "[{\"source\":\"chInfo_ch_appcenter__chsub_9patch\",\"targetId\":\"" + targetId + "\",\"targetUserId\":\"" + targetUserId + "\"}]");
    }

    /**
     * 6秒拼手速 打地鼠
     */
    public static String startWhackMole(String source) {
        return RequestManager.requestString("alipay.antforest.forest.h5.startWhackMole", "[{\"source\":\"" + source + "\"}]");
    }

    public static String settlementWhackMole(String token, List<String> moleIdList, String source) {
        return RequestManager.requestString(
                "alipay.antforest.forest.h5.settlementWhackMole",
                "[{\"moleIdList\":["
                        + String.join(",", moleIdList)
                        + "],\"settlementScene\":\"NORMAL\",\"source\":" + source + ",\"token\":\""
                        + token
                        + "\",\"version\":\""
                        + VERSION
                        + "\"}]");
    }

    public static String closeWhackMole(String source) {
        return RequestManager.requestString("alipay.antforest.forest.h5.updateUserConfig", "[{\"configMap\":{\"whackMole\":\"N\"}," +
                "\"source\":\"" + source + "\"}]");
    }

    /**
     * 森林集市
     */
    public static String consultForSendEnergyByAction(String sourceType) {
        return RequestManager.requestString("alipay.bizfmcg.greenlife.consultForSendEnergyByAction", "[{\"sourceType\":\"" + sourceType + "\"}]");
    }

    /**
     * 森林集市
     */
    public static String sendEnergyByAction(String sourceType) {
        return RequestManager.requestString(
                "alipay.bizfmcg.greenlife.sendEnergyByAction",
                "[{\"actionType\":\"GOODS_BROWSE\",\"requestId\":\"" + RandomUtil.getRandomString(8) + "\",\"sourceType\":\"" + sourceType + "\"}]");
    }

    /**
     * 翻倍额外能量收取
     */
    public static String collectRobExpandEnergy(String propId, String propType) {
        return RequestManager.requestString(
                "alipay.antforest.forest.h5.collectRobExpandEnergy",
                "[{\"propId\":\"" + propId + "\",\"propType\":\"" + propType + "\",\"source\":\"chInfo_ch_appcenter__chsub_9patch\"}]");
    }

    /**
     * 医疗健康
     */
    public static String medical_health_feeds_query() {
        return RequestManager.requestString(
                "alipay.iblib.channel.build.query",
                "[{\"activityCode\":\"medical_health_feeds_query\",\"activityId\":\"2023072600001207\",\"body\":{\"apiVersion\":\"3.1.0\",\"bizId\":\"B213\"," +
                        "\"businessCode\":\"JKhealth\",\"businessId\":\"O2023071900061804\",\"cityCode\":\"330100\",\"cityName\":\"杭州\"," +
                        "\"exclContentIds\":[],\"filterItems\":[]," +
                        "\"latitude\":\"\",\"longitude\":\"\",\"moduleParam\":{\"COMMON_FEEDS_BLOCK_2024041200243259\":{}}," +
                        "\"pageCode\":\"YM2024041200137150\",\"pageNo\":1,\"pageSize\":10,\"pid\":\"BC_PD_20230713000008526\",\"queryQuizActivityFeed\":1," +
                        "\"scenceCode\":\"HEALTH_CHANNEL\",\"schemeParams\":{}," +
                        "\"scope\":\"PARTIAL\",\"selectedTabCode\":\"\",\"sourceType\":\"miniApp\",\"specialItemId\":\"\",\"specialItemType\":\"\"," +
                        "\"tenantCode\":\"2021003141652419\",\"underTakeContentId\":\"\"},\"version\":\"2.0\"}]");


    }

    public static String studentQqueryCheckInModel() throws JSONException {
        JSONObject jo = new JSONObject();
        jo.put("chInfo", "ch_appcollect__chsub_my-recentlyUsed");
        jo.put("skipTaskModule", false);
        return RequestManager.requestString("alipay.membertangram.biz.rpc.student.queryCheckInModel", new JSONArray().put(jo).toString());
    }

    /*青春特权领红包*/
    public static String studentCheckin() throws JSONException {
        JSONObject jo = new JSONObject();
        jo.put("source", "chInfo_ch_appcenter__chsub_9patch");
        return RequestManager.requestString("alipay.membertangram.biz.rpc.student.checkIn", new JSONArray().put(jo).toString());
    }

    public static String query_forest_energy() {
        return RequestManager.requestString(
                "alipay.iblib.channel.data",
                "[{\"activityCode\":\"query_forest_energy\",\"activityId\":\"2024052300762675\",\"appId\":\"2021003141652419\"," +
                        "\"body\":{\"scene\":\"FEEDS\"},\"version\":\"2.0\"}]");
    }

    public static String produce_forest_energy(String uniqueId) {
        return RequestManager.requestString(
                "alipay.iblib.channel.data",
                "[{\"activityCode\":\"produce_forest_energy\",\"activityId\":\"2024052300762674\",\"appId\":\"2021003141652419\"," +
                        "\"body\":{\"scene\":\"FEEDS\",\"uniqueId\":\""
                        + uniqueId
                        + "\"},\"version\":\"2.0\"}]");
    }

    public static String harvest_forest_energy(int energy, String id) {
        return RequestManager.requestString(
                "alipay.iblib.channel.data",
                "[{\"activityCode\":\"harvest_forest_energy\",\"activityId\":\"2024052300762676\",\"appId\":\"2021003141652419\"," +
                        "\"body\":{\"bubbles\":[{\"energy\":"
                        + energy
                        + ",\"id\":\""
                        + id
                        + "\"}],\"scene\":\"FEEDS\"},\"version\":\"2.0\"}]");
    }

    /**
     * 查询绿色行动
     *
     * @return 结果
     */
    public static String ecolifeQueryHomePage() {
        return RequestManager.requestString("alipay.ecolife.rpc.h5.queryHomePage",
                "[{\"channel\":\"ALIPAY\",\"source\":\"search_brandbox\"}]");
    }

    /**
     * 开通绿色行动
     *
     * @return 结果
     */
    public static String ecolifeOpenEcolife() {
        return RequestManager.requestString("alipay.ecolife.rpc.h5.openEcolife",
                "[{\"channel\":\"ALIPAY\",\"source\":\"renwuGD\"}]");
    }

    /**
     * 执行任务
     *
     * @param actionId actionId
     * @param dayPoint 当前日期
     * @param source   来源renwuGD,photo-comparison,search_brandbox
     * @return 结果
     */
    public static String ecolifeTick(String actionId, String dayPoint, String source) {
        String args1 = "[{\"actionId\":\"" + actionId + "\",\"channel\":\"ALIPAY\",\"dayPoint\":\""
                + dayPoint + "\",\"generateEnergy\":false,\"source\":\"" + source + "\"}]";
        return RequestManager.requestString("alipay.ecolife.rpc.h5.tick", args1);
    }

    /**
     * 查询任务信息
     *
     * @param source   来源renwuGD,photo-comparison,search_brandbox
     * @param dayPoint 当前日期
     * @return 结果
     */
    public static String ecolifeQueryDish(String source, String dayPoint) {
        return RequestManager.requestString("alipay.ecolife.rpc.h5.queryDish",
                "[{\"channel\":\"ALIPAY\",\"dayPoint\":\"" + dayPoint
                        + "\",\"source\":\"" + source + "\"}]");
    }

    /**
     * 上传照片
     *
     * @param operateType 类型：餐前、餐后
     * @param imageId     图片id
     * @param conf1       位移值？
     * @param conf2       conf2
     * @param conf3       conf3
     * @return 结果
     */
    public static String ecolifeUploadDishImage(String operateType, String imageId,
                                                double conf1, double conf2, double conf3, String dayPoint) {
        return RequestManager.requestString("alipay.ecolife.rpc.h5.uploadDishImage",
                "[{\"channel\":\"ALIPAY\",\"dayPoint\":\"" + dayPoint +
                        "\",\"source\":\"photo-comparison\",\"uploadParamMap\":{\"AIResult\":[{\"conf\":" + conf1 + ",\"kvPair\":false," +
                        "\"label\":\"other\",\"pos\":[1.0002995,0.22104378,0.0011976048,0.77727276],\"value\":\"\"}," +
                        "{\"conf\":" + conf2 + ",\"kvPair\":false,\"label\":\"guangpan\",\"pos\":[1.0002995,0.22104378,0.0011976048,0.77727276]," +
                        "\"value\":\"\"},{\"conf\":" + conf3 + ",\"kvPair\":false,\"label\":\"feiguangpan\"," +
                        "\"pos\":[1.0002995,0.22104378,0.0011976048,0.77727276],\"value\":\"\"}],\"existAIResult\":true,\"imageId\":\"" +
                        imageId + "\",\"imageUrl\":\"https://mdn.alipayobjects.com/afts/img/" + imageId +
                        "/original?bz=APM_20000067\",\"operateType\":\"" + operateType + "\"}}]");
    }

    // 查询森林能量
    public static String queryForestEnergy(String scene) {
        String args = "[{\"activityCode\":\"query_forest_energy\",\"activityId\":\"2024052300762675\",\"body\":{\"scene\":\"" + scene + "\"},\"version\":\"2" +
                ".0\"}]";
        return RequestManager.requestString("alipay.iblib.channel.data", args);
    }

    // 生成森林能量
    public static String produceForestEnergy(String scene) {
        long uniqueId = System.currentTimeMillis();
        String args = "[{\"activityCode\":\"produce_forest_energy\",\"activityId\":\"2024052300762674\",\"body\":{\"scene\":\"" + scene + "\",\"uniqueId" +
                "\":\"" + uniqueId + "\"},\"version\":\"2.0\"}]";
        return RequestManager.requestString("alipay.iblib.channel.data", args);
    }

    // 领取森林能量
    public static String harvestForestEnergy(String scene, JSONArray bubbles) {
        String args =
                "[{\"activityCode\":\"harvest_forest_energy\",\"activityId\":\"2024052300762676\",\"body\":{\"bubbles\":" + bubbles + ",\"scene\":\"" + scene + "\"},\"version\":\"2.0\"}]";
        return RequestManager.requestString("alipay.iblib.channel.data", args);
    }
}
