package fansirsqi.xposed.sesame.task.antFarm;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.UUID;

import fansirsqi.xposed.sesame.data.Status;
import fansirsqi.xposed.sesame.hook.RequestManager;
import fansirsqi.xposed.sesame.util.RandomUtil;

public class AntFarmRpcCall {
    private static final String VERSION = "1.8.2302070202.46";


    /**
     * 进入农场
     *
     * @param userId       自己的用户id
     * @param targetUserId 所在农场的用户id
     * @return 返回结果
     * @throws JSONException 异常内容
     */
    public static String enterFarm(String userId, String targetUserId) throws JSONException {
        JSONObject args = new JSONObject();
        args.put("animalId", "");
        args.put("bizCode", "");
        args.put("gotoneScene", "");
        args.put("gotoneTemplateId", "");
        args.put("groupId", "");
        args.put("growthExtInfo", "");
        args.put("inviteUserId", "");
        args.put("masterFarmId", "");
        args.put("queryLastRecordNum", true);
        args.put("recall", false);
        args.put("requestType", "NORMAL");
        args.put("sceneCode", "ANTFARM");
        args.put("shareId", "");
        args.put("shareUniqueId", System.currentTimeMillis() + "_" + targetUserId);
        args.put("source", "ANTFOREST");
        args.put("starFarmId", "");
        args.put("subBizCode", "");
        args.put("touchRecordId", "");
        args.put("userId", userId);
        args.put("userToken", "");
        args.put("version", VERSION);
        String pamras = "[" + args + "]";
        return RequestManager.requestString("com.alipay.antfarm.enterFarm", pamras);
    }


    // 一起拿小鸡饲料
    public static String letsGetChickenFeedTogether() {
        String args1 = "[{\"needHasInviteUserByCycle\":\"true\",\"requestType\":\"RPC\",\"sceneCode\":\"ANTFARM_P2P\",\"source\":\"ANTFARM\",\"startIndex\":0," + "\"version\":\"" + VERSION + "\"}]";
        String args = "[{\"needHasInviteUserByCycle\":true,\"requestType\":\"RPC\",\"sceneCode\":\"ANTFARM_FAMILY_SHARE\",\"source\":\"ANTFARM\",\"startIndex\":0}]";
        return RequestManager.requestString("com.alipay.antiep.canInvitePersonListP2P", args1);
    }

    // 赠送饲料
    public static String giftOfFeed(String bizTraceId, String userId) {
        String args1 = "[{\"beInvitedUserId\":\"" + userId +
                "\",\"bizTraceId\":\"" + bizTraceId +
                "\",\"requestType\":\"RPC\",\"sceneCode\":\"ANTFARM_P2P\"," +
                "\"source\":\"ANTFARM\",\"version\":\"" + VERSION + "\"}]";
        return RequestManager.requestString("com.alipay.antiep.inviteP2P", args1);
    }

    public static String syncAnimalStatus(String farmId, String operTag, String operType) throws JSONException {
        JSONObject args = new JSONObject();
        args.put("farmId", farmId);
        args.put("operTag", operTag);
        args.put("operType", operType);
        args.put("requestType", "NORMAL");
        args.put("sceneCode", "ANTFARM");
        args.put("source", "H5");
        args.put("version", VERSION);
        String params = "[" + args + "]";
        return RequestManager.requestString("com.alipay.antfarm.syncAnimalStatus", params);
    }


    public static String sleep() {
        String args1 = "[{\"requestType\":\"RPC\",\"sceneCode\":\"ANTFARM\",\"source\":\"LOVECABIN\",\"version\":\"unknown\"}]";
        return RequestManager.requestString("com.alipay.antfarm.sleep", args1);
    }

    public static String wakeUp() {
        String args1 = "[{\"requestType\":\"RPC\",\"sceneCode\":\"ANTFARM\",\"source\":\"LOVECABIN\",\"version\":\"unknown\"}]";
        return RequestManager.requestString("com.alipay.antfarm.wakeUp", args1);
    }

    public static String queryLoveCabin(String userId) {
        String args1 = "[{\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"source\":\"ENTERFARM\",\"userId\":\"" +
                userId + "\",\"version\":\"" + VERSION + "\"}]";
        return RequestManager.requestString("com.alipay.antfarm.queryLoveCabin", args1);
    }

    public static String rewardFriend(String consistencyKey, String friendId, String productNum, String time) {
        String args1 = "[{\"canMock\":true,\"consistencyKey\":\"" + consistencyKey
                + "\",\"friendId\":\"" + friendId + "\",\"operType\":\"1\",\"productNum\":" + productNum +
                ",\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"source\":\"H5\",\"time\":"
                + time + ",\"version\":\"" + VERSION + "\"}]";
        return RequestManager.requestString("com.alipay.antfarm.rewardFriend", args1);
    }

    public static String recallAnimal(String animalId, String currentFarmId, String masterFarmId) {
        String args1 = "[{\"animalId\":\"" + animalId + "\",\"currentFarmId\":\""
                + currentFarmId + "\",\"masterFarmId\":\"" + masterFarmId +
                "\",\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"source\":\"H5\",\"version\":\""
                + VERSION + "\"}]";
        return RequestManager.requestString("com.alipay.antfarm.recallAnimal", args1);
    }

