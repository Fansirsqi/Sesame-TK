package fansirsqi.xposed.sesame.task.antCooperate;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.Objects;

import fansirsqi.xposed.sesame.entity.CooperateEntity;
import fansirsqi.xposed.sesame.model.BaseModel;
import fansirsqi.xposed.sesame.model.ModelFields;
import fansirsqi.xposed.sesame.model.ModelGroup;
import fansirsqi.xposed.sesame.model.modelFieldExt.BooleanModelField;
import fansirsqi.xposed.sesame.model.modelFieldExt.SelectAndCountModelField;
import fansirsqi.xposed.sesame.task.ModelTask;
import fansirsqi.xposed.sesame.task.TaskCommon;
import fansirsqi.xposed.sesame.util.GlobalThreadPools;
import fansirsqi.xposed.sesame.util.Log;
import fansirsqi.xposed.sesame.util.maps.CooperateMap;
import fansirsqi.xposed.sesame.util.maps.UserMap;
import fansirsqi.xposed.sesame.util.ResChecker;
import fansirsqi.xposed.sesame.data.Status;
import fansirsqi.xposed.sesame.util.TimeUtil;

public class AntCooperate extends ModelTask {
    private static final String TAG = AntCooperate.class.getSimpleName();
    private static int num;
    private static int limitNum;

    @Override
    public String getName() {
        return "合种";
    }

    @Override
    public ModelGroup getGroup() {
        return ModelGroup.FOREST;
    }

    @Override
    public String getIcon() {
        return "AntCooperate.png";
    }

    private final BooleanModelField cooperateWater = new BooleanModelField("cooperateWater", "合种浇水|开启", false);
    private final SelectAndCountModelField cooperateWaterList = new SelectAndCountModelField("cooperateWaterList", "合种浇水列表", new LinkedHashMap<>(), CooperateEntity.Companion.getList(), "开启合种浇水后执行一次重载");
    private final SelectAndCountModelField cooperateWaterTotalLimitList = new SelectAndCountModelField("cooperateWaterTotalLimitList", "浇水总量限制列表", new LinkedHashMap<>(), CooperateEntity.Companion.getList());
    private final BooleanModelField cooperateSendCooperateBeckon = new BooleanModelField("cooperateSendCooperateBeckon", "合种 | 召唤队友浇水| 仅队长 ", false);

