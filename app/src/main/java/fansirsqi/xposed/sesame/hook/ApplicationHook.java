package fansirsqi.xposed.sesame.hook;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AndroidAppHelper;
import android.app.Application;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import androidx.annotation.NonNull;
import org.json.JSONException;
import org.json.JSONObject;
import org.luckypray.dexkit.DexKitBridge;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import fansirsqi.xposed.sesame.BuildConfig;
import fansirsqi.xposed.sesame.data.Config;
import fansirsqi.xposed.sesame.data.DataCache;
import fansirsqi.xposed.sesame.data.General;
import fansirsqi.xposed.sesame.data.RunType;
import fansirsqi.xposed.sesame.data.Statistics;
import fansirsqi.xposed.sesame.data.Status;
import fansirsqi.xposed.sesame.data.ViewAppInfo;
import fansirsqi.xposed.sesame.entity.AlipayVersion;
import fansirsqi.xposed.sesame.entity.FriendWatch;
import fansirsqi.xposed.sesame.hook.rpc.bridge.NewRpcBridge;
import fansirsqi.xposed.sesame.hook.rpc.bridge.OldRpcBridge;
import fansirsqi.xposed.sesame.hook.rpc.bridge.RpcBridge;
import fansirsqi.xposed.sesame.hook.rpc.bridge.RpcVersion;
import fansirsqi.xposed.sesame.hook.rpc.debug.DebugRpc;
import fansirsqi.xposed.sesame.hook.rpc.intervallimit.RpcIntervalLimit;
import fansirsqi.xposed.sesame.model.BaseModel;
import fansirsqi.xposed.sesame.model.Model;
import fansirsqi.xposed.sesame.task.BaseTask;
import fansirsqi.xposed.sesame.task.ModelTask;
import fansirsqi.xposed.sesame.task.TaskCommon;
import fansirsqi.xposed.sesame.task.antMember.AntMemberRpcCall;
import fansirsqi.xposed.sesame.task.recreation.Recreation;
import fansirsqi.xposed.sesame.util.AssetUtil;
import fansirsqi.xposed.sesame.util.Detector;
import fansirsqi.xposed.sesame.util.GlobalThreadPools;
import fansirsqi.xposed.sesame.util.JsonUtil;
import fansirsqi.xposed.sesame.util.Log;
import fansirsqi.xposed.sesame.util.Maps.UserMap;
import fansirsqi.xposed.sesame.util.Notify;
import fansirsqi.xposed.sesame.util.PermissionUtil;
import fansirsqi.xposed.sesame.util.StringUtil;
import fansirsqi.xposed.sesame.util.TimeUtil;
import lombok.Getter;

public class ApplicationHook implements IXposedHookLoadPackage {
    static final String TAG = ApplicationHook.class.getSimpleName();
    @Getter
    public static final String modelVersion = BuildConfig.VERSION_NAME;
    public static final Map<Object, Object[]> rpcHookMap = new ConcurrentHashMap<>();
    public static final Map<String, PendingIntent> wakenAtTimeAlarmMap = new ConcurrentHashMap<>();
    @Getter
    public static ClassLoader classLoader = null;
    @Getter
    public static Object microApplicationContextObject = null;
    @Getter
    @SuppressLint("StaticFieldLeak")
    static Context context = null;
    @Getter
    public static AlipayVersion alipayVersion = new AlipayVersion("");
    @Getter
    public static volatile boolean hooked = false;
    public static volatile boolean init = false;
    public static volatile Calendar dayCalendar;
    @Getter
    public static volatile boolean offline = false;

    @Getter
    public static final AtomicInteger reLoginCount = new AtomicInteger(0);
    @SuppressLint("StaticFieldLeak")
    public static Service service;
    @Getter
    public static Handler mainHandler;
    public static BaseTask mainTask;
    public static RpcBridge rpcBridge;
    @Getter
    public static RpcVersion rpcVersion;
    public static PowerManager.WakeLock wakeLock;
    public static PendingIntent alarm0Pi;
    public static XC_MethodHook.Unhook rpcRequestUnhook;
    public static XC_MethodHook.Unhook rpcResponseUnhook;
    public final ExecutorService MAIN_THREAD_POOL = GlobalThreadPools.getGeneralPurposeExecutor();

    public static void setOffline(boolean offline) {
        ApplicationHook.offline = offline;
    }

    private volatile long lastExecTime = 0; // 添加为类成员变量

     static {
        // 初始化dayCalendar
        dayCalendar = Calendar.getInstance();
        dayCalendar.set(Calendar.HOUR_OF_DAY, 0);
        dayCalendar.set(Calendar.MINUTE, 0);
        dayCalendar.set(Calendar.SECOND, 0);
    }

