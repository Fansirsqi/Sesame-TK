package fansirsqi.xposed.sesame.task.antForest

import fansirsqi.xposed.sesame.util.Log
import fansirsqi.xposed.sesame.util.maps.UserMap
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max

/**
 * 蚂蚁森林好友管理器
 * 
 * 功能：
 * 1. 好友能量统计和智能筛选
 * 2. 找能量成功率统计
 * 3. 动态调整找能量冷却时间
 * 
 * @author TK优化版
 * @since 2025-10-19
 */
object ForestFriendManager {
    private const val TAG = "ForestFriendManager"
    
    // ==================== 好友能量统计 ====================
    
    /**
     * 好友能量数据
     */
    data class FriendEnergyData(
        var userId: String,
        var totalEnergy: Long = 0,          // 累计收取能量
        var collectCount: Int = 0,          // 收取次数
        var lastCollectTime: Long = 0,      // 最后收取时间
        var consecutiveZero: Int = 0,       // 连续0能量次数
        var protectedCount: Int = 0,        // 被保护罩次数
        var avgEnergy: Int = 0              // 平均能量
    ) {
        fun updateAvgEnergy() {
            avgEnergy = if (collectCount > 0) {
                (totalEnergy / collectCount).toInt()
            } else 0
        }
        
        /**
         * 计算好友优先级得分（越高越优先）
         */
        fun getPriorityScore(): Int {
            // 基础分：平均能量
            var score = avgEnergy
            
            // 惩罚：连续0能量
            score -= consecutiveZero * 10
            
            // 惩罚：经常被保护罩
            if (protectedCount > collectCount * 0.5) {
                score -= 20
            }
            
            // 奖励：最近有收取
            val timeSinceLastCollect = System.currentTimeMillis() - lastCollectTime
            if (timeSinceLastCollect < 24 * 60 * 60 * 1000) { // 24小时内
                score += 10
            }
            
            return max(score, 0)
        }
    }
    
    /**
     * 好友能量数据映射
     */
    private val friendDataMap = ConcurrentHashMap<String, FriendEnergyData>()
    
    /**
     * 记录好友能量收取
     */
    fun recordFriendCollect(userId: String, energy: Int, isProtected: Boolean = false) {
        val data = friendDataMap.computeIfAbsent(userId) { FriendEnergyData(userId) }
        
        data.collectCount++
        data.lastCollectTime = System.currentTimeMillis()
        
        if (isProtected) {
            data.protectedCount++
        } else if (energy > 0) {
            data.totalEnergy += energy
            data.consecutiveZero = 0
        } else {
            data.consecutiveZero++
        }
        
        data.updateAvgEnergy()
    }
    
    /**
     * 获取智能筛选的好友列表
     * 
     * @param allFriends 所有好友列表
     * @param topN 返回前N个好友（默认100）
     * @return 按优先级排序的好友列表
     */
    fun getSmartFilteredFriends(allFriends: List<String>, topN: Int = 100): List<String> {
        // 计算每个好友的优先级得分
        val friendScores = allFriends.map { userId ->
            val data = friendDataMap[userId] ?: FriendEnergyData(userId)
            Pair(userId, data.getPriorityScore())
        }
        
        // 按得分排序，取前topN个
        val topFriends = friendScores
            .sortedByDescending { it.second }
            .take(topN)
            .map { it.first }
        
        Log.record(TAG, "智能筛选: 从${allFriends.size}位好友中选出前${topFriends.size}位")
        return topFriends
    }
    
    /**
     * 获取好友统计信息
     */
    fun getFriendStats(userId: String): FriendEnergyData? {
        return friendDataMap[userId]
    }
    
    /**
     * 获取所有好友统计（按平均能量排序）
     */
    fun getAllFriendStats(topN: Int = 20): List<FriendEnergyData> {
        return friendDataMap.values
            .sortedByDescending { it.avgEnergy }
            .take(topN)
    }
    
    // ==================== 找能量成功率统计 ====================
    
    /**
     * 找能量统计数据
     */
    data class FindEnergyStats(
        var totalAttempts: Int = 0,         // 总尝试次数
        var successfulFinds: Int = 0,       // 成功找到能量的次数
        var totalFriendsChecked: Int = 0,   // 检查的好友总数
        var totalEnergyFound: Long = 0,     // 找到的总能量
        var lastAttemptTime: Long = 0,      // 最后尝试时间
        var lastSuccessTime: Long = 0       // 最后成功时间
    ) {
        /**
         * 成功率
         */
        val successRate: Double
            get() = if (totalAttempts > 0) {
                successfulFinds.toDouble() / totalAttempts
            } else 0.0
            
        /**
         * 最近10次的成功率
         */
        var recentAttempts: MutableList<Boolean> = mutableListOf()
        
        fun addAttempt(success: Boolean) {
            recentAttempts.add(success)
            if (recentAttempts.size > 10) {
                recentAttempts.removeAt(0)
            }
        }
        
        val recentSuccessRate: Double
            get() = if (recentAttempts.isNotEmpty()) {
                recentAttempts.count { it }.toDouble() / recentAttempts.size
            } else successRate
    }
    
