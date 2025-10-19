package fansirsqi.xposed.sesame.task.antForest

import fansirsqi.xposed.sesame.util.Log
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.abs

/**
 * 能量收集优化器
 * 
 * 功能：
 * 1. 智能预测好友能量成熟时间
 * 2. 优化访问顺序，优先访问即将成熟的好友
 * 3. 缓存空森林和保护罩用户，减少无效请求
 * 4. 统计并发性能指标
 *
 * @author Performance Optimizer
 * @since 2025-10-18
 */
object EnergyCollectOptimizer {
    private const val TAG = "EnergyCollectOptimizer"
    
    /**
     * 用户能量成熟时间预测缓存
     * Key: userId, Value: 预测的能量成熟时间戳(ms)
     */
    private val energyTimePredictions = ConcurrentHashMap<String, Long>()
    
    /**
     * 空森林缓存（本轮任务）
     * Key: userId, Value: 记录时间戳
     */
    private val emptyForestCache = ConcurrentHashMap<String, Long>()
    
    /**
     * 保护罩用户缓存
     * Key: userId, Value: 保护罩结束时间戳
     */
    private val shieldUsersCache = ConcurrentHashMap<String, Long>()
    
    /**
     * 性能统计
     */
    private val totalRequests = AtomicLong(0)
    private val skippedRequests = AtomicLong(0)
    private val cacheHits = AtomicLong(0)
    
    /**
     * 预测用户下次能量成熟时间
     * 
     * @param userId 用户ID
     * @param lastCollectTime 上次收集时间
     * @return 预测的成熟时间戳，null表示无法预测
     */
    fun predictEnergyTime(userId: String, lastCollectTime: Long): Long? {
        return energyTimePredictions[userId]?.let { predicted ->
            // 如果预测时间已过，更新为下一个可能的时间点（通常是6小时或12小时后）
            val now = System.currentTimeMillis()
            if (predicted < now) {
                val nextTime = now + 6 * 3600 * 1000 // 默认6小时后
                energyTimePredictions[userId] = nextTime
                nextTime
            } else {
                predicted
            }
        }
    }
    
    /**
     * 记录能量成熟时间（用于学习模式）
     * 
     * @param userId 用户ID
     * @param maturityTime 能量成熟时间
     */
    fun recordEnergyMaturity(userId: String, maturityTime: Long) {
        val now = System.currentTimeMillis()
        
        // 计算下次可能的成熟时间（基于常见的能量产生模式）
        val possibleNextTimes = listOf(
            maturityTime + 6 * 3600 * 1000,  // 6小时后
            maturityTime + 12 * 3600 * 1000, // 12小时后
            maturityTime + 18 * 3600 * 1000  // 18小时后
        )
        
        // 选择最接近未来的时间
        val nextPrediction = possibleNextTimes.firstOrNull { it > now }
        if (nextPrediction != null) {
            energyTimePredictions[userId] = nextPrediction
        }
    }
    
    /**
     * 检查用户是否应该跳过（基于缓存）
     * 
     * @param userId 用户ID
     * @return true表示应该跳过
     */
    fun shouldSkipUser(userId: String): Boolean {
        totalRequests.incrementAndGet()
        
        val now = System.currentTimeMillis()
        
        // 检查空森林缓存（5分钟内）
        emptyForestCache[userId]?.let { recordTime ->
            if (now - recordTime < 5 * 60 * 1000) {
                skippedRequests.incrementAndGet()
                cacheHits.incrementAndGet()
                return true
            } else {
                emptyForestCache.remove(userId)
            }
        }
        
        // 检查保护罩缓存
        shieldUsersCache[userId]?.let { shieldEndTime ->
            if (now < shieldEndTime) {
                skippedRequests.incrementAndGet()
                cacheHits.incrementAndGet()
                return true
            } else {
                shieldUsersCache.remove(userId)
            }
        }
        
        return false
    }
    
    /**
     * 标记用户为空森林
     * 
     * @param userId 用户ID
     */
    fun markEmptyForest(userId: String) {
        emptyForestCache[userId] = System.currentTimeMillis()
    }
    
    /**
     * 标记用户有保护罩
     * 
     * @param userId 用户ID
     * @param shieldEndTime 保护罩结束时间，null表示使用默认时间（24小时）
     */
    fun markShieldUser(userId: String, shieldEndTime: Long? = null) {
        val endTime = shieldEndTime ?: (System.currentTimeMillis() + 24 * 3600 * 1000)
        shieldUsersCache[userId] = endTime
    }
    
    /**
     * 清理本轮任务的临时缓存
     */
    fun clearRoundCache() {
        emptyForestCache.clear()
        Log.debug(TAG, "已清理本轮空森林缓存")
    }
    
    /**
     * 清理过期的保护罩缓存
     */
    fun cleanupExpiredShields() {
        val now = System.currentTimeMillis()
        val iterator = shieldUsersCache.entries.iterator()
        var cleaned = 0
        
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.value < now) {
                iterator.remove()
                cleaned++
            }
        }
        
        if (cleaned > 0) {
            Log.debug(TAG, "清理了 $cleaned 个过期保护罩缓存")
        }
    }
    
    /**
     * 对用户列表按预测能量成熟时间排序
     * 优先访问即将成熟的用户
     * 
     * @param userIds 用户ID列表
     * @return 排序后的用户ID列表
     */
    fun sortByPredictedEnergy(userIds: List<String>): List<String> {
        val now = System.currentTimeMillis()
        
        return userIds.sortedBy { userId ->
            val predictedTime = energyTimePredictions[userId]
            
            when {
                // 已经成熟的优先级最高
                predictedTime != null && predictedTime <= now -> 0L
                // 即将成熟的按时间排序（最接近的优先）
                predictedTime != null -> abs(predictedTime - now)
                // 没有预测数据的放最后
                else -> Long.MAX_VALUE
            }
        }
    }
    
    /**
     * 获取性能统计信息
     * 
     * @return 统计信息字符串
     */
    fun getPerformanceStats(): String {
        val total = totalRequests.get()
        val skipped = skippedRequests.get()
        val hits = cacheHits.get()
        
        val skipRate = if (total > 0) (skipped * 100.0 / total) else 0.0
        val hitRate = if (total > 0) (hits * 100.0 / total) else 0.0
        
        return "总请求: $total, 跳过: $skipped (${String.format("%.1f", skipRate)}%), " +
               "缓存命中: $hits (${String.format("%.1f", hitRate)}%)"
    }
    
    /**
     * 重置性能统计
     */
    fun resetStats() {
        totalRequests.set(0)
        skippedRequests.set(0)
        cacheHits.set(0)
    }
    
    /**
     * 完全清理所有缓存
     */
    fun clearAllCaches() {
        energyTimePredictions.clear()
        emptyForestCache.clear()
        shieldUsersCache.clear()
        resetStats()
        Log.record(TAG, "已清理所有优化器缓存")
    }
}
