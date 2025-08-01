package fansirsqi.xposed.sesame.entity

/**
 * 扩展功能项的数据类
 * @param name 功能名称
 * @param action 点击时执行的动作
 */
data class ExtendFunctionItem(
    val name: String,
    val action: () -> Unit
)