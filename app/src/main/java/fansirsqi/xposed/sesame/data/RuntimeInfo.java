package fansirsqi.xposed.sesame.data;
import org.json.JSONException;
import org.json.JSONObject;
import fansirsqi.xposed.sesame.util.Files;
import fansirsqi.xposed.sesame.util.Log;
import fansirsqi.xposed.sesame.util.Maps.UserMap;
import java.util.Objects;
/**
 * RuntimeInfo 用于存储和管理运行时的配置信息。
 * 该类提供了获取、保存、更新运行时信息的功能，并基于用户 ID 区分不同的配置。
 */
public class RuntimeInfo {
    private static final String TAG = RuntimeInfo.class.getSimpleName();
    // 当前单例实例
    private static RuntimeInfo instance;
    // 当前用户 ID
    private final String userId;
    // 存储所有运行时信息的 JSON 对象
    private JSONObject joAll;
    // 存储当前用户运行时信息的 JSON 对象
    private JSONObject joCurrent;
    /**
     * 枚举类型，定义所有可以存储和获取的运行时信息的键
     */
    public enum RuntimeInfoKey {
        ForestPauseTime // 示例键
    }
    /**
     * 获取 RuntimeInfo 的单例实例。
     * 如果当前用户的 ID 与之前不同，则会重新创建实例。
     *
     * @return 返回 RuntimeInfo 的单例实例
     */
    public static RuntimeInfo getInstance() {
        if (instance == null || !Objects.equals(instance.userId, UserMap.getCurrentUid())) {
            instance = new RuntimeInfo();
        }
        return instance;
    }
    /**
     * 构造函数，初始化当前用户的运行时信息。
     * 从文件中读取运行时数据，并初始化相关的 JSON 对象。
     */
    private RuntimeInfo() {
        userId = UserMap.getCurrentUid();
        String content = Files.readFromFile(Files.runtimeInfoFile(userId));
        // 如果文件读取成功，则解析 JSON 数据，否则初始化为空的 JSON 对象
        try {
            joAll = new JSONObject(content);
        } catch (Exception ignored) {
            joAll = new JSONObject();
        }
        // 确保 "joAll" 中包含当前用户的条目
        try {
            if (!joAll.has(userId)) {
                joAll.put(userId, new JSONObject());
            }
        } catch (Exception ignored) {
        }
        // 获取当前用户的运行时信息
        try {
            joCurrent = joAll.getJSONObject(userId);
        } catch (Exception ignored) {
            joCurrent = new JSONObject();
        }
    }
    /**
     * 将运行时信息保存到文件中。
     */
    public synchronized void save() {
        Files.write2File(joAll.toString(), Files.runtimeInfoFile(userId));
    }
    /**
     * 获取指定键的值（Object 类型）。如果该键不存在，返回 null。
     *
     * @param key 键
     * @return 键对应的值
     * @throws JSONException 可能抛出的异常
     */
    public Object get(RuntimeInfoKey key) throws JSONException {
        return joCurrent.opt(key.name());
    }
    /**
     * 根据键获取对应的字符串值。如果键不存在，返回空字符串。
     *
     * @param key 键
     * @return 对应的字符串值
     */
    public String getString(String key) {
        return joCurrent.optString(key);
    }
    /**
     * 根据键获取对应的 long 值。如果键不存在，返回默认值。
     *
     * @param key  键
     * @param def 默认值
     * @return 对应的 long 值
     */
    public Long getLong(String key, long def) {
        return joCurrent.optLong(key, def);
    }
    /**
     * 根据键获取对应的布尔值。如果键不存在，返回默认值。
     *
     * @param key  键
     * @param def 默认值
     * @return 对应的布尔值
     */
    public boolean getBool(String key, boolean def) {
        return joCurrent.optBoolean(key, def);
    }
    /**
     * 根据枚举键获取对应的字符串值。
     *
     * @param key 键（枚举值）
     * @return 对应的字符串值
     */
    public String getString(RuntimeInfoKey key) {
        return joCurrent.optString(key.name());
    }
    /**
     * 根据枚举键获取对应的 long 值。如果键不存在，返回默认值 0L。
     *
     * @param key 键（枚举值）
     * @return 对应的 long 值
     */
    public Long getLong(RuntimeInfoKey key) {
        return joCurrent.optLong(key.name(), 0L);
    }
    /**
     * 使用枚举键将值存储到当前用户的运行时信息中。
     *
     * @param key   键（枚举值）
     * @param value 存储的值
     */
    public void put(RuntimeInfoKey key, Object value) {
        put(key.name(), value);
    }
    /**
     * 根据键将值存储到当前用户的运行时信息中。
     *
     * @param key   键
     * @param value 存储的值
     */
    public void put(String key, Object value) {
        try {
            joCurrent.put(key, value);
            joAll.put(userId, joCurrent);
        } catch (JSONException e) {
            // 错误日志
            Log.runtime(TAG, "put err:");
            Log.printStackTrace(TAG, e);
        }
        // 保存数据到文件
        save();
    }
}
