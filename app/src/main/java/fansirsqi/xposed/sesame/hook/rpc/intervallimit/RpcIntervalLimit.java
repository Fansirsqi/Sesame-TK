package fansirsqi.xposed.sesame.hook.rpc.intervallimit;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import fansirsqi.xposed.sesame.util.Log;
import fansirsqi.xposed.sesame.util.ThreadUtil;
/**
 * RpcIntervalLimit类用于管理不同方法的调用间隔限制，确保调用间隔不小于设定值。
 * 提供添加、更新、进入间隔限制以及清除限制的功能。
 */
public class RpcIntervalLimit {
    // 默认的间隔限制设置为50毫秒
    private static final IntervalLimit DEFAULT_INTERVAL_LIMIT = new DefaultIntervalLimit(500);
    // 存储各方法的间隔限制，使用线程安全的ConcurrentHashMap
    private static final Map<String, IntervalLimit> intervalLimitMap = new ConcurrentHashMap<>();
    /**
     * 为指定方法添加间隔限制。
     *
     * @param method   方法名称
     * @param interval 间隔时间（毫秒）
     */
    public static void addIntervalLimit(String method, Integer interval) {
        addIntervalLimit(method, new DefaultIntervalLimit(interval));
    }
    /**
     * 为指定方法添加自定义间隔限制对象。
     *
     * @param method        方法名称
     * @param intervalLimit 自定义的间隔限制对象
     */
    public static void addIntervalLimit(String method, IntervalLimit intervalLimit) {
        {
            synchronized (intervalLimitMap) {
                if (intervalLimitMap.containsKey(method)) {
                    Log.runtime("方法：" + method + " 间隔限制已存在");
                    throw new IllegalArgumentException("方法：" + method + " 间隔限制已存在");
                }
                intervalLimitMap.put(method, intervalLimit);
            }
        }
    }
    /**
     * 更新指定方法的间隔限制。
     *
     * @param method   方法名称
     * @param interval 新的间隔时间（毫秒）
     */
    public static void updateIntervalLimit(String method, Integer interval) {
        updateIntervalLimit(method, new DefaultIntervalLimit(interval));
    }
    /**
     * 更新指定方法的间隔限制对象。
     *
     * @param method        方法名称
     * @param intervalLimit 新的自定义间隔限制对象
     */
    public static void updateIntervalLimit(String method, IntervalLimit intervalLimit) {
        intervalLimitMap.put(method, intervalLimit);
    }
    /**
     * 进入指定方法的间隔限制，确保调用间隔时间不小于设定值。
     *
     * @param method 方法名称
     */
    public static void enterIntervalLimit(String method) {
        IntervalLimit intervalLimit;
        intervalLimit = intervalLimitMap.getOrDefault(method, DEFAULT_INTERVAL_LIMIT);
        synchronized (Objects.requireNonNull(intervalLimit, "间隔限制对象不能为空")) {
            long sleep = intervalLimit.getInterval() - (System.currentTimeMillis() - intervalLimit.getTime());
            if (sleep > 0) {
                ThreadUtil.sleep(sleep);
            }
            intervalLimit.setTime(System.currentTimeMillis());
        }
    }
    /**
     * 清除所有方法的间隔限制。
     */
    public static void clearIntervalLimit() {
        intervalLimitMap.clear();
    }
}
