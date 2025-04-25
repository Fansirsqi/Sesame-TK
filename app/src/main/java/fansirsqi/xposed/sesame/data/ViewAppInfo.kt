package fansirsqi.xposed.sesame.data

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.os.Bundle
import fansirsqi.xposed.sesame.BuildConfig
import fansirsqi.xposed.sesame.R
import fansirsqi.xposed.sesame.data.RunType.Companion.getByCode
import fansirsqi.xposed.sesame.util.Log

object ViewAppInfo {
    val TAG: String = ViewAppInfo::class.java.getSimpleName()

    @SuppressLint("StaticFieldLeak")
    var context: Context? = null
    var appTitle: String = ""
    var appVersion: String = ""
    var appBuildTarget: String = ""
    var appBuildNumber: String = ""

    var runType: RunType? = RunType.DISABLE


    /**
     * 初始化 ViewAppInfo，设置应用的相关信息，如版本号、构建日期等
     *
     * @param context 上下文对象，用于获取应用的资源信息
     */
    fun init(context: Context) {
        if (ViewAppInfo.context == null) {
            ViewAppInfo.context = context
            appBuildNumber = BuildConfig.VERSION_CODE.toString()
            appTitle = context.getString(R.string.app_name)
            appBuildTarget = BuildConfig.BUILD_DATE + " " + BuildConfig.BUILD_TIME + " ⏰"
            try {
                appVersion = BuildConfig.VERSION_NAME.replace(
                    BuildConfig.BUILD_TIME.replace(":", "."),
                    BuildConfig.BUILD_NUMBER
                ) + " 📦"
            } catch (e: Exception) {
                Log.printStackTrace(e)
            }
        }
    }

    /**
     * 检查当前应用的运行类型，判断是否启用或禁用 通过与 content provider 交互来检查应用是否处于激活状态
     */
    fun checkRunType() {
        // 如果 runType 已设置，直接返回
        if (runType != null) {
            Log.runtime(TAG, "runType 已设置，直接返回")
            return
        }
        try {
            // 如果 context 为空，设置 runType 为 DISABLE 并返回
            if (context == null) {
                Log.runtime(TAG, "context 为空，设置 runType 为 DISABLE")
                runType = RunType.DISABLE
                return
            }
            // 获取 ContentResolver
            val contentResolver = context!!.getContentResolver()
            Log.runtime(TAG, "获取 ContentResolver")
            // 定义 ContentProvider 的 Uri
            val uri = Uri.parse("content://me.weishu.exposed.CP/")
            Log.runtime(TAG, "解析 Uri: content://me.weishu.exposed.CP/")
            // 调用 ContentProvider，检查应用是否处于激活状态
            var result: Bundle? = null
            try {
                Log.runtime(TAG, "尝试调用 ContentProvider 的 active 方法")
                result = contentResolver.call(uri, "active", null, null)
            } catch (e: RuntimeException) {
                Log.runtime(TAG, "调用 ContentProvider 失败，尝试通过 Intent 启动 Activity")
                try {
                    val intent = Intent("me.weishu.exp.ACTION_ACTIVE")
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context!!.startActivity(intent)
                } catch (e1: Throwable) {
                    Log.runtime(TAG, "启动 Activity 失败，设置 runType 为 DISABLE")
                    runType = RunType.DISABLE
                    return
                }
            }
            // 如果第一次调用失败，尝试再次调用
            if (result == null) {
                Log.runtime(TAG, "第一次调用 ContentProvider 返回 null，尝试再次调用")
                result = contentResolver.call(uri, "active", null, null)
            }
            // 如果仍然失败，设置 runType 为 DISABLE 并返回
            if (result == null) {
                Log.runtime(TAG, "ContentProvider 返回 null，设置 runType 为 DISABLE")
                runType = RunType.DISABLE
                return
            }
            // 根据返回结果设置 runType
            if (result.getBoolean("active", false)) {
                Log.runtime(TAG, "ContentProvider 返回 true，设置 runType 为 MODEL")
                runType = RunType.ACTIVE // 激活状态
                return
            }
            Log.runtime(TAG, "ContentProvider 返回 false，设置 runType 为 DISABLE")
        } catch (ignored: Throwable) {
            Log.runtime(TAG, "捕获异常，设置 runType 为 DISABLE")
        }
        runType = RunType.DISABLE
    }

    /**
     * 根据运行类型的编码设置当前应用的运行状态
     *
     * @param runTypeCode 运行类型编码
     */
    fun setRunTypeByCode(runTypeCode: Int?) {
        var newRunType: RunType?
        if (runTypeCode == null) {          // 处理null情况，直接设置默认值
            newRunType = RunType.DISABLE
        } else {
            newRunType = getByCode(runTypeCode) // 直接传入Integer会自动拆箱为int
            if (newRunType == null) {
                newRunType = RunType.DISABLE
            }
        }
        runType = newRunType
    }

    val isApkInDebug: Boolean
        /**
         * 判断当前应用是否处于调试模式
         *
         * @return 如果应用处于调试模式返回 true，否则返回 false
         */
        get() {
            try {
                val info = context!!.getApplicationInfo()
                return (info.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
            } catch (e: Exception) {
                return false
            }
        }
}
