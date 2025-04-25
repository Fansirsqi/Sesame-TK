package fansirsqi.xposed.sesame.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.File;

import fansirsqi.xposed.sesame.ui.SettingActivity;
import fansirsqi.xposed.sesame.ui.WebSettingsActivity;
import fansirsqi.xposed.sesame.ui.OldSettingsActivity;
import fansirsqi.xposed.sesame.util.Files;
import fansirsqi.xposed.sesame.util.JsonUtil;
import fansirsqi.xposed.sesame.util.Log;
import lombok.Data;
import lombok.Setter;

@Data
public class UIConfig {
    private static final String TAG = UIConfig.class.getSimpleName();
    public static final UIConfig INSTANCE = new UIConfig();

    @JsonIgnore // 排除掉不需要序列化的字段
    private boolean init;


    public static final String UI_OPTION_OLD = "old";
    public static final String UI_OPTION_WEB = "web"; //webUI
    public static final String UI_OPTION_NEW = "new";

    @Setter
    @JsonProperty("uiOption") // 直接序列化 uiOption 字段
    private String uiOption = UI_OPTION_WEB; // 默认值为 "new"

    private UIConfig() {
    }

    public static Boolean save() {
        Log.record("保存UI配置");
        return Files.setTargetFileofDir(JsonUtil.formatJson(INSTANCE), new File(Files.CONFIG_DIR, "app_config.json"));
    }

    public static synchronized UIConfig load() {
        File targetFile = Files.getTargetFileofDir(Files.CONFIG_DIR, "app_config.json");
        try {
            if (targetFile.exists()) {
                String json = Files.readFromFile(targetFile);
                if (!json.trim().isEmpty()) {
                    JsonUtil.copyMapper().readerForUpdating(INSTANCE).readValue(json);
                    String formatted = JsonUtil.formatJson(INSTANCE);
                    if (formatted != null && !formatted.equals(json)) {
                        Log.runtime(TAG, "格式化" + TAG + "配置");
                        Files.write2File(formatted, targetFile);
                    }
                } else {
                    resetToDefault();
                }
            } else {
                resetToDefault();
                Files.write2File(JsonUtil.formatJson(INSTANCE), targetFile);
            }
        } catch (Exception e) {
            Log.printStackTrace(TAG, e);
            Log.runtime(TAG, "重置" + TAG + "配置");
            resetToDefault();
            try {
                Files.write2File(JsonUtil.formatJson(INSTANCE), targetFile);
            } catch (Exception e2) {
                Log.printStackTrace(TAG, e2);
            }
        }
        INSTANCE.setInit(true);
        return INSTANCE;
    }

    private static synchronized void resetToDefault() {
        Log.runtime(TAG, "重置UI配置");
        INSTANCE.setUiOption(UI_OPTION_WEB); // 默认设置为 "new"
        INSTANCE.setInit(false);
    }

    @JsonIgnore
    public Class<?> getTargetActivityClass() {
        return switch (uiOption) {
            case UI_OPTION_OLD -> OldSettingsActivity.class;
            case UI_OPTION_WEB -> WebSettingsActivity.class;
            case UI_OPTION_NEW -> SettingActivity.class;
            default -> {
                Log.runtime(TAG, "未知的 UI 选项: " + uiOption);
                yield WebSettingsActivity.class;
            }
        };
    }
}
