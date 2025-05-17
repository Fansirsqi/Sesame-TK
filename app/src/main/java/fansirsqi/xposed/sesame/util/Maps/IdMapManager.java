package fansirsqi.xposed.sesame.util.Maps;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import fansirsqi.xposed.sesame.util.Files;
import fansirsqi.xposed.sesame.util.JsonUtil;
import fansirsqi.xposed.sesame.util.Log;
/**
 * 抽象ID映射工具类。
 * 提供通用的线程安全的ID映射功能，并支持单例管理。
 */
public abstract class IdMapManager {
    /**
     * 存储ID映射的并发HashMap。
     */
    private final Map<String, String> idMap = new ConcurrentHashMap<>();
    /**
     * 只读的ID映射。
     */
    private final Map<String, String> readOnlyIdMap = Collections.unmodifiableMap(idMap);
    private static final Map<Class<? extends IdMapManager>, IdMapManager> instances = new ConcurrentHashMap<>();
    public static <T extends IdMapManager> T getInstance(Class<T> clazz) {
        T instance = (T) instances.get(clazz); // 尝试从缓存中获取实例
        if (instance == null) { // 如果缓存中没有
            try {
                instance = clazz.getDeclaredConstructor().newInstance(); // 创建新实例
                instances.put(clazz, instance); // 将实例放入缓存
            } catch (Exception e) {
                throw new RuntimeException("Failed to create instance for " + clazz.getName(), e);
            }
        }
        return instance; // 返回实例
    }

    /**
     * 强制子类提供文件名。
     * @return 文件名。
     */
    protected abstract String thisFileName();
    /**
     * 获取只读的ID映射。
     * @return 只读的ID映射。
     */
    public Map<String, String> getMap() {
        return readOnlyIdMap;
    }
    /**
     * 根据键获取值。
     * @param key 键。
     * @return 键对应的值，如果不存在则返回null。
     */
    public String get(String key) {
        return idMap.get(key);
    }
    /**
     * 添加或更新ID映射。
     * @param key 键。
     * @param value 值。
     */
    public synchronized void add(String key, String value) {
        idMap.put(key, value);
    }
    /**
     * 从ID映射中删除键值对。
     * @param key 键。
     */
    public synchronized void remove(String key) {
        idMap.remove(key);
    }
    /**
     * 从文件加载ID映射。
     * @param userId 用户ID。
     */
    public synchronized void load(String userId) {
        idMap.clear();
        try {
            File file = Files.getTargetFileofUser(userId, thisFileName());
            String body = Files.readFromFile(file);
            if (!body.isEmpty()) {
                ObjectMapper objectMapper = new ObjectMapper();
                Map<String, String> newMap = objectMapper.readValue(body, new TypeReference<>() {});
                idMap.putAll(newMap);
            }
        } catch (Exception e) {
            Log.printStackTrace(e);
        }
    }
    public synchronized void load() {
        idMap.clear();
        try {
            File file = Files.getTargetFileofDir(Files.MAIN_DIR, thisFileName());
            String body = Files.readFromFile(file);
            if (!body.isEmpty()) {
                ObjectMapper objectMapper = new ObjectMapper();
                Map<String, String> newMap = objectMapper.readValue(body, new TypeReference<>() {});
                idMap.putAll(newMap);
            }
        } catch (Exception e) {
            Log.printStackTrace(e);
        }
    }
    /**
     * 将ID映射保存到文件。
     * @param userId 用户ID。
     * @return 如果保存成功返回true，否则返回false。
     */
    public synchronized boolean save(String userId) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String json = JsonUtil.formatJson(idMap);
//             json = objectMapper.writeValueAsString(idMap);
            File file = Files.getTargetFileofUser(userId, thisFileName());
            return Files.write2File(json, file);
        } catch (Exception e) {
            Log.printStackTrace(e);
            return false;
        }
    }
    public synchronized boolean save() {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String json = JsonUtil.formatJson(idMap);
//            String json = objectMapper.writeValueAsString(idMap);
            File file = Files.getTargetFileofDir(Files.MAIN_DIR, thisFileName());
            return Files.write2File(json, file);
        } catch (Exception e) {
            Log.printStackTrace(e);
            return false;
        }
    }
    /**
     * 清除ID映射。
     */
    public synchronized void clear() {
        idMap.clear();
    }
}
