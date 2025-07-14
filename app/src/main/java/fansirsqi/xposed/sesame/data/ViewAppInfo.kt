package fansirsqi.xposed.sesame.data

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Bundle
import fansirsqi.xposed.sesame.BuildConfig
import fansirsqi.xposed.sesame.R
import fansirsqi.xposed.sesame.util.Log
import androidx.core.net.toUri

@SuppressLint("StaticFieldLeak")
object ViewAppInfo {
    val TAG: String = ViewAppInfo::class.java.simpleName

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
            appTitle = context.getString(R.string.app_name) //+ BuildConfig.VERSION_NAME
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
        if (runType != null) {
            Log.runtime(TAG, "runType 已设置，直接返回")
            return
        }
        try {
            if (context == null) {
                Log.runtime(TAG, "context 为空，设置 runType 为 DISABLE")
                runType = RunType.DISABLE
                return
            }
            val contentResolver = context!!.contentResolver
            val uri = "content://me.weishu.exposed.CP/".toUri()
            var result: Bundle? = null
            try {
                result = contentResolver.call(uri, "active", null, null)
            } catch (_: RuntimeException) {
                try {
                    val intent = Intent("me.weishu.exp.ACTION_ACTIVE")
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context!!.startActivity(intent)
                } catch (_: Throwable) {
                    runType = RunType.DISABLE
                    return
                }
            }
            if (result == null) {
                result = contentResolver.call(uri, "active", null, null)
            }
            if (result == null) {
                runType = RunType.DISABLE
                return
            }
            if (result.getBoolean("active", false)) {
                runType = RunType.ACTIVE // 激活状态
                return
            }
        } catch (_: Throwable) {
            Log.runtime(TAG, "捕获异常，设置 runType 为 DISABLE")
        }
        runType = RunType.DISABLE
    }



    @JvmStatic
    val isApkInDebug: Boolean
        /**
         * 判断当前应用是否处于调试模式
         *
         * @return 如果应用处于调试模式返回 true，否则返回 false
         */
        get() {
            try {
                val info = context!!.applicationInfo
                return (info.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
            } catch (_: Exception) {
                return false
            }
        }
}
