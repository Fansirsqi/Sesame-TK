package fansirsqi.xposed.sesame.task.consumeGold;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;
import fansirsqi.xposed.sesame.data.RuntimeInfo;
import fansirsqi.xposed.sesame.model.BaseModel;
import fansirsqi.xposed.sesame.model.ModelFields;
import fansirsqi.xposed.sesame.model.ModelGroup;
import fansirsqi.xposed.sesame.model.modelFieldExt.BooleanModelField;
import fansirsqi.xposed.sesame.model.modelFieldExt.IntegerModelField;
import fansirsqi.xposed.sesame.task.ModelTask;
import fansirsqi.xposed.sesame.task.TaskCommon;
import fansirsqi.xposed.sesame.util.Log;
import fansirsqi.xposed.sesame.util.ThreadUtil;
import fansirsqi.xposed.sesame.util.TimeUtil;
public class ConsumeGold extends ModelTask {
    private static final String TAG = ConsumeGold.class.getSimpleName();
    @Override
    public String getName() {
        return "消费金";
    }
    @Override
    public ModelGroup getGroup() {
        return ModelGroup.OTHER;
    }
    @Override
    public String getIcon() {
        return "ConsumeGold.svg";
    }
    private IntegerModelField lastExecutionInterval;
    private BooleanModelField consumeGoldSign;
    private BooleanModelField consumeGoldAward;
    private BooleanModelField consumeGoldGainRepair;
    private BooleanModelField consumeGoldRepairSign;
    private IntegerModelField consumeGoldRepairSignUseLimit;
    private BooleanModelField consumeGoldGainTask;
    private IntegerModelField eachTaskDelay;
    private IntegerModelField watchAdDelay;
    @Override
    public ModelFields getFields() {
        ModelFields modelFields = new ModelFields();
        modelFields.addField(lastExecutionInterval = new IntegerModelField("lastExecutionInterval", "距上次执行间隔不小于（毫秒，默认6小时）", 21600000, 0, 86400000));
        modelFields.addField(consumeGoldSign = new BooleanModelField("consumeGoldSign", "签到", false));
        modelFields.addField(consumeGoldAward = new BooleanModelField("consumeGoldAward", "抽奖（每日免费三次）", false));
        modelFields.addField(consumeGoldGainRepair = new BooleanModelField("consumeGoldGainRepair", "领取补签卡", false));
        modelFields.addField(consumeGoldRepairSign = new BooleanModelField("consumeGoldRepairSign", "使用补签卡", false));
        modelFields.addField(consumeGoldRepairSignUseLimit = new IntegerModelField("consumeGoldRepairSignUseLimit", "补签卡每日使用次数（当日过期）", 1, 1, 10));
        modelFields.addField(consumeGoldGainTask = new BooleanModelField("consumeGoldGainTask", "完成积分任务", false));
        modelFields.addField(eachTaskDelay = new IntegerModelField("eachTaskDelay", "执行下一项任务的延时（毫秒，默认200）", 200));
        modelFields.addField(watchAdDelay = new IntegerModelField("watchAdDelay", "观看15s广告任务执行延时（毫秒，默认16000）", 16000));
        return modelFields;
    }
    public Boolean check() {
        if (TaskCommon.IS_ENERGY_TIME){
            Log.record("⏸ 当前为只收能量时间【"+ BaseModel.getEnergyTime().getValue() +"】，停止执行" + getName() + "任务！");
            return false;
        }else if (TaskCommon.IS_MODULE_SLEEP_TIME) {
            Log.record("💤 模块休眠时间【"+ BaseModel.getModelSleepTime().getValue() +"】停止执行" + getName() + "任务！");
            return false;
        } else {
            long executeTime = RuntimeInfo.getInstance().getLong("consumeGold", 0);
            return System.currentTimeMillis() - executeTime >= lastExecutionInterval.getValue();
        }

    }
    public void run() {
        try {
            Log.record("执行开始-" + getName());
            RuntimeInfo.getInstance().put("consumeGold", System.currentTimeMillis());
            if (consumeGoldSign.getValue()) {
                consumeGoldSign();
                ThreadUtil.sleep(eachTaskDelay.getValue());
            }
            if (consumeGoldAward.getValue()) {
                consumeGoldAward();
                ThreadUtil.sleep(eachTaskDelay.getValue());
            }
            if (consumeGoldGainRepair.getValue()) {
                consumeGoldGainRepair();
                ThreadUtil.sleep(eachTaskDelay.getValue());
            }
            if (consumeGoldRepairSign.getValue()) {
                consumeGoldRepairSign();
                ThreadUtil.sleep(eachTaskDelay.getValue());
            }
            if (consumeGoldGainTask.getValue()) {
                consumeGoldGainTask();
                ThreadUtil.sleep(eachTaskDelay.getValue());
            }
        } catch (Throwable t) {
            Log.printStackTrace(TAG + ".run", t);
        } finally {
            Log.record("执行结束-" + getName());
        }
    }
    /**
     * 签到
     */
    private void consumeGoldSign() {
        try {
            String s = ConsumeGoldRpcCall.signinCalendar();
            ThreadUtil.sleep(200);
            JSONObject jo = new JSONObject(s);
            if (!jo.optBoolean("success")) {
                Log.other(TAG + ".consumeGoldSign.signinCalendar", "消费金🪙[响应失败]#" + jo.getString("errorMsg"));
                Log.error(TAG + ".consumeGoldSign.signinCalendar", "消费金🪙[响应失败]#" + s);
                return;
            }
            if (jo.optBoolean("isSignInToday")) {
                return;
            }
            s = ConsumeGoldRpcCall.taskV2Index("CG_SIGNIN_AD_FEEDS");
            ThreadUtil.sleep(200);
            jo = new JSONObject(s);
            if (!jo.optBoolean("success")) {
                Log.other(TAG + ".consumeGoldSign.taskV2Index", "消费金🪙[响应失败]#" + jo.getString("errorMsg"));
                Log.error(TAG + ".consumeGoldSign.taskV2Index", "消费金🪙[响应失败]#" + s);
                return;
            }
            JSONArray taskList = jo.getJSONArray("taskList");
            if (taskList.length() == 0) {
                return;
            }
            jo = taskList.getJSONObject(0);
            String taskId = jo.getJSONObject("extInfo").getString("actionBizId");
            s = ConsumeGoldRpcCall.taskV2Trigger(taskId, "CG_SIGNIN_AD_FEEDS", "SIGN_UP");
            ThreadUtil.sleep(200);
            jo = new JSONObject(s);
            if (!jo.optBoolean("success")) {
                Log.other(TAG + ".consumeGoldSign.taskV2Trigger", "消费金🪙[响应失败]#" + jo.getString("errorMsg"));
                Log.error(TAG + ".consumeGoldSign.taskV2Trigger", "消费金🪙[响应失败]#" + s);
                return;
            }
            s = ConsumeGoldRpcCall.taskOpenBoxAward();
            ThreadUtil.sleep(500);
            jo = new JSONObject(s);
            if (!jo.optBoolean("success")) {
                Log.other(TAG + ".consumeGoldSign.taskOpenBoxAward", "消费金🪙[响应失败]#" + jo.getString("errorMsg"));
                Log.error(TAG + ".consumeGoldSign.taskOpenBoxAward", "消费金🪙[响应失败]#" + s);
                return;
            }
            int amount = jo.getInt("amount");
            Log.other("消费金🪙[签到]#获得" + amount);
        } catch (Throwable t) {
            Log.printStackTrace(TAG + ".consumeGoldSign", t);
        }
    }
    /**
     * 抽奖
     */
    private void consumeGoldAward() {
        try {
            String s = ConsumeGoldRpcCall.promoIndex();
            ThreadUtil.sleep(500);
            JSONObject jo = new JSONObject(s);
            if (!jo.optBoolean("success")) {
                Log.other(TAG + ".consumeGoldAward.promoIndex", "消费金🪙[响应失败]#" + jo.getString("errorMsg"));
                Log.error(TAG + ".consumeGoldAward.promoIndex", "消费金🪙[响应失败]#" + s);
                return;
            }
            jo = jo.getJSONObject("homePromoInfoDTO");
            JSONArray homePromoTokenDTOList = jo.getJSONArray("homePromoTokenDTOList");
            int tokenTotalAmount = 0;
            int tokenLeftAmount = 0;
            for (int i = 0; i < homePromoTokenDTOList.length(); i++) {
                jo = homePromoTokenDTOList.getJSONObject(i);
                if ("FREE".equals(jo.getString("tokenType"))) {
                    tokenTotalAmount = jo.getInt("tokenTotalAmount");
                    tokenLeftAmount = jo.getInt("tokenLeftAmount");
                    break;
                }
            }
            if (tokenLeftAmount <= 0) {
                return;
            }
            for (int j = tokenTotalAmount - tokenLeftAmount; j < tokenTotalAmount; j++) {
                s = ConsumeGoldRpcCall.promoTrigger();
                ThreadUtil.sleep(1000);
                jo = new JSONObject(s);
                if (!jo.optBoolean("success")) {
                    Log.other(TAG + ".consumeGoldAward.promoTrigger", "消费金🪙[响应失败]#" + jo.getString("errorMsg"));
                    Log.error(TAG + ".consumeGoldAward.promoTrigger", "消费金🪙[响应失败]#" + s);
                    return;
                }
                jo = jo.getJSONObject("homePromoPrizeInfoDTO");
                int quantity = jo.getInt("quantity");
                Log.other("消费金🪙[抽奖(" + (j + 1) + "/" + tokenTotalAmount + ")]#获得" + quantity);
            }
        } catch (Throwable t) {
            Log.printStackTrace(TAG + ".consumeGoldAward", t);
        }
    }
    /**
     * 领取补签卡
     */
    private void consumeGoldGainRepair() {
        try {
            // task type 1
            String s = ConsumeGoldRpcCall.signinCalendar();
            ThreadUtil.sleep(200);
            JSONObject jo = new JSONObject(s);
            if (!jo.optBoolean("success")) {
                Log.other(TAG + ".consumeGoldGainRepair.signinCalendar", "消费金🪙[响应失败]#" + jo.getString("errorMsg"));
                Log.error(TAG + ".consumeGoldGainRepair.signinCalendar", "消费金🪙[响应失败]#" + s);
                return;
            }
            if (jo.has("taskList")) {
                execTask(jo.getJSONArray("taskList"), "REPAIR_SIGN_TOKEN", "领取补签卡", true, true, true);
            }
            // task type 2
            s = ConsumeGoldRpcCall.taskV2Index("REPAIR_SIGN_XLIGHT");
            jo = new JSONObject(s);
            if (!jo.optBoolean("success")) {
                Log.other(TAG + ".consumeGoldGainRepair.taskV2Index", "消费金🪙[响应失败]#" + jo.getString("errorMsg"));
                Log.error(TAG + ".consumeGoldGainRepair.taskV2Index", "消费金🪙[响应失败]#" + s);
                return;
            }
            if (jo.has("taskList")) {
                execTask(jo.getJSONArray("taskList"), "REPAIR_SIGN_XLIGHT", "领取补签卡", true, true, false);
            }
        } catch (Throwable t) {
            Log.printStackTrace(TAG + ".consumeGoldGainRepair", t);
        }
    }
    /**
     * 使用补签卡
     */
    private void consumeGoldRepairSign() {
        try {
            String currentDate = TimeUtil.getFormatDate();
            if (!currentDate.equals(RuntimeInfo.getInstance().getString("consumeGoldRepairSignDate"))) {
                RuntimeInfo.getInstance().put("consumeGoldRepairSignUsed", 0);
                RuntimeInfo.getInstance().put("consumeGoldRepairSignDate", currentDate);
            }
            long consumeGoldRepairUseLimit = RuntimeInfo.getInstance().getLong("consumeGoldRepairSignUsed", 0);
            String s = ConsumeGoldRpcCall.signinCalendar();
            ThreadUtil.sleep(200);
            JSONObject jo = new JSONObject(s);
            if (!jo.optBoolean("success")) {
                Log.other(TAG + ".consumeGoldRepairSign.signinCalendar", "消费金🪙[响应失败]#" + jo.getString("errorMsg"));
                Log.error(TAG + ".consumeGoldRepairSign.signinCalendar", "消费金🪙[响应失败]#" + s);
                return;
            }
            JSONObject repairSignInInfo = jo.getJSONObject("repairSignInInfo");
            boolean canRepair = repairSignInInfo.optBoolean("repair");
            int repairCardNum = repairSignInInfo.getInt("repairCardTokenNum");
            if (!canRepair || repairCardNum == 0) {
                return;
            }
            JSONArray calendarGroup = jo.getJSONArray("calendarGroup");
            HashMap<String, Boolean> calendarMap = new HashMap<>();
            for (int i = 0; i < calendarGroup.length(); i++) {
                JSONArray tempArray = calendarGroup.getJSONObject(i).getJSONArray("dateList");
                for (int j = 0; j < tempArray.length(); j++) {
                    jo = tempArray.getJSONObject(j);
                    calendarMap.put(jo.getString("date"), (jo.optBoolean("isRepairable") && !jo.optBoolean("isSignIn")));
                }
            }
            ArrayList<String> repairDateList = new ArrayList<>();
            for (int offset = -1; offset >= -calendarMap.size() && repairDateList.size() < repairCardNum && consumeGoldRepairUseLimit < consumeGoldRepairSignUseLimit.getValue(); offset--) {
                String tempTime = TimeUtil.getFormatTime(offset, "yyyy-MM-dd");
                if (!calendarMap.containsKey(tempTime)) {
                    return;
                }
                if (Boolean.TRUE.equals(calendarMap.get(tempTime))) {
                    repairDateList.add(tempTime.replaceAll("-", ""));
                    consumeGoldRepairUseLimit++;
                }
            }
            if (repairDateList.isEmpty()) {
                return;
            }
            consumeGoldRepairUseLimit = RuntimeInfo.getInstance().getLong("consumeGoldRepairSignUsed", 0);
            for (String repairDate : repairDateList) {
                s = ConsumeGoldRpcCall.signinTrigger("check", repairDate);
                ThreadUtil.sleep(500);
                jo = new JSONObject(s);
                if (!jo.optBoolean("success")) {
                    Log.other(TAG + ".consumeGoldRepairSign.signinTrigger.check", "消费金🪙[响应失败]#" + jo.getString("errorMsg"));
                    Log.error(TAG + ".consumeGoldRepairSign.signinTrigger.check", "消费金🪙[响应失败]#" + s);
                    return;
                }
                s = ConsumeGoldRpcCall.signinTrigger("repair", repairDate);
                ThreadUtil.sleep(500);
                jo = new JSONObject(s);
                if (!jo.optBoolean("success")) {
                    Log.other(TAG + ".consumeGoldRepairSign.signinTrigger.repair", "消费金🪙[响应失败]#" + jo.getString("errorMsg"));
                    Log.error(TAG + ".consumeGoldRepairSign.signinTrigger.repair", "消费金🪙[响应失败]#" + s);
                    return;
                }
                Log.other("消费金🪙[补签" + repairDate + "成功]#补签卡剩余" + --repairCardNum + "张");
                RuntimeInfo.getInstance().put("consumeGoldRepairSignUsed", ++consumeGoldRepairUseLimit);
            }
        } catch (Throwable t) {
            Log.printStackTrace(TAG + ".consumeGoldRepairSign", t);
        }
    }
    /**
     * 积分任务
     */
    private void consumeGoldGainTask() {
        try {
            String s = ConsumeGoldRpcCall.taskV2Index("ALL_DAILY_TASK_LIST");
            ThreadUtil.sleep(200);
            JSONObject jo = new JSONObject(s);
            if (!jo.optBoolean("success")) {
                Log.other(TAG + ".consumeGoldGainTask.taskV2Index", "消费金🪙[响应失败]#" + jo.getString("errorMsg"));
                Log.error(TAG + ".consumeGoldGainTask.taskV2Index", "消费金🪙[响应失败]#" + s);
                return;
            }
            if (jo.has("taskList")) {
                execTask(jo.getJSONArray("taskList"), "ALL_DAILY_TASK_LIST", "消费金任务", true, true, true);
            }
        } catch (Throwable t) {
            Log.printStackTrace(TAG + ".consumeGoldGainTask", t);
        }
    }
    /**
     * 执行任务
     *
     * @param taskList    任务列表
     * @param needSignUp  执行sign up操作
     * @param needSend    执行send操作
     * @param needReceive 执行receive操作
     * @throws JSONException JSON格式化异常，上抛处理
     */
    private void execTask(JSONArray taskList, String taskSceneCode, String execType, boolean needSignUp, boolean needSend, boolean needReceive) throws JSONException {
        String s;
        JSONObject jo;
        for (int i = 0; i < taskList.length(); i++) {
            JSONObject task = taskList.getJSONObject(i);
            int amount = 0;
            if (task.has("prizeInfoList")) {
                amount = task.getJSONArray("prizeInfoList").getJSONObject(0).getInt("prizeModulus");
            } else {
                amount = task.getInt("pointNum");
            }
            String type = task.getString("type");
            // only can run with "BROWSER" && "CLICK_DIRECT_FINISH"
            if ("BROWSER".equals(type) || "CLICK_DIRECT_FINISH".equals(type)) {
                continue;
            }
            task = task.getJSONObject("extInfo");
            String taskId = task.getString("actionBizId");
            String title = task.getString("title");
            String status = task.getString("taskStatus");
            switch (status) {
                case "NONE_SIGNUP":
                    if (needSignUp) {
                        ThreadUtil.sleep(200);
                        s = ConsumeGoldRpcCall.taskV2Trigger(taskId, taskSceneCode, "SIGN_UP");
                        jo = new JSONObject(s);
                        if (!jo.optBoolean("success")) {
                            Log.other(TAG + ".execTask.taskV2Trigger.SIGN_UP", "消费金🪙[响应失败]#" + s);
                            continue;
                        }
                    }
                case "SIGNUP_COMPLETE":
                    if (needSend) {
                        ThreadUtil.sleep(watchAdDelay.getValue());
                        s = ConsumeGoldRpcCall.taskV2Trigger(taskId, taskSceneCode, "SEND");
                        jo = new JSONObject(s);
                        if (!jo.optBoolean("success")) {
                            Log.other(TAG + ".execTask.taskV2Trigger.SEND", "消费金🪙[响应失败]#" + s);
                            continue;
                        }
                    }
                case "TO_RECEIVE":
                    if (needReceive) {
                        ThreadUtil.sleep(200);
                        s = ConsumeGoldRpcCall.taskV2Trigger(taskId, taskSceneCode, "RECEIVE");
                        jo = new JSONObject(s);
                        if (!jo.optBoolean("success")) {
                            Log.other(TAG + ".execTask.taskV2Trigger.RECEIVE", "消费金🪙[响应失败]#" + s);
                        }
                    }
                    break;
                case "RECEIVE_SUCCESS":
                    continue;
            }
            Log.other("消费金🪙[" + execType + "(" + title + ")]#获得" + amount);
        }
    }
}
