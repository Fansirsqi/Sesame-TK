package fansirsqi.xposed.sesame.task.otherTask;

import android.os.Build;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import fansirsqi.xposed.sesame.data.Status;
import fansirsqi.xposed.sesame.entity.AlipayUser;
import fansirsqi.xposed.sesame.model.BaseModel;
import fansirsqi.xposed.sesame.model.ModelFields;
import fansirsqi.xposed.sesame.model.ModelGroup;
import fansirsqi.xposed.sesame.model.modelFieldExt.BooleanModelField;
import fansirsqi.xposed.sesame.model.modelFieldExt.IntegerModelField;
import fansirsqi.xposed.sesame.model.modelFieldExt.SelectModelField;
import fansirsqi.xposed.sesame.model.modelFieldExt.StringModelField;
import fansirsqi.xposed.sesame.model.modelFieldExt.TextModelField;
import fansirsqi.xposed.sesame.task.ModelTask;
import fansirsqi.xposed.sesame.task.TaskCommon;
import fansirsqi.xposed.sesame.task.antDodo.AntDodo;
import fansirsqi.xposed.sesame.util.JsonUtil;
import fansirsqi.xposed.sesame.util.Log;
import fansirsqi.xposed.sesame.util.RandomUtil;
import fansirsqi.xposed.sesame.util.TimeUtil;

public class OtherTask extends ModelTask {
    private static final String TAG = "🔥其他任务🔥";
    /**
     * private String displayName = "工作中心积分红包 🎉"; // 庆祝
     * private String taskName1 = "浏览游戏中心15秒 💼";  // 工作
     * private String taskName2 = "完成任务领取奖励 🏆"; // 奖杯
     * private String taskName3 = "热门活动进行中 🔥";   // 火热
     * private String taskName4 = "亮点任务推荐 ⭐";     // 星星
     * private String taskName5 = "创意任务 💡";         // 灯泡
     * private String taskName6 = "精准目标达成 🎯";     // 靶心
     * private String taskName7 = "快速启动任务 🚀";     // 火箭
     * private String taskName7 = "快速启动任务 🧠 ";     // 火箭
     * private String taskName7 = "快速启动任务 ✅ ";     // 火箭
     * private String taskName7 = "快速启动任务 ❌  ";     // 火箭
     * private String taskName7 = "快速启动任务 ⚠️  "📈🧾💵🏦
     * private String displayName = "月月赚 💰";
     * private String redEnvelopeTask = "实体红包 🧧";
     * private String videoRedEnvelopeTask = "看视频领红包 🎁";
     * private String shakeRedEnvelopeTask = "摇红包 💸";
     * private String salaryRainTask = "红包雨 🌦️";
     * private String dailyRewardTask = "每日福利 🎉";
     * 👑 💎 🛡️ 🔑
     */
    // 全局线程池
    private static final ExecutorService executor = Executors.newCachedThreadPool();

    @Override
    public String getName() {
        return "其他任务";
    }

    @Override
    public ModelGroup getGroup() {
        return ModelGroup.OTHER;
    }

    @Override
    public String getIcon() {
        return "AntSports.png";
    }
    @Override
    public Boolean isSync() {
        return true;
    }

    protected Integer executeIntervalInt = 2000;  // 执行间隔
    private final StringModelField startTime = new StringModelField("startTime", "开始执行时间(关闭:-1)", "0600");
    private final IntegerModelField executeInterval = new IntegerModelField("executeInterval", "执行间隔(毫秒)", this.executeIntervalInt);