    public static String orchardRecallAnimal(String animalId, String userId) {
        String args1 = "[{\"animalId\":\"" + animalId + "\",\"orchardUserId\":\"" + userId +
                "\",\"requestType\":\"NORMAL\",\"sceneCode\":\"ORCHARD\",\"source\":\"zhuangyuan_zhaohuixiaoji\",\"version\":\"0.1.2403061630.6\"}]";
        return RequestManager.requestString("com.alipay.antorchard.recallAnimal", args1);
    }

    public static String sendBackAnimal(String sendType, String animalId, String currentFarmId, String masterFarmId) {
        String args1 = "[{\"animalId\":\"" + animalId + "\",\"currentFarmId\":\""
                + currentFarmId + "\",\"masterFarmId\":\"" + masterFarmId +
                "\",\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"sendType\":\""
                + sendType + "\",\"source\":\"H5\",\"version\":\""
                + VERSION + "\"}]";
        return RequestManager.requestString("com.alipay.antfarm.sendBackAnimal", args1);
    }

    public static String harvestProduce(String farmId) {
        String args1 = "[{\"canMock\":true,\"farmId\":\"" + farmId +
                "\",\"giftType\":\"\",\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"source\":\"H5\",\"version\":\""
                + VERSION + "\"}]";
        return RequestManager.requestString("com.alipay.antfarm.harvestProduce", args1);
    }

    public static String listActivityInfo() {
        String args1 = "[{\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"source\":\"H5\",\"version\":\""
                + VERSION + "\"}]";
        return RequestManager.requestString("com.alipay.antfarm.listActivityInfo", args1);
    }

    public static String donation(String activityId, int donationAmount) {
        String args1 = "[{\"activityId\":\"" + activityId + "\",\"donationAmount\":" + donationAmount +
                ",\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"source\":\"H5\",\"version\":\""
                + VERSION + "\"}]";
        return RequestManager.requestString("com.alipay.antfarm.donation", args1);
    }

    public static String listFarmTask() {
        String args1 = "[{\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"source\":\"H5\",\"version\":\""
                + VERSION + "\"}]";
        return RequestManager.requestString("com.alipay.antfarm.listFarmTask", args1);
    }

    public static String getAnswerInfo() {
        String args1 = "[{\"answerSource\":\"foodTask\",\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"source\":\"H5\",\"version\":\""
                + VERSION + "\"}]";
        return RequestManager.requestString("com.alipay.antfarm.getAnswerInfo", args1);
    }

    public static String answerQuestion(String quesId, int answer) {
        String args1 = "[{\"answers\":\"[{\\\"questionId\\\":\\\"" + quesId + "\\\",\\\"answers\\\":[" + answer +
                "]}]\",\"bizkey\":\"ANSWER\",\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"source\":\"H5\",\"version\":\""
                + VERSION + "\"}]";
        return RequestManager.requestString("com.alipay.antfarm.doFarmTask", args1);
    }

    public static String receiveFarmTaskAward(String taskId) {
        String args1 = "[{\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"source\":\"H5\",\"taskId\":\""
                + taskId + "\",\"version\":\"" + VERSION + "\"}]";
        return RequestManager.requestString("com.alipay.antfarm.receiveFarmTaskAward", args1);
    }

    public static String listToolTaskDetails() {
        String args1 = "[{\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"source\":\"H5\",\"version\":\""
                + VERSION + "\"}]";
        return RequestManager.requestString("com.alipay.antfarm.listToolTaskDetails", args1);
    }

    public static String receiveToolTaskReward(String rewardType, int rewardCount, String taskType) {
        String args1 = "[{\"ignoreLimit\":false,\"requestType\":\"NORMAL\",\"rewardCount\":" + rewardCount
                + ",\"rewardType\":\"" + rewardType + "\",\"sceneCode\":\"ANTFARM\",\"source\":\"H5\",\"taskType\":\""
                + taskType + "\",\"version\":\"" + VERSION + "\"}]";
        return RequestManager.requestString("com.alipay.antfarm.receiveToolTaskReward", args1);
    }

    public static String feedAnimal(String farmId) throws JSONException {
//        [{"animalType":"CHICK","canMock":true,"farmId":"xxxxxxxxxx","requestType":"NORMAL","sceneCode":"ANTFARM","source":"chInfo_ch_appcenter__chsub_9patch","version":"1.8.2302070202.46"}]
        JSONObject args = new JSONObject();
        args.put("animalType", "CHICK");
        args.put("canMock", true);
        args.put("farmId", farmId);
        args.put("requestType", "NORMAL");
        args.put("sceneCode", "ANTFARM");
        args.put("source", "chInfo_ch_appcenter__chsub_9patch");
        args.put("version", VERSION);
        String params = "[" + args + "]";
        return RequestManager.requestString("com.alipay.antfarm.feedAnimal", params);
    }

    public static String listFarmTool() {
        String args1 = "[{\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"source\":\"H5\",\"version\":\"" + VERSION + "\"}]";
        return RequestManager.requestString("com.alipay.antfarm.listFarmTool", args1);
    }

