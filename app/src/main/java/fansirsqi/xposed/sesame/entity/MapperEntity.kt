package fansirsqi.xposed.sesame.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import fansirsqi.xposed.sesame.util.HanziToPinyin

abstract class MapperEntity : Comparable<MapperEntity> {
    @JvmField
    public var name: String = ""
    @JvmField
    public var id: String = ""

    @JsonIgnore
    private var pinyinCache: List<String>? = null

    @JsonIgnore
    fun getPinyin(): List<String> = pinyinCache ?: run {
        HanziToPinyin.getInstance()
            .get(name)
            .map { it.target }
            .also { pinyinCache = it }
    }

    override fun compareTo(other: MapperEntity): Int {
        val list1 = getPinyin()
        val list2 = other.getPinyin()

        return list1.zip(list2) { a, b -> a.compareTo(b) }
            .firstOrNull { it != 0 }
            ?: (list1.size - list2.size)
    }
}
