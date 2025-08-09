package fansirsqi.xposed.sesame.util;

import android.content.Context;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.type.TypeFactory;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class JsonUtil {
    private static final String TAG = "JsonUtil";
    private static final ObjectMapper MAPPER = new ObjectMapper(); // JSON对象映射器
    public static final TypeFactory TYPE_FACTORY = TypeFactory.defaultInstance(); // 类型工厂
    public static final JsonFactory JSON_FACTORY = new JsonFactory(); // JSON工厂

    static {
        // 配置 ObjectMapper
        MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false); // 忽略未知属性
        MAPPER.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false); // 忽略空对象
        MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL); // 忽略空属性
        MAPPER.setTimeZone(TimeZone.getDefault()); // 设置时区
        MAPPER.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())); // 设置日期格式
    }

    public static ObjectMapper copyMapper() {
        return MAPPER.copy(); // 复制 ObjectMapper
    }

    /**
     * 将对象转换为格式化的 JSON 字符串
     *
     * @param object 要转换的对象
     * @return 格式化后的 JSON 字符串
     */
    public static String formatJson(Object object) {
        try {
            if (object instanceof JSONObject) {
                return ((JSONObject) object).toString(4); // 使用 4 个空格进行缩进
            }
            return execute(() -> MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(object));
        } catch (Exception e) {
            Log.runtime(TAG,"formatJson err:");
            Log.printStackTrace(e);
            return execute(() -> MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(object));
        }
    }

    /**
     * 将对象转换为 JSON 字符串
     *
     * @param object 要转换的对象
     * @param pretty 是否格式化 JSON 字符串
     * @return JSON 字符串
     */
    public static String formatJson(Object object, boolean pretty) {
        try {
            if (object instanceof JSONObject) {
                if (pretty) {
                    return ((JSONObject) object).toString(4);
                } else {
                    return object.toString();
                }
            }
            if (pretty) {
                return execute(() -> MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(object));
            } else {
                return execute(() -> MAPPER.writeValueAsString(object));
            }
        } catch (Exception e) {
            Log.runtime(TAG,"formatJson err:");
            Log.printStackTrace(e);
            return execute(() -> MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(object));
        }
    }

    /**
     * 创建 JSON 解析器
     *
     * @param body JSON 字符串
     * @return JsonParser 解析器
     */
    public static JsonParser getJsonParser(String body) {
        return execute(() -> JSON_FACTORY.createParser(body)); // 执行解析器创建
    }

    /**
     * 解析 JSON 字符串为指定类型的对象
     *
     * @param body JSON 字符串
     * @param type 目标类型
     * @param <T>  目标类型泛型
     * @return 解析后的对象
     */
    public static <T> T parseObject(String body, Type type) {
        return parseObjectInternal(() -> MAPPER.readValue(body, TYPE_FACTORY.constructType(type))); // 执行解析
    }

    /**
     * 解析 JSON 字符串为指定类型的对象
     *
     * @param body     JSON 字符串
     * @param javaType 目标 JavaType
     * @param <T>      目标类型泛型
     * @return 解析后的对象
     */
    public static <T> T parseObject(String body, JavaType javaType) {
        return parseObjectInternal(() -> MAPPER.readValue(body, javaType)); // 执行解析
    }

    /**
     * 解析 JSON 字符串为指定类型的对象
     *
     * @param body         JSON 字符串
     * @param valueTypeRef 目标类型引用
     * @param <T>          目标类型泛型
     * @return 解析后的对象
     */
    public static <T> T parseObject(String body, TypeReference<T> valueTypeRef) {
        return parseObjectInternal(() -> MAPPER.readValue(body, valueTypeRef)); // 执行解析
    }

    /**
     * 解析 JSON 字符串为指定类型的对象
     *
     * @param body  JSON 字符串
     * @param clazz 目标类
     * @param <T>   目标类型泛型
     * @return 解析后的对象
     */
    public static <T> T parseObject(String body, Class<T> clazz) {
        return parseObjectInternal(() -> MAPPER.readValue(body, clazz)); // 执行解析
    }

    /**
     * 从 JsonParser 解析为指定类型的对象
     *
     * @param jsonParser JsonParser 实例
     * @param type       目标类型
     * @param <T>        目标类型泛型
     * @return 解析后的对象
     */
    public static <T> T parseObject(JsonParser jsonParser, Type type) {
        return parseObjectInternal(() -> MAPPER.readValue(jsonParser, TYPE_FACTORY.constructType(type))); // 执行解析
    }

    /**
     * 从 JsonParser 解析为指定类型的对象
     *
     * @param jsonParser JsonParser 实例
     * @param javaType   目标 JavaType
     * @param <T>        目标类型泛型
     * @return 解析后的对象
     */
    public static <T> T parseObject(JsonParser jsonParser, JavaType javaType) {
        return parseObjectInternal(() -> MAPPER.readValue(jsonParser, javaType)); // 执行解析
    }

    /**
     * 从 JsonParser 解析为指定类型的对象
     *
     * @param jsonParser   JsonParser 实例
     * @param valueTypeRef 目标类型引用
     * @param <T>          目标类型泛型
     * @return 解析后的对象
     */
    public static <T> T parseObject(JsonParser jsonParser, TypeReference<T> valueTypeRef) {
        return parseObjectInternal(() -> MAPPER.readValue(jsonParser, valueTypeRef)); // 执行解析
    }

    /**
     * 从 JsonParser 解析为指定类型的对象
     *
     * @param jsonParser JsonParser 实例
     * @param clazz      目标类
     * @param <T>        目标类型泛型
     * @return 解析后的对象
     */
    public static <T> T parseObject(JsonParser jsonParser, Class<T> clazz) {
        return parseObjectInternal(() -> MAPPER.readValue(jsonParser, clazz)); // 执行解析
    }

    /**
     * 将对象转换为指定类型的对象
     *
     * @param bean 源对象
     * @param type 目标类型
     * @param <T>  目标类型泛型
     * @return 转换后的对象
     */
    public static <T> T parseObject(Object bean, Type type) {
        return parseObjectInternal(() -> MAPPER.convertValue(bean, TYPE_FACTORY.constructType(type))); // 执行转换
    }

    /**
     * 将对象转换为指定类型的对象
     *
     * @param bean     源对象
     * @param javaType 目标 JavaType
     * @param <T>      目标类型泛型
     * @return 转换后的对象
     */
    public static <T> T parseObject(Object bean, JavaType javaType) {
        return parseObjectInternal(() -> MAPPER.convertValue(bean, javaType)); // 执行转换
    }

    /**
     * 将对象转换为指定类型的对象
     *
     * @param bean         源对象
     * @param valueTypeRef 目标类型引用
     * @param <T>          目标类型泛型
     * @return 转换后的对象
     */
    public static <T> T parseObject(Object bean, TypeReference<T> valueTypeRef) {
        return parseObjectInternal(() -> MAPPER.convertValue(bean, valueTypeRef)); // 执行转换
    }

    /**
     * 将对象转换为指定类型的对象
     *
     * @param bean  源对象
     * @param clazz 目标类
     * @param <T>   目标类型泛型
     * @return 转换后的对象
     */
    public static <T> T parseObject(Object bean, Class<T> clazz) {
        return parseObjectInternal(() -> MAPPER.convertValue(bean, clazz)); // 执行转换
    }

    /**
     * 解析 JSON 字符串中的指定字段为字符串
     *
     * @param body  JSON 字符串
     * @param field 指定字段名
     * @return 字段值
     */
    public static String parseString(String body, String field) {
        return execute(() -> {
            JsonNode node = MAPPER.readTree(body).get(field); // 获取字段节点
            return node != null ? node.asText() : null; // 返回字段值
        });
    }

    /**
     * 解析 JSON 字符串中的指定字段为整数
     *
     * @param body  JSON 字符串
     * @param field 指定字段名
     * @return 字段值
     */
    public static Integer parseInteger(String body, String field) {
        return execute(() -> {
            JsonNode node = MAPPER.readTree(body).get(field); // 获取字段节点
            return node != null ? node.asInt() : null; // 返回字段值
        });
    }

    /**
     * 解析 JSON 字符串中的指定字段为整数列表
     *
     * @param body  JSON 字符串
     * @param field 指定字段名
     * @return 字段值列表
     */
    public static List<Integer> parseIntegerList(String body, String field) {
        return execute(() -> {
            JsonNode node = MAPPER.readTree(body).get(field); // 获取字段节点
            return node != null ? MAPPER.convertValue(node, new TypeReference<List<Integer>>() {
            }) : null; // 返回字段值列表
        });
    }

    /**
     * 解析 JSON 字符串中的指定字段为布尔值
     *
     * @param body  JSON 字符串
     * @param field 指定字段名
     * @return 字段值
     */
    public static Boolean parseBoolean(String body, String field) {
        return execute(() -> {
            JsonNode node = MAPPER.readTree(body).get(field); // 获取字段节点
            return node != null ? node.asBoolean() : null; // 返回字段值
        });
    }

    /**
     * 解析 JSON 字符串中的指定字段为短整型
     *
     * @param body  JSON 字符串
     * @param field 指定字段名
     * @return 字段值
     */
    public static Short parseShort(String body, String field) {
        return execute(() -> {
            JsonNode node = MAPPER.readTree(body).get(field); // 获取字段节点
            return node != null ? (short) node.asInt() : null; // 返回字段值
        });
    }

    /**
     * 解析 JSON 字符串中的指定字段为字节型
     *
     * @param body  JSON 字符串
     * @param field 指定字段名
     * @return 字段值
     */
    public static Byte parseByte(String body, String field) {
        return execute(() -> {
            JsonNode node = MAPPER.readTree(body).get(field); // 获取字段节点
            return node != null ? (byte) node.asInt() : null; // 返回字段值
        });
    }

    /**
     * 解析 JSON 字符串为指定类型的对象列表
     *
     * @param body  JSON 字符串
     * @param clazz 目标类
     * @param <T>   目标类型泛型
     * @return 解析后的对象列表
     */
    public static <T> List<T> parseList(String body, Class<T> clazz) {
        return parseObjectInternal(() -> MAPPER.readValue(body, TYPE_FACTORY.constructCollectionType(ArrayList.class, clazz))); // 执行解析
    }

    /**
     * 将 JSON 字符串转换为 JsonNode
     *
     * @param json JSON 字符串
     * @return JsonNode 对象
     */
    public static JsonNode toNode(String json) {
        return json == null ? null : execute(() -> MAPPER.readTree(json)); // 执行转换
    }

    /**
     * 根据路径获取 JSON 对象中的值
     *
     * @param jsonObject JSON 对象
     * @param path       字段路径（以 "." 分隔）
     * @return 字段值
     */
    public static String getValueByPath(JSONObject jsonObject, String path) {
        Object value = getValueByPathObject(jsonObject, path); // 获取字段值
        return value == null ? "" : value.toString(); // 返回字段值的字符串形式
    }

    /**
     * 根据路径获取 JSON 对象中的值
     *
     * @param jsonObject JSON 对象
     * @param path       字段路径（以 "." 分隔）
     * @return 字段值
     */
    public static Object getValueByPathObject(JSONObject jsonObject, String path) {
        String[] parts = path.split("\\."); // 分割路径
        try {
            Object current = jsonObject; // 当前对象
            for (String part : parts) {
                if (current instanceof JSONObject) {
                    current = ((JSONObject) current).get(part); // 从 JSONObject 获取值
                } else if (current instanceof JSONArray) {
                    int index = Integer.parseInt(part.replaceAll("\\D", "")); // 获取数组索引
                    current = ((JSONArray) current).get(index); // 从 JSONArray 获取值
                } else {
                    current = new JSONObject(current.toString()).get(part); // 将当前对象转为 JSONObject 并获取值
                }
            }
            return current; // 返回最终的值
        } catch (Exception e) {
            return null; // 异常时返回 null
        }
    }

    /**
     * 将 JSONArray 转换为字符串列表
     *
     * @param jsonArray 源 JSONArray
     * @return 字符串列表
     */
    public static List<String> jsonArrayToList(JSONArray jsonArray) {
        List<String> list = new ArrayList<>(); // 创建列表
        for (int i = 0; i < jsonArray.length(); i++) {
            try {
                list.add(jsonArray.getString(i)); // 添加字符串到列表
            } catch (Exception e) {
                Log.printStackTrace(e); // 打印异常栈
                list.add(""); // 异常时添加空字符串
            }
        }
        return list; // 返回列表
    }

    /**
     * 内部方法，执行 JSON 操作并处理异常
     *
     * @param action JSON 操作
     * @param <T>    操作返回类型
     * @return 操作结果
     */
    private static <T> T parseObjectInternal(JsonAction<T> action) {
        return execute(action); // 执行操作
    }

    /**
     * 执行 JSON 操作并处理异常
     *
     * @param action JSON 操作
     * @param <T>    操作返回类型
     * @return 操作结果
     */
    private static <T> T execute(JsonAction<T> action) {
        try {
            return action.execute(); // 执行操作
        } catch (Exception e) {
            throw new RuntimeException(e); // 异常时抛出运行时异常
        }
    }

    /**
     * 函数式接口，用于执行 JSON 操作
     *
     * @param <T> 操作返回类型
     */
    @FunctionalInterface
    private interface JsonAction<T> {
        T execute() throws Exception; // 执行操作
    }
}
