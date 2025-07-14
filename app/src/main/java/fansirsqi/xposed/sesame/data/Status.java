package fansirsqi.xposed.sesame.data;
import com.fasterxml.jackson.databind.JsonMappingException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import fansirsqi.xposed.sesame.task.ModelTask;
import fansirsqi.xposed.sesame.task.antForest.AntForest;
import fansirsqi.xposed.sesame.util.Files;
import fansirsqi.xposed.sesame.util.JsonUtil;
import fansirsqi.xposed.sesame.util.Log;
import fansirsqi.xposed.sesame.util.Maps.UserMap;
import fansirsqi.xposed.sesame.util.StringUtil;
import fansirsqi.xposed.sesame.util.TimeUtil;
import lombok.Data;
@Data
public class Status {
    private static final String TAG = Status.class.getSimpleName();
    public static final Status INSTANCE = new Status();
    // ===========================forest
    private Map<String, Integer> waterFriendLogList = new HashMap<>();
    private Set<String> cooperateWaterList = new HashSet<>();//合作浇水
    private Map<String, Integer> reserveLogList = new HashMap<>();
    private Set<String> ancientTreeCityCodeList = new HashSet<>();//古树
    private Set<String> protectBubbleList = new HashSet<>();
    private int exchangeDoubleCard = 0; // 活力值兑换双倍卡
    private int exchangeTimes = 0;
    private int exchangeTimesLongTime = 0;
    private int doubleTimes = 0;
    private boolean exchangeEnergyShield = false; //活力值兑换能量保护罩
    private boolean exchangeCollectHistoryAnimal7Days = false;
    private boolean exchangeCollectToFriendTimes7Days = false;
    private boolean youthPrivilege = true;
    private boolean studentTask = true;
    private Map<String, Integer> VitalityStoreList = new HashMap<>();
    // ===========================farm
    private Boolean answerQuestion = false;
    private Map<String, Integer> feedFriendLogList = new HashMap<>();
    private Map<String, Integer> visitFriendLogList = new HashMap<>();
    private Set<String> dailyAnswerList = new HashSet<>();
    private Set<String> donationEggList = new HashSet<>();
    private int useAccelerateToolCount = 0;
    /**
     * 小鸡换装
     */
    private Boolean canOrnament = true;
    private Boolean animalSleep = false;
    // =============================stall
    private Map<String, Integer> stallHelpedCountLogList = new HashMap<>();
    private Set<String> spreadManureList = new HashSet<>();
    private Set<String> stallP2PHelpedList = new HashSet<>();
    private Boolean canStallDonate = true;
    // ==========================sport
    private Set<String> syncStepList = new HashSet<>();
    private Set<String> exchangeList = new HashSet<>();
    /**
     * 捐运动币
     */
    private boolean donateCharityCoin = false;
    // =======================other
    private Set<String> memberSignInList = new HashSet<>();
    private final Set<String> flagList = new HashSet<>();
    /**
     * 口碑签到
     */
    private int kbSignIn = 0;
    /**
     * 保存时间
     */
    private Long saveTime = 0L;
    /**
     * 新村助力好友，已上限的用户
     */
    private Set<String> antStallAssistFriend = new HashSet<>();
    /**
     * 新村-罚单已贴完的用户
     */
    private Set<String> canPasteTicketTime = new HashSet<>();
    /**
     * 绿色经营，收取好友金币已完成用户
     */
    private Set<String> greenFinancePointFriend = new HashSet<>();
    /**
     * 绿色经营，评级领奖已完成用户
     */
    private Map<String, Integer> greenFinancePrizesMap = new HashMap<>();
    /**
     * 农场助力
     */
    private Set<String> antOrchardAssistFriend = new HashSet<>();

    /**
     * 会员权益
     */
    private Set<String> memberPointExchangeBenefitLogList = new HashSet<>();