    /**
     * 执行检查方法
     *
     * @return true表示检查失败，false表示检查成功
     */
    private boolean executeCheckTask(long lastExecTime) {
        FutureTask<Boolean> checkTask = new FutureTask<>(AntMemberRpcCall::check);
        MAIN_THREAD_POOL.submit(checkTask); // Submit the task

        try {
            // Attempt to get the result with a timeout
            boolean taskResult = checkTask.get(10, TimeUnit.SECONDS);

            if (!taskResult) {
                // AntMemberRpcCall.check() completed and returned false
                Log.record(TAG, "执行失败：检查逻辑返回false");
                // The original code had a peculiar sleep here.
                // This calculation for waitTime is suspicious and might need review by the user.
                // For now, we include the interrupt handling for this sleep.
                long waitTime = 10000 - System.currentTimeMillis() + lastExecTime;
                if (waitTime > 0) {
                    try {
                        Thread.sleep(waitTime);
                    } catch (InterruptedException ie) {
                        Log.record(TAG, "执行失败：在额外等待期间被中断");
                        Thread.currentThread().interrupt(); // Preserve interrupt status
                        return true; // Treat as failure
                    }
                }
                return true; // Failure because check() returned false
            }

            // Task completed successfully and returned true
            reLoginCount.set(0);
            return false; // Success

        } catch (InterruptedException e) {
            Log.record(TAG, "执行失败：等待检查结果时被中断");
            Thread.currentThread().interrupt(); // Preserve interrupt status
            checkTask.cancel(true); // Attempt to cancel the underlying task as well
            return true; // Treat as failure
        } catch (TimeoutException e) {
            Log.record(TAG, "执行失败：检查操作超时 (10秒)");
            checkTask.cancel(true); // Attempt to cancel the underlying task
            return true; // Treat as failure
        } catch (ExecutionException e) {
            Log.record(TAG, "执行失败：检查操作内部抛出异常");
            Throwable cause = e.getCause();
            Log.printStackTrace(TAG, cause != null ? cause : e);
            if (cause instanceof InterruptedException) {
                // If the wrapped exception was InterruptedException, ensure current thread is interrupted
                Thread.currentThread().interrupt();
            }
            return true; // Treat as failure
        } catch (
                Exception e) { // Catch-all for other unexpected errors, e.g. submitting task or CancellationException
            Log.record(TAG, "执行失败：处理检查任务时发生未知错误");
            Log.printStackTrace(TAG, e);
            if (e instanceof CancellationException) {
                Log.record(TAG, "执行失败：检查任务已被取消");
            }
            return true; // Treat as failure
        }
    }

    /**
     * 调度定时执行
     *
     * @param lastExecTime 上次执行时间
     */
    private void scheduleNextExecution(long lastExecTime) {
        try {
            int checkInterval = BaseModel.getCheckInterval().getValue();
            List<String> execAtTimeList = BaseModel.getExecAtTimeList().getValue();
            if (execAtTimeList != null && execAtTimeList.contains("-1")) {
                Log.record(TAG, "定时执行未开启");
                return;
            }
            try {
                if (execAtTimeList != null) {
                    Calendar lastExecTimeCalendar = TimeUtil.getCalendarByTimeMillis(lastExecTime);
                    Calendar nextExecTimeCalendar = TimeUtil.getCalendarByTimeMillis(lastExecTime + checkInterval);
                    for (String execAtTime : execAtTimeList) {
                        Calendar execAtTimeCalendar = TimeUtil.getTodayCalendarByTimeStr(execAtTime);
                        if (execAtTimeCalendar != null && lastExecTimeCalendar.compareTo(execAtTimeCalendar) < 0 && nextExecTimeCalendar.compareTo(execAtTimeCalendar) > 0) {
                            Log.record(TAG, "设置定时执行:" + execAtTime);
                            execDelayedHandler(execAtTimeCalendar.getTimeInMillis() - lastExecTime);
                            return;
                        }
                    }
                }
            } catch (Exception e) {
                Log.runtime(TAG, "execAtTime err:：" + e.getMessage());
                Log.printStackTrace(TAG, e);
            }
            execDelayedHandler(checkInterval);
        } catch (Exception e) {
            Log.runtime(TAG, "scheduleNextExecution：" + e.getMessage());
            Log.printStackTrace(TAG, e);
        }
    }


    @SuppressLint("UnsafeDynamicallyLoadedCode")
    private void loadNativeLibs(Context context, File soFile) {
        try {
            String soPath = context.getApplicationInfo().dataDir + File.separator + "lib" + File.separator + soFile.getName();
            if (AssetUtil.INSTANCE.copyDtorageSoFileToPrivateDir(context, soFile)) {
                System.load(soPath);
            } else {
                Detector.INSTANCE.loadLibrary("checker");
            }
            Log.runtime(TAG, "Loading " + soFile.getName() + " from :" + soPath);
        } catch (Exception e) {
            Log.error(TAG, "载入so库失败！！");
            Log.printStackTrace(e);
        }
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        if (General.MODULE_PACKAGE_NAME.equals(loadPackageParam.packageName)) {
            try {
                ViewAppInfo.setRunType(RunType.ACTIVE);
                Log.runtime(TAG, "handleLoadPackage setRunType: " + ViewAppInfo.getRunType());
            } catch (Exception e) {
                Log.printStackTrace(e);
            }
        } else if (General.PACKAGE_NAME.equals(loadPackageParam.packageName) && General.PACKAGE_NAME.equals(loadPackageParam.processName)) {
               if (hooked) return;
            classLoader = loadPackageParam.classLoader;
            XposedHelpers.findAndHookMethod(Application.class, "attach", Context.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    context = (Context) param.args[0];
                    try {
                        PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
                        assert pInfo.versionName != null;
                        alipayVersion = new AlipayVersion(pInfo.versionName);
                        Log.runtime(TAG, "handleLoadPackage alipayVersion: " + alipayVersion.getVersionString());
                        loadNativeLibs(context, AssetUtil.INSTANCE.getCheckerDestFile());
                        loadNativeLibs(context, AssetUtil.INSTANCE.getDexkitDestFile());
                        HookUtil.INSTANCE.fuckAccounLimit(loadPackageParam);
                    } catch (Exception e) {
                        Log.printStackTrace(e);
                    }
                    super.afterHookedMethod(param);
                }
            });


