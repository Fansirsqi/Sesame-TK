package fansirsqi.xposed.sesame.util;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Objects;
import fansirsqi.xposed.sesame.data.Config;
import fansirsqi.xposed.sesame.util.Maps.CooperateMap;
import fansirsqi.xposed.sesame.util.Maps.UserMap;
/**
 * Utility class for handling import and export operations.
 */
public class PortUtil {
    public static void handleExport(Context context, Uri uri, String userId) {
        if (uri == null) {
            ToastUtil.makeText("未选择目标位置", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            File configV2File = StringUtil.isEmpty(userId) ?
                    Files.getDefaultConfigV2File() :
                    Files.getConfigV2File(userId);
            FileInputStream inputStream = new FileInputStream(configV2File);
            if (Files.streamTo(inputStream, context.getContentResolver().openOutputStream(uri))) {
                ToastUtil.makeText("导出成功！", Toast.LENGTH_SHORT).show();
            } else {
                ToastUtil.makeText("导出失败！", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Log.printStackTrace(e);
            ToastUtil.makeText("导出失败：发生异常", Toast.LENGTH_SHORT).show();
        }
    }
    public static void handleImport(Context context, Uri uri, String userId) {
        if (uri == null) {
            ToastUtil.makeText("导入失败：未选择文件", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            File configV2File = StringUtil.isEmpty(userId) ?
                    Files.getDefaultConfigV2File() :
                    Files.getConfigV2File(userId);
            FileOutputStream outputStream = new FileOutputStream(configV2File);
            if (Files.streamTo(Objects.requireNonNull(context.getContentResolver().openInputStream(uri)), outputStream)) {
                ToastUtil.makeText("导入成功！", Toast.LENGTH_SHORT).show();
                if (!StringUtil.isEmpty(userId)) {
                    try {
                        Intent intent = new Intent("com.eg.android.AlipayGphone.sesame.restart");
                        intent.putExtra("userId", userId);
                        context.sendBroadcast(intent);
                    } catch (Throwable th) {
                        Log.printStackTrace(th);
                    }
                }
                Intent intent = ((android.app.Activity) context).getIntent();
                ((android.app.Activity) context).finish();
                context.startActivity(intent);
            } else {
                ToastUtil.makeText("导入失败！", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Log.printStackTrace(e);
            ToastUtil.makeText("导入失败：发生异常", Toast.LENGTH_SHORT).show();
        }
    }
    public static void save(Context context, String userId) {
        try {
            if (Config.isModify(userId) && Config.save(userId, false)) {
                ToastUtil.showToastWithDelay("保存成功！", 100);
                if (!StringUtil.isEmpty(userId)) {
                    Intent intent = new Intent("com.eg.android.AlipayGphone.sesame.restart");
                    intent.putExtra("userId", userId);
                    context.sendBroadcast(intent);
                }
            }
            if (!StringUtil.isEmpty(userId)) {
                UserMap.save(userId);
                CooperateMap.getInstance(CooperateMap.class).save(userId);
            }
        } catch (Throwable th) {
            Log.printStackTrace(th);
        }
    }
}
