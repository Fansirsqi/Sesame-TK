package fansirsqi.xposed.sesame.task

import fansirsqi.xposed.sesame.util.Log
import kotlinx.coroutines.*
import kotlin.coroutines.coroutineContext
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * 任务健康监控器
 * 
 * 功能：
 * - 检测任务假死
 * - 监控任务执行时间
 * - 自动重启假死任务
 * - 记录任务执行统计
 * 
 * @author Performance Optimizer
 * @since 2025-10-18
 */
object TaskHealthMonitor {
    private const val TAG = "TaskHealthMonitor"
    
    /**
     * 任务假死超时时间（5分钟）
     */
    private const val TASK_TIMEOUT = 5 * 60 * 1000L
    
    /**
     * 健康检查间隔（30秒）
     */
    private const val HEALTH_CHECK_INTERVAL = 30 * 1000L
    
    /**
     * 任务执行信息
     */
    data class TaskExecutionInfo(
        val taskId: String,
        val startTime: Long,
        var lastActiveTime: Long,
        var executionCount: Int = 0,
        var isHealthy: Boolean = true
    )
    
    /**
     * 任务监控映射
     */
    private val monitoredTasks = ConcurrentHashMap<String, TaskExecutionInfo>()
    
    /**
     * 统计信息
     */
    private val totalExecutions = AtomicInteger(0)
    private val totalTimeouts = AtomicInteger(0)
    private val totalRestarts = AtomicInteger(0)
    
    /**
     * 监控器状态
     */
    @Volatile
    private var isMonitoring = false
    