    public static String useFarmTool(String targetFarmId, String toolId, String toolType) {
        String args1 = "[{\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"source\":\"H5\",\"targetFarmId\":\""
                + targetFarmId + "\",\"toolId\":\"" + toolId + "\",\"toolType\":\"" + toolType + "\",\"version\":\""
                + VERSION + "\"}]";
        return RequestManager.requestString("com.alipay.antfarm.useFarmTool", args1);
    }

    public static String rankingList(int pageStartSum) {
        String args1 = "[{\"pageSize\":20,\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"source\":\"H5\",\"startNum\":"
                + pageStartSum + ",\"version\":\"" + VERSION + "\"}]";
        return RequestManager.requestString("com.alipay.antfarm.rankingList", args1);
    }

    public static String notifyFriend(String animalId, String notifiedFarmId) {
        String args1 = "[{\"animalId\":\"" + animalId +
                "\",\"animalType\":\"CHICK\",\"canBeGuest\":true,\"notifiedFarmId\":\"" + notifiedFarmId +
                "\",\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"source\":\"H5\",\"version\":\""
                + VERSION + "\"}]";
        return RequestManager.requestString("com.alipay.antfarm.notifyFriend", args1);
    }

    public static String feedFriendAnimal(String friendFarmId) throws JSONException {
//        [{"friendFarmId":"10171020124112012088822393935729","requestType":"NORMAL","sceneCode":"ANTFARM","source":"chInfo_ch_appcenter__chsub_9patch","version":"1.8.2302070202.46"}]
        JSONObject args = new JSONObject();
        args.put("friendFarmId", friendFarmId);
        args.put("requestType", "NORMAL");
        args.put("sceneCode", "ANTFARM");
        args.put("source", "chInfo_ch_appcenter__chsub_9patch");
        args.put("version", VERSION);
        String params = "[" + args + "]";

        return RequestManager.requestString("com.alipay.antfarm.feedFriendAnimal", params);
    }

    public static String farmId2UserId(String farmId) {
        int l = farmId.length() / 2;
        return farmId.substring(l);
    }

    /**
     * 收集肥料
     *
     * @param manurePotNO 肥料袋号
     * @return 返回结果
     */
    public static String collectManurePot(String manurePotNO) {
//        "isSkipTempLimit":true, 肥料满了也强行收取，解决 农场未开通 打扫鸡屎失败问题
        return RequestManager.requestString("com.alipay.antfarm.collectManurePot", "[{\"isSkipTempLimit\":true,\"manurePotNOs\":\"" + manurePotNO +
                "\",\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"source\":\"H5\",\"version\":\"" + VERSION
                + "\"}]");
    }

    public static String sign() {
        return RequestManager.requestString("com.alipay.antfarm.sign", "[{\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"source\":\"H5\",\"version\":\"" + VERSION + "\"}]");
    }

    public static String initFarmGame(String gameType) {
        if ("flyGame".equals(gameType)) {
            return RequestManager.requestString("com.alipay.antfarm.initFarmGame",
                    "[{\"gameType\":\"flyGame\",\"requestType\":\"RPC\",\"sceneCode\":\"FLAYGAME\"," +
                            "\"source\":\"FARM_game_yundongfly\",\"toolTypes\":\"ACCELERATETOOL,SHARETOOL,NONE\",\"version\":\"\"}]");
        }
        return RequestManager.requestString("com.alipay.antfarm.initFarmGame",
                "[{\"gameType\":\"" + gameType
                        + "\",\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"source\":\"H5\",\"toolTypes\":\"STEALTOOL,ACCELERATETOOL,SHARETOOL\"}]");
    }

    public static int RandomScore(String str) {
        if ("starGame".equals(str)) {
            return RandomUtil.nextInt(300, 400);
        } else if ("jumpGame".equals(str)) {
            return RandomUtil.nextInt(250, 270) * 10;
        } else if ("flyGame".equals(str)) {
            return RandomUtil.nextInt(4000, 8000);
        } else if ("hitGame".equals(str)) {
            return RandomUtil.nextInt(80, 120);
        } else {
            return 210;
        }
    }

    public static String recordFarmGame(String gameType) {
        String uuid = getUuid();
        String md5String = getMD5(uuid);
        int score = RandomScore(gameType);
        if ("flyGame".equals(gameType)) {
            int foodCount = score / 50;
            return RequestManager.requestString("com.alipay.antfarm.recordFarmGame",
                    "[{\"foodCount\":" + foodCount + ",\"gameType\":\"flyGame\",\"md5\":\"" + md5String
                            + "\",\"requestType\":\"RPC\",\"sceneCode\":\"FLAYGAME\",\"score\":" + score
                            + ",\"source\":\"ANTFARM\",\"toolTypes\":\"ACCELERATETOOL,SHARETOOL,NONE\",\"uuid\":\"" + uuid
                            + "\",\"version\":\"\"}]");
        }
        return RequestManager.requestString("com.alipay.antfarm.recordFarmGame",
                "[{\"gameType\":\"" + gameType + "\",\"md5\":\"" + md5String
                        + "\",\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"score\":" + score
                        + ",\"source\":\"H5\",\"toolTypes\":\"STEALTOOL,ACCELERATETOOL,SHARETOOL\",\"uuid\":\"" + uuid
                        + "\"}]");
    }

