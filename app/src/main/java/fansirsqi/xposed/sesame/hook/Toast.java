package fansirsqi.xposed.sesame.hook;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import fansirsqi.xposed.sesame.model.BaseModel;
import fansirsqi.xposed.sesame.util.Log;
public class Toast {
    private static final String TAG = Toast.class.getSimpleName();
    /**
     * 显示 Toast 消息
     *
     * @param message 要显示的消息
     */
    public static void show(CharSequence message) {
        show(message, false);
    }
    /**
     * 显示 Toast 消息
     *
     * @param message 要显示的消息
     * @param force   是否强制显示
     */
    public static void show(CharSequence message, boolean force) {
        Context context = ApplicationHook.getAppContext();
        if (context == null) {
            Log.runtime(TAG, "Context is null, cannot show toast");
            return;
        }
        boolean shouldShow = force || (BaseModel.getShowToast() != null && BaseModel.getShowToast().getValue());
        if (shouldShow) {
            displayToast(context.getApplicationContext(), message);
        }
    }
    /**
     * 显示 Toast 消息（确保在主线程中调用）
     *
     * @param context 上下文
     * @param message 要显示的消息
     */
    private static void displayToast(Context context, CharSequence message) {
        try {
            Handler mainHandler = new Handler(Looper.getMainLooper());
            if (Looper.myLooper() == Looper.getMainLooper()) {
                // 如果当前线程是主线程，直接显示
                createAndShowToast(context, message);
            } else {
                // 在非主线程，通过 Handler 切换到主线程
                mainHandler.post(() -> createAndShowToast(context, message));
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "displayToast err:");
            Log.printStackTrace(TAG, t);
        }
    }
    /**
     * 创建并显示 Toast
     *
     * @param context 上下文
     * @param message 要显示的消息
     */
    private static void createAndShowToast(Context context, CharSequence message) {
        try {
            android.widget.Toast toast = android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT);
            toast.setGravity(
                    toast.getGravity(),
                    toast.getXOffset(),
                    BaseModel.getToastOffsetY() != null ? BaseModel.getToastOffsetY().getValue() : 0
            );
            toast.show();
        } catch (Throwable t) {
            Log.runtime(TAG, "createAndShowToast err:");
            Log.printStackTrace(TAG, t);
        }
    }
}
