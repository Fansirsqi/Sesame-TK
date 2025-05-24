package fansirsqi.xposed.sesame.task.recreation;

import org.json.JSONException;

import fansirsqi.xposed.sesame.model.BaseModel;
import fansirsqi.xposed.sesame.model.ModelFields;
import fansirsqi.xposed.sesame.model.ModelGroup;
import fansirsqi.xposed.sesame.model.modelFieldExt.BooleanModelField;
import fansirsqi.xposed.sesame.model.modelFieldExt.IntegerModelField;
import fansirsqi.xposed.sesame.model.modelFieldExt.StringModelField;
import fansirsqi.xposed.sesame.model.modelFieldExt.TextModelField;
import fansirsqi.xposed.sesame.task.ModelTask;
import fansirsqi.xposed.sesame.task.TaskCommon;
import fansirsqi.xposed.sesame.util.Log;
import fansirsqi.xposed.sesame.util.TimeUtil;
import lombok.Getter;

public class Recreation extends ModelTask {
    private static final String TAG = Recreation.class.getSimpleName();

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
    public BooleanModelField antFishpond; // 福气鱼塘
    @Getter
    public static BooleanModelField fishpondAngle; // 自动钓鱼
    @Getter
    public static StringModelField fishpondToken; // 钓鱼Token
    public BooleanModelField neverland; // 悦动健康岛
    public BooleanModelField neverLandJump; // 悦动健康岛跳一跳
    public BooleanModelField contentInteract; // 看视频领红包
    public IntegerModelField contentInteractCount; // 看视频领红包线程

    @Override
    public ModelFields getFields() {
        ModelFields modelFields = new ModelFields();
        modelFields.addField(startTime = new StringModelField("startTime", "开始执行时间(关闭:-1)", "0800"));
        modelFields.addField(antFishpond = new BooleanModelField("antFishpond", "福气鱼塘", false));
        modelFields.addField(fishpondAngle = new BooleanModelField("fishpondAngle", "福气鱼塘-自动钓鱼", false));
        modelFields.addField(fishpondToken = new StringModelField("fishpondToken", "福气鱼塘钓鱼Token", ""));
        modelFields.addField(neverland = new BooleanModelField("neverland", "悦动健康岛", false));
        modelFields.addField(neverLandJump = new BooleanModelField("neverLandJump", "悦动健康岛-跳一跳", false));
        modelFields.addField(contentInteract = new BooleanModelField("contentInteract", "看视频领红包", false));
        modelFields.addField(contentInteractCount = new IntegerModelField("contentInteractCount", "看视频领红包线程", 1));
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
            Log.record(TAG, "💤 不设执行时间，开始执行任务");
        } else if (!TimeUtil.isNowAfterTimeStr(value)) {
            Log.record(TAG, "💤 当前时间早于设定时间:" + value + "，不执行任务");
            return false;
        }
        return true;
    }

    @Override
    public void run() {

        Fishpond fish = new Fishpond(TAG);
        // 福气鱼塘任务
        if (antFishpond.getValue()){
            try {
                Log.other("执行" + Fishpond.displayName + "任务开始");
                fish.listTask();
                Log.other("执行"+Fishpond.displayName+"任务结束");
            } catch (RuntimeException e) {
                Log.other("执行"+Fishpond.displayName+"任务异常终止");
                throw new RuntimeException(e);
            }

        }

        // 自动钓鱼
        if(fishpondAngle.getValue()){
            try {
                Log.other("执行"+Fishpond.displayName+"钓鱼开始");
                fish.fishpondAngle();
                Log.other("执行"+Fishpond.displayName+"钓鱼结束");
            } catch (JSONException e) {
                Log.other("执行"+Fishpond.displayName+"自动钓鱼异常终止");
                throw new RuntimeException(e);
            }
        }

        // 悦动健康岛
        if(neverland.getValue()){

            if(neverLandJump.getValue()){

            }
        }

        // 看视频领红包
        if(contentInteract.getValue()){

        }

    }
}