    public static int getVitalityCount(String skuId) {
        Integer exchangedCount = INSTANCE.VitalityStoreList.get(skuId);
        if (exchangedCount == null) {
            exchangedCount = 0;
        }
        return exchangedCount;
    }
    public static Boolean canVitalityExchangeToday(String skuId, int count) {
        return !hasFlagToday("forest::VitalityExchangeLimit::" + skuId)
                && getVitalityCount(skuId) < count;
    }
    public static void vitalityExchangeToday(String skuId) {
        int count = getVitalityCount(skuId) + 1;
        INSTANCE.VitalityStoreList.put(skuId, count);
        save();
    }
    public static boolean canStudentTask() {
        return INSTANCE.studentTask;
    }
    public static void setStudentTaskToday() {
        if (INSTANCE.studentTask) {
            INSTANCE.studentTask = false;
            save();
        }
    }
    public static boolean canYouthPrivilegeToday() {
        return INSTANCE.youthPrivilege;
    }
    public static void setYouthPrivilegeToday() {
        if (INSTANCE.youthPrivilege) {
            INSTANCE.youthPrivilege = false;
            save();
        }
    }

    public static boolean canAnimalSleep() {
        return !INSTANCE.animalSleep;
    }
    public static void animalSleep() {
        if (!INSTANCE.animalSleep) {
            INSTANCE.animalSleep = true;
            save();
        }
    }
    public static boolean canWaterFriendToday(String id, int newCount) {
        id = UserMap.getCurrentUid() + "-" + id;
        Integer count = INSTANCE.waterFriendLogList.get(id);
        if (count == null) {
            return true;
        }
        return count < newCount;
    }
    public static void waterFriendToday(String id, int count) {
        id = UserMap.getCurrentUid() + "-" + id;
        INSTANCE.waterFriendLogList.put(id, count);
        save();
    }
    public static int getReserveTimes(String id) {
        Integer count = INSTANCE.reserveLogList.get(id);
        if (count == null) {
            return 0;
        }
        return count;
    }
    public static boolean canReserveToday(String id, int count) {
        return getReserveTimes(id) < count;
    }
    public static void reserveToday(String id, int newCount) {
        Integer count = INSTANCE.reserveLogList.get(id);
        if (count == null) {
            count = 0;
        }
        INSTANCE.reserveLogList.put(id, count + newCount);
        save();
    }
    public static boolean canCooperateWaterToday(String uid, String coopId) {
        return !INSTANCE.cooperateWaterList.contains(uid + "_" + coopId);
    }
    public static void cooperateWaterToday(String uid, String coopId) {
        String v = uid + "_" + coopId;
        if (!INSTANCE.cooperateWaterList.contains(v)) {
            INSTANCE.cooperateWaterList.add(v);
            save();
        }
    }
    public static boolean canAncientTreeToday(String cityCode) {
        return !INSTANCE.ancientTreeCityCodeList.contains(cityCode);
    }
    public static void ancientTreeToday(String cityCode) {
        if (!INSTANCE.ancientTreeCityCodeList.contains(cityCode)) {
            INSTANCE.ancientTreeCityCodeList.add(cityCode);
            save();
        }
    }
    public static boolean canAnswerQuestionToday() {
        return !INSTANCE.answerQuestion;
    }
    public static void answerQuestionToday() {
        if (!INSTANCE.answerQuestion) {
            INSTANCE.answerQuestion = true;
            save();
        }
    }
    public static boolean canFeedFriendToday(String id, int newCount) {
        Integer count = INSTANCE.feedFriendLogList.get(id);
        if (count == null) {
            return true;
        }
        return count < newCount;
    }
    public static void feedFriendToday(String id) {
        Integer count = INSTANCE.feedFriendLogList.get(id);
        if (count == null) {
            count = 0;
        }
        INSTANCE.feedFriendLogList.put(id, count + 1);
        save();
    }
    public static boolean canVisitFriendToday(String id, int newCount) {
        id = UserMap.getCurrentUid() + "-" + id;
        Integer count = INSTANCE.visitFriendLogList.get(id);
        if (count == null) {
            return true;
        }
        return count < newCount;
    }
    public static void visitFriendToday(String id, int newCount) {
        id = UserMap.getCurrentUid() + "-" + id;
        INSTANCE.visitFriendLogList.put(id, newCount);
        save();
    }

