package fansirsqi.xposed.sesame.data

import com.fasterxml.jackson.annotation.JsonIgnore
import fansirsqi.xposed.sesame.util.Files
import fansirsqi.xposed.sesame.util.JsonUtil
import fansirsqi.xposed.sesame.util.Log
import fansirsqi.xposed.sesame.util.RandomUtil
import fansirsqi.xposed.sesame.util.StringUtil
import java.io.File

/**
 * @author Byseven
 * @date 2025/2/21
 * @apiNote
 */
object DataCache {
    private const val TAG: String = "DataCache"
    private const val FILENAME = "dataCache.json"

    @get:JsonIgnore
    private var init = false

    // 光盘行动图片缓存
    val photoGuangPanList: MutableList<Map<String, String>> = ArrayList()

    init {
        // 在单例初始化时加载数据
        load()
    }

    private fun checkGuangPanPhoto(guangPanPhoto: Map<String, String>?): Boolean {
        if (guangPanPhoto == null) {
            return false
        }
        val beforeImageId = guangPanPhoto["before"]
        val afterImageId = guangPanPhoto["after"]
        return !StringUtil.isEmpty(beforeImageId)
                && !StringUtil.isEmpty(afterImageId)
                && beforeImageId != afterImageId
    }

    fun saveGuangPanPhoto(guangPanPhoto: Map<String, String>) : Boolean{
        if (!checkGuangPanPhoto(guangPanPhoto)) {
            Log.error(TAG,"传入的参数不合法：${guangPanPhoto}")
            return false
        }
        if (!photoGuangPanList.contains(guangPanPhoto)) {
            photoGuangPanList.add(guangPanPhoto)
        }
        return save()
    }

    // 动态获取光盘行动图片数量
    @get:JsonIgnore
    val guangPanPhotoCount: Int
        get() = photoGuangPanList.size

    fun clearGuangPanPhoto(): Boolean {
        photoGuangPanList.clear()
        return save()
    }

    @get:JsonIgnore
    val randomGuangPanPhoto: Map<String, String>?
        get() {
            if (photoGuangPanList.isEmpty()) {
                return null
            }
            val pos = RandomUtil.nextInt(0, photoGuangPanList.size - 1)
            val photo = photoGuangPanList[pos]
            return if (checkGuangPanPhoto(photo)) photo else null
        }

    private fun save(): Boolean {
        Log.record(TAG, "save DataCache")
        return Files.write2File(JsonUtil.formatJson(this), Files.getTargetFileofDir(Files.MAIN_DIR, FILENAME))
    }

    @Synchronized
    fun load() {
        if (init) return // 避免重复加载
        val targetFile = Files.getTargetFileofDir(Files.MAIN_DIR, FILENAME)
        try {
            if (targetFile.exists()) {
                val json = Files.readFromFile(targetFile)
                JsonUtil.copyMapper().readerForUpdating(this).readValue<Any>(json)
                val formatted = JsonUtil.formatJson(this)
                if (formatted != null && formatted != json) {
                    Log.runtime(TAG, "format $TAG config")
                    Files.write2File(formatted, targetFile)
                }
            } else {
                Log.runtime(TAG, "init $TAG config")
                JsonUtil.copyMapper().updateValue(this, DataCache)
                Files.write2File(JsonUtil.formatJson(this), targetFile)
            }
        } catch (e: Exception) {
            reset(targetFile)
            Log.error(TAG, "重置缓存，会影响光盘等配置${e.message}")
        }
        init = true
    }

    @Synchronized
    fun reset(targetFile: File?) {
        try {
            JsonUtil.copyMapper().updateValue(this, DataCache)
            Files.write2File(JsonUtil.formatJson(this), targetFile)
            Log.runtime(TAG, "reset $TAG config success")
        } catch (e: Exception) {
            Log.printStackTrace(TAG, e)
            Log.error(TAG, "reset $TAG config failed")
        }
    }
}