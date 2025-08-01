package fansirsqi.xposed.sesame.hook.rpc.intervallimit

import java.util.concurrent.ThreadLocalRandom

/**
 * 用于实现一个可变间隔限制器。
 * 支持固定间隔或范围间隔两种模式。
 */
class FixedOrRangeIntervalLimit(fixedOrRangeStr: String?, private val min: Int, private val max: Int) : IntervalLimit {

    /**
     * 是否为固定间隔模式。如果为 true，则表示固定间隔；否则为范围间隔。
     */
    private val fixedOrRange: Boolean

    /**
     * 固定间隔时间（毫秒），仅在固定间隔模式下使用。
     */
    private val fixedInt: Int?

    /**
     * 范围间隔的最小值（毫秒），仅在范围间隔模式下使用。
     */
    private val rangeMin: Int?

    /**
     * 范围间隔的最大值（毫秒，非闭区间），仅在范围间隔模式下使用。
     */
    private val rangeMax: Int?

    /**
     * 获取当前
     */
    override val interval: Int?
        get() {
            return if (fixedOrRange) {
                fixedInt
            } else {
                if (rangeMin != null && rangeMax != null) {
                    ThreadLocalRandom.current().nextInt(rangeMin, rangeMax)
                } else {
                    null
                }
            }
        }

    /**
     * 实现 IntervalLimit 接口的 time 属性
     */
    override var time: Long = 0
        get() = field
        set(value) {
            field = value
        }

    init {
        require(min <= max) { "min must be less than or equal to max" }

        var parsedFixedOrRange = false
        var parsedFixedInt: Int? = null
        var parsedRangeMin: Int? = null
        var parsedRangeMax: Int? = null

        if (fixedOrRangeStr.isNullOrBlank()) {
            // 默认使用固定间隔模式，值为 max
            parsedFixedOrRange = true
            parsedFixedInt = max
        } else {
            val split = fixedOrRangeStr.split("-")
            if (split.size == 2) {
                // 处理范围间隔模式
                val rangeMinTmp = parseAndClamp(split[0], min, max, min)
                val rangeMaxTmp = parseAndClamp(split[1], min, max, max)
                parsedFixedOrRange = false
                parsedRangeMin = rangeMinTmp
                parsedRangeMax = rangeMaxTmp + 1 // 调整为非闭区间
            } else {
                // 处理固定间隔模式
                val fixedIntTmp = parseAndClamp(fixedOrRangeStr, min, max, max)
                parsedFixedOrRange = true
                parsedFixedInt = fixedIntTmp
            }
        }

        this.fixedOrRange = parsedFixedOrRange
        this.fixedInt = parsedFixedInt
        this.rangeMin = parsedRangeMin
        this.rangeMax = parsedRangeMax
    }


    /**
     * 解析字符串并限制在 [min, max] 范围内，失败时返回 fallback 值。
     */
    private fun parseAndClamp(str: String, min: Int, max: Int, fallback: Int): Int {
        return try {
            val value = str.toInt()
            when {
                value < min -> min
                value > max -> max
                else -> value
            }
        } catch (e: NumberFormatException) {
            fallback
        }
    }
}