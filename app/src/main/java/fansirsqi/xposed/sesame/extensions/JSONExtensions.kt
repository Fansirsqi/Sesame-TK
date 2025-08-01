package fansirsqi.xposed.sesame.extensions

import org.json.JSONArray
import org.json.JSONException

// 所有JSON相关的扩展函数放在这里


/**
 * JSON 相关扩展函数集合
 */
object JSONExtensions {

    /**
     * 安全地将 List<String> 转换为 JSONArray
     * @receiver 字符串列表（允许为null，返回空JSONArray）
     */
    fun List<String>?.toSafeJSONArray(): JSONArray {
        return this?.let { JSONArray(it) } ?: JSONArray()
    }

    /**
     * 将 MutableList<String> 转换为 JSONArray
     * @receiver 可变的字符串集合
     * @throws JSONException 如果元素包含无效的JSON值
     */
    fun MutableList<String>.toJSONArray(): JSONArray = JSONArray(this)

    /**
     * 将 JSONArray 转换为 MutableList<String>
     * @receiver JSON数组对象
     * @throws JSONException 如果数组包含非字符串元素
     */
    fun JSONArray.toMutableStringList(): MutableList<String> {
        return MutableList(length()) { getString(it) }
    }

    /**
     * 安全地将 JSONArray 转换为 List<String>
     * @receiver JSON数组对象（允许为null，返回空列表）
     * @param default 转换失败时的默认值
     */
    fun JSONArray?.toSafeStringList(default: String = ""): List<String> {
        if (this == null) return emptyList()
        return try {
            List(length()) { getString(it) ?: default }
        } catch (e: JSONException) {
            emptyList()
        }
    }

    /**
     * 将 JSONArray 转换为指定类型的列表
     * @param transform 类型转换函数
     */
    inline fun <reified T> JSONArray.toList(transform: (Any?) -> T): List<T> {
        return List(length()) { transform(get(it)) }
    }

    /**
     * 检查 JSONArray 是否为空
     */
    fun JSONArray?.isNullOrEmpty(): Boolean {
        return this == null || length() == 0
    }

    /**
     * 如果 JSONArray 为null则返回空数组
     */
    fun JSONArray?.orEmpty(): JSONArray {
        return this ?: JSONArray()
    }
}


