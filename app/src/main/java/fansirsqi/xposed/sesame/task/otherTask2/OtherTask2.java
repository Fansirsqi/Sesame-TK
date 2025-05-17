package fansirsqi.xposed.sesame.task.otherTask2;

import org.json.JSONException;

import fansirsqi.xposed.sesame.data.Status;
import fansirsqi.xposed.sesame.model.BaseModel;
import fansirsqi.xposed.sesame.model.ModelFields;
import fansirsqi.xposed.sesame.model.ModelGroup;
import fansirsqi.xposed.sesame.model.modelFieldExt.BooleanModelField;
import fansirsqi.xposed.sesame.model.modelFieldExt.IntegerModelField;
import fansirsqi.xposed.sesame.model.modelFieldExt.StringModelField;
import fansirsqi.xposed.sesame.task.ModelTask;
import fansirsqi.xposed.sesame.task.TaskCommon;
import fansirsqi.xposed.sesame.task.otherTask.CompletedKeyEnum;
import fansirsqi.xposed.sesame.util.Log;

public class OtherTask2 extends ModelTask {
    private static final String TAG = "🔥其他任务2🔥";

    @Override
    public String getName() {
        return "其他任务2";
    }
    protected Integer executeIntervalInt = 2000;  // 执行间隔
    private final StringModelField startTime = new StringModelField("startTime", "开始执行时间(关闭:-1)", "0700");
    private final IntegerModelField executeInterval = new IntegerModelField("executeInterval", "执行间隔(毫秒)", this.executeIntervalInt);
//    private final BooleanModelField payAwardProd = new BooleanModelField("payAwardProd", "支付赚红包", false);
    private BooleanModelField memberTaskNew = new BooleanModelField("memberTaskNew", "会员任务", true);
    private BooleanModelField expressTask = new BooleanModelField("expressTask", "快递积分", false);
    private BooleanModelField gameCenter = new BooleanModelField("gameCenter", "游戏中心浏览任务", false);
    private final BooleanModelField monthTRA = new BooleanModelField("monthTRA", "月月赚转账红包", false);
    private final BooleanModelField scholarship = new BooleanModelField("scholarship", "奖学金", false);
    @Override
    public ModelFields getFields() {
        ModelFields modelFields = new ModelFields();
        modelFields.addField(startTime);
        modelFields.addField(executeInterval);
        modelFields.addField(memberTaskNew);
        modelFields.addField(expressTask );
        modelFields.addField(gameCenter);
        modelFields.addField(monthTRA);//月月赚
//        modelFields.addField(payAwardProd);//支付赚红包
        modelFields.addField(scholarship);//奖学金
        return modelFields;
    }

    @Override
    public Boolean check() {
        if (startTime.getValue().equals("-1")){
            return false;
        }
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
    public ModelGroup getGroup() {
        return ModelGroup.OTHER2;
    }

    @Override
    public String getIcon() {
        return "AntSports.png";
    }
    @Override
    public Boolean isSync() {
        return true;
    }

    @Override
    public void run() {
        //会员任务
        try {
            if (memberTaskNew.getValue()) {
               new MemberNew().run(this.executeIntervalInt);
            }
        } catch (JSONException e) {
            Log.error(TAG + "会员任务New--error:" + e);
        }

        //快递积分
        if (expressTask.getValue()) {
            new KuaiDiFuLiJia().handle(this.executeIntervalInt);
        }

        //游戏中心
        try {
            if (gameCenter.getValue()) {
                if (!Status.hasFlagToday(CompletedKeyEnum.GameCenterTask.name())){
                    new GameCenter().run(this.executeIntervalInt);
                }
            }
        } catch (JSONException e) {
            Log.error(TAG + "游戏中心--error:" + e);
        }
        // 月月赚
        try {
            if (this.monthTRA.getValue()) {
                if(!Status.hasFlagToday(CompletedKeyEnum.MonthTask.name())) {
                    new MonthTra().run(this.executeIntervalInt);
                }
            }
        } catch (Exception e) {
            Log.error(TAG + "月月赚--error:" + e);
        }
//        // 支付赚红包
//        try {
//            if (this.payAwardProd.getValue()) {
//                if(!Status.hasFlagToday(CompletedKeyEnum.PayAwardProd.name())) {
//                    new PayAwardProd().run(this.executeIntervalInt);
//                }
//            }
//        } catch (Exception e) {
//            Log.error(TAG + "支付赚红包--error:" + e);
//        }
        // 奖学金
        try {
            if (this.scholarship.getValue()) {
                if(!Status.hasFlagToday("ScholarshipTask")) {
                    new Scholarship().run(this.executeIntervalInt);
                }
            }
        } catch (Exception e) {
            Log.error(TAG + "奖学金--error:" + e);
        }
    }


}