            try {
                XposedHelpers.findAndHookMethod("com.alipay.mobile.quinox.LauncherActivity", classLoader, "onResume",
                        new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(MethodHookParam param) {
                                Log.runtime(TAG, "hook onResume after start");
                                String targetUid = getUserId();
                                if (targetUid == null) {
                                    Log.record(TAG, "onResume:用户未登录");
                                    Toast.show("用户未登录");
                                    return;
                                }
                                if (!init) {
                                    if (initHandler(true)) {
                                        init = true;
                                    }
                                    Log.runtime(TAG, "initHandler success");
                                    return;
                                }
                                String currentUid = UserMap.getCurrentUid();
                                if (!targetUid.equals(currentUid)) {
                                    if (currentUid != null) {
                                        initHandler(true);
                                        Log.record(TAG, "用户已切换");
                                        Toast.show("用户已切换");
                                        return;
                                    }
                                    UserMap.initUser(targetUid);
                                }
                                if (offline) {
                                    offline = false;
                                    execHandler();
                                    ((Activity) param.thisObject).finish();
                                    Log.runtime(TAG, "Activity reLogin");
                                }
                                Log.runtime(TAG, "hook onResume after end");
                            }
                        });
                Log.runtime(TAG, "hook login successfully");
            } catch (Throwable t) {
                Log.runtime(TAG, "hook login err");
                Log.printStackTrace(TAG, t);
            }
            //hook "android.app.Service" 类的onCreate方法
            try {
                XposedHelpers.findAndHookMethod("android.app.Service", classLoader, "onCreate",
                        new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(MethodHookParam param) {
                                Service appService = (Service) param.thisObject;
                                if (!General.CURRENT_USING_SERVICE.equals(appService.getClass().getCanonicalName())) {
                                    return;
                                }
                                Log.runtime(TAG, "Service onCreate");
                                context = appService.getApplicationContext();
                                boolean isok = Detector.INSTANCE.isLegitimateEnvironment(context);
                                if (isok) {
                                    Detector.INSTANCE.dangerous(context);
                                    return;
                                }
                                String packageName = loadPackageParam.packageName;
                                String apkPath = loadPackageParam.appInfo.sourceDir;
                                try (DexKitBridge bridge = DexKitBridge.create(apkPath)) {
                                    // Other use cases
                                    Log.runtime(TAG, "hook dexkit successfully");
                                }
                                service = appService;
                                mainHandler = new Handler(Looper.getMainLooper());
                                AtomicReference<String> UserId = new AtomicReference<>();
//                                if (!MMKV.isInitialized()) {
//                                    MMKV.initialize(AndroidAppHelper.currentApplication());
//                                }

                                mainTask = BaseTask.newInstance("MAIN_TASK", () -> {
                                    try {
                                        TaskCommon.update();
                                        if (TaskCommon.IS_MODULE_SLEEP_TIME) {
                                            Log.record(TAG, "️💤跳过执行-休眠时间");
                                            return;
                                        }
                                        if (!init) {
                                            Log.record(TAG, "️🐣跳过执行-未初始化");
                                            return;
                                        }
                                        if (!Config.isLoaded()) {
                                            Log.record(TAG, "️⚙跳过执行-用户模块配置未加载");
                                            return;
                                        }
                                        Log.record(TAG, "开始执行");
                                        long currentTime = System.currentTimeMillis();
                                        if (lastExecTime + 2000 > currentTime) {
                                            Log.record(TAG, "执行间隔较短，跳过执行");
                                            execDelayedHandler(BaseModel.getCheckInterval().getValue());
                                            return;
                                        }
                                        String currentUid = UserMap.getCurrentUid();
                                        String targetUid = getUserId();
                                        if (targetUid == null || !targetUid.equals(currentUid)) {
                                            Log.record(TAG, "用户切换或为空，重新登录");
                                            reLogin();
                                            return;
                                        }
                                        lastExecTime = currentTime; // 更新最后执行时间
//                                        if (executeCheckTask(lastExecTime)) {
//                                            reLogin();
//                                            return;
//                                        }
                                        ModelTask.startAllTask(false);
                                        scheduleNextExecution(lastExecTime);
                                        UserId.set(targetUid);
                                    } catch (Exception e) {
                                        Log.record(TAG, "❌执行异常");
                                        Log.printStackTrace(TAG, e);
                                    }
                                });
                                registerBroadcastReceiver(appService);
                                Statistics.load();
                                FriendWatch.load(UserId.get());
                                dayCalendar = Calendar.getInstance();
                                if (initHandler(true)) {
                                    init = true;
                                }
                            }
                        }

                );
                Log.runtime(TAG, "hook service onCreate successfully");
            } catch (Throwable t) {
                Log.runtime(TAG, "hook service onCreate err");
                Log.printStackTrace(TAG, t);
            }


            //
            try {
                XposedHelpers.findAndHookMethod("android.app.Service", classLoader, "onDestroy",
                        new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(MethodHookParam param) {
                                Service service = (Service) param.thisObject;
                                if (!General.CURRENT_USING_SERVICE.equals(service.getClass().getCanonicalName()))
                                    return;
                                Log.record(TAG, "支付宝前台服务被销毁");
                                Notify.updateStatusText("支付宝前台服务被销毁");
                                destroyHandler(true);
                                FriendWatch.unload();
                                Statistics.unload();
                                restartByBroadcast();
                            }
                        });
            } catch (Throwable t) {
                Log.runtime(TAG, "hook service onDestroy err");
                Log.printStackTrace(TAG, t);
            }
            try {
                XposedHelpers.findAndHookMethod("com.alipay.mobile.common.fgbg.FgBgMonitorImpl", classLoader, "isInBackground",
                        XC_MethodReplacement.returnConstant(false));
            } catch (Throwable t) {
                Log.runtime(TAG, "hook FgBgMonitorImpl method 1 err");
                Log.printStackTrace(TAG, t);
            }
            try {
                XposedHelpers.findAndHookMethod("com.alipay.mobile.common.fgbg.FgBgMonitorImpl", classLoader, "isInBackground",
                        boolean.class, XC_MethodReplacement.returnConstant(false));
            } catch (Throwable t) {
                Log.runtime(TAG, "hook FgBgMonitorImpl method 2 err");
                Log.printStackTrace(TAG, t);
            }
            try {
                XposedHelpers.findAndHookMethod("com.alipay.mobile.common.fgbg.FgBgMonitorImpl", classLoader, "isInBackgroundV2",
                        XC_MethodReplacement.returnConstant(false));
            } catch (Throwable t) {
                Log.runtime(TAG, "hook FgBgMonitorImpl method 3 err");
                Log.printStackTrace(TAG, t);
            }
            try {
                XposedHelpers.findAndHookMethod("com.alipay.mobile.common.transport.utils.MiscUtils", classLoader, "isAtFrontDesk",
                        classLoader.loadClass("android.content.Context"), XC_MethodReplacement.returnConstant(true));
                Log.runtime(TAG, "hook MiscUtils successfully");
            } catch (Throwable t) {
                Log.runtime(TAG, "hook MiscUtils err");
                Log.printStackTrace(TAG, t);
            }
            hooked = true;
            Log.runtime(TAG, "load success: " + loadPackageParam.packageName);
        }
    }

    private static void setWakenAtTimeAlarm() {
        try {
            List<String> wakenAtTimeList = BaseModel.getWakenAtTimeList().getValue();
            if (wakenAtTimeList != null && wakenAtTimeList.contains("-1")) {
                Log.record(TAG, "定时唤醒未开启");
                return;
            }
            unsetWakenAtTimeAlarm();
            try {
                PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, new Intent("com.eg.android.AlipayGphone.sesame.execute"),
                        getPendingIntentFlag());
                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.DAY_OF_MONTH, 1);
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                if (setAlarmTask(calendar.getTimeInMillis(), pendingIntent)) {
                    alarm0Pi = pendingIntent;
                    Log.record(TAG, "⏰ 设置定时唤醒:0|000000");
                }
            } catch (Exception e) {
                Log.runtime(TAG, "setWakenAt0 err:");
                Log.printStackTrace(TAG, e);
            }
            if (wakenAtTimeList != null && !wakenAtTimeList.isEmpty()) {
                Calendar nowCalendar = Calendar.getInstance();
                for (int i = 1, len = wakenAtTimeList.size(); i < len; i++) {
                    try {
                        String wakenAtTime = wakenAtTimeList.get(i);
                        Calendar wakenAtTimeCalendar = TimeUtil.getTodayCalendarByTimeStr(wakenAtTime);
                        if (wakenAtTimeCalendar != null) {
                            if (wakenAtTimeCalendar.compareTo(nowCalendar) > 0) {
                                PendingIntent wakenAtTimePendingIntent = PendingIntent.getBroadcast(context, i, new Intent("com.eg.android.AlipayGphone" +
                                        ".sesame.execute"), getPendingIntentFlag());
                                if (setAlarmTask(wakenAtTimeCalendar.getTimeInMillis(), wakenAtTimePendingIntent)) {
                                    String wakenAtTimeKey = i + "|" + wakenAtTime;
                                    wakenAtTimeAlarmMap.put(wakenAtTimeKey, wakenAtTimePendingIntent);
                                    Log.record(TAG, "⏰ 设置定时唤醒:" + wakenAtTimeKey);
                                }
                            }
                        }
                    } catch (Exception e) {
                        Log.runtime(TAG, "setWakenAtTime err:");
                        Log.printStackTrace(TAG, e);
                    }
                }
            }
        } catch (Exception e) {
            Log.runtime(TAG, "setWakenAtTimeAlarm err:");
            Log.printStackTrace(TAG, e);
        }
    }

    private static void unsetWakenAtTimeAlarm() {
        try {
            for (Map.Entry<String, PendingIntent> entry : wakenAtTimeAlarmMap.entrySet()) {
                try {
                    String wakenAtTimeKey = entry.getKey();
                    PendingIntent wakenAtTimePendingIntent = entry.getValue();
                    if (unsetAlarmTask(wakenAtTimePendingIntent)) {
                        wakenAtTimeAlarmMap.remove(wakenAtTimeKey);
                        Log.record(TAG, "⏰ 取消定时唤醒:" + wakenAtTimeKey);
                    }
                } catch (Exception e) {
                    Log.runtime(TAG, "unsetWakenAtTime err:");
                    Log.printStackTrace(TAG, e);
                }
            }
            try {
                if (unsetAlarmTask(alarm0Pi)) {
                    alarm0Pi = null;
                    Log.record(TAG, "⏰ 取消定时唤醒:0|000000");
                }
            } catch (Exception e) {
                Log.runtime(TAG, "unsetWakenAt0 err:");
                Log.printStackTrace(TAG, e);
            }
        } catch (Exception e) {
            Log.runtime(TAG, "unsetWakenAtTimeAlarm err:");
            Log.printStackTrace(TAG, e);
        }
    }

    @SuppressLint("WakelockTimeout")
    private static synchronized Boolean initHandler(Boolean force) {
        try {
            TaskCommon.update();
            if (service == null) {
                return false;
            }
            if (TaskCommon.IS_MODULE_SLEEP_TIME) {
                Log.record(TAG, "💤 模块休眠中,停止初始化");
                return false;
            }
            destroyHandler(force);
            if (force) {
                String userId = getUserId();
                if (userId == null) {
                    Log.record(TAG, "initHandler:用户未登录");
                    Toast.show("initHandler:用户未登录");
                    return false;
                }
                UserMap.initUser(userId);
                Model.initAllModel();
                String startMsg = "芝麻粒-TK 开始初始化...";
                Log.record(TAG, startMsg);
                Log.record(TAG, "⚙️模块版本：" + modelVersion);
                Log.record(TAG, "📦应用版本：" + alipayVersion.getVersionString());
                Config.load(userId);
                if (!Config.isLoaded()) {
                    Log.record(TAG, "用户模块配置加载失败");
                    Toast.show("用户模块配置加载失败");
                    return false;
                }
                // ！！所有权限申请应该放在加载配置之后

                //闹钟权限申请
                if (!PermissionUtil.checkAlarmPermissions()) {
                    Log.record(TAG, "❌ 支付宝无闹钟权限");
                    mainHandler.postDelayed(
                            () -> {
                                if (!PermissionUtil.checkOrRequestAlarmPermissions(context)) {
                                    Toast.show("请授予支付宝使用闹钟权限");
                                }
                            },
                            2000);
                    return false;
                }
                // 检查并请求后台运行权限
                if (BaseModel.getBatteryPerm().getValue() && !init && !PermissionUtil.checkBatteryPermissions()) {
                    Log.record(TAG, "支付宝无始终在后台运行权限");
                    mainHandler.postDelayed(
                            () -> {
                                if (!PermissionUtil.checkOrRequestBatteryPermissions(context)) {
                                    Toast.show("请授予支付宝始终在后台运行权限");
                                }
                            },
                            2000);
                }
                Notify.start(service);
                // 获取 BaseModel 实例
                BaseModel baseModel = Model.getModel(BaseModel.class);
                if (baseModel == null) {
                    Log.error(TAG, "BaseModel 未找到 初始化失败");
                    Notify.setStatusTextDisabled();
                    return false;
                }
                // 检查 enableField 的值
                if (!baseModel.getEnableField().getValue()) {
                    Log.record(TAG, "❌ 芝麻粒已禁用");
                    Toast.show("❌ 芝麻粒已禁用");
                    Notify.setStatusTextDisabled();
                    return false;
                }
                // 保持唤醒锁，防止设备休眠
                if (BaseModel.getStayAwake().getValue()) {
                    try {
                        PowerManager pm = (PowerManager) service.getSystemService(Context.POWER_SERVICE);
                        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, service.getClass().getName());
                        wakeLock.acquire(); // 确保唤醒锁在前台服务启动前
                    } catch (Throwable t) {
                        Log.record(TAG, "唤醒锁申请失败:");
                        Log.printStackTrace(t);
                    }
                }
                setWakenAtTimeAlarm();
                if (BaseModel.getNewRpc().getValue()) {
                    rpcBridge = new NewRpcBridge();
                } else {
                    rpcBridge = new OldRpcBridge();
                }
                rpcBridge.load();
                rpcVersion = rpcBridge.getVersion();
                if (BaseModel.getNewRpc().getValue() && BaseModel.getDebugMode().getValue()) {
                    try {
                        rpcRequestUnhook = XposedHelpers.findAndHookMethod(
                                "com.alibaba.ariver.commonability.network.rpc.RpcBridgeExtension", classLoader
                                , "rpc"
                                , String.class, boolean.class, boolean.class, String.class,
                                classLoader.loadClass(General.JSON_OBJECT_NAME), String.class,
                                classLoader.loadClass(General.JSON_OBJECT_NAME), boolean.class, boolean.class, int.class, boolean.class, String.class,
                                classLoader.loadClass("com.alibaba.ariver.app.api.App"),
                                classLoader.loadClass("com.alibaba.ariver.app.api.Page"),
                                classLoader.loadClass("com.alibaba.ariver.engine.api.bridge.model.ApiContext"),
                                classLoader.loadClass("com.alibaba.ariver.engine.api.bridge.extension.BridgeCallback")
                                , new XC_MethodHook() {
                                    @SuppressLint("WakelockTimeout")
                                    @Override
                                    protected void beforeHookedMethod(MethodHookParam param) {
                                        Object[] args = param.args;
                                        Object object = args[15];
                                        Object[] recordArray = new Object[4];
                                        recordArray[0] = System.currentTimeMillis();
                                        recordArray[1] = args[0];
                                        recordArray[2] = args[4];
                                        rpcHookMap.put(object, recordArray);
                                    }

                                    @SuppressLint("WakelockTimeout")
                                    @Override
                                    protected void afterHookedMethod(MethodHookParam param) {
                                        Object object = param.args[15];
                                        Object[] recordArray = rpcHookMap.remove(object);
                                        if (recordArray != null) {
                                            try {
                                                JSONObject Res = new JSONObject();
                                                Res.put("TimeStamp", recordArray[0]);
                                                Res.put("Method", recordArray[1]);
                                                Res.put("Params", recordArray[2]);
                                                Res.put("Data", recordArray[3]);

                                                Object[] objArr2 = param.args;
                                                if (objArr2 == null || objArr2.length < 15 || (objArr2[15]) == null) {
                                                    Log.runtime(TAG,"Hook鱼塘Token数据检查失败");
                                                }
                                                setConfig(recordArray[1].toString(), recordArray[2].toString(), recordArray[3]);
                                                Log.runtime(TAG,"记录\n时间: " + recordArray[0] + "\n方法: " + recordArray[1] + "\n参数: " + recordArray[2] + "\n数据: " + recordArray[3] + "\n");

                                                if (BaseModel.getSendHookData().getValue()) {
                                                    HookSender.sendHookData(Res);
                                                }
                                                if (recordArray[3] != null && !recordArray[3].equals("null")) {
                                                    Log.capture(Res.toString());
                                                }
                                            } catch (JSONException j) {
                                                Log.runtime(TAG, "JSON Exception: " + j.getMessage());
                                            }
                                        } else {
                                            Log.capture("delete record ID: " + object.hashCode());
                                        }
                                    }
                                });
                        Log.runtime(TAG, "hook record request successfully");
                    } catch (Throwable t) {
                        Log.runtime(TAG, "hook record request err:");
                        Log.printStackTrace(TAG, t);
                    }
                    try {
                        rpcResponseUnhook = XposedHelpers.findAndHookMethod("com.alibaba.ariver.engine.common.bridge.internal.DefaultBridgeCallback", classLoader
                                , "sendJSONResponse"
                                , classLoader.loadClass(General.JSON_OBJECT_NAME)
                                , new XC_MethodHook() {
                                    @SuppressLint("WakelockTimeout")
                                    @Override
                                    protected void beforeHookedMethod(MethodHookParam param) {
                                        Object object = param.thisObject;
                                        Object[] recordArray = rpcHookMap.get(object);
                                        if (recordArray != null) {
                                            recordArray[3] = String.valueOf(param.args[0]);
                                        }
                                    }
                                });
                        Log.runtime(TAG, "hook record response successfully");
                    } catch (Throwable t) {
                        Log.runtime(TAG, "hook record response err:");
                        Log.printStackTrace(TAG, t);
                    }
                }
                Model.bootAllModel(classLoader);
                Status.load();
                DataCache.INSTANCE.load();
                updateDay(userId);
                BaseModel.initData();
                String successMsg = "芝麻粒-TK 加载成功✨";
                Log.record(successMsg);
                Toast.show(successMsg);
            }
            offline = false;
            execHandler();
            return true;
        } catch (Throwable th) {
            Log.runtime(TAG, "startHandler err:");
            Log.printStackTrace(TAG, th);
            Toast.show("芝麻粒加载失败 🎃");
            return false;
        }
    }

    static synchronized void destroyHandler(Boolean force) {
        try {
            if (force) {
                if (service != null) {
                    stopHandler();
                    BaseModel.destroyData();
                    Status.unload();
                    Notify.stop();
                    RpcIntervalLimit.clearIntervalLimit();
                    Config.unload();
                    Model.destroyAllModel();
                    UserMap.unload();
                }
                if (rpcResponseUnhook != null) {
                    try {
                        rpcResponseUnhook.unhook();
                    } catch (Exception e) {
                        Log.printStackTrace(e);
                    }
                }
                if (rpcRequestUnhook != null) {
                    try {
                        rpcRequestUnhook.unhook();
                    } catch (Exception e) {
                        Log.printStackTrace(e);
                    }
                }
                if (wakeLock != null) {
                    wakeLock.release();
                    wakeLock = null;
                }
                if (rpcBridge != null) {
                    rpcVersion = null;
                    rpcBridge.unload();
                    rpcBridge = null;
                }
            } else {
                ModelTask.stopAllTask();
            }
        } catch (Throwable th) {
            Log.runtime(TAG, "stopHandler err:");
            Log.printStackTrace(TAG, th);
        }
    }

    static void execHandler() {
        mainTask.startTask(false);
    }

    /**
     * 安排主任务在指定的延迟时间后执行，并更新通知中的下次执行时间。
     *
     * @param delayMillis 延迟执行的毫秒数
     */
    static void execDelayedHandler(long delayMillis) {
        mainHandler.postDelayed(
                () -> mainTask.startTask(true), delayMillis);
        try {
            Notify.updateNextExecText(System.currentTimeMillis() + delayMillis);
        } catch (Exception e) {
            Log.printStackTrace(e);
        }
    }

    private static void stopHandler() {
        mainTask.stopTask();
        ModelTask.stopAllTask();
    }

    public static void updateDay(String userId) {
        Calendar nowCalendar = Calendar.getInstance();
        try {
            if (dayCalendar == null) {
                dayCalendar = (Calendar) nowCalendar.clone();
                dayCalendar.set(Calendar.HOUR_OF_DAY, 0);
                dayCalendar.set(Calendar.MINUTE, 0);
                dayCalendar.set(Calendar.SECOND, 0);
                Log.record(TAG, "初始化日期为：" + dayCalendar.get(Calendar.YEAR) + "-" + (dayCalendar.get(Calendar.MONTH) + 1) + "-" + dayCalendar.get(Calendar.DAY_OF_MONTH));
                setWakenAtTimeAlarm();
                return;
            }

            int nowYear = nowCalendar.get(Calendar.YEAR);
            int nowMonth = nowCalendar.get(Calendar.MONTH);
            int nowDay = nowCalendar.get(Calendar.DAY_OF_MONTH);
            if (dayCalendar.get(Calendar.YEAR) != nowYear || dayCalendar.get(Calendar.MONTH) != nowMonth || dayCalendar.get(Calendar.DAY_OF_MONTH) != nowDay) {
                dayCalendar = (Calendar) nowCalendar.clone();
                dayCalendar.set(Calendar.HOUR_OF_DAY, 0);
                dayCalendar.set(Calendar.MINUTE, 0);
                dayCalendar.set(Calendar.SECOND, 0);
                Log.record(TAG, "日期更新为：" + nowYear + "-" + (nowMonth + 1) + "-" + nowDay);
                setWakenAtTimeAlarm();
            }
        } catch (Exception e) {
            Log.printStackTrace(e);
        }
        try {
            Statistics.save(Calendar.getInstance());
        } catch (Exception e) {
            Log.printStackTrace(e);
        }
        try {
            Status.save(nowCalendar);
        } catch (Exception e) {
            Log.printStackTrace(e);
        }
        try {
            FriendWatch.updateDay(userId);
        } catch (Exception e) {
            Log.printStackTrace(e);
        }
    }

    @SuppressLint({"ScheduleExactAlarm", "ObsoleteSdkInt", "MissingPermission"})
    private static Boolean setAlarmTask(long triggerAtMillis, PendingIntent operation) {
        try {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, operation);
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, operation);
            }
            Log.runtime(TAG,
                    "setAlarmTask triggerAtMillis:" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(triggerAtMillis) + " operation:" + operation);
            return true;
        } catch (Throwable th) {
            Log.runtime(TAG, "setAlarmTask err:");
            Log.printStackTrace(TAG, th);
        }
        return false;
    }

    private static Boolean unsetAlarmTask(PendingIntent operation) {
        try {
            if (operation != null) {
                AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                alarmManager.cancel(operation);
            }
            return true;
        } catch (Throwable th) {
            Log.runtime(TAG, "unsetAlarmTask err:");
            Log.printStackTrace(TAG, th);
        }
        return false;
    }

    public static void reLoginByBroadcast() {
        try {
            context.sendBroadcast(new Intent("com.eg.android.AlipayGphone.sesame.reLogin"));
        } catch (Throwable th) {
            Log.runtime(TAG, "sesame sendBroadcast reLogin err:");
            Log.printStackTrace(TAG, th);
        }
    }

    public static void restartByBroadcast() {
        try {
            context.sendBroadcast(new Intent("com.eg.android.AlipayGphone.sesame.restart"));
        } catch (Throwable th) {
            Log.runtime(TAG, "sesame sendBroadcast restart err:");
            Log.printStackTrace(TAG, th);
        }
    }

    @SuppressLint("ObsoleteSdkInt")
    private static int getPendingIntentFlag() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT;
        } else {
            return PendingIntent.FLAG_UPDATE_CURRENT;
        }
    }

    public static Object getMicroApplicationContext() {
        if (microApplicationContextObject == null) {
            try {
                Class<?> alipayApplicationClass = XposedHelpers.findClass(
                        "com.alipay.mobile.framework.AlipayApplication", classLoader
                );
                Object alipayApplicationInstance = XposedHelpers.callStaticMethod(
                        alipayApplicationClass, "getInstance"
                );
                if (alipayApplicationInstance == null) {
                    return null;
                }
                microApplicationContextObject = XposedHelpers.callMethod(
                        alipayApplicationInstance, "getMicroApplicationContext"
                );
            } catch (Throwable t) {
                Log.printStackTrace(t);
            }
        }
        return microApplicationContextObject;
    }

    public static Object getServiceObject(String service) {
        try {
            return XposedHelpers.callMethod(getMicroApplicationContext(), "findServiceByInterface", service);
        } catch (Throwable th) {
            Log.runtime(TAG, "getUserObject err");
            Log.printStackTrace(TAG, th);
        }
        return null;
    }

    public static Object getUserObject() {
        try {
            return XposedHelpers.callMethod(
                    getServiceObject(XposedHelpers.findClass("com.alipay.mobile.personalbase.service.SocialSdkContactService", classLoader).getName()),
                    "getMyAccountInfoModelByLocal");
        } catch (Throwable th) {
            Log.runtime(TAG, "getUserObject err");
            Log.printStackTrace(TAG, th);
        }
        return null;
    }

    public static String getUserId() {
        try {
            Object userObject = getUserObject();
            if (userObject != null) {
                return (String) XposedHelpers.getObjectField(userObject, "userId");
            }
        } catch (Throwable th) {
            Log.runtime(TAG, "getUserId err");
            Log.printStackTrace(TAG, th);
        }
        return null;
    }

    public static void reLogin() {
        mainHandler.post(
                () -> {
                    if (reLoginCount.get() < 5) {
                        execDelayedHandler(reLoginCount.getAndIncrement() * 5000L);
                    } else {
                        execDelayedHandler(Math.max(BaseModel.getCheckInterval().getValue(), 180_000));
                    }
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setClassName(General.PACKAGE_NAME, General.CURRENT_USING_ACTIVITY);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    offline = true;
                    context.startActivity(intent);
                });
    }

    public static class AlipayBroadcastReceiver extends BroadcastReceiver {
        public static final String EXTRA_DATA = "data";
        public static final String EXTRA_METHOD = "method";
        public static final String EXTRA_RESULT = "result";
        public static final String EXTRA_TYPE = "type";

        public AlipayBroadcastReceiver() {
        }
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.runtime(TAG, "Alipay got Broadcast " + action + " intent:" + intent);
            if (action != null) {
                switch (action) {
                    case "com.eg.android.AlipayGphone.sesame.restart":
                        String userId = intent.getStringExtra("userId");
                        if (StringUtil.isEmpty(userId) || Objects.equals(UserMap.getCurrentUid(), userId)) {
                            initHandler(true);
                        }
                        break;
                    case "com.eg.android.AlipayGphone.sesame.execute":
                        initHandler(false);
                        break;
                    case "com.eg.android.AlipayGphone.sesame.reLogin":
                        reLogin();
                        break;
                    case "com.eg.android.AlipayGphone.sesame.status":
                        try {
                            if (ViewAppInfo.getRunType() == RunType.DISABLE) {
                                Intent replyIntent = new Intent("fansirsqi.xposed.sesame.status");
                                replyIntent.putExtra("EXTRA_RUN_TYPE", RunType.ACTIVE.getNickName());
                                replyIntent.setPackage(General.MODULE_PACKAGE_NAME);
                                context.sendBroadcast(replyIntent);
                                Log.system(TAG, "Replied with status: " + RunType.ACTIVE.getNickName());
                            }
                        } catch (Throwable th) {
                            Log.runtime(TAG, "sesame sendBroadcast status err:");
                            Log.printStackTrace(TAG, th);
                        }
                        break;
                    case "com.eg.android.AlipayGphone.sesame.rpctest":
                        try {
                            String method = intent.getStringExtra(EXTRA_METHOD);
                            String data = intent.getStringExtra(EXTRA_DATA);
                            String type = intent.getStringExtra(EXTRA_TYPE);
                            DebugRpc rpcInstance = new DebugRpc(); // 创建实例
                            rpcInstance.start(method, data, type); // 通过实例调用非静态方法
                        } catch (Throwable th) {
                            Log.runtime(TAG, "sesame 测试RPC请求失败:");
                            Log.printStackTrace(TAG, th);
                        }
                        break;
                    default:
                        throw new IllegalStateException("Unexpected value: " + action);
                }
            }
        }
    }

    /**
     * 注册广播接收器以监听支付宝相关动作。
     *
     * @param context 应用程序上下文
     */
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    // 忽略Lint关于注册广播接收器时未指定导出属性的警告
    void registerBroadcastReceiver(Context context) {
        //创建一个IntentFilter实例，用于过滤出我们需要捕获的广播
        try {
            IntentFilter intentFilter = getIntentFilter();
            // 根据Android SDK版本注册广播接收器
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // 在Android 13及以上版本，注册广播接收器并指定其可以被其他应用发送的广播触发
                context.registerReceiver(new AlipayBroadcastReceiver(), intentFilter, Context.RECEIVER_EXPORTED);
            } else {
                // 在Android 13以下版本，注册广播接收器
                context.registerReceiver(new AlipayBroadcastReceiver(), intentFilter);
            }
            // 记录成功注册广播接收器的日志
            Log.runtime(TAG, "hook registerBroadcastReceiver successfully");
        } catch (Throwable th) {
            // 记录注册广播接收器失败的日志
            Log.runtime(TAG, "hook registerBroadcastReceiver err:");
            // 打印异常堆栈信息
            Log.printStackTrace(TAG, th);
        }
    }

    @NonNull
    private static IntentFilter getIntentFilter() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.eg.android.AlipayGphone.sesame.restart"); // 重启支付宝服务的动作
        intentFilter.addAction("com.eg.android.AlipayGphone.sesame.execute"); // 执行特定命令的动作
        intentFilter.addAction("com.eg.android.AlipayGphone.sesame.reLogin"); // 重新登录支付宝的动作
        intentFilter.addAction("com.eg.android.AlipayGphone.sesame.status"); // 查询支付宝状态的动作
        intentFilter.addAction("com.eg.android.AlipayGphone.sesame.rpctest"); // 调试RPC的动作
        return intentFilter;
    }

    public static void setConfig(String str, String str2, Object obj) {
        try {
            if (!str2.isEmpty() && obj != null) {
                if ("com.alipay.antfishpond.fishpondAngle".equals(str)) {
                    String valueByPath = JsonUtil.getValueByPath(new JSONObject(str2), "requestData.[0].riskToken");
                    if (Objects.equals(Recreation.getFishpondToken().getValue(), valueByPath)) {
                        Log.runtime(TAG,"福气鱼塘已保存相同Token:"+valueByPath);
                        return;
                    } else {
                        Recreation.getFishpondToken().setValue(valueByPath);
                        Log.runtime(TAG,"保存福气鱼塘Token:"+valueByPath);
                        Toast.show("🎉 已为您自动获取保存福气鱼塘Token");
                    }
                }
                Config.save(UserMap.getCurrentUid(), false);
            }
        } catch (Throwable th) {
            Log.printStackTrace(TAG, th);
        }
    }


}
