package fansirsqi.xposed.sesame.util;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.view.accessibility.AccessibilityEventCompat;

import fansirsqi.xposed.sesame.R;
import fansirsqi.xposed.sesame.data.RuntimeInfo;
import fansirsqi.xposed.sesame.model.BaseModel;
import lombok.Getter;

public class NotificationUtil {
    private static final String CHANNEL_ID = "leo.xposed.sesame.ANTFOREST_NOTIFY_CHANNEL";
    private static final String CHANNEL_NAME = "提醒";
    private static final int NOTIFICATION_ID = 99;
    private static final String NOTIFY_CHANNEL_ID = "leo.xposed.sesame.NOTIFY_CHANNEL";
    private static int NOTIFY_NOTIFICATION_ID = 1;
    private static Notification.Builder builder = null;
    private static String contentText = "";
    private static Context context = null;
    @Getter
    private static volatile long lastNoticeTime = 0;
    private static NotificationManager mNotifyManager = null;
    private static String titleText = "";

    public static void start(Context context2) {
        try {
            context = context2;
            stop();
            titleText = "启动中";
            contentText = "暂无消息";
            mNotifyManager = (NotificationManager) context2.getSystemService(Context.NOTIFICATION_SERVICE);
            Intent intent = new Intent("android.intent.action.VIEW");
            intent.setData(Uri.parse("alipays://platformapi/startapp?appId="));
            @SuppressLint("WrongConstant") PendingIntent activity = PendingIntent.getActivity(context2, 0, intent, 201326592);
            NotificationChannel notificationChannel = getNotificationChannel();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mNotifyManager.createNotificationChannel(notificationChannel);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                builder = new Notification.Builder(context2, CHANNEL_ID);
            }
            if (Build.VERSION.SDK_INT >= 28) {
                builder.setCategory(NotificationCompat.CATEGORY_NAVIGATION);
            }
            builder.setSmallIcon(R.drawable.sym_def_app_icon).setLargeIcon(BitmapFactory.decodeResource(context2.getResources(), R.drawable.sym_def_app_icon)).setSubText("芝麻粒").setAutoCancel(false).setContentIntent(activity);
            if (BaseModel.getEnableOnGoing().getValue().booleanValue()) {
                builder.setOngoing(true);
            }
            mNotifyManager.notify(NOTIFICATION_ID, builder.build());
        } catch (Exception e) {
            Log.printStackTrace(e);
        }
    }

    @Nullable
    private static NotificationChannel getNotificationChannel() {
        NotificationChannel notificationChannel = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationChannel = new NotificationChannel(CHANNEL_ID, "能量提醒", NotificationManager.IMPORTANCE_LOW);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationChannel.enableLights(false);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationChannel.enableVibration(false);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationChannel.setShowBadge(false);
        }
        return notificationChannel;
    }

    public static void stop() {
        try {
            NotificationManager notificationManager = mNotifyManager;
            if (notificationManager != null) {
                notificationManager.cancel(NOTIFICATION_ID);
            } else {
                Context context2 = context;
                if (context2 != null) {
                    ((NotificationManager) context2.getSystemService(Context.NOTIFICATION_SERVICE)).cancel(NOTIFICATION_ID);
                }
            }
            mNotifyManager = null;
        } catch (Exception e) {
            Log.printStackTrace(e);
        }
    }

    public static void updateStatusText(String str) {
        try {
            long longValue = RuntimeInfo.getInstance().getLong(RuntimeInfo.RuntimeInfoKey.ForestPauseTime).longValue();
            if (longValue > System.currentTimeMillis()) {
                str = "触发异常，等待至" + TimeUtil.getCommonDate(Long.valueOf(longValue));
            }
            titleText = str;
            lastNoticeTime = System.currentTimeMillis();
            sendText();
        } catch (Exception e) {
            Log.printStackTrace(e);
        }
    }

    public static void updateNextExecText(long j) {
        String str;
        if (j > 0) {
            try {
                str = "下次执行 " + TimeUtil.getTimeStr(j);
            } catch (Exception e) {
                Log.printStackTrace(e);
                return;
            }
        } else {
            str = "";
        }
        titleText = str;
        sendText();
    }

    public static void updateLastExecText(String str) {
        try {
            contentText = "上次执行  " + TimeUtil.getTimeStr(System.currentTimeMillis()) + " " + str;
            lastNoticeTime = System.currentTimeMillis();
            sendText();
        } catch (Exception e) {
            Log.printStackTrace(e);
        }
    }

    public static void setStatusTextExec() {
        updateStatusText("执行中");
    }

    private static void sendText() {
        try {
            builder.setContentTitle(titleText);
            if (!StringUtil.isEmpty(contentText)) {
                builder.setContentText(contentText);
            }
            mNotifyManager.notify(NOTIFICATION_ID, builder.build());
        } catch (Exception e) {
            Log.printStackTrace(e);
        }
    }

    @SuppressLint("WrongConstant")
    public static void showNotification(Context context2, String str) {
        createNotificationChannel(context2);
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.setData(Uri.parse("alipays://platformapi/startapp?appId=20000001"));
        intent.setFlags(268468224);
        ((NotificationManager) context2.getSystemService("notification")).notify(NOTIFY_NOTIFICATION_ID, new NotificationCompat.Builder(context2, NOTIFY_CHANNEL_ID).setSmallIcon(R.drawable.sym_def_app_icon).setLargeIcon(BitmapFactory.decodeResource(context2.getResources(), R.drawable.sym_def_app_icon)).setContentTitle(CHANNEL_NAME).setContentText(str).setPriority(0).setContentIntent(PendingIntent.getActivity(context2, 0, intent, AccessibilityEventCompat.TYPE_VIEW_TARGETED_BY_SCROLL)).setAutoCancel(true).build());
        NOTIFY_NOTIFICATION_ID++;
    }

    private static void createNotificationChannel(Context context2) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ((NotificationManager) context2.getSystemService(NotificationManager.class)).createNotificationChannel(new NotificationChannel(NOTIFY_CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT));
            }
        }
    }
}
