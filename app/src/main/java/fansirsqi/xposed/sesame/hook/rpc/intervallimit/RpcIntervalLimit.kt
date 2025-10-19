package fansirsqi.xposed.sesame.hook.rpc.intervallimit

import fansirsqi.xposed.sesame.util.GlobalThreadPools
import fansirsqi.xposed.sesame.util.Log
import java.util.concurrent.ConcurrentHashMap

object RpcIntervalLimit {
    private const val TAG = "RpcIntervalLimit"
    private const val DEFAULT_INTERVAL = 500
    private val DEFAULT_INTERVAL_LIMIT = DefaultIntervalLimit(DEFAULT_INTERVAL)
    private val intervalLimitMap = ConcurrentHashMap<String, IntervalLimit>()

    /**
     * 为指定方法添加间隔限制。
     *
     * @param method 方法名称
     * @param interval 间隔时间（毫秒）
     */
    fun addIntervalLimit(method: String, interval: Int) {
        addIntervalLimit(method, DefaultIntervalLimit(interval))
    }

    /**
     * 为指定方法添加自定义间隔限制对象。
     *
     * @param method 方法名称
     * @param intervalLimit 自定义的间隔限制对象
     */
    fun addIntervalLimit(method: String, intervalLimit: IntervalLimit) {
        synchronized(intervalLimitMap) {
            if (intervalLimitMap.containsKey(method)) {
                Log.runtime(TAG, "方法：$method 间隔限制已存在")
                throw IllegalArgumentException("方法：$method 间隔限制已存在")
            }
            intervalLimitMap[method] = intervalLimit
        }
    }

    /**
     * 更新指定方法的间隔限制。
     *
     * @param method 方法名称
     * @param interval 新的间隔时间（毫秒）
     */
    fun updateIntervalLimit(method: String, interval: Int) {
        updateIntervalLimit(method, DefaultIntervalLimit(interval))
    }

    /**
     * 更新指定方法的间隔限制对象。
     *
     * @param method 方法名称
     * @param intervalLimit 新的自定义间隔限制对象
     */
    fun updateIntervalLimit(method: String, intervalLimit: IntervalLimit) {
        intervalLimitMap[method] = intervalLimit
    }

    /**
     * 进入指定方法的间隔限制，确保调用间隔时间不小于设定值。
     * 优化：减少锁持有时间，仅在必要时等待
     *
     * @param method 方法名称
     */
    fun enterIntervalLimit(method: String) {
        val intervalLimit = intervalLimitMap.getOrDefault(method, DEFAULT_INTERVAL_LIMIT)
        val lock = requireNotNull(intervalLimit) { "间隔限制对象不能为空" }

        // 优化：先在锁外计算需要等待的时间，减少锁持有时间
        val sleepTime = synchronized(lock) {
            val interval = intervalLimit.interval ?: DEFAULT_INTERVAL
            val now = System.currentTimeMillis()
            val lastTime = intervalLimit.time
            interval - (now - lastTime)
        }

        // 优化：在锁外进行sleep，避免阻塞其他线程
        if (sleepTime > 0) {
            GlobalThreadPools.sleepCompat(sleepTime)
        }

        // 优化：仅在需要时更新时间戳
        synchronized(lock) {
            intervalLimit.time = System.currentTimeMillis()
        }
    }

    /**
     * 清除所有方法的间隔限制。
     */
    fun clearIntervalLimit() {
        intervalLimitMap.clear()
    }
}