    public final BooleanModelField contentInteract = new BooleanModelField("contentInteract", "看视频领红包", false);
    public final IntegerModelField contentInteractCount = new IntegerModelField("contentInteractCount", "看视频领红包-线程数", 1);
    //------------------------------------------------------------------------
    private static BooleanModelField fishpondAngle = new BooleanModelField("fishpondAngle", "福气鱼塘-自动钓鱼", true);
    private static StringModelField fishpondToken = new StringModelField("fishpondToken", "福气鱼塘钓鱼Token", "");
    private final BooleanModelField privilege = new BooleanModelField("privilege", "青春特权-兑换", false);
    private static final BooleanModelField privilegeNew = new BooleanModelField("privilegeNew", "青春特权-兑换新机制", false);
    private final BooleanModelField promoprodRedEnvelope = new BooleanModelField("promoprodRedEnvelope", "实体红包", true);
    private final BooleanModelField fundapplication = new BooleanModelField("fundapplication", "摇红包", true);
    private final BooleanModelField salaryday = new BooleanModelField("salaryday", "红包雨", true);
    private final BooleanModelField yebExpGold = new BooleanModelField("yebExpGold", "体验金", true);
    private final BooleanModelField antFishpond = new BooleanModelField("antFishpond", "福气鱼塘", false);
    private final SelectModelField antFishpondList = new SelectModelField("antFishpondList", "福气鱼塘邀请好友", new LinkedHashSet<>(), AlipayUser::getList);
    private final BooleanModelField hundredTimesDiscountCard = new BooleanModelField("hundredTimesDiscountCard", "百次立减卡", false);
    private final BooleanModelField neverland = new BooleanModelField("neverland", "悦动健康岛", false);
    private final BooleanModelField neverLandJump = new BooleanModelField("neverLandJump", "悦动健康岛-跳一跳", false);
    private static IntegerModelField neverLandJumpTIme = new IntegerModelField("neverLandJumpTIme", "每次跳多少步", 0);
    private static IntegerModelField neverLandJumpTImes = new IntegerModelField("neverLandJumpTImes", "每天多少次", 5);
    private final BooleanModelField luckCode = new BooleanModelField("luckcode", "收益天天乐", false);
    private final BooleanModelField goldTicket = new BooleanModelField("goldTicket", "黄金票", false);
    private final BooleanModelField antfarms = new BooleanModelField("antfarms", "芭芭农场任务", false);
    private final BooleanModelField huabeijin = new BooleanModelField("huabeijin", "花呗金", false);
    private final BooleanModelField travelDeals = new BooleanModelField("travelDeals", "出行特惠", false);
    private final BooleanModelField jobRight = new BooleanModelField("jobRight", "工作中心积分红包", false);
    private final BooleanModelField huaCard = new BooleanModelField("hauCard", "花花卡", false);
    private final BooleanModelField luckCard = new BooleanModelField("luckCard", "好运卡", false);
    private final BooleanModelField yebSceneBff = new BooleanModelField("yebSceneBff", "余额宝养鱼", false);
    private final BooleanModelField dayDaySave = new BooleanModelField("dayDaySave", "蛋定生财", false);


    //-------------------------------------------------------------------------
    public static BooleanModelField getFishpondAngle() {
        return fishpondAngle;
    }
    public static StringModelField getFishpondToken() {
        return fishpondToken;
    }
    public static IntegerModelField getneverLandJumpTIme() {
        return neverLandJumpTIme;
    }
    public static IntegerModelField getneverLandJumpTImes() {
        return neverLandJumpTImes;
    }
    public static BooleanModelField getPrivilegeNew() {
        return privilegeNew;
    }
    //------------------------------------------------------------------------

    @Override
    public ModelFields getFields() {
        ModelFields modelFields = new ModelFields();
        modelFields.addField(this.startTime);  // 开始执行时间
        modelFields.addField(this.executeInterval); // 执行间隔
        modelFields.addField(this.contentInteract);  // 看视频领红包
        modelFields.addField(this.contentInteractCount); // 视频线程
        modelFields.addField(this.antFishpond);  // 鱼塘
        modelFields.addField(this.fishpondAngle);  // 鱼塘自动钓鱼
        modelFields.addField(this.antFishpondList);  // 鱼塘邀请好友
        modelFields.addField(this.fishpondToken);  // 鱼塘token
        modelFields.addField(this.privilege);  // 青春特权兑换
        modelFields.addField(this.privilegeNew);  // 青春特权兑换新机制
        modelFields.addField(this.promoprodRedEnvelope);  // 实体红包
        modelFields.addField(this.fundapplication);  // 摇红包
        modelFields.addField(this.salaryday);  // 红包雨
        modelFields.addField(this.yebExpGold);  // 体验金
        modelFields.addField(this.hundredTimesDiscountCard);  // 百次立减
        modelFields.addField(this.neverland);  // 悦动健康
        modelFields.addField(this.neverLandJump);  // 悦动健康跳一跳
        modelFields.addField(this.neverLandJumpTIme);  // 悦动健康跳一跳
        modelFields.addField(this.neverLandJumpTImes);  // 悦动健康跳一跳
        modelFields.addField(this.luckCode);  // 收益天天乐
        modelFields.addField(this.goldTicket);  // 黄金票
        modelFields.addField(this.antfarms);  // 农场任务
        modelFields.addField(this.huabeijin);  // 花呗金
        modelFields.addField(this.travelDeals);  // 出行特惠
        modelFields.addField(this.jobRight);  // 工作中心积分
        modelFields.addField(this.huaCard);  // 花花卡
        modelFields.addField(this.luckCard);  // 好运卡
        modelFields.addField(this.yebSceneBff); // 余额宝养鱼
        modelFields.addField(this.dayDaySave);// 蛋定生财
        return modelFields;
    }

