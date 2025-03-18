package fansirsqi.xposed.sesame.util;

import android.content.Context;
import android.content.pm.PackageManager;

import java.io.File;

import fansirsqi.xposed.sesame.BuildConfig;

public class Detector {


    public static String getLibPath(Context context) {
        String libSesamePath = null;
        try {
            libSesamePath = context.getPackageManager().
                    getApplicationInfo(BuildConfig.APPLICATION_ID, 0).
                    nativeLibraryDir + File.separator + System.mapLibraryName(
                    "checker");
        } catch (PackageManager.NameNotFoundException e) {
            ToastUtil.showToast(context, "请授予支付宝读取芝麻粒的权限");
            Log.record("请授予支付宝读取芝麻粒的权限");
        }
        return libSesamePath;
    }


    public static Boolean loadLibrary(String libraryName) {
        try {
            System.loadLibrary(libraryName);
            return true;
        } catch (UnsatisfiedLinkError e) {
            return false;
        }
    }

    private static native void init(Context context);
    public static native void tips(Context context, String message);

    public static void initDetector(Context context) {
        try {
            init(context);
        } catch (Exception e) {
            Log.error("initDetector", e.getMessage());
        }
    }


    // 只保留一个 native 方法声明
    private static native boolean checkImpl(Context context);

    public static boolean isLspatchDetected(Context context) {
        try {
            return checkImpl(context);
        } catch (Exception e) {
            Log.error("isLspatchDetected", e.getMessage());
            return false;
        }
    }
}