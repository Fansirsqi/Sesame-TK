package fansirsqi.xposed.sesame.data;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import fansirsqi.xposed.sesame.entity.UserEntity;
import fansirsqi.xposed.sesame.model.ModelConfig;
import fansirsqi.xposed.sesame.model.ModelField;
import fansirsqi.xposed.sesame.model.ModelFields;
import fansirsqi.xposed.sesame.task.ModelTask;
import fansirsqi.xposed.sesame.util.Files;
import fansirsqi.xposed.sesame.util.JsonUtil;
import fansirsqi.xposed.sesame.util.Log;
import fansirsqi.xposed.sesame.util.Maps.UserMap;
import fansirsqi.xposed.sesame.util.StringUtil;
import lombok.Data;

/**
 * 配置类，负责加载、保存、管理应用的配置数据。
 */
@Data
public class Config {
    private static final String TAG = Config.class.getSimpleName();
    // 单例实例
    public static final Config INSTANCE = new Config();
    // 是否初始化标志
    @JsonIgnore
    private volatile boolean init = false;
    // 存储模型字段的映射
    private final Map<String, ModelFields> modelFieldsMap = new ConcurrentHashMap<>();

    /**
     * 设置新的模型字段配置
     *
     * @param newModels 新的模型字段映射
     */
    public void setModelFieldsMap(Map<String, ModelFields> newModels) {
        modelFieldsMap.clear();
        Map<String, ModelConfig> modelConfigMap = ModelTask.getModelConfigMap();
        // 如果传入的 newModels 为 null，初始化为空
        if (newModels == null) {
            newModels = new HashMap<>();
        }
        // 遍历所有模型配置，合并字段配置
        for (ModelConfig modelConfig : modelConfigMap.values()) {
            String modelCode = modelConfig.getCode();
            ModelFields newModelFields = new ModelFields();
            ModelFields configModelFields = modelConfig.getFields();
            ModelFields modelFields = newModels.get(modelCode);
            if (modelFields != null) {
                // 如果已有模型字段，则按值覆盖配置
                for (ModelField<?> configModelField : configModelFields.values()) {
                    ModelField<?> modelField = modelFields.get(configModelField.getCode());
                    try {
                        if (modelField != null) {
                            Object value = modelField.getValue();
                            if (value != null) {
                                configModelField.setObjectValue(value);
                            }
                        }
                    } catch (Exception e) {
                        Log.printStackTrace(e);
                    }
                    newModelFields.addField(configModelField);
                }
            } else {
                // 如果没有找到对应的模型字段，则直接添加配置字段
                for (ModelField<?> configModelField : configModelFields.values()) {
                    newModelFields.addField(configModelField);
                }
            }
            modelFieldsMap.put(modelCode, newModelFields);
        }
    }

    /**
     * 检查是否存在指定的模型字段
     *
     * @param modelCode 模型代码
     * @return 是否存在该模型字段
     */
    public Boolean hasModelFields(String modelCode) {
        return modelFieldsMap.containsKey(modelCode);
    }

    /**
     * 检查指定模型字段是否存在
     *
     * @param modelCode 模型代码
     * @param fieldCode 字段代码
     * @return 是否存在该字段
     */
    public Boolean hasModelField(String modelCode, String fieldCode) {
        ModelFields modelFields = modelFieldsMap.get(modelCode);
        if (modelFields == null) {
            return false;
        }
        return modelFields.containsKey(fieldCode);
    }

    /**
     * 判断配置文件是否已修改
     *
     * @param userId 用户 ID
     * @return 是否已修改
     */
    public static Boolean isModify(String userId) {
        String json = null;
        java.io.File configV2File;
        if (StringUtil.isEmpty(userId)) {
            configV2File = Files.getDefaultConfigV2File();
        } else {
            configV2File = Files.getConfigV2File(userId);
        }
        if (configV2File.exists()) {
            json = Files.readFromFile(configV2File);
        }
        if (json != null) {
            String formatted = JsonUtil.formatJson(INSTANCE);
            return formatted == null || !formatted.equals(json);
        }
        return true;
    }

