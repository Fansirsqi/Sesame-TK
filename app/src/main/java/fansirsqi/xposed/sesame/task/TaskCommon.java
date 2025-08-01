package fansirsqi.xposed.sesame.task;

import java.util.List;

import fansirsqi.xposed.sesame.model.BaseModel;
import fansirsqi.xposed.sesame.util.Log;
import fansirsqi.xposed.sesame.util.TimeUtil;

/**
 * 通用任务工具类
 * <p>
 * 提供任务相关的通用功能，包括时间判断和状态更新。
 */
public class TaskCommon {
    public static volatile Boolean IS_ENERGY_TIME = false;
    public static volatile Boolean IS_AFTER_8AM = false;
    public static volatile Boolean IS_MODULE_SLEEP_TIME = false;

    public static void update() {

        Log.runtime("TaskCommon Update:");
        long currentTimeMillis = System.currentTimeMillis();
        List<String> isEnergyTime = BaseModel.getEnergyTime().getValue();
        Log.runtime("获取能量时间配置:" + isEnergyTime);
        if (isConfigDisabled(isEnergyTime)) {
            Log.runtime("只收能量时间配置已关闭");
            IS_ENERGY_TIME = false;
        } else {
            IS_ENERGY_TIME = TimeUtil.checkInTimeRange(currentTimeMillis, isEnergyTime);
        }

        List<String> isModuleSleepTime = BaseModel.getModelSleepTime().getValue();
        Log.runtime("获取模块休眠配置:" + isModuleSleepTime);
        if (isConfigDisabled(isModuleSleepTime)) {
            Log.runtime("休眠配置已关闭");
            IS_MODULE_SLEEP_TIME = false;
        } else {
            IS_MODULE_SLEEP_TIME = TimeUtil.checkInTimeRange(currentTimeMillis, isModuleSleepTime);
        }

        IS_AFTER_8AM = TimeUtil.isAfterOrCompareTimeStr(currentTimeMillis, "0800");
    }

    /**
     * 判断当前配置是否表示“关闭”
     *
     * @param config 输入的字符串列表
     * @return true 表示关闭
     */
    public static boolean isConfigDisabled(List<String> config) {
        if (config == null || config.isEmpty()) return true;

        String first = config.get(0).trim();

        return "-1".equals(first);
    }
}
