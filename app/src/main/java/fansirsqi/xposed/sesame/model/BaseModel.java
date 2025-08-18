package fansirsqi.xposed.sesame.model;


import fansirsqi.xposed.sesame.model.modelFieldExt.BooleanModelField;
import fansirsqi.xposed.sesame.model.modelFieldExt.ChoiceModelField;
import fansirsqi.xposed.sesame.model.modelFieldExt.IntegerModelField;
import fansirsqi.xposed.sesame.model.modelFieldExt.ListModelField;
import fansirsqi.xposed.sesame.model.modelFieldExt.StringModelField;
import fansirsqi.xposed.sesame.util.ListUtil;
import fansirsqi.xposed.sesame.util.Log;
import fansirsqi.xposed.sesame.util.maps.BeachMap;
import fansirsqi.xposed.sesame.util.maps.IdMapManager;
import lombok.Getter;

/**
 * 基础配置模块
 */
public class BaseModel extends Model {
    private static final String TAG = "BaseModel";

    /**
     * 是否保持唤醒状态
     */
    @Getter
    public static final BooleanModelField stayAwake = new BooleanModelField("stayAwake", "保持唤醒", true);
    /**
     * 执行间隔时间（分钟）
     */
    @Getter
    public static final IntegerModelField.MultiplyIntegerModelField checkInterval =
            new IntegerModelField.MultiplyIntegerModelField("checkInterval", "执行间隔(分钟)", 50, 1, 12 * 60, 60_000);//此处调整至30分钟执行一次，可能会比平常耗电一点。。
    /**
     * 定时执行的时间点列表
     */
    @Getter
    public static final ListModelField.ListJoinCommaToStringModelField execAtTimeList =
            new ListModelField.ListJoinCommaToStringModelField("execAtTimeList", "定时执行(关闭:-1)", ListUtil.newArrayList(
                    "0700", "0730", "1200", "1230", "1700", "1730", "2000", "2030", "2359"
            ));
    /**
     * 定时唤醒的时间点列表
     */
    @Getter
    public static final ListModelField.ListJoinCommaToStringModelField wakenAtTimeList =
            new ListModelField.ListJoinCommaToStringModelField("wakenAtTimeList", "定时唤醒(关闭:-1)", ListUtil.newArrayList(
                    "0650", "2350"
            ));
    /**
     * 能量收集的时间范围
     */
    @Getter
    public static final ListModelField.ListJoinCommaToStringModelField energyTime =
            new ListModelField.ListJoinCommaToStringModelField("energyTime", "只收能量时间(范围|关闭:-1)", ListUtil.newArrayList("0700-0730"));

    /**
     * 模块休眠时间范围
     */
    @Getter
    public static final ListModelField.ListJoinCommaToStringModelField modelSleepTime =
            new ListModelField.ListJoinCommaToStringModelField("modelSleepTime", "模块休眠时间(范围|关闭:-1)", ListUtil.newArrayList("0100-0540"));

    /**
     * 定时任务模式选择
     */
    @Getter
    public static final ChoiceModelField timedTaskModel = new ChoiceModelField("timedTaskModel", "定时任务模式", TimedTaskModel.SYSTEM, TimedTaskModel.nickNames);
    /**
     * 超时是否重启
     */
    @Getter
    public static final BooleanModelField timeoutRestart = new BooleanModelField("timeoutRestart", "超时重启", true);
    /**
     * 异常发生时的等待时间（分钟）
     */
    @Getter
    public static final IntegerModelField.MultiplyIntegerModelField waitWhenException =
            new IntegerModelField.MultiplyIntegerModelField("waitWhenException", "异常等待时间(分钟)", 60, 0, 24 * 60, 60_000);
    /**
     * 异常通知开关
     */
    @Getter
    public static final BooleanModelField errNotify = new BooleanModelField("errNotify", "开启异常通知", false);

    @Getter
    public static final IntegerModelField setMaxErrorCount = new IntegerModelField("setMaxErrorCount", "异常次数阈值", 8);
    /**
     * 是否启用新接口（最低支持版本 v10.3.96.8100）
     */
    @Getter
    public static final BooleanModelField newRpc = new BooleanModelField("newRpc", "使用新接口(最低支持v10.3.96.8100)", true);
    /**
     * 是否开启抓包调试模式
     */
    @Getter
    public static final BooleanModelField debugMode = new BooleanModelField("debugMode", "开启抓包(基于新接口)", false);

