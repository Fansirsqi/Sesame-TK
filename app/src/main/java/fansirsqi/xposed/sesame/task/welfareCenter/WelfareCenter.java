package fansirsqi.xposed.sesame.task.welfareCenter;

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

import org.json.JSONArray;
import org.json.JSONObject;

import fansirsqi.xposed.sesame.data.Status;
import fansirsqi.xposed.sesame.model.BaseModel;
import fansirsqi.xposed.sesame.model.ModelFields;
import fansirsqi.xposed.sesame.model.ModelGroup;
import fansirsqi.xposed.sesame.model.modelFieldExt.BooleanModelField;
import fansirsqi.xposed.sesame.model.modelFieldExt.IntegerModelField;
import fansirsqi.xposed.sesame.task.ModelTask;
import fansirsqi.xposed.sesame.task.TaskCommon;
import fansirsqi.xposed.sesame.task.otherTask.CompletedKeyEnum;
import fansirsqi.xposed.sesame.util.JsonUtil;
import fansirsqi.xposed.sesame.util.Log;
import fansirsqi.xposed.sesame.util.TimeUtil;

public class WelfareCenter extends ModelTask {
    private static final String TAG = "WelfareCenter";
    private static final String displayName = "网商银行🏦";
    private static final int DEFAULT_INTERVAL = 3000;

    // 使用原子操作保证线程安全
    private final AtomicInteger executeIntervalInt = new AtomicInteger(DEFAULT_INTERVAL);

    // 配置字段
    private final BooleanModelField assignDateExpirePoint;
    private final IntegerModelField executeInterval = new IntegerModelField("executeInterval", "执行间隔(毫秒)", executeIntervalInt.get());
    private final BooleanModelField welfareCenterProfit;
    private final BooleanModelField welfareCenterTask;
    private final BooleanModelField welfareCenterWSLuckDraw;
    private final BooleanModelField welfareCenterWSTask;
    private final BooleanModelField wenLiBao;

    public WelfareCenter() {
        this.welfareCenterProfit = new BooleanModelField("welfareCenterProfit", "福利金领奖", false);
        this.welfareCenterTask = new BooleanModelField("welfareCenterTask", "福利金任务", false);
        this.welfareCenterWSTask = new BooleanModelField("welfareCenterWSTask", "网商银行任务", false);
        this.welfareCenterWSLuckDraw = new BooleanModelField("welfareCenterWSLuckDraw", "网商银行抽奖", false);
        this.assignDateExpirePoint = new BooleanModelField("assignDateExpirePoint", "快过期抽奖", false);
        this.wenLiBao = new BooleanModelField("wenLiBao", "稳利宝", false);
    }

    private void executeWithDelay(Runnable task) {
        try {
            task.run();
        } catch (Exception e) {
            Log.printStackTrace(TAG, e);
        } finally {
            TimeUtil.sleep(executeIntervalInt.get());
        }
    }

    private void assignDateExpirePoint() {
        if (!((Boolean) this.assignDateExpirePoint.getValue()).booleanValue()) {
            return;
        }

        executeWithDelay(() -> {
            try {
                String nextMonthFirstDay = TimeUtil.getNextMonthFirstDay();
                JSONObject response = new JSONObject(WelfareCenterRpcCall.pointBanlance(nextMonthFirstDay));

                if (!response.getBoolean("success")) {
                    return;
                }

                JSONObject result = response.getJSONObject("result");
                JSONObject expirePoint = result.optJSONObject("assignDateExpirePoint");

                if (expirePoint == null) {
                    return;
                }

                Log.other(new StringBuilder()
                        .append("网商银行🏦总福利金[")
                        .append(result.optInt("pointBalance"))
                        .append("]今年快过期[")
                        .append(result.optInt("currentYearExpirePoint"))
                        .append("]过期[")
                        .append(expirePoint)
                        .append("]").toString());

                int optInt = expirePoint.optInt(nextMonthFirstDay.replace("-", ""));
                if (optInt == 0) {
                    return;
                }

                optInt /= 300;
                String extParams = "{\"bkPointUseMemo\": \"抽奖消耗\",\"pcbfcCertMemo\": \"FULICenterUSE\"}";

                for (int i = 0; i <= optInt; i++) {
                    JSONObject drawResponse = new JSONObject(WelfareCenterRpcCall.campTrigger("CP15205657", extParams));

                    if (drawResponse.getBoolean("success")) {
                        Log.other("网商银行🏦抽奖获得[" +
                                JsonUtil.getValueByPath(drawResponse, "result.prizes.[0].prizeName") + "]");
                        TimeUtil.sleep((long) executeIntervalInt.get());
                    }
                }

            } catch (Exception e) {
                Log.error(TAG + ".assignDateExpirePoint error: ", String.valueOf(e));
            }
        });
    }

    private void batchUseVirtualProfit() {
        executeWithDelay(() -> {
            try {
                String sceneCode = "PLAY102815727";
                JSONObject response = new JSONObject(WelfareCenterRpcCall.queryEnableVirtualProfitV2(sceneCode));

                if (!response.getBoolean("success")) {
                    Log.error(TAG + ".batchUseVirtualProfit err " + response.optString("resultDesc"));
                    return;
                }

                JSONArray profitList = response.getJSONObject("result").getJSONArray("virtualProfitList");

                for (int i = 0; i < profitList.length(); i++) {
                    JSONObject item = profitList.getJSONObject(i);

                    if ("signin".equals(item.getString("type"))) {
                        signIn(sceneCode);
                    } else {
                        JSONArray ids = item.optJSONArray("virtualProfitIds");
                        if (ids != null && ids.length() > 0) {
                            JSONObject useResponse = new JSONObject(WelfareCenterRpcCall.batchUseVirtualProfit(ids));

                            if (useResponse.getBoolean("success")) {
                                Log.other(String.format("网商银行🏦福利金[%s]%s×%d",
                                        item.getString("sceneDesc"),
                                        item.getString("reward"),
                                        ids.length()));
                            } else {
                                Log.error(TAG + ".batchUseVirtualProfit err " + useResponse.optString("resultDesc"));
                            }
                        }
                    }
                }

            } catch (Exception e) {
                Log.error(TAG + ".batchUseVirtualProfit error: ", String.valueOf(e));
            }
        });
    }