    public static boolean canMemberSignInToday(String uid) {
        return !INSTANCE.memberSignInList.contains(uid);
    }
    public static void memberSignInToday(String uid) {
        if (!INSTANCE.memberSignInList.contains(uid)) {
            INSTANCE.memberSignInList.add(uid);
            save();
        }
    }
    public static boolean canUseAccelerateTool() {
        return INSTANCE.useAccelerateToolCount < 8;
    }
    public static void useAccelerateTool() {
        INSTANCE.useAccelerateToolCount += 1;
        save();
    }
    public static boolean canDonationEgg(String uid) {
        return !INSTANCE.donationEggList.contains(uid);
    }
    public static void donationEgg(String uid) {
        if (!INSTANCE.donationEggList.contains(uid)) {
            INSTANCE.donationEggList.add(uid);
            save();
        }
    }
    public static boolean canSpreadManureToday(String uid) {
        return !INSTANCE.spreadManureList.contains(uid);
    }
    public static void spreadManureToday(String uid) {
        if (!INSTANCE.spreadManureList.contains(uid)) {
            INSTANCE.spreadManureList.add(uid);
            save();
        }
    }

    /**
     * 是否可以新村助力
     *
     * @return true是，false否
     */
    public static boolean canAntStallAssistFriendToday() {
        return !INSTANCE.antStallAssistFriend.contains(UserMap.getCurrentUid());
    }
    /**
     * 设置新村助力已到上限
     */
    public static void antStallAssistFriendToday() {
        String uid = UserMap.getCurrentUid();
        if (!INSTANCE.antStallAssistFriend.contains(uid)) {
            INSTANCE.antStallAssistFriend.add(uid);
            save();
        }
    }
    // 农场助力
    public static boolean canAntOrchardAssistFriendToday() {
        return !INSTANCE.antOrchardAssistFriend.contains(UserMap.getCurrentUid());
    }
    public static void antOrchardAssistFriendToday() {
        String uid = UserMap.getCurrentUid();
        if (!INSTANCE.antOrchardAssistFriend.contains(uid)) {
            INSTANCE.antOrchardAssistFriend.add(uid);
            save();
        }
    }
    public static boolean canProtectBubbleToday(String uid) {
        return !INSTANCE.protectBubbleList.contains(uid);
    }
    public static void protectBubbleToday(String uid) {
        if (!INSTANCE.protectBubbleList.contains(uid)) {
            INSTANCE.protectBubbleList.add(uid);
            save();
        }
    }
    /**
     * 是否可以贴罚单
     *
     * @return true是，false否
     */
    public static boolean canPasteTicketTime() {
        return !INSTANCE.canPasteTicketTime.contains(UserMap.getCurrentUid());
    }
    /**
     * 罚单贴完了
     */
    public static void pasteTicketTime() {
        if (INSTANCE.canPasteTicketTime.contains(UserMap.getCurrentUid())) {
            return;
        }
        INSTANCE.canPasteTicketTime.add(UserMap.getCurrentUid());
        save();
    }
    public static boolean canDoubleToday() {
        AntForest task = ModelTask.getModel(AntForest.class);
        if (task == null) {
            return false;
        }
        return INSTANCE.doubleTimes < task.getDoubleCountLimit().getValue();
    }
    public static void DoubleToday() {
        INSTANCE.doubleTimes += 1;
        save();
    }
    public static boolean canKbSignInToday() {
        return INSTANCE.kbSignIn < Statistics.INSTANCE.getDay().time;
    }
    public static void KbSignInToday() {
        if (INSTANCE.kbSignIn != Statistics.INSTANCE.getDay().time) {
            INSTANCE.kbSignIn = Statistics.INSTANCE.getDay().time;
            save();
        }
    }
    public static Set<String> getDadaDailySet() {
        return INSTANCE.dailyAnswerList;
    }
    public static void setDadaDailySet(Set<String> dailyAnswerList) {
        INSTANCE.dailyAnswerList = dailyAnswerList;
        save();
    }
    public static boolean canDonateCharityCoin() {
        return !INSTANCE.donateCharityCoin;
    }
    public static void donateCharityCoin() {
        if (!INSTANCE.donateCharityCoin) {
            INSTANCE.donateCharityCoin = true;
            save();
        }
    }
    public static boolean canSyncStepToday(String uid) {
        return !INSTANCE.syncStepList.contains(uid);
    }
    public static void SyncStepToday(String uid) {
        if (!INSTANCE.syncStepList.contains(uid)) {
            INSTANCE.syncStepList.add(uid);
            save();
        }
    }
    public static boolean canExchangeToday(String uid) {
        return !INSTANCE.exchangeList.contains(uid);
    }
    public static void exchangeToday(String uid) {
        if (!INSTANCE.exchangeList.contains(uid)) {
            INSTANCE.exchangeList.add(uid);
            save();
        }
    }
    /**
     * 绿色经营-是否可以收好友金币
     *
     * @return true是，false否
     */
    public static boolean canGreenFinancePointFriend() {
        return INSTANCE.greenFinancePointFriend.contains(UserMap.getCurrentUid());
    }
    /**
     * 绿色经营-收好友金币完了
     */
    public static void greenFinancePointFriend() {
        if (canGreenFinancePointFriend()) {
            return;
        }
        INSTANCE.greenFinancePointFriend.add(UserMap.getCurrentUid());
        save();
    }
    /**
     * 绿色经营-是否可以做评级任务
     *
     * @return true是，false否
     */
    public static boolean canGreenFinancePrizesMap() {
        int week = TimeUtil.getWeekNumber(new Date());
        String currentUid = UserMap.getCurrentUid();
        if (INSTANCE.greenFinancePrizesMap.containsKey(currentUid)) {
            Integer storedWeek = INSTANCE.greenFinancePrizesMap.get(currentUid);
            return storedWeek == null || storedWeek != week;
        }
        return true;
    }
    /**
     * 绿色经营-评级任务完了
     */
    public static void greenFinancePrizesMap() {
        if (!canGreenFinancePrizesMap()) {
            return;
        }
        INSTANCE.greenFinancePrizesMap.put(UserMap.getCurrentUid(), TimeUtil.getWeekNumber(new Date()));
        save();
    }
    /**
     * 加载状态文件
     *
     * @return 状态对象
     */
    public static synchronized Status load() {
        String currentUid = UserMap.getCurrentUid();
        if (StringUtil.isEmpty(currentUid)) {
            Log.runtime(TAG, "用户为空，状态加载失败");
            throw new RuntimeException("用户为空，状态加载失败");
        }
        try {
            java.io.File statusFile = Files.getStatusFile(currentUid);
            if (statusFile.exists()) {
                Log.runtime(TAG, "加载 status.json");
                String json = Files.readFromFile(statusFile);
                if (!json.trim().isEmpty()) {
                    JsonUtil.copyMapper().readerForUpdating(INSTANCE).readValue(json);
                    String formatted = JsonUtil.formatJson(INSTANCE);
                    if (formatted != null && !formatted.equals(json)) {
                        Log.runtime(TAG, "重新格式化 status.json");
                        Files.write2File(formatted, statusFile);
                    }
                } else {
                    Log.runtime(TAG, "配置文件为空，初始化默认配置");
                    initializeDefaultConfig(statusFile);
                }
            } else {
                Log.runtime(TAG, "配置文件不存在，初始化默认配置");
                initializeDefaultConfig(statusFile);
            }
        } catch (Throwable t) {
            Log.printStackTrace(TAG, t);
            Log.runtime(TAG, "状态文件格式有误，已重置");
            resetAndSaveConfig();
        }
        if (INSTANCE.saveTime == null) {
            INSTANCE.saveTime = System.currentTimeMillis();
        }
        return INSTANCE;
    }
    /**
     * 初始化默认配置
     *
     * @param statusFile 状态文件
     */
    private static void initializeDefaultConfig(java.io.File statusFile) {
        try {
            JsonUtil.copyMapper().updateValue(INSTANCE, new Status());
            Log.runtime(TAG, "初始化 status.json");
            Files.write2File(JsonUtil.formatJson(INSTANCE), statusFile);
        } catch (JsonMappingException e) {
            Log.printStackTrace(TAG, e);
            throw new RuntimeException("初始化配置失败", e);
        }
    }
    /**
     * 重置配置并保存
     */
    private static void resetAndSaveConfig() {
        try {
            JsonUtil.copyMapper().updateValue(INSTANCE, new Status());
            Files.write2File(JsonUtil.formatJson(INSTANCE), Files.getStatusFile(UserMap.getCurrentUid()));
        } catch (JsonMappingException e) {
            Log.printStackTrace(TAG, e);
            throw new RuntimeException("重置配置失败", e);
        }
    }
    public static synchronized void unload() {
        try {
            JsonUtil.copyMapper().updateValue(INSTANCE, new Status());
        } catch (JsonMappingException e) {
            Log.printStackTrace(TAG, e);
        }
    }
    public static synchronized void save() {
        save(Calendar.getInstance());
    }
    public static synchronized void save(Calendar nowCalendar) {
        String currentUid = UserMap.getCurrentUid();
        if (StringUtil.isEmpty(currentUid)) {
            Log.record("用户为空，状态保存失败");
            throw new RuntimeException("用户为空，状态保存失败");
        }
        if (updateDay(nowCalendar)) {
            Log.runtime(TAG, "重置 statistics.json");
        } else {
            Log.runtime(TAG, "保存 status.json");
        }
        long lastSaveTime = INSTANCE.saveTime;
        try {
            INSTANCE.saveTime = System.currentTimeMillis();
            Files.write2File(JsonUtil.formatJson(INSTANCE), Files.getStatusFile(currentUid));
        } catch (Exception e) {
            INSTANCE.saveTime = lastSaveTime;
            throw e;
        }
    }
    public static Boolean updateDay(Calendar nowCalendar) {
        if (TimeUtil.isLessThanSecondOfDays(INSTANCE.saveTime, nowCalendar.getTimeInMillis())) {
            Status.unload();
            return true;
        } else {
            return false;
        }
    }
    public static boolean canOrnamentToday() {
        return INSTANCE.canOrnament;
    }
    public static void setOrnamentToday() {
        if (INSTANCE.canOrnament) {
            INSTANCE.canOrnament = false;
            save();
        }
    }
    // 新村捐赠
    public static boolean canStallDonateToday() {
        return INSTANCE.canStallDonate;
    }
    public static void setStallDonateToday() {
        if (INSTANCE.canStallDonate) {
            INSTANCE.canStallDonate = false;
            save();
        }
    }
    public static Boolean hasFlagToday(String flag) {
        return INSTANCE.flagList.contains(flag);
    }
    public static void setFlagToday(String flag) {
        if (!hasFlagToday(flag)) {
            INSTANCE.flagList.add(flag);
            save();
        }
    }

    public static Boolean canMemberPointExchangeBenefitToday(String benefitId) {
        return !INSTANCE.memberPointExchangeBenefitLogList.contains(benefitId);
    }

    public static void memberPointExchangeBenefitToday(String benefitId) {
        if (canMemberPointExchangeBenefitToday(benefitId)) {
            INSTANCE.memberPointExchangeBenefitLogList.add(benefitId);
            save();
        }
    }

    /**
     * 乐园商城-是否可以兑换该商品
     * @param spuId 商品spuId
     * @return true 可以兑换 false 兑换达到上限
     */
    public static boolean canParadiseCoinExchangeBenefitToday(String spuId) {
        return !hasFlagToday("farm::paradiseCoinExchangeLimit::" + spuId);
    }
}