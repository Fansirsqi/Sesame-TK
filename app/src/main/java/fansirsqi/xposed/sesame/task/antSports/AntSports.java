package fansirsqi.xposed.sesame.task.antSports;

import android.annotation.SuppressLint;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashSet;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import fansirsqi.xposed.sesame.entity.AlipayUser;
import fansirsqi.xposed.sesame.hook.ApplicationHook;
import fansirsqi.xposed.sesame.model.BaseModel;
import fansirsqi.xposed.sesame.model.ModelFields;
import fansirsqi.xposed.sesame.model.ModelGroup;
import fansirsqi.xposed.sesame.model.modelFieldExt.BooleanModelField;
import fansirsqi.xposed.sesame.model.modelFieldExt.ChoiceModelField;
import fansirsqi.xposed.sesame.model.modelFieldExt.IntegerModelField;
import fansirsqi.xposed.sesame.model.modelFieldExt.SelectModelField;
import fansirsqi.xposed.sesame.model.modelFieldExt.StringModelField;
import fansirsqi.xposed.sesame.task.ModelTask;
import fansirsqi.xposed.sesame.task.TaskCommon;
import fansirsqi.xposed.sesame.util.GlobalThreadPools;
import fansirsqi.xposed.sesame.util.Log;
import fansirsqi.xposed.sesame.util.maps.UserMap;
import fansirsqi.xposed.sesame.util.RandomUtil;
import fansirsqi.xposed.sesame.util.ResChecker;
import fansirsqi.xposed.sesame.data.Status;
import fansirsqi.xposed.sesame.util.TimeUtil;

public class AntSports extends ModelTask {
    private static final String TAG = AntSports.class.getSimpleName();
    private int tmpStepCount = -1;
    private BooleanModelField walk;
    private ChoiceModelField walkPathTheme;
    private String walkPathThemeId;
    private BooleanModelField walkCustomPath;
    private StringModelField walkCustomPathId;
    private BooleanModelField openTreasureBox;
    private BooleanModelField receiveCoinAsset;
    private BooleanModelField donateCharityCoin;
    private ChoiceModelField donateCharityCoinType;
    private IntegerModelField donateCharityCoinAmount;
    private IntegerModelField minExchangeCount;
    private IntegerModelField latestExchangeTime;
    private IntegerModelField syncStepCount;
    private BooleanModelField tiyubiz;
    private BooleanModelField battleForFriends;
    private ChoiceModelField battleForFriendType;
    private SelectModelField originBossIdList;
    private BooleanModelField sportsTasks;
    private BooleanModelField coinExchangeDoubleCard;

    /**
     * 获取任务名称
     *
     * @return 运动任务名称
     */
    @Override
    public String getName() {
        return "运动";
    }

    /**
     * 获取任务分组
     *
     * @return 运动分组
     */
    @Override
    public ModelGroup getGroup() {
        return ModelGroup.SPORTS;
    }

    /**
     * 获取任务图标
     *
     * @return 运动任务图标文件名
     */
    @Override
    public String getIcon() {
        return "AntSports.png";
    }

    @Override
    public ModelFields getFields() {
        ModelFields modelFields = new ModelFields();
        modelFields.addField(walk = new BooleanModelField("walk", "行走路线 | 开启", false));
        modelFields.addField(walkPathTheme = new ChoiceModelField("walkPathTheme", "行走路线 | 主题", WalkPathTheme.DA_MEI_ZHONG_GUO, WalkPathTheme.nickNames));
        modelFields.addField(walkCustomPath = new BooleanModelField("walkCustomPath", "行走路线 | 开启自定义路线", false));
        modelFields.addField(walkCustomPathId = new StringModelField("walkCustomPathId", "行走路线 | 自定义路线代码(debug)", "p0002023122214520001"));
        modelFields.addField(openTreasureBox = new BooleanModelField("openTreasureBox", "开启宝箱", false));
        modelFields.addField(sportsTasks = new BooleanModelField("sportsTasks", "开启运动任务", false));
        modelFields.addField(receiveCoinAsset = new BooleanModelField("receiveCoinAsset", "收运动币", false));
        modelFields.addField(donateCharityCoin = new BooleanModelField("donateCharityCoin", "捐运动币 | 开启", false));
        modelFields.addField(donateCharityCoinType = new ChoiceModelField("donateCharityCoinType", "捐运动币 | 方式", DonateCharityCoinType.ONE, DonateCharityCoinType.nickNames));
        modelFields.addField(donateCharityCoinAmount = new IntegerModelField("donateCharityCoinAmount", "捐运动币 | 数量(每次)", 100));
        modelFields.addField(battleForFriends = new BooleanModelField("battleForFriends", "抢好友 | 开启", false));
        modelFields.addField(battleForFriendType = new ChoiceModelField("battleForFriendType", "抢好友 | 动作", BattleForFriendType.ROB, BattleForFriendType.nickNames));
        modelFields.addField(originBossIdList = new SelectModelField("originBossIdList", "抢好友 | 好友列表", new LinkedHashSet<>(), AlipayUser::getList));
        modelFields.addField(tiyubiz = new BooleanModelField("tiyubiz", "文体中心", false));
        modelFields.addField(minExchangeCount = new IntegerModelField("minExchangeCount", "最小捐步步数", 0));
        modelFields.addField(latestExchangeTime = new IntegerModelField("latestExchangeTime", "最晚捐步时间(24小时制)", 22));
        modelFields.addField(syncStepCount = new IntegerModelField("syncStepCount", "自定义同步步数", 22000));
        modelFields.addField(coinExchangeDoubleCard = new BooleanModelField("coinExchangeDoubleCard", "运动币兑换限时能量双击卡", false));
        return modelFields;
    }

