package fansirsqi.xposed.sesame.task.antFarm;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;

import fansirsqi.xposed.sesame.data.DataCache;
import fansirsqi.xposed.sesame.entity.AlipayUser;
import fansirsqi.xposed.sesame.entity.MapperEntity;
import fansirsqi.xposed.sesame.entity.OtherEntityProvider;
import fansirsqi.xposed.sesame.entity.ParadiseCoinBenefit;
import fansirsqi.xposed.sesame.hook.rpc.intervallimit.RpcIntervalLimit;
import fansirsqi.xposed.sesame.model.BaseModel;
import fansirsqi.xposed.sesame.model.ModelFields;
import fansirsqi.xposed.sesame.model.ModelGroup;
import fansirsqi.xposed.sesame.model.modelFieldExt.BooleanModelField;
import fansirsqi.xposed.sesame.model.modelFieldExt.ChoiceModelField;
import fansirsqi.xposed.sesame.model.modelFieldExt.IntegerModelField;
import fansirsqi.xposed.sesame.model.modelFieldExt.ListModelField;
import fansirsqi.xposed.sesame.model.modelFieldExt.SelectAndCountModelField;
import fansirsqi.xposed.sesame.model.modelFieldExt.SelectModelField;
import fansirsqi.xposed.sesame.model.modelFieldExt.StringModelField;
import fansirsqi.xposed.sesame.task.AnswerAI.AnswerAI;
import fansirsqi.xposed.sesame.task.ModelTask;
import fansirsqi.xposed.sesame.task.TaskCommon;
import fansirsqi.xposed.sesame.task.TaskStatus;
import fansirsqi.xposed.sesame.util.GlobalThreadPools;
import fansirsqi.xposed.sesame.util.JsonUtil;
import fansirsqi.xposed.sesame.util.ListUtil;
import fansirsqi.xposed.sesame.util.Log;
import fansirsqi.xposed.sesame.util.maps.IdMapManager;
import fansirsqi.xposed.sesame.util.maps.ParadiseCoinBenefitIdMap;
import fansirsqi.xposed.sesame.util.maps.UserMap;
import fansirsqi.xposed.sesame.util.RandomUtil;
import fansirsqi.xposed.sesame.util.ResChecker;
import fansirsqi.xposed.sesame.data.Status;
import fansirsqi.xposed.sesame.util.StringUtil;
import fansirsqi.xposed.sesame.util.TimeUtil;
import lombok.ToString;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

public class AntFarm extends ModelTask {
    private static final String TAG = AntFarm.class.getSimpleName();
    private String ownerFarmId;
    private Animal[] animals;
    private Animal ownerAnimal = new Animal();
    private static final ObjectMapper objectMapper = new ObjectMapper();
    /**
     * 小鸡饲料g
     */
    private int foodStock;
    private int foodStockLimit;
    private String rewardProductNum;
    private RewardFriend[] rewardList;
    /**
     * 慈善评分
     */
    private double benevolenceScore;
    private double harvestBenevolenceScore;

    /**
     * 未领取的饲料奖励
     */
    private int unreceiveTaskAward = 0;
    /**
     * 小鸡心情值
     */
    private double finalScore = 0d;
    private String familyGroupId;
    private FarmTool[] farmTools;

    static {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    public String getName() {
        return "庄园";
    }

    @Override
    public ModelGroup getGroup() {
        return ModelGroup.FARM;
    }

    @Override
    public String getIcon() {
        return "AntFarm.png";
    }

    private static final String FARM_ANSWER_CACHE_KEY = "farmAnswerQuestionCache";
    private static final String ANSWERED_FLAG = "farmQuestion::answered"; // 今日是否已答题
    private static final String CACHED_FLAG = "farmQuestion::cache";     // 是否已缓存明日答案

    /**
     * 小鸡睡觉时间
     */
    private StringModelField sleepTime;
    /**
     * 小鸡睡觉时长
     */
    private IntegerModelField sleepMinutes;
    /**
     * 自动喂鸡
     */
    private BooleanModelField feedAnimal;
    /**
     * 打赏好友
     */
    private BooleanModelField rewardFriend;
    /**
     * 遣返小鸡
     */
    private BooleanModelField sendBackAnimal;
    /**
     * 遣返方式
     */
    private ChoiceModelField sendBackAnimalWay;
    /**
     * 遣返动作
     */
    private ChoiceModelField sendBackAnimalType;
    /**
     * 遣返好友列表
     */
    private SelectModelField sendBackAnimalList;
    /**
     * 召回小鸡
     */
    private ChoiceModelField recallAnimalType;
    /**
     * s收取道具奖励
     */
    private BooleanModelField receiveFarmToolReward;
    /**
     * 游戏改分
     */
    private BooleanModelField recordFarmGame;
    /**
     * 小鸡游戏时间
     */
    private ListModelField.ListJoinCommaToStringModelField farmGameTime;
    /**
     * 小鸡厨房
     */
    private BooleanModelField kitchen;
    /**
     * 使用特殊食品
     */
    private BooleanModelField useSpecialFood;
    private BooleanModelField useNewEggCard;
    private BooleanModelField harvestProduce;
    private BooleanModelField donation;
    private ChoiceModelField donationCount;
    /**
     * 收取饲料奖励
     */
    private BooleanModelField receiveFarmTaskAward;
    private BooleanModelField useAccelerateTool;
    private BooleanModelField useAccelerateToolContinue;
    private BooleanModelField useAccelerateToolWhenMaxEmotion;
    /**
     * 喂鸡列表
     */
    private SelectAndCountModelField feedFriendAnimalList;
    private BooleanModelField notifyFriend;
    private ChoiceModelField notifyFriendType;
    private SelectModelField notifyFriendList;
    private BooleanModelField acceptGift;
    private SelectAndCountModelField visitFriendList;
    private BooleanModelField chickenDiary;
    private BooleanModelField diaryTietie;
    private ChoiceModelField collectChickenDiary;
    private BooleanModelField enableChouchoule;
    private BooleanModelField listOrnaments;
    private BooleanModelField hireAnimal;
    private ChoiceModelField hireAnimalType;
    private SelectModelField hireAnimalList;
    private BooleanModelField enableDdrawGameCenterAward;
    private BooleanModelField getFeed;
    private SelectModelField getFeedlList;
    private ChoiceModelField getFeedType;
    private BooleanModelField family;
    private SelectModelField familyOptions;
    private SelectModelField notInviteList;
    private StringModelField giftFamilyDrawFragment;
    private BooleanModelField paradiseCoinExchangeBenefit;
    private SelectModelField paradiseCoinExchangeBenefitList;

    @Override
    public ModelFields getFields() {
        ModelFields modelFields = new ModelFields();
        modelFields.addField(sleepTime = new StringModelField("sleepTime", "小鸡睡觉时间(关闭:-1)", "2001"));
        modelFields.addField(sleepMinutes = new IntegerModelField("sleepMinutes", "小鸡睡觉时长(分钟)", 10 * 59, 1, 10 * 60));
        modelFields.addField(recallAnimalType = new ChoiceModelField("recallAnimalType", "召回小鸡", RecallAnimalType.ALWAYS, RecallAnimalType.nickNames));
        modelFields.addField(rewardFriend = new BooleanModelField("rewardFriend", "打赏好友", false));
        modelFields.addField(feedAnimal = new BooleanModelField("feedAnimal", "自动喂小鸡", false));
        modelFields.addField(feedFriendAnimalList = new SelectAndCountModelField("feedFriendAnimalList", "喂小鸡好友列表", new LinkedHashMap<>(), AlipayUser::getList));
        modelFields.addField(getFeed = new BooleanModelField("getFeed", "一起拿饲料", false));
        modelFields.addField(getFeedType = new ChoiceModelField("getFeedType", "一起拿饲料 | 动作", GetFeedType.GIVE, GetFeedType.nickNames));
        modelFields.addField(getFeedlList = new SelectModelField("getFeedlList", "一起拿饲料 | 好友列表", new LinkedHashSet<>(), AlipayUser::getList));
        modelFields.addField(acceptGift = new BooleanModelField("acceptGift", "收麦子", false));
        modelFields.addField(visitFriendList = new SelectAndCountModelField("visitFriendList", "送麦子好友列表", new LinkedHashMap<>(), AlipayUser::getList));
        modelFields.addField(hireAnimal = new BooleanModelField("hireAnimal", "雇佣小鸡 | 开启", false));
        modelFields.addField(hireAnimalType = new ChoiceModelField("hireAnimalType", "雇佣小鸡 | 动作", HireAnimalType.DONT_HIRE, HireAnimalType.nickNames));
        modelFields.addField(hireAnimalList = new SelectModelField("hireAnimalList", "雇佣小鸡 | 好友列表", new LinkedHashSet<>(), AlipayUser::getList));
        modelFields.addField(sendBackAnimal = new BooleanModelField("sendBackAnimal", "遣返 | 开启", false));
        modelFields.addField(sendBackAnimalWay = new ChoiceModelField("sendBackAnimalWay", "遣返 | 方式", SendBackAnimalWay.NORMAL, SendBackAnimalWay.nickNames));
        modelFields.addField(sendBackAnimalType = new ChoiceModelField("sendBackAnimalType", "遣返 | 动作", SendBackAnimalType.NOT_BACK, SendBackAnimalType.nickNames));
        modelFields.addField(sendBackAnimalList = new SelectModelField("dontSendFriendList", "遣返 | 好友列表", new LinkedHashSet<>(), AlipayUser::getList));
        modelFields.addField(notifyFriend = new BooleanModelField("notifyFriend", "通知赶鸡 | 开启", false));
        modelFields.addField(notifyFriendType = new ChoiceModelField("notifyFriendType", "通知赶鸡 | 动作", NotifyFriendType.NOTIFY, NotifyFriendType.nickNames));
        modelFields.addField(notifyFriendList = new SelectModelField("notifyFriendList", "通知赶鸡 | 好友列表", new LinkedHashSet<>(), AlipayUser::getList));
        modelFields.addField(donation = new BooleanModelField("donation", "每日捐蛋 | 开启", false));
        modelFields.addField(donationCount = new ChoiceModelField("donationCount", "每日捐蛋 | 次数", DonationCount.ONE, DonationCount.nickNames));
        modelFields.addField(useAccelerateTool = new BooleanModelField("useAccelerateTool", "加速卡 | 使用", false));
        modelFields.addField(useAccelerateToolContinue = new BooleanModelField("useAccelerateToolContinue", "加速卡 | 连续使用", false));
        modelFields.addField(useAccelerateToolWhenMaxEmotion = new BooleanModelField("useAccelerateToolWhenMaxEmotion", "加速卡 | 仅在满状态时使用", false));
        modelFields.addField(useSpecialFood = new BooleanModelField("useSpecialFood", "使用特殊食品", false));
        modelFields.addField(useNewEggCard = new BooleanModelField("useNewEggCard", "使用新蛋卡", false));
        modelFields.addField(receiveFarmTaskAward = new BooleanModelField("receiveFarmTaskAward", "收取饲料奖励", false));
        modelFields.addField(receiveFarmToolReward = new BooleanModelField("receiveFarmToolReward", "收取道具奖励", false));
        modelFields.addField(harvestProduce = new BooleanModelField("harvestProduce", "收获爱心鸡蛋", false));
        modelFields.addField(kitchen = new BooleanModelField("kitchen", "小鸡厨房", false));
        modelFields.addField(chickenDiary = new BooleanModelField("chickenDiary", "小鸡日记", false));
        modelFields.addField(diaryTietie = new BooleanModelField("diaryTietie", "小鸡日记 | 贴贴", false));
        modelFields.addField(collectChickenDiary = new ChoiceModelField("collectChickenDiary", "小鸡日记 | 点赞", collectChickenDiaryType.ONCE, collectChickenDiaryType.nickNames));
        modelFields.addField(enableChouchoule = new BooleanModelField("enableChouchoule", "开启小鸡抽抽乐", false));
        modelFields.addField(listOrnaments = new BooleanModelField("listOrnaments", "小鸡每日换装", false));
        modelFields.addField(enableDdrawGameCenterAward = new BooleanModelField("enableDdrawGameCenterAward", "开宝箱", false));
        modelFields.addField(recordFarmGame = new BooleanModelField("recordFarmGame", "游戏改分(星星球、登山赛、飞行赛、揍小鸡)", false));
        modelFields.addField(farmGameTime = new ListModelField.ListJoinCommaToStringModelField("farmGameTime", "小鸡游戏时间(范围)", ListUtil.newArrayList("2200-2400")));
        modelFields.addField(family = new BooleanModelField("family", "家庭 | 开启", false));
        modelFields.addField(familyOptions = new SelectModelField("familyOptions", "家庭 | 选项", new LinkedHashSet<>(), OtherEntityProvider.farmFamilyOption()));
        modelFields.addField(notInviteList = new SelectModelField("notInviteList", "家庭 | 好友分享排除列表", new LinkedHashSet<>(), AlipayUser::getList));
//        modelFields.addField(giftFamilyDrawFragment = new StringModelField("giftFamilyDrawFragment", "家庭 | 扭蛋碎片赠送用户ID(配置目录查看)", ""));
        modelFields.addField(paradiseCoinExchangeBenefit = new BooleanModelField("paradiseCoinExchangeBenefit", "小鸡乐园 | 兑换权益", false));
        modelFields.addField(paradiseCoinExchangeBenefitList = new SelectModelField("paradiseCoinExchangeBenefitList", "小鸡乐园 | 权益列表", new LinkedHashSet<>(), ParadiseCoinBenefit::getList));
        return modelFields;
    }

    @Override
    public void boot(ClassLoader classLoader) {
        super.boot(classLoader);
        RpcIntervalLimit.INSTANCE.addIntervalLimit("com.alipay.antfarm.enterFarm", 2000);
    }

    @Override
    public Boolean check() {
        if (TaskCommon.IS_ENERGY_TIME) {
            Log.record(TAG, "⏸ 当前为只收能量时间【" + BaseModel.getEnergyTime().getValue() + "】，停止执行" + getName() + "任务！");
            return false;
        } else if (TaskCommon.IS_MODULE_SLEEP_TIME) {
            Log.record(TAG, "💤 模块休眠时间【" + BaseModel.getModelSleepTime().getValue() + "】停止执行" + getName() + "任务！");
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void run() {
        try {
            String userId = UserMap.getCurrentUid();
            Log.record(TAG, "执行开始-蚂蚁" + getName());
            if (enterFarm() == null) {
                return;
            }
            listFarmTool();//装载道具信息

            if (rewardFriend.getValue()) {
                rewardFriend();
            }
            if (sendBackAnimal.getValue()) {
                sendBackAnimal();
            }

            if (receiveFarmToolReward.getValue()) {
                receiveToolTaskReward();
            }
            if (recordFarmGame.getValue()) {
                for (String time : farmGameTime.getValue()) {
                    if (TimeUtil.checkNowInTimeRange(time)) {
                        recordFarmGame(GameType.starGame);
                        recordFarmGame(GameType.jumpGame);
                        recordFarmGame(GameType.flyGame);
                        recordFarmGame(GameType.hitGame);
                        break;
                    }
                }
            }
            if (kitchen.getValue()) {
                collectDailyFoodMaterial();
                collectDailyLimitedFoodMaterial();
                cook();
            }

            if (chickenDiary.getValue()) {
                doChickenDiary();
            }

            if (useNewEggCard.getValue()) {
                useFarmTool(ownerFarmId, ToolType.NEWEGGTOOL);
                syncAnimalStatus(ownerFarmId);
            }
            if (harvestProduce.getValue() && benevolenceScore >= 1) {
                Log.record(TAG, "有可收取的爱心鸡蛋");
                harvestProduce(ownerFarmId);
            }
            if (donation.getValue() && Status.canDonationEgg(userId) && harvestBenevolenceScore >= 1) {
                handleDonation(donationCount.getValue());
            }
            if (receiveFarmTaskAward.getValue()) {
                doFarmTasks();
                receiveFarmAwards();
            }

            recallAnimal();

            handleAutoFeedAnimal();

            // 到访小鸡送礼
            visitAnimal();
            // 送麦子
            visit();
            // 帮好友喂鸡
            feedFriend();
            // 通知好友赶鸡
            if (notifyFriend.getValue()) {
                notifyFriend();
            }

            // 抽抽乐
            if (enableChouchoule.getValue()) {
                ChouChouLe ccl = new ChouChouLe();
                ccl.chouchoule();
            }

            // 雇佣小鸡
            if (hireAnimal.getValue()) {
                hireAnimal();
            }
            if (getFeed.getValue()) {
                letsGetChickenFeedTogether();
            }
            //家庭
            if (family.getValue()) {

//                family();
                AntFarmFamily.INSTANCE.run(familyOptions, notInviteList);
            }
            // 开宝箱
            if (enableDdrawGameCenterAward.getValue()) {
                drawGameCenterAward();
            }
            // 小鸡乐园道具兑换
            if (paradiseCoinExchangeBenefit.getValue()) {
                paradiseCoinExchangeBenefit();
            }
            //小鸡睡觉&起床
            animalSleepAndWake();
        } catch (Throwable t) {
            Log.runtime(TAG, "AntFarm.start.run err:");
            Log.printStackTrace(TAG, t);
        } finally {
            Log.record(TAG, "执行结束-蚂蚁" + getName());
        }
    }


    /**
     * 召回小鸡
     */
    private void recallAnimal() {
        try {
            //召回小鸡相关操作
            if (!AnimalInteractStatus.HOME.name().equals(ownerAnimal.animalInteractStatus)) {//如果小鸡不在家
                if ("ORCHARD".equals(ownerAnimal.locationType)) {
                    Log.farm("庄园通知📣[你家的小鸡给拉去除草了！]");
                    JSONObject joRecallAnimal = new JSONObject(AntFarmRpcCall.orchardRecallAnimal(ownerAnimal.animalId, ownerAnimal.currentFarmMasterUserId));
                    int manureCount = joRecallAnimal.getInt("manureCount");
                    Log.farm("召回小鸡📣[收获:肥料" + manureCount + "g]");
                } else {

                    Log.runtime(TAG, "DEBUG:" + ownerAnimal.toString());

                    syncAnimalStatus(ownerFarmId);
                    boolean guest = false;
                    switch (SubAnimalType.valueOf(ownerAnimal.subAnimalType)) {
                        case GUEST:
                            guest = true;
                            Log.record(TAG, "小鸡到好友家去做客了");
                            break;
                        case NORMAL:
                            Log.record(TAG, "小鸡太饿，离家出走了");
                            break;
                        case PIRATE:
                            Log.record(TAG, "小鸡外出探险了");
                            break;
                        case WORK:
                            Log.record(TAG, "小鸡出去工作啦");
                            break;
                        default:
                            Log.record(TAG, "小鸡不在庄园" + " " + ownerAnimal.subAnimalType);
                    }
                    boolean hungry = false;
                    String userName = UserMap.getMaskName(AntFarmRpcCall.farmId2UserId(ownerAnimal.currentFarmId));
                    switch (AnimalFeedStatus.valueOf(ownerAnimal.animalFeedStatus)) {
                        case HUNGRY:
                            hungry = true;
                            Log.record(TAG, "小鸡在[" + userName + "]的庄园里挨饿");
                            break;
                        case EATING:
                            Log.record(TAG, "小鸡在[" + userName + "]的庄园里吃得津津有味");
                            break;
                    }
                    boolean recall = switch (recallAnimalType.getValue()) {
                        case RecallAnimalType.ALWAYS -> true;
                        case RecallAnimalType.WHEN_THIEF -> !guest;
                        case RecallAnimalType.WHEN_HUNGRY -> hungry;
                        default -> false;
                    };
                    if (recall) {
                        recallAnimal(ownerAnimal.animalId, ownerAnimal.currentFarmId, ownerFarmId, userName);
                        syncAnimalStatus(ownerFarmId);
                    }
                }
            }
        } catch (Exception e) {
            Log.printStackTrace(TAG, "recallAnimal err:", e);
        }
    }

    private void paradiseCoinExchangeBenefit() {
        try {

            JSONObject jo = new JSONObject(AntFarmRpcCall.getMallHome());

            if (!ResChecker.checkRes(TAG, jo)) {
                Log.error(TAG, "小鸡乐园币💸[未获取到可兑换权益]");
                return;
            }
            JSONArray mallItemSimpleList = jo.getJSONArray("mallItemSimpleList");
            for (int i = 0; i < mallItemSimpleList.length(); i++) {
                JSONObject mallItemInfo = mallItemSimpleList.getJSONObject(i);
                String oderInfo;
                String spuName = mallItemInfo.getString("spuName");
                int minPrice = mallItemInfo.getInt("minPrice");
                String controlTag = mallItemInfo.getString("controlTag");
                String spuId = mallItemInfo.getString("spuId");
                oderInfo = spuName + "\n价格" + minPrice + "乐园币\n" + controlTag;
                IdMapManager.getInstance(ParadiseCoinBenefitIdMap.class).add(spuId, oderInfo);
                JSONArray itemStatusList = mallItemInfo.getJSONArray("itemStatusList");
                if (!Status.canParadiseCoinExchangeBenefitToday(spuId) || !paradiseCoinExchangeBenefitList.getValue().contains(spuId) || isExchange(itemStatusList, spuId, spuName)) {
                    continue;
                }
                int exchangedCount = 0;
                while (exchangeBenefit(spuId)) {
                    exchangedCount += 1;
                    Log.farm("乐园币兑换💸#花费[" + minPrice + "乐园币]" + "#第" + exchangedCount + "次兑换" + "[" + spuName + "]");
                    TimeUtil.sleep(3000);
                }
            }
            IdMapManager.getInstance(ParadiseCoinBenefitIdMap.class).save(UserMap.getCurrentUid());
        } catch (Throwable t) {
            Log.runtime(TAG, "paradiseCoinExchangeBenefit err:");
            Log.printStackTrace(TAG, t);
        }
    }

    private boolean exchangeBenefit(String spuId) {
        try {
            JSONObject jo = new JSONObject(AntFarmRpcCall.getMallItemDetail(spuId));
            if (!ResChecker.checkRes(TAG, jo)) {
                return false;
            }
            JSONObject mallItemDetail = jo.getJSONObject("mallItemDetail");
            JSONArray mallSubItemDetailList = mallItemDetail.getJSONArray("mallSubItemDetailList");
            for (int i = 0; i < mallSubItemDetailList.length(); i++) {
                JSONObject mallSubItemDetail = mallSubItemDetailList.getJSONObject(i);
                String skuId = mallSubItemDetail.getString("skuId");
                String skuName = mallSubItemDetail.getString("skuName");
                JSONArray itemStatusList = mallSubItemDetail.getJSONArray("itemStatusList");

                if (isExchange(itemStatusList, spuId, skuName)) {
                    return false;
                }

                if (exchangeBenefit(spuId, skuId)) {
                    return true;
                }
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "exchangeBenefit err:");
            Log.printStackTrace(TAG, t);
        }
        return false;
    }

    private boolean exchangeBenefit(String spuId, String skuId) {
        try {
            JSONObject jo = new JSONObject(AntFarmRpcCall.exchangeBenefit(spuId, skuId));
            return ResChecker.checkRes(TAG, jo);
        } catch (Throwable t) {
            Log.runtime(TAG, "exchangeBenefit err:");
            Log.printStackTrace(TAG, t);
        }
        return false;
    }

    private boolean isExchange(JSONArray itemStatusList, String spuId, String spuName) {
        try {
            for (int j = 0; j < itemStatusList.length(); j++) {
                String itemStatus = itemStatusList.getString(j);
                if (PropStatus.REACH_LIMIT.name().equals(itemStatus)
                        || PropStatus.REACH_USER_HOLD_LIMIT.name().equals(itemStatus)
                        || PropStatus.NO_ENOUGH_POINT.name().equals(itemStatus)) {
                    Log.record(TAG, "乐园兑换💸[" + spuName + "]停止:" + PropStatus.valueOf(itemStatus).nickName());
                    if (PropStatus.REACH_LIMIT.name().equals(itemStatus)) {
                        Status.setFlagToday("farm::paradiseCoinExchangeLimit::" + spuId);
                    }
                    return true;
                }
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "isItemExchange err:");
            Log.printStackTrace(TAG, t);
        }
        return false;
    }

    private void animalSleepAndWake() {
        try {
            String sleepTimeStr = sleepTime.getValue();
            if ("-1".equals(sleepTimeStr)) {
                Log.runtime(TAG, "当前已关闭小鸡睡觉");
                return;
            }
            Calendar now = TimeUtil.getNow();
            Calendar animalSleepTimeCalendar = TimeUtil.getTodayCalendarByTimeStr(sleepTimeStr);
            if (animalSleepTimeCalendar == null) {
                Log.record(TAG, "小鸡睡觉时间格式错误，请重新设置");
                return;
            }
            Integer sleepMinutesInt = sleepMinutes.getValue();
            Calendar animalWakeUpTimeCalendar = (Calendar) animalSleepTimeCalendar.clone();
            animalWakeUpTimeCalendar.add(Calendar.MINUTE, sleepMinutesInt);
            long animalSleepTime = animalSleepTimeCalendar.getTimeInMillis();
            long animalWakeUpTime = animalWakeUpTimeCalendar.getTimeInMillis();
            if (animalSleepTime > animalWakeUpTime) {
                Log.record(TAG, "小鸡睡觉设置有误，请重新设置");
                return;
            }
            boolean afterSleepTime = now.compareTo(animalSleepTimeCalendar) > 0;
            boolean afterWakeUpTime = now.compareTo(animalWakeUpTimeCalendar) > 0;
            if (afterSleepTime && afterWakeUpTime) {
                if (!Status.canAnimalSleep()) {
                    return;
                }
                Log.record(TAG, "已错过小鸡今日睡觉时间");
                return;
            }
            String sleepTaskId = "AS|" + animalSleepTime;
            String wakeUpTaskId = "AW|" + animalWakeUpTime;
            if (!hasChildTask(sleepTaskId) && !afterSleepTime) {
                addChildTask(new ChildModelTask(sleepTaskId, "AS", this::animalSleepNow, animalSleepTime));
                Log.record(TAG, "添加定时睡觉🛌[" + UserMap.getCurrentMaskName() + "]在[" + TimeUtil.getCommonDate(animalSleepTime) + "]执行");
            }
            if (!hasChildTask(wakeUpTaskId) && !afterWakeUpTime) {
                addChildTask(new ChildModelTask(wakeUpTaskId, "AW", this::animalWakeUpNow, animalWakeUpTime));
                Log.record(TAG, "添加定时起床🛌[" + UserMap.getCurrentMaskName() + "]在[" + TimeUtil.getCommonDate(animalWakeUpTime) + "]执行");
            }
            if (afterSleepTime) {
                if (Status.canAnimalSleep()) {
                    animalSleepNow();
                }
            }
        } catch (Exception e) {
            Log.runtime(TAG, "animalSleepAndWake err:");
            Log.printStackTrace(e);
        }
    }

    /**
     * 初始化庄园
     *
     * @return 庄园信息
     */
    private JSONObject enterFarm() {
        try {
            String userId = UserMap.getCurrentUid();
            JSONObject jo = new JSONObject(AntFarmRpcCall.enterFarm(userId, userId));
            if (ResChecker.checkRes(TAG, jo)) {
                rewardProductNum = jo.getJSONObject("dynamicGlobalConfig").getString("rewardProductNum");
                JSONObject joFarmVO = jo.getJSONObject("farmVO");
                JSONObject familyInfoVO = jo.getJSONObject("familyInfoVO");
                foodStock = joFarmVO.getInt("foodStock");
                foodStockLimit = joFarmVO.getInt("foodStockLimit");
                harvestBenevolenceScore = joFarmVO.getDouble("harvestBenevolenceScore");

                parseSyncAnimalStatusResponse(joFarmVO);

                userId = joFarmVO.getJSONObject("masterUserInfoVO").getString("userId");
                familyGroupId = familyInfoVO.optString("groupId", null);
                // 领取活动食物
                JSONObject activityData = jo.optJSONObject("activityData");
                if (activityData != null) {
                    for (Iterator<String> it = activityData.keys(); it.hasNext(); ) {
                        String key = it.next();
                        if (key.contains("Gifts")) {
                            JSONArray gifts = activityData.optJSONArray(key);
                            if (gifts == null) continue;
                            for (int i = 0; i < gifts.length(); i++) {
                                JSONObject gift = gifts.optJSONObject(i);
                                clickForGiftV2(gift);
                            }
                        }
                    }
                }
                if (useSpecialFood.getValue()) {//使用特殊食品
                    JSONArray cuisineList = jo.getJSONArray("cuisineList");
                    if (!AnimalFeedStatus.SLEEPY.name().equals(ownerAnimal.animalFeedStatus))
                        useSpecialFood(cuisineList);
                }

                if (jo.has("lotteryPlusInfo")) {//彩票附加信息
                    drawLotteryPlus(jo.getJSONObject("lotteryPlusInfo"));
                }

                if (acceptGift.getValue() && joFarmVO.getJSONObject("subFarmVO").has("giftRecord")
                        && foodStockLimit - foodStock >= 10) {
                    acceptGift();
                }
                return jo;
            }
        } catch (Exception e) {
            Log.printStackTrace(e);
        }
        return null;
    }


    /**
     * 自动喂鸡
     */
    private void handleAutoFeedAnimal() {
        if (!AnimalInteractStatus.HOME.name().equals(ownerAnimal.animalInteractStatus)) {
            return; // 小鸡不在家，不执行喂养逻辑
        }
        boolean needReload = false;
        // 1. 判断是否需要喂食
        if (AnimalFeedStatus.HUNGRY.name().equals(ownerAnimal.animalFeedStatus)) {
            if (feedAnimal.getValue()) {
                Log.record("小鸡在挨饿~Tk 尝试为你自动喂食");
                if (feedAnimal(ownerFarmId)) {
                    needReload = true;
                }
            }
        }

        // 2. 判断是否需要使用加速道具
        if (useAccelerateTool.getValue() && !AnimalFeedStatus.HUNGRY.name().equals(ownerAnimal.animalFeedStatus)) {
            if (useAccelerateTool()) {
                needReload = true;
            }
        }

        // 3. 如果有操作导致状态变化，则刷新庄园信息
        if (needReload) {
            enterFarm();
            syncAnimalStatus(ownerFarmId);
        }

        // 4. 计算并安排下一次自动喂食任务
        try {
            Long startEatTime = ownerAnimal.startEatTime;
            double allFoodHaveEatten = 0d;
            double allConsumeSpeed = 0d;

            for (Animal animal : animals) {
                allFoodHaveEatten += animal.foodHaveEatten;
                allConsumeSpeed += animal.consumeSpeed;
            }

            if (allConsumeSpeed > 0) {
                long nextFeedTime = startEatTime + (long) ((180 - allFoodHaveEatten) / allConsumeSpeed) * 1000;
                String taskId = "FA|" + ownerFarmId;

                if (!hasChildTask(taskId)) {
                    addChildTask(new ChildModelTask(taskId, "FA", () -> feedAnimal(ownerFarmId), nextFeedTime));
                    Log.record(TAG, "添加蹲点投喂🥣[" + UserMap.getCurrentMaskName() + "]在[" + TimeUtil.getCommonDate(nextFeedTime) + "]执行");
                } else {
                    // 更新时间即可
                    addChildTask(new ChildModelTask(taskId, "FA", () -> feedAnimal(ownerFarmId), nextFeedTime));
                }
            }
        } catch (Exception e) {
            Log.printStackTrace(e);
        }

        // 5. 其他功能（换装、领取饲料）
        // 小鸡换装
        if (listOrnaments.getValue() && Status.canOrnamentToday()) {
            listOrnaments();
        }
        if (unreceiveTaskAward > 0) {
            Log.record(TAG, "还有待领取的饲料");
            receiveFarmAwards();
        }
    }


    private void animalSleepNow() {
        try {
            String s = AntFarmRpcCall.queryLoveCabin(UserMap.getCurrentUid());
            JSONObject jo = new JSONObject(s);
            if (ResChecker.checkRes(TAG, jo)) {
                JSONObject sleepNotifyInfo = jo.getJSONObject("sleepNotifyInfo");
                if (sleepNotifyInfo.optBoolean("canSleep", false)) {
                    s = AntFarmRpcCall.sleep();
                    jo = new JSONObject(s);
                    if (ResChecker.checkRes(TAG, jo)) {
                        Log.farm("小鸡睡觉🛌");
                        Status.animalSleep();
                    }
                } else {
                    Log.farm("小鸡无需睡觉🛌");
                }
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "animalSleepNow err:");
            Log.printStackTrace(t);
        }
    }

    private void animalWakeUpNow() {
        try {
            String s = AntFarmRpcCall.queryLoveCabin(UserMap.getCurrentUid());
            JSONObject jo = new JSONObject(s);
            if (ResChecker.checkRes(TAG, jo)) {
                JSONObject sleepNotifyInfo = jo.getJSONObject("sleepNotifyInfo");
                if (!sleepNotifyInfo.optBoolean("canSleep", true)) {
                    s = AntFarmRpcCall.wakeUp();
                    jo = new JSONObject(s);
                    if (ResChecker.checkRes(TAG, jo)) {
                        Log.farm("小鸡起床 🛏");
                    }
                } else {
                    Log.farm("小鸡无需起床 🛏");
                }
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "animalWakeUpNow err:");
            Log.printStackTrace(t);
        }
    }


    /**
     * 同步小鸡状态通用方法
     *
     * @param farmId 庄园id
     */
    private JSONObject syncAnimalStatus(String farmId, String operTag, String operateType) {
        try {
            return new JSONObject(AntFarmRpcCall.syncAnimalStatus(farmId, operTag, operateType));
        } catch (Exception e) {
            Log.printStackTrace(TAG, e);
            return null;
        }
    }

    private void syncAnimalStatus(String farmId) {
        try {
            JSONObject jo = syncAnimalStatus(farmId, "SYNC_RESUME", "QUERY_ALL");
            parseSyncAnimalStatusResponse(jo);
        } catch (Throwable t) {
            Log.printStackTrace(TAG, "syncAnimalStatus err:", t);
        }
    }

    private JSONObject syncAnimalStatusAfterFeedAnimal(String farmId) {
        try {
            return syncAnimalStatus(farmId, "SYNC_AFTER_FEED_ANIMAL", "QUERY_EMOTION_INFO|QUERY_ORCHARD_RIGHTS");
        } catch (Exception e) {
            Log.printStackTrace(TAG, e);
        }
        return null;
    }

    private JSONObject syncAnimalStatusQueryFamilyAnimals(String farmId) {
        try {
            return syncAnimalStatus(farmId, "SYNC_RESUME_FAMILY", "QUERY_ALL|QUERY_FAMILY_ANIMAL");
        } catch (Exception e) {
            Log.printStackTrace(TAG, e);
        }
        return null;
    }


    private void syncAnimalStatusAtOtherFarm(String userId, String friendUserId) {
        try {
            String s = AntFarmRpcCall.enterFarm(userId, friendUserId);
            JSONObject jo = new JSONObject(s);
            Log.runtime(TAG, "DEBUG" + jo);
            jo = jo.getJSONObject("farmVO").getJSONObject("subFarmVO");
            JSONArray jaAnimals = jo.getJSONArray("animals");
            for (int i = 0; i < jaAnimals.length(); i++) {
                JSONObject jaAnimaJson = jaAnimals.getJSONObject(i);
                if (jaAnimaJson.getString("masterFarmId").equals(ownerFarmId)) { // 过滤出当前用户的小鸡
                    JSONObject animal = jaAnimals.getJSONObject(i);
                    ownerAnimal = objectMapper.readValue(animal.toString(), Animal.class);
                    break;
                }
            }
        } catch (JSONException j) {
            Log.printStackTrace(TAG, "syncAnimalStatusAtOtherFarm err:", j);

        } catch (Throwable t) {
            Log.printStackTrace(TAG, "syncAnimalStatusAtOtherFarm err:", t);
        }
    }

    private void rewardFriend() {
        try {
            if (rewardList != null) {
                for (RewardFriend rewardFriend : rewardList) {
                    String s = AntFarmRpcCall.rewardFriend(rewardFriend.consistencyKey, rewardFriend.friendId,
                            rewardProductNum, rewardFriend.time);
                    JSONObject jo = new JSONObject(s);
                    String memo = jo.getString("memo");
                    if (ResChecker.checkRes(TAG, jo)) {
                        double rewardCount = benevolenceScore - jo.getDouble("farmProduct");
                        benevolenceScore -= rewardCount;
                        Log.farm(String.format(Locale.CHINA, "打赏好友💰[%s]# 得%.2f颗爱心鸡蛋", UserMap.getMaskName(rewardFriend.friendId), rewardCount));
                    } else {
                        Log.record(memo);
                        Log.runtime(s);
                    }
                }
                rewardList = null;
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "rewardFriend err:");
            Log.printStackTrace(TAG, t);
        }
    }

    private void recallAnimal(String animalId, String currentFarmId, String masterFarmId, String user) {
        try {
            String s = AntFarmRpcCall.recallAnimal(animalId, currentFarmId, masterFarmId);
            JSONObject jo = new JSONObject(s);
            String memo = jo.getString("memo");
            if (ResChecker.checkRes(TAG, jo)) {
                double foodHaveStolen = jo.getDouble("foodHaveStolen");
                Log.farm("召回小鸡📣，偷吃[" + user + "]#" + foodHaveStolen + "g");
                // 这里不需要加
                // add2FoodStock((int)foodHaveStolen);
            } else {
                Log.record(memo);
                Log.runtime(s);
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "recallAnimal err:");
            Log.printStackTrace(TAG, t);
        }
    }

    private void sendBackAnimal() {
        if (animals == null) {
            return;
        }
        try {
            for (Animal animal : animals) {
                if (AnimalInteractStatus.STEALING.name().equals(animal.animalInteractStatus)
                        && !SubAnimalType.GUEST.name().equals(animal.subAnimalType)
                        && !SubAnimalType.WORK.name().equals(animal.subAnimalType)) {
                    // 赶鸡
                    String user = AntFarmRpcCall.farmId2UserId(animal.masterFarmId);
                    boolean isSendBackAnimal = sendBackAnimalList.getValue().contains(user);
                    if (sendBackAnimalType.getValue() == SendBackAnimalType.BACK) {
                        isSendBackAnimal = !isSendBackAnimal;
                    }
                    if (isSendBackAnimal) {
                        continue;
                    }
                    int sendTypeInt = sendBackAnimalWay.getValue();
                    user = UserMap.getMaskName(user);
                    String s = AntFarmRpcCall.sendBackAnimal(
                            SendBackAnimalWay.nickNames[sendTypeInt], animal.animalId,
                            animal.currentFarmId, animal.masterFarmId);
                    JSONObject jo = new JSONObject(s);
                    String memo = jo.getString("memo");
                    if (ResChecker.checkRes(TAG, jo)) {
                        if (sendTypeInt == SendBackAnimalWay.HIT) {
                            if (jo.has("hitLossFood")) {
                                s = "胖揍小鸡🤺[" + user + "]，掉落[" + jo.getInt("hitLossFood") + "g]";
                                if (jo.has("finalFoodStorage"))
                                    foodStock = jo.getInt("finalFoodStorage");
                            } else
                                s = "[" + user + "]的小鸡躲开了攻击";
                        } else {
                            s = "驱赶小鸡🧶[" + user + "]";
                        }
                        Log.farm(s);
                    } else {
                        Log.record(memo);
                        Log.runtime(s);
                    }
                }
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "sendBackAnimal err:");
            Log.printStackTrace(TAG, t);
        }
    }

    private void receiveToolTaskReward() {
        try {
            String s = AntFarmRpcCall.listToolTaskDetails();
            JSONObject jo = new JSONObject(s);
            String memo = jo.getString("memo");
            if (ResChecker.checkRes(TAG, jo)) {
                JSONArray jaList = jo.getJSONArray("list");
                for (int i = 0; i < jaList.length(); i++) {
                    JSONObject joItem = jaList.getJSONObject(i);
                    if (joItem.has("taskStatus")
                            && TaskStatus.FINISHED.name().equals(joItem.getString("taskStatus"))) {
                        JSONObject bizInfo = new JSONObject(joItem.getString("bizInfo"));
                        String awardType = bizInfo.getString("awardType");
                        ToolType toolType = ToolType.valueOf(awardType);
                        boolean isFull = false;
                        for (FarmTool farmTool : farmTools) {
                            if (farmTool.toolType == toolType) {
                                if (farmTool.toolCount == farmTool.toolHoldLimit) {
                                    isFull = true;
                                }
                                break;
                            }
                        }
                        if (isFull) {
                            Log.record(TAG, "领取道具[" + toolType.nickName() + "]#已满，暂不领取");
                            continue;
                        }
                        int awardCount = bizInfo.getInt("awardCount");
                        String taskType = joItem.getString("taskType");
                        String taskTitle = bizInfo.getString("taskTitle");
                        s = AntFarmRpcCall.receiveToolTaskReward(awardType, awardCount, taskType);
                        jo = new JSONObject(s);
                        memo = jo.getString("memo");
                        if (ResChecker.checkRes(TAG, jo)) {
                            Log.farm("领取道具🎖️[" + taskTitle + "-" + toolType.nickName() + "]#" + awardCount + "张");
                        } else {
                            memo = memo.replace("道具", toolType.nickName());
                            Log.record(memo);
                            Log.runtime(s);
                        }
                    }
                }
            } else {
                Log.record(memo);
                Log.runtime(s);
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "receiveToolTaskReward err:");
            Log.printStackTrace(TAG, t);
        }
    }

    private void harvestProduce(String farmId) {
        try {
            String s = AntFarmRpcCall.harvestProduce(farmId);
            JSONObject jo = new JSONObject(s);
            String memo = jo.getString("memo");
            if (ResChecker.checkRes(TAG, jo)) {
                double harvest = jo.getDouble("harvestBenevolenceScore");
                harvestBenevolenceScore = jo.getDouble("finalBenevolenceScore");
                Log.farm("收取鸡蛋🥚[" + harvest + "颗]#剩余" + harvestBenevolenceScore + "颗");
            } else {
                Log.record(memo);
                Log.runtime(s);
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "harvestProduce err:");
            Log.printStackTrace(TAG, t);
        }
    }

    /* 捐赠爱心鸡蛋 */
    private void handleDonation(int donationType) {
        try {
            String s = AntFarmRpcCall.listActivityInfo();
            JSONObject jo = new JSONObject(s);
            String memo = jo.getString("memo");
            if (ResChecker.checkRes(TAG, jo)) {
                JSONArray jaActivityInfos = jo.getJSONArray("activityInfos");
                String activityId = null, activityName;
                boolean isDonation = false;
                for (int i = 0; i < jaActivityInfos.length(); i++) {
                    jo = jaActivityInfos.getJSONObject(i);
                    if (!jo.get("donationTotal").equals(jo.get("donationLimit"))) {
                        activityId = jo.getString("activityId");
                        activityName = jo.optString("projectName", activityId);
                        if (performDonation(activityId, activityName)) {
                            isDonation = true;
                            if (donationType == DonationCount.ONE) {
                                break;
                            }
                        }
                    }
                }
                if (isDonation) {
                    String userId = UserMap.getCurrentUid();
                    Status.donationEgg(userId);
                }
                if (activityId == null) {
                    Log.record(TAG, "今日已无可捐赠的活动");
                }
            } else {
                Log.record(memo);
                Log.runtime(s);
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "donation err:");
            Log.printStackTrace(TAG, t);
        }
    }

    private Boolean performDonation(String activityId, String activityName) {
        try {
            String s = AntFarmRpcCall.donation(activityId, 1);
            JSONObject donationResponse = new JSONObject(s);
            String memo = donationResponse.getString("memo");
            if (ResChecker.checkRes(TAG, donationResponse)) {
                JSONObject donationDetails = donationResponse.getJSONObject("donation");
                harvestBenevolenceScore = donationDetails.getDouble("harvestBenevolenceScore");
                Log.farm("捐赠活动❤️[" + activityName + "]#累计捐赠" + donationDetails.getInt("donationTimesStat") + "次");
                return true;
            } else {
                Log.record(memo);
                Log.runtime(s);
            }
        } catch (Throwable t) {
            Log.printStackTrace(t);
        }
        return false;
    }

    private void answerQuestion(String activityId) {
        try {
            String today = TimeUtil.getDateStr2();
            String tomorrow = TimeUtil.getDateStr2(1);
            Map<String, String> farmAnswerCache = DataCache.INSTANCE.getData(FARM_ANSWER_CACHE_KEY, new HashMap<>());
            cleanOldAnswers(farmAnswerCache, today);

            // 检查是否今天已经答过题
            if (Status.hasFlagToday(ANSWERED_FLAG)) {
                if (!Status.hasFlagToday(CACHED_FLAG)) {
                    JSONObject jo = new JSONObject(DadaDailyRpcCall.home(activityId));
                    if (ResChecker.checkRes(TAG, jo)) {
                        JSONArray operationConfigList = jo.getJSONArray("operationConfigList");
                        updateTomorrowAnswerCache(operationConfigList, tomorrow);
                        Status.setFlagToday(CACHED_FLAG);
                    }
                }
                return;
            }

            // 获取题目信息
            JSONObject jo = new JSONObject(DadaDailyRpcCall.home(activityId));
            if (!ResChecker.checkRes(TAG, jo)) return;

            JSONObject question = jo.getJSONObject("question");
            long questionId = question.getLong("questionId");
            JSONArray labels = question.getJSONArray("label");
            String title = question.getString("title");

            String answer = null;
            boolean cacheHit = false;
            String cacheKey = title + "|" + today;

            // 改进的缓存匹配逻辑
            if (farmAnswerCache != null && farmAnswerCache.containsKey(cacheKey)) {
                String cachedAnswer = farmAnswerCache.get(cacheKey);
                Log.farm("🎉 缓存[" + cachedAnswer + "] 🎯 题目：" + cacheKey);

                // 1. 首先尝试精确匹配
                for (int i = 0; i < labels.length(); i++) {
                    String option = labels.getString(i);
                    if (option.equals(cachedAnswer)) {
                        answer = option;
                        cacheHit = true;
                        break;
                    }
                }

                // 2. 如果精确匹配失败，尝试模糊匹配
                if (!cacheHit) {
                    for (int i = 0; i < labels.length(); i++) {
                        String option = labels.getString(i);
                        if (option.contains(cachedAnswer) || cachedAnswer.contains(option)) {
                            answer = option;
                            cacheHit = true;
                            Log.farm("⚠️ 缓存模糊匹配成功：" + cachedAnswer + " → " + option);
                            break;
                        }
                    }
                }

            }

            // 缓存未命中时调用AI
            if (!cacheHit) {
                Log.record(TAG, "缓存未命中，尝试使用AI答题：" + title);
                answer = AnswerAI.getAnswer(title, JsonUtil.jsonArrayToList(labels), "farm");
                if (answer == null || answer.isEmpty()) {
                    answer = labels.getString(0); // 默认选择第一个选项
                }
            }

            // 提交答案
            JSONObject joDailySubmit = new JSONObject(DadaDailyRpcCall.submit(activityId, answer, questionId));
            Status.setFlagToday(ANSWERED_FLAG);
            if (ResChecker.checkRes(TAG, joDailySubmit)) {
                JSONObject extInfo = joDailySubmit.getJSONObject("extInfo");
                boolean correct = joDailySubmit.getBoolean("correct");
                Log.farm("饲料任务答题：" + (correct ? "正确" : "错误") + "领取饲料［" + extInfo.getString("award") + "g］");
                JSONArray operationConfigList = joDailySubmit.getJSONArray("operationConfigList");
                updateTomorrowAnswerCache(operationConfigList, tomorrow);
                Status.setFlagToday(CACHED_FLAG);
            }
        } catch (Exception e) {
            Log.printStackTrace(TAG, "答题出错", e);
        }
    }

    /**
     * 更新明日答案缓存
     *
     * @param operationConfigList 操作配置列表
     * @param date                日期字符串，格式 "yyyy-MM-dd"
     */
    private void updateTomorrowAnswerCache(JSONArray operationConfigList, String date) {
        try {
            Log.runtime(TAG, "updateTomorrowAnswerCache 开始更新缓存");
            Map<String, String> farmAnswerCache = DataCache.INSTANCE.getData(FARM_ANSWER_CACHE_KEY, new HashMap<>());
            if (farmAnswerCache == null) {
                farmAnswerCache = new HashMap<>();
            }
            for (int j = 0; j < operationConfigList.length(); j++) {
                JSONObject operationConfig = operationConfigList.getJSONObject(j);
                String type = operationConfig.getString("type");
                if ("PREVIEW_QUESTION".equals(type)) {
                    String previewTitle = operationConfig.getString("title") + "|" + date;
                    JSONArray actionTitle = new JSONArray(operationConfig.getString("actionTitle"));
                    for (int k = 0; k < actionTitle.length(); k++) {
                        JSONObject joActionTitle = actionTitle.getJSONObject(k);
                        boolean isCorrect = joActionTitle.getBoolean("correct");
                        if (isCorrect) {
                            String nextAnswer = joActionTitle.getString("title");
                            farmAnswerCache.put(previewTitle, nextAnswer); // 缓存下一个问题的答案
                        }
                    }
                }
            }
            DataCache.INSTANCE.saveData(FARM_ANSWER_CACHE_KEY, farmAnswerCache);
            Log.runtime(TAG, "updateTomorrowAnswerCache 缓存更新完毕");
        } catch (Exception e) {
            Log.printStackTrace(TAG, "updateTomorrowAnswerCache 错误:", e);
        }
    }


    /**
     * 清理缓存超过7天的B答案
     */
    private void cleanOldAnswers(Map<String, String> farmAnswerCache, String today) {
        try {
            Log.runtime(TAG, "cleanOldAnswers 开始清理缓存");
            if (farmAnswerCache == null || farmAnswerCache.isEmpty()) return;
            // 将今天日期转为数字格式：20250405
            int todayInt = convertDateToInt(today); // 如 "2025-04-05" → 20250405
            // 设置保留天数（例如7天）
            int daysToKeep = 7;
            Map<String, String> cleanedMap = new HashMap<>();
            for (Map.Entry<String, String> entry : farmAnswerCache.entrySet()) {
                String key = entry.getKey();
                if (key.contains("|")) {
                    String[] parts = key.split("\\|", 2);
                    if (parts.length == 2) {
                        String dateStr = parts[1];//获取日期部分 20
                        int dateInt = convertDateToInt(dateStr);
                        if (dateInt == -1) continue;
                        if (todayInt - dateInt <= daysToKeep) {
                            cleanedMap.put(entry.getKey(), entry.getValue());//保存7天内的答案
                            Log.runtime(TAG, "保留 日期：" + todayInt + "缓存日期：" + dateInt + " 题目：" + parts[0]);
                        }
                    }
                }
            }
            DataCache.INSTANCE.saveData(FARM_ANSWER_CACHE_KEY, cleanedMap);
            Log.runtime(TAG, "cleanOldAnswers 清理缓存完毕");
        } catch (Exception e) {
            Log.printStackTrace(TAG, "cleanOldAnswers error:", e);
        }
    }

    /**
     * 将日期字符串转为数字格式
     *
     * @param dateStr 日期字符串，格式 "yyyy-MM-dd"
     * @return 日期数字格式，如 "2025-04-05" → 20250405
     */
    private int convertDateToInt(String dateStr) {
        Log.runtime(TAG, "convertDateToInt 开始转换日期：" + dateStr);
        if (dateStr == null || dateStr.length() != 10 || dateStr.charAt(4) != '-' || dateStr.charAt(7) != '-') {
            Log.error("日期格式错误：" + dateStr);
            return -1; // 格式错误
        }
        try {
            int year = Integer.parseInt(dateStr.substring(0, 4));
            int month = Integer.parseInt(dateStr.substring(5, 7));
            int day = Integer.parseInt(dateStr.substring(8, 10));
            if (month < 1 || month > 12 || day < 1 || day > 31) {
                Log.error("日期无效：" + dateStr);
                return -1; // 日期无效
            }
            return year * 10000 + month * 100 + day;
        } catch (NumberFormatException e) {
            Log.error(TAG, "日期转换失败：" + dateStr + e.getMessage());
            return -1;
        }
    }


    private void recordFarmGame(GameType gameType) {
        try {
            do {
                try {
                    JSONObject jo = new JSONObject(AntFarmRpcCall.initFarmGame(gameType.name()));
                    if (ResChecker.checkRes(TAG, jo)) {
                        if (jo.getJSONObject("gameAward").getBoolean("level3Get")) {
                            return;
                        }
                        if (jo.optInt("remainingGameCount", 1) == 0) {
                            return;
                        }
                        jo = new JSONObject(AntFarmRpcCall.recordFarmGame(gameType.name()));
                        if (ResChecker.checkRes(TAG, jo)) {
                            JSONArray awardInfos = jo.getJSONArray("awardInfos");
                            StringBuilder award = new StringBuilder();
                            for (int i = 0; i < awardInfos.length(); i++) {
                                JSONObject awardInfo = awardInfos.getJSONObject(i);
                                award.append(awardInfo.getString("awardName")).append("*").append(awardInfo.getInt("awardCount"));
                            }
                            if (jo.has("receiveFoodCount")) {
                                award.append(";肥料*").append(jo.getString("receiveFoodCount"));
                            }
                            Log.farm("庄园游戏🎮[" + gameType.gameName() + "]#" + award);
                            if (jo.optInt("remainingGameCount", 0) > 0) {
                                continue;
                            }
                        } else {
                            Log.runtime(TAG, "庄园游戏" + jo);
                        }
                    } else {
                        Log.runtime(TAG, "进入庄园游戏失败" + jo);
                    }
                    break;
                } finally {
                    GlobalThreadPools.sleep(2000);
                }
            } while (true);
        } catch (Throwable t) {
            Log.runtime(TAG, "recordFarmGame err:");
            Log.printStackTrace(TAG, t);
        }
    }

    /**
     * 庄园任务，目前支持i
     * 视频，杂货铺，抽抽乐，家庭，618会场，芭芭农场，小鸡厨房
     * 添加组件，雇佣，会员签到，逛咸鱼，今日头条极速版，UC浏览器
     * 一起拿饲料，到店付款，线上支付，鲸探
     */
    private void doFarmTasks() {
        try {
            List<String> taskList = new ArrayList<>(List.of(
                    "HEART_DONATION_ADVANCED_FOOD_V2",
                    "HEART_DONATE"
            ));
            List<String> cachedList = DataCache.INSTANCE.getData("farmCompletedTaskSet", taskList);
            taskList = new ArrayList<>(new LinkedHashSet<>(cachedList)); // 去重可选
            JSONObject jo = new JSONObject(AntFarmRpcCall.listFarmTask());
            if (ResChecker.checkRes(TAG, jo)) {
                JSONArray farmTaskList = jo.getJSONArray("farmTaskList");
                for (int i = 0; i < farmTaskList.length(); i++) {
                    JSONObject task = farmTaskList.getJSONObject(i);
                    String title = task.optString("title", "未知任务");
                    String taskStatus = task.getString("taskStatus");
                    String bizKey = task.getString("bizKey");
                    String taskMode = task.optString("taskMode");
                    // 跳过已被屏蔽的任务
                    if (taskList.contains(bizKey)) {
                        continue;
                    }
                    if (TaskStatus.TODO.name().equals(taskStatus)) {
                        if (!taskList.contains(bizKey)) {
                            if ("VIDEO_TASK".equals(bizKey)) {
                                JSONObject taskVideoDetailjo = new JSONObject(AntFarmRpcCall.queryTabVideoUrl());
                                if (ResChecker.checkRes(TAG, taskVideoDetailjo)) {
                                    String videoUrl = taskVideoDetailjo.getString("videoUrl");
                                    String contentId = videoUrl.substring(videoUrl.indexOf("&contentId=") + 11, videoUrl.indexOf("&refer"));
                                    JSONObject videoDetailjo = new JSONObject(AntFarmRpcCall.videoDeliverModule(contentId));
                                    if (ResChecker.checkRes(TAG, videoDetailjo)) {
                                        GlobalThreadPools.sleep(15 * 1000L);
                                        JSONObject resultVideojo = new JSONObject(AntFarmRpcCall.videoTrigger(contentId));
                                        if (ResChecker.checkRes(TAG, resultVideojo)) {
                                            Log.farm("庄园任务🧾[" + title + "]");
                                        }
                                    }
                                }
                            } else if ("ANSWER".equals(bizKey)) {
                                answerQuestion("100"); //答题
                            } else {
                                JSONObject taskDetailjo = new JSONObject(AntFarmRpcCall.doFarmTask(bizKey));
                                if (ResChecker.checkRes(TAG, taskDetailjo)) {
                                    Log.farm("庄园任务🧾[" + title + "]");
                                } else {
                                    Log.error("庄园任务失败：" + title + "\n" + taskDetailjo);
                                    taskList.add(bizKey); // 避免重复失败
                                }
                            }
                        }
                    }
                    if ("ANSWER".equals(bizKey) && !Status.hasFlagToday(CACHED_FLAG)) {//单独处理答题任务
                        answerQuestion("100"); //答题
                    }
                    GlobalThreadPools.sleep(1000);
                }
            }
            DataCache.INSTANCE.saveData("farmCompletedTaskSet", taskList);
        } catch (Throwable t) {
            Log.printStackTrace(TAG, "doFarmTasks 错误:", t);
        }
    }

    private void receiveFarmAwards() {
        try {
            boolean doubleCheck;
            do {
                doubleCheck = false;
                JSONObject jo = new JSONObject(AntFarmRpcCall.listFarmTask());
                if (ResChecker.checkRes(TAG, jo)) {
                    JSONArray farmTaskList = jo.getJSONArray("farmTaskList");
                    JSONObject signList = jo.getJSONObject("signList");
                    farmSign(signList);
                    for (int i = 0; i < farmTaskList.length(); i++) {
                        JSONObject task = farmTaskList.getJSONObject(i);
                        String taskStatus = task.getString("taskStatus");
                        String taskTitle = task.optString("title", "未知任务");
                        int awardCount = task.optInt("awardCount", 0);
                        String taskId = task.optString("taskId");
                        if (TaskStatus.FINISHED.name().equals(taskStatus)) {
                            if (Objects.equals(task.optString("awardType"), "ALLPURPOSE")) {
                                if (awardCount + foodStock > foodStockLimit) {
                                    unreceiveTaskAward++;
                                    Log.record(TAG, taskTitle + "领取" + awardCount + "g饲料后将超过[" + foodStockLimit + "g]上限，终止领取");
                                    break;
                                }
                            }
                            JSONObject receiveTaskAwardjo = new JSONObject(AntFarmRpcCall.receiveFarmTaskAward(taskId));
                            if (ResChecker.checkRes(TAG, receiveTaskAwardjo)) {
                                add2FoodStock(awardCount);
                                Log.farm("庄园奖励🎖️[" + taskTitle + "]#" + awardCount + "g");
                                doubleCheck = true;
                                if (unreceiveTaskAward > 0)
                                    unreceiveTaskAward--;
                            }
                        }
                        GlobalThreadPools.sleep(1000);
                    }
                }
            } while (doubleCheck);
        } catch (Throwable t) {
            Log.printStackTrace(TAG, "receiveFarmAwards 错误:", t);
        }
    }

    private void farmSign(JSONObject signList) {
        try {
            String flag = "farm::sign";
            if (Status.hasFlagToday(flag)) return;
            JSONArray jaFarmSignList = signList.getJSONArray("signList");
            String currentSignKey = signList.getString("currentSignKey");
            for (int i = 0; i < jaFarmSignList.length(); i++) {
                JSONObject jo = jaFarmSignList.getJSONObject(i);
                String signKey = jo.getString("signKey");
                boolean signed = jo.getBoolean("signed");
                String awardCount = jo.getString("awardCount");
                if (currentSignKey.equals(signKey)) {
                    if (!signed) {
                        String signResponse = AntFarmRpcCall.sign();
                        if (ResChecker.checkRes(TAG, signResponse)) {
                            Log.farm("庄园签到📅获得饲料" + awardCount + "g");
                            Status.setFlagToday(flag);
                        }
                    }
                    return;
                }
            }
        } catch (JSONException e) {
            Log.printStackTrace(TAG, "庄园签到 JSON解析错误:", e);
        }
    }

    /**
     * 喂鸡
     *
     * @param farmId 庄园ID
     * @return true: 喂鸡成功，false: 喂鸡失败
     */
    private Boolean feedAnimal(String farmId) {
        try {
            if (foodStock < 180) {
                Log.record(TAG, "喂鸡饲料不足");
            } else {
                JSONObject jo = new JSONObject(AntFarmRpcCall.feedAnimal(farmId));
                int feedFood = foodStock - jo.getInt("foodStock");
                add2FoodStock(-feedFood);
                Log.farm("投喂小鸡🥣[" + feedFood + "g]#剩余" + foodStock + "g");
                return true;
            }
        } catch (Throwable t) {
            Log.printStackTrace(TAG, "feedAnimal err:", t);
        }
        return false;
    }

    /**
     * 加载持有道具信息
     */
    private void listFarmTool() {
        try {
            JSONObject jo = new JSONObject(AntFarmRpcCall.listFarmTool());
            if (ResChecker.checkRes(TAG, jo)) {
                JSONArray jaToolList = jo.getJSONArray("toolList");
                farmTools = new FarmTool[jaToolList.length()];
                for (int i = 0; i < jaToolList.length(); i++) {
                    jo = jaToolList.getJSONObject(i);
                    farmTools[i] = new FarmTool();
                    farmTools[i].toolId = jo.optString("toolId", "");
                    farmTools[i].toolType = ToolType.valueOf(jo.getString("toolType"));
                    farmTools[i].toolCount = jo.getInt("toolCount");
                    farmTools[i].toolHoldLimit = jo.optInt("toolHoldLimit", 20);
                }
            }
        } catch (Throwable t) {
            Log.printStackTrace(TAG, "listFarmTool err:", t);
        }
    }

    /**
     * 连续使用加速卡
     *
     * @return true: 使用成功，false: 使用失败
     */
    private Boolean useAccelerateTool() {
        if (!Status.canUseAccelerateTool()) {
            return false;
        }
        if (!useAccelerateToolContinue.getValue() && AnimalBuff.ACCELERATING.name().equals(ownerAnimal.animalBuff)) {
            return false;
        }
        syncAnimalStatus(ownerFarmId);
        double consumeSpeed = 0d;
        double allFoodHaveEatten = 0d;
        long nowTime = System.currentTimeMillis() / 1000;
        for (Animal animal : animals) {
            if (animal.masterFarmId.equals(ownerFarmId)) {
                consumeSpeed = animal.consumeSpeed;
            }
            allFoodHaveEatten += animal.foodHaveEatten;
            allFoodHaveEatten += animal.consumeSpeed * (nowTime - (double) animal.startEatTime / 1000);
        }
        // consumeSpeed: g/s
        // AccelerateTool: -1h = -60m = -3600s
        boolean isUseAccelerateTool = false;
        while (180 - allFoodHaveEatten >= consumeSpeed * 3600) {
            if ((useAccelerateToolWhenMaxEmotion.getValue() && finalScore != 100)) {
                break;
            }
            if (useFarmTool(ownerFarmId, ToolType.ACCELERATETOOL)) {
                allFoodHaveEatten += consumeSpeed * 3600;
                isUseAccelerateTool = true;
                Status.useAccelerateTool();
                GlobalThreadPools.sleep(1000);
            } else {
                break;
            }
            if (!useAccelerateToolContinue.getValue()) {
                break;
            }
        }
        return isUseAccelerateTool;
    }

    private Boolean useFarmTool(String targetFarmId, ToolType toolType) {
        try {
            String s = AntFarmRpcCall.listFarmTool();
            JSONObject jo = new JSONObject(s);
            String memo = jo.getString("memo");
            if (ResChecker.checkRes(TAG, jo)) {
                JSONArray jaToolList = jo.getJSONArray("toolList");
                for (int i = 0; i < jaToolList.length(); i++) {
                    jo = jaToolList.getJSONObject(i);
                    if (toolType.name().equals(jo.getString("toolType"))) {
                        int toolCount = jo.getInt("toolCount");
                        if (toolCount > 0) {
                            String toolId = "";
                            if (jo.has("toolId"))
                                toolId = jo.getString("toolId");
                            s = AntFarmRpcCall.useFarmTool(targetFarmId, toolId, toolType.name());
                            jo = new JSONObject(s);
                            memo = jo.getString("memo");
                            if (ResChecker.checkRes(TAG, jo)) {
                                Log.farm("使用道具🎭[" + toolType.nickName() + "]#剩余" + (toolCount - 1) + "张");
                                return true;
                            } else {
                                Log.record(memo);
                            }
                            Log.runtime(s);
                        }
                        break;
                    }
                }
            } else {
                Log.record(memo);
                Log.runtime(s);
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "useFarmTool err:");
            Log.printStackTrace(TAG, t);
        }
        return false;
    }

    private void feedFriend() {
        try {
            Map<String, Integer> feedFriendAnimalMap = feedFriendAnimalList.getValue();
            for (Map.Entry<String, Integer> entry : feedFriendAnimalMap.entrySet()) {
                String userId = entry.getKey();
                if (userId.equals(UserMap.getCurrentUid()))//跳过自己
                    continue;
                if (!Status.canFeedFriendToday(userId, entry.getValue()))
                    continue;
                JSONObject jo = new JSONObject(AntFarmRpcCall.enterFarm(userId, userId));
                GlobalThreadPools.sleep(3 * 1000L);//延迟3秒
                if (ResChecker.checkRes(TAG, jo)) {
                    JSONObject subFarmVOjo = jo.getJSONObject("farmVO").getJSONObject("subFarmVO");
                    String friendFarmId = subFarmVOjo.getString("farmId");
                    JSONArray jaAnimals = subFarmVOjo.getJSONArray("animals");
                    for (int j = 0; j < jaAnimals.length(); j++) {
                        JSONObject animalsjo = jaAnimals.getJSONObject(j);

                        String masterFarmId = animalsjo.getString("masterFarmId");
                        if (masterFarmId.equals(friendFarmId)) { //遍历到的鸡 如果在自己的庄园
                            JSONObject animalStatusVO = animalsjo.getJSONObject("animalStatusVO");
                            String animalInteractStatus = animalStatusVO.getString("animalInteractStatus");//动物互动状态
                            String animalFeedStatus = animalStatusVO.getString("animalFeedStatus");//动物饲料状态
                            if (AnimalInteractStatus.HOME.name().equals(animalInteractStatus) && AnimalFeedStatus.HUNGRY.name().equals(animalFeedStatus)) { //状态是饥饿 并且在庄园
                                String user = UserMap.getMaskName(userId);//喂 给我喂
                                if (foodStock < 180) {
                                    if (unreceiveTaskAward > 0) {
                                        Log.record(TAG, "✨还有待领取的饲料");
                                        receiveFarmAwards();//先去领个饲料
                                    }
                                }
                                //第二次检查
                                if (foodStock >= 180) {
                                    if (Status.hasFlagToday("farm::feedFriendLimit")) {
                                        return;
                                    }
                                    JSONObject feedFriendAnimaljo = new JSONObject(AntFarmRpcCall.feedFriendAnimal(friendFarmId));
                                    if (ResChecker.checkRes(TAG, feedFriendAnimaljo)) {
                                        int feedFood = foodStock - feedFriendAnimaljo.getInt("foodStock");
                                        if (feedFood > 0) {
                                            add2FoodStock(-feedFood);
                                            Log.farm("帮喂好友🥣[" + user + "]的小鸡[" + feedFood + "g]#剩余" + foodStock + "g");
                                            Status.feedFriendToday(AntFarmRpcCall.farmId2UserId(friendFarmId));
                                        }
                                    } else {
                                        Log.error(TAG, "😞喂[" + user + "]的鸡失败" + feedFriendAnimaljo);
                                        Status.setFlagToday("farm::feedFriendLimit");
                                        break;
                                    }
                                } else {
                                    Log.record(TAG, "😞喂鸡[" + user + "]饲料不足");
                                }

                            }
                            break;
                        }
                    }
                }
            }
        } catch (Throwable t) {
            Log.printStackTrace(TAG, "feedFriendAnimal err:", t);
        }
    }


    private void notifyFriend() {
        if (foodStock >= foodStockLimit)
            return;
        try {
            boolean hasNext = false;
            int pageStartSum = 0;
            String s;
            JSONObject jo;
            do {
                s = AntFarmRpcCall.rankingList(pageStartSum);
                jo = new JSONObject(s);
                String memo = jo.getString("memo");
                if (ResChecker.checkRes(TAG, jo)) {
                    hasNext = jo.getBoolean("hasNext");
                    JSONArray jaRankingList = jo.getJSONArray("rankingList");
                    pageStartSum += jaRankingList.length();
                    for (int i = 0; i < jaRankingList.length(); i++) {
                        jo = jaRankingList.getJSONObject(i);
                        String userId = jo.getString("userId");
                        String userName = UserMap.getMaskName(userId);
                        boolean isNotifyFriend = notifyFriendList.getValue().contains(userId);
                        if (notifyFriendType.getValue() == NotifyFriendType.DONT_NOTIFY) {
                            isNotifyFriend = !isNotifyFriend;
                        }
                        if (!isNotifyFriend || userId.equals(UserMap.getCurrentUid())) {
                            continue;
                        }
                        boolean starve = jo.has("actionType") && "starve_action".equals(jo.getString("actionType"));
                        if (jo.getBoolean("stealingAnimal") && !starve) {
                            s = AntFarmRpcCall.enterFarm(userId, userId);
                            jo = new JSONObject(s);
                            memo = jo.getString("memo");
                            if (ResChecker.checkRes(TAG, jo)) {
                                jo = jo.getJSONObject("farmVO").getJSONObject("subFarmVO");
                                String friendFarmId = jo.getString("farmId");
                                JSONArray jaAnimals = jo.getJSONArray("animals");
                                boolean notified = !notifyFriend.getValue();
                                for (int j = 0; j < jaAnimals.length(); j++) {
                                    jo = jaAnimals.getJSONObject(j);
                                    String animalId = jo.getString("animalId");
                                    String masterFarmId = jo.getString("masterFarmId");
                                    if (!masterFarmId.equals(friendFarmId) && !masterFarmId.equals(ownerFarmId)) {
                                        if (notified)
                                            continue;
                                        jo = jo.getJSONObject("animalStatusVO");
                                        notified = notifyFriend(jo, friendFarmId, animalId, userName);
                                    }
                                }
                            } else {
                                Log.record(memo);
                                Log.runtime(s);
                            }
                        }
                    }
                } else {
                    Log.record(memo);
                    Log.runtime(s);
                }
            } while (hasNext);
            Log.record(TAG, "饲料剩余[" + foodStock + "g]");
        } catch (Throwable t) {
            Log.runtime(TAG, "notifyFriend err:");
            Log.printStackTrace(TAG, t);
        }
    }

    private boolean notifyFriend(JSONObject joAnimalStatusVO, String friendFarmId, String animalId, String user) {
        try {
            if (AnimalInteractStatus.STEALING.name().equals(joAnimalStatusVO.getString("animalInteractStatus"))
                    && AnimalFeedStatus.EATING.name().equals(joAnimalStatusVO.getString("animalFeedStatus"))) {
                String s = AntFarmRpcCall.notifyFriend(animalId, friendFarmId);
                JSONObject jo = new JSONObject(s);
                String memo = jo.getString("memo");
                if (ResChecker.checkRes(TAG, jo)) {
                    double rewardCount = jo.getDouble("rewardCount");
                    if (jo.getBoolean("refreshFoodStock"))
                        foodStock = (int) jo.getDouble("finalFoodStock");
                    else
                        add2FoodStock((int) rewardCount);
                    Log.farm("通知好友📧[" + user + "]被偷吃#奖励" + rewardCount + "g");
                    return true;
                } else {
                    Log.record(memo);
                    Log.runtime(s);
                }
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "notifyFriend err:");
            Log.printStackTrace(TAG, t);
        }
        return false;
    }

    /**
     * 解析同步响应状态
     *
     * @param jo 同步响应状态
     */
    private void parseSyncAnimalStatusResponse(JSONObject jo) {
        try {
            if (!jo.has("subFarmVO")) {
                return;
            }
            if (jo.has("emotionInfo")) {//小鸡心情
                finalScore = jo.getJSONObject("emotionInfo").getDouble("finalScore");
            }
            JSONObject subFarmVO = jo.getJSONObject("subFarmVO");
            if (subFarmVO.has("foodStock")) {
                foodStock = subFarmVO.getInt("foodStock");
            }
            if (subFarmVO.has("manureVO")) { //粪肥 鸡屎
                JSONArray manurePotList = subFarmVO.getJSONObject("manureVO").getJSONArray("manurePotList");
                for (int i = 0; i < manurePotList.length(); i++) {
                    JSONObject manurePot = manurePotList.getJSONObject(i);
                    if (manurePot.getInt("manurePotNum") >= 100) {//粪肥数量
                        JSONObject joManurePot = new JSONObject(AntFarmRpcCall.collectManurePot(manurePot.getString("manurePotNO")));
                        if (ResChecker.checkRes(TAG, joManurePot)) {
                            int collectManurePotNum = joManurePot.getInt("collectManurePotNum");
                            Log.farm("打扫鸡屎🧹[" + collectManurePotNum + "g]" + i + 1 + "次");
                        } else {
                            Log.runtime(TAG, "打扫鸡屎失败: 第" + i + 1 + "次" + joManurePot);
                        }
                    }
                }
            }


            ownerFarmId = subFarmVO.getString("farmId");

            JSONObject farmProduce = subFarmVO.getJSONObject("farmProduce");//产物 -🥚
            benevolenceScore = farmProduce.getDouble("benevolenceScore");//慈善评分

            if (subFarmVO.has("rewardList")) {
                JSONArray jaRewardList = subFarmVO.getJSONArray("rewardList");
                if (jaRewardList.length() > 0) {
                    rewardList = new RewardFriend[jaRewardList.length()];
                    for (int i = 0; i < rewardList.length; i++) {
                        JSONObject joRewardList = jaRewardList.getJSONObject(i);
                        if (rewardList[i] == null)
                            rewardList[i] = new RewardFriend();
                        rewardList[i].consistencyKey = joRewardList.getString("consistencyKey");
                        rewardList[i].friendId = joRewardList.getString("friendId");
                        rewardList[i].time = joRewardList.getString("time");
                    }
                }
            }

            JSONArray jaAnimals = subFarmVO.getJSONArray("animals");//小鸡们
            List<Animal> animalList = new ArrayList<>();
            for (int i = 0; i < jaAnimals.length(); i++) {
                JSONObject animalJson = jaAnimals.getJSONObject(i);
                Animal animal = objectMapper.readValue(animalJson.toString(), Animal.class);
                animalList.add(animal);
                if (animal.masterFarmId.equals(ownerFarmId)) {
                    ownerAnimal = animal;
                }
//                Log.record(TAG, "当前动物：" + animal.toString());
            }
            animals = animalList.toArray(new Animal[0]);
        } catch (Throwable t) {
            Log.runtime(TAG, "parseSyncAnimalStatusResponse err:");
            Log.printStackTrace(TAG, t);
        }
    }

    private void add2FoodStock(int i) {
        foodStock += i;
        if (foodStock > foodStockLimit) {
            foodStock = foodStockLimit;
        }
        if (foodStock < 0) {
            foodStock = 0;
        }
    }


    /**
     * 收集每日食材
     */
    private void collectDailyFoodMaterial() {
        try {
            String userId = UserMap.getCurrentUid();
            JSONObject jo = new JSONObject(AntFarmRpcCall.enterKitchen(userId));
            if (ResChecker.checkRes(TAG, jo)) {
                boolean canCollectDailyFoodMaterial = jo.getBoolean("canCollectDailyFoodMaterial");
                int dailyFoodMaterialAmount = jo.getInt("dailyFoodMaterialAmount");
                int garbageAmount = jo.optInt("garbageAmount", 0);
                if (jo.has("orchardFoodMaterialStatus")) {
                    JSONObject orchardFoodMaterialStatus = jo.getJSONObject("orchardFoodMaterialStatus");
                    if ("FINISHED".equals(orchardFoodMaterialStatus.optString("foodStatus"))) {
                        jo = new JSONObject(AntFarmRpcCall.farmFoodMaterialCollect());
                        if (ResChecker.checkRes(TAG, jo)) {
                            Log.farm("小鸡厨房👨🏻‍🍳[领取农场食材]#" + jo.getInt("foodMaterialAddCount") + "g");
                        }
                    }
                }
                if (canCollectDailyFoodMaterial) {
                    jo = new JSONObject(AntFarmRpcCall.collectDailyFoodMaterial(dailyFoodMaterialAmount));
                    if (ResChecker.checkRes(TAG, jo)) {
                        Log.farm("小鸡厨房👨🏻‍🍳[领取今日食材]#" + dailyFoodMaterialAmount + "g");
                    }
                }
                if (garbageAmount > 0) {
                    jo = new JSONObject(AntFarmRpcCall.collectKitchenGarbage());
                    if (ResChecker.checkRes(TAG, jo)) {
                        Log.farm("小鸡厨房👨🏻‍🍳[领取肥料]#" + jo.getInt("recievedKitchenGarbageAmount") + "g");
                    }
                }
            }
        } catch (Throwable t) {
            Log.printStackTrace(TAG, "收集每日食材", t);
        }
    }

    /**
     * 领取爱心食材店食材
     */
    private void collectDailyLimitedFoodMaterial() {
        try {
            JSONObject jo = new JSONObject(AntFarmRpcCall.queryFoodMaterialPack());
            if (ResChecker.checkRes(TAG, jo)) {
                boolean canCollectDailyLimitedFoodMaterial = jo.getBoolean("canCollectDailyLimitedFoodMaterial");
                if (canCollectDailyLimitedFoodMaterial) {
                    int dailyLimitedFoodMaterialAmount = jo.getInt("dailyLimitedFoodMaterialAmount");
                    jo = new JSONObject(AntFarmRpcCall.collectDailyLimitedFoodMaterial(dailyLimitedFoodMaterialAmount));
                    if (ResChecker.checkRes(TAG, jo)) {
                        Log.farm("小鸡厨房👨🏻‍🍳[领取爱心食材店食材]#" + dailyLimitedFoodMaterialAmount + "g");
                    }
                }
            }
        } catch (Throwable t) {
            Log.printStackTrace(TAG, "领取爱心食材店食材", t);
        }
    }

    private void cook() {
        try {
            String userId = UserMap.getCurrentUid();
            JSONObject jo = new JSONObject(AntFarmRpcCall.enterKitchen(userId));
            Log.runtime(TAG, "cook userid :" + userId);
            if (ResChecker.checkRes(TAG, jo)) {
                int cookTimesAllowed = jo.getInt("cookTimesAllowed");
                if (cookTimesAllowed > 0) {
                    for (int i = 0; i < cookTimesAllowed; i++) {
                        jo = new JSONObject(AntFarmRpcCall.cook(userId, "VILLA"));
                        if (ResChecker.checkRes(TAG, jo)) {
                            JSONObject cuisineVO = jo.getJSONObject("cuisineVO");
                            Log.farm("小鸡厨房👨🏻‍🍳[" + cuisineVO.getString("name") + "]制作成功");
                        } else {
                            Log.runtime(TAG, "小鸡厨房制作" + jo);
                        }
                        GlobalThreadPools.sleep(RandomUtil.delay());
                    }
                }
            } else {
                Log.runtime(TAG, "小鸡厨房制作1" + jo);
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "cook err:");
            Log.printStackTrace(TAG, t);
        }
    }

    private void useSpecialFood(JSONArray cuisineList) {
        try {
            JSONObject jo;
            String cookbookId;
            String cuisineId;
            String name;
            for (int i = 0; i < cuisineList.length(); i++) {
                jo = cuisineList.getJSONObject(i);
                if (jo.getInt("count") <= 0)
                    continue;
                cookbookId = jo.getString("cookbookId");
                cuisineId = jo.getString("cuisineId");
                name = jo.getString("name");
                jo = new JSONObject(AntFarmRpcCall.useFarmFood(cookbookId, cuisineId));
                if (ResChecker.checkRes(TAG, jo)) {
                    double deltaProduce = jo.getJSONObject("foodEffect").getDouble("deltaProduce");
                    Log.farm("使用美食🍱[" + name + "]#加速" + deltaProduce + "颗爱心鸡蛋");
                }
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "useFarmFood err:");
            Log.printStackTrace(TAG, t);
        }
    }

    private void drawLotteryPlus(JSONObject lotteryPlusInfo) {
        try {
            if (!lotteryPlusInfo.has("userSevenDaysGiftsItem"))
                return;
            String itemId = lotteryPlusInfo.getString("itemId");
            JSONObject userSevenDaysGiftsItem = lotteryPlusInfo.getJSONObject("userSevenDaysGiftsItem");
            JSONArray userEverydayGiftItems = userSevenDaysGiftsItem.getJSONArray("userEverydayGiftItems");
            for (int i = 0; i < userEverydayGiftItems.length(); i++) {
                userSevenDaysGiftsItem = userEverydayGiftItems.getJSONObject(i);
                if (userSevenDaysGiftsItem.getString("itemId").equals(itemId)) {
                    if (!userSevenDaysGiftsItem.getBoolean("received")) {
                        String singleDesc = userSevenDaysGiftsItem.getString("singleDesc");
                        int awardCount = userSevenDaysGiftsItem.getInt("awardCount");
                        if (singleDesc.contains("饲料") && awardCount + foodStock > foodStockLimit) {
                            Log.record(TAG, "暂停领取[" + awardCount + "]g饲料，上限为[" + foodStockLimit + "]g");
                            break;
                        }
                        userSevenDaysGiftsItem = new JSONObject(AntFarmRpcCall.drawLotteryPlus());
                        if ("SUCCESS".equals(userSevenDaysGiftsItem.getString("memo"))) {
                            Log.farm("惊喜礼包🎁[" + singleDesc + "*" + awardCount + "]");
                        }
                    }
                    break;
                }
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "drawLotteryPlus err:");
            Log.printStackTrace(TAG, t);
        }
    }

    private void visit() {
        try {
            Map<String, Integer> map = visitFriendList.getValue();
            for (Map.Entry<String, Integer> entry : map.entrySet()) {
                String userId = entry.getKey();
                Integer count = entry.getValue();
                if (userId.equals(UserMap.getCurrentUid()))
                    continue;
                if (count <= 0)
                    continue;
                if (count > 3)
                    count = 3;
                if (Status.canVisitFriendToday(userId, count)) {
                    count = visitFriend(userId, count);
                    if (count > 0)
                        Status.visitFriendToday(userId, count);
                }
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "visit err:");
            Log.printStackTrace(TAG, t);
        }
    }

    private int visitFriend(String userId, int count) {
        int visitedTimes = 0;
        try {
            String s = AntFarmRpcCall.enterFarm(userId, userId);
            JSONObject jo = new JSONObject(s);
            if (ResChecker.checkRes(TAG, jo)) {
                JSONObject farmVO = jo.getJSONObject("farmVO");
                foodStock = farmVO.getInt("foodStock");
                JSONObject subFarmVO = farmVO.getJSONObject("subFarmVO");
                if (subFarmVO.optBoolean("visitedToday", true))
                    return 3;
                String farmId = subFarmVO.getString("farmId");
                for (int i = 0; i < count; i++) {
                    if (foodStock < 10)
                        break;
                    jo = new JSONObject(AntFarmRpcCall.visitFriend(farmId));
                    if (ResChecker.checkRes(TAG, jo)) {
                        foodStock = jo.getInt("foodStock");
                        Log.farm("赠送麦子🌾[" + UserMap.getMaskName(userId) + "]#" + jo.getInt("giveFoodNum") + "g");
                        visitedTimes++;
                        if (jo.optBoolean("isReachLimit")) {
                            Log.record(TAG, "今日给[" + UserMap.getMaskName(userId) + "]送麦子已达上限");
                            visitedTimes = 3;
                            break;
                        }
                    } else {
                        Log.record(jo.getString("memo"));
                        Log.runtime(jo.toString());
                    }
                    GlobalThreadPools.sleep(1000L);
                }
            } else {
                Log.record(jo.getString("memo"));
                Log.runtime(s);
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "visitFriend err:");
            Log.printStackTrace(TAG, t);
        }
        return visitedTimes;
    }

    private void acceptGift() {
        try {
            JSONObject jo = new JSONObject(AntFarmRpcCall.acceptGift());
            if (ResChecker.checkRes(TAG, jo)) {
                int receiveFoodNum = jo.getInt("receiveFoodNum");
                Log.farm("收取麦子🌾[" + receiveFoodNum + "g]");
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "acceptGift err:");
            Log.printStackTrace(TAG, t);
        }
    }

    /**
     * 贴贴小鸡
     *
     * @param queryDayStr 日期，格式：yyyy-MM-dd
     */
    private void diaryTietie(String queryDayStr) {
        String diaryDateStr;
        try {
            JSONObject jo = new JSONObject(AntFarmRpcCall.queryChickenDiary(queryDayStr));
            if (ResChecker.checkRes(TAG, jo)) {
                JSONObject data = jo.getJSONObject("data");
                JSONObject chickenDiary = data.getJSONObject("chickenDiary");
                diaryDateStr = chickenDiary.getString("diaryDateStr");
                if (data.has("hasTietie")) {
                    if (!data.optBoolean("hasTietie", true)) {
                        jo = new JSONObject(AntFarmRpcCall.diaryTietie(diaryDateStr, "NEW"));
                        if (ResChecker.checkRes(TAG, jo)) {
                            String prizeType = jo.getString("prizeType");
                            int prizeNum = jo.optInt("prizeNum", 0);
                            Log.farm("[" + diaryDateStr + "]" + "贴贴小鸡💞[" + prizeType + "*" + prizeNum + "]");
                        } else {
                            Log.runtime(TAG, "贴贴小鸡失败:");
                            Log.runtime(jo.getString("memo"), jo.toString());
                        }
                        if (!chickenDiary.has("statisticsList"))
                            return;
                        JSONArray statisticsList = chickenDiary.getJSONArray("statisticsList");
                        if (statisticsList.length() > 0) {
                            for (int i = 0; i < statisticsList.length(); i++) {
                                JSONObject tietieStatus = statisticsList.getJSONObject(i);
                                String tietieRoleId = tietieStatus.getString("tietieRoleId");
                                jo = new JSONObject(AntFarmRpcCall.diaryTietie(diaryDateStr, tietieRoleId));
                                if (ResChecker.checkRes(TAG, jo)) {
                                    String prizeType = jo.getString("prizeType");
                                    int prizeNum = jo.optInt("prizeNum", 0);
                                    Log.farm("[" + diaryDateStr + "]" + "贴贴小鸡💞[" + prizeType + "*" + prizeNum + "]");
                                } else {
                                    Log.runtime(TAG, "贴贴小鸡失败:");
                                    Log.runtime(jo.getString("memo"), jo.toString());
                                }
                            }
                        }
                    }
                }

            } else {
                Log.runtime(TAG, "贴贴小鸡-获取小鸡日记详情 err:");
                Log.runtime(jo.getString("resultDesc"), jo.toString());
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "queryChickenDiary err:");
            Log.printStackTrace(TAG, t);
        }
    }

    /**
     * 点赞小鸡日记
     *
     * @param queryDayStr
     * @return
     */
    private String collectChickenDiary(String queryDayStr) {
        String diaryDateStr = null;
        try {
            JSONObject jo = new JSONObject(AntFarmRpcCall.queryChickenDiary(queryDayStr));
            if (ResChecker.checkRes(TAG, jo)) {
                JSONObject data = jo.getJSONObject("data");
                JSONObject chickenDiary = data.getJSONObject("chickenDiary");
                diaryDateStr = chickenDiary.getString("diaryDateStr");
                // 点赞小鸡日记
                if (!chickenDiary.optBoolean("collectStatus", true)) {
                    String diaryId = chickenDiary.getString("diaryId");
                    jo = new JSONObject(AntFarmRpcCall.collectChickenDiary(diaryId));
                    if (jo.optBoolean("success", true)) {
                        Log.farm("[" + diaryDateStr + "]" + "点赞小鸡日记💞成功");
                    }
                }
            } else {
                Log.runtime(TAG, "日记点赞-获取小鸡日记详情 err:");
                Log.runtime(jo.getString("resultDesc"), jo.toString());
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "queryChickenDiary err:");
            Log.printStackTrace(TAG, t);
        }
        return diaryDateStr;
    }

    private boolean queryChickenDiaryList(String queryMonthStr, Function<String, String> fun) {
        boolean hasPreviousMore = false;
        try {
            JSONObject jo = null;
            if (StringUtil.isEmpty(queryMonthStr)) {
                jo = new JSONObject(AntFarmRpcCall.queryChickenDiaryList());
            } else {
                jo = new JSONObject(AntFarmRpcCall.queryChickenDiaryList(queryMonthStr));
            }
            if (ResChecker.checkRes(TAG, jo)) {
                jo = jo.getJSONObject("data");
                hasPreviousMore = jo.optBoolean("hasPreviousMore", false);
                JSONArray chickenDiaryBriefList = jo.optJSONArray("chickenDiaryBriefList");
                if (chickenDiaryBriefList != null && chickenDiaryBriefList.length() > 0) {
                    for (int i = chickenDiaryBriefList.length() - 1; i >= 0; i--) {
                        jo = chickenDiaryBriefList.getJSONObject(i);
                        if (!jo.optBoolean("read", true) ||
                                !jo.optBoolean("collectStatus")) {
                            String dateStr = jo.getString("dateStr");
                            fun.apply(dateStr);
                            GlobalThreadPools.sleep(300);
                        }
                    }
                }
            } else {
                Log.runtime(jo.getString("resultDesc"), jo.toString());
            }
        } catch (Throwable t) {
            hasPreviousMore = false;
            Log.runtime(TAG, "queryChickenDiaryList err:");
            Log.printStackTrace(TAG, t);
        }
        return hasPreviousMore;
    }

    private void doChickenDiary() {

        if (diaryTietie.getValue()) { // 贴贴小鸡
            diaryTietie("");
        }

        // 小鸡日记点赞
        String dateStr = null;
        YearMonth yearMonth = YearMonth.now();
        boolean previous = false;
        try {
            if (collectChickenDiary.getValue() >= collectChickenDiaryType.ONCE) {
                GlobalThreadPools.sleep(300);
                dateStr = collectChickenDiary("");
            }
            if (collectChickenDiary.getValue() >= collectChickenDiaryType.MONTH) {
                if (dateStr == null) {
                    Log.error(TAG, "小鸡日记点赞-dateStr为空，使用当前日期");
                } else {
                    yearMonth = YearMonth.from(LocalDate.parse(dateStr));
                }
                GlobalThreadPools.sleep(300);
                previous = queryChickenDiaryList(yearMonth.toString(), this::collectChickenDiary);
            }
            if (collectChickenDiary.getValue() >= collectChickenDiaryType.ALL) {
                while (previous) {
                    GlobalThreadPools.sleep(300);
                    yearMonth = yearMonth.minusMonths(1);
                    previous = queryChickenDiaryList(yearMonth.toString(), this::collectChickenDiary);
                }
            }
        } catch (Exception e) {
            Log.runtime(TAG, "doChickenDiary err:");
            Log.printStackTrace(TAG, e);
        }
    }

    private void visitAnimal() {
        try {
            JSONObject jo = new JSONObject(AntFarmRpcCall.visitAnimal());
            if (ResChecker.checkRes(TAG, jo)) {
                if (!jo.has("talkConfigs"))
                    return;
                JSONArray talkConfigs = jo.getJSONArray("talkConfigs");
                JSONArray talkNodes = jo.getJSONArray("talkNodes");
                JSONObject data = talkConfigs.getJSONObject(0);
                String farmId = data.getString("farmId");
                jo = new JSONObject(AntFarmRpcCall.feedFriendAnimalVisit(farmId));
                if (ResChecker.checkRes(TAG, jo)) {
                    for (int i = 0; i < talkNodes.length(); i++) {
                        jo = talkNodes.getJSONObject(i);
                        if (!"FEED".equals(jo.getString("type")))
                            continue;
                        String consistencyKey = jo.getString("consistencyKey");
                        jo = new JSONObject(AntFarmRpcCall.visitAnimalSendPrize(consistencyKey));
                        if (ResChecker.checkRes(TAG, jo)) {
                            String prizeName = jo.getString("prizeName");
                            Log.farm("小鸡到访💞[" + prizeName + "]");
                        } else {
                            Log.runtime(jo.getString("memo"), jo.toString());
                        }
                    }
                } else {
                    Log.runtime(jo.getString("memo"), jo.toString());
                }
            } else {
                Log.runtime(jo.getString("resultDesc"), jo.toString());
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "visitAnimal err:");
            Log.printStackTrace(TAG, t);
        }
    }

    /* 雇佣好友小鸡 */
    private void hireAnimal() {
        JSONArray animals = null;
        try {
            JSONObject jsonObject = enterFarm();
            if (jsonObject == null) {
                return;
            }
            if ("SUCCESS".equals(jsonObject.getString("memo"))) {
                JSONObject farmVO = jsonObject.getJSONObject("farmVO");
                JSONObject subFarmVO = farmVO.getJSONObject("subFarmVO");
                animals = subFarmVO.getJSONArray("animals");
            } else {
                Log.record(jsonObject.getString("memo"));
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "getAnimalCount err:");
            Log.printStackTrace(TAG, t);
            return;
        }
        if (animals == null) {
            return;
        }
        try {
            for (int i = 0, len = animals.length(); i < len; i++) {
                JSONObject joo = animals.getJSONObject(i);
                if (Objects.equals(joo.getString("subAnimalType"), "WORK")) {
                    String taskId = "HIRE|" + joo.getString("animalId");
                    long beHiredEndTime = joo.getLong("beHiredEndTime");
                    if (!hasChildTask(taskId)) {
                        addChildTask(new ChildModelTask(taskId, "HIRE", () -> {
                            if (hireAnimal.getValue()) {
                                hireAnimal();
                            }
                        }, beHiredEndTime));
                        Log.record(TAG, "添加蹲点雇佣👷在[" + TimeUtil.getCommonDate(beHiredEndTime) + "]执行");
                    } else {
                        addChildTask(new ChildModelTask(taskId, "HIRE", () -> {
                            if (hireAnimal.getValue()) {
                                hireAnimal();
                            }
                        }, beHiredEndTime));
                    }
                }
            }
            int animalCount = animals.length();
            if (animalCount >= 3) {
                return;
            }
            Log.farm("雇佣小鸡👷[当前可雇佣小鸡数量:" + (3 - animalCount) + "只]");
            if (foodStock < 50) {
                Log.record(TAG, "饲料不足，暂不雇佣");
                return;
            }
            Set<String> hireAnimalSet = hireAnimalList.getValue();
            boolean hasNext;
            int pageStartSum = 0;
            String s;
            JSONObject jo;
            do {
                s = AntFarmRpcCall.rankingList(pageStartSum);
                jo = new JSONObject(s);
                String memo = jo.getString("memo");
                if (ResChecker.checkRes(TAG, jo)) {
                    hasNext = jo.getBoolean("hasNext");
                    JSONArray jaRankingList = jo.getJSONArray("rankingList");
                    pageStartSum += jaRankingList.length();
                    for (int i = 0; i < jaRankingList.length(); i++) {
                        JSONObject joo = jaRankingList.getJSONObject(i);
                        String userId = joo.getString("userId");
                        boolean isHireAnimal = hireAnimalSet.contains(userId);
                        if (hireAnimalType.getValue() == HireAnimalType.DONT_HIRE) {
                            isHireAnimal = !isHireAnimal;
                        }
                        if (!isHireAnimal || userId.equals(UserMap.getCurrentUid())) {
                            continue;
                        }
                        String actionTypeListStr = joo.getJSONArray("actionTypeList").toString();
                        if (actionTypeListStr.contains("can_hire_action")) {
                            if (hireAnimalAction(userId)) {
                                animalCount++;
                                break;
                            }
                        }
                    }
                } else {
                    Log.record(memo);
                    Log.runtime(s);
                    break;
                }
            } while (hasNext && animalCount < 3);
            if (animalCount < 3) {
                Log.farm("雇佣小鸡失败，没有足够的小鸡可以雇佣");
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "hireAnimal err:");
            Log.printStackTrace(TAG, t);
        }
    }

    private boolean hireAnimalAction(String userId) {
        try {
            String s = AntFarmRpcCall.enterFarm(userId, userId);
            JSONObject jo = new JSONObject(s);
            if (ResChecker.checkRes(TAG, jo)) {
                JSONObject farmVO = jo.getJSONObject("farmVO");
                JSONObject subFarmVO = farmVO.getJSONObject("subFarmVO");
                String farmId = subFarmVO.getString("farmId");
                JSONArray animals = subFarmVO.getJSONArray("animals");
                for (int i = 0, len = animals.length(); i < len; i++) {
                    JSONObject animal = animals.getJSONObject(i);
                    if (Objects.equals(animal.getJSONObject("masterUserInfoVO").getString("userId"), userId)) {
                        String animalId = animal.getString("animalId");
                        jo = new JSONObject(AntFarmRpcCall.hireAnimal(farmId, animalId));
                        if (ResChecker.checkRes(TAG, jo)) {
                            Log.farm("雇佣小鸡👷[" + UserMap.getMaskName(userId) + "] 成功");
                            JSONArray newAnimals = jo.getJSONArray("animals");
                            for (int ii = 0, newLen = newAnimals.length(); ii < newLen; ii++) {
                                JSONObject joo = newAnimals.getJSONObject(ii);
                                if (Objects.equals(joo.getString("animalId"), animalId)) {
                                    long beHiredEndTime = joo.getLong("beHiredEndTime");
                                    addChildTask(new ChildModelTask("HIRE|" + animalId, "HIRE", () -> {
                                        if (hireAnimal.getValue()) {
                                            hireAnimal();
                                        }
                                    }, beHiredEndTime));
                                    Log.record(TAG, "添加蹲点雇佣👷在[" + TimeUtil.getCommonDate(beHiredEndTime) + "]执行");
                                    break;
                                }
                            }
                            return true;
                        } else {
                            Log.record(jo.getString("memo"));
                            Log.runtime(s);
                        }
                        return false;
                    }
                }
            } else {
                Log.record(jo.getString("memo"));
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "hireAnimal err:");
            Log.printStackTrace(TAG, t);
        }
        return false;
    }

    private void drawGameCenterAward() {
        try {
            JSONObject jo = new JSONObject(AntFarmRpcCall.queryGameList());
            GlobalThreadPools.sleep(3000);
            if (jo.optBoolean("success")) {
                JSONObject gameDrawAwardActivity = jo.getJSONObject("gameDrawAwardActivity");
                int canUseTimes = gameDrawAwardActivity.getInt("canUseTimes");
                while (canUseTimes > 0) {
                    try {
                        jo = new JSONObject(AntFarmRpcCall.drawGameCenterAward());
                        GlobalThreadPools.sleep(3000);
                        if (jo.optBoolean("success")) {
                            canUseTimes = jo.getInt("drawRightsTimes");
                            JSONArray gameCenterDrawAwardList = jo.getJSONArray("gameCenterDrawAwardList");
                            ArrayList<String> awards = new ArrayList<>();
                            for (int i = 0; i < gameCenterDrawAwardList.length(); i++) {
                                JSONObject gameCenterDrawAward = gameCenterDrawAwardList.getJSONObject(i);
                                int awardCount = gameCenterDrawAward.getInt("awardCount");
                                String awardName = gameCenterDrawAward.getString("awardName");
                                awards.add(awardName + "*" + awardCount);
                            }
                            Log.farm("庄园小鸡🎁[开宝箱:获得" + StringUtil.collectionJoinString(",", awards) + "]");
                        } else {
                            Log.runtime(TAG, "drawGameCenterAward falsed result: " + jo);
                        }
                    } catch (Throwable t) {
                        Log.printStackTrace(TAG, t);
                    }
                }
            } else {
                Log.runtime(TAG, "queryGameList falsed result: " + jo);
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "queryChickenDiaryList err:");
            Log.printStackTrace(TAG, t);
        }
    }

    // 小鸡换装
    private void listOrnaments() {
        try {
            String s = AntFarmRpcCall.queryLoveCabin(UserMap.getCurrentUid());
            JSONObject jsonObject = new JSONObject(s);
            if ("SUCCESS".equals(jsonObject.getString("memo"))) {
                JSONObject ownAnimal = jsonObject.getJSONObject("ownAnimal");
                String animalId = ownAnimal.getString("animalId");
                String farmId = ownAnimal.getString("farmId");
                String listResult = AntFarmRpcCall.listOrnaments();
                JSONObject jolistOrnaments = new JSONObject(listResult);
                // 检查是否有 achievementOrnaments 数组
                if (!jolistOrnaments.has("achievementOrnaments")) {
                    return; // 数组为空，直接返回
                }
                JSONArray achievementOrnaments = jolistOrnaments.getJSONArray("achievementOrnaments");
                Random random = new Random();
                List<String> possibleOrnaments = new ArrayList<>(); // 收集所有可保存的套装组合
                for (int i = 0; i < achievementOrnaments.length(); i++) {
                    JSONObject ornament = achievementOrnaments.getJSONObject(i);
                    if (ornament.getBoolean("acquired")) {
                        JSONArray sets = ornament.getJSONArray("sets");
                        List<JSONObject> availableSets = new ArrayList<>();
                        // 收集所有带有 cap 和 coat 的套装组合
                        for (int j = 0; j < sets.length(); j++) {
                            JSONObject set = sets.getJSONObject(j);
                            if ("cap".equals(set.getString("subType")) || "coat".equals(set.getString("subType"))) {
                                availableSets.add(set);
                            }
                        }
                        // 如果有可用的帽子和外套套装组合
                        if (availableSets.size() >= 2) {
                            // 将所有可保存的套装组合添加到 possibleOrnaments 列表中
                            for (int j = 0; j < availableSets.size() - 1; j++) {
                                JSONObject selectedCoat = availableSets.get(j);
                                JSONObject selectedCap = availableSets.get(j + 1);
                                String id1 = selectedCoat.getString("id"); // 外套 ID
                                String id2 = selectedCap.getString("id"); // 帽子 ID
                                String ornaments = id1 + "," + id2;
                                possibleOrnaments.add(ornaments);
                            }
                        }
                    }
                }
                // 如果有可保存的套装组合，则随机选择一个进行保存
                if (!possibleOrnaments.isEmpty()) {
                    String ornamentsToSave = possibleOrnaments.get(random.nextInt(possibleOrnaments.size()));
                    String saveResult = AntFarmRpcCall.saveOrnaments(animalId, farmId, ornamentsToSave);
                    JSONObject saveResultJson = new JSONObject(saveResult);
                    // 判断保存是否成功并输出日志
                    if (saveResultJson.optBoolean("success")) {
                        // 获取保存的整套服装名称
                        String[] ornamentIds = ornamentsToSave.split(",");
                        String wholeSetName = ""; // 整套服装名称
                        // 遍历 achievementOrnaments 查找对应的套装名称
                        for (int i = 0; i < achievementOrnaments.length(); i++) {
                            JSONObject ornament = achievementOrnaments.getJSONObject(i);
                            JSONArray sets = ornament.getJSONArray("sets");
                            // 找到对应的整套服装名称
                            if (sets.length() == 2 && sets.getJSONObject(0).getString("id").equals(ornamentIds[0])
                                    && sets.getJSONObject(1).getString("id").equals(ornamentIds[1])) {
                                wholeSetName = ornament.getString("name");
                                break;
                            }
                        }
                        // 输出日志
                        Log.farm("庄园小鸡💞[换装:" + wholeSetName + "]");
                        Status.setOrnamentToday();
                    } else {
                        Log.runtime(TAG, "保存时装失败，错误码： " + saveResultJson);
                    }
                }
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "listOrnaments err: " + t.getMessage());
            Log.printStackTrace(TAG, t);
        }
    }

    // 一起拿小鸡饲料
    private void letsGetChickenFeedTogether() {
        try {
            JSONObject jo = new JSONObject(AntFarmRpcCall.letsGetChickenFeedTogether());
            if (jo.optBoolean("success")) {
                String bizTraceId = jo.getString("bizTraceId");
                JSONArray p2pCanInvitePersonDetailList = jo.getJSONArray("p2pCanInvitePersonDetailList");
                int canInviteCount = 0;
                int hasInvitedCount = 0;
                List<String> userIdList = new ArrayList<>(); // 保存 userId
                for (int i = 0; i < p2pCanInvitePersonDetailList.length(); i++) {
                    JSONObject personDetail = p2pCanInvitePersonDetailList.getJSONObject(i);
                    String inviteStatus = personDetail.getString("inviteStatus");
                    String userId = personDetail.getString("userId");
                    if (inviteStatus.equals("CAN_INVITE")) {
                        userIdList.add(userId);
                        canInviteCount++;
                    } else if (inviteStatus.equals("HAS_INVITED")) {
                        hasInvitedCount++;
                    }
                }
                int invitedToday = hasInvitedCount;
                int remainingInvites = 5 - invitedToday;
                int invitesToSend = Math.min(canInviteCount, remainingInvites);
                if (invitesToSend == 0) {
                    return;
                }
                Set<String> getFeedSet = getFeedlList.getValue();
                if (getFeedType.getValue() == GetFeedType.GIVE) {
                    for (String userId : userIdList) {
                        if (invitesToSend <= 0) {
//                            Log.record(TAG,"已达到最大邀请次数限制，停止发送邀请。");
                            break;
                        }
                        if (getFeedSet.contains(userId)) {
                            jo = new JSONObject(AntFarmRpcCall.giftOfFeed(bizTraceId, userId));
                            if (jo.optBoolean("success")) {
                                Log.farm("一起拿小鸡饲料🥡 [送饲料：" + UserMap.getMaskName(userId) + "]");
                                invitesToSend--; // 每成功发送一次邀请，减少一次邀请次数
                            } else {
                                Log.record(TAG, "邀请失败：" + jo);
                                break;
                            }
                        }
                    }
                } else {
                    Random random = new Random();
                    for (int j = 0; j < invitesToSend; j++) {
                        int randomIndex = random.nextInt(userIdList.size());
                        String userId = userIdList.get(randomIndex);
                        jo = new JSONObject(AntFarmRpcCall.giftOfFeed(bizTraceId, userId));
                        if (jo.optBoolean("success")) {
                            Log.farm("一起拿小鸡饲料🥡 [送饲料：" + UserMap.getMaskName(userId) + "]");
                        } else {
                            Log.record(TAG, "邀请失败：" + jo);
                            break;
                        }
                        userIdList.remove(randomIndex);
                    }
                }
            }
        } catch (JSONException e) {
            Log.runtime(TAG, "letsGetChickenFeedTogether err:");
            Log.printStackTrace(e);
        }
    }

    public interface DonationCount {
        int ONE = 0;
        int ALL = 1;
        String[] nickNames = {"随机一次", "随机多次"};
    }

    public interface RecallAnimalType {
        int ALWAYS = 0;
        int WHEN_THIEF = 1;
        int WHEN_HUNGRY = 2;
        int NEVER = 3;
        String[] nickNames = {"始终召回", "偷吃召回", "饥饿召回", "暂不召回"};
    }

    public interface SendBackAnimalWay {
        int HIT = 0;
        int NORMAL = 1;
        String[] nickNames = {"攻击", "常规"};
    }

    public interface SendBackAnimalType {
        int BACK = 0;
        int NOT_BACK = 1;
        String[] nickNames = {"选中遣返", "选中不遣返"};
    }

    public interface collectChickenDiaryType {
        int CLOSE = 0;
        int ONCE = 0;
        int MONTH = 1;
        int ALL = 2;
        String[] nickNames = {"不开启", "一次", "当月", "所有"};
    }

    public enum AnimalBuff {//小鸡buff
        ACCELERATING, INJURED, NONE
    }

    public enum AnimalFeedStatus {
        HUNGRY, EATING, SLEEPY
    }

    public enum AnimalInteractStatus { //小鸡关互动状态
        HOME, GOTOSTEAL, STEALING
    }

    public enum SubAnimalType {
        NORMAL, GUEST, PIRATE, WORK
    }

    public enum ToolType {
        STEALTOOL, ACCELERATETOOL, SHARETOOL, FENCETOOL, NEWEGGTOOL, DOLLTOOL, ORDINARY_ORNAMENT_TOOL, ADVANCE_ORNAMENT_TOOL;

        public static final CharSequence[] nickNames = {"蹭饭卡", "加速卡", "救济卡", "篱笆卡", "新蛋卡", "公仔补签卡", "普通装扮补签卡", "高级装扮补签卡"};

        public CharSequence nickName() {
            return nickNames[ordinal()];
        }
    }

    public enum GameType {
        starGame, jumpGame, flyGame, hitGame;
        public static final CharSequence[] gameNames = {"星星球", "登山赛", "飞行赛", "欢乐揍小鸡"};

        public CharSequence gameName() {
            return gameNames[ordinal()];
        }
    }


    @ToString
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class Animal {
        @JsonProperty("animalId")
        public String animalId;

        @JsonProperty("currentFarmId")
        public String currentFarmId;

        @JsonProperty("masterFarmId")
        public String masterFarmId;

        @JsonProperty("animalBuff")
        public String animalBuff;

        @JsonProperty("subAnimalType")
        public String subAnimalType;

        @JsonProperty("currentFarmMasterUserId")
        public String currentFarmMasterUserId;

        public String animalFeedStatus;

        public String animalInteractStatus;

        @JsonProperty("locationType")
        public String locationType;

        @JsonProperty("startEatTime")
        public Long startEatTime;

        @JsonProperty("consumeSpeed")
        public Double consumeSpeed;

        @JsonProperty("foodHaveEatten")
        public Double foodHaveEatten;

        @JsonProperty("animalStatusVO")
        private void unmarshalAnimalStatusVO(Map<String, Object> map) {
            if (map != null) {
                this.animalFeedStatus = (String) map.get("animalFeedStatus");
                this.animalInteractStatus = (String) map.get("animalInteractStatus");
            }
        }
    }

    private static class RewardFriend {
        public String consistencyKey, friendId, time;
    }

    private static class FarmTool {
        public ToolType toolType;
        public String toolId;
        public int toolCount, toolHoldLimit;
    }

    @SuppressWarnings("unused")
    public interface HireAnimalType {
        int HIRE = 0;
        int DONT_HIRE = 1;
        String[] nickNames = {"选中雇佣", "选中不雇佣"};
    }

    @SuppressWarnings("unused")
    public interface GetFeedType {
        int GIVE = 0;
        int RANDOM = 1;
        String[] nickNames = {"选中赠送", "随机赠送"};
    }

    public interface NotifyFriendType {
        int NOTIFY = 0;
        int DONT_NOTIFY = 1;
        String[] nickNames = {"选中通知", "选中不通知"};
    }

    public enum PropStatus {
        REACH_USER_HOLD_LIMIT, NO_ENOUGH_POINT, REACH_LIMIT;

        public static final CharSequence[] nickNames = {"达到用户持有上限", "乐园币不足", "兑换达到上限"};

        public CharSequence nickName() {
            return nickNames[ordinal()];
        }
    }

    public void family() {
        if (StringUtil.isEmpty(familyGroupId)) {
            return;
        }
        try {
            JSONObject jo = new JSONObject(AntFarmRpcCall.enterFamily());
            if (!ResChecker.checkRes(TAG, jo)) return;
            familyGroupId = jo.getString("groupId");
            int familyAwardNum = jo.getInt("familyAwardNum");
            boolean familySignTips = jo.getBoolean("familySignTips");
            //顶梁柱
            JSONObject assignFamilyMemberInfo = jo.getJSONObject("assignFamilyMemberInfo");
            //美食配置
            JSONObject eatTogetherConfig = jo.getJSONObject("eatTogetherConfig");
            //扭蛋
            JSONObject familyDrawInfo = jo.getJSONObject("familyDrawInfo");
            JSONArray familyInteractActions = jo.getJSONArray("familyInteractActions");
            JSONArray animals = jo.getJSONArray("animals");
            List<String> familyUserIds = new ArrayList<>();

            for (int i = 0; i < animals.length(); i++) {
                jo = animals.getJSONObject(i);
                String userId = jo.getString("userId");
                familyUserIds.add(userId);
            }
            if (familySignTips && familyOptions.getValue().contains("familySign")) {
                AntFarmFamily.INSTANCE.familySign();
            }
            if (familyAwardNum > 0 && familyOptions.getValue().contains("familyClaimReward")) {
                AntFarmFamily.INSTANCE.familyClaimRewardList();
            }

            //帮喂成员
            if (familyOptions.getValue().contains("feedFriendAnimal")) {
                familyFeedFriendAnimal(animals);
            }
            //请吃美食
            if (familyOptions.getValue().contains("eatTogetherConfig")) {
                familyEatTogether(eatTogetherConfig, familyInteractActions, familyUserIds);
            }

            //好友分享
            if (familyOptions.getValue().contains("inviteFriendVisitFamily")) {
                inviteFriendVisitFamily(familyUserIds);
            }
            boolean drawActivitySwitch = familyDrawInfo.getBoolean("drawActivitySwitch");
            //扭蛋
            if (drawActivitySwitch && familyOptions.getValue().contains("familyDrawInfo")) {
                familyDrawTask(familyUserIds, familyDrawInfo);
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "family err:");
            Log.printStackTrace(TAG, t);
        }
    }


    private void syncFamilyStatusIntimacy(String groupId) {
        try {
            String userId = UserMap.getCurrentUid();
            JSONObject jo = new JSONObject(AntFarmRpcCall.syncFamilyStatus(groupId, "INTIMACY_VALUE", userId));
            ResChecker.checkRes(TAG, jo);
        } catch (Throwable t) {
            Log.runtime(TAG, "syncFamilyStatus err:");
            Log.printStackTrace(TAG, t);
        }
    }

    private void inviteFriendVisitFamily(List<String> friendUserIds) {
        try {
            if (Status.hasFlagToday("antFarm::inviteFriendVisitFamily")) {
                return;
            }
            Set<String> familyValue = notInviteList.getValue();
            if (familyValue.isEmpty()) {
                return;
            }
            if (Objects.isNull(friendUserIds) || friendUserIds.isEmpty()) {
                return;
            }
            JSONArray userIdArray = new JSONArray();
            for (String u : familyValue) {
                if (!friendUserIds.contains(u) && userIdArray.length() < 6) {
                    userIdArray.put(u);
                }
                if (userIdArray.length() >= 6) {
                    break;
                }
            }
            JSONObject jo = new JSONObject(AntFarmRpcCall.inviteFriendVisitFamily(userIdArray));
            if (Objects.equals("SUCCESS", jo.getString("memo"))) {
                Log.farm("亲密家庭🏠提交任务[分享好友]");
                Status.setFlagToday("antFarm::inviteFriendVisitFamily");
                GlobalThreadPools.sleep(500);
                syncFamilyStatusIntimacy(familyGroupId);
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "inviteFriendVisitFamily err:");
            Log.printStackTrace(TAG, t);
        }
    }

    private void familyBatchInviteP2PTask(List<String> friendUserIds, JSONObject familyDrawInfo) {
        try {
            if (Status.hasFlagToday("antFarm::familyBatchInviteP2P")) {
                return;
            }
            if (Objects.isNull(friendUserIds) || friendUserIds.isEmpty()) {
                return;
            }
            String activityId = familyDrawInfo.optString("activityId");
            String sceneCode = "ANTFARM_FD_VISIT_" + activityId;
            JSONObject jo = new JSONObject(AntFarmRpcCall.familyShareP2PPanelInfo(sceneCode));
            if (ResChecker.checkRes(TAG, jo)) {
                JSONArray p2PFriendVOList = jo.getJSONArray("p2PFriendVOList");
                if (Objects.isNull(p2PFriendVOList) || p2PFriendVOList.length() <= 0) {
                    return;
                }
                JSONArray inviteP2PVOList = new JSONArray();
                for (int i = 0; i < p2PFriendVOList.length(); i++) {
                    if (inviteP2PVOList.length() < 6) {
                        JSONObject object = new JSONObject();
                        object.put("beInvitedUserId", p2PFriendVOList.getJSONObject(i).getString("userId"));
                        object.put("bizTraceId", "");
                        inviteP2PVOList.put(object);
                    }
                    if (inviteP2PVOList.length() >= 6) {
                        break;
                    }
                }
                jo = new JSONObject(AntFarmRpcCall.familyBatchInviteP2P(inviteP2PVOList, sceneCode));
                if (ResChecker.checkRes(TAG, jo)) {
                    Log.farm("亲密家庭🏠提交任务[好友串门送扭蛋]");
                    Status.setFlagToday("antFarm::familyBatchInviteP2P");
                    GlobalThreadPools.sleep(500);
                }
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "familyBatchInviteP2PTask err:");
            Log.printStackTrace(TAG, t);
        }
    }

    private void familyDrawTask(List<String> friendUserIds, JSONObject familyDrawInfo) {
        try {
            JSONArray listFarmTask = familyDrawListFarmTask();
            if (listFarmTask == null) {
                return;
            }
            for (int i = 0; i < listFarmTask.length(); i++) {
                JSONObject jo = listFarmTask.getJSONObject(i);
                TaskStatus taskStatus = TaskStatus.valueOf(jo.getString("taskStatus"));
                String taskId = jo.optString("taskId");
                String title = jo.optString("title");
                if (taskStatus == TaskStatus.RECEIVED) {
                    continue;
                }
                if (taskStatus == TaskStatus.TODO && Objects.equals(taskId, "FAMILY_DRAW_VISIT_TASK") && familyOptions.getValue().contains("batchInviteP2P")) {
                    //分享
                    familyBatchInviteP2PTask(friendUserIds, familyDrawInfo);
                    continue;
                }
                if (taskStatus == TaskStatus.FINISHED && Objects.equals(taskId, "FAMILY_DRAW_FREE_TASK")) {
                    //签到
                    familyDrawSignReceiveFarmTaskAward(taskId, title);
                    continue;
                }
                GlobalThreadPools.sleep(1000);
            }
            JSONObject jo = new JSONObject(AntFarmRpcCall.queryFamilyDrawActivity());
            if (ResChecker.checkRes(TAG, jo)) {
                GlobalThreadPools.sleep(1000);
                int drawTimes = jo.optInt("familyDrawTimes");
                //碎片个数
                int giftNum = jo.optInt("mengliFragmentCount");
                if (giftNum >= 20 && !Objects.isNull(giftFamilyDrawFragment.getValue())) {
                    giftFamilyDrawFragment(giftFamilyDrawFragment.getValue(), giftNum);
                }
                for (int i = 0; i < drawTimes; i++) {
                    if (!familyDraw()) {
                        return;
                    }
                    GlobalThreadPools.sleep(1500);
                }
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "familyDrawTask err:");
            Log.printStackTrace(TAG, t);
        }
    }

    private void giftFamilyDrawFragment(String giftUserId, int giftNum) {
        try {
            JSONObject jo = new JSONObject(AntFarmRpcCall.giftFamilyDrawFragment(giftUserId, giftNum));
            if (ResChecker.checkRes(TAG, jo)) {
                Log.farm("亲密家庭🏠赠送扭蛋碎片#" + giftNum + "个#" + giftUserId);
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "giftFamilyDrawFragment err:");
            Log.printStackTrace(TAG, t);
        }
    }

    private JSONArray familyDrawListFarmTask() {
        try {
            JSONObject jo = new JSONObject(AntFarmRpcCall.familyDrawListFarmTask());
            if (ResChecker.checkRes(TAG, jo)) {
                return jo.getJSONArray("farmTaskList");
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "familyDrawListFarmTask err:");
            Log.printStackTrace(TAG, t);
        }
        return null;
    }

    private Boolean familyDraw() {
        try {
            JSONObject jo = new JSONObject(AntFarmRpcCall.familyDraw());
            if (ResChecker.checkRes(TAG, jo)) {
                JSONObject familyDrawPrize = jo.getJSONObject("familyDrawPrize");
                String title = familyDrawPrize.optString("title");
                String awardCount = familyDrawPrize.getString("awardCount");
                int familyDrawTimes = jo.optInt("familyDrawTimes");
                Log.farm("开扭蛋🎟️抽中[" + title + "]#[" + awardCount + "]");
                return familyDrawTimes != 0;
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "familyDraw err:");
            Log.printStackTrace(TAG, t);
        }
        return false;
    }

    private void familyEatTogether(JSONObject eatTogetherConfig, JSONArray familyInteractActions, List<String> friendUserIds) {
        try {
            boolean isEat = false;
            JSONArray periodItemList = eatTogetherConfig.getJSONArray("periodItemList");
            if (Objects.isNull(periodItemList) || periodItemList.length() <= 0) {
                return;
            }
            if (!Objects.isNull(familyInteractActions) && familyInteractActions.length() > 0) {
                for (int i = 0; i < familyInteractActions.length(); i++) {
                    JSONObject familyInteractAction = familyInteractActions.getJSONObject(i);
                    if ("EatTogether".equals(familyInteractAction.optString("familyInteractType"))) {
                        return;
                    }
                }
            }
            String periodName = "";
            Calendar currentTime = Calendar.getInstance();
            for (int i = 0; i < periodItemList.length(); i++) {
                JSONObject periodItem = periodItemList.getJSONObject(i);
                int startHour = periodItem.optInt("startHour");
                int startMinute = periodItem.optInt("startMinute");
                int endHour = periodItem.optInt("endHour");
                int endMinute = periodItem.optInt("endMinute");
                Calendar startTime = Calendar.getInstance();
                startTime.set(Calendar.HOUR_OF_DAY, startHour);
                startTime.set(Calendar.MINUTE, startMinute);
                Calendar endTime = Calendar.getInstance();
                endTime.set(Calendar.HOUR_OF_DAY, endHour);
                endTime.set(Calendar.MINUTE, endMinute);
                if (currentTime.after(startTime) && currentTime.before(endTime)) {
                    periodName = periodItem.optString("periodName");
                    isEat = true;
                    break;
                }
            }
            if (!isEat) {
                return;
            }
            if (Objects.isNull(friendUserIds) || friendUserIds.isEmpty()) {
                return;
            }
            JSONArray array = queryRecentFarmFood(friendUserIds.size());
            if (array == null) {
                return;
            }
            JSONArray friendUserIdList = new JSONArray();
            for (String userId : friendUserIds) {
                friendUserIdList.put(userId);
            }
            JSONObject jo = new JSONObject(AntFarmRpcCall.familyEatTogether(familyGroupId, friendUserIdList, array));
            if (ResChecker.checkRes(TAG, jo)) {
                Log.farm("庄园家庭🏠" + periodName + "请客#消耗美食" + friendUserIdList.length() + "份");
                GlobalThreadPools.sleep(500);
                syncFamilyStatusIntimacy(familyGroupId);
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "familyEatTogether err:");
            Log.printStackTrace(TAG, t);
        }
    }

    private void familyDrawSignReceiveFarmTaskAward(String taskId, String title) {
        try {
            JSONObject jo = new JSONObject(AntFarmRpcCall.familyDrawSignReceiveFarmTaskAward(taskId));
            if (ResChecker.checkRes(TAG, jo)) {
                Log.farm("亲密家庭🏠扭蛋任务#" + title + "#奖励领取成功");
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "familyDrawSignReceiveFarmTaskAward err:");
            Log.printStackTrace(TAG, t);
        }
    }

    private JSONArray queryRecentFarmFood(int queryNum) {
        try {
            JSONObject jo = new JSONObject(AntFarmRpcCall.queryRecentFarmFood(queryNum));
            if (!ResChecker.checkRes(TAG, jo)) {
                return null;
            }
            JSONArray cuisines = jo.getJSONArray("cuisines");
            if (Objects.isNull(cuisines) || cuisines.length() == 0) {
                return null;
            }
            int count = 0;
            for (int i = 0; i < cuisines.length(); i++) {
                count += cuisines.getJSONObject(i).optInt("count");
            }
            if (count >= queryNum) {
                return cuisines;
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "queryRecentFarmFood err:");
            Log.printStackTrace(TAG, t);
        }
        return null;
    }

    private void familyFeedFriendAnimal(JSONArray animals) {
        try {
            for (int i = 0; i < animals.length(); i++) {
                JSONObject animal = animals.getJSONObject(i);
                JSONObject animalStatusVo = animal.getJSONObject("animalStatusVO");
                if (AnimalInteractStatus.HOME.name().equals(animalStatusVo.getString("animalInteractStatus")) && AnimalFeedStatus.HUNGRY.name().equals(animalStatusVo.getString("animalFeedStatus"))) {
                    String groupId = animal.getString("groupId");
                    String farmId = animal.getString("farmId");
                    String userId = animal.getString("userId");
                    if (!UserMap.getUserIdSet().contains(userId)) {
                        //非好友
                        continue;
                    }
                    if (Status.hasFlagToday("farm::feedFriendLimit")) {
                        Log.runtime("今日喂鸡次数已达上限🥣");
                        return;
                    }
                    JSONObject jo = new JSONObject(AntFarmRpcCall.feedFriendAnimal(farmId, groupId));
                    if (ResChecker.checkRes(TAG, jo)) {
                        int feedFood = foodStock - jo.getInt("foodStock");
                        if (feedFood > 0) {
                            add2FoodStock(-feedFood);
                        }
                        Log.farm("庄园家庭🏠帮喂好友🥣[" + UserMap.getMaskName(userId) + "]的小鸡[" + feedFood + "g]#剩余" + foodStock + "g");
                    }
                }
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "familyFeedFriendAnimal err:");
            Log.printStackTrace(TAG, t);
        }
    }

    /**
     * 点击领取活动食物
     *
     * @param gift
     */
    private void clickForGiftV2(JSONObject gift) {
        if (gift == null) return;
        try {
            JSONObject resultJson = new JSONObject(AntFarmRpcCall.clickForGiftV2(gift.getString("foodType"), gift.getInt("giftIndex")));
            if (ResChecker.checkRes(TAG, resultJson)) {
                Log.farm("领取活动食物成功," + "已领取" + resultJson.optInt("foodCount"));
            }
        } catch (Exception e) {
            Log.runtime(TAG, "clickForGiftV2 err:");
            Log.printStackTrace(TAG, e);
        }
    }

    static class AntFarmFamilyOption extends MapperEntity {
        public AntFarmFamilyOption(String i, String n) {
            id = i;
            name = n;
        }

        public static List<AntFarmFamilyOption> getAntFarmFamilyOptions() {
            List<AntFarmFamilyOption> list = new ArrayList<>();
            list.add(new AntFarmFamilyOption("familySign", "每日签到"));
            list.add(new AntFarmFamilyOption("eatTogetherConfig", "请吃美食"));
            list.add(new AntFarmFamilyOption("feedFamilyAnimal", "帮喂小鸡"));
//            list.add(new AntFarmFamilyOption("deliverMsgSend", "道早安"));
            list.add(new AntFarmFamilyOption("familyClaimReward", "领取奖励"));
            list.add(new AntFarmFamilyOption("inviteFriendVisitFamily", "好友分享"));
            list.add(new AntFarmFamilyOption("assignRights", "使用顶梁柱特权"));
            list.add(new AntFarmFamilyOption("familyDrawInfo", "开扭蛋"));
            list.add(new AntFarmFamilyOption("batchInviteP2P", "串门送扭蛋"));
            return list;
        }
    }
}