    private static String getUuid() {
        StringBuilder sb = new StringBuilder();
        for (String str : UUID.randomUUID().toString().split("-")) {
            sb.append(str.substring(str.length() / 2));
        }
        return sb.toString();
    }

    public static String getMD5(String password) {
        try {
            // 得到一个信息摘要器
            MessageDigest digest = MessageDigest.getInstance("md5");
            byte[] result = digest.digest(password.getBytes());
            StringBuilder buffer = new StringBuilder();
            // 把没一个byte 做一个与运算 0xff;
            for (byte b : result) {
                // 与运算
                int number = b & 0xff;// 加盐
                String str = Integer.toHexString(number);
                if (str.length() == 1) {
                    buffer.append("0");
                }
                buffer.append(str);
            }
            // 标准的md5加密后的结果
            return buffer.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return "";
        }
    }


    /**
     * 小鸡厨房 - 进厨房
     *
     * @param userId 用户id
     * @return 返回结果
     * @throws JSONException 异常
     */
    public static String enterKitchen(String userId) throws JSONException {
        JSONObject args = new JSONObject();
        args.put("requestType", "RPC");
        args.put("sceneCode", "ANTFARM");
        args.put("source", "VILLA");
        args.put("userId", userId);
        args.put("version", "unknown");
        String params = "[" + args + "]";
        return RequestManager.requestString("com.alipay.antfarm.enterKitchen", params);
    }

    public static String collectDailyFoodMaterial(int dailyFoodMaterialAmount) {
        return RequestManager.requestString("com.alipay.antfarm.collectDailyFoodMaterial",
                "[{\"collectDailyFoodMaterialAmount\":" + dailyFoodMaterialAmount + ",\"requestType\":\"RPC\",\"sceneCode\":\"ANTFARM\",\"source\":\"VILLA\",\"version\":\"unknown\"}]");
    }

    public static String queryFoodMaterialPack() {
        return RequestManager.requestString("com.alipay.antfarm.queryFoodMaterialPack",
                "[{\"requestType\":\"RPC\",\"sceneCode\":\"ANTFARM\",\"source\":\"kitchen\",\"version\":\"unknown\"}]");
    }

    public static String collectDailyLimitedFoodMaterial(int dailyLimitedFoodMaterialAmount) {
        return RequestManager.requestString("com.alipay.antfarm.collectDailyLimitedFoodMaterial",
                "[{\"collectDailyLimitedFoodMaterialAmount\":" + dailyLimitedFoodMaterialAmount
                        + ",\"requestType\":\"RPC\",\"sceneCode\":\"ANTFARM\",\"source\":\"kitchen\",\"version\":\"unknown\"}]");
    }

    public static String farmFoodMaterialCollect() {
        return RequestManager.requestString("com.alipay.antorchard.farmFoodMaterialCollect",
                "[{\"collect\":true,\"requestType\":\"RPC\",\"sceneCode\":\"ORCHARD\",\"source\":\"VILLA\",\"version\":\"unknown\"}]");
    }

    /**
     * 小鸡厨房 - 做菜
     *
     * @param userId
     * @param source
     * @return
     * @throws JSONException
     */
    public static String cook(String userId, String source) throws JSONException {
//[{"requestType":"RPC","sceneCode":"ANTFARM","source":"VILLA","userId":"2088522730162798","version":"unknown"}]
        JSONObject args = new JSONObject();
        args.put("requestType", "RPC");
        args.put("sceneCode", "ANTFARM");
        args.put("source", source);
        args.put("userId", userId);
        args.put("version", "unknown");
        String params = "[" + args + "]";
        return RequestManager.requestString("com.alipay.antfarm.cook", params);
    }

    public static String useFarmFood(String cookbookId, String cuisineId) {
        return RequestManager.requestString("com.alipay.antfarm.useFarmFood",
                "[{\"cookbookId\":\"" + cookbookId + "\",\"cuisineId\":\"" + cuisineId
                        + "\",\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"source\":\"chInfo_ch_appcenter__chsub_9patch\",\"useCuisine\":true,\"version\":\""
                        + VERSION + "\"}]");
    }

    public static String collectKitchenGarbage() {
        return RequestManager.requestString("com.alipay.antfarm.collectKitchenGarbage",
                "[{\"requestType\":\"RPC\",\"sceneCode\":\"ANTFARM\",\"source\":\"VILLA\",\"version\":\"unknown\"}]");
    }

    /* 日常任务 */
    public static String doFarmTask(String bizKey) {
        return RequestManager.requestString("com.alipay.antfarm.doFarmTask",
                "[{\"bizKey\":\"" + bizKey
                        + "\",\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"source\":\"H5\",\"version\":\""
                        + VERSION + "\"}]");
    }

    public static String queryTabVideoUrl() {
        return RequestManager.requestString("com.alipay.antfarm.queryTabVideoUrl",
                "[{\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"source\":\"H5\",\"version\":\"" + VERSION
                        + "\"}]");
    }