    /**
     * 保存配置文件
     *
     * @param userId 用户 ID
     * @param force  是否强制保存
     * @return 保存是否成功
     */
    public static synchronized Boolean save(String userId, Boolean force) {
        if (!force && !isModify(userId)) {
            return true;
        }
        String json;
        try {
            json = JsonUtil.formatJson(INSTANCE);
            if (json == null) {
                throw new IllegalStateException("配置格式化失败，返回的 JSON 为空");
            }
        } catch (Exception e) {
            Log.printStackTrace(TAG, e);
            Log.record("保存用户配置失败，格式化 JSON 时出错");
            return false;
        }
        boolean success;
        try {
            if (StringUtil.isEmpty(userId)) {
                userId = "默认";
                success = Files.setDefaultConfigV2File(json);
            } else {
                success = Files.setConfigV2File(userId, json);
            }
            if (!success) {
                throw new IOException("配置文件保存失败");
            }
            String userName;
            if (StringUtil.isEmpty(userId)) {
                userName = "默认用户";
            } else {
                UserEntity userEntity = UserMap.get(userId);
                userName = userEntity != null ? userEntity.getShowName() : "未知用户";
            }
            Log.record("保存用户[" + userName + "]配置");
        } catch (Exception e) {
            Log.printStackTrace(TAG, e);
            Log.record("保存用户配置失败");
            return false;
        }
        return true;
    }

    public static boolean isLoaded() {
        return INSTANCE.init;
    }

    /**
     * 加载配置文件
     *
     * @param userId 用户 ID
     * @return 配置是否成功加载
     */
    public static synchronized Config load(String userId) {
        Log.record(TAG, "开始加载配置");
        String userName = "";
        File configV2File = null;
        try {
            if (StringUtil.isEmpty(userId)) {
                configV2File = Files.getDefaultConfigV2File();
                userName = "默认";
                if (!configV2File.exists()) {
                    Log.record(TAG, "默认配置文件不存在，初始化新配置");
                    unload();
                    Files.write2File(toSaveStr(), configV2File);
                }
            } else {
                configV2File = Files.getConfigV2File(userId);
                UserEntity userEntity = UserMap.get(userId);
                userName = (userEntity == null) ? userId : userEntity.getShowName();
            }

            Log.record(TAG, "加载配置: " + userName);
            boolean configV2FileExists = configV2File.exists();
            boolean defaultConfigV2FileExists = Files.getDefaultConfigV2File().exists();

            if (configV2FileExists) {
                String json = Files.readFromFile(configV2File);
                Log.record(TAG, "读取配置文件成功: " + configV2File.getPath());
                JsonUtil.copyMapper().readerForUpdating(INSTANCE).readValue(json);
                Log.record(TAG, "反序列化配置成功");
                String formatted = toSaveStr();
                if (formatted != null && !formatted.equals(json)) {
                    Log.runtime(TAG, "格式化配置: " + userName);
                    Files.write2File(formatted, configV2File);
                }
            } else if (defaultConfigV2FileExists) {
                String json = Files.readFromFile(Files.getDefaultConfigV2File());
                JsonUtil.copyMapper().readerForUpdating(INSTANCE).readValue(json);
                Log.runtime(TAG, "复制新配置: " + userName);
                Files.write2File(json, configV2File);
            } else {
                unload();
                Log.runtime(TAG, "初始新配置: " + userName);
                Files.write2File(toSaveStr(), configV2File);
            }
        } catch (Throwable t) {
            Log.printStackTrace(TAG, t);
            Log.record(TAG, "重置配置: " + userName);
            try {
                unload();
                if (configV2File != null) {
                    Files.write2File(toSaveStr(), configV2File);
                }
            } catch (Exception e) {
                Log.printStackTrace(TAG, e);
                throw new RuntimeException("重置配置失败", e);
            }
        }
        INSTANCE.setInit(true);
        Log.record(TAG, "加载配置结束");
        return INSTANCE;
    }

    /**
     * 卸载当前配置
     */
    public static synchronized void unload() {
        for (ModelFields modelFields : INSTANCE.modelFieldsMap.values()) {
            for (ModelField<?> modelField : modelFields.values()) {
                if (modelField != null) {
                    modelField.reset();
                }
            }
        }
    }

    public static String toSaveStr() {
        return JsonUtil.formatJson(INSTANCE);
    }

}
