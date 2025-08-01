package fansirsqi.xposed.sesame.data

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import fansirsqi.xposed.sesame.util.Files
import fansirsqi.xposed.sesame.util.JsonUtil
import fansirsqi.xposed.sesame.util.Log
import java.io.File

/**
 * @author Byseven
 * @date 2025/2/21
 * @apiNote
 */
object DataCache {
    private const val TAG: String = "DataCache"
    private const val FILENAME = "dataCache.json"
    private val FILE_PATH = Files.CONFIG_DIR

    @get:JsonIgnore
    private var init = false
    val dataMap: MutableMap<String, Any> = mutableMapOf()

    init {
        load()
    }

    fun <T> saveData(key: String, value: T): Boolean {
        if (value == null) {
            Log.error(TAG, "Value for key '$key' cannot be null.")
            return false
        }
        try {
            dataMap[key] = value as Any
            return save()
        } catch (e: Exception) {
            Log.printStackTrace(TAG, "save data for key '$key' failed", e)
        }
        return false
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> getData(key: String, defaultValue: T? = null): T? {
        Log.runtime(TAG, "get data for key '$key'")
        return dataMap[key] as? T ?: defaultValue
    }

    /**
     * 通用的反序列化方法，根据指定的 TypeReference 反序列化缓存数据
     * @param key 缓存数据的 key
     * @param typeReference 反序列化类型
     */
    fun <T> getDataWithType(key: String, typeReference: TypeReference<T>, defaultValue: T? = null): T? {
        Log.runtime(TAG, "get data for key '$key'")
        val value = dataMap[key]
        return try {
            if (value == null) {
                defaultValue
            } else {
                ObjectMapper().convertValue(value, typeReference)
            }
        } catch (e: Exception) {
            Log.error(TAG, "反序列化缓存失败：${e.message}")
            defaultValue
        }
    }

    fun removeData(key: String): Boolean {
        if (dataMap.containsKey(key)) {
            dataMap.remove(key)
            return save()
        }
        return false
    }


    @Synchronized
    private fun save(): Boolean {
        Log.runtime(TAG, "【SAVE】当前 dataMap 内容: $dataMap")
        val targetFile = File(FILE_PATH, FILENAME)
        val tempFile = File(targetFile.parent, "${targetFile.name}.tmp")
        return try {
            // 1. 序列化对象为格式化后的 JSON 字符串
            val json = JsonUtil.formatJson(this) ?: throw IllegalStateException("JSON 序列化失败")
            // 2. 写入临时文件
            tempFile.writeText(json)
            // 3. 原子性替换
            if (tempFile.exists()) {
                targetFile.delete()
                tempFile.renameTo(targetFile)
                true
            } else {
                Log.error(TAG, "临时文件写入失败")
                false
            }
        } catch (e: Exception) {
            Log.error(TAG, "保存缓存数据失败：${e.message}")
            false
        }
    }

    private fun cleanUpDataMap() {
        fun Any.deepClean(): Any? {
            return when (this) {
                is Map<*, *> -> {
                    @Suppress("UNCHECKED_CAST")
                    val mutable = (this as? MutableMap<Any?, Any?>) ?: this.toMutableMap()
                    val entries = mutable.toList() // 复制一份避免并发修改
                    for ((key, value) in entries) {
                        val cleanedValue = value?.deepClean()
                        if (cleanedValue == null) {
                            mutable.remove(key)
                        } else {
                            mutable[key] = cleanedValue
                        }
                    }
                    mutable
                }

                is Collection<*> -> {
                    // 处理 List 和 Set
                    val list = this.filterNotNull().mapNotNull { it.deepClean() }
                    if (list.isNotEmpty()) {
                        if (list.all { it is String }) {
                            // 如果全是字符串，做去重和过滤空值
                            list.distinct().filter { it is String && it.isNotEmpty() }
                        } else {
                            // 否则只保留非空且已清理的元素
                            list.distinct()
                        }
                    } else {
                        emptyList<Any>()
                    }
                }

                is String -> if (isNotEmpty()) this else null
                else -> this
            }
        }

        for ((key, value) in dataMap.toMap()) {
            Log.runtime(TAG, "【CLEANUP】处理 key: $key, value type: ${value.javaClass}")
            try {
                val cleanedValue = value.deepClean()
                if (cleanedValue == null || (cleanedValue is Collection<*> && cleanedValue.isEmpty())) {
                    dataMap.remove(key)
                } else {
                    dataMap[key] = cleanedValue
                }
            } catch (e: Exception) {
                Log.error(TAG, "清理键 '$key' 时出错: ${e.message}")
                dataMap.remove(key)
            }
        }
    }

    @Synchronized
    fun load(): Boolean {
        if (init) return true
        val oldFile = Files.getTargetFileofDir(Files.MAIN_DIR, FILENAME)
        val targetFile = Files.getTargetFileofDir(FILE_PATH, FILENAME)
        var success = false
        try {
            if (targetFile.exists()) {
                val json = Files.readFromFile(targetFile)
                ObjectMapper().readerForUpdating(this).readValue<Any>(json)
                cleanUpDataMap()
                val formatted = JsonUtil.formatJson(this)
                if (formatted != null && formatted != json) {
                    Log.runtime(TAG, "format $TAG config")
                    save() // 使用临时文件写入
                }
                if (oldFile.exists()) oldFile.delete()
            } else if (oldFile.exists()) {
                if (Files.copy(oldFile, targetFile)) {
                    val json = Files.readFromFile(targetFile)
                    ObjectMapper().readerForUpdating(this).readValue<Any>(json)
                    cleanUpDataMap()
                    val formatted = JsonUtil.formatJson(this)
                    if (formatted != null && formatted != json) {
                        Log.runtime(TAG, "format $TAG config")
                        save()
                    }
                    oldFile.delete()
                } else {
                    Log.error(TAG, "copy old config to new config failed")
                    return false
                }
            } else {
                Log.runtime(TAG, "init $TAG config")
                ObjectMapper().updateValue(this, DataCache)
                val formatted = JsonUtil.formatJson(this)
                if (formatted != null) {
                    save()
                }
            }
            success = true
        } catch (e: Exception) {
            Log.error(TAG, "加载缓存数据失败：${e.message}")
            // 尝试恢复默认配置
            ObjectMapper().updateValue(this, DataCache)
        } finally {
            init = success
        }
        return success
    }

}