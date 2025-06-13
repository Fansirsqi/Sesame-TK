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

        // Log.runtime("TaskCommon Update:");
        // long currentTimeMillis = System.currentTimeMillis();
        // List<String> isEnergyTime = BaseModel.getEnergyTime().getValue();
        // if (isEnergyTime.contains("-1")) {
        //     IS_ENERGY_TIME = false;
        // } else {
        //     IS_ENERGY_TIME = TimeUtil.checkInTimeRange(currentTimeMillis, isEnergyTime);
        // }

        // List<String> isModuleSleepTime = BaseModel.getModelSleepTime().getValue();
        // Log.runtime("获取模块休眠配置:" + isModuleSleepTime);
        // if (isModuleSleepTime.contains("-1")) {
        //     Log.runtime("模块休眠配置 -1，不进行休眠");
        //     IS_MODULE_SLEEP_TIME = false;
        // } else {
        //     IS_MODULE_SLEEP_TIME = TimeUtil.checkInTimeRange(currentTimeMillis, isModuleSleepTime);
        // }

        IS_AFTER_8AM = TimeUtil.isAfterOrCompareTimeStr(currentTimeMillis, "0800");
    }
}
