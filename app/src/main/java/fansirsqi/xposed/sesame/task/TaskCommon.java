package fansirsqi.xposed.sesame.task;
import fansirsqi.xposed.sesame.model.BaseModel;
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
        long currentTimeMillis = System.currentTimeMillis();
        IS_MODULE_SLEEP_TIME = TimeUtil.checkInTimeRange(currentTimeMillis, BaseModel.getModelSleepTime().getValue());
        IS_ENERGY_TIME = TimeUtil.checkInTimeRange(currentTimeMillis, BaseModel.getEnergyTime().getValue());
        IS_AFTER_8AM = TimeUtil.isAfterOrCompareTimeStr(currentTimeMillis, "0800");
    }
}
