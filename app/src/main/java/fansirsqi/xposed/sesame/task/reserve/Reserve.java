package fansirsqi.xposed.sesame.task.reserve;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import fansirsqi.xposed.sesame.entity.ReserveEntity;
import fansirsqi.xposed.sesame.model.BaseModel;
import fansirsqi.xposed.sesame.model.ModelFields;
import fansirsqi.xposed.sesame.model.ModelGroup;
import fansirsqi.xposed.sesame.model.modelFieldExt.SelectAndCountModelField;
import fansirsqi.xposed.sesame.task.ModelTask;
import fansirsqi.xposed.sesame.task.TaskCommon;
import fansirsqi.xposed.sesame.util.GlobalThreadPools;
import fansirsqi.xposed.sesame.util.Log;
import fansirsqi.xposed.sesame.util.maps.IdMapManager;
import fansirsqi.xposed.sesame.util.maps.ReserveaMap;
import fansirsqi.xposed.sesame.util.RandomUtil;
import fansirsqi.xposed.sesame.util.ResChecker;
import fansirsqi.xposed.sesame.data.Status;

public class Reserve extends ModelTask {
    private static final String TAG = Reserve.class.getSimpleName();

    @Override
    public String getName() {
        return "保护地";
    }

    @Override
    public ModelGroup getGroup() {
        return ModelGroup.FOREST;
    }

    @Override
    public String getIcon() {
        return "Reserve.png";
    }

    private SelectAndCountModelField reserveList;

    @Override
    public ModelFields getFields() {
        ModelFields modelFields = new ModelFields();
        modelFields.addField(reserveList = new SelectAndCountModelField("reserveList", "保护地列表", new LinkedHashMap<>(), ReserveEntity::getList));
        return modelFields;
    }

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

    public void run() {
        try {
            Log.record(TAG, "开始保护地任务");
            initReserve();
            animalReserve();
        } catch (Throwable t) {
            Log.runtime(TAG, "start.run err:");
            Log.printStackTrace(TAG, t);
        } finally {
            Log.record(TAG, "保护地任务");
        }
    }

    /**
     * 初始化保护地任务。通过 ReserveRpc 接口查询可兑换的树项目，将符合条件的保护地任务存入 ReserveIdMapUtil。 条件：项目类型为 "RESERVE" 且状态为 "AVAILABLE"。若调用失败则加载备份的 ReserveIdMapUtil。
     */
    public static void initReserve() {
        try {
            String response = ReserveRpcCall.queryTreeItemsForExchange();
            JSONObject jsonResponse = new JSONObject(response);
            if (ResChecker.checkRes(TAG, jsonResponse)) {
                JSONArray treeItems = jsonResponse.optJSONArray("treeItems");
                if (treeItems != null) {
                    for (int i = 0; i < treeItems.length(); i++) {
                        JSONObject item = treeItems.getJSONObject(i);
                        // 跳过未定义 projectType 字段的项目
                        if (!item.has("projectType")) {
                            continue;
                        }
                        // 过滤出 projectType 为 "RESERVE" 且 applyAction 为 "AVAILABLE" 的项目
                        if ("RESERVE".equals(item.getString("projectType")) && "AVAILABLE".equals(item.getString("applyAction"))) {
                            // 将符合条件的项目添加到 ReserveIdMapUtil
                            String itemId = item.getString("itemId");
                            String itemName = item.getString("itemName");
                            int energy = item.getInt("energy");
                            IdMapManager.getInstance(ReserveaMap.class).add(itemId, itemName + "(" + energy + "g)");
                        }
                    }
                    Log.runtime(TAG, "初始化保护地任务成功。");
                }
                // 将筛选结果保存到 ReserveIdMapUtil
                IdMapManager.getInstance(ReserveaMap.class).save();
            } else {
                // 若 resultCode 不为 SUCCESS，记录错误描述
                Log.runtime(jsonResponse.optString("resultDesc", "未知错误"));
            }
        } catch (JSONException e) {
            // 捕获 JSON 解析错误并记录日志
            Log.runtime(TAG, "JSON 解析错误：" + e.getMessage());
            Log.printStackTrace(e);
            IdMapManager.getInstance(ReserveaMap.class).load(); // 若出现异常则加载保存的 ReserveIdMapUtil 备份
        } catch (Exception e) {
            // 捕获所有其他异常并记录
            Log.runtime(TAG, "初始化保护地任务时出错：" + e.getMessage());
            Log.printStackTrace(e);
            IdMapManager.getInstance(ReserveaMap.class).load(); // 加载备份的 ReserveIdMapUtil
        }
    }