    @Override
    public ModelFields getFields() {
        ModelFields modelFields = new ModelFields();
        modelFields.addField(cooperateWater);
        modelFields.addField(cooperateWaterList);
        modelFields.addField(cooperateWaterTotalLimitList);
        modelFields.addField(cooperateSendCooperateBeckon);
        return modelFields;
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
            Log.record(TAG, "执行开始-" + getName());
            if (cooperateWater.getValue()) {
                String s = AntCooperateRpcCall.queryUserCooperatePlantList();
                JSONObject jo = new JSONObject(s);
                if (ResChecker.checkRes(TAG,jo)) {
                    Log.runtime(TAG, "获取合种列表成功");
                    int userCurrentEnergy = jo.getInt("userCurrentEnergy");
                    JSONArray ja = jo.getJSONArray("cooperatePlants");
                    for (int i = 0; i < ja.length(); i++) {
                        jo = ja.getJSONObject(i);
                        String cooperationId = jo.getString("cooperationId");
                        if (!jo.has("name")) {
                            s = AntCooperateRpcCall.queryCooperatePlant(cooperationId);
                            jo = new JSONObject(s).getJSONObject("cooperatePlant");
                        }
                        String admin = jo.getString("admin");
                        String name = jo.getString("name");
                        if (cooperateSendCooperateBeckon.getValue() && Objects.equals(UserMap.getCurrentUid(), admin)) {
                            cooperateSendCooperateBeckon(cooperationId, name);
                        }
                        int waterDayLimit = jo.getInt("waterDayLimit");
                        Log.runtime(TAG, "合种[" + name + "]: 日限额:" + waterDayLimit);
                        CooperateMap.getInstance(CooperateMap.class).add(cooperationId, name);
                        if (!Status.canCooperateWaterToday(UserMap.getCurrentUid(), cooperationId)) {
                            Log.runtime(TAG, "[" + name + "]今日已浇水💦");
                            continue;
                        }
                        Integer waterId = cooperateWaterList.getValue().get(cooperationId);
                        if (waterId != null) {
                            Integer limitNum = cooperateWaterTotalLimitList.getValue().get(cooperationId);
                            if (limitNum != null) {
                                int cumulativeWaterAmount = calculatedWaterNum(cooperationId);
                                if (cumulativeWaterAmount < 0) {
                                    Log.runtime(TAG, "当前用户[" + UserMap.getCurrentUid() + "]的累计浇水能量获取失败,跳过本次浇水！");
                                    continue;
                                }
                                waterId = limitNum - cumulativeWaterAmount;
                                Log.runtime(TAG, "[" + name + "] 调整后的浇水数量: " + waterId);
                            }
                            if (waterId > waterDayLimit) {
                                waterId = waterDayLimit;
                            }
                            if (waterId > userCurrentEnergy) {
                                waterId = userCurrentEnergy;
                            }
                            if (waterId > 0) {
                                cooperateWater(cooperationId, waterId, name);
                            } else {
                                Log.runtime(TAG, "浇水数量为0，跳过[" + name + "]");
                            }
                        } else {
                            Log.runtime(TAG, "浇水列表中没有为[" + name + "]配置");
                        }
                    }
                } else {
                    Log.error(TAG, "获取合种列表失败:");
                    Log.runtime(TAG + "获取合种列表失败:", jo.getString("resultDesc"));
                }
            } else {
                Log.runtime(TAG, "合种浇水功能未开启");
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "start.run err:");
            Log.printStackTrace(TAG, t);
        } finally {
            CooperateMap.getInstance(CooperateMap.class).save(UserMap.getCurrentUid());
            Log.record(TAG, "执行结束-" + getName());
        }
    }

    private static void cooperateWater(String coopId, int count, String name) {
        try {
            String s = AntCooperateRpcCall.cooperateWater(UserMap.getCurrentUid(), coopId, count);
            JSONObject jo = new JSONObject(s);
            if (ResChecker.checkRes(TAG,jo)) {
                Log.forest("合种浇水🚿[" + name + "]" + jo.getString("barrageText"));
                Status.cooperateWaterToday(UserMap.getCurrentUid(), coopId);
            } else {
                Log.runtime(TAG, "浇水失败[" + name + "]: " + jo.getString("resultDesc"));
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "cooperateWater err:");
            Log.printStackTrace(TAG, t);
        } finally {
            GlobalThreadPools.sleep(1500);
        }
    }

    private static int calculatedWaterNum(String coopId) {
        try {
            String s = AntCooperateRpcCall.queryCooperateRank("A", coopId);
            JSONObject jo = new JSONObject(s);
            if (jo.optBoolean("success", false)) {
                JSONArray jaList = jo.getJSONArray("cooperateRankInfos");
                for (int i = 0; i < jaList.length(); i++) {
                    JSONObject joItem = jaList.getJSONObject(i);
                    String userId = joItem.getString("userId");
                    if (userId.equals(UserMap.getCurrentUid())) {
                        // 未获取到累计浇水量 返回 -1 不执行浇水
                        int energySummation = joItem.optInt("energySummation", -1);
                        if (energySummation >= 0) {
                            Log.runtime(TAG, "当前用户[" + userId + "]的累计浇水能量: " + energySummation);
                        }
                        return energySummation;
                    }
                }
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "calculatedWaterNum err:");
            Log.printStackTrace(TAG, t);
        }
        return -1; // 未获取到累计浇水量，停止浇水
    }

    private static void cooperateSendCooperateBeckon(String cooperationId, String name) {
        try {
            if (TimeUtil.isNowBeforeTimeStr("1800")) {
                return;
            }
            TimeUtil.sleep(500);
            JSONObject jo = new JSONObject(AntCooperateRpcCall.queryCooperateRank("D", cooperationId));
            if (ResChecker.checkRes(TAG, jo)) {
                JSONArray cooperateRankInfos = jo.getJSONArray("cooperateRankInfos");
                for (int i = 0; i < cooperateRankInfos.length(); i++) {
                    JSONObject rankInfo = cooperateRankInfos.getJSONObject(i);
                    if (rankInfo.getBoolean("canBeckon")) {
                        jo = new JSONObject(AntCooperateRpcCall.sendCooperateBeckon(rankInfo.getString("userId"), cooperationId));
                        if (ResChecker.checkRes(TAG,jo)) {
                            Log.forest("合种🚿[" + name + "]#召唤队友[" + rankInfo.getString("displayName") + "]成功");
                        }
                        TimeUtil.sleep(1000);
                    }
                }
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "cooperateSendCooperateBeckon err:");
            Log.printStackTrace(TAG, t);
        }
    }
}
