package fansirsqi.xposed.sesame.newutil

import android.os.Build
import androidx.annotation.RequiresApi
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.core.util.DefaultIndenter
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.File
import java.nio.file.StandardWatchEventKinds
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.thread
import kotlin.concurrent.write

object DataStore {
    private val mapper = jacksonObjectMapper()
    private val data = ConcurrentHashMap<String, Any>()
    private val lock = ReentrantReadWriteLock()
    private lateinit var storageFile: File

    fun init(dir: File) {
        storageFile = File(dir, "DataStore.json").apply {
            if (!exists()) createNewFile()
        }
        loadFromDisk()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startWatcherNio() else startWatcher()
    }

    inline fun <reified T : Any> DataStore.getOrCreate(key: String) = getOrCreate(key, object : TypeReference<T>() {})

    private fun checkInit() {
        if (!::storageFile.isInitialized)
            throw IllegalStateException("DataStore.init(dir) must be called first!")
    }

    fun <T> get(key: String, clazz: Class<T>): T? = lock.read {
        data[key]?.let { mapper.convertValue(it, clazz) }
    }


    /* -------------------------------------------------- */
    /*  类型安全读取：Class 版（基本 / 自定义对象）         */
    /* -------------------------------------------------- */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getOrCreate(key: String, clazz: Class<T>): T = lock.write {
        data[key]?.let { return mapper.convertValue(it, clazz) }
        val default: T = when (clazz) {
            /* 基本容器 */
            java.util.List::class.java -> mutableListOf<Any>() as T
            java.util.Set::class.java -> mutableSetOf<Any>() as T
            java.util.Map::class.java -> mutableMapOf<String, Any>() as T

            /* 基本包装类型 */
            String::class.java -> "" as T
            Boolean::class.java -> false as T
            Int::class.java -> 0 as T
            Long::class.java -> 0L as T
            Double::class.java -> 0.0 as T
            Float::class.java -> 0f as T

            /* 其它：尝试无参构造 */
            else -> clazz.getDeclaredConstructor().newInstance()
        }
        data[key] = default
        saveToDisk()
        default
    }

    /* -------------------------------------------------- */
    /*  类型安全读取：TypeReference 版（支持嵌套泛型）       */
    /* -------------------------------------------------- */
    fun <T : Any> getOrCreate(key: String, typeRef: TypeReference<T>): T = lock.write {
        data[key]?.let { return mapper.convertValue(it, typeRef) }
        val default: T = createDefault(typeRef)
        data[key] = default
        saveToDisk()
        default
    }


    /* 根据 TypeReference 创建默认实例（支持嵌套） */
    @Suppress("UNCHECKED_CAST")
    private fun <T> createDefault(typeRef: TypeReference<T>): T {
        mapper.typeFactory.constructType(typeRef)
        val raw = mapper.typeFactory.constructType(typeRef).rawClass
        return when (raw) {
            java.util.List::class.java -> mutableListOf<Any>() as T
            java.util.Set::class.java -> mutableSetOf<Any>() as T
            java.util.Map::class.java -> mutableMapOf<String, Any>() as T
            else -> raw.getDeclaredConstructor().newInstance() as T
        }
    }

    private fun loadFromDisk() {
        if (storageFile.length() == 0L) return
        val loaded: Map<String, Any> = mapper.readValue(storageFile)
        data.putAll(loaded)
    }

    private val prettyPrinter = DefaultPrettyPrinter().apply {
        indentArraysWith(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE)   // 数组换行
        indentObjectsWith(DefaultIndenter("    ", DefaultIndenter.SYS_LF)) // 对象换行 + 4 空格
    }

    private fun saveToDisk() {
        storageFile.writeText(mapper.writer(prettyPrinter).writeValueAsString(data))
    }

    private fun startWatcher() {
        Thread {
            var last = storageFile.lastModified()
            while (true) {
                Thread.sleep(1000)
                val current = storageFile.lastModified()
                if (current > last) {
                    last = current
                    loadFromDisk()
                }
            }
        }.apply { isDaemon = true }.start()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun startWatcherNio() = thread(isDaemon = true) {
        val path = storageFile.toPath().parent
        val watch = path.fileSystem.newWatchService()
        path.register(watch, StandardWatchEventKinds.ENTRY_MODIFY)
        while (true) {
            val key = watch.take()
            key.pollEvents().forEach {
                if (it.context().toString() == storageFile.name) loadFromDisk()
            }
            key.reset()
        }
    }

    /* -------------------------------------------------- */
    /*  简易 put / remove（可选）                          */
    /* -------------------------------------------------- */
    fun put(key: String, value: Any) = lock.write {
        data[key] = value
        saveToDisk()
    }

    fun remove(key: String) = lock.write {
        data.remove(key)
        saveToDisk()
    }
}