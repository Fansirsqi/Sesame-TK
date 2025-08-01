package fansirsqi.xposed.sesame.task.antFarm

object FarmUtil {

    val syncAnimalType = listOf(
        "all" to listOf("SYNC_RESUME", "QUERY_ALL"),//全局同步
        "after_fence" to listOf("SYNC_AFTER_TOOL_FENCE", "QUERY_FARM_INFO"),//使用道具后同步
        "after_accelerate" to listOf("SYNC_AFTER_TOOL_ACCELERATE", "QUERY_FARM_INFO"),//加速后同步
        "after_hire" to listOf("SYNC_AFTER_HIRE_DONE", "QUERY_FARM_INFO"),//雇佣完成后同步
        "after_feed" to listOf("SYNC_AFTER_FEED_ANIMAL", "QUERY_EMOTION_INFO|QUERY_ORCHARD_RIGHTS"),//喂食完成后同步
    )

}