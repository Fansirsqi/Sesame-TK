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
import fansirsqi.xposed.sesame.util.maps.UserMap;
import fansirsqi.xposed.sesame.util.StringUtil;
import fansirsqi.xposed.sesame.util.TimeUtil;
import lombok.Data;

@Data
public class Status {
    private static final String TAG = Status.class.getSimpleName();
    private static final Status INSTANCE = new Status();
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
    private long kbSignIn = 0;
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


    public static long getCurrentDayTimestamp() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis(); // 返回当天零点时间戳
    }


    public static int getVitalityCount(String skuId) {
        Integer exchangedCount = getINSTANCE().getVitalityStoreList().get(skuId);
        if (exchangedCount == null) {
            exchangedCount = 0;
        }
        return exchangedCount;
    }

    public static Boolean canVitalityExchangeToday(String skuId, int count) {
        return !hasFlagToday("forest::VitalityExchangeLimit::" + skuId) && getVitalityCount(skuId) < count;
    }

    public static void vitalityExchangeToday(String skuId) {
        int count = getVitalityCount(skuId) + 1;
        getINSTANCE().getVitalityStoreList().put(skuId, count);
        save();
    }

    public static boolean canAnimalSleep() {
        return !getINSTANCE().getAnimalSleep();
    }

    public static void animalSleep() {
        if (!getINSTANCE().getAnimalSleep()) {
            getINSTANCE().setAnimalSleep(true);
            save();
        }
    }

    public static boolean canWaterFriendToday(String id, int newCount) {
        id = UserMap.getCurrentUid() + "-" + id;
        Integer count = getINSTANCE().getWaterFriendLogList().get(id);
        if (count == null) {
            return true;
        }
        return count < newCount;
    }

    public static void waterFriendToday(String id, int count) {
        id = UserMap.getCurrentUid() + "-" + id;
        getINSTANCE().getWaterFriendLogList().put(id, count);
        save();
    }

    public static int getReserveTimes(String id) {
        Integer count = getINSTANCE().getReserveLogList().get(id);
        if (count == null) {
            return 0;
        }
        return count;
    }

    public static boolean canReserveToday(String id, int count) {
        return getReserveTimes(id) < count;
    }

    public static void reserveToday(String id, int newCount) {
        Integer count = getINSTANCE().getReserveLogList().get(id);
        if (count == null) {
            count = 0;
        }
        getINSTANCE().getReserveLogList().put(id, count + newCount);
        save();
    }

    public static boolean canCooperateWaterToday(String uid, String coopId) {
        return !getINSTANCE().getCooperateWaterList().contains(uid + "_" + coopId);
    }

    public static void cooperateWaterToday(String uid, String coopId) {
        String v = uid + "_" + coopId;
        if (!getINSTANCE().getCooperateWaterList().contains(v)) {
            getINSTANCE().getCooperateWaterList().add(v);
            save();
        }
    }

    public static boolean canAncientTreeToday(String cityCode) {
        return !getINSTANCE().getAncientTreeCityCodeList().contains(cityCode);
    }

    public static void ancientTreeToday(String cityCode) {
        if (!getINSTANCE().getAncientTreeCityCodeList().contains(cityCode)) {
            getINSTANCE().getAncientTreeCityCodeList().add(cityCode);
            save();
        }
    }

    public static boolean canAnswerQuestionToday() {
        return !getINSTANCE().answerQuestion;
    }

    public static void answerQuestionToday() {
        if (!getINSTANCE().answerQuestion) {
            getINSTANCE().answerQuestion = true;
            save();
        }
    }

    public static boolean canFeedFriendToday(String id, int newCount) {
        Integer count = getINSTANCE().feedFriendLogList.get(id);
        if (count == null) {
            return true;
        }
        return count < newCount;
    }

    public static void feedFriendToday(String id) {
        Integer count = getINSTANCE().feedFriendLogList.get(id);
        if (count == null) {
            count = 0;
        }
        getINSTANCE().feedFriendLogList.put(id, count + 1);
        save();
    }

    public static boolean canVisitFriendToday(String id, int newCount) {
        id = UserMap.getCurrentUid() + "-" + id;
        Integer count = getINSTANCE().visitFriendLogList.get(id);
        if (count == null) {
            return true;
        }
        return count < newCount;
    }

    public static void visitFriendToday(String id, int newCount) {
        id = UserMap.getCurrentUid() + "-" + id;
        getINSTANCE().visitFriendLogList.put(id, newCount);
        save();
    }

    public static boolean canMemberSignInToday(String uid) {
        return !getINSTANCE().memberSignInList.contains(uid);
    }

    public static void memberSignInToday(String uid) {
        if (!getINSTANCE().memberSignInList.contains(uid)) {
            getINSTANCE().memberSignInList.add(uid);
            save();
        }
    }

    public static boolean canUseAccelerateTool() {
        return getINSTANCE().useAccelerateToolCount < 8;
    }

    public static void useAccelerateTool() {
        getINSTANCE().useAccelerateToolCount += 1;
        save();
    }

    public static boolean canDonationEgg(String uid) {
        return !getINSTANCE().donationEggList.contains(uid);
    }

    public static void donationEgg(String uid) {
        if (!getINSTANCE().donationEggList.contains(uid)) {
            getINSTANCE().donationEggList.add(uid);
            save();
        }
    }

    public static boolean canSpreadManureToday(String uid) {
        return !getINSTANCE().spreadManureList.contains(uid);
    }

    public static void spreadManureToday(String uid) {
        if (!getINSTANCE().spreadManureList.contains(uid)) {
            getINSTANCE().spreadManureList.add(uid);
            save();
        }
    }

    /**
     * 是否可以新村助力
     *
     * @return true是，false否
     */
    public static boolean canAntStallAssistFriendToday() {
        return !getINSTANCE().antStallAssistFriend.contains(UserMap.getCurrentUid());
    }

    /**
     * 设置新村助力已到上限
     */
    public static void antStallAssistFriendToday() {
        String uid = UserMap.getCurrentUid();
        if (!getINSTANCE().antStallAssistFriend.contains(uid)) {
            getINSTANCE().antStallAssistFriend.add(uid);
            save();
        }
    }

    // 农场助力
    public static boolean canAntOrchardAssistFriendToday() {
        return !getINSTANCE().antOrchardAssistFriend.contains(UserMap.getCurrentUid());
    }

    public static void antOrchardAssistFriendToday() {
        String uid = UserMap.getCurrentUid();
        if (!getINSTANCE().antOrchardAssistFriend.contains(uid)) {
            getINSTANCE().antOrchardAssistFriend.add(uid);
            save();
        }
    }

    public static boolean canProtectBubbleToday(String uid) {
        return !getINSTANCE().getProtectBubbleList().contains(uid);
    }

    public static void protectBubbleToday(String uid) {
        if (!getINSTANCE().getProtectBubbleList().contains(uid)) {
            getINSTANCE().getProtectBubbleList().add(uid);
            save();
        }
    }

    /**
     * 是否可以贴罚单
     *
     * @return true是，false否
     */
    public static boolean canPasteTicketTime() {
        return !getINSTANCE().canPasteTicketTime.contains(UserMap.getCurrentUid());
    }

    /**
     * 罚单贴完了
     */
    public static void pasteTicketTime() {
        if (getINSTANCE().canPasteTicketTime.contains(UserMap.getCurrentUid())) {
            return;
        }
        getINSTANCE().canPasteTicketTime.add(UserMap.getCurrentUid());
        save();
    }

    public static boolean canDoubleToday() {
        AntForest task = ModelTask.getModel(AntForest.class);
        if (task == null) {
            return false;
        }
        return getINSTANCE().getDoubleTimes() < task.getDoubleCountLimit().getValue();
    }

    public static void DoubleToday() {
        getINSTANCE().setDoubleTimes(getINSTANCE().getDoubleTimes() + 1);
        save();
    }

    public static boolean canKbSignInToday() {
        return getINSTANCE().kbSignIn < getCurrentDayTimestamp();
    }

    public static void KbSignInToday() {
        long todayZero = getCurrentDayTimestamp(); // 获取当天零点时间戳
        if (getINSTANCE().kbSignIn != todayZero) {
            getINSTANCE().kbSignIn = todayZero;
            save();
        }
    }

    public static void setDadaDailySet(Set<String> dailyAnswerList) {
        getINSTANCE().dailyAnswerList = dailyAnswerList;
        save();
    }

    public static boolean canDonateCharityCoin() {
        return !getINSTANCE().donateCharityCoin;
    }

    public static void donateCharityCoin() {
        if (!getINSTANCE().donateCharityCoin) {
            getINSTANCE().donateCharityCoin = true;
            save();
        }
    }

    public static boolean canExchangeToday(String uid) {
        return !getINSTANCE().exchangeList.contains(uid);
    }

    public static void exchangeToday(String uid) {
        if (!getINSTANCE().exchangeList.contains(uid)) {
            getINSTANCE().exchangeList.add(uid);
            save();
        }
    }

    /**
     * 绿色经营-是否可以收好友金币
     *
     * @return true是，false否
     */
    public static boolean canGreenFinancePointFriend() {
        return getINSTANCE().greenFinancePointFriend.contains(UserMap.getCurrentUid());
    }

    /**
     * 绿色经营-收好友金币完了
     */
    public static void greenFinancePointFriend() {
        if (canGreenFinancePointFriend()) {
            return;
        }
        getINSTANCE().greenFinancePointFriend.add(UserMap.getCurrentUid());
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
        if (getINSTANCE().greenFinancePrizesMap.containsKey(currentUid)) {
            Integer storedWeek = getINSTANCE().greenFinancePrizesMap.get(currentUid);
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
        getINSTANCE().greenFinancePrizesMap.put(UserMap.getCurrentUid(), TimeUtil.getWeekNumber(new Date()));
        save();
    }

    /**
     * 加载状态文件
     *
     * @return 状态对象
     */
    public static synchronized Status load(String currentUid) {
//        String currentUid = UserMap.getCurrentUid();
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
                    JsonUtil.copyMapper().readerForUpdating(getINSTANCE()).readValue(json);
                    String formatted = JsonUtil.formatJson(getINSTANCE());
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
        if (getINSTANCE().saveTime == null) {
            getINSTANCE().saveTime = System.currentTimeMillis();
        }
        return getINSTANCE();
    }

    /**
     * 初始化默认配置
     *
     * @param statusFile 状态文件
     */
    private static void initializeDefaultConfig(java.io.File statusFile) {
        try {
            JsonUtil.copyMapper().updateValue(getINSTANCE(), new Status());
            Log.runtime(TAG, "初始化 status.json");
            Files.write2File(JsonUtil.formatJson(getINSTANCE()), statusFile);
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
            JsonUtil.copyMapper().updateValue(getINSTANCE(), new Status());
            Files.write2File(JsonUtil.formatJson(getINSTANCE()), Files.getStatusFile(UserMap.getCurrentUid()));
        } catch (JsonMappingException e) {
            Log.printStackTrace(TAG, e);
            throw new RuntimeException("重置配置失败", e);
        }
    }

    public static synchronized void unload() {
        try {
            JsonUtil.copyMapper().updateValue(getINSTANCE(), new Status());
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
            Log.record(TAG, "用户为空，状态保存失败");
            throw new RuntimeException("用户为空，状态保存失败");
        }
        if (updateDay(nowCalendar)) {
            Log.runtime(TAG, "重置 statistics.json");
        } else {
            Log.runtime(TAG, "保存 status.json");
        }
        long lastSaveTime = getINSTANCE().saveTime;
        try {
            getINSTANCE().saveTime = System.currentTimeMillis();
            Files.write2File(JsonUtil.formatJson(getINSTANCE()), Files.getStatusFile(currentUid));
        } catch (Exception e) {
            getINSTANCE().saveTime = lastSaveTime;
            throw e;
        }
    }

    public static Boolean updateDay(Calendar nowCalendar) {
        if (TimeUtil.isLessThanSecondOfDays(getINSTANCE().saveTime, nowCalendar.getTimeInMillis())) {
            Status.unload();
            return true;
        } else {
            return false;
        }
    }

    public static boolean canOrnamentToday() {
        return getINSTANCE().canOrnament;
    }

    public static void setOrnamentToday() {
        if (getINSTANCE().canOrnament) {
            getINSTANCE().canOrnament = false;
            save();
        }
    }

    // 新村捐赠
    public static boolean canStallDonateToday() {
        return getINSTANCE().canStallDonate;
    }

    public static void setStallDonateToday() {
        if (getINSTANCE().canStallDonate) {
            getINSTANCE().canStallDonate = false;
            save();
        }
    }

    public static Boolean hasFlagToday(String flag) {
        return getINSTANCE().flagList.contains(flag);
    }

    public static void setFlagToday(String flag) {
        if (!hasFlagToday(flag)) {
            getINSTANCE().flagList.add(flag);
            save();
        }
    }

    public static Boolean canMemberPointExchangeBenefitToday(String benefitId) {
        return !getINSTANCE().memberPointExchangeBenefitLogList.contains(benefitId);
    }

    public static void memberPointExchangeBenefitToday(String benefitId) {
        if (canMemberPointExchangeBenefitToday(benefitId)) {
            getINSTANCE().memberPointExchangeBenefitLogList.add(benefitId);
            save();
        }
    }

    /**
     * 乐园商城-是否可以兑换该商品
     *
     * @param spuId 商品spuId
     * @return true 可以兑换 false 兑换达到上限
     */
    public static boolean canParadiseCoinExchangeBenefitToday(String spuId) {
        return !hasFlagToday("farm::paradiseCoinExchangeLimit::" + spuId);
    }
    
    public static Status getINSTANCE() {
        return INSTANCE;
    }
}