    public static String videoDeliverModule(String bizId) {
        return RequestManager.requestString("alipay.content.reading.life.deliver.module",
                "[{\"bizId\":\"" + bizId
                        + "\",\"bizType\":\"CONTENT\",\"chInfo\":\"ch_antFarm\",\"refer\":\"antFarm\",\"timestamp\":\""
                        + System.currentTimeMillis() + "\"}]");
    }

    public static String videoTrigger(String bizId) {
        return RequestManager.requestString("alipay.content.reading.life.prize.trigger",
                "[{\"bizId\":\"" + bizId
                        + "\",\"bizType\":\"CONTENT\",\"prizeFlowNum\":\"VIDEO_TASK\",\"prizeType\":\"farmFeed\"}]");
    }

    /* 惊喜礼包 */
    public static String drawLotteryPlus() {
        return RequestManager.requestString("com.alipay.antfarm.drawLotteryPlus",
                "[{\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"source\":\"H5 \",\"version\":\"\"}]");
    }

    /* 小麦 */
    public static String acceptGift() {
        return RequestManager.requestString("com.alipay.antfarm.acceptGift",
                "[{\"ignoreLimit\":false,\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"source\":\"H5\",\"version\":\""
                        + VERSION + "\"}]");
    }

    public static String visitFriend(String friendFarmId) {
        return RequestManager.requestString("com.alipay.antfarm.visitFriend",
                "[{\"friendFarmId\":\"" + friendFarmId
                        + "\",\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"source\":\"H5\",\"version\":\""
                        + VERSION + "\"}]");
    }

    /**
     * 小鸡日志当月日期查询
     *
     * @return
     */
    public static String queryChickenDiaryList() {
        return RequestManager.requestString("com.alipay.antfarm.queryChickenDiaryList",
                "[{\"requestType\":\"NORMAL\",\"sceneCode\":\"DIARY\",\"source\":\"antfarm_icon\"}]");
    }

    /**
     * 小鸡日志指定月份日期查询
     *
     * @param yearMonth 日期格式：yyyy-MM
     * @return
     */
    public static String queryChickenDiaryList(String yearMonth) {
        return RequestManager.requestString("com.alipay.antfarm.queryChickenDiaryList",
                "[{\"queryMonthStr\":\"" + yearMonth + "\",\"requestType\":\"NORMAL\",\"sceneCode\":\"DIARY\",\"source\":\"antfarm_icon\"}]");
    }

    public static String queryChickenDiary(String queryDayStr) {
        return RequestManager.requestString("com.alipay.antfarm.queryChickenDiary",
                "[{\"queryDayStr\":\"" + queryDayStr
                        + "\",\"requestType\":\"NORMAL\",\"sceneCode\":\"DIARY\",\"source\":\"antfarm_icon\"}]");
    }

    public static String diaryTietie(String diaryDate, String roleId) {
        return RequestManager.requestString("com.alipay.antfarm.diaryTietie",
                "[{\"diaryDate\":\"" + diaryDate + "\",\"requestType\":\"NORMAL\",\"roleId\":\"" + roleId
                        + "\",\"sceneCode\":\"DIARY\",\"source\":\"antfarm_icon\"}]");
    }

    /**
     * 小鸡日记点赞
     *
     * @param DiaryId 日记id
     * @return
     */
    public static String collectChickenDiary(String DiaryId) {
        return RequestManager.requestString("com.alipay.antfarm.collectChickenDiary",
                "[{\"collectStatus\":true,\"diaryId\":\"" + DiaryId + "\",\"requestType\":\"NORMAL\",\"sceneCode\":\"MOOD\",\"source\":\"H5\"}]");
    }

    public static String visitAnimal() {
        return RequestManager.requestString("com.alipay.antfarm.visitAnimal",
                "[{\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"source\":\"H5\",\"version\":\"" + VERSION +
                        "\"}]");
    }

    public static String feedFriendAnimalVisit(String friendFarmId) {
        return RequestManager.requestString("com.alipay.antfarm.feedFriendAnimal",
                "[{\"friendFarmId\":\"" + friendFarmId + "\",\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\"," +
                        "\"source\":\"visitChicken\",\"version\":\"" + VERSION + "\"}]");
    }

    public static String visitAnimalSendPrize(String token) {
        return RequestManager.requestString("com.alipay.antfarm.visitAnimalSendPrize",
                "[{\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"source\":\"H5\",\"token\":\"" + token +
                        "\",\"version\":\"" + VERSION + "\"}]");
    }

    /* 抽抽乐 */
    public static String enterDrawMachine() {
        return RequestManager.requestString("com.alipay.antfarm.enterDrawMachine",
                "[{\"requestType\":\"RPC\",\"sceneCode\":\"ANTFARM\",\"source\":\"siliaorenwu\"}]");
    }

    /**
     * 抽抽乐-抽奖类型选择器
     *
     * @param drawType 抽奖类型 ipDraw-对应IP抽奖
     * @return ""
     */
    private static String chouchouleSelector(String drawType) {
        if (drawType.equals("ipDraw")) {
            return "ANTFARM_IP_DRAW_TASK";
        }
        return "ANTFARM_DRAW_TIMES_TASK";
    }