    /**
     * 是否申请支付宝的后台运行权限
     */
    @Getter
    public static final BooleanModelField batteryPerm = new BooleanModelField("batteryPerm", "为支付宝申请后台运行权限", true);
    /**
     * 是否记录record日志
     */
    @Getter
    public static final BooleanModelField recordLog = new BooleanModelField("recordLog", "全部 | 记录record日志", true);
    /**
     * 是否记录runtime日志
     */
    @Getter
    public static final BooleanModelField runtimeLog = new BooleanModelField("runtimeLog", "全部 | 记录runtime日志", false);
    /**
     * 是否显示气泡提示
     */
    @Getter
    public static final BooleanModelField showToast = new BooleanModelField("showToast", "气泡提示", true);
    /**
     * 气泡提示的纵向偏移量
     */
    @Getter
    public static final IntegerModelField toastOffsetY = new IntegerModelField("toastOffsetY", "气泡纵向偏移", 99);
    /**
     * 只显示中文并设置时区
     */
    @Getter
    public static final BooleanModelField languageSimplifiedChinese = new BooleanModelField("languageSimplifiedChinese", "只显示中文并设置时区", true);
    /**
     * 是否开启状态栏禁删
     */
    @Getter
    public static final BooleanModelField enableOnGoing = new BooleanModelField("enableOnGoing", "开启状态栏禁删", false);

    @Getter
    public static final BooleanModelField sendHookData = new BooleanModelField("sendHookData", "启用Hook数据转发", false);
    @Getter
    static final StringModelField sendHookDataUrl = new StringModelField("sendHookDataUrl", "Hook数据转发地址", "http://127.0.0.1:9527/hook");

    @Override
    public String getName() {
        return "基础";
    }

    @Override
    public ModelGroup getGroup() {
        return ModelGroup.BASE;
    }

    @Override
    public String getIcon() {
        return "BaseModel.png";
    }

    @Override
    public String getEnableFieldName() {
        return "启用模块";
    }

    @Override
    public ModelFields getFields() {
        ModelFields modelFields = new ModelFields();
        modelFields.addField(stayAwake);//是否保持唤醒状态
        modelFields.addField(checkInterval);//执行间隔时间
        modelFields.addField(modelSleepTime);//模块休眠时间范围
        modelFields.addField(execAtTimeList);//定时执行的时间点列表
        modelFields.addField(wakenAtTimeList);//定时唤醒的时间点列表
        modelFields.addField(energyTime);//能量收集的时间范围
        modelFields.addField(timedTaskModel);//定时任务模式选择
        modelFields.addField(timeoutRestart);//超时是否重启
        modelFields.addField(waitWhenException);//异常发生时的等待时间
        modelFields.addField(errNotify);//异常通知开关
        modelFields.addField(setMaxErrorCount);//异常次数阈值
        modelFields.addField(newRpc);//是否启用新接口
        modelFields.addField(debugMode);//是否开启抓包调试模式
        modelFields.addField(sendHookData);//启用Hook数据转发
        modelFields.addField(sendHookDataUrl);//Hook数据转发地址
        modelFields.addField(batteryPerm);//是否申请支付宝的后台运行权限
        modelFields.addField(recordLog);//是否记录record日志
        modelFields.addField(runtimeLog);//是否记录runtime日志
        modelFields.addField(showToast);//是否显示气泡提示
        modelFields.addField(enableOnGoing);//是否开启状态栏禁删
        modelFields.addField(languageSimplifiedChinese);//是否只显示中文并设置时区
        modelFields.addField(toastOffsetY);//气泡提示的纵向偏移量
        return modelFields;
    }


    /**
     * 清理数据，在模块销毁时调用，清空 Reserve 和 Beach 数据。
     */
    public static void destroyData() {
        try {
            Log.runtime(TAG, "�清理所有数据");
            IdMapManager.getInstance(BeachMap.class).clear();
//            IdMapManager.getInstance(ReserveaMap.class).clear();
//            IdMapManager.getInstance(CooperateMap.class).clear();
//            IdMapManager.getInstance(MemberBenefitsMap.class).clear();
//            IdMapManager.getInstance(ParadiseCoinBenefitIdMap.class).clear();
//            IdMapManager.getInstance(VitalityRewardsMap.class).clear();
            //其他也可以清理清理
        } catch (Exception e) {
            Log.printStackTrace(e);
        }
    }


    public interface TimedTaskModel {
        int SYSTEM = 0;
        int PROGRAM = 1;
        String[] nickNames = {"�系统计时", "�程序计时"};
    }
}
