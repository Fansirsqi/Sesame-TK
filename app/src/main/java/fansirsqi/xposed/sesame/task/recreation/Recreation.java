package fansirsqi.xposed.sesame.task.recreation;

import static org.slf4j.MDC.put;

import org.json.JSONException;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

import fansirsqi.xposed.sesame.model.BaseModel;
import fansirsqi.xposed.sesame.model.ModelFields;
import fansirsqi.xposed.sesame.model.ModelGroup;
import fansirsqi.xposed.sesame.model.modelFieldExt.BooleanModelField;
import fansirsqi.xposed.sesame.model.modelFieldExt.IntegerModelField;
import fansirsqi.xposed.sesame.model.modelFieldExt.SelectModelField;
import fansirsqi.xposed.sesame.model.modelFieldExt.StringModelField;
import fansirsqi.xposed.sesame.task.ModelTask;
import fansirsqi.xposed.sesame.task.TaskCommon;
import fansirsqi.xposed.sesame.task.antDodo.AntDodoExternal;
import fansirsqi.xposed.sesame.util.Log;
import fansirsqi.xposed.sesame.util.TimeUtil;
import lombok.Getter;

public class Recreation extends ModelTask {
    private static final String TAG = Recreation.class.getSimpleName();
    private int executeIntervalInt = 2000;

    @Override
    public String getName() {
        return "娱乐";
    }

    @Override
    public ModelGroup getGroup() {
        return ModelGroup.RECREATION;
    }

    @Override
    public String getIcon() {
        return "";
    }

    public StringModelField startTime; // 开始执行时间
    @Getter
    public BooleanModelField antFishpond; // 福气鱼塘
    @Getter
    public static BooleanModelField fishpondAngle; // 自动钓鱼
    @Getter
    public static StringModelField fishpondToken; // 钓鱼Token
    @Getter
    public static SelectModelField antFishpondList; // 钓鱼邀请
    @Getter
    public static BooleanModelField neverLand; // 悦动健康岛
    @Getter
    public static BooleanModelField neverLandJump; // 悦动健康岛跳一跳
    @Getter
    public static BooleanModelField contentInteract; // 看视频领红包
    @Getter
    public static IntegerModelField contentInteractCount; // 看视频领红包线程
    public static SelectModelField contentInviteList; // 看视频领邀请好友
    @Getter
    public static BooleanModelField contentInteractVV; // 看短视频领红包

    @Override
    public ModelFields getFields() {
        ModelFields modelFields = new ModelFields();
        modelFields.addField(startTime = new StringModelField("startTime", "开始执行时间(关闭:-1)", "0800"));
        modelFields.addField(antFishpond = new BooleanModelField("antFishpond", "福气鱼塘", false));
        modelFields.addField(fishpondAngle = new BooleanModelField("fishpondAngle", "福气鱼塘-自动钓鱼", false));
        modelFields.addField(fishpondToken = new StringModelField("fishpondToken", "福气鱼塘钓鱼Token", ""));
        modelFields.addField(antFishpondList = new SelectModelField("antFishpondList", "福气鱼塘邀请好友", new LinkedHashSet<>(), new AntDodoExternal()));
        modelFields.addField(neverLand = new BooleanModelField("neverLand", "悦动健康岛", false));
        modelFields.addField(neverLandJump = new BooleanModelField("neverLandJump", "悦动健康岛-跳一跳", false));
        modelFields.addField(contentInteract = new BooleanModelField("contentInteract", "看视频领红包", false));
        modelFields.addField(contentInteractCount = new IntegerModelField("contentInteractCount", "看视频领红包线程", 1));
        modelFields.addField(contentInviteList = new SelectModelField("contentInviteList", "视频邀请好友列表", new LinkedHashSet<>(), new AntDodoExternal()));
        modelFields.addField(contentInteractVV = new BooleanModelField("contentInteractVV", "看短视频领红包", false));
        return modelFields;
    }

    @Override
    public Boolean check() {
        String value = startTime.getValue();
        if (TaskCommon.IS_ENERGY_TIME) {
            Log.record(TAG, "⏸ 当前为只收能量时间【" + BaseModel.getEnergyTime().getValue() + "】，停止执行" + getName() + "任务！");
            return false;
        } else if (TaskCommon.IS_MODULE_SLEEP_TIME) {
            Log.record(TAG, "💤 模块休眠时间【" + BaseModel.getModelSleepTime().getValue() + "】停止执行" + getName() + "任务！");
            return false;
        } else if ("-1".equals(value)) {
            Log.other(TAG, "💤 不设执行时间，开始执行任务");
        } else if (!TimeUtil.isNowAfterTimeStr(value)) {
            Log.other(TAG, "💤 当前时间早于设定时间:" + value + "，不执行任务");
            return false;
        }
        return true;
    }

    @Override
    public void run() {

        // 福气鱼塘任务
        if (antFishpond.getValue()) {
            try {
                Log.other("福气鱼塘🐟开始执行");
                new AntFishpond().run(this.executeIntervalInt, new LinkedHashMap<String, Object>() {
                    {
                        put("antFishpondList", Recreation.antFishpondList.getValue());
                        put("fishpondAngle", Recreation.fishpondAngle.getValue());
                    }
                });
                Log.other("福气鱼塘🐟执行结束");
            } catch (RuntimeException e) {
                Log.other("福气鱼塘🐟异常退出");
                throw new RuntimeException(e);
            }
        }else{
            Log.other("福气鱼塘🐟未启用");
        }
        // 悦动健康岛
        if (neverLand.getValue()) {
            try {
                Log.other("悦动健康岛🍰开始执行");
                new NeverLand().run(this.executeIntervalInt, new LinkedHashMap<String, Object>() {
                    {
                        put("neverLandJump", Recreation.neverLandJump.getValue());
                    }
                });
                Log.other("悦动健康岛🍰执行结束");
            } catch (RuntimeException e) {
                Log.other("悦动健康岛🍰异常退出");
                throw new RuntimeException(e);
            }
        } else {
            Log.other("悦动健康岛🍰未启用");
        }


        // 看视频领红包
        if (contentInteract.getValue()) {
            try {
                Log.other("看视频领红包🏯开始执行");
                new ContentInteract(this).run(this.executeIntervalInt, new LinkedHashMap<String, Object>() {
                    {
                        put("contentInteract", Recreation.contentInteract.getValue());
                        put("contentInteractCount", Recreation.contentInteractCount.getValue());
                        put("contentInteractVV", Recreation.contentInteractVV.getValue());
                    }
                }, contentInviteList.getValue());
                Log.other("看视频领红包🏯执行结束");
            } catch (RuntimeException e) {
                Log.other("看视频领红包🏯异常退出");
                throw new RuntimeException(e);
            }
        }

    }

}