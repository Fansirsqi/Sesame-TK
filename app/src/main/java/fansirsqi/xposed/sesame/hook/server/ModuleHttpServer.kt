package fansirsqi.xposed.sesame.hook.server

import com.fasterxml.jackson.databind.ObjectMapper
import fansirsqi.xposed.sesame.hook.server.handlers.HttpHandler
import fansirsqi.xposed.sesame.hook.server.handlers.DebugHandler
import fansirsqi.xposed.sesame.util.Log
import fi.iki.elonen.NanoHTTPD

class ModuleHttpServer(
    port: Int = 8080,
    secretToken: String = ""
) : NanoHTTPD("0.0.0.0", port) {
    private val tag = "ModuleHttpServer"

    companion object {
        const val MIME_PLAINTEXT = "text/plain"
    }

    private val routes = mutableMapOf<String, HttpHandler>()
    private val pathDescriptions = mapOf(
        "/debugHandler" to "调试接口",
    )

    init {
        // 后续新增接口只需在这里注册即可

        register("/debugHandler", DebugHandler(secretToken))
    }

    private fun register(path: String, handler: HttpHandler) {
        Log.runtime(tag, "Registering handler : $pathDescriptions[$path]")
        routes[path] = handler
    }

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri
        val handler = routes[uri] ?: return notFound()

        // 如果是 POST 请求，读取 body
        var body: String? = null
        if (session.method === Method.POST) {
            body = getPostBody(session)
        }

        return handler.handle(session, body)
    }

    private fun getPostBody(session: IHTTPSession): String? {
        val size = session.headers["content-length"]?.toIntOrNull() ?: return null
        val buffer = ByteArray(size)
        session.inputStream.read(buffer, 0, size)
        return String(buffer)
    }

    private fun notFound(): Response {
        return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not Found")
    }
}