    /**
     * 查询抽抽乐任务列表
     *
     * @param drawType 抽奖类型
     * @return 返回结果
     * @throws JSONException 异常
     */
    public static String chouchouleListFarmTask(String drawType) throws JSONException {
        String taskSceneCode = chouchouleSelector(drawType);
        JSONObject args = new JSONObject();
        args.put("requestType", "NORMAL");
        args.put("sceneCode", "ANTFARM");
        args.put("source", "H5");
        args.put("taskSceneCode", taskSceneCode);
        args.put("topTask", "");
        String params = "[" + args + "]";
        return RequestManager.requestString("com.alipay.antfarm.listFarmTask", params);
    }

    /**
     * 执行抽抽乐任务
     *
     * @param drawType 抽奖类型
     * @param bizKey   任务ID
     * @return 返回结果
     * @throws JSONException 异常
     */
    public static String chouchouleDoFarmTask(String drawType, String bizKey) throws JSONException {
        String taskSceneCode = chouchouleSelector(drawType);
        JSONObject args = new JSONObject();
        args.put("bizKey", bizKey);
        args.put("requestType", "RPC");
        args.put("sceneCode", "ANTFARM");
        args.put("source", "H5");
        args.put("taskSceneCode", taskSceneCode);
        String params = "[" + args + "]";
        return RequestManager.requestString("com.alipay.antfarm.doFarmTask", params);
    }


    /**
     * 领取抽抽乐任务奖励-抽奖次数
     *
     * @param drawType 抽奖类型
     * @param taskId   任务ID
     * @return 返回结果
     * @throws JSONException 异常
     */
    public static String chouchouleReceiveFarmTaskAward(String drawType, String taskId) throws JSONException {
        String taskSceneCode = chouchouleSelector(drawType);
        JSONObject args = new JSONObject();
        args.put("requestType", "RPC");
        args.put("sceneCode", "ANTFARM");
        args.put("source", "H5");
        args.put("taskId", taskId);
        args.put("taskSceneCode", taskSceneCode);
        String params = "[" + args + "]";
        return RequestManager.requestString("com.alipay.antfarm.receiveFarmTaskAward", params);
    }

    /**
     * IP抽抽乐查询活动与抽奖次数
     **/
    public static String queryDrawMachineActivity() {
        return RequestManager.requestString("com.alipay.antfarm.queryDrawMachineActivity", "[{\"otherScenes\":[\"dailyDrawMachine\"],\"requestType\":\"RPC\",\"scene\":\"ipDrawMachine\",\"sceneCode\":\"ANTFARM\",\"source\":\"ip_ccl\"}]");
    }

    /**
     * IP抽抽乐抽奖
     **/
    public static String drawMachine() {
        return RequestManager.requestString("com.alipay.antfarm.drawMachine", "[{\"requestType\":\"RPC\",\"scene\":\"ipDrawMachine\",\"sceneCode\":\"ANTFARM\",\"source\":\"ip_ccl\"}]");
    }

    public static String hireAnimal(String farmId, String animalId) {
        return RequestManager.requestString("com.alipay.antfarm.hireAnimal",
                "[{\"friendFarmId\":\"" + farmId + "\",\"hireActionType\":\"HIRE_IN_FRIEND_FARM\",\"hireAnimalId\":\"" + animalId + "\",\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"sendCardChat\":false,\"source\":\"H5\",\"version\":\"" + VERSION + "\"}]");
    }

    public static String DrawPrize() {
        return RequestManager.requestString("com.alipay.antfarm.DrawPrize",
                "[{\"requestType\":\"RPC\",\"sceneCode\":\"ANTFARM\",\"source\":\"chouchoule\"}]");
    }

    public static String DrawPrize(String activityId) {
        return RequestManager.requestString("com.alipay.antfarm.DrawPrize",
                "[{\"activityId\":\"" + activityId + "\",\"requestType\":\"RPC\",\"sceneCode\":\"ANTFARM\",\"source\":\"icon\"}]");
    }

    public static String drawGameCenterAward() {
        return RequestManager.requestString("com.alipay.antfarm.drawGameCenterAward",
                "[{\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"source\":\"H5\",\"version\":\"" + VERSION + "\"}]");
    }

    public static String queryGameList() {
        return RequestManager.requestString("com.alipay.antfarm.queryGameList",
                "[{\"commonDegradeResult\":{\"deviceLevel\":\"high\",\"resultReason\":0,\"resultType\":0},\"platform\":\"Android\",\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"source\":\"H5\",\"version\":\"" + VERSION + "\"}]");
    }

    // 小鸡换装
    public static String listOrnaments() {
        return RequestManager.requestString("com.alipay.antfarm.listOrnaments",
                "[{\"pageNo\":\"1\",\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"setsType\":\"ACHIEVEMENTSETS\",\"source\":\"H5\",\"subType\":\"sets\",\"type\":\"apparels\",\"version\":\"" + VERSION + "\"}]");
    }

    public static String saveOrnaments(String animalId, String farmId, String ornaments) {
        return RequestManager.requestString("com.alipay.antfarm.saveOrnaments",
                "[{\"animalId\":\"" + animalId + "\",\"farmId\":\"" + farmId + "\",\"ornaments\":\"" + ornaments + "\",\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"source\":\"H5\",\"version\":\"" + VERSION + "\"}]");
    }

