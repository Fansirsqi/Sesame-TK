package fansirsqi.xposed.sesame.hook.rpc.intervallimit

import java.util.concurrent.ThreadLocalRandom

/**
 * 实现一个支持固定间隔与范围间隔的限制器。
 * 格式：
 * - 空或单个数字：固定间隔
 * - 形如 "1000-3000"：范围间隔（闭区间 -> 开区间）
 */
class FixedOrRangeIntervalLimit(
    fixedOrRangeStr: String?,
    private val min: Int,
    private val max: Int
) : IntervalLimit {

    private val isFixed: Boolean
    private val fixedInterval: Int
    private val rangeMin: Int
    private val rangeMax: Int

    override val interval: Int
        get() = if (isFixed) {
            fixedInterval
        } else {
            ThreadLocalRandom.current().nextInt(rangeMin, rangeMax)
        }

    override var time: Long = 0

    init {
        require(min <= max) { "min must be <= max" }

        val (fixedMode, fInt, rMin, rMax) = parseIntervalString(fixedOrRangeStr)
        isFixed = fixedMode
        fixedInterval = fInt
        rangeMin = rMin
        rangeMax = rMax
    }

    private fun parseIntervalString(str: String?): Quad {
        if (str.isNullOrBlank()) {
            // 默认使用固定模式，值为 max
            val fixed = clamp(max)
            return Quad(true, fixed, -1, -1)
        }

        val parts = str.split("-")
        return if (parts.size == 2) {
            // 范围模式
            val minVal = clamp(parts[0].toIntOrNull() ?: min)
            val maxVal = clamp(parts[1].toIntOrNull() ?: max)
            require(minVal < maxVal) { "rangeMin must be less than rangeMax" }
            Quad(false, -1, minVal, maxVal + 1) // 开区间
        } else {
            // 固定模式
            val fixed = clamp(str.toIntOrNull() ?: max)
            Quad(true, fixed, -1, -1)
        }
    }

    private fun clamp(value: Int): Int = value.coerceIn(min, max)

    /**
     * 本地辅助类用于解析结果返回
     */
    private data class Quad(
        val isFixed: Boolean,
        val fixedInterval: Int,
        val rangeMin: Int,
        val rangeMax: Int
    )
}
