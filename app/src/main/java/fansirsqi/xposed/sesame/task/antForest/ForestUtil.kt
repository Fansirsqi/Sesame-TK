@file:JvmName("ForestUtil")

package fansirsqi.xposed.sesame.task.antForest

import org.json.JSONObject

object ForestUtil {

    @JvmStatic
    fun hasShield(userHomeObj: JSONObject, serverTime: Long): Boolean {
        return hasPropGroup(userHomeObj, "shield", serverTime)
    }

    @JvmStatic
    fun hasBombCard(userHomeObj: JSONObject, serverTime: Long): Boolean {
        return hasPropGroup(userHomeObj, "energyBombCard", serverTime)
    }

    private fun hasPropGroup(userHomeObj: JSONObject, group: String, serverTime: Long): Boolean {
        val props = userHomeObj.optJSONArray("usingUserProps")
            ?: userHomeObj.optJSONArray("usingUserPropsNew")
            ?: return false
        return (0 until props.length()).any { i ->
            val prop = props.optJSONObject(i)
            prop?.optString("propGroup") == group && prop.optLong("endTime", 0L) > serverTime
        }
    }
}
