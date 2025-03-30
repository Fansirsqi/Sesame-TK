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

    @JsonIgnore
    private var init = false

    // 光盘行动图片缓存
    private val photoGuangPanCacheSet: MutableSet<Map<String, String>> = HashSet()

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

    fun saveGuangPanPhoto(guangPanPhoto: Map<String, String>) {
        if (!checkGuangPanPhoto(guangPanPhoto)) {
            return
        }
        if (!photoGuangPanCacheSet.contains(guangPanPhoto)) {
            photoGuangPanCacheSet.add(guangPanPhoto)
            save()
        }
    }

    val guangPanPhotoCount: Int
        get() = photoGuangPanCacheSet.size // 直接返回缓存大小，避免触发加载

    fun clearGuangPanPhoto(): Boolean {
        photoGuangPanCacheSet.clear()
        return save()
    }

    val randomGuangPanPhoto: Map<String, String>?
        get() {
            val list: List<Map<String, String>> = ArrayList(photoGuangPanCacheSet)
            if (list.isEmpty()) {
                return null
            }
            val pos = RandomUtil.nextInt(0, list.size - 1)
            val photo = list[pos]
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