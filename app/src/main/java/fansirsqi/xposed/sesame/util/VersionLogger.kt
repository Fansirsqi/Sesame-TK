package fansirsqi.xposed.sesame.util

import android.content.Context
import android.content.pm.PackageManager
import fansirsqi.xposed.sesame.BuildConfig
import fansirsqi.xposed.sesame.data.General

/**
 * 版本信息记录器
 * 
 * 功能：
 * - 记录芝麻粒版本信息
 * - 记录支付宝版本信息
 * - 记录Android系统版本
 * - 记录设备信息
 * 
 * @author Performance Optimizer
 * @since 2025-10-18
 */
object VersionLogger {
    private const val TAG = "VersionLogger"
    
    /**
     * 版本信息数据类
     */
    data class VersionInfo(
        val moduleVersion: String,
        val moduleBuildType: String,
        val moduleBuildTime: String,
        val alipayVersion: String,
        val alipayVersionCode: Long,
        val androidVersion: String,
        val androidSdk: Int,
        val deviceModel: String,
        val deviceManufacturer: String
    )
    
    /**
     * 当前版本信息（缓存）
     */
    @Volatile
    private var cachedVersionInfo: VersionInfo? = null
    
    /**
     * 获取完整版本信息
     */
    fun getVersionInfo(context: Context?): VersionInfo {
        // 如果已缓存且上下文为空，直接返回缓存
        if (cachedVersionInfo != null && context == null) {
            return cachedVersionInfo!!
        }
        
        // 构建版本信息
        val versionInfo = VersionInfo(
            moduleVersion = BuildConfig.VERSION_NAME,
            moduleBuildType = BuildConfig.BUILD_TYPE,
            moduleBuildTime = "${BuildConfig.BUILD_DATE} ${BuildConfig.BUILD_TIME}",
            alipayVersion = getAlipayVersion(context),
            alipayVersionCode = getAlipayVersionCode(context),
            androidVersion = android.os.Build.VERSION.RELEASE,
            androidSdk = android.os.Build.VERSION.SDK_INT,
            deviceModel = android.os.Build.MODEL,
            deviceManufacturer = android.os.Build.MANUFACTURER
        )
        
        cachedVersionInfo = versionInfo
        return versionInfo
    }
    
    /**
     * 获取支付宝版本名称
     */
    private fun getAlipayVersion(context: Context?): String {
        return try {
            if (context == null) {
                "未知（Context为空）"
            } else {
                val packageInfo = context.packageManager.getPackageInfo(
                    General.PACKAGE_NAME,
                    0
                )
                packageInfo.versionName ?: "未知"
            }
        } catch (e: PackageManager.NameNotFoundException) {
            "未安装"
        } catch (e: Exception) {
            Log.error(TAG, "获取支付宝版本失败: ${e.message}")
            "获取失败"
        }
    }
    
    /**
     * 获取支付宝版本号
     */
    private fun getAlipayVersionCode(context: Context?): Long {
        return try {
            if (context == null) {
                0L
            } else {
                val packageInfo = context.packageManager.getPackageInfo(
                    General.PACKAGE_NAME,
                    0
                )
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    packageInfo.longVersionCode
                } else {
                    @Suppress("DEPRECATION")
                    packageInfo.versionCode.toLong()
                }
            }
        } catch (e: Exception) {
            Log.error(TAG, "获取支付宝版本号失败: ${e.message}")
            0L
        }
    }
    
    /**
     * 记录版本信息到日志
     */
    fun logVersionInfo(context: Context?) {
        try {
            val info = getVersionInfo(context)
            
            Log.record(TAG, "═══════════════ 版本信息 ═══════════════")
            Log.record(TAG, "📦 芝麻粒版本: ${info.moduleVersion}")
            Log.record(TAG, "🏗️ 构建类型: ${info.moduleBuildType}")
            Log.record(TAG, "⏰ 构建时间: ${info.moduleBuildTime}")
            Log.record(TAG, "💰 支付宝版本: ${info.alipayVersion} (${info.alipayVersionCode})")
            Log.record(TAG, "🤖 Android版本: ${info.androidVersion} (API ${info.androidSdk})")
            Log.record(TAG, "📱 设备型号: ${info.deviceManufacturer} ${info.deviceModel}")
            Log.record(TAG, "═══════════════════════════════════════")
            
            // 检查版本兼容性
            checkVersionCompatibility(info)
            
        } catch (e: Exception) {
            Log.error(TAG, "记录版本信息失败: ${e.message}")
            Log.printStackTrace(e)
        }
    }
    
    /**
     * 检查版本兼容性
     */
    private fun checkVersionCompatibility(info: VersionInfo) {
        val warnings = mutableListOf<String>()
        
        // 检查Android版本
        if (info.androidSdk < 21) {
            warnings.add("⚠️ Android版本过低（API ${info.androidSdk}），建议升级到Android 5.0+")
        }
        
        // 检查支付宝版本（使用版本比较，而不是versionCode）
        try {
            val currentVersion = fansirsqi.xposed.sesame.entity.AlipayVersion(info.alipayVersion)
            val minVersion = fansirsqi.xposed.sesame.entity.AlipayVersion("10.3.96")
            if (currentVersion.compareTo(minVersion) < 0) {
                warnings.add("⚠️ 支付宝版本过低（${info.alipayVersion}），建议升级到10.3.96+")
            }
        } catch (e: Exception) {
            Log.debug(TAG, "版本比较失败: ${e.message}")
        }
        
        // 输出警告
        if (warnings.isNotEmpty()) {
            Log.record(TAG, "版本兼容性警告:")
            warnings.forEach { Log.record(TAG, it) }
        } else {
            Log.record(TAG, "✅ 版本兼容性检查通过")
        }
    }
    
    /**
     * 获取版本摘要字符串
     */
    fun getVersionSummary(context: Context?): String {
        val info = getVersionInfo(context)
        return "芝麻粒${info.moduleVersion} | 支付宝${info.alipayVersion} | Android${info.androidVersion}"
    }
    
    /**
     * 清除缓存（强制重新获取）
     */
    fun clearCache() {
        cachedVersionInfo = null
    }
}
