package fansirsqi.xposed.sesame.hook.server.handlers

data class RpcRequest(
    val methodName: String,
    val requestData: String
)