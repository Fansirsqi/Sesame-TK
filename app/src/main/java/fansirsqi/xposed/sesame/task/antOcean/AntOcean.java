package fansirsqi.xposed.sesame.task.antOcean;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import fansirsqi.xposed.sesame.entity.AlipayBeach;
import fansirsqi.xposed.sesame.entity.AlipayUser;
import fansirsqi.xposed.sesame.model.BaseModel;
import fansirsqi.xposed.sesame.model.ModelFields;
import fansirsqi.xposed.sesame.model.ModelGroup;
import fansirsqi.xposed.sesame.model.modelFieldExt.BooleanModelField;
import fansirsqi.xposed.sesame.model.modelFieldExt.ChoiceModelField;
import fansirsqi.xposed.sesame.model.modelFieldExt.SelectAndCountModelField;
import fansirsqi.xposed.sesame.model.modelFieldExt.SelectModelField;
import fansirsqi.xposed.sesame.task.ModelTask;
import fansirsqi.xposed.sesame.task.TaskCommon;
import fansirsqi.xposed.sesame.task.antFarm.AntFarm.TaskStatus;
import fansirsqi.xposed.sesame.task.antForest.AntForestRpcCall;
import fansirsqi.xposed.sesame.util.Log;
import fansirsqi.xposed.sesame.util.Maps.UserMap;
import fansirsqi.xposed.sesame.util.ResUtil;
import fansirsqi.xposed.sesame.util.StringUtil;
import fansirsqi.xposed.sesame.util.ThreadUtil;
/**
 * @author Constanline
 * @since 2023/08/01
 */
