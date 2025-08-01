package fansirsqi.xposed.sesame.data

import lombok.Getter

/**
 * 运行状态枚举类，用于表示不同的运行状态。
 * 每个枚举值代表一种状态，包含状态码和状态描述。
 */
@Getter
enum class RunType(val code: Int, val nickName: String) {
    DISABLE(0, "未激活"),
    ACTIVE(1, "已激活"),
    LOADED(2, "已加载");

    companion object {
        private val codeMap = entries.associateBy { it.code }
        /**
         * 根据状态码获取枚举实例
         * @param code 状态编码
         * @return 匹配的枚举实例，未找到时返回null
         */
        fun fromCode(code: Int): RunType? = codeMap[code]
    }
}
