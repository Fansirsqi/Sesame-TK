package fansirsqi.xposed.sesame.util;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.PowerManager;
import android.provider.Settings;

import androidx.appcompat.app.AppCompatActivity;

import fansirsqi.xposed.sesame.data.General;
import fansirsqi.xposed.sesame.hook.ApplicationHook;
import fansirsqi.xposed.sesame.task.antForest.AntForestRpcCall;
/** 权限工具类，用于检查和请求所需权限。 */
public class PermissionUtil {
  private static final String TAG = AntForestRpcCall.class.getSimpleName();
  private static final int REQUEST_EXTERNAL_STORAGE = 1;
  private static final String[] PERMISSIONS_STORAGE = {"android.permission.READ_EXTERNAL_STORAGE", "android.permission.WRITE_EXTERNAL_STORAGE"};
  /**
   * 检查应用是否具有文件存储权限。
   *
   * @param context 应用上下文。
   * @return 如果权限被授予，返回true，否则返回false。
   */
  public static boolean checkFilePermissions(Context context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      // Android 11及以上版本，检查是否有管理所有文件的权限
      return Environment.isExternalStorageManager();
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      // Android 6.0及以上版本，检查读写外部存储的权限
      for (String permission : PERMISSIONS_STORAGE) {
        if (context.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
          return false;
        }
      }
    }
    return true;
  }
  /**
   * 检查或请求文件存储权限。
   *
   * @param activity 发起权限请求的Activity。
   * @return 如果权限被授予，返回true，否则返回false。
   */
  public static Boolean checkOrRequestFilePermissions(AppCompatActivity activity) {
    if (checkFilePermissions(activity)) {
      return true;
    }
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        // 请求管理所有文件的权限
        Intent appIntent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
        appIntent.setData(Uri.parse("package:" + activity.getPackageName()));
        startActivitySafely(activity, appIntent, Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
      } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        // 请求外部存储读写权限
        activity.requestPermissions(PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);
      }
    } catch (Exception e) {
      Log.printStackTrace(TAG, e);
    }
    return false;
  }
  /**
   * 检查应用是否具有闹钟权限。
   *
   * @return 如果权限被授予，返回true，否则返回false。
   */
  public static boolean checkAlarmPermissions() {
    Context context = getContextSafely();
    if (context == null) return false;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      // Android 12及以上版本，检查是否可以设置精确闹钟
      AlarmManager systemService = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
      return systemService != null && systemService.canScheduleExactAlarms();
    }
    return true;
  }
  /**
   * 检查或请求闹钟权限。
   *
   * @param context 发起权限请求的上下文。
   * @return 如果权限被授予，返回true，否则返回false。
   */
  public static Boolean checkOrRequestAlarmPermissions(Context context) {
    if (checkAlarmPermissions()) {
      return true;
    }
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        // 请求设置精确闹钟的权限
        Intent appIntent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
        appIntent.setData(Uri.parse("package:" + General.PACKAGE_NAME));
        startActivitySafely(context, appIntent, Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
      }
    } catch (Exception e) {
      Log.printStackTrace(TAG, e);
    }
    return false;
  }
  /**
   * 检查应用是否具有电池优化豁免权限。
   *
   * @return 如果权限被授予，返回true，否则返回false。
   */
  public static boolean checkBatteryPermissions() {
    Context context = getContextSafely();
    if (context == null) return false;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      // 检查是否被豁免电池优化
      PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
      return powerManager != null && powerManager.isIgnoringBatteryOptimizations(General.PACKAGE_NAME);
    }
    return true;
  }
  /**
   * 检查电池优化豁免权限，但不再直接请求该权限，以符合Google Play政策。
   *
   * @param context 发起检查请求的上下文。
   * @return 如果权限被授予，返回true，否则返回false。
   */
  public static Boolean checkOrRequestBatteryPermissions(Context context) {
    // 我们不再请求电池优化豁免权限，符合Google Play政策
    try {
      if (checkBatteryPermissions()) {
        return true;
      }
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        // 跳转到权限页，请求权限
        @SuppressLint("BatteryLife")
        Intent appIntent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
        appIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        appIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        appIntent.setData(Uri.parse("package:" + General.PACKAGE_NAME));
        // appIntent.setData(Uri.fromParts("package", General.PACKAGE_NAME, null));
        try {
          context.startActivity(appIntent);
        } catch (ActivityNotFoundException ex) {
          @SuppressLint("BatteryLife")
          Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
          intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
          intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
          context.startActivity(intent);
        }
      }
    } catch (Exception e) {
      Log.printStackTrace(TAG, e);
    }
    return false;
  }
  /**
   * 安全启动Activity的方法，处理启动失败的异常。
   *
   * @param context 用于启动Activity的上下文。
   * @param intent 要启动的Intent。
   * @param fallbackAction 如果第一个Intent失败，使用备用Action启动。
   */
  private static void startActivitySafely(Context context, Intent intent, String fallbackAction) {
    try {
      // 检查上下文是否为 Activity 类型
      if (!(context instanceof Activity)) {
        // 如果不是 Activity 类型，添加 FLAG_ACTIVITY_NEW_TASK
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      } else {
        // 如果是 Activity 类型，可以选择是否使用 addFlags()
        // addFlags 用于添加标志，而不会覆盖已有的标志
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      }
      // 尝试启动活动
      context.startActivity(intent);
    } catch (ActivityNotFoundException ex) {
      // 如果活动未找到，启动一个回退的 Intent
      Intent fallbackIntent = new Intent(fallbackAction);
      fallbackIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      context.startActivity(fallbackIntent);
    } catch (Exception e) {
      // 处理其他可能的异常
      Log.printStackTrace(e);
    }
  }
  /**
   * 安全地获取应用上下文，如果未挂钩则返回null。
   *
   * @return 如果存在上下文则返回，否则返回null。
   */
  private static Context getContextSafely() {
    try {
      if (!ApplicationHook.isHooked()) {
        return null;
      }
      return ApplicationHook.getAppContext();
    } catch (Exception e) {
      return null;
    }
  }
}
