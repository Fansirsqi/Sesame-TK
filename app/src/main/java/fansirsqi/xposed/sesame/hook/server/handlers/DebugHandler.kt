package fansirsqi.xposed.sesame.hook.server.handlers

import com.fasterxml.jackson.databind.JsonNode
import fansirsqi.xposed.sesame.hook.RequestManager
import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoHTTPD.IHTTPSession
import fi.iki.elonen.NanoHTTPD.Response

class DebugHandler(secretToken: String) : BaseHandler(secretToken) {

    override fun onGet(session: IHTTPSession): Response {
        return ok(mapOf("status" to "success", "method" to "GET", "message" to "Not implemented"))
    }

    override fun onPost(session: IHTTPSession, body: String?): Response {
        val jsonNode: JsonNode = try {
            mapper.readTree(body ?: return badRequest("Empty body"))
        } catch (e: Exception) {
            return badRequest("Invalid JSON: ${e.message}")
        }

        val methodName = jsonNode.get("methodName")?.asText() ?: return badRequest("Missing methodName")
        val requestDataNode = jsonNode.get("requestData") ?: return badRequest("Missing requestData")

        val requestData = when {
            requestDataNode.isTextual -> requestDataNode.asText()
            requestDataNode.isArray || requestDataNode.isObject -> mapper.writeValueAsString(requestDataNode)
            else -> null
        } ?: return badRequest("Invalid requestData format")

        val result = try {
            RequestManager.requestString(methodName, requestData)
        } catch (e: Exception) {
            return badRequest("RPC call failed: ${e.message}")
        }

        return NanoHTTPD.newFixedLengthResponse(Response.Status.OK, BaseHandler.MIME_JSON, result)
    }

}