    /**
     * 协程作用域
     */
    private val monitorScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Default + CoroutineName("TaskHealthMonitor")
    )
    
    /**
     * 监控Job
     */
    private var monitorJob: Job? = null
    
    /**
     * 启动监控
     */
    fun startMonitoring() {
        if (!isMonitoring) {
            isMonitoring = true
            monitorJob = monitorScope.launch {
                runHealthCheck()
            }
            Log.record(TAG, "✅ 任务健康监控器已启动")
        }
    }
    
    /**
     * 停止监控
     */
    fun stopMonitoring() {
        isMonitoring = false
        monitorJob?.cancel()
        monitorJob = null
        Log.record(TAG, "任务健康监控器已停止")
    }
    
    /**
     * 注册任务
     */
    fun registerTask(taskId: String) {
        val now = System.currentTimeMillis()
        val info = TaskExecutionInfo(
            taskId = taskId,
            startTime = now,
            lastActiveTime = now
        )
        monitoredTasks[taskId] = info
        totalExecutions.incrementAndGet()
        
        Log.debug(TAG, "注册任务监控: $taskId")
    }
    
    /**
     * 任务心跳（表示任务仍在活跃）
     */
    fun taskHeartbeat(taskId: String) {
        monitoredTasks[taskId]?.let { info ->
            info.lastActiveTime = System.currentTimeMillis()
            info.isHealthy = true
            info.executionCount++
        }
    }
    
    /**
     * 取消注册任务
     */
    fun unregisterTask(taskId: String) {
        monitoredTasks.remove(taskId)
        Log.debug(TAG, "取消任务监控: $taskId")
    }
    
    /**
     * 运行健康检查
     */
    private suspend fun runHealthCheck() = coroutineScope {
        while (isActive && isMonitoring) {
            try {
                checkTaskHealth()
                delay(HEALTH_CHECK_INTERVAL)
            } catch (e: CancellationException) {
                Log.debug(TAG, "健康检查被取消")
                throw e
            } catch (e: Exception) {
                Log.error(TAG, "健康检查异常: ${e.message}")
                Log.printStackTrace(e)
                delay(HEALTH_CHECK_INTERVAL)
            }
        }
    }
    
    /**
     * 检查任务健康状态
     */
    private fun checkTaskHealth() {
        val now = System.currentTimeMillis()
        
        monitoredTasks.values.forEach { info ->
            val inactiveTime = now - info.lastActiveTime
            
            // 检测假死
            if (inactiveTime > TASK_TIMEOUT) {
                handleTaskTimeout(info)
            } else if (inactiveTime > TASK_TIMEOUT / 2) {
                // 警告：任务即将超时
                Log.record(TAG, "⚠️ 任务 ${info.taskId} 响应缓慢，已无活动 ${inactiveTime / 1000}秒")
            }
            
            // 检测异常长时间运行
            val runningTime = now - info.startTime
            if (runningTime > 30 * 60 * 1000) { // 超过30分钟
                Log.record(TAG, "⚠️ 任务 ${info.taskId} 运行时间过长: ${runningTime / 60000}分钟")
            }
        }
    }
    
    /**
     * 处理任务超时
     */
    private fun handleTaskTimeout(info: TaskExecutionInfo) {
        if (info.isHealthy) {
            info.isHealthy = false
            totalTimeouts.incrementAndGet()
            
            Log.record(TAG, "🚨 检测到任务假死: ${info.taskId}")
            Log.record(TAG, "   - 开始时间: ${formatTime(info.startTime)}")
            Log.record(TAG, "   - 最后活跃: ${formatTime(info.lastActiveTime)}")
            Log.record(TAG, "   - 执行次数: ${info.executionCount}")
            
            // 尝试重启任务
            attemptTaskRestart(info.taskId)
        }
    }
    
    /**
     * 尝试重启任务
     */
    private fun attemptTaskRestart(taskId: String) {
        try {
            Log.record(TAG, "🔄 尝试重启假死任务: $taskId")
            
            // 触发任务重启（通过广播）
            fansirsqi.xposed.sesame.hook.ApplicationHook.restartByBroadcast()
            
            totalRestarts.incrementAndGet()
            
            // 移除旧的监控记录
            unregisterTask(taskId)
            
            Log.record(TAG, "✅ 任务重启命令已发送")
            
        } catch (e: Exception) {
            Log.error(TAG, "重启任务失败: ${e.message}")
            Log.printStackTrace(e)
        }
    }
    
    /**
     * 格式化时间
     */
    private fun formatTime(timeMillis: Long): String {
        return fansirsqi.xposed.sesame.util.TimeUtil.getTimeStr(timeMillis)
    }
    
    /**
     * 获取监控统计
     */
    fun getStats(): String {
        val activeTaskCount = monitoredTasks.size
        val unhealthyTaskCount = monitoredTasks.values.count { !it.isHealthy }
        val executions = totalExecutions.get()
        val timeouts = totalTimeouts.get()
        val restarts = totalRestarts.get()
        
        val timeoutRate = if (executions > 0) {
            (timeouts * 100.0 / executions)
        } else {
            0.0
        }
        
        return """
            |任务健康监控统计:
            |  - 活跃任务: $activeTaskCount
            |  - 不健康任务: $unhealthyTaskCount
            |  - 总执行次数: $executions
            |  - 超时次数: $timeouts (${String.format("%.1f", timeoutRate)}%)
            |  - 重启次数: $restarts
        """.trimMargin()
    }
    
    /**
     * 获取任务详情
     */
    fun getTaskDetails(): String {
        if (monitoredTasks.isEmpty()) {
            return "当前无监控任务"
        }
        
        val sb = StringBuilder()
        sb.appendLine("当前监控任务详情:")
        
        monitoredTasks.values.sortedByDescending { it.startTime }.forEach { info ->
            val now = System.currentTimeMillis()
            val runningTime = (now - info.startTime) / 1000
            val inactiveTime = (now - info.lastActiveTime) / 1000
            val status = if (info.isHealthy) "正常" else "异常"
            
            sb.appendLine("  - ${info.taskId}: $status, 运行${runningTime}秒, 最后活跃${inactiveTime}秒前, 执行${info.executionCount}次")
        }
        
        return sb.toString()
    }
    
    /**
     * 重置统计
     */
    fun resetStats() {
        totalExecutions.set(0)
        totalTimeouts.set(0)
        totalRestarts.set(0)
        Log.record(TAG, "已重置任务监控统计")
    }
}

/**
 * BaseTask扩展 - 自动集成健康监控
 */
fun BaseTask.withHealthMonitoring(taskId: String = this.toString()): BaseTask {
    // 注册监控
    TaskHealthMonitor.registerTask(taskId)
    
    // 包装原始run方法
    val originalRun = this::run
    
    // 返回带监控的任务
    return object : BaseTask() {
        override fun run() {
            try {
                // 定期发送心跳
                val heartbeatJob = kotlinx.coroutines.GlobalScope.launch {
                    while (isActive) {
                        TaskHealthMonitor.taskHeartbeat(taskId)
                        kotlinx.coroutines.delay(10000) // 每10秒心跳一次
                    }
                }
                
                // 执行原始任务
                originalRun()
                
                // 取消心跳
                heartbeatJob.cancel()
                
            } finally {
                // 取消注册
                TaskHealthMonitor.unregisterTask(taskId)
            }
        }
        
        override fun check(): Boolean = this@withHealthMonitoring.check()
    }
}
