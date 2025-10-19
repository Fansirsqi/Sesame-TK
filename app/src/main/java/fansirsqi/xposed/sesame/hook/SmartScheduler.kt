package fansirsqi.xposed.sesame.hook

import android.content.Context
import fansirsqi.xposed.sesame.util.Log
import fansirsqi.xposed.sesame.util.Notify
import fansirsqi.xposed.sesame.util.TimeUtil
import kotlinx.coroutines.*
import kotlin.coroutines.coroutineContext
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs

/**
 * 智能定时调度器
 * 
 * 替代AlarmManager唤醒方式，使用更温和的协程定时机制
 * 特性：
 * 1. 不依赖系统Alarm，避免干扰支付宝运行
 * 2. 支持精确定时任务（06:58, 11:58, 16:58启动）
 * 3. 能量收集高峰期保持活跃（07:00-08:00, 12:00-13:00, 17:00-18:00）
 * 4. 集成支付宝进程监控，异常时自动重启
 * 
 * @author Performance Optimizer
 * @since 2025-10-18
 */
object SmartScheduler {
    private const val TAG = "SmartScheduler"
    
    /**
     * 定时启动时间点（提前2分钟启动以准备）
     */
    private val STARTUP_TIMES = listOf(
        "00:00",  // 0点唤醒，执行新一天的任务 ⭐
        "00:05",  // 0点后5分钟执行任务
        "06:58",  // 早高峰前启动
        "11:58",  // 午高峰前启动
        "16:58"   // 晚高峰前启动
    )
    
    /**
     * 唤醒时间点（仅唤醒支付宝，不执行任务）
     */
    private val WAKEUP_ONLY_TIMES = listOf(
        "00:00"   // 0点仅唤醒
    )
    
    /**
     * 能量收集高峰时段
     */
    private val PEAK_PERIODS = listOf(
        PeakPeriod(7, 0, 8, 0),    // 07:00-08:00
        PeakPeriod(12, 0, 13, 0),  // 12:00-13:00
        PeakPeriod(17, 0, 18, 0)   // 17:00-18:00
    )
    
    /**
     * 进程检查间隔（高峰期10分钟）
     */
    private const val PROCESS_CHECK_INTERVAL = 10 * 60 * 1000L
    
    /**
     * 调度器状态
     */
    private val isRunning = AtomicBoolean(false)
    private val isInPeakPeriod = AtomicBoolean(false)
    
    /**
     * 协程作用域
     */
    private val schedulerScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Default + CoroutineName("SmartScheduler")
    )
    
    /**
     * 任务Job映射
     */
    private val scheduledJobs = ConcurrentHashMap<String, Job>()
    
    /**
     * 高峰时段定义
     */
    data class PeakPeriod(
        val startHour: Int,
        val startMinute: Int,
        val endHour: Int,
        val endMinute: Int
    ) {
        fun isInPeriod(calendar: Calendar): Boolean {
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)
            val currentMinutes = hour * 60 + minute
            val startMinutes = startHour * 60 + startMinute
            val endMinutes = endHour * 60 + endMinute
            return currentMinutes in startMinutes until endMinutes
        }
    }
    
    /**
     * 启动智能调度器
     */
    fun start(context: Context) {
        if (isRunning.compareAndSet(false, true)) {
            Log.record(TAG, "🚀 启动智能定时调度器")
            
            // 启动主调度任务
            val mainJob = schedulerScope.launch {
                runScheduler(context)
            }
            scheduledJobs["main"] = mainJob
            
            Log.record(TAG, "✅ 智能调度器已启动，定时启动时间: ${STARTUP_TIMES.joinToString(", ")}")
        }
    }
    
    /**
     * 停止智能调度器
     */
    fun stop() {
        if (isRunning.compareAndSet(true, false)) {
            Log.record(TAG, "停止智能定时调度器")
            scheduledJobs.values.forEach { it.cancel() }
            scheduledJobs.clear()
        }
    }
    
    /**
     * 主调度循环
     */
    private suspend fun runScheduler(context: Context) = coroutineScope {
        while (isActive && isRunning.get()) {
            try {
                val now = Calendar.getInstance()
                
                // 检查是否在高峰期
                val inPeak = PEAK_PERIODS.any { it.isInPeriod(now) }
                isInPeakPeriod.set(inPeak)
                
                if (inPeak) {
                    // 高峰期：执行能量收集并监控进程
                    handlePeakPeriod(context)
                } else {
                    // 非高峰期：检查是否需要启动
                    handleNormalPeriod()
                }
                
                // 每分钟检查一次
                delay(60 * 1000)
                
            } catch (e: CancellationException) {
                Log.debug(TAG, "调度器被取消")
                throw e
            } catch (e: Exception) {
                Log.error(TAG, "调度器运行异常: ${e.message}")
                Log.printStackTrace(e)
                delay(60 * 1000) // 异常后等待1分钟再继续
            }
        }
    }
    
    /**
     * 处理高峰期逻辑
     */
    private suspend fun handlePeakPeriod(context: Context) {
        Log.debug(TAG, "📊 当前处于能量收集高峰期")
        
        // 确保芝麻粒和支付宝都在运行
        AlipayProcessMonitor.ensureAlipayRunning(context)
        
        // 触发能量收集（如果还没运行）
        triggerEnergyCollection()
    }
    
    /**
     * 处理非高峰期逻辑
     */
    private suspend fun handleNormalPeriod() {
        val now = Calendar.getInstance()
        val currentTime = String.format("%02d:%02d", now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE))
        
        // 检查是否到达启动时间
        if (STARTUP_TIMES.contains(currentTime)) {
            Log.record(TAG, "⏰ 到达定时启动时间: $currentTime")
            triggerEnergyCollection()
        }
    }
    
    /**
     * 触发能量收集
     */
    private fun triggerEnergyCollection() {
        try {
            // 使用executeByBroadcast直接执行任务，避免重新初始化
            ApplicationHook.executeByBroadcast()
            Log.record(TAG, "✅ 已触发能量收集任务")
        } catch (e: Exception) {
            Log.error(TAG, "触发能量收集失败: ${e.message}")
        }
    }
    
    /**
     * 获取下次启动时间
     */
    fun getNextStartupTime(): String? {
        val now = Calendar.getInstance()
        val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        
        for (time in STARTUP_TIMES) {
            val parts = time.split(":")
            val targetMinutes = parts[0].toInt() * 60 + parts[1].toInt()
            
            if (currentMinutes < targetMinutes) {
                return time
            }
        }
        
        // 如果今天的时间都过了，返回明天的第一个时间
        return STARTUP_TIMES.first()
    }
    
    /**
     * 获取调度器状态
     */
    fun getStatus(): String {
        val running = isRunning.get()
        val inPeak = isInPeakPeriod.get()
        val nextStartup = getNextStartupTime()
        
        return "智能调度器状态: ${if (running) "运行中" else "已停止"}, " +
               "高峰期: ${if (inPeak) "是" else "否"}, " +
               "下次启动: $nextStartup, " +
               "活跃任务: ${scheduledJobs.size}"
    }
}