    private void animalReserve() {
        try {
            Log.record(TAG, "开始执行-" + getName());
            String s = ReserveRpcCall.queryTreeItemsForExchange();
            if (s == null) {
                GlobalThreadPools.sleep(RandomUtil.delay());
                s = ReserveRpcCall.queryTreeItemsForExchange();
            }
            JSONObject jo = new JSONObject(s);
            if (ResChecker.checkRes(TAG,jo)) {
                JSONArray ja = jo.getJSONArray("treeItems");
                for (int i = 0; i < ja.length(); i++) {
                    jo = ja.getJSONObject(i);
                    if (!jo.has("projectType")) {
                        continue;
                    }
                    if (!"RESERVE".equals(jo.getString("projectType"))) {
                        continue;
                    }
                    if (!"AVAILABLE".equals(jo.getString("applyAction"))) {
                        continue;
                    }
                    String projectId = jo.getString("itemId");
                    String itemName = jo.getString("itemName");
                    Map<String, Integer> map = reserveList.getValue();
                    for (Map.Entry<String, Integer> entry : map.entrySet()) {
                        if (Objects.equals(entry.getKey(), projectId)) {
                            Integer count = entry.getValue();
                            if (count != null && count > 0 && Status.canReserveToday(projectId, count)) {
                                exchangeTree(projectId, itemName, count);
                            }
                            break;
                        }
                    }
                }
            } else {
                Log.runtime(TAG, jo.getString("resultDesc"));
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "animalReserve err:");
            Log.printStackTrace(TAG, t);
        } finally {
            Log.record(TAG, "结束执行-" + getName());
        }
    }

    private boolean queryTreeForExchange(String projectId) {
        try {
            String s = ReserveRpcCall.queryTreeForExchange(projectId);
            JSONObject jo = new JSONObject(s);
            if (ResChecker.checkRes(TAG,jo)) {
                String applyAction = jo.getString("applyAction");
                int currentEnergy = jo.getInt("currentEnergy");
                jo = jo.getJSONObject("exchangeableTree");
                if ("AVAILABLE".equals(applyAction)) {
                    if (currentEnergy >= jo.getInt("energy")) {
                        return true;
                    } else {
                        Log.forest("领保护地🏕️[" + jo.getString("projectName") + "]#能量不足停止申请");
                        return false;
                    }
                } else {
                    Log.forest("领保护地🏕️[" + jo.getString("projectName") + "]#似乎没有了");
                    return false;
                }
            } else {
                Log.record(jo.getString("resultDesc"));
                Log.runtime(s);
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "queryTreeForExchange err:");
            Log.printStackTrace(TAG, t);
        }
        return false;
    }

    private void exchangeTree(String projectId, String itemName, int count) {
        int appliedTimes = 0;
        try {
            String s;
            JSONObject jo;
            boolean canApply = queryTreeForExchange(projectId);
            if (!canApply)
                return;
            for (int applyCount = 1; applyCount <= count; applyCount++) {
                s = ReserveRpcCall.exchangeTree(projectId);
                jo = new JSONObject(s);
                if (ResChecker.checkRes(TAG,jo)) {
                    int vitalityAmount = jo.optInt("vitalityAmount", 0);
                    appliedTimes = Status.getReserveTimes(projectId) + 1;
                    String str = "领保护地🏕️[" + itemName + "]#第" + appliedTimes + "次"
                            + (vitalityAmount > 0 ? "-活力值+" + vitalityAmount : "");
                    Log.forest(str);
                    Status.reserveToday(projectId, 1);
                } else {
                    Log.record(jo.getString("resultDesc"));
                    Log.runtime(jo.toString());
                    Log.forest("领保护地🏕️[" + itemName + "]#发生未知错误，停止申请");
                    // Statistics.reserveToday(projectId, count);
                    break;
                }
                GlobalThreadPools.sleep(300);
                canApply = queryTreeForExchange(projectId);
                if (!canApply) {
                    // Statistics.reserveToday(projectId, count);
                    break;
                } else {
                    GlobalThreadPools.sleep(300);
                }
                if (!Status.canReserveToday(projectId, count))
                    break;
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "exchangeTree err:");
            Log.printStackTrace(TAG, t);
        }
    }
}
