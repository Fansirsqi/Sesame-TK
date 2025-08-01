package fansirsqi.xposed.sesame.hook.server.handlers

import com.fasterxml.jackson.databind.ObjectMapper
import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoHTTPD.Response
import fi.iki.elonen.NanoHTTPD.IHTTPSession
import fi.iki.elonen.NanoHTTPD.Method

abstract class BaseHandler(private val secretToken: String) : HttpHandler {

    protected val mapper = ObjectMapper()

    final override fun handle(session: IHTTPSession, body: String?): Response {
        if (!verifyToken(session)) {
            return unauthorized()
        }

        val method = session.method
        return when {
            method === Method.GET -> onGet(session)
            method === Method.POST -> onPost(session, body)
            else -> methodNotAllowed()
        }
    }

    private fun verifyToken(session: IHTTPSession): Boolean {
        val authHeader = session.headers["authorization"] ?: return false
        if (!authHeader.startsWith("Bearer ", ignoreCase = true)) {
            return false
        }
        val token = authHeader.substring(7).trim()
        return token == secretToken
    }

    open fun onGet(session: IHTTPSession): Response {
        return notFound()
    }

    open fun onPost(session: IHTTPSession, body: String?): Response {
        return notFound()
    }

    protected fun json(status: Response.Status, data: Map<String, Any?>): Response {
        return NanoHTTPD.newFixedLengthResponse(status, MIME_JSON, mapper.writeValueAsString(data))
    }

    protected fun ok(data: Map<String, Any?>): Response {
        return json(Response.Status.OK, data)
    }

    protected fun badRequest(message: String): Response {
        return json(Response.Status.BAD_REQUEST, mapOf("status" to "error", "message" to message))
    }

    protected fun unauthorized(): Response {
        return json(Response.Status.UNAUTHORIZED, mapOf("status" to "unauthorized"))
    }

    protected fun methodNotAllowed(): Response {
        return json(Response.Status.METHOD_NOT_ALLOWED, mapOf("status" to "method_not_allowed"))
    }

    protected fun notFound(): Response {
        return json(Response.Status.NOT_FOUND, mapOf("status" to "not_found"))
    }

    companion object {
        const val MIME_JSON = "application/json"
    }
}