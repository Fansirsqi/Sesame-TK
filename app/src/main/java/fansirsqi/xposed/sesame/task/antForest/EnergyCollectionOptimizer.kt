package fansirsqi.xposed.sesame.task.antForest

import fansirsqi.xposed.sesame.util.Log
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.min

/**
 * 能量收取优化器（安全版本）
 * 
 * 使用lazy延迟初始化，避免类加载时崩溃
 * 所有方法都有详细的异常处理和日志
 * 
 * @author 芝麻粒优化版 v2
 * @date 2025-10-19
 */
object EnergyCollectionOptimizer {
    private const val TAG = "EnergyOptimizer"
    
    // 使用lazy延迟初始化，避免在类加载时就初始化
    private val latencyHistory by lazy { 
        try {
            mutableListOf<Long>()
        } catch (e: Exception) {
            Log.error(TAG, "初始化latencyHistory失败: ${e.message}")
            mutableListOf<Long>()
        }
    }
    
    private val stats by lazy {
        try {
            CollectStats()
        } catch (e: Exception) {
            Log.error(TAG, "初始化stats失败: ${e.message}")
            CollectStats()
        }
    }
    
    private val friendEnergyHistory by lazy {
        try {
            ConcurrentHashMap<String, FriendEnergyRecord>()
        } catch (e: Exception) {
            Log.error(TAG, "初始化friendEnergyHistory失败: ${e.message}")
            ConcurrentHashMap<String, FriendEnergyRecord>()
        }
    }
    
    // ==================== 1. 时间段动态间隔 ====================
    
    /**
     * 根据时间段计算收取间隔（带异常处理）
     * 
     * 时间段规则：
     * - 7-8点（早高峰）：2分钟
     * - 12-13点（午休）：2分钟
     * - 17-18点（晚高峰）：2分钟
     * - 0-1点（午夜能量成熟）：2分钟
     * - 2-6点（深夜）：30分钟
     * - 其他时间：5分钟
     */
    @JvmStatic
    fun calculateDynamicInterval(): Long {
        return try {
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            
            when (hour) {
                in 7..8 -> 2 * 60 * 1000L      // 早高峰：2分钟
                in 12..13 -> 2 * 60 * 1000L    // 午休：2分钟
                in 17..18 -> 2 * 60 * 1000L    // 晚高峰：2分钟
                0, 1 -> 2 * 60 * 1000L         // 午夜：2分钟
                in 2..6 -> 30 * 60 * 1000L     // 深夜：30分钟
                else -> 5 * 60 * 1000L         // 其他：5分钟
            }
        } catch (e: Exception) {
            Log.error(TAG, "计算动态间隔失败: ${e.message}")
            5 * 60 * 1000L // 返回默认值
        }
    }
    
    /**
     * 获取下次检查时间描述（带异常处理）
     */
    @JvmStatic
    fun getNextCheckTimeDescription(): String {
        return try {
            val interval = calculateDynamicInterval()
            val nextTime = System.currentTimeMillis() + interval
            val calendar = Calendar.getInstance().apply { timeInMillis = nextTime }
            
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val reason = when (hour) {
                in 7..8 -> "早高峰"
                in 12..13 -> "午休"
                in 17..18 -> "晚高峰"
                0, 1 -> "午夜成熟"
                in 2..6 -> "深夜"
                else -> "常规"
            }
            
            String.format(
                "%02d:%02d:%02d [%s期，间隔%d分钟]",
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                calendar.get(Calendar.SECOND),
                reason,
                interval / 60000
            )
        } catch (e: Exception) {
            Log.error(TAG, "获取下次检查时间失败: ${e.message}")
            "未知"
        }
    }
    
    // ==================== 2. 自适应批量大小 ====================
    
    enum class NetworkQuality {
        EXCELLENT,  // 优秀 (延迟<200ms)
        GOOD,       // 良好 (200-500ms)
        POOR,       // 较差 (>500ms)
        UNKNOWN     // 未知
    }
    
