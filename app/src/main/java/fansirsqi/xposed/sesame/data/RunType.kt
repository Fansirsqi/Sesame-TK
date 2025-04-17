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
        private val CODE_MAP: MutableMap<Int, RunType> = HashMap()

        init {
            for (runType in entries) {
                CODE_MAP[runType.code] = runType
            }
        }

        fun getByCode(code: Int?): RunType? {
            return CODE_MAP[code]
        }

    }
}
