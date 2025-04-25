package fansirsqi.xposed.sesame.util;

import android.annotation.SuppressLint;
import android.app.*;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import fansirsqi.xposed.sesame.data.RuntimeInfo;
import fansirsqi.xposed.sesame.model.BaseModel;
import fansirsqi.xposed.sesame.task.ModelTask;

import lombok.Getter;

public class Notify {
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    @SuppressLint("StaticFieldLeak")
    public static Context context;
    private static final int NOTIFICATION_ID = 99;
    private static final int ERROR_NOTIFICATION_ID = 98;
    private static final String CHANNEL_ID = "fansirsqi.xposed.sesame.ANTFOREST_NOTIFY_CHANNEL";
    private static NotificationManager mNotifyManager;
    private static Notification.Builder builder;

    private static long lastUpdateTime = 0;
    private static long nextExecTimeCache = 0;
    private static String titleText = "";
    private static String contentText = "";

    @SuppressLint("ObsoleteSdkInt")
    public static void sendErrorNotification(String title, String content) {
        try {
            if (context == null) {
                return;
            }
            mNotifyManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID, "‼️ 芝麻粒异常通知", NotificationManager.IMPORTANCE_LOW);
                mNotifyManager.createNotificationChannel(notificationChannel);
                builder = new Notification.Builder(context, CHANNEL_ID);
            } else {
                //安卓8.0以下
                builder = new Notification.Builder(context).setPriority(Notification.PRIORITY_LOW);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                builder.setCategory(Notification.CATEGORY_ERROR);
            builder
                    .setSmallIcon(android.R.drawable.sym_def_app_icon)
                    .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), android.R.drawable.sym_def_app_icon))
                    .setContentTitle(title)
                    .setContentText(content)
                    .setSubText("芝麻粒")
                    .setAutoCancel(true);
            Notification mNotification = builder.build();
            if (context instanceof Service) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    mNotifyManager.notify(ERROR_NOTIFICATION_ID, mNotification);
                } else {
                    ((Service) context).startForeground(ERROR_NOTIFICATION_ID, mNotification);
                }
            } else {
                mNotifyManager.notify(ERROR_NOTIFICATION_ID, mNotification);
            }
        } catch (Exception e) {
            Log.printStackTrace(e);
        }
    }

    @SuppressLint("ObsoleteSdkInt")
    @Getter
    private static volatile long lastNoticeTime = 0;

    public static void start(Context context) {
        try {
            Notify.context = context;
            Notify.stop();
            titleText = "🚀 启动中";
            contentText = "🔔 暂无消息";
            lastUpdateTime = System.currentTimeMillis();
            mNotifyManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            Intent it = new Intent(Intent.ACTION_VIEW);
            it.setData(Uri.parse("alipays://platformapi/startapp?appId="));
            PendingIntent pi = PendingIntent.getActivity(context, 0, it, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID, "🔔 芝麻粒能量提醒", NotificationManager.IMPORTANCE_LOW);
                notificationChannel.enableLights(false);
                notificationChannel.enableVibration(false);
                notificationChannel.setShowBadge(false);
                mNotifyManager.createNotificationChannel(notificationChannel);
                builder = new Notification.Builder(context, CHANNEL_ID);
            } else {
                //安卓8.0以下
                builder = new Notification.Builder(context).setPriority(Notification.PRIORITY_LOW);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                builder.setCategory(Notification.CATEGORY_NAVIGATION);
            builder
                    .setSmallIcon(android.R.drawable.sym_def_app_icon)
                    .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), android.R.drawable.sym_def_app_icon))
                    .setContentTitle(titleText)
                    .setContentText(contentText)
                    .setSubText("芝麻粒")
                    .setAutoCancel(false)
                    .setContentIntent(pi);
            if (BaseModel.getEnableOnGoing().getValue()) {
                builder.setOngoing(true);
            }
            Notification mNotification = builder.build();
            if (context instanceof Service) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    mNotifyManager.notify(NOTIFICATION_ID, mNotification);
                } else {
                    ((Service) context).startForeground(NOTIFICATION_ID, mNotification);
                }
            } else {
                mNotifyManager.notify(NOTIFICATION_ID, mNotification);
            }
        } catch (Exception e) {
            Log.printStackTrace(e);
        }
    }

    /**
     * 停止通知。 移除通知并停止前台服务。
     */
    public static void stop() {
        try {
            if (context instanceof Service) {
                ((Service) context).stopForeground(true);
            } else {
                if (mNotifyManager != null) {
                    mNotifyManager.cancel(NOTIFICATION_ID);
                } else if (context != null) {
                    NotificationManager systemService = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                    if (systemService != null) {
                        systemService.cancel(NOTIFICATION_ID);
                    }
                }
            }
            mNotifyManager = null;
        } catch (Exception e) {
            Log.printStackTrace(e);
        }
    }

    /**
     * 更新通知文本。 更新通知的标题和内容文本，并发送通知。
     *
     * @param status 要更新的状态文本。
     */
    public static void updateStatusText(String status) {
        try {
            long forestPauseTime = RuntimeInfo.getInstance().getLong(RuntimeInfo.RuntimeInfoKey.ForestPauseTime);
            if (forestPauseTime > System.currentTimeMillis()) {
                status = "❌ 触发异常，等待至" + TimeUtil.getCommonDate(forestPauseTime) + "恢复运行";
            }

            if (BaseModel.getEnableProgress().getValue() && !ModelTask.isAllTaskFinished()) {
                builder.setProgress(100, ModelTask.completedTaskPercentage(), false);
            } else {
                builder.setProgress(0, 0, false);
            }

            titleText = status;
            mainHandler.post(() -> sendText(true));
        } catch (Exception e) {
            Log.printStackTrace(e);
        }
    }

    /**
     * 更新下一次执行时间的文本。
     *
     * @param nextExecTime 下一次执行的时间。
     */
    public static void updateNextExecText(long nextExecTime) {
        try {
            if (nextExecTime != -1) {
                nextExecTimeCache = nextExecTime;
            }
            if (BaseModel.getEnableProgress().getValue() && !ModelTask.isAllTaskFinished()) {
                builder.setProgress(100, ModelTask.completedTaskPercentage(), false);
            } else {
                builder.setProgress(0, 0, false);
            }
            if (ModelTask.isAllTaskFinished()) {
                titleText = nextExecTimeCache > 0 ? "⏰ 下次执行 " + TimeUtil.getTimeStr(nextExecTimeCache) : "";
            }
            mainHandler.post(() -> sendText(false));
        } catch (Exception e) {
            Log.printStackTrace(e);
        }
    }

    /**
     * 强制刷新通知，全部任务结束后调用
     */
    public static void forceUpdateText() {
        if (BaseModel.getEnableProgress().getValue() && !ModelTask.isAllTaskFinished()) {
            builder.setProgress(100, ModelTask.completedTaskPercentage(), false);
        } else {
            builder.setProgress(0, 0, false);
        }
        if (ModelTask.isAllTaskFinished()) {
            titleText = nextExecTimeCache > 0 ? "⏰ 下次执行 " + TimeUtil.getTimeStr(nextExecTimeCache) : "";
        }
        mainHandler.post(() -> sendText(true));
    }

    /**
     * 更新上一次执行的文本。
     *
     * @param content 上一次执行的内容。
     */
    public static void updateLastExecText(String content) {
        try {
            contentText = "📌 上次执行 " + TimeUtil.getTimeStr(System.currentTimeMillis()) + "\n🌾 " + content;
            mainHandler.post(() -> sendText(false));
        } catch (Exception e) {
            Log.printStackTrace(e);
        }
    }


    /**
     * 设置状态文本为执行中。
     */
    public static void setStatusTextExec() {
        try {
            long forestPauseTime = RuntimeInfo.getInstance().getLong(RuntimeInfo.RuntimeInfoKey.ForestPauseTime);

            if (forestPauseTime > System.currentTimeMillis()) {
                titleText = "❌ 触发异常，等待至" + TimeUtil.getCommonDate(forestPauseTime) + "恢复运行";
            }
            if (BaseModel.getEnableProgress().getValue()) {
                builder.setProgress(100, 0, false);
            }
            titleText = "⚙️ 芝麻粒正在施工中...";
            builder.setContentTitle(titleText);
            mainHandler.post(() -> sendText(true));
        } catch (Exception e) {
            Log.printStackTrace(e);
        }
    }

    /**
     * 设置状态文本为已禁用
     */
    public static void setStatusTextDisabled() {
        try {
            builder.setContentTitle("🚫 芝麻粒已禁用");
            if (!StringUtil.isEmpty(contentText)) {
                builder.setContentText(contentText);
            }
            builder.setProgress(0, 0, false);
            mainHandler.post(() -> sendText(true));
        } catch (Exception e) {
            Log.printStackTrace(e);
        }
    }

    public static void setStatusTextExec(String content) {
        updateStatusText("⚙️ " + content + " 施工中...");

    }

    /**
     * 发送文本更新。 更新通知的内容文本，并重新发送通知。
     *
     * @param force 是否强制刷新
     */
    private static void sendText(Boolean force) {
        try {
            if (!force && System.currentTimeMillis() - lastUpdateTime < 500) {
                return;
            }
            lastUpdateTime = System.currentTimeMillis();
            builder.setContentTitle(titleText);
            if (!StringUtil.isEmpty(contentText)) {
                builder.setContentText(contentText);
            }
            if (!BaseModel.getEnableProgress().getValue()) {
                builder.setProgress(0, 0, false);
            }
            mNotifyManager.notify(NOTIFICATION_ID, builder.build());
        } catch (Exception e) {
            Log.printStackTrace(e);
        }
    }

    @SuppressLint("ObsoleteSdkInt")
    public static void sendNewNotification(Context context, String title, String content, int newNotificationId) {
        try {
            NotificationManager notifyManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            Notification.Builder newBuilder;
            Intent it = new Intent(Intent.ACTION_VIEW);
            it.setData(Uri.parse("alipays://platformapi/startapp?appId="));
            PendingIntent pi = PendingIntent.getActivity(context, 0, it, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID, "🔔 芝麻粒其他提醒", NotificationManager.IMPORTANCE_HIGH);
                notifyManager.createNotificationChannel(notificationChannel);
                newBuilder = new Notification.Builder(context, CHANNEL_ID);
            } else {
                newBuilder = new Notification.Builder(context);
            }
            // 配置新通知的样式
            newBuilder
                    .setSmallIcon(android.R.drawable.sym_def_app_icon)
                    .setContentTitle(title)
                    .setContentText(content)
                    .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), android.R.drawable.sym_def_app_icon))
                    .setAutoCancel(true)
                    .setContentIntent(pi)
                    .setPriority(Notification.PRIORITY_HIGH);
            // 发送新通知
            Notification newNotification = newBuilder.build();
            if (context instanceof Service) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    notifyManager.notify(newNotificationId, newNotification);
                } else {
                    ((Service) context).startForeground(newNotificationId, newNotification);
                }
            } else {
                notifyManager.notify(newNotificationId, newNotification);
            }
        } catch (Exception e) {
            Log.printStackTrace(e);
        }
    }
}
