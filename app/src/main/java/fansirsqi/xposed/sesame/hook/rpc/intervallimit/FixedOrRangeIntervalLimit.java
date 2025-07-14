package fansirsqi.xposed.sesame.hook.rpc.intervallimit;
import lombok.Getter;
import lombok.Setter;
import java.util.concurrent.ThreadLocalRandom;
/**
 * 用于实现一个可变间隔限制器。
 * 支持固定间隔或范围间隔两种模式。
 */
public class FixedOrRangeIntervalLimit implements IntervalLimit {
    /**
     * 是否为固定间隔模式。如果为 true，则表示固定间隔；否则为范围间隔。
     */
    private final Boolean fixedOrRange;
    /**
     * 固定间隔时间（毫秒），仅在固定间隔模式下使用。
     */
    private final Integer fixedInt;
    /**
     * 范围间隔的最小值（毫秒），仅在范围间隔模式下使用。
     */
    private final Integer rangeMin;
    /**
     * 范围间隔的最大值（毫秒，非闭区间），仅在范围间隔模式下使用。
     */
    private final Integer rangeMax;
    /**
     * 上一次调用的时间戳（毫秒）。
     */
    @Getter
    @Setter
    private Long time = 0L;
    /**
     * 构造函数，根据输入字符串决定使用固定间隔还是范围间隔模式。
     *
     * @param fixedOrRangeStr  固定间隔或范围间隔的描述字符串。如果为固定间隔，传入一个数字字符串；
     *                         如果为范围间隔，传入格式为 "min-max" 的字符串。
     * @param min              间隔的最小值（毫秒）。
     * @param max              间隔的最大值（毫秒）。
     */
    public FixedOrRangeIntervalLimit(String fixedOrRangeStr, int min, int max) {
        // 确保 min 不大于 max
        if (min > max) {
            max = min;
        }
        if (fixedOrRangeStr != null && !fixedOrRangeStr.isEmpty()) {
            String[] split = fixedOrRangeStr.split("-");
            if (split.length == 2) {
                // 处理范围间隔模式
                int rangeMinTmp, rangeMaxTmp;
                try {
                    rangeMinTmp = Math.max(Integer.parseInt(split[0]), min);
                } catch (Exception ignored) {
                    rangeMinTmp = min;
                }
                try {
                    rangeMaxTmp = Math.min(Integer.parseInt(split[1]), max);
                } catch (Exception ignored) {
                    rangeMaxTmp = max;
                }
                if (rangeMinTmp > rangeMaxTmp) {
                    rangeMaxTmp = rangeMinTmp;
                }
                fixedInt = null;
                rangeMin = rangeMinTmp;
                rangeMax = rangeMaxTmp + 1; // 调整为非闭区间
                fixedOrRange = false;
            } else {
                // 处理固定间隔模式
                int fixedIntTmp;
                try {
                    fixedIntTmp = Integer.parseInt(fixedOrRangeStr);
                } catch (Exception ignored) {
                    fixedIntTmp = max;
                }
                fixedIntTmp = Math.max(fixedIntTmp, min);
                fixedIntTmp = Math.min(fixedIntTmp, max);
                fixedInt = fixedIntTmp;
                rangeMin = null;
                rangeMax = null;
                fixedOrRange = true;
            }
        } else {
            // 默认使用固定间隔模式，值为最大值
            fixedInt = max;
            rangeMin = null;
            rangeMax = null;
            fixedOrRange = true;
        }
    }
    /**
     * 获取当前间隔时间。
     *
     * @return 如果为固定间隔模式，返回固定间隔时间；
     *         如果为范围间隔模式，返回范围内随机值。
     */
    public Integer getInterval() {
        if (fixedOrRange) {
            return fixedInt;
        }
        return ThreadLocalRandom.current().nextInt(rangeMin, rangeMax);
    }
}
