package fansirsqi.xposed.sesame.entity

import java.io.Serial
import java.io.Serializable

open class KVMap<K, V> protected constructor() : Serializable {

    companion object {
        @Serial
        const val serialVersionUID: Long = 1L
    }

    constructor(key: K, value: V) : this() {
        this.key = key
        this.value = value
    }

    var key: K? = null
    var value: V? = null
}