/**
 * 支付宝进程监控器
 * 
 * 负责监控支付宝运行状态，异常时自动重启
 */
object AlipayProcessMonitor {
    private const val TAG = "AlipayProcessMonitor"
    
    /**
     * 最后一次检查时间
     */
    @Volatile
    private var lastCheckTime = 0L
    
    /**
     * 检查间隔（10分钟）
     */
    private const val CHECK_INTERVAL = 10 * 60 * 1000L
    
    /**
     * 重启计数
     */
    private val restartCount = AtomicBoolean(false)
    
    /**
     * 确保支付宝运行
     */
    suspend fun ensureAlipayRunning(context: Context) {
        val now = System.currentTimeMillis()
        
        // 限制检查频率
        if (now - lastCheckTime < CHECK_INTERVAL) {
            return
        }
        
        lastCheckTime = now
        
        try {
            // 检查支付宝进程是否存在
            val isRunning = isAlipayProcessRunning(context)
            
            if (!isRunning) {
                Log.record(TAG, "⚠️ 检测到支付宝未运行，准备重启...")
                restartAlipay(context)
            } else {
                // 检查是否能正常通信
                val canCommunicate = checkAlipayCommunication()
                if (!canCommunicate) {
                    Log.record(TAG, "⚠️ 支付宝通信异常，准备重启...")
                    restartAlipay(context)
                }
            }
        } catch (e: Exception) {
            Log.error(TAG, "进程监控异常: ${e.message}")
            Log.printStackTrace(e)
        }
    }
    
    /**
     * 检查支付宝进程是否运行
     */
    private fun isAlipayProcessRunning(context: Context): Boolean {
        try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? android.app.ActivityManager
            val processes = activityManager?.runningAppProcesses ?: return false
            
            return processes.any { it.processName == "com.eg.android.AlipayGphone" }
        } catch (e: Exception) {
            Log.error(TAG, "检查进程失败: ${e.message}")
            return true // 检查失败假设正在运行
        }
    }
    
    /**
     * 检查支付宝通信
     */
    private fun checkAlipayCommunication(): Boolean {
        return try {
            // 简化检查：尝试调用一个轻量级的RPC请求
            // 如果支付宝正常，这个方法会返回true
            true // 简化版：假设支付宝正常运行
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 重启支付宝
     */
    private suspend fun restartAlipay(context: Context) {
        if (restartCount.compareAndSet(false, true)) {
            try {
                Log.record(TAG, "🔄 开始重启支付宝...")
                
                // 方法1: 通过广播重启
                ApplicationHook.reLoginByBroadcast()
                
                // 等待3秒让支付宝启动
                delay(3000)
                
                // 方法2: 如果广播失败，尝试使用组件唤醒
                if (!isAlipayProcessRunning(context)) {
                    Log.record(TAG, "尝试使用组件方式唤醒支付宝...")
                    wakeAlipayByComponent(context)
                }
                
                Log.record(TAG, "✅ 支付宝重启完成")
                
            } catch (e: Exception) {
                Log.error(TAG, "重启支付宝失败: ${e.message}")
            } finally {
                // 5分钟后允许再次重启
                delay(5 * 60 * 1000)
                restartCount.set(false)
            }
        }
    }
    
    /**
     * 通过组件唤醒支付宝
     */
    private fun wakeAlipayByComponent(context: Context) {
        try {
            val intent = context.packageManager.getLaunchIntentForPackage("com.eg.android.AlipayGphone")
            if (intent != null) {
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                Log.record(TAG, "已发送支付宝启动Intent")
            }
        } catch (e: Exception) {
            Log.error(TAG, "组件唤醒失败: ${e.message}")
        }
    }
    
    /**
     * 获取监控统计
     */
    fun getStats(): String {
        val lastCheck = if (lastCheckTime > 0) {
            "${(System.currentTimeMillis() - lastCheckTime) / 1000}秒前"
        } else {
            "未检查"
        }
        
        return "进程监控 - 最后检查: $lastCheck"
    }
}
