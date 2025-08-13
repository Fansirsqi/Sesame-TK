package fansirsqi.xposed.sesame.task.ancientTree

import fansirsqi.xposed.sesame.hook.RequestManager.requestString

object AncientTreeRpcCall {
    private const val VERSION = "20230522"
    fun homePage(selectCityCode: String?): String {
        return requestString(
            "alipay.greenmatrix.rpc.h5.ancienttree.homePage",
            ("[{\"cityCode\":\"330100\",\"selectCityCode\":\"" + selectCityCode
                    + "\",\"source\":\"antforesthome\"}]")
        )
    }

    fun queryTreeItemsForExchange(cityCode: String?): String {
        return requestString(
            "alipay.antforest.forest.h5.queryTreeItemsForExchange",
            ("[{\"cityCode\":\"" + cityCode
                    + "\",\"itemTypes\":\"\",\"source\":\"chInfo_ch_appcenter__chsub_9patch\",\"version\":\""
                    + VERSION + "\"}]")
        )
    }

    fun districtDetail(districtCode: String?): String {
        return requestString(
            "alipay.greenmatrix.rpc.h5.ancienttree.districtDetail",
            "[{\"districtCode\":\"" + districtCode + "\",\"source\":\"antforesthome\"}]"
        )
    }

    fun projectDetail(ancientTreeProjectId: String?, cityCode: String?): String {
        return requestString(
            "alipay.greenmatrix.rpc.h5.ancienttree.projectDetail",
            ("[{\"ancientTreeProjectId\":\"" + ancientTreeProjectId
                    + "\",\"channel\":\"ONLINE\",\"cityCode\":\"" + cityCode
                    + "\",\"source\":\"ancientreethome\"}]")
        )
    }

    fun protect(activityId: String?, ancientTreeProjectId: String?, cityCode: String?): String {
        return requestString(
            "alipay.greenmatrix.rpc.h5.ancienttree.protect",
            ("[{\"ancientTreeActivityId\":\"" + activityId + "\",\"ancientTreeProjectId\":\""
                    + ancientTreeProjectId + "\",\"cityCode\":\"" + cityCode
                    + "\",\"source\":\"ancientreethome\"}]")
        )
    }
}