    /**
     * 记录RPC延迟（带异常处理）
     */
    @JvmStatic
    fun recordRpcLatency(latency: Long) {
        try {
            latencyHistory.add(latency)
            
            // 只保留最近10次记录
            if (latencyHistory.size > 10) {
                latencyHistory.removeAt(0)
            }
        } catch (e: Exception) {
            Log.error(TAG, "记录RPC延迟失败: ${e.message}")
        }
    }
    
    /**
     * 检测网络质量（带异常处理）
     */
    @JvmStatic
    fun detectNetworkQuality(): NetworkQuality {
        return try {
            if (latencyHistory.isEmpty()) {
                return NetworkQuality.UNKNOWN
            }
            
            val avgLatency = latencyHistory.average()
            
            when {
                avgLatency < 200 -> NetworkQuality.EXCELLENT
                avgLatency < 500 -> NetworkQuality.GOOD
                else -> NetworkQuality.POOR
            }
        } catch (e: Exception) {
            Log.error(TAG, "检测网络质量失败: ${e.message}")
            NetworkQuality.UNKNOWN
        }
    }
    
    /**
     * 计算最优批量大小（带异常处理）
     */
    @JvmStatic
    fun calculateOptimalBatchSize(totalBubbles: Int): Int {
        return try {
            val quality = detectNetworkQuality()
            
            val baseBatchSize = when (quality) {
                NetworkQuality.EXCELLENT -> 10  // 网络优秀：10个
                NetworkQuality.GOOD -> 6        // 网络良好：6个
                NetworkQuality.POOR -> 3        // 网络较差：3个
                NetworkQuality.UNKNOWN -> 6     // 未知：默认6个
            }
            
            min(baseBatchSize, totalBubbles)
        } catch (e: Exception) {
            Log.error(TAG, "计算最优批量大小失败: ${e.message}")
            min(6, totalBubbles) // 返回默认值
        }
    }
    
    // ==================== 3. 收取统计 ====================
    
    data class CollectStats(
        var batchCount: Int = 0,
        var singleCount: Int = 0,
        var batchSuccessCount: Int = 0,
        var batchFailCount: Int = 0,
        var totalCollected: Int = 0,
        var totalBatchTime: Long = 0,
        var totalSingleTime: Long = 0,
        var lastResetTime: Long = System.currentTimeMillis()
    )
    
    /**
     * 记录批量收取（带异常处理）
     */
    @JvmStatic
    fun recordBatchCollect(success: Boolean, duration: Long, energyCollected: Int) {
        try {
            stats.batchCount++
            if (success) {
                stats.batchSuccessCount++
            } else {
                stats.batchFailCount++
            }
            stats.totalBatchTime += duration
            stats.totalCollected += energyCollected
        } catch (e: Exception) {
            Log.error(TAG, "记录批量收取失败: ${e.message}")
        }
    }
    
    /**
     * 记录单个收取（带异常处理）
     */
    @JvmStatic
    fun recordSingleCollect(duration: Long, energyCollected: Int) {
        try {
            stats.singleCount++
            stats.totalSingleTime += duration
            stats.totalCollected += energyCollected
        } catch (e: Exception) {
            Log.error(TAG, "记录单个收取失败: ${e.message}")
        }
    }
    