    /**
     * 启动任务，进行必要的初始化操作
     *
     * @param classLoader 类加载器
     */
    @Override
    public void boot(ClassLoader classLoader) {
        try {
            XposedHelpers.findAndHookMethod("com.alibaba.health.pedometer.core.datasource.PedometerAgent", classLoader,
                    "readDailyStep", new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            int originStep = (Integer) param.getResult();
                            int step = tmpStepCount();
                            if (TaskCommon.IS_AFTER_8AM && originStep < step) {//早于8点或步数小于自定义步数hook
                                param.setResult(step);
                            }
                        }
                    });
            Log.runtime(TAG, "hook readDailyStep successfully");
        } catch (Throwable t) {
            Log.runtime(TAG, "hook readDailyStep err:");
            Log.printStackTrace(TAG, t);
        }
    }

    /**
     * 检查任务是否可以执行
     *
     * @return 是否可以执行运动任务
     */
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

    /**
     * 执行运动任务的主要逻辑
     */
    @Override
    public void run() {
        Log.record(TAG, "执行开始-" + getName());
        try {

            if (!Status.hasFlagToday("sport::syncStep") && TimeUtil.isNowAfterOrCompareTimeStr("0600")) {
                addChildTask(new ChildModelTask("syncStep", () -> {
                    int step = tmpStepCount();
                    try {
                        ClassLoader classLoader = ApplicationHook.getClassLoader();
                        if ((Boolean) XposedHelpers.callMethod(XposedHelpers.callStaticMethod(classLoader.loadClass("com.alibaba.health.pedometer.intergation.rpc.RpcManager"), "a"), "a", new Object[]{step, Boolean.FALSE, "system"})) {
                            Log.other(TAG, "同步步数🏃🏻‍♂️[" + step + "步]");
                        } else {
                            Log.error(TAG, "同步运动步数失败:" + step);
                        }
                        Status.setFlagToday("sport::syncStep");
                    } catch (Throwable t) {
                        Log.printStackTrace(TAG, t);
                    }
                }));
            }
            if (sportsTasks.getValue())
                sportsTasks();
            ClassLoader loader = ApplicationHook.getClassLoader();
            if (walk.getValue()) {
                getWalkPathThemeIdOnConfig();
                walk();
            }
            if (openTreasureBox.getValue() && !walk.getValue())
                queryMyHomePage(loader);
            if (donateCharityCoin.getValue() && Status.canDonateCharityCoin())
                queryProjectList(loader);
            if (minExchangeCount.getValue() > 0 && Status.canExchangeToday(UserMap.getCurrentUid()))
                queryWalkStep(loader);
            if (tiyubiz.getValue()) {
                userTaskGroupQuery("SPORTS_DAILY_SIGN_GROUP");
                userTaskGroupQuery("SPORTS_DAILY_GROUP");
                userTaskRightsReceive();
                pathFeatureQuery();
                participate();
            }
            if (battleForFriends.getValue()) {
                queryClubHome();
                queryTrainItem();
                buyMember();
            }
            if (receiveCoinAsset.getValue())
                receiveCoinAsset();
        } catch (Throwable t) {
            Log.runtime(TAG, "start.run err:");
            Log.printStackTrace(TAG, t);
        } finally {
            Log.record(TAG, "执行结束-" + getName());
        }
    }

    private void coinExchangeItem(String itemId) {
        try {
            JSONObject jo = new JSONObject(AntSportsRpcCall.queryItemDetail(itemId));
            if (!ResChecker.checkRes(TAG + "查询商品详情失败:", jo)) {
                return;
            }
            jo = jo.getJSONObject("data");
            if (!"OK".equals(jo.optString("exchangeBtnStatus"))) {
                return;
            }
            jo = jo.getJSONObject("itemBaseInfo");
            String itemTitle = jo.getString("itemTitle");
            int valueCoinCount = jo.getInt("valueCoinCount");
            jo = new JSONObject(AntSportsRpcCall.exchangeItem(itemId, valueCoinCount));
            if (!ResChecker.checkRes(TAG + "兑换商品失败:", jo)) {
                return;
            }
            jo = jo.getJSONObject("data");
            if (jo.optBoolean("exgSuccess")) {
                Log.other(TAG, "运动好礼🎐兑换[" + itemTitle + "]花费" + valueCoinCount + "运动币");
            }
        } catch (Throwable t) {
            Log.error(TAG, "trainMember err:");
            Log.printStackTrace(TAG, t);
        }
    }

    /**
     * 获取临时步数
     *
     * @return 步数值
     */
    public int tmpStepCount() {
        if (tmpStepCount >= 0) {
            return tmpStepCount;
        }
        tmpStepCount = syncStepCount.getValue();
        if (tmpStepCount > 0) {
            tmpStepCount = RandomUtil.nextInt(tmpStepCount, tmpStepCount + 2000);
            if (tmpStepCount > 100000) {
                tmpStepCount = 100000;
            }
        }
        return tmpStepCount;
    }

    // 运动
    private void sportsTasks() {
        try {
            sportsCheck_in();
            String taskPanelResponse = AntSportsRpcCall.queryCoinTaskPanel();
            if (taskPanelResponse == null || taskPanelResponse.trim().isEmpty()) {
                Log.record(TAG, "查询任务面板失败：返回空响应");
                return;
            }
            
            JSONObject jo = new JSONObject(taskPanelResponse);
            if (jo.optBoolean("success")) {
                JSONObject data = jo.getJSONObject("data");
                JSONArray taskList = data.getJSONArray("taskList");
                for (int i = 0; i < taskList.length(); i++) {
                    JSONObject taskDetail = taskList.getJSONObject(i);
                    String taskId = taskDetail.getString("taskId");
                    String taskName = taskDetail.getString("taskName");
                    String prizeAmount = taskDetail.getString("prizeAmount");
                    String taskStatus = taskDetail.getString("taskStatus");
                    int currentNum = taskDetail.getInt("currentNum");
                    // 要完成的次数
                    int limitConfigNum = taskDetail.getInt("limitConfigNum") - currentNum;
                    if (taskStatus.equals("HAS_RECEIVED"))
                        return;
                    for (int i1 = 0; i1 < limitConfigNum; i1++) {
                        jo = new JSONObject(AntSportsRpcCall.completeExerciseTasks(taskId));
                        if (jo.optBoolean("success")) {
                            Log.record(TAG, "做任务得运动币👯[完成任务：" + taskName + "，得" + prizeAmount + "💰]");
                            receiveCoinAsset();
                        }
                        if (limitConfigNum > 1)
                            GlobalThreadPools.sleep(10000);
                        else
                            GlobalThreadPools.sleep(1000);
                    }
                }
            }
        } catch (Exception e) {
            Log.printStackTrace(e);
        }
    }

    private void sportsCheck_in() {
        try {
            String checkInResponse = AntSportsRpcCall.sportsCheck_in();
            if (checkInResponse == null || checkInResponse.trim().isEmpty()) {
                Log.record(TAG, "运动签到失败：返回空响应");
                return;
            }
            
            JSONObject jo = new JSONObject(checkInResponse);
            if (jo.optBoolean("success")) {
                JSONObject data = jo.getJSONObject("data");
                if (!data.getBoolean("signed")) {
                    JSONObject subscribeConfig;
                    if (data.has("subscribeConfig")) {
                        subscribeConfig = data.getJSONObject("subscribeConfig");
                        Log.record(TAG, "做任务得运动币👯[完成任务：签到" + subscribeConfig.getString("subscribeExpireDays") + "天，" + data.getString("toast") + "💰]");
                    }
                } else {
                    Log.record(TAG, "运动签到今日已签到");
                }
            } else {
                Log.record(jo.toString());
            }
        } catch (Exception e) {
            Log.record(TAG, "sportsCheck_in err");
            Log.printStackTrace(e);
        }
    }

    private void receiveCoinAsset() {
        try {
            String s = AntSportsRpcCall.queryCoinBubbleModule();
            if (s == null || s.trim().isEmpty()) {
                Log.record(TAG, "查询金币气泡模块失败：返回空响应");
                return;
            }
            
            JSONObject jo = new JSONObject(s);
            if (jo.optBoolean("success")) {
                JSONObject data = jo.getJSONObject("data");
                if (!data.has("receiveCoinBubbleList"))
                    return;
                JSONArray ja = data.getJSONArray("receiveCoinBubbleList");
                for (int i = 0; i < ja.length(); i++) {
                    jo = ja.getJSONObject(i);
                    String assetId = jo.getString("assetId");
                    int coinAmount = jo.getInt("coinAmount");
                    jo = new JSONObject(AntSportsRpcCall.receiveCoinAsset(assetId, coinAmount));
                    if (jo.optBoolean("success")) {
                        Log.other(TAG, "收集金币💰[" + coinAmount + "个]");
                    } else {
                        Log.record(TAG, "首页收集金币" + " " + jo);
                    }
                }
            } else {
                Log.runtime(TAG, s);
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "receiveCoinAsset err:");
            Log.printStackTrace(TAG, t);
        }
    }

    /*
     * 新版行走路线 -- begin
     */
    private void walk() {
        try {
            String userResponse = AntSportsRpcCall.queryUser();
            if (userResponse == null || userResponse.trim().isEmpty()) {
                Log.record(TAG, "查询用户信息失败：返回空响应");
                return;
            }
            
            JSONObject user = new JSONObject(userResponse);
            if (!user.optBoolean("success")) {
                return;
            }
            String joinedPathId = user.getJSONObject("data").getString("joinedPathId");
            if (joinedPathId == null) {
                String pathId = queryJoinPath(walkPathThemeId);
                joinPath(pathId);
                return;
            }
            JSONObject path = queryPath(joinedPathId);
            JSONObject userPathStep = path.getJSONObject("userPathStep");
            if ("COMPLETED".equals(userPathStep.getString("pathCompleteStatus"))) {
                Log.record(TAG, "行走路线🚶🏻‍♂️路线[" + userPathStep.getString("pathName") + "]已完成");
                String pathId = queryJoinPath(walkPathThemeId);
                joinPath(pathId);
                return;
            }
            int minGoStepCount = path.getJSONObject("path").getInt("minGoStepCount");
            int pathStepCount = path.getJSONObject("path").getInt("pathStepCount");
            int forwardStepCount = userPathStep.getInt("forwardStepCount");
            int remainStepCount = userPathStep.getInt("remainStepCount");
            int needStepCount = pathStepCount - forwardStepCount;
            if (remainStepCount >= minGoStepCount) {
                int useStepCount = Math.min(remainStepCount, needStepCount);
                walkGo(userPathStep.getString("pathId"), useStepCount, userPathStep.getString("pathName"));
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "walk err:");
            Log.printStackTrace(TAG, t);
        }
    }

    private void walkGo(String pathId, int useStepCount, String pathName) {
        try {
            Date date = new Date();
            @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            String walkGoResponse = AntSportsRpcCall.walkGo("202312191135", sdf.format(date), pathId, useStepCount);
            if (walkGoResponse == null || walkGoResponse.trim().isEmpty()) {
                Log.record(TAG, "行走前进失败：返回空响应");
                return;
            }
            
            JSONObject jo = new JSONObject(walkGoResponse);
            if (jo.optBoolean("success")) {
                Log.record(TAG, "行走路线🚶🏻‍♂️路线[" + pathName + "]#前进了" + useStepCount + "步");
                queryPath(pathId);
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "walkGo err:");
            Log.printStackTrace(TAG, t);
        }
    }

    private JSONObject queryWorldMap(String themeId) {
        JSONObject theme = null;
        try {
            String worldMapResponse = AntSportsRpcCall.queryWorldMap(themeId);
            if (worldMapResponse == null || worldMapResponse.trim().isEmpty()) {
                Log.record(TAG, "查询世界地图失败：返回空响应");
                return null;
            }
            
            JSONObject jo = new JSONObject(worldMapResponse);
            if (jo.optBoolean("success")) {
                theme = jo.getJSONObject("data");
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "queryWorldMap err:");
            Log.printStackTrace(TAG, t);
        }
        return theme;
    }

    private JSONObject queryCityPath(String cityId) {
        JSONObject city = null;
        try {
            String cityPathResponse = AntSportsRpcCall.queryCityPath(cityId);
            if (cityPathResponse == null || cityPathResponse.trim().isEmpty()) {
                Log.record(TAG, "查询城市路径失败：返回空响应");
                return null;
            }
            
            JSONObject jo = new JSONObject(cityPathResponse);
            if (jo.optBoolean("success")) {
                city = jo.getJSONObject("data");
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "queryCityPath err:");
            Log.printStackTrace(TAG, t);
        }
        return city;
    }

    private JSONObject queryPath(String pathId) {
        JSONObject path = null;
        try {
            Date date = new Date();
            @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            String pathResponse = AntSportsRpcCall.queryPath("202312191135", sdf.format(date), pathId);
            if (pathResponse == null || pathResponse.trim().isEmpty()) {
                Log.record(TAG, "查询路径失败：返回空响应");
                return null;
            }
            
            JSONObject jo = new JSONObject(pathResponse);
            if (jo.optBoolean("success")) {
                path = jo.getJSONObject("data");
                JSONArray ja = jo.getJSONObject("data").getJSONArray("treasureBoxList");
                for (int i = 0; i < ja.length(); i++) {
                    JSONObject treasureBox = ja.getJSONObject(i);
                    receiveEvent(treasureBox.getString("boxNo"));
                }
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "queryPath err:");
            Log.printStackTrace(TAG, t);
        }
        return path;
    }

    private void receiveEvent(String eventBillNo) {
        try {
            String eventResponse = AntSportsRpcCall.receiveEvent(eventBillNo);
            if (eventResponse == null || eventResponse.trim().isEmpty()) {
                Log.record(TAG, "接收事件失败：返回空响应");
                return;
            }
            
            JSONObject jo = new JSONObject(eventResponse);
            if (!jo.optBoolean("success")) {
                return;
            }
            JSONArray ja = jo.getJSONObject("data").getJSONArray("rewards");
            for (int i = 0; i < ja.length(); i++) {
                jo = ja.getJSONObject(i);
                Log.record(TAG, "行走路线🎁开启宝箱[" + jo.getString("rewardName") + "]*" + jo.getInt("count"));
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "receiveEvent err:");
            Log.printStackTrace(TAG, t);
        }
    }

    private String queryJoinPath(String themeId) {
        if (walkCustomPath.getValue()) {
            return walkCustomPathId.getValue();
        }
        String pathId = null;
        try {
            JSONObject theme = queryWorldMap(walkPathThemeId);
            if (theme == null) {
                return pathId;
            }
            JSONArray cityList = theme.getJSONArray("cityList");
            for (int i = 0; i < cityList.length(); i++) {
                String cityId = cityList.getJSONObject(i).getString("cityId");
                JSONObject city = queryCityPath(cityId);
                if (city == null) {
                    continue;
                }
                JSONArray cityPathList = city.getJSONArray("cityPathList");
                for (int j = 0; j < cityPathList.length(); j++) {
                    JSONObject cityPath = cityPathList.getJSONObject(j);
                    pathId = cityPath.getString("pathId");
                    if (!"COMPLETED".equals(cityPath.getString("pathCompleteStatus"))) {
                        return pathId;
                    }
                }
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "queryJoinPath err:");
            Log.printStackTrace(TAG, t);
        }
        return pathId;
    }

    private void joinPath(String pathId) {
        if (pathId == null) {
            // 龙年祈福线
            pathId = "p0002023122214520001";
        }
        try {
            String joinPathResponse = AntSportsRpcCall.joinPath(pathId);
            if (joinPathResponse == null || joinPathResponse.trim().isEmpty()) {
                Log.record(TAG, "加入路径失败：返回空响应");
                return;
            }
            
            JSONObject jo = new JSONObject(joinPathResponse);
            if (jo.optBoolean("success")) {
                JSONObject path = queryPath(pathId);
                Log.record(TAG, "行走路线🚶🏻‍♂️路线[" + path.getJSONObject("path").getString("name") + "]已加入");
            } else {
                Log.record(TAG, "行走路线🚶🏻‍♂️路线[" + pathId + "]有误，无法加入！");
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "joinPath err:");
            Log.printStackTrace(TAG, t);
        }
    }

    private void getWalkPathThemeIdOnConfig() {
        if (walkPathTheme.getValue() == WalkPathTheme.DA_MEI_ZHONG_GUO) {
            walkPathThemeId = "M202308082226";
        }
        if (walkPathTheme.getValue() == WalkPathTheme.GONG_YI_YI_XIAO_BU) {
            walkPathThemeId = "M202401042147";
        }
        if (walkPathTheme.getValue() == WalkPathTheme.DENG_DING_ZHI_MA_SHAN) {
            walkPathThemeId = "V202405271625";
        }
        if (walkPathTheme.getValue() == WalkPathTheme.WEI_C_DA_TIAO_ZHAN) {
            walkPathThemeId = "202404221422";
        }
        if (walkPathTheme.getValue() == WalkPathTheme.LONG_NIAN_QI_FU) {
            walkPathThemeId = "WF202312050200";
        }
    }

    /*
     * 新版行走路线 -- end
     */
    private void queryMyHomePage(ClassLoader loader) {
        try {
            String s = AntSportsRpcCall.queryMyHomePage();
            if (s == null || s.trim().isEmpty()) {
                Log.record(TAG, "查询我的主页失败：返回空响应");
                return;
            }
            
            JSONObject jo = new JSONObject(s);
            if (ResChecker.checkRes(TAG + "查询运动主页失败:", jo)) {
                s = jo.getString("pathJoinStatus");
                if ("GOING".equals(s)) {
                    if (jo.has("pathCompleteStatus")) {
                        if ("COMPLETED".equals(jo.getString("pathCompleteStatus"))) {
                            jo = new JSONObject(AntSportsRpcCall.queryBaseList());
                            if (ResChecker.checkRes(TAG + "查询运动基地列表失败:", jo)) {
                                JSONArray allPathBaseInfoList = jo.getJSONArray("allPathBaseInfoList");
                                JSONArray otherAllPathBaseInfoList = jo.getJSONArray("otherAllPathBaseInfoList")
                                        .getJSONObject(0)
                                        .getJSONArray("allPathBaseInfoList");
                                join(loader, allPathBaseInfoList, otherAllPathBaseInfoList, "");
                            } else {
                                Log.runtime(TAG, jo.getString("resultDesc"));
                            }
                        }
                    } else {
                        String rankCacheKey = jo.getString("rankCacheKey");
                        JSONArray ja = jo.getJSONArray("treasureBoxModelList");
                        for (int i = 0; i < ja.length(); i++) {
                            parseTreasureBoxModel(loader, ja.getJSONObject(i), rankCacheKey);
                        }
                        JSONObject joPathRender = jo.getJSONObject("pathRenderModel");
                        String title = joPathRender.getString("title");
                        int minGoStepCount = joPathRender.getInt("minGoStepCount");
                        jo = jo.getJSONObject("dailyStepModel");
                        int consumeQuantity = jo.getInt("consumeQuantity");
                        int produceQuantity = jo.getInt("produceQuantity");
                        String day = jo.getString("day");
                        int canMoveStepCount = produceQuantity - consumeQuantity;
                        if (canMoveStepCount >= minGoStepCount) {
                            go(loader, day, rankCacheKey, canMoveStepCount, title);
                        }
                    }
                } else if ("NOT_JOIN".equals(s)) {
                    String firstJoinPathTitle = jo.getString("firstJoinPathTitle");
                    JSONArray allPathBaseInfoList = jo.getJSONArray("allPathBaseInfoList");
                    JSONArray otherAllPathBaseInfoList = jo.getJSONArray("otherAllPathBaseInfoList").getJSONObject(0)
                            .getJSONArray("allPathBaseInfoList");
                    join(loader, allPathBaseInfoList, otherAllPathBaseInfoList, firstJoinPathTitle);
                }
            } else {
                Log.runtime(TAG, jo.getString("resultDesc"));
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "queryMyHomePage err:");
            Log.printStackTrace(TAG, t);
        }
    }

    private void join(ClassLoader loader, JSONArray allPathBaseInfoList, JSONArray otherAllPathBaseInfoList,
                      String firstJoinPathTitle) {
        try {
            int index = -1;
            String title = null;
            String pathId = null;
            JSONObject jo = new JSONObject();
            for (int i = allPathBaseInfoList.length() - 1; i >= 0; i--) {
                jo = allPathBaseInfoList.getJSONObject(i);
                if (jo.getBoolean("unlocked")) {
                    title = jo.getString("title");
                    pathId = jo.getString("pathId");
                    index = i;
                    break;
                }
            }
            if (index < 0 || index == allPathBaseInfoList.length() - 1) {
                for (int j = otherAllPathBaseInfoList.length() - 1; j >= 0; j--) {
                    jo = otherAllPathBaseInfoList.getJSONObject(j);
                    if (jo.getBoolean("unlocked")) {
                        if (j != otherAllPathBaseInfoList.length() - 1 || index != allPathBaseInfoList.length() - 1) {
                            title = jo.getString("title");
                            pathId = jo.getString("pathId");
                            index = j;
                        }
                        break;
                    }
                }
            }
            if (index >= 0) {
                String s;
                if (title.equals(firstJoinPathTitle)) {
                    s = AntSportsRpcCall.openAndJoinFirst();
                } else {
                    s = AntSportsRpcCall.join(pathId);
                }
                jo = new JSONObject(s);
                if (ResChecker.checkRes(TAG + "加入运动线路失败:", jo)) {
                    Log.other(TAG, "加入线路🚶🏻‍♂️[" + title + "]");
                    queryMyHomePage(loader);
                } else {
                    Log.runtime(TAG, jo.getString("resultDesc"));
                }
            } else {
                Log.record(TAG, "好像没有可走的线路了！");
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "join err:");
            Log.printStackTrace(TAG, t);
        }
    }

    private void go(ClassLoader loader, String day, String rankCacheKey, int stepCount, String title) {
        try {
            String s = AntSportsRpcCall.go(day, rankCacheKey, stepCount);
            if (s == null || s.trim().isEmpty()) {
                Log.record(TAG, "行走前进失败：返回空响应");
                return;
            }
            
            JSONObject jo = new JSONObject(s);
            if (ResChecker.checkRes(TAG + "运动行走失败:", jo)) {
                Log.other(TAG, "行走线路🚶🏻‍♂️[" + title + "]#前进了" + jo.getInt("goStepCount") + "步");
                boolean completed = "COMPLETED".equals(jo.getString("completeStatus"));
                JSONArray ja = jo.getJSONArray("allTreasureBoxModelList");
                for (int i = 0; i < ja.length(); i++) {
                    parseTreasureBoxModel(loader, ja.getJSONObject(i), rankCacheKey);
                }
                if (completed) {
                    Log.other(TAG, "完成线路🚶🏻‍♂️[" + title + "]");
                    queryMyHomePage(loader);
                }
            } else {
                Log.runtime(TAG, jo.getString("resultDesc"));
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "go err:");
            Log.printStackTrace(TAG, t);
        }
    }

    private void parseTreasureBoxModel(ClassLoader loader, JSONObject jo, String rankCacheKey) {
        try {
            String canOpenTime = jo.getString("canOpenTime");
            String issueTime = jo.getString("issueTime");
            String boxNo = jo.getString("boxNo");
            String userId = jo.getString("userId");
            if (canOpenTime.equals(issueTime)) {
                openTreasureBox(loader, boxNo, userId);
            } else {
                long cot = Long.parseLong(canOpenTime);
                long now = Long.parseLong(rankCacheKey);
                long delay = cot - now;
                if (delay <= 0) {
                    openTreasureBox(loader, boxNo, userId);
                    return;
                }
                if (delay < BaseModel.getCheckInterval().getValue()) {
                    String taskId = "BX|" + boxNo;
                    if (hasChildTask(taskId)) {
                        return;
                    }
                    Log.record(TAG, "还有 " + delay + "ms 开运动宝箱");
                    addChildTask(new ChildModelTask(taskId, "BX", () -> {
                        Log.record(TAG, "蹲点开箱开始");
                        long startTime = System.currentTimeMillis();
                        while (System.currentTimeMillis() - startTime < 5_000) {
                            if (openTreasureBox(loader, boxNo, userId) > 0) {
                                break;
                            }
                            GlobalThreadPools.sleep(200);
                        }
                    }, System.currentTimeMillis() + delay));
                }
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "parseTreasureBoxModel err:");
            Log.printStackTrace(TAG, t);
        }
    }

    private int openTreasureBox(ClassLoader loader, String boxNo, String userId) {
        try {
            String s = AntSportsRpcCall.openTreasureBox(boxNo, userId);
            if (s == null || s.trim().isEmpty()) {
                Log.record(TAG, "开启宝箱失败：返回空响应");
                return 0;
            }
            
            JSONObject jo = new JSONObject(s);
            if (ResChecker.checkRes(TAG + "开启运动宝箱失败:", jo)) {
                JSONArray ja = jo.getJSONArray("treasureBoxAwards");
                int num = 0;
                for (int i = 0; i < ja.length(); i++) {
                    jo = ja.getJSONObject(i);
                    num += jo.getInt("num");
                    Log.other(TAG, "运动宝箱🎁[" + num + jo.getString("name") + "]");
                }
                return num;
            } else if ("TREASUREBOX_NOT_EXIST".equals(jo.getString("resultCode"))) {
                Log.record(jo.getString("resultDesc"));
                return 1;
            } else {
                Log.record(jo.getString("resultDesc"));
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "openTreasureBox err:");
            Log.printStackTrace(TAG, t);
        }
        return 0;
    }

    private void queryProjectList(ClassLoader loader) {
        try {
            String projectListResponse = AntSportsRpcCall.queryProjectList(0);
            if (projectListResponse == null || projectListResponse.trim().isEmpty()) {
                Log.record(TAG, "查询项目列表失败：返回空响应");
                return;
            }
            
            JSONObject jo = new JSONObject(projectListResponse);
            if (ResChecker.checkRes(TAG + "查询运动项目列表失败:", jo)) {
                int charityCoinCount = jo.getInt("charityCoinCount");
                if (charityCoinCount < donateCharityCoinAmount.getValue()) {
                    return;
                }
                JSONArray ja = jo.getJSONObject("projectPage").getJSONArray("data");
                for (int i = 0; i < ja.length() && charityCoinCount >= donateCharityCoinAmount.getValue(); i++) {
                    jo = ja.getJSONObject(i).getJSONObject("basicModel");
                    if ("DONATE_COMPLETED".equals(jo.getString("footballFieldStatus"))) {
                        break;
                    }
                    donate(loader, donateCharityCoinAmount.getValue(), jo.getString("projectId"), jo.getString("title"));
                    Status.donateCharityCoin();
                    charityCoinCount -= donateCharityCoinAmount.getValue();
                    if (donateCharityCoinType.getValue() == DonateCharityCoinType.ONE) {
                        break;
                    }
                }
            } else {
                Log.record(TAG);
                Log.runtime(jo.getString("resultDesc"));
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "queryProjectList err:");
            Log.printStackTrace(TAG, t);
        }
    }

    private void donate(ClassLoader loader, int donateCharityCoin, String projectId, String title) {
        try {
            String s = AntSportsRpcCall.donate(donateCharityCoin, projectId);
            JSONObject jo = new JSONObject(s);
            if (ResChecker.checkRes(TAG + "运动捐赠失败:", jo)) {
                Log.other(TAG, "捐赠活动❤️[" + title + "][" + donateCharityCoin + "运动币]");
            } else {
                Log.runtime(TAG, jo.getString("resultDesc"));
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "donate err:");
            Log.printStackTrace(TAG, t);
        }
    }

    private void queryWalkStep(ClassLoader loader) {
        try {
            String s = AntSportsRpcCall.queryWalkStep();
            if (s == null || s.trim().isEmpty()) {
                Log.record(TAG, "查询行走步数失败：返回空响应");
                return;
            }
            
            JSONObject jo = new JSONObject(s);
            if (ResChecker.checkRes(TAG + "查询运动步数失败:", jo)) {
                jo = jo.getJSONObject("dailyStepModel");
                int produceQuantity = jo.getInt("produceQuantity");
                int hour = Integer.parseInt(TimeUtil.getFormatTime().split(":")[0]);
                ;
                if (produceQuantity >= minExchangeCount.getValue() || hour >= latestExchangeTime.getValue()) {
                    s = AntSportsRpcCall.walkDonateSignInfo(produceQuantity);
                    s = AntSportsRpcCall.donateWalkHome(produceQuantity);
                    jo = new JSONObject(s);
                    if (!jo.getBoolean("isSuccess"))
                        return;
                    JSONObject walkDonateHomeModel = jo.getJSONObject("walkDonateHomeModel");
                    JSONObject walkUserInfoModel = walkDonateHomeModel.getJSONObject("walkUserInfoModel");
                    if (!walkUserInfoModel.has("exchangeFlag")) {
                        Status.exchangeToday(UserMap.getCurrentUid());
                        return;
                    }
                    String donateToken = walkDonateHomeModel.getString("donateToken");
                    JSONObject walkCharityActivityModel = walkDonateHomeModel.getJSONObject("walkCharityActivityModel");
                    String activityId = walkCharityActivityModel.getString("activityId");
                    s = AntSportsRpcCall.exchange(activityId, produceQuantity, donateToken);
                    jo = new JSONObject(s);
                    if (jo.getBoolean("isSuccess")) {
                        JSONObject donateExchangeResultModel = jo.getJSONObject("donateExchangeResultModel");
                        int userCount = donateExchangeResultModel.getInt("userCount");
                        double amount = donateExchangeResultModel.getJSONObject("userAmount").getDouble("amount");
                        Log.other(TAG, "捐出活动❤️[" + userCount + "步]#兑换" + amount + "元公益金");
                        Status.exchangeToday(UserMap.getCurrentUid());
                    } else if (s.contains("已捐步")) {
                        Status.exchangeToday(UserMap.getCurrentUid());
                    } else {
                        Log.runtime(TAG, jo.getString("resultDesc"));
                    }
                }
            } else {
                Log.runtime(TAG, jo.getString("resultDesc"));
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "queryWalkStep err:");
            Log.printStackTrace(TAG, t);
        }
    }

    /* 文体中心 */// SPORTS_DAILY_SIGN_GROUP SPORTS_DAILY_GROUP
    private void userTaskGroupQuery(String groupId) {
        try {
            String s = AntSportsRpcCall.userTaskGroupQuery(groupId);
            if (s == null || s.trim().isEmpty()) {
                Log.record(TAG, "查询任务组失败：返回空响应");
                return;
            }
            
            JSONObject jo = new JSONObject(s);
            if (jo.optBoolean("success")) {
                jo = jo.getJSONObject("group");
                JSONArray userTaskList = jo.getJSONArray("userTaskList");
                for (int i = 0; i < userTaskList.length(); i++) {
                    jo = userTaskList.getJSONObject(i);
                    if (!"TODO".equals(jo.getString("status")))
                        continue;
                    JSONObject taskInfo = jo.getJSONObject("taskInfo");
                    String bizType = taskInfo.getString("bizType");
                    String taskId = taskInfo.getString("taskId");
                    
                    String completeResponse = AntSportsRpcCall.userTaskComplete(bizType, taskId);
                    if (completeResponse == null || completeResponse.trim().isEmpty()) {
                        Log.record(TAG, "完成任务失败：返回空响应");
                        continue;
                    }
                    
                    jo = new JSONObject(completeResponse);
                    if (jo.optBoolean("success")) {
                        String taskName = taskInfo.optString("taskName", taskId);
                        Log.other(TAG, "完成任务🧾[" + taskName + "]");
                    } else {
                        Log.record(TAG, "文体每日任务" + " " + jo);
                    }
                }
            } else {
                Log.record(TAG, "文体每日任务" + " " + s);
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "userTaskGroupQuery err:");
            Log.printStackTrace(TAG, t);
        }
    }

    private void participate() {
        try {
            String s = AntSportsRpcCall.queryAccount();
            if (s == null || s.trim().isEmpty()) {
                Log.record(TAG, "查询账户信息失败：返回空响应");
                return;
            }
            
            JSONObject jo = new JSONObject(s);
            if (jo.optBoolean("success")) {
                double balance = jo.getDouble("balance");
                if (balance < 100)
                    return;
                jo = new JSONObject(AntSportsRpcCall.queryRoundList());
                if (jo.optBoolean("success")) {
                    JSONArray dataList = jo.getJSONArray("dataList");
                    for (int i = 0; i < dataList.length(); i++) {
                        jo = dataList.getJSONObject(i);
                        if (!"P".equals(jo.getString("status")))
                            continue;
                        if (jo.has("userRecord"))
                            continue;
                        JSONArray instanceList = jo.getJSONArray("instanceList");
                        int pointOptions = 0;
                        String roundId = jo.getString("id");
                        String InstanceId = null;
                        String ResultId = null;
                        for (int j = instanceList.length() - 1; j >= 0; j--) {
                            jo = instanceList.getJSONObject(j);
                            if (jo.getInt("pointOptions") < pointOptions)
                                continue;
                            pointOptions = jo.getInt("pointOptions");
                            InstanceId = jo.getString("id");
                            ResultId = jo.getString("instanceResultId");
                        }
                        jo = new JSONObject(AntSportsRpcCall.participate(pointOptions, InstanceId, ResultId, roundId));
                        if (jo.optBoolean("success")) {
                            jo = jo.getJSONObject("data");
                            String roundDescription = jo.getString("roundDescription");
                            int targetStepCount = jo.getInt("targetStepCount");
                            Log.other(TAG, "走路挑战🚶🏻‍♂️[" + roundDescription + "]#" + targetStepCount);
                        } else {
                            Log.record(TAG, "走路挑战赛" + " " + jo);
                        }
                    }
                } else {
                    Log.record(TAG, "queryRoundList" + " " + jo);
                }
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "participate err:");
            Log.printStackTrace(TAG, t);
        }
    }

    private void userTaskRightsReceive() {
        try {
            String s = AntSportsRpcCall.userTaskGroupQuery("SPORTS_DAILY_GROUP");
            if (s == null || s.trim().isEmpty()) {
                Log.record(TAG, "查询任务组失败：返回空响应");
                return;
            }
            
            JSONObject jo = new JSONObject(s);
            if (jo.optBoolean("success")) {
                jo = jo.getJSONObject("group");
                JSONArray userTaskList = jo.getJSONArray("userTaskList");
                for (int i = 0; i < userTaskList.length(); i++) {
                    jo = userTaskList.getJSONObject(i);
                    if (!"COMPLETED".equals(jo.getString("status")))
                        continue;
                    String userTaskId = jo.getString("userTaskId");
                    JSONObject taskInfo = jo.getJSONObject("taskInfo");
                    String taskId = taskInfo.getString("taskId");
                    jo = new JSONObject(AntSportsRpcCall.userTaskRightsReceive(taskId, userTaskId));
                    if (jo.optBoolean("success")) {
                        String taskName = taskInfo.optString("taskName", taskId);
                        JSONArray rightsRuleList = taskInfo.getJSONArray("rightsRuleList");
                        StringBuilder award = new StringBuilder();
                        for (int j = 0; j < rightsRuleList.length(); j++) {
                            jo = rightsRuleList.getJSONObject(j);
                            award.append(jo.getString("rightsName")).append("*").append(jo.getInt("baseAwardCount"));
                        }
                        Log.other(TAG, "领取奖励🎖️[" + taskName + "]#" + award);
                    } else {
                        Log.record(TAG, "文体中心领取奖励");
                        Log.runtime(jo.toString());
                    }
                }
            } else {
                Log.record(TAG, "文体中心领取奖励");
                Log.runtime(s);
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "userTaskRightsReceive err:");
            Log.printStackTrace(TAG, t);
        }
    }

    private void pathFeatureQuery() {
        try {
            String s = AntSportsRpcCall.pathFeatureQuery();
            JSONObject jo = new JSONObject(s);
            if (jo.optBoolean("success")) {
                JSONObject path = jo.getJSONObject("path");
                String pathId = path.getString("pathId");
                String title = path.getString("title");
                int minGoStepCount = path.getInt("minGoStepCount");
                if (jo.has("userPath")) {
                    JSONObject userPath = jo.getJSONObject("userPath");
                    String userPathRecordStatus = userPath.getString("userPathRecordStatus");
                    if ("COMPLETED".equals(userPathRecordStatus)) {
                        pathMapHomepage(pathId);
                        pathMapJoin(title, pathId);
                    } else if ("GOING".equals(userPathRecordStatus)) {
                        pathMapHomepage(pathId);
                        String countDate = TimeUtil.getFormatDate();
                        jo = new JSONObject(AntSportsRpcCall.stepQuery(countDate, pathId));
                        if (jo.optBoolean("success")) {
                            int canGoStepCount = jo.getInt("canGoStepCount");
                            if (canGoStepCount >= minGoStepCount) {
                                String userPathRecordId = userPath.getString("userPathRecordId");
                                tiyubizGo(countDate, title, canGoStepCount, pathId, userPathRecordId);
                            }
                        }
                    }
                } else {
                    pathMapJoin(title, pathId);
                }
            } else {
                Log.runtime(TAG, jo.getString("resultDesc"));
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "pathFeatureQuery err:");
            Log.printStackTrace(TAG, t);
        }
    }

    private void pathMapHomepage(String pathId) {
        try {
            String s = AntSportsRpcCall.pathMapHomepage(pathId);
            JSONObject jo = new JSONObject(s);
            if (jo.optBoolean("success")) {
                if (!jo.has("userPathGoRewardList"))
                    return;
                JSONArray userPathGoRewardList = jo.getJSONArray("userPathGoRewardList");
                for (int i = 0; i < userPathGoRewardList.length(); i++) {
                    jo = userPathGoRewardList.getJSONObject(i);
                    if (!"UNRECEIVED".equals(jo.getString("status")))
                        continue;
                    String userPathRewardId = jo.getString("userPathRewardId");
                    jo = new JSONObject(AntSportsRpcCall.rewardReceive(pathId, userPathRewardId));
                    if (jo.optBoolean("success")) {
                        jo = jo.getJSONObject("userPathRewardDetail");
                        JSONArray rightsRuleList = jo.getJSONArray("userPathRewardRightsList");
                        StringBuilder award = new StringBuilder();
                        for (int j = 0; j < rightsRuleList.length(); j++) {
                            jo = rightsRuleList.getJSONObject(j).getJSONObject("rightsContent");
                            award.append(jo.getString("name")).append("*").append(jo.getInt("count"));
                        }
                        Log.other(TAG, "文体宝箱🎁[" + award + "]");
                    } else {
                        Log.record(TAG, "文体中心开宝箱");
                        Log.runtime(jo.toString());
                    }
                }
            } else {
                Log.record(TAG, "文体中心开宝箱");
                Log.runtime(s);
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "pathMapHomepage err:");
            Log.printStackTrace(TAG, t);
        }
    }

    private void pathMapJoin(String title, String pathId) {
        try {
            JSONObject jo = new JSONObject(AntSportsRpcCall.pathMapJoin(pathId));
            if (jo.optBoolean("success")) {
                Log.other(TAG, "加入线路🚶🏻‍♂️[" + title + "]");
                pathFeatureQuery();
            } else {
                Log.runtime(TAG, jo.toString());
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "pathMapJoin err:");
            Log.printStackTrace(TAG, t);
        }
    }

    private void tiyubizGo(String countDate, String title, int goStepCount, String pathId,
                           String userPathRecordId) {
        try {
            String s = AntSportsRpcCall.tiyubizGo(countDate, goStepCount, pathId, userPathRecordId);
            JSONObject jo = new JSONObject(s);
            if (jo.optBoolean("success")) {
                jo = jo.getJSONObject("userPath");
                Log.other(TAG, "行走线路🚶🏻‍♂️[" + title + "]#前进了" + jo.getInt("userPathRecordForwardStepCount") + "步");
                pathMapHomepage(pathId);
                boolean completed = "COMPLETED".equals(jo.getString("userPathRecordStatus"));
                if (completed) {
                    Log.other(TAG, "完成线路🚶🏻‍♂️[" + title + "]");
                    pathFeatureQuery();
                }
            } else {
                Log.runtime(TAG, s);
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "tiyubizGo err:");
            Log.printStackTrace(TAG, t);
        }
    }

    /* 抢好友大战 */
    private void queryClubHome() {
        try {
            // 发送 RPC 请求获取 club home 数据
            JSONObject clubHomeData = new JSONObject(AntSportsRpcCall.queryClubHome());
            // 处理 mainRoom 中的 bubbleList
            processBubbleList(clubHomeData.optJSONObject("mainRoom"));
            // 处理 roomList 中的每个房间的 bubbleList
            JSONArray roomList = clubHomeData.optJSONArray("roomList");
            if (roomList != null) {
                for (int i = 0; i < roomList.length(); i++) {
                    JSONObject room = roomList.optJSONObject(i);
                    processBubbleList(room);
                }
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "queryClubHome err:");
            Log.printStackTrace(TAG, t);
        }
    }

    // 抢好友大战-收金币
    private void processBubbleList(JSONObject object) {
        if (object != null && object.has("bubbleList")) {
            try {
                JSONArray bubbleList = object.getJSONArray("bubbleList");
                for (int j = 0; j < bubbleList.length(); j++) {
                    JSONObject bubble = bubbleList.getJSONObject(j);
                    // 获取 bubbleId
                    String bubbleId = bubble.optString("bubbleId");
                    // 调用 collectBubble 方法
                    AntSportsRpcCall.collectBubble(bubbleId);
                    // 输出日志信息
                    int fullCoin = bubble.optInt("fullCoin");
                    Log.other(TAG, "训练好友💰️[获得:" + fullCoin + "金币]");
                    // 添加 1 秒的等待时间
                    GlobalThreadPools.sleep(1000);
                }
            } catch (Throwable t) {
                Log.runtime(TAG, "processBubbleList err:");
                Log.printStackTrace(TAG, t);
            }
        }
    }

    // 抢好友大战-训练好友
    private void queryTrainItem() {
        try {
            // 发送 RPC 请求获取 club home 数据
            JSONObject clubHomeData = new JSONObject(AntSportsRpcCall.queryClubHome());
            // 检查是否存在 roomList
            if (clubHomeData.has("roomList")) {
                JSONArray roomList = clubHomeData.getJSONArray("roomList");
                // 遍历 roomList
                for (int i = 0; i < roomList.length(); i++) {
                    JSONObject room = roomList.getJSONObject(i);
                    // 获取 memberList
                    JSONArray memberList = room.getJSONArray("memberList");
                    // 遍历 memberList
                    for (int j = 0; j < memberList.length(); j++) {
                        JSONObject member = memberList.getJSONObject(j);
                        // 提取 memberId 和 originBossId
                        String memberId = member.getString("memberId");
                        String originBossId = member.getString("originBossId");
                        // 获取用户名称
                        String userName = UserMap.getMaskName(originBossId);
                        // 发送 RPC 请求获取 train item 数据
                        String responseData = AntSportsRpcCall.queryTrainItem();
                        // 解析 JSON 数据
                        JSONObject responseJson = new JSONObject(responseData);
                        // 检查请求是否成功
                        boolean success = responseJson.optBoolean("success");
                        if (!success) {
                            return;
                        }
                        // 获取 trainItemList
                        JSONArray trainItemList = responseJson.getJSONArray("trainItemList");
                        // 遍历 trainItemList
                        for (int k = 0; k < trainItemList.length(); k++) {
                            JSONObject trainItem = trainItemList.getJSONObject(k);
                            // 提取训练项目的相关信息
                            String itemType = trainItem.getString("itemType");
                            // 如果找到了 itemType 为 "barbell" 的训练项目，则调用 trainMember 方法并传递 itemType、memberId 和 originBossId 值
                            if ("barbell".equals(itemType)) {
                                // 调用 trainMember 方法并传递 itemType、memberId 和 originBossId 值
                                String trainMemberResponse = AntSportsRpcCall.trainMember(itemType, memberId, originBossId);
                                // 解析 trainMember 响应数据
                                JSONObject trainMemberResponseJson = new JSONObject(trainMemberResponse);
                                // 检查 trainMember 响应是否成功
                                boolean trainMemberSuccess = trainMemberResponseJson.optBoolean("success");
                                if (!trainMemberSuccess) {
                                    Log.runtime(TAG, "trainMember request failed");
                                    continue; // 如果 trainMember 请求失败，继续处理下一个训练项目
                                }
                                // 获取训练项目的名称
                                String trainItemName = trainItem.getString("name");
                                // 将用户名称和训练项目的名称添加到日志输出
                                Log.other(TAG, "训练好友🥋[训练:" + userName + " " + trainItemName + "]");
                            }
                        }
                    }
                    // 添加 1 秒的间隔
                    GlobalThreadPools.sleep(1000);
                }
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "queryTrainItem err:");
            Log.printStackTrace(TAG, t);
        }
    }

    // 抢好友大战-抢购好友
    private void buyMember() {
        try {
            // 发送 RPC 请求获取 club home 数据
            String clubHomeResponse = AntSportsRpcCall.queryClubHome();
            GlobalThreadPools.sleep(500);
            JSONObject clubHomeJson = new JSONObject(clubHomeResponse);
            // 判断 clubAuth 字段是否为 "ENABLE"
            if (!clubHomeJson.optString("clubAuth").equals("ENABLE")) {
                // 如果 clubAuth 不是 "ENABLE"，停止执行
                Log.record(TAG, "抢好友大战🧑‍🤝‍🧑未授权开启");
                return;
            }
            // 获取 coinBalance 的值
            JSONObject assetsInfo = clubHomeJson.getJSONObject("assetsInfo");
            int coinBalance = assetsInfo.getInt("coinBalance");
            JSONArray roomList = clubHomeJson.getJSONArray("roomList");
            // 遍历 roomList
            for (int i = 0; i < roomList.length(); i++) {
                JSONObject room = roomList.getJSONObject(i);
                JSONArray memberList = room.optJSONArray("memberList");
                // 检查 memberList 是否为空
                if (memberList == null || memberList.length() == 0) {
                    // 获取 roomId 的值
                    String roomId = room.getString("roomId");
                    // 调用 queryMemberPriceRanking 方法并传递 coinBalance 的值
                    String memberPriceResult = AntSportsRpcCall.queryMemberPriceRanking(String.valueOf(coinBalance));
                    GlobalThreadPools.sleep(500);
                    JSONObject memberPriceJson = new JSONObject(memberPriceResult);
                    // 检查是否存在 rank 字段
                    if (memberPriceJson.has("rank") && memberPriceJson.getJSONObject("rank").has("data")) {
                        JSONArray dataArray = memberPriceJson.getJSONObject("rank").getJSONArray("data");
                        // 遍历 data 数组
                        for (int j = 0; j < dataArray.length(); j++) {
                            JSONObject dataObj = dataArray.getJSONObject(j);
                            String originBossId = dataObj.getString("originBossId");
                            // 检查 originBossId 是否在 originBossIdList 中
                            boolean isBattleForFriend = originBossIdList.getValue().contains(originBossId);
                            if (battleForFriendType.getValue() == BattleForFriendType.DONT_ROB) {
                                isBattleForFriend = !isBattleForFriend;
                            }
                            if (isBattleForFriend) {
                                // 在这里调用 queryClubMember 方法并传递 memberId 和 originBossId 的值
                                String clubMemberResult = AntSportsRpcCall.queryClubMember(dataObj.getString("memberId"), originBossId);
                                GlobalThreadPools.sleep(500);
                                // 解析 queryClubMember 返回的 JSON 数据
                                JSONObject clubMemberJson = new JSONObject(clubMemberResult);
                                if (clubMemberJson.has("member")) {
                                    JSONObject memberObj = clubMemberJson.getJSONObject("member");
                                    // 获取当前成员的信息
                                    String currentBossId = memberObj.getString("currentBossId");
                                    String memberId = memberObj.getString("memberId");
                                    String priceInfo = memberObj.getString("priceInfo");
                                    // 调用 buyMember 方法
                                    String buyMemberResult = AntSportsRpcCall.buyMember(currentBossId, memberId, originBossId, priceInfo, roomId);
                                    GlobalThreadPools.sleep(500);
                                    // 处理 buyMember 的返回结果
                                    JSONObject buyMemberResponse = new JSONObject(buyMemberResult);
                                    if (ResChecker.checkRes(TAG + "抢购运动好友失败:", buyMemberResponse)) {
                                        String userName = UserMap.getMaskName(originBossId);
                                        Log.other(TAG, "抢购好友🥋[成功:将 " + userName + " 抢回来]");
                                        // 执行训练好友
                                        queryTrainItem();
                                    } else if ("CLUB_AMOUNT_NOT_ENOUGH".equals(buyMemberResponse.getString("resultCode"))) {
                                        Log.record(TAG, "[运动币不足，无法完成抢购好友！]");
                                    } else if ("CLUB_MEMBER_TRADE_PROTECT".equals(buyMemberResponse.getString("resultCode"))) {
                                        Log.record(TAG, "[暂时无法抢购好友，给Ta一段独处的时间吧！]");
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "buyMember err:");
            Log.printStackTrace(TAG, t);
        }
    }

    public interface WalkPathTheme {
        int DA_MEI_ZHONG_GUO = 0;
        int GONG_YI_YI_XIAO_BU = 1;
        int DENG_DING_ZHI_MA_SHAN = 2;
        int WEI_C_DA_TIAO_ZHAN = 3;
        int LONG_NIAN_QI_FU = 4;
        String[] nickNames = {"大美中国", "公益一小步", "登顶芝麻山", "维C大挑战", "龙年祈福"};
    }

    public interface DonateCharityCoinType {
        int ONE = 0;
        int ALL = 1;
        String[] nickNames = {"捐赠一个项目", "捐赠所有项目"};
    }

    public interface BattleForFriendType {
        int ROB = 0;
        int DONT_ROB = 1;
        String[] nickNames = {"选中抢", "选中不抢"};
    }
}