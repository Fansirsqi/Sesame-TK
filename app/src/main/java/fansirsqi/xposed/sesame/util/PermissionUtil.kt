package fansirsqi.xposed.sesame.util

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlarmManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import fansirsqi.xposed.sesame.data.General
import fansirsqi.xposed.sesame.hook.ApplicationHook
import fansirsqi.xposed.sesame.task.antForest.AntForestRpcCall

/** 权限工具类，用于检查和请求所需权限。  */
object PermissionUtil {
    private val TAG: String = AntForestRpcCall::class.java.getSimpleName()
    private const val REQUEST_EXTERNAL_STORAGE = 1
    private val PERMISSIONS_STORAGE = arrayOf("android.permission.READ_EXTERNAL_STORAGE", "android.permission.WRITE_EXTERNAL_STORAGE")

    /**
     * 检查应用是否具有文件存储权限。
     *
     * @param context 应用上下文。
     * @return 如果权限被授予，返回true，否则返回false。
     */
    fun checkFilePermissions(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11及以上版本，检查是否有管理所有文件的权限
            return Environment.isExternalStorageManager()
        } else {
            // Android 6.0及以上版本，检查读写外部存储的权限
            for (permission in PERMISSIONS_STORAGE) {
                if (context.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                    return false
                }
            }
        }
        return true
    }

    /**
     * 检查或请求文件存储权限。
     *
     * @param activity 发起权限请求的Activity。
     * @return 如果权限被授予，返回true，否则返回false。
     */
    fun checkOrRequestFilePermissions(activity: AppCompatActivity): Boolean {
        if (checkFilePermissions(activity)) {
            return true
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // 请求管理所有文件的权限
                val appIntent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                appIntent.setData(("package:" + activity.packageName).toUri())
                startActivitySafely(activity, appIntent, Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
            } else {
                // 请求外部存储读写权限
                activity.requestPermissions(PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE)
            }
        } catch (e: Exception) {
            Log.printStackTrace(TAG, e)
        }
        return false
    }

    /**
     * 检查应用是否具有闹钟权限。
     *
     * @return 如果权限被授予，返回true，否则返回false。
     */
    @JvmStatic
    fun checkAlarmPermissions(): Boolean {
        val context: Context? = contextSafely
        if (context == null) return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12及以上版本，检查是否可以设置精确闹钟
            val systemService = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager?
            return systemService != null && systemService.canScheduleExactAlarms()
        }
        return true
    }

    /**
     * 检查或请求闹钟权限。
     *
     * @param context 发起权限请求的上下文。
     * @return 如果权限被授予，返回true，否则返回false。
     */
    @JvmStatic
    fun checkOrRequestAlarmPermissions(context: Context): Boolean {
        if (checkAlarmPermissions()) {
            return true
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // 请求设置精确闹钟的权限
                val appIntent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                appIntent.setData(("package:" + General.PACKAGE_NAME).toUri())
                startActivitySafely(context, appIntent, Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            }
        } catch (e: Exception) {
            Log.printStackTrace(TAG, e)
        }
        return false
    }

    /**
     * 检查应用是否具有电池优化豁免权限。
     *
     * @return 如果权限被授予，返回true，否则返回false。
     */
    @JvmStatic
    fun checkBatteryPermissions(): Boolean {
        val context: Context? = contextSafely
        if (context == null) return false
        // 检查是否被豁免电池优化
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager?
        return powerManager != null && powerManager.isIgnoringBatteryOptimizations(General.PACKAGE_NAME)
    }

    /**
     * 检查电池优化豁免权限，但不再直接请求该权限，以符合Google Play政策。
     *
     * @param context 发起检查请求的上下文。
     * @return 如果权限被授予，返回true，否则返回false。
     */
    @JvmStatic
    fun checkOrRequestBatteryPermissions(context: Context): Boolean {
        // 我们不再请求电池优化豁免权限，符合Google Play政策
        try {
            if (checkBatteryPermissions()) {
                return true
            }
            // 跳转到权限页，请求权限
            @SuppressLint("BatteryLife") val appIntent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            appIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            appIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            appIntent.setData(("package:" + General.PACKAGE_NAME).toUri())
            // appIntent.setData(Uri.fromParts("package", General.PACKAGE_NAME, null));
            try {
                context.startActivity(appIntent)
            } catch (_: ActivityNotFoundException) {
                @SuppressLint("BatteryLife") val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            Log.printStackTrace(TAG, e)
        }
        return false
    }

    /**
     * 安全启动Activity的方法，处理启动失败的异常。
     *
     * @param context 用于启动Activity的上下文。
     * @param intent 要启动的Intent。
     * @param fallbackAction 如果第一个Intent失败，使用备用Action启动。
     */
    private fun startActivitySafely(context: Context, intent: Intent, fallbackAction: String?) {
        try {
            // 检查上下文是否为 Activity 类型
            if (context !is Activity) {
                // 如果不是 Activity 类型，添加 FLAG_ACTIVITY_NEW_TASK
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            } else {
                // 如果是 Activity 类型，可以选择是否使用 addFlags()
                // addFlags 用于添加标志，而不会覆盖已有的标志
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            // 尝试启动活动
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            // 如果活动未找到，启动一个回退的 Intent
            val fallbackIntent = Intent(fallbackAction)
            fallbackIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(fallbackIntent)
        } catch (e: Exception) {
            // 处理其他可能的异常
            Log.printStackTrace(e)
        }
    }

    private val contextSafely: Context?
        /**
         * 安全地获取应用上下文，如果未挂钩则返回null。
         *
         * @return 如果存在上下文则返回，否则返回null。
         */
        get() {
            try {
                if (!ApplicationHook.isHooked()) {
                    return null
                }
                return ApplicationHook.getAppContext()
            } catch (_: Exception) {
                return null
            }
        }
}
