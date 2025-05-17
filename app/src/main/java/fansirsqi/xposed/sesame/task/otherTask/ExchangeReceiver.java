package fansirsqi.xposed.sesame.task.otherTask;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.PowerManager;
import android.util.Log;

public class ExchangeReceiver extends BroadcastReceiver {
    private static final String TAG = "ExchangeReceiver";
    private static final long WAKE_UP_EARLY = 5 * 1000L; // 提前 5 秒唤醒

    @Override
    public void onReceive(Context context, Intent intent) {
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "Sesame::ExchangeWakelockTag"
        );
        wakeLock.acquire(60 * 1000); // 唤醒并保持 60 秒

        Log.i(TAG, "⏰ 设备已唤醒，准备执行兑换逻辑");

        try {
            // 启动子线程执行兑换逻辑
            HandlerThread handlerThread = new HandlerThread("ExchangeWorker");
            handlerThread.start();
            Handler handler = new Handler(handlerThread.getLooper());

            handler.post(() -> {
                try {
                    // 计算剩余时间，等待到整点再执行
                    long now = System.currentTimeMillis();
                    long nextMinute = (now / 60000 + 1) * 60000; // 下一分钟整点
                    long delay = nextMinute - now - WAKE_UP_EARLY;

                    if (delay > 0) {
                        Log.i(TAG, "⏳ 等待 " + delay + " 毫秒到达整点");
                        Thread.sleep(delay);
                    }

                    Log.i(TAG, "🚀 开始执行兑换逻辑");
                    new PrivilegeExTest().handle();

                } catch (Exception e) {
                    Log.e(TAG, "❌ 兑换逻辑执行失败", e);
                } finally {
                    handlerThread.quitSafely();
                    if (wakeLock.isHeld()) {
                        wakeLock.release();
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "🚨 唤醒失败", e);
            if (wakeLock.isHeld()) {
                wakeLock.release();
            }
        }
    }
}
