package fansirsqi.xposed.sesame.util;
import android.content.Context;
import android.content.res.Configuration;
import java.util.Locale;
import fansirsqi.xposed.sesame.model.BaseModel;
/**
 * 语言工具类，用于设置应用程序的语言环境。
 */
public class LanguageUtil {
    /**
     * 设置应用程序的语言环境为简体中文。
     * 如果配置指定使用简体中文，则忽略系统语言设置，强制应用简体中文。
     *
     * @param context 应用程序上下文，用于访问资源和配置。
     */
    public static void setLocale(Context context) {
        // 检查是否设置了简体中文
        if (BaseModel.getLanguageSimplifiedChinese().getValue()) {
            // 创建简体中文的Locale对象
            Locale locale = new Locale("zh", "CN");
            // 设置默认的Locale
            Locale.setDefault(locale);
            // 获取当前的配置信息
            Configuration config = new Configuration(context.getResources().getConfiguration());
            // 更新配置信息中的Locale
            config.locale = locale;
            // 更新资源的配置信息，以应用新的Locale设置
            context.getResources().updateConfiguration(config, context.getResources().getDisplayMetrics());
        }
    }
}