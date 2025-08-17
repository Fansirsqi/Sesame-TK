package fansirsqi.xposed.sesame.newutil

import android.content.Context
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.tencent.mmkv.MMKV

object MMKVSettingsManager {

    val objectMapper = jacksonObjectMapper()
    lateinit var mmkv: MMKV

    fun init(context: Context) {
        MMKV.initialize(context)
        mmkv = MMKV.mmkvWithID(
            "sesame-tk", // 唯一标识符
            MMKV.MULTI_PROCESS_MODE  //多进程访问
        )
    }

    fun ensureInit() {
        check(::mmkv.isInitialized) { "MMKVSettingsManager must be initialized before use" }
    }

    // ========= 基础类型 =========

    fun putBoolean(key: String, value: Boolean) {
        ensureInit()
        mmkv.encode(key, value)
    }

    fun getBoolean(key: String, def: Boolean = false): Boolean {
        ensureInit()
        return mmkv.decodeBool(key, def)
    }

    fun putString(key: String, value: String) {
        ensureInit()
        mmkv.encode(key, value)
    }

    fun getString(key: String, def: String = ""): String {
        ensureInit()
        return mmkv.decodeString(key, def) ?: def
    }

    // ========= 泛型对象 =========

    inline fun <reified T> getObject(key: String, def: T): T {
        ensureInit()
        val json = mmkv.decodeString(key) ?: return def
        return runCatching {
            objectMapper.readValue(json, object : TypeReference<T>() {})
        }.getOrElse {
            def // 解析失败就返回默认值
        }
    }

    fun <T> putObject(key: String, value: T) {
        ensureInit()
        val json = runCatching {
            objectMapper.writeValueAsString(value)
        }.getOrNull() ?: return
        mmkv.encode(key, json)
    }

    fun remove(key: String) {
        ensureInit()
        mmkv.remove(key)
    }

    fun contains(key: String): Boolean {
        ensureInit()
        return mmkv.containsKey(key)
    }
}