    private void playTrigger() {
        executeWithDelay(() -> {
            try {
                JSONObject response = new JSONObject(WelfareCenterRpcCall.queryCert(new String[]{"CT02048186", "CT32675397"}));

                if (!response.getBoolean("success")) {
                    Log.error(TAG + ".playTrigger err " + response.optString("resultDesc"));
                    return;
                }

                JSONObject cert = (JSONObject) JsonUtil.getValueByPathObject(response, "result.cert");
                if (cert == null) {
                    return;
                }

                Iterator<String> keys = cert.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    int count = cert.getInt(key);

                    for (int i = 0; i < count; i++) {
                        String triggerResponse = WelfareCenterRpcCall.playTrigger("PLAY100576638");
                        TimeUtil.sleep(500);

                        JSONObject result = new JSONObject(triggerResponse);
                        if (result.getBoolean("success")) {
                            JSONArray prizes = (JSONArray) JsonUtil.getValueByPathObject(result, "result.extInfo.result.sendResult.prizeSendOrderList");

                            if (prizes != null) {
                                for (int j = 0; j < prizes.length(); j++) {
                                    JSONObject prize = prizes.getJSONObject(j);
                                    Log.other("网商银行🏦获得[" + prize.getString("prizeName") + "]");
                                }
                            }
                        } else {
                            Log.error(TAG + ".playTrigger err " + result.optString("resultDesc"));
                        }
                    }
                }

            } catch (Exception e) {
                Log.error(TAG + ".playTrigger error: ", String.valueOf(e));
            }
        });
    }

    private void signIn(String sceneCode) {
        executeWithDelay(() -> {
            try {
                JSONObject response = new JSONObject(WelfareCenterRpcCall.signInTrigger(sceneCode));

                if (response.getBoolean("success")) {
                    Log.other(String.format("网商银行🏦福利金[签到成功]%s",
                            JsonUtil.getValueByPath(response, "result.prizeOrderDTOList.[0].price")));
                } else {
                    Log.error(TAG + ".signIn err: " + response.optString("resultDesc"));
                }

            } catch (Exception e) {
                Log.error(TAG + ".signIn error: ", String.valueOf(e));
            }
        });
    }

    private void signinPlay() {
        try {
            if (!Status.hasFlagToday(CompletedKeyEnum.WelfareCenterSigninPlay.name())) {
                JSONObject response = new JSONObject(WelfareCenterRpcCall.signinPlay());

                if (response.getBoolean("success")) {
                    Log.other(String.format("网商银行🏦签到[%s]",
                            JsonUtil.getValueByPath(response, "result.todaySignInfo.signPrizeSentPoint.point")));
                    Status.setFlagToday(CompletedKeyEnum.WelfareCenterSigninPlay.name());
                } else {
                    Log.error(TAG + ".signinPlay err: " + response.optString("resultDesc"));
                }
            }
        } catch (Exception e) {
            Log.error(TAG + ".signinPlay exception: ", String.valueOf(e));
        }
    }

    @Override
    public void run() {
        if(Status.hasFlagToday(CompletedKeyEnum.WelfareCenterTask.name())) {
            return;
        }
        // 合并配置更新逻辑
        int intervalValue = Math.max(
                ((Integer) this.executeInterval.getValue()).intValue(),
                executeIntervalInt.get());
        executeIntervalInt.set(intervalValue);

        // 顺序执行各任务模块
        if (((Boolean) this.welfareCenterTask.getValue()).booleanValue()) {
            WelfareCenterRpcCall.doTask("AP1269301", TAG, "网商银行🏦福利金");
        }

        if (((Boolean) this.welfareCenterWSTask.getValue()).booleanValue()) {
            WelfareCenterRpcCall.doTask("AP12202921", TAG, displayName);
        }

        if (((Boolean) this.welfareCenterWSLuckDraw.getValue()).booleanValue()) {
            playTrigger();
        }

        if (((Boolean) this.welfareCenterProfit.getValue()).booleanValue()) {
            batchUseVirtualProfit();
            signinPlay();
        }
        if (((Boolean) this.wenLiBao.getValue()).booleanValue()){
            new WenLiBao().handle();
        }
        assignDateExpirePoint();
        Status.setFlagToday(CompletedKeyEnum.WelfareCenterTask.name());
    }

    @Override
    public ModelFields getFields() {
        ModelFields modelFields = new ModelFields();
        modelFields.addField(this.executeInterval);
        modelFields.addField(this.welfareCenterProfit);
        modelFields.addField(this.welfareCenterTask);
        modelFields.addField(this.welfareCenterWSTask);
        modelFields.addField(this.welfareCenterWSLuckDraw);
        modelFields.addField(this.assignDateExpirePoint);
        modelFields.addField(this.wenLiBao);
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
    public ModelGroup getGroup() {
        return ModelGroup.OTHER;
    }

    @Override
    public String getIcon() {
        return "";
    }

    @Override
    public String getName() {
        return "网商银行";
    }
}
