package fansirsqi.xposed.sesame.entity

import fansirsqi.xposed.sesame.util.maps.IdMapManager
import fansirsqi.xposed.sesame.util.maps.MemberBenefitsMap

class MemberBenefit(i: String, n: String) : MapperEntity() {

    init {
        id = i
        name = n
    }

    companion object {
        fun getList(): List<MemberBenefit> {
            return IdMapManager.getInstance(MemberBenefitsMap::class.java).map
                .map { (key, value) -> MemberBenefit(key, value) }
        }
    }
}