    // 亲密家庭
    public static String enterFamily() {
        String args = "[{\"fromAnn\":false,\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"source\":\"H5\",\"timeZoneId\":\"Asia/Shanghai\"}]";
        return RequestManager.requestString("com.alipay.antfarm.enterFamily", args);
    }

    public static String familyReceiveFarmTaskAward(String taskId) {
        String args = "[{\"awardType\":\"FAMILY_INTIMACY\",\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"source\":\"H5\",\"taskId\":\"" + taskId + "\",\"taskSceneCode\":\"ANTFARM_FAMILY_TASK\"}]";
        return RequestManager.requestString("com.alipay.antfarm.receiveFarmTaskAward", args);
    }

    public static String familyAwardList() {
        String args = "[{\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"source\":\"H5\"}]";
        return RequestManager.requestString("com.alipay.antfarm.familyAwardList", args);
    }

    public static String receiveFamilyAward(String rightId) {
        String args = "[{\"requestType\":\"NORMAL\",\"rightId\":\"" + rightId + "\",\"sceneCode\":\"ANTFARM\",\"source\":\"H5\"}]";
        return RequestManager.requestString("com.alipay.antfarm.receiveFamilyAward", args);
    }

    public static String assignFamilyMember(String assignAction, String beAssignUser) {
        return RequestManager.requestString("com.alipay.antfarm.assignFamilyMember",
                "[{\"assignAction\":\"" + assignAction + "\",\"beAssignUser\":\"" + beAssignUser + "\",\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"source\":\"H5\"}]");
    }

    public static String sendChat(String chatCardType, String receiverUserId) {
        return RequestManager.requestString("com.alipay.antfarm.sendChat",
                "[{\"chatCardType\":\"" + chatCardType + "\",\"receiverUserId\":\"" + receiverUserId + "\",\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"source\":\"H5\"}]");
    }

    public static String deliverSubjectRecommend(JSONArray friendUserIdList) {
        String args = "[{\"friendUserIds\":" + friendUserIdList + ",\"requestType\":\"NORMAL\",\"sceneCode\":\"ChickFamily\",\"source\":\"H5\"}]";
        return RequestManager.requestString("com.alipay.antfarm.deliverSubjectRecommend", args);
    }

    public static String deliverContentExpand(JSONArray friendUserIdList, String ariverRpcTraceId) throws JSONException {
        JSONObject args = new JSONObject();
        args.put("ariverRpcTraceId", ariverRpcTraceId);
        args.put("eventId", "event-deliver-familygoodmorning");
        args.put("eventName", "AI传话家庭版早安");
        args.put("friendUserIds", friendUserIdList);
        args.put("memo", "SUCCESS");
        args.put("requestType", "NORMAL");
        args.put("resultCode", "100");
        args.put("sceneCode", "ANTFARM");
        args.put("sceneId", "deliver-familygoodmorning");
        args.put("sceneName", "小鸡传话家庭版早安");
        args.put("source", "H5");
        args.put("success", true);
        String params = "[" + args + "]";
        return RequestManager.requestString("com.alipay.antfarm.DeliverContentExpand", params);
    }

    public static String QueryExpandContent(String deliverId) throws JSONException {
        JSONObject args = new JSONObject();
        args.put("requestType", "NORMAL");
        args.put("sceneCode", "ANTFARM");
        args.put("source", "H5");
        args.put("deliverId", deliverId);
        String params = "[{" + args + "}]";
        return RequestManager.requestString("com.alipay.antfarm.QueryExpandContent", params);
    }

    public static String deliverMsgSend(String groupId, JSONArray friendUserIds, String content, String deliverId) throws JSONException {
        JSONObject args = new JSONObject();
        args.put("content", content);
        args.put("deliverId", deliverId);
        args.put("friendUserIds", friendUserIds);
        args.put("groupId", groupId);
        args.put("mode", "AI");
        args.put("requestType", "NORMAL");
        args.put("sceneCode", "ANTFARM");
        args.put("source", "H5");
        args.put("spaceType", "ChickFamily");
        String params = "[{" + args + "}]";
        return RequestManager.requestString("com.alipay.antfarm.DeliverMsgSend", params);
    }

    public static String syncFamilyStatus(String groupId, String operType, String syncUserIds) {
        String args = "[{\"groupId\":\"" + groupId + "\",\"operType\":\"" + operType + "\",\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"source\":\"H5\",\"syncUserIds\":[\"" + syncUserIds + "\"]}]";
        return RequestManager.requestString("com.alipay.antfarm.syncFamilyStatus", args);
    }

    public static String inviteFriendVisitFamily(JSONArray receiverUserId) {
        String args = "[{\"bizType\":\"FAMILY_SHARE\",\"receiverUserId\":" + receiverUserId + ",\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"source\":\"H5\"}]";
        return RequestManager.requestString("com.alipay.antfarm.inviteFriendVisitFamily", args);
    }

    public static String familyEatTogether(String groupId, JSONArray friendUserIdList, JSONArray cuisines) {
        String args = "[{\"cuisines\":" + cuisines + ",\"friendUserIds\":" + friendUserIdList + ",\"groupId\":\"" + groupId + "\",\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"source\":\"H5\",\"spaceType\":\"ChickFamily\"}]";
        return RequestManager.requestString("com.alipay.antfarm.familyEatTogether", args);
    }

    public static String queryRecentFarmFood(int queryNum) {
        String args = "[{\"queryNum\": " + queryNum + ",\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"source\":\"H5\"}]";
        return RequestManager.requestString("com.alipay.antfarm.queryRecentFarmFood", args);
    }

    public static String feedFriendAnimal(String friendFarmId, String groupId) {
        String args = "[{\"friendFarmId\": \"" + friendFarmId + "\",\"groupId\": \"" + groupId + "\",\"requestType\":\"NORMAL\",\"sceneCode\":\"ChickFamily\",\"source\":\"H5\",\"spaceType\":\"ChickFamily\"}]";
        return RequestManager.requestString("com.alipay.antfarm.feedFriendAnimal", args);
    }

    public static String queryFamilyDrawActivity() {
        String args = "[{\"bizType\":\"ANTFARM_GAME_CENTER\",\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"source\":\"H5\"}]";
        return RequestManager.requestString("com.alipay.antfarm.queryFamilyDrawActivity", args);
    }

    public static String familyDraw() {
        String args = "[{\"bizType\":\"ANTFARM_GAME_CENTER\",\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"source\":\"H5\"}]";
        return RequestManager.requestString("com.alipay.antfarm.familyDraw", args);
    }

    public static String familyBatchInviteP2P(JSONArray inviteP2PVOList, String sceneCode) {
        String args = "[{\"inviteP2PVOList\":" + inviteP2PVOList + ",\"requestType\":\"RPC\",\"sceneCode\":\"" + sceneCode + "\",\"source\":\"antfarm\"}]";
        return RequestManager.requestString("com.alipay.antiep.batchInviteP2P", args);
    }

    public static String familyDrawSignReceiveFarmTaskAward(String taskId) {
        String args = "[{\"awardType\":\"FAMILY_DRAW_TIME\",\"bizType\":\"ANTFARM_GAME_CENTER\",\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"source\":\"H5\",\"taskId\":\"" + taskId + "\",\"taskSceneCode\":\"ANTFARM_FAMILY_DRAW_TASK\"}]";
        return RequestManager.requestString("com.alipay.antfarm.receiveFarmTaskAward", args);
    }

    /**
     * 扭蛋任务查询好友列表
     */
    public static String familyShareP2PPanelInfo(String sceneCode) throws JSONException {
        JSONObject jo = new JSONObject();
        jo.put("requestType", "RPC");
        jo.put("source", "antfarm");
        jo.put("sceneCode", sceneCode);
        return RequestManager.requestString("com.alipay.antiep.shareP2PPanelInfo", new JSONArray().put(jo).toString());
    }

    /**
     * 扭蛋任务列表
     */
    public static String familyDrawListFarmTask() {
        String args = "[{\"bizType\":\"ANTFARM_GAME_CENTER\",\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM_FAMILY_DRAW_TASK\",\"signSceneCode\":\"\",\"source\":\"H5\",\"taskSceneCode\":\"ANTFARM_FAMILY_DRAW_TASK\"}]";
        return RequestManager.requestString("com.alipay.antfarm.listFarmTask", args);
    }

    public static String giftFamilyDrawFragment(String giftUserId, int giftNum) {
        String args = "[{\"bizType\":\"ANTFARM_GAME_CENTER\",\"giftNum\":" + giftNum + ",\"giftUserId\":\"" + giftUserId + "\",\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"source\":\"H5\"}]";
        return RequestManager.requestString("com.alipay.antfarm.giftFamilyDrawFragment", args);
    }

    public static String getMallHome() {
        String data = "[{\"bizType\":\"ANTFARM_GAME_CENTER\",\"pageSize\":10,\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"source\":\"H5\",\"startIndex\":0}]";
        return RequestManager.requestString("com.alipay.charitygamecenter.getMallHome", data);
    }

    public static String getMallItemDetail(String spuId) {
        String data = "[{\"bizType\":\"ANTFARM_GAME_CENTER\",\"itemId\":\"" + spuId + "\",\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"source\":\"H5\"}]";
        return RequestManager.requestString("com.alipay.charitygamecenter.getMallItemDetail", data);
    }

    public static String exchangeBenefit(String spuId, String skuId) {
        String data = "[{\"bizType\":\"ANTFARM_GAME_CENTER\",\"ignoreHoldLimit\":false,\"itemId\":\"" + spuId + "\",\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"source\":\"H5\",\"subItemId\":\"" + skuId + "\"}]";
        return RequestManager.requestString("com.alipay.charitygamecenter.buyMallItem", data);
    }

    /**
     * 领取活动食物
     *
     * @param foodType
     * @param giftIndex
     * @return
     */
    public static String clickForGiftV2(String foodType, int giftIndex) {
        String data = "[{\"foodType\":\"" + foodType + "\",\"giftIndex\":" + giftIndex + ",\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"source\":\"ANTFOREST\",\"version\":\"" + VERSION + "\"}]";
        return RequestManager.requestString("com.alipay.antfarm.clickForGiftV2", data);
    }
}