public class AntOcean extends ModelTask {
    private static final String TAG = AntOcean.class.getSimpleName();
    @Override
    public String getName() {
        return "海洋";
    }
    @Override
    public ModelGroup getGroup() {
        return ModelGroup.FOREST;
    }
    @Override
    public String getIcon() {
        return "AntOcean.png";
    }
    /**每日海洋任务*/
    private BooleanModelField dailyOceanTask;
    /**领取碎片奖励*/
    private BooleanModelField receiveOceanTaskAward;
    /**清理 | 开启*/
    private BooleanModelField cleanOcean;
    /**清理 | 动作*/
    private ChoiceModelField cleanOceanType;
    /**清理 | 好友列表*/
    private SelectModelField cleanOceanList;
    /**神奇海洋 | 制作万能拼图*/
    private BooleanModelField exchangeProp;
    /**神奇海洋 | 使用万能拼图*/
    private BooleanModelField usePropByType;
    /**保护 | 开启*/
    private BooleanModelField protectOcean;
    /**保护 | 海洋列表*/
    private SelectAndCountModelField protectOceanList;
    @Override
    public ModelFields getFields() {
        ModelFields modelFields = new ModelFields();
        modelFields.addField(dailyOceanTask = new BooleanModelField("dailyOceanTask", "每日海洋任务", false));
        modelFields.addField(receiveOceanTaskAward = new BooleanModelField("receiveOceanTaskAward", "领取碎片奖励", false));
        modelFields.addField(cleanOcean = new BooleanModelField("cleanOcean", "清理 | 开启", false));
        modelFields.addField(cleanOceanType = new ChoiceModelField("cleanOceanType", "清理 | 动作", CleanOceanType.DONT_CLEAN, CleanOceanType.nickNames));
        modelFields.addField(cleanOceanList = new SelectModelField("cleanOceanList", "清理 | 好友列表", new LinkedHashSet<>(), AlipayUser::getList));
        modelFields.addField(exchangeProp = new BooleanModelField("exchangeProp", "神奇海洋 | 制作万能拼图", false));
        modelFields.addField(usePropByType = new BooleanModelField("usePropByType", "神奇海洋 | 使用万能拼图", false));
        modelFields.addField(protectOcean = new BooleanModelField("protectOcean", "保护 | 开启", false));
        modelFields.addField(protectOceanList = new SelectAndCountModelField("protectOceanList", "保护 | 海洋列表", new LinkedHashMap<>(), AlipayBeach::getList));
        return modelFields;
    }
    @Override
    public Boolean check() {
        if (TaskCommon.IS_ENERGY_TIME){
            Log.record("⏸ 当前为只收能量时间【"+ BaseModel.getEnergyTime().getValue() +"】，停止执行" + getName() + "任务！");
            return false;
        }else if (TaskCommon.IS_MODULE_SLEEP_TIME) {
            Log.record("💤 模块休眠时间【"+ BaseModel.getModelSleepTime().getValue() +"】停止执行" + getName() + "任务！");
            return false;
        } else {
            return true;
        }
    }
    @Override
    public void run() {
        try {
            Log.record("执行开始-" + getName());
            String s = AntOceanRpcCall.queryOceanStatus();
            JSONObject jo = new JSONObject(s);
            if (ResUtil.checkResultCode(jo)) {
                if (jo.getBoolean("opened")) {
                    queryHomePage();
                } else {
                    getEnableField().setValue(false);
                    Log.other("请先开启神奇海洋，并完成引导教程");
                }
            } else {
                Log.runtime(TAG, jo.getString("resultDesc"));
            }
            if (protectOcean.getValue()) {
                protectOcean();
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "start.run err:");
            Log.printStackTrace(TAG, t);
        }
        finally {
            Log.record("执行结束-" + getName());
        }
    }
    private void queryHomePage() {
        try {
            JSONObject joHomePage = new JSONObject(AntOceanRpcCall.queryHomePage());
            if (ResUtil.checkResultCode(joHomePage)) {
                if (joHomePage.has("bubbleVOList")) {
                    collectEnergy(joHomePage.getJSONArray("bubbleVOList"));
                }
                JSONObject userInfoVO = joHomePage.getJSONObject("userInfoVO");
                int rubbishNumber = userInfoVO.optInt("rubbishNumber", 0);
                String userId = userInfoVO.getString("userId");
                cleanOcean(userId, rubbishNumber);
                JSONObject ipVO = userInfoVO.optJSONObject("ipVO");
                if (ipVO != null) {
                    int surprisePieceNum = ipVO.optInt("surprisePieceNum", 0);
                    if (surprisePieceNum > 0) {
                        ipOpenSurprise();
                    }
                }
                queryReplicaHome();
                queryMiscInfo();
                queryUserRanking();
                querySeaAreaDetailList();
                if (dailyOceanTask.getValue()) {
                    doOceanDailyTask();
                }
                if (receiveOceanTaskAward.getValue()) {
                    receiveTaskAward();
                }
                // 制作万能碎片
                if (exchangeProp.getValue()) {
                    exchangeProp();
                }
                // 使用万能拼图
                if (usePropByType.getValue()) {
                    usePropByType();
                }
            } else {
                Log.runtime(TAG, joHomePage.getString("resultDesc"));
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "queryHomePage err:");
            Log.printStackTrace(TAG, t);
        }
    }
    private static void collectEnergy(JSONArray bubbleVOList) {
        try {
            for (int i = 0; i < bubbleVOList.length(); i++) {
                JSONObject bubble = bubbleVOList.getJSONObject(i);
                if (!"ocean".equals(bubble.getString("channel"))) {
                    continue;
                }
                if ("AVAILABLE".equals(bubble.getString("collectStatus"))) {
                    long bubbleId = bubble.getLong("id");
                    String userId = bubble.getString("userId");
                    String s = AntForestRpcCall.collectEnergy(null, userId, bubbleId);
                    JSONObject jo = new JSONObject(s);
                    if (ResUtil.checkResultCode(jo)) {
                        JSONArray retBubbles = jo.optJSONArray("bubbles");
                        if (retBubbles != null) {
                            for (int j = 0; j < retBubbles.length(); j++) {
                                JSONObject retBubble = retBubbles.optJSONObject(j);
                                if (retBubble != null) {
                                    int collectedEnergy = retBubble.getInt("collectedEnergy");
                                    Log.forest("神奇海洋🌊收取[" + UserMap.getMaskName(userId) + "]的海洋能量#"
                                            + collectedEnergy + "g");
                                }
                            }
                        }
                    } else {
                        Log.runtime(TAG, jo.getString("resultDesc"));
                    }
                }
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "queryHomePage err:");
            Log.printStackTrace(TAG, t);
        }
    }
    private static void cleanOcean(String userId, int rubbishNumber) {
        try {
            for (int i = 0; i < rubbishNumber; i++) {
                String s = AntOceanRpcCall.cleanOcean(userId);
                JSONObject jo = new JSONObject(s);
                if (ResUtil.checkResultCode(jo)) {
                    JSONArray cleanRewardVOS = jo.getJSONArray("cleanRewardVOS");
                    checkReward(cleanRewardVOS);
                    Log.forest("神奇海洋🌊[清理:" + UserMap.getMaskName(userId) + "海域]");
                } else {
                    Log.runtime(TAG, jo.getString("resultDesc"));
                }
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "cleanOcean err:");
            Log.printStackTrace(TAG, t);
        }
    }
    private static void ipOpenSurprise() {
        try {
            String s = AntOceanRpcCall.ipOpenSurprise();
            JSONObject jo = new JSONObject(s);
            if (ResUtil.checkResultCode(jo)) {
                JSONArray rewardVOS = jo.getJSONArray("surpriseRewardVOS");
                checkReward(rewardVOS);
            } else {
                Log.runtime(TAG, jo.getString("resultDesc"));
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "ipOpenSurprise err:");
            Log.printStackTrace(TAG, t);
        }
    }
    private static void combineFish(String fishId) {
        try {
            String s = AntOceanRpcCall.combineFish(fishId);
            JSONObject jo = new JSONObject(s);
            if (ResUtil.checkResultCode(jo)) {
                JSONObject fishDetailVO = jo.getJSONObject("fishDetailVO");
                String name = fishDetailVO.getString("name");
                Log.forest("神奇海洋🌊[" + name + "]合成成功");
            } else {
                Log.runtime(TAG, jo.getString("resultDesc"));
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "combineFish err:");
            Log.printStackTrace(TAG, t);
        }
    }
    private static void checkReward(JSONArray rewards) {
        try {
            for (int i = 0; i < rewards.length(); i++) {
                JSONObject reward = rewards.getJSONObject(i);
                String name = reward.getString("name");
                JSONArray attachReward = reward.getJSONArray("attachRewardBOList");
                if (attachReward.length() > 0) {
                    Log.forest("神奇海洋🌊[获得:" + name + "碎片]");
                    boolean canCombine = true;
                    for (int j = 0; j < attachReward.length(); j++) {
                        JSONObject detail = attachReward.getJSONObject(j);
                        if (detail.optInt("count", 0) == 0) {
                            canCombine = false;
                            break;
                        }
                    }
                    if (canCombine && reward.optBoolean("unlock", false)) {
                        String fishId = reward.getString("id");
                        combineFish(fishId);
                    }
                }
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "checkReward err:");
            Log.printStackTrace(TAG, t);
        }
    }
    private static void collectReplicaAsset(int canCollectAssetNum) {
        try {
            for (int i = 0; i < canCollectAssetNum; i++) {
                String s = AntOceanRpcCall.collectReplicaAsset();
                JSONObject jo = new JSONObject(s);
                if (ResUtil.checkResultCode(jo)) {
                    Log.forest("神奇海洋🌊[学习海洋科普知识]#潘多拉能量+1");
                } else {
                    Log.runtime(TAG, jo.getString("resultDesc"));
                }
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "collectReplicaAsset err:");
            Log.printStackTrace(TAG, t);
        }
    }
    private static void unLockReplicaPhase(String replicaCode, String replicaPhaseCode) {
        try {
            String s = AntOceanRpcCall.unLockReplicaPhase(replicaCode, replicaPhaseCode);
            JSONObject jo = new JSONObject(s);
            if (ResUtil.checkResultCode(jo)) {
                String name = jo.getJSONObject("currentPhaseInfo").getJSONObject("extInfo").getString("name");
                Log.forest("神奇海洋🌊迎回[" + name + "]");
            } else {
                Log.runtime(TAG, jo.getString("resultDesc"));
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "unLockReplicaPhase err:");
            Log.printStackTrace(TAG, t);
        }
    }
    private static void queryReplicaHome() {
        try {
            String s = AntOceanRpcCall.queryReplicaHome();
            JSONObject jo = new JSONObject(s);
            if (ResUtil.checkResultCode(jo)) {
                if (jo.has("userReplicaAssetVO")) {
                    JSONObject userReplicaAssetVO = jo.getJSONObject("userReplicaAssetVO");
                    int canCollectAssetNum = userReplicaAssetVO.getInt("canCollectAssetNum");
                    collectReplicaAsset(canCollectAssetNum);
                }
                if (jo.has("userCurrentPhaseVO")) {
                    JSONObject userCurrentPhaseVO = jo.getJSONObject("userCurrentPhaseVO");
                    String phaseCode = userCurrentPhaseVO.getString("phaseCode");
                    String code = jo.getJSONObject("userReplicaInfoVO").getString("code");
                    if ("COMPLETED".equals(userCurrentPhaseVO.getString("phaseStatus"))) {
                        unLockReplicaPhase(code, phaseCode);
                    }
                }
            } else {
                Log.runtime(TAG, jo.getString("resultDesc"));
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "queryReplicaHome err:");
            Log.printStackTrace(TAG, t);
        }
    }
    private static void queryOceanPropList() {
        try {
            String s = AntOceanRpcCall.queryOceanPropList();
            JSONObject jo = new JSONObject(s);
            if (ResUtil.checkResultCode(jo)) {
                AntOceanRpcCall.repairSeaArea();
            } else {
                Log.runtime(TAG, jo.getString("resultDesc"));
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "queryOceanPropList err:");
            Log.printStackTrace(TAG, t);
        }
    }
    private static void switchOceanChapter() {
        String s = AntOceanRpcCall.queryOceanChapterList();
        try {
            JSONObject jo = new JSONObject(s);
            if (ResUtil.checkResultCode(jo)) {
                String currentChapterCode = jo.getString("currentChapterCode");
                JSONArray chapterVOs = jo.getJSONArray("userChapterDetailVOList");
                boolean isFinish = false;
                String dstChapterCode = "";
                String dstChapterName = "";
                for (int i = 0; i < chapterVOs.length(); i++) {
                    JSONObject chapterVO = chapterVOs.getJSONObject(i);
                    int repairedSeaAreaNum = chapterVO.getInt("repairedSeaAreaNum");
                    int seaAreaNum = chapterVO.getInt("seaAreaNum");
                    if (chapterVO.getString("chapterCode").equals(currentChapterCode)) {
                        isFinish = repairedSeaAreaNum >= seaAreaNum;
                    } else {
                        if (repairedSeaAreaNum >= seaAreaNum || !chapterVO.getBoolean("chapterOpen")) {
                            continue;
                        }
                        dstChapterName = chapterVO.getString("chapterName");
                        dstChapterCode = chapterVO.getString("chapterCode");
                    }
                }
                if (isFinish && !StringUtil.isEmpty(dstChapterCode)) {
                    s = AntOceanRpcCall.switchOceanChapter(dstChapterCode);
                    jo = new JSONObject(s);
                    if (ResUtil.checkResultCode(jo)) {
                        Log.forest("神奇海洋🌊切换到[" + dstChapterName + "]系列");
                    } else {
                        Log.runtime(TAG, jo.getString("resultDesc"));
                    }
                }
            } else {
                Log.runtime(TAG, jo.getString("resultDesc"));
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "queryUserRanking err:");
            Log.printStackTrace(TAG, t);
        }
    }
    private static void querySeaAreaDetailList() {
        try {
            String s = AntOceanRpcCall.querySeaAreaDetailList();
            JSONObject jo = new JSONObject(s);
            if (ResUtil.checkResultCode(jo)) {
                int seaAreaNum = jo.getInt("seaAreaNum");
                int fixSeaAreaNum = jo.getInt("fixSeaAreaNum");
                int currentSeaAreaIndex = jo.getInt("currentSeaAreaIndex");
                if (currentSeaAreaIndex < fixSeaAreaNum && seaAreaNum > fixSeaAreaNum) {
                    queryOceanPropList();
                }
                JSONArray seaAreaVOs = jo.getJSONArray("seaAreaVOs");
                for (int i = 0; i < seaAreaVOs.length(); i++) {
                    JSONObject seaAreaVO = seaAreaVOs.getJSONObject(i);
                    JSONArray fishVOs = seaAreaVO.getJSONArray("fishVO");
                    for (int j = 0; j < fishVOs.length(); j++) {
                        JSONObject fishVO = fishVOs.getJSONObject(j);
                        if (!fishVO.getBoolean("unlock") && "COMPLETED".equals(fishVO.getString("status"))) {
                            String fishId = fishVO.getString("id");
                            combineFish(fishId);
                        }
                    }
                }
            } else {
                Log.runtime(TAG, jo.getString("resultDesc"));
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "querySeaAreaDetailList err:");
            Log.printStackTrace(TAG, t);
        }
    }
    private static void queryMiscInfo() {
        try {
            String s = AntOceanRpcCall.queryMiscInfo();
            JSONObject jo = new JSONObject(s);
            if (ResUtil.checkResultCode(jo)) {
                JSONObject miscHandlerVOMap = jo.getJSONObject("miscHandlerVOMap");
                JSONObject homeTipsRefresh = miscHandlerVOMap.getJSONObject("HOME_TIPS_REFRESH");
                if (homeTipsRefresh.optBoolean("fishCanBeCombined") || homeTipsRefresh.optBoolean("canBeRepaired")) {
                    querySeaAreaDetailList();
                }
                switchOceanChapter();
            } else {
                Log.runtime(TAG, jo.getString("resultDesc"));
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "queryMiscInfo err:");
            Log.printStackTrace(TAG, t);
        }
    }
    private void cleanFriendOcean(JSONObject fillFlag) {
        if (!fillFlag.optBoolean("canClean")) {
            return;
        }
        try {
            String userId = fillFlag.getString("userId");
            boolean isOceanClean = cleanOceanList.getValue().contains(userId);
            if (cleanOceanType.getValue() == CleanOceanType.DONT_CLEAN) {
                isOceanClean = !isOceanClean;
            }
            if (!isOceanClean) {
                return;
            }
            String s = AntOceanRpcCall.queryFriendPage(userId);
            JSONObject jo = new JSONObject(s);
            if (ResUtil.checkResultCode(jo)) {
                s = AntOceanRpcCall.cleanFriendOcean(userId);
                jo = new JSONObject(s);
                Log.forest("神奇海洋🌊[帮助:" + UserMap.getMaskName
                        (userId) + "清理海域]");
                if (ResUtil.checkResultCode(jo)) {
                    JSONArray cleanRewardVOS = jo.getJSONArray("cleanRewardVOS");
                    checkReward(cleanRewardVOS);
                } else {
                    Log.runtime(TAG, jo.getString("resultDesc"));
                }
            } else {
                Log.runtime(TAG, jo.getString("resultDesc"));
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "queryMiscInfo err:");
            Log.printStackTrace(TAG, t);
        }
    }
    private void queryUserRanking() {
        try {
            String s = AntOceanRpcCall.queryUserRanking();
            JSONObject jo = new JSONObject(s);
            if (ResUtil.checkResultCode(jo)) {
                JSONArray fillFlagVOList = jo.getJSONArray("fillFlagVOList");
                for (int i = 0; i < fillFlagVOList.length(); i++) {
                    JSONObject fillFlag = fillFlagVOList.getJSONObject(i);
                    if (cleanOcean.getValue()) {
                        cleanFriendOcean(fillFlag);
                    }
                }
            } else {
                Log.runtime(TAG, jo.getString("resultDesc"));
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "queryMiscInfo err:");
            Log.printStackTrace(TAG, t);
        }
    }
    @SuppressWarnings("unused")
    private static boolean isTargetTask(String taskType) {
        // 在这里添加其他任务类型，以便后续扩展
        return "DAOLIU_TAOJINBI".equals(taskType) // 去逛淘金币看淘金仔
                || "DAOLIU_NNYY".equals(taskType) // 逛余额宝新春活动
                || "ANTOCEAN_TASK#DAOLIU_GUANGHUABEIBANGHAI".equals(taskType) // 逛逛花呗活动会场
                || "BUSINESS_LIGHTS01".equals(taskType) // 逛一逛市集15s
                || "DAOLIU_ELEMEGUOYUAN".equals(taskType) // 去逛饿了么夺宝
                || "ZHUANHUA_NONGCHANGYX".equals(taskType) // 去玩趣味小游戏
                || "ZHUANHUA_HUIYUN_OZB".equals(taskType); // 一键传球欧洲杯
    }
    private static void doOceanDailyTask() {
        try {
            JSONObject jo = new JSONObject(AntOceanRpcCall.queryTaskList());
            if (ResUtil.checkResultCode(jo)) {
                JSONArray jaTaskList = jo.getJSONArray("antOceanTaskVOList");
                for (int i = 0; i < jaTaskList.length(); i++) {
                    JSONObject taskJson = jaTaskList.getJSONObject(i);
                    if (TaskStatus.TODO.name().equals(taskJson.getString("taskStatus"))){
                        finishTask(taskJson);
                    }
                    ThreadUtil.sleep(500);
                }
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "doOceanDailyTask err:");
            Log.printStackTrace(TAG, t);
        }
    }
    private static void finishTask(JSONObject task) {
        try {
            if (task.has("taskProgress")) {
                return;
            }
            JSONObject bizInfo = new JSONObject(task.getString("bizInfo"));
            String taskTitle = bizInfo.optString("taskTitle");
            if (taskTitle.contains("答题学海洋知识")) {
                // 答题操作
                answerQuestion();
            } else if (taskTitle.startsWith("随机任务：") || taskTitle.startsWith("绿色任务：")) {
                String sceneCode = task.getString("sceneCode");
                String taskType = task.getString("taskType");
                if(Objects.equals(taskType,"mokuai_senlin_hy")){
                    return;
                }
                int rightsTimes = task.optInt("rightsTimes", 1);
                int rightsTimesLimit = task.optInt("rightsTimesLimit", 1);
                int times = rightsTimesLimit - rightsTimes;
                for (int i = 0; i < times; i++) {
                    JSONObject jo = new JSONObject(AntOceanRpcCall.finishTask(sceneCode, taskType));
                    if (ResUtil.checkSuccess(TAG, jo)) {
                        Log.forest("海洋任务🧾️完成[" + taskTitle + "]" + (times > 1 ? "#第" + (i + 1) + "次" : ""));
                    } else {
                        return;
                    }
                    ThreadUtil.sleep(2000);
                }
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "finishOceanTask err:");
            Log.printStackTrace(TAG, t);
        }
    }
    private static void receiveTaskAward() {
        try {
            JSONObject jo = new JSONObject(AntOceanRpcCall.queryTaskList());
            if (ResUtil.checkResultCode(jo)) {
                JSONArray jaTaskList = jo.getJSONArray("antOceanTaskVOList");
                for (int i = 0; i < jaTaskList.length(); i++) {
                    jo = jaTaskList.getJSONObject(i);
                    if (!TaskStatus.FINISHED.name().equals(jo.getString("taskStatus")))
                        continue;
                    JSONObject bizInfo = new JSONObject(jo.getString("bizInfo"));
                    String taskType = jo.getString("taskType");
                    String sceneCode = jo.getString("sceneCode");
                    jo = new JSONObject(AntOceanRpcCall.receiveTaskAward(sceneCode, taskType));
                    ThreadUtil.sleep(500);
                    if (jo.optBoolean("success")) {
                        String taskTitle = bizInfo.optString("taskTitle", taskType);
                        String awardCount = bizInfo.optString("awardCount", "0");
                        Log.forest("海洋奖励🎖️[" + taskTitle + "]得:#" + awardCount + "碎片");
                        // 潘多拉任务领取
                        doOceanPDLTask();
                    } else {
                        Log.record(jo.getString("desc"));
                        Log.runtime(jo.toString());
                    }
                }
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "receiveTaskAward err:");
            Log.printStackTrace(TAG, t);
        }
    }
    // 海洋答题任务
    private static void answerQuestion() {
        try {
            String questionResponse = AntOceanRpcCall.getQuestion();
            JSONObject questionJson = new JSONObject(questionResponse);
            if (questionJson.getBoolean("answered")) {
                Log.record("问题已经被回答过，跳过答题流程");
                return;
            }
            if (questionJson.getInt("resultCode") == 200) {
                String questionId = questionJson.getString("questionId");
                JSONArray options = questionJson.getJSONArray("options");
                String answer = options.getString(0);
                String submitResponse = AntOceanRpcCall.submitAnswer(answer, questionId);
                ThreadUtil.sleep(500);
                JSONObject submitJson = new JSONObject(submitResponse);
                if (submitJson.getInt("resultCode") == 200) {
                    Log.record("海洋答题成功");
                } else {
                    Log.record("答题失败：" + submitJson.getString("resultMsg"));
                }
            } else {
                Log.record("获取问题失败：" + questionJson.getString("resultMsg"));
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "answerQuestion err:");
            Log.printStackTrace(TAG, t);
        }
    }
    /**潘多拉海洋任务领取*/
    private static void doOceanPDLTask() {
        try {
            String homeResponse = AntOceanRpcCall.PDLqueryReplicaHome();
            JSONObject homeJson = new JSONObject(homeResponse);
            if (ResUtil.checkResultCode(homeJson)) {
                String taskListResponse = AntOceanRpcCall.PDLqueryTaskList();
                ThreadUtil.sleep(300);
                JSONObject taskListJson = new JSONObject(taskListResponse);
                JSONArray antOceanTaskVOList = taskListJson.getJSONArray("antOceanTaskVOList");
                for (int i = 0; i < antOceanTaskVOList.length(); i++) {
                    JSONObject task = antOceanTaskVOList.getJSONObject(i);
                    String taskStatus = task.getString("taskStatus");
                    if ("FINISHED".equals(taskStatus)) {
                        String bizInfoString = task.getString("bizInfo");
                        JSONObject bizInfo = new JSONObject(bizInfoString);
                        String taskTitle = bizInfo.getString("taskTitle");
                        int awardCount = bizInfo.getInt("awardCount");
                        String taskType = task.getString("taskType");
                        String receiveTaskResponse = AntOceanRpcCall.PDLreceiveTaskAward(taskType);
                        ThreadUtil.sleep(300);
                        JSONObject receiveTaskJson = new JSONObject(receiveTaskResponse);
                        int code = receiveTaskJson.getInt("code");
                        if (code == 100000000) {
                            Log.forest("海洋奖励🌊[领取:" + taskTitle + "]获得潘多拉能量x" + awardCount);
                        } else {
                            if (receiveTaskJson.has("message")) {
                                Log.record("领取任务奖励失败: " + receiveTaskJson.getString("message"));
                            } else {
                                Log.record("领取任务奖励失败，未返回错误信息");
                            }
                        }
                    }
                }
            } else {
                Log.record("PDLqueryReplicaHome调用失败: " + homeJson.optString("message"));
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "doOceanPDLTask err:");
            Log.printStackTrace(TAG, t);
        }
    }
    private void protectOcean() {
        try {
            String s = AntOceanRpcCall.queryCultivationList();
            JSONObject jo = new JSONObject(s);
            if (ResUtil.checkResultCode(jo)) {
                JSONArray ja = jo.getJSONArray("cultivationItemVOList");
                for (int i = 0; i < ja.length(); i++) {
                    jo = ja.getJSONObject(i);
                    if (!jo.has("templateSubType")) {
                        continue;
                    }
                    if (!"BEACH".equals(jo.getString("templateSubType"))
                            && !"COOPERATE_SEA_TREE".equals(jo.getString("templateSubType")) && !"SEA_ANIMAL".equals(jo.getString("templateSubType"))) {
                        continue;
                    }
                    if (!"AVAILABLE".equals(jo.getString("applyAction"))) {
                        continue;
                    }
                    String cultivationName = jo.getString("cultivationName");
                    String templateCode = jo.getString("templateCode");
                    JSONObject projectConfig = jo.getJSONObject("projectConfigVO");
                    String projectCode = projectConfig.getString("code");
                    Map<String, Integer> map = protectOceanList.getValue();
                    for (Map.Entry<String, Integer> entry : map.entrySet()) {
                        if (Objects.equals(entry.getKey(), templateCode)) {
                            Integer count = entry.getValue();
                            if (count != null && count > 0) {
                                oceanExchangeTree(templateCode, projectCode, cultivationName, count);
                            }
                            break;
                        }
                    }
                }
            } else {
                Log.runtime(TAG, jo.getString("resultDesc"));
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "protectBeach err:");
            Log.printStackTrace(TAG, t);
        }
    }
    private static void oceanExchangeTree(String cultivationCode, String projectCode, String itemName, int count) {
        try {
            String s;
            JSONObject jo;
            int appliedTimes = queryCultivationDetail(cultivationCode, projectCode, count);
            if (appliedTimes < 0)
                return;
            for (int applyCount = 1; applyCount <= count; applyCount++) {
                s = AntOceanRpcCall.oceanExchangeTree(cultivationCode, projectCode);
                jo = new JSONObject(s);
                if (ResUtil.checkResultCode(jo)) {
                    JSONArray awardInfos = jo.getJSONArray("rewardItemVOs");
                    StringBuilder award = new StringBuilder();
                    for (int i = 0; i < awardInfos.length(); i++) {
                        jo = awardInfos.getJSONObject(i);
                        award.append(jo.getString("name")).append("*").append(jo.getInt("num"));
                    }
                    String str = "保护海洋🏖️[" + itemName + "]#第" + appliedTimes + "次"
                            + "-获得奖励" + award;
                    Log.forest(str);
                } else {
                    Log.record(jo.getString("resultDesc"));
                    Log.runtime(jo.toString());
                    Log.forest("保护海洋🏖️[" + itemName + "]#发生未知错误，停止申请");
                    break;
                }
                ThreadUtil.sleep(300);
                appliedTimes = queryCultivationDetail(cultivationCode, projectCode, count);
                if (appliedTimes < 0) {
                    break;
                } else {
                    ThreadUtil.sleep(300);
                }
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "oceanExchangeTree err:");
            Log.printStackTrace(TAG, t);
        }
    }
    private static int queryCultivationDetail(String cultivationCode, String projectCode, int count) {
        int appliedTimes = -1;
        try {
            String s = AntOceanRpcCall.queryCultivationDetail(cultivationCode, projectCode);
            JSONObject jo = new JSONObject(s);
            if (ResUtil.checkResultCode(jo)) {
                JSONObject userInfo = jo.getJSONObject("userInfoVO");
                int currentEnergy = userInfo.getInt("currentEnergy");
                jo = jo.getJSONObject("cultivationDetailVO");
                String applyAction = jo.getString("applyAction");
                int certNum = jo.getInt("certNum");
                if ("AVAILABLE".equals(applyAction)) {
                    if (currentEnergy >= jo.getInt("energy")) {
                        if (certNum < count) {
                            appliedTimes = certNum + 1;
                        }
                    } else {
                        Log.forest("保护海洋🏖️[" + jo.getString("cultivationName") + "]#能量不足停止申请");
                    }
                } else {
                    Log.forest("保护海洋🏖️[" + jo.getString("cultivationName") + "]#似乎没有了");
                }
            } else {
                Log.record(jo.getString("resultDesc"));
                Log.runtime(s);
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "queryCultivationDetail err:");
            Log.printStackTrace(TAG, t);
        }
        return appliedTimes;
    }
    // 制作万能碎片
    private static void exchangeProp() {
        try {
            boolean shouldContinue = true;
            while (shouldContinue) {
                // 获取道具兑换列表的JSON数据
                String propListJson = AntOceanRpcCall.exchangePropList();
                JSONObject propListObj = new JSONObject(propListJson);
                // 检查是否成功获取道具列表
                if (ResUtil.checkResultCode(propListObj)) {
                    // 获取道具重复数量
                    int duplicatePieceNum = propListObj.getInt("duplicatePieceNum");
                    // 如果道具重复数量小于10，直接返回并停止循环
                    if (duplicatePieceNum < 10) {
                        shouldContinue = false;
                        return;
                    }
                    // 如果道具重复数量大于等于10，则执行道具兑换操作
                    String exchangeResultJson = AntOceanRpcCall.exchangeProp();
                    JSONObject exchangeResultObj = new JSONObject(exchangeResultJson);
                    // 获取兑换后的碎片数量和兑换数量
                    String exchangedPieceNum = exchangeResultObj.getString("duplicatePieceNum");
                    String exchangeNum = exchangeResultObj.getString("exchangeNum");
                    // 检查道具兑换操作是否成功
                    if (ResUtil.checkResultCode(exchangeResultObj)) {
                        // 输出日志信息
                        Log.forest("神奇海洋🏖️[万能拼图]制作" + exchangeNum + "张,剩余" + exchangedPieceNum + "张碎片");
                        // 制作完成后休眠1秒钟
                        ThreadUtil.sleep(1000);
                    }
                } else {
                    // 如果未成功获取道具列表，停止循环
                    shouldContinue = false;
                }
            }
        } catch (Throwable t) {
            // 捕获并记录异常
            Log.runtime(TAG, "exchangeProp error:");
            Log.printStackTrace(TAG, t);
        }
    }
    // 使用万能拼图
    private static void usePropByType() {
        try {
            // 获取道具使用类型列表的JSON数据
            String propListJson = AntOceanRpcCall.usePropByTypeList();
            JSONObject propListObj = new JSONObject(propListJson); // 使用 JSONObject 解析返回的 JSON 数据
            if (ResUtil.checkResultCode(propListObj)) {
                // 获取道具类型列表中的holdsNum值
                JSONArray oceanPropVOByTypeList = propListObj.getJSONArray("oceanPropVOByTypeList"); // 获取数组中的数据
                // 遍历每个道具类型信息
                for (int i = 0; i < oceanPropVOByTypeList.length(); i++) {
                    JSONObject propInfo = oceanPropVOByTypeList.getJSONObject(i);
                    int holdsNum = propInfo.getInt("holdsNum");
                    // 只要holdsNum大于0，就继续执行循环操作
                    int pageNum = 0;
                    th:
                    while (holdsNum > 0) {
                        // 查询鱼列表的JSON数据
                        pageNum++;
                        String fishListJson = AntOceanRpcCall.queryFishList(pageNum);
                        JSONObject fishListObj = new JSONObject(fishListJson);
                        // 检查是否成功获取到鱼列表并且 hasMore 为 true
                        if (!ResUtil.checkResultCode(fishListObj)) {
                            // 如果没有成功获取到鱼列表或者 hasMore 为 false，则停止后续操作
                            break;
                        }
                        // 获取鱼列表中的fishVOS数组
                        JSONArray fishVOS = fishListObj.optJSONArray("fishVOS");
                        if (fishVOS == null) {
                            break;
                        }
                        // 遍历fishVOS数组，寻找pieces中num值为0的鱼的order和id
                        for (int j = 0; j < fishVOS.length(); j++) {
                            JSONObject fish = fishVOS.getJSONObject(j);
                            JSONArray pieces = fish.optJSONArray("pieces");
                            if (pieces == null) {
                                continue;
                            }
                            int order = fish.getInt("order");
                            String name = fish.getString("name");
                            Set<Integer> idSet = new HashSet<>();
                            for (int k = 0; k < pieces.length(); k++) {
                                JSONObject piece = pieces.getJSONObject(k);
                                if (piece.optInt("num") == 0) {
                                    idSet.add(Integer.parseInt(piece.getString("id")));
                                    holdsNum--;
                                    if (holdsNum <= 0) {
                                        break;
                                    }
                                }
                            }
                            if (!idSet.isEmpty()) {
                                String usePropResult = AntOceanRpcCall.usePropByType(order, idSet);
                                JSONObject usePropResultObj = new JSONObject(usePropResult);
                                if (ResUtil.checkResultCode(usePropResultObj)) {
                                    int userCount = idSet.size();
                                    Log.forest("神奇海洋🏖️[万能拼图]使用" + userCount + "张，获得[" + name + "]剩余" + holdsNum + "张");
                                    ThreadUtil.sleep(1000);
                                    if (holdsNum <= 0) {
                                        break th;
                                    }
                                }
                            }
                        }
                        if (!fishListObj.optBoolean("hasMore")) {
                            break;
                        }
                    }
                }
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "usePropByType error:");
            Log.printStackTrace(TAG, t);
        }
    }
    @SuppressWarnings("unused")
    public interface CleanOceanType {
        int CLEAN = 0;
        int DONT_CLEAN = 1;
        String[] nickNames = {"选中清理", "选中不清理"};
    }
}
