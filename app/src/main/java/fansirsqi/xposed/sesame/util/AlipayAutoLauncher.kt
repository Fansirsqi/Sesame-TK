package fansirsqi.xposed.sesame.util

import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import fansirsqi.xposed.sesame.util.Log

/**
 * 支付宝自动启动器（优化版本）
 * 
 * 功能：在应用启动时自动后台唤醒支付宝进行能量收取
 * 优化：
 * 1. 使用ADB命令启动支付宝
 * 2. 检查支付宝是否已运行，避免重复启动
 * 3. 优化错误提示逻辑
 * 
 * @author 芝麻粒优化版 v2
 * @date 2025-10-19
 */
object AlipayAutoLauncher {
    private const val TAG = "AlipayAutoLauncher"
    
    // 支付宝包名
    private const val ALIPAY_PACKAGE = "com.eg.android.AlipayGphone"
    
    // 支付宝启动Activity
    private const val ALIPAY_SCHEME_ACTIVITY = "com.alipay.mobile.framework.service.common.SchemeStartActivity"
    
    // 蚂蚁森林AppId
    private const val ANTFOREST_APPID = "60000002"
    
    // 标记是否已经自动启动过（避免重复启动）
    @Volatile
    private var hasAutoLaunched = false
    
    /**
     * 在应用启动时自动唤醒支付宝（优化版本）
     * 
     * 此方法应该在Activity的onResume中调用，而不是Application.onCreate
     * 
     * @param context Activity的Context
     * @param forceRelaunch 是否强制重新启动（默认false，只启动一次）
     */
    @JvmStatic
    fun autoLaunchOnAppStart(context: Context?, forceRelaunch: Boolean = false) {
        // 如果已经启动过且不强制重启，直接返回
        if (hasAutoLaunched && !forceRelaunch) {
            Log.debug(TAG, "已自动启动过支付宝，跳过")
            return
        }
        
        if (context == null) {
            Log.error(TAG, "Context为null，无法启动支付宝")
            return
        }
        
        try {
            // 检查支付宝是否已安装（使用多重检测）
            val alipayStatus = checkAlipayStatus(context)
            if (!alipayStatus.installed) {
                // 只有确认未安装时才输出error日志
                Log.error(TAG, "⚠️ 支付宝未安装，无法自动唤醒")
                Log.error(TAG, "   检测详情: ${alipayStatus.message}")
                hasAutoLaunched = true
                return
            }
            
            // 输出检测结果
            Log.debug(TAG, "✅ 支付宝已安装: ${alipayStatus.message}")
            
            // 检查支付宝是否已在运行
            if (alipayStatus.running) {
                Log.record(TAG, "✅ 支付宝已在运行，跳过启动")
                hasAutoLaunched = true
                return
            }
            
            Log.record(TAG, "🚀 准备后台启动支付宝...")
            
            // 使用AM命令后台启动支付宝（推荐方式）
            val intent = Intent().apply {
                component = ComponentName(ALIPAY_PACKAGE, ALIPAY_SCHEME_ACTIVITY)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                // 不添加FLAG_ACTIVITY_BROUGHT_TO_FRONT，让它在后台启动
            }
            
            // 延迟3秒启动，避免应用启动时卡顿
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                try {
                    context.startActivity(intent)
                    Log.record(TAG, "✅ 已后台启动支付宝（AM命令）")
                    hasAutoLaunched = true
                } catch (e: Exception) {
                    Log.error(TAG, "后台启动支付宝失败: ${e.message}")
                    // 降级方案：使用DeepLink启动
                    try {
                        val deepLinkIntent = Intent(Intent.ACTION_VIEW).apply {
                            data = android.net.Uri.parse("alipays://platformapi/startapp?appId=$ANTFOREST_APPID")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        }
                        context.startActivity(deepLinkIntent)
                        Log.record(TAG, "✅ 已使用DeepLink启动支付宝（降级方案）")
                        hasAutoLaunched = true
                    } catch (ex: Exception) {
                        Log.error(TAG, "DeepLink启动也失败: ${ex.message}")
                    }
                }
            }, 3000)
            
        } catch (e: Exception) {
            Log.error(TAG, "自动唤醒支付宝异常: ${e.message}")
            Log.printStackTrace(TAG, e)
        }
    }
    
    /**
     * 支付宝状态信息
     */
    data class AlipayStatus(
        val installed: Boolean,
        val running: Boolean,
        val version: String,
        val message: String
    )
    
    /**
     * 检查支付宝状态（多重检测）
     */
    private fun checkAlipayStatus(context: Context): AlipayStatus {
        var installed = false
        var running = false
        var version = "未知"
        val messages = mutableListOf<String>()
        
        try {
            // 方法1: 检查包名
            val packageInfo = context.packageManager.getPackageInfo(ALIPAY_PACKAGE, 0)
            installed = true
            version = packageInfo.versionName ?: "未知"
            messages.add("版本${version}")
            
            // 方法2: 检查进程状态
            running = isAlipayRunning(context)
            if (running) {
                messages.add("正在运行")
            } else {
                messages.add("未运行")
            }
            
        } catch (e: Exception) {
            // 方法3: 尝试查询应用信息
            try {
                val appInfo = context.packageManager.getApplicationInfo(ALIPAY_PACKAGE, 0)
                installed = true
                version = "已安装"
                messages.add("通过应用信息检测")
            } catch (ex: Exception) {
                installed = false
                messages.add("检测失败: ${e.message}")
            }
        }
        
        val message = messages.joinToString(", ")
        return AlipayStatus(installed, running, version, message)
    }
    
    /**
     * 检查支付宝是否正在运行
     */
    private fun isAlipayRunning(context: Context): Boolean {
        return try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            if (activityManager == null) {
                Log.debug(TAG, "ActivityManager为null")
                return false
            }
            
            // Android 5.0+ 使用不同的方法
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val runningAppProcesses = activityManager.runningAppProcesses
                if (runningAppProcesses != null) {
                    for (processInfo in runningAppProcesses) {
                        if (processInfo.processName == ALIPAY_PACKAGE) {
                            Log.debug(TAG, "支付宝进程正在运行")
                            return true
                        }
                    }
                }
            } else {
                // Android 5.0以下使用旧方法
                try {
                    @Suppress("DEPRECATION")
                    val runningTasks = activityManager.getRunningTasks(100)
                    for (taskInfo in runningTasks) {
                        if (taskInfo.baseActivity?.packageName == ALIPAY_PACKAGE) {
                            Log.debug(TAG, "支付宝任务正在运行")
                            return true
                        }
                    }
                } catch (e: SecurityException) {
                    Log.debug(TAG, "无权限查询运行任务")
                }
            }
            
            Log.debug(TAG, "支付宝未运行")
            false
        } catch (e: Exception) {
            Log.debug(TAG, "检查支付宝运行状态异常: ${e.message}")
            false
        }
    }
    
    /**
     * 重置自动启动标志（用于测试或重新启动）
     */
    @JvmStatic
    fun resetAutoLaunchFlag() {
        hasAutoLaunched = false
        Log.debug(TAG, "已重置自动启动标志")
    }
    
    /**
     * 手动启动支付宝到蚂蚁森林（立即启动，无延迟）
     */
    @JvmStatic
    fun launchAlipayToForest(context: Context?) {
        if (context == null) {
            Log.error(TAG, "Context为null，无法启动支付宝")
            return
        }
        
        try {
            val alipayStatus = checkAlipayStatus(context)
            if (!alipayStatus.installed) {
                Log.record(TAG, "⚠️ 支付宝未安装")
                return
            }
            
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = android.net.Uri.parse("alipays://platformapi/startapp?appId=$ANTFOREST_APPID")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            
            context.startActivity(intent)
            Log.record(TAG, "✅ 已启动支付宝到蚂蚁森林")
        } catch (e: Exception) {
            Log.error(TAG, "手动启动支付宝失败: ${e.message}")
        }
    }
}