    @Override
    public Boolean check() {
        if (TaskCommon.IS_ENERGY_TIME) {
            Log.record("⏸ 当前为只收能量时间【" + BaseModel.getEnergyTime().getValue() + "】，停止执行" + getName() + "任务！");
            return false;
        } else if (TaskCommon.IS_MODULE_SLEEP_TIME) {
            Log.record("💤 模块休眠时间【" + BaseModel.getModelSleepTime().getValue() + "】停止执行" + getName() + "任务！");
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void run() {
        this.executeIntervalInt = Math.max(this.executeInterval.getValue(), this.executeIntervalInt);

        // 分组执行任务
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            CompletableFuture.runAsync(() -> executeGroup1(), executor);
        }else {
            new Thread(() -> executeGroup1()).start();
        }
        TimeUtil.sleep(RandomUtil.nextInt(1000,5000));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            CompletableFuture.runAsync(() -> executeGroup2(), executor);
        }else {
            new Thread(() -> executeGroup2()).start();
        }
        TimeUtil.sleep(RandomUtil.nextInt(10000,20000));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            CompletableFuture.runAsync(() -> executeGroup3(), executor);
        }else{
            new Thread(() -> executeGroup3()).start();
        }
//        executeGroup1();
//        executeGroup2();
//        executeGroup3();
    }

    // 组1：青春特权兑换、看视频领红包
    private void executeGroup1() {
        try {
            // 青春特权兑换
            if (this.privilege.getValue()) {
                if(!Status.hasFlagToday(CompletedKeyEnum.privilegeEX.name())) {
                    new PrivilegeExTest().run(this.executeIntervalInt);
                }
            }
        } catch (Exception e) {
            Log.error(TAG + "青春特权兑换--error:" + e);
        }
        try {
            // 鱼塘
            if (this.antFishpond.getValue().booleanValue()) {
                new AntFishpond().run(this.executeIntervalInt.intValue(), new LinkedHashMap<String, Object>() {
                    {
                        put("antFishpondList", OtherTask.this.antFishpondList.getValue());
                        put("fishpondAngle", OtherTask.fishpondAngle.getValue());
                    }
                });
            }
        } catch (Exception e) {
            Log.error(TAG + "鱼塘--error:" + e);
        }

        try {
            // 出行特惠
            if (this.travelDeals.getValue()) {
                new TravelDeals().run(this.executeIntervalInt);
            }
        } catch (Exception e) {
            Log.error(TAG + "出行特惠--error:" + e);
        }

        try {
            // 悦动健康
            if (this.neverland.getValue()) {
                new NeverLand().run(this.executeIntervalInt, new LinkedHashMap<String, Object>() {{
                    put("neverLandJump", OtherTask.this.neverLandJump.getValue());
                }});
            }
        } catch (Exception e) {
            Log.error(TAG + "悦动健康--error:" + e);
        }
        try {
            // 看视频领红包
            if (!Status.hasFlagToday(CompletedKeyEnum.VIDEOCOMPLETE.name())) {
                if (this.contentInteract.getValue()) {
                    new ContentInteract(this).run(this.executeIntervalInt, new LinkedHashMap<String, Object>() {{
                        put("contentInteract", OtherTask.this.contentInteract.getValue());
                        put("contentInteractCount", OtherTask.this.contentInteractCount.getValue());
                    }});
                }
            }
        } catch (Exception e) {
            Log.error(TAG + "刷视频领红包--error:" + e);
        }
    }

    // 组2：
    private void executeGroup2() {
        try {
            // 花呗金
            if (this.huabeijin.getValue()) {
                new HuaBeiJin().run(this.executeIntervalInt);
            }
        } catch (Exception e) {
            Log.error(TAG + "花呗金--error:" + e);
        }
        try {
            // 好运卡
            if (this.luckCard.getValue()) {
                new LuckCard().run(this.executeIntervalInt);
            }
        } catch (Exception e) {
            Log.error(TAG + "好运卡--error:" + e);
        }
        try {
            // 花花卡
            if (this.huaCard.getValue()) {
                if (!Status.hasFlagToday("HuaHuaKa_TaskCompleted")) {
                    new HuaHuaKa().run(this.executeIntervalInt);
                }
            }
        } catch (Exception e) {
            Log.error(TAG + "花花卡--error:" + e);
        }
    }

    // 组3：体验金、百次立减、红包雨、摇红包、实体红包、收益天天乐、黄金票、农场任务、花呗金
    private void executeGroup3() {
        try {
            // 体验金
            if (this.yebExpGold.getValue()) {
                new YebExpGold().handle(this.executeIntervalInt);
            }
        } catch (Exception e) {
            Log.error(TAG + "体验金--error:" + e);
        }

        try {
            // 余额宝养鱼
            if (this.yebSceneBff.getValue()) {
                new YebSceneBffish().run(this.executeIntervalInt);
            }
        } catch (Exception e) {
            Log.error(TAG + "余额宝养鱼--error:" + e);
        }

        try {
            // 蛋定生财
            if (this.dayDaySave.getValue()) {
                new DayDaySave().run(this.executeIntervalInt);
            }
        } catch (Exception e) {
            Log.error(TAG + "蛋定生财--error:" + e);
        }

        try {
            // 百次立减
            if (this.hundredTimesDiscountCard.getValue()) {
                new HundredTimesDiscountCard().run(this.executeIntervalInt);
            }
        } catch (Exception e) {
            Log.error(TAG + "百次立减--error:" + e);
        }

        try {
            // 红包雨
            if (this.salaryday.getValue()) {
                new Salaryday().run(this.executeIntervalInt);
            }
        } catch (Exception e) {
            Log.error(TAG + "红包雨--error:" + e);
        }

        try {
            // 摇红包
            if (this.fundapplication.getValue()) {
                new FundApplication().handle(this.executeIntervalInt);
            }
        } catch (Exception e) {
            Log.error(TAG + "摇红包--error:" + e);
        }

        try {
            // 实体红包
            if (this.promoprodRedEnvelope.getValue()) {
                promoprodTaskList();
            }
        } catch (Exception e) {
            Log.error(TAG + "实体红包--error:" + e);
        }

        try {
            // 收益天天乐
            if (this.luckCode.getValue()) {
                new LuckyCode().run(this.executeIntervalInt);
            }
        } catch (Exception e) {
            Log.error(TAG + "收益天天乐--error:" + e);
        }

        try {
            // 黄金票
            if (this.goldTicket.getValue()) {
                if (!Status.hasFlagToday("GoldTicket_TaskCompleted")) {
                    new GoldTicket().run(this.executeIntervalInt);
                }
            }
        } catch (Exception e) {
            Log.error(TAG + "黄金票--error:" + e);
        }
        try {
            // 工作积分红包
            if (this.jobRight.getValue()) {
                new JobRight().run(this.executeIntervalInt);
            }
        } catch (Exception e) {
            Log.error(TAG + "工作中心积分红包--error:" + e);
        }

        try {
            // 农场任务
            if (this.antfarms.getValue()) {
                new AntFarms().run(this.executeIntervalInt);
            }
        } catch (Exception e) {
            Log.error(TAG + "芭芭农场任务--error:" + e);
        }
    }

    //-----------------------------------------------------------------------
    private void promoprodTaskList() throws JSONException {
        JSONObject jSONObject;
        JSONArray jSONArray;
        int length;
        jSONObject = new JSONObject(OtherTaskRpcCall.queryTaskList());
        if (jSONObject.getBoolean("success") && (length = (jSONArray = jSONObject.getJSONArray("taskDetailList")).length()) != 0) {
            for (int i = 0; i < length; i++) {
                JSONObject jSONObject2 = jSONArray.getJSONObject(i);
                String string = jSONObject2.getString("taskProcessStatus");
                String string2 = jSONObject2.getString("taskType");
                if (!"RECEIVE_SUCCESS".equals(string) && !"TRANSFORMER".equals(string2)) {
                    if (!"SIGNUP_COMPLETE".equals(string)) {
                        JSONObject jSONObject3 = new JSONObject(OtherTaskRpcCall.signup(JsonUtil.getValueByPath(jSONObject2, "taskParticipateExtInfo.gplusItem"), jSONObject2.getString("taskId")));
                        if (!jSONObject3.getBoolean("success")) {
                            Log.error(TAG + ".queryTaskList.signup" + jSONObject3.optString("errorMsg"));
                        }
                        TimeUtil.sleep(this.executeIntervalInt);
                    }
                    JSONObject jSONObject4 = new JSONObject(OtherTaskRpcCall.complete(jSONObject2.getString("taskId")));
                    if (!jSONObject4.getBoolean("success")) {
                        Log.error(TAG + ".queryTaskList.complete" + jSONObject4.optString("errorMsg"));
                    } else {
                        Log.other("实体红包🍷获取[" + JsonUtil.getValueByPath(jSONObject4, "appletBaseConfigDTO.appletName") + "]" + JsonUtil.getValueByPath(jSONObject4, "prizeSendInfo.price.prizePrice") + "元");
                        TimeUtil.sleep(this.executeIntervalInt);
                    }
                }
            }
        }
    }
    //-----------------------------------------------------------------------

}
