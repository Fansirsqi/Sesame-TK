package fansirsqi.xposed.sesame.data;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;

import fansirsqi.xposed.sesame.BuildConfig;
import fansirsqi.xposed.sesame.R;
import fansirsqi.xposed.sesame.util.Log;
import lombok.Getter;
import lombok.Setter;
/**
 * ViewAppInfo 类用于提供应用信息相关的功能，包括初始化应用信息、检查运行状态、设置运行类型等。
 */
public final class ViewAppInfo {
    @SuppressLint("StaticFieldLeak")
    @Getter
    private static Context context = null;
    // 应用名称
    @Getter
    private static String appTitle = "";
    // 应用版本号
    @Getter
    private static String appVersion = "";
    // 构建目标信息
    @Getter
    private static String appBuildTarget = "";
    // 构建编号
    @Getter
    private static String appBuildNumber = "";
    // 运行状态类型，默认为禁用
    @Setter
    @Getter
    private static RunType runType = RunType.DISABLE;
    /**
     * 初始化 ViewAppInfo，设置应用的相关信息，如版本号、构建日期等
     *
     * @param context 上下文对象，用于获取应用的资源信息
     */
    public static void init(Context context) {
        // 防止重复初始化
        if (ViewAppInfo.context == null) {
            ViewAppInfo.context = context;
            // 此处
            appBuildNumber = String.valueOf(BuildConfig.VERSION_CODE);
            // 设置标题栏-应用名称
//      appTitle = context.getString(R.string.app_name) + "·" + BuildConfig.BUILD_TAG;
//            appTitle = context.getString(R.string.app_name) + "·" + BuildConfig.VERSION_NAME;
            appTitle = context.getString(R.string.app_name) ;
            // 设置构建目标信息
            appBuildTarget = BuildConfig.BUILD_DATE + " " + BuildConfig.BUILD_TIME + " ⏰";
            // 设置版本号
            try {
                appVersion = BuildConfig.VERSION_NAME.replace(BuildConfig.BUILD_TIME.replace(":", "."), BuildConfig.BUILD_NUMBER) + " 📦";
            } catch (Exception e) {
                Log.printStackTrace(e);
            }
        }
    }
    /**
     * 检查当前应用的运行类型，判断是否启用或禁用 通过与 content provider 交互来检查应用是否处于激活状态
     */
    public static void checkRunType() {
        // 如果 runType 已经被设置，则无需再执行检查
        if (runType != null) {
            return;
        }
        try {
            if (context == null) {
                runType = RunType.DISABLE;
                return;
            }
            ContentResolver contentResolver = context.getContentResolver();
            Uri uri = Uri.parse("content://me.weishu.exposed.CP/");
            Bundle result = null;
            try {
                result = contentResolver.call(uri, "active", null, null);
            } catch (RuntimeException e) {
                try {
                    Intent intent = new Intent("me.weishu.exp.ACTION_ACTIVE");
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                } catch (Throwable e1) {
                    runType = RunType.DISABLE;
                    return;
                }
            }
            if (result == null) {
                result = contentResolver.call(uri, "active", null, null);
            }
            if (result == null) {
                runType = RunType.DISABLE;
                return;
            }
            if (result.getBoolean("active", false)) {
                runType = RunType.MODEL;
                return;
            }
            runType = RunType.DISABLE;
        } catch (Throwable ignored) {
        }
        runType = RunType.DISABLE;
    }
    /**
     * 根据运行类型的编码设置当前应用的运行状态
     *
     * @param runTypeCode 运行类型编码
     */
    public static void setRunTypeByCode(Integer runTypeCode) {
        RunType newRunType = RunType.getByCode(runTypeCode);
        // 如果编码无效，则默认为禁用
        if (newRunType == null) {
            newRunType = RunType.DISABLE;
        }
        ViewAppInfo.runType = newRunType;
    }
    /**
     * 判断当前应用是否处于调试模式
     *
     * @return 如果应用处于调试模式返回 true，否则返回 false
     */
    public static boolean isApkInDebug() {
        try {
            ApplicationInfo info = context.getApplicationInfo();
            boolean isDebuggable = (info.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
            if (isDebuggable) {
                Log.system("当前应用[" + info.packageName + "]处于调试模式");
                return true;
            } else {
                Log.system("当前应用[" + info.packageName + "]不处于调试模式");
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }
    /**
     * 判断目标应用是否处于调试模式
     *
     * @param packageName 目标应用的包名
     * @return 如果目标应用处于调试模式返回 true，否则返回 false
     */
    public static boolean isTargetApkInDebug(Context context, String packageName) {
        try {
            ApplicationInfo info = context.getPackageManager().getApplicationInfo(packageName, 0);
            boolean isDebuggable = (info.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
            if (isDebuggable) {
                Log.system("目标应用[" + packageName + "]处于调试模式");
                return true;
            } else {
                Log.system("目标应用[" + packageName + "]不处于调试模式");
                return false;
            }
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
}