    /**
     * 打印统计信息（带异常处理）
     */
    @JvmStatic
    fun printStats() {
        try {
            if (stats.batchCount == 0 && stats.singleCount == 0) {
                Log.record(TAG, "📊 暂无收取统计数据")
                return
            }
            
            val runtime = (System.currentTimeMillis() - stats.lastResetTime) / 1000 / 60
            val batchSuccessRate = if (stats.batchCount > 0) {
                stats.batchSuccessCount * 100.0 / stats.batchCount
            } else 0.0
            
            val avgBatchTime = if (stats.batchCount > 0) {
                stats.totalBatchTime / stats.batchCount
            } else 0
            
            val avgSingleTime = if (stats.singleCount > 0) {
                stats.totalSingleTime / stats.singleCount
            } else 0
            
            val networkQuality = detectNetworkQuality()
            val networkDesc = when (networkQuality) {
                NetworkQuality.EXCELLENT -> "优秀"
                NetworkQuality.GOOD -> "良好"
                NetworkQuality.POOR -> "较差"
                NetworkQuality.UNKNOWN -> "未知"
            }
            
            Log.record(TAG, """
                |
                |📊 ========== 能量收取统计报告 ==========
                |⏱️  运行时长: ${runtime}分钟
                |💰 总收取能量: ${stats.totalCollected}g
                |
                |🎯 批量收取:
                |   - 收取次数: ${stats.batchCount}
                |   - 成功次数: ${stats.batchSuccessCount}
                |   - 失败次数: ${stats.batchFailCount}
                |   - 成功率: ${String.format("%.1f", batchSuccessRate)}%
                |   - 平均耗时: ${avgBatchTime}ms
                |
                |🔹 单个收取:
                |   - 收取次数: ${stats.singleCount}
                |   - 平均耗时: ${avgSingleTime}ms
                |
                |🌐 网络质量: $networkDesc
                |📈 平均延迟: ${if (latencyHistory.isNotEmpty()) "${latencyHistory.average().toInt()}ms" else "未知"}
                |==========================================
            """.trimMargin())
        } catch (e: Exception) {
            Log.error(TAG, "打印统计信息失败: ${e.message}")
        }
    }
    
    // ==================== 4. 智能预测引擎 ====================
    
    /**
     * 能量成熟时间段
     */
    enum class EnergyMaturityPeriod(val hourRange: IntRange) {
        MORNING(7..8),      // 早晨
        NOON(12..13),       // 午休
        EVENING(17..18),    // 傍晚
        MIDNIGHT(0..1);     // 午夜
        
        companion object {
            fun getCurrentPeriod(): EnergyMaturityPeriod? {
                return try {
                    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                    values().find { hour in it.hourRange }
                } catch (e: Exception) {
                    Log.error(TAG, "获取当前时段失败: ${e.message}")
                    null
                }
            }
        }
    }
    
    /**
     * 好友能量记录
     */
    private data class FriendEnergyRecord(
        var lastMorningEnergy: Long = 0,
        var lastNoonEnergy: Long = 0,
        var lastEveningEnergy: Long = 0,
        var lastMidnightEnergy: Long = 0
    )
    
    /**
     * 记录好友能量收取（带异常处理）
     */
    @JvmStatic
    fun recordFriendEnergy(userId: String) {
        try {
            val record = friendEnergyHistory.getOrPut(userId) { FriendEnergyRecord() }
            val period = EnergyMaturityPeriod.getCurrentPeriod()
            val now = System.currentTimeMillis()
            
            when (period) {
                EnergyMaturityPeriod.MORNING -> record.lastMorningEnergy = now
                EnergyMaturityPeriod.NOON -> record.lastNoonEnergy = now
                EnergyMaturityPeriod.EVENING -> record.lastEveningEnergy = now
                EnergyMaturityPeriod.MIDNIGHT -> record.lastMidnightEnergy = now
                null -> {} // 不在能量成熟时间段
            }
        } catch (e: Exception) {
            Log.error(TAG, "记录好友能量失败: ${e.message}")
        }
    }
    
    /**
     * 判断当前是否在能量成熟高峰期（带异常处理）
     */
    @JvmStatic
    fun isInPeakPeriod(): Boolean {
        return try {
            EnergyMaturityPeriod.getCurrentPeriod() != null
        } catch (e: Exception) {
            Log.error(TAG, "判断高峰期失败: ${e.message}")
            false
        }
    }
}