    /**
     * 找能量统计数据
     */
    private val findEnergyStats = FindEnergyStats()
    
    /**
     * 最小冷却时间（10分钟）
     */
    private const val MIN_COOLDOWN = 10 * 60 * 1000L
    
    /**
     * 最大冷却时间（20分钟）
     */
    private const val MAX_COOLDOWN = 20 * 60 * 1000L
    
    /**
     * 当前冷却时间
     */
    private var currentCooldown = 15 * 60 * 1000L // 默认15分钟
    
    /**
     * 记录找能量尝试
     * 
     * @param foundEnergy 找到的能量数
     * @param friendsChecked 检查的好友数
     */
    fun recordFindEnergyAttempt(foundEnergy: Long, friendsChecked: Int) {
        findEnergyStats.totalAttempts++
        findEnergyStats.totalFriendsChecked += friendsChecked
        findEnergyStats.lastAttemptTime = System.currentTimeMillis()
        
        val success = foundEnergy > 0
        findEnergyStats.addAttempt(success)
        
        if (success) {
            findEnergyStats.successfulFinds++
            findEnergyStats.totalEnergyFound += foundEnergy
            findEnergyStats.lastSuccessTime = System.currentTimeMillis()
        }
        
        // 动态调整冷却时间
        adjustCooldownTime()
        
        Log.record(TAG, "找能量记录: 找到${foundEnergy}g, 检查${friendsChecked}个好友, " +
                "成功率: ${"%.1f".format(findEnergyStats.recentSuccessRate * 100)}%, " +
                "冷却时间: ${currentCooldown / 60000}分钟")
    }
    
    /**
     * 根据成功率动态调整冷却时间
     */
    private fun adjustCooldownTime() {
        val rate = findEnergyStats.recentSuccessRate
        
        currentCooldown = when {
            rate >= 0.7 -> {
                // 成功率70%以上，缩短到10分钟
                MIN_COOLDOWN
            }
            rate >= 0.4 -> {
                // 成功率40-70%，15分钟
                15 * 60 * 1000L
            }
            else -> {
                // 成功率低于40%，延长到20分钟
                MAX_COOLDOWN
            }
        }
        
        Log.debug(TAG, "冷却时间调整为: ${currentCooldown / 60000}分钟 (成功率: ${"%.1f".format(rate * 100)}%)")
    }
    
    /**
     * 获取当前冷却时间（毫秒）
     */
    fun getCurrentCooldown(): Long {
        return currentCooldown
    }
    
    /**
     * 获取找能量统计
     */
    fun getFindEnergyStats(): FindEnergyStats {
        return findEnergyStats
    }
    
    /**
     * 重置统计数据
     */
    fun resetStats() {
        friendDataMap.clear()
        findEnergyStats.totalAttempts = 0
        findEnergyStats.successfulFinds = 0
        findEnergyStats.totalFriendsChecked = 0
        findEnergyStats.totalEnergyFound = 0
        findEnergyStats.recentAttempts.clear()
        currentCooldown = 15 * 60 * 1000L
        
        Log.record(TAG, "已重置所有统计数据")
    }
    
    // ==================== 统计报告 ====================
    
    /**
     * 生成统计报告
     */
    fun generateReport(): String {
        val sb = StringBuilder()
        sb.appendLine("========== 蚂蚁森林统计报告 ==========")
        sb.appendLine()
        
        // 找能量统计
        sb.appendLine("📊 找能量统计:")
        sb.appendLine("  总尝试次数: ${findEnergyStats.totalAttempts}")
        sb.appendLine("  成功次数: ${findEnergyStats.successfulFinds}")
        sb.appendLine("  总成功率: ${"%.1f".format(findEnergyStats.successRate * 100)}%")
        sb.appendLine("  最近成功率: ${"%.1f".format(findEnergyStats.recentSuccessRate * 100)}%")
        sb.appendLine("  累计能量: ${findEnergyStats.totalEnergyFound}g")
        sb.appendLine("  当前冷却: ${currentCooldown / 60000}分钟")
        sb.appendLine()
        
        // 好友能量排行
        val topFriends = getAllFriendStats(10)
        if (topFriends.isNotEmpty()) {
            sb.appendLine("🏆 好友能量排行 (前10):")
            topFriends.forEachIndexed { index, data ->
                val name = UserMap.getMaskName(data.userId)
                sb.appendLine("  ${index + 1}. $name - 平均${data.avgEnergy}g, 累计${data.totalEnergy}g, 收取${data.collectCount}次")
            }
            sb.appendLine()
        }
        
        sb.appendLine("=====================================")
        return sb.toString()
    }
    
    /**
     * 打印统计报告
     */
    fun printReport() {
        Log.record(TAG, generateReport())
    }
}
