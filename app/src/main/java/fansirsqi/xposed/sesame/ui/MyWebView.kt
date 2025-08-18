package fansirsqi.xposed.sesame.ui

import android.content.Context
import android.util.AttributeSet
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.CharsetDecoder
import java.nio.charset.CodingErrorAction
//import android.util.Log

/**
 * 自定义 WebView 类，提供默认的初始化设置和滚动到底部的功能。
 */
class MyWebView : WebView {
/// 日志实时显示 begin
    private var pollRunnable: Runnable? = null
    private var raf: RandomAccessFile? = null
    private var watchingFile: File? = null
/// 日志实时显示 end
    /**
     * 构造函数，用于当没有 AttributeSet 参数时。
     *
     * @param c Context 对象
     */
    constructor(c: Context) : super(c) {
        defInit()
    }

    /**
     * 构造函数，用于当有 AttributeSet 参数时。
     *
     * @param context Context 对象
     * @param attrs   AttributeSet 对象
     */
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        defInit()
    }

    /**
     * 构造函数，用于当有 AttributeSet 和 defStyleAttr 参数时。
     *
     * @param context     Context 对象
     * @param attrs       AttributeSet 对象
     * @param defStyleAttr 默认样式属性
     */
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        defInit()
    }

    /**
     * 构造函数，用于当有 AttributeSet、defStyleAttr 和 defStyleRes 参数时。
     *
     * @param context    Context 对象
     * @param attrs      AttributeSet 对象
     * @param defStyleAttr 默认样式属性
     * @param defStyleRes 默认样式资源
     */
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
        defInit()
    }

    /**
     * 默认初始化方法，设置 WebView 的一些默认属性。
     */
    private fun defInit() {
        val settings = settings
        settings.setSupportZoom(true) // 支持缩放
        settings.builtInZoomControls = true // 显示内置缩放控件
        settings.displayZoomControls = false // 不显示缩放控件
        settings.useWideViewPort = false // 不使用宽视图端口
        settings.layoutAlgorithm = WebSettings.LayoutAlgorithm.NORMAL // 设置布局算法为 NORMAL
        settings.allowFileAccess = true // 允许访问文件
        settings.javaScriptEnabled = true
        addJavascriptInterface(object {
            @android.webkit.JavascriptInterface
            fun appendLog(log: String) {
                post {
                    evaluateJavascript("appendLog(${log.quoteJsString()})", null)
                }
            }
        }, "LogAppender")
        // 设置 WebViewClient 以处理页面加载完成事件
        webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                // 如果 URL 以 .log 结尾，则尝试滚动到底部
                if (url.endsWith(".log")) {
                    // 初次滚动到底部
                        postDelayed(object : Runnable {
                        override fun run() {
                            // 如果内容高度为 0，则每隔 100 毫秒检查一次，直到有内容
                            if (contentHeight == 0) {
                                postDelayed(this, 100)
                            } else {
                                scrollToBottom() // 滚动到底部
                            }
                        }
                    }, 500) // 延迟 500 毫秒执行
                }
            }
        }
    }

    /**
     * 滚动到 WebView 的底部。
     */
    fun scrollToBottom() {
        // 计算垂直滚动范围并滚动到底部
        scrollTo(0, computeVerticalScrollRange())
    }

/// 日志实时显示 begin
    // 持久化的 UTF-8 增量解码器 + 未解码尾巴
    private val utf8Decoder: CharsetDecoder = Charsets.UTF_8
        .newDecoder()
        .onMalformedInput(CodingErrorAction.REPORT)
        .onUnmappableCharacter(CodingErrorAction.REPORT)

    private var undecodedTail = ByteArray(0)

    fun startWatchingIncremental(path: String) {
        stopWatchingIncremental()

        val f = File(path)
        watchingFile = f
        raf = RandomAccessFile(f, "r").apply {
            // 如果只想看新增，从文件末尾开始；若想包含历史，注释掉下一行
            seek(f.length())
        }

        pollRunnable = object : Runnable {
            override fun run() {
                val file = watchingFile
                val r = raf
                if (file == null || r == null) return

                try {
                    val newLen = file.length()
                    // 文件被清空/轮转：回到开头
                    if (newLen < r.filePointer) r.seek(0)

                    if (newLen > r.filePointer) {
                        val chunk = readBytes(r, newLen)           // 纯字节
                        val text = decodeUtf8Incremental(chunk)    // 增量 UTF-8 解码（防截断）
                        if (text.isNotEmpty()) {
                            post {
                                evaluateJavascript("appendLog(${text.quoteJsString()});", null)
                            }
                        }
                    }
                } catch (_: Throwable) {
                    // 需要可加你的日志打印
                } finally {
                    postDelayed(this, 1000) // 500ms/次 更顺滑；可按需调
                }
            }
        }
        post(pollRunnable!!)
    }

    fun stopWatchingIncremental() {
//        Log.i("MyWebView","stopWatchingIncremental");
        pollRunnable?.let { removeCallbacks(it) }
        pollRunnable = null
        try { raf?.close() } catch (_: Throwable) {}
        raf = null
        watchingFile = null
        // 重置解码器状态与尾巴
        utf8Decoder.reset()
        undecodedTail = ByteArray(0)
    }

    override fun onDetachedFromWindow() {
        // WebView 被从视图树移除或销毁时自动停止
        stopWatchingIncremental()
        super.onDetachedFromWindow()
    }

    /** 从 r.filePointer 读到 upto（不包含） */
    private fun readBytes(r: RandomAccessFile, upto: Long): ByteArray {
        val len = (upto - r.filePointer).toInt()
        val buf = ByteArray(len)
        r.readFully(buf)
        return buf
    }

    /** 增量 UTF-8 解码：把不完整的多字节序列留到下一轮 */
    private fun decodeUtf8Incremental(newBytes: ByteArray): String {
        // 拼上上轮没解开的尾巴
        val all = if (undecodedTail.isNotEmpty()) {
            ByteArray(undecodedTail.size + newBytes.size).also {
                System.arraycopy(undecodedTail, 0, it, 0, undecodedTail.size)
                System.arraycopy(newBytes, 0, it, undecodedTail.size, newBytes.size)
            }
        } else newBytes

        val inBuf = ByteBuffer.wrap(all)
        // 经验值：UTF-8 最大 4 字节/字符，留点富余
        val outBuf = CharBuffer.allocate((inBuf.remaining() * utf8Decoder.maxCharsPerByte()).toInt() + 8)

        // endOfInput=false：允许结尾出现不完整序列，不当成错误
        val result = utf8Decoder.decode(inBuf, outBuf, /*endOfInput*/ false)

        // 剩余未消费的字节（一般是不完整的多字节序列），留到下一轮
        undecodedTail = if (inBuf.hasRemaining()) {
            ByteArray(inBuf.remaining()).also { inBuf.get(it) }
        } else ByteArray(0)

        outBuf.flip()
        return outBuf.toString()
    }

    /** 把任意文本安全变成 JS 字符串字面量 */
    fun String.quoteJsString(): String {
        if (isEmpty()) return "''"
        val sb = StringBuilder(length + 16)
        sb.append('\'')
        for (ch in this) {
            when (ch) {
                '\'' -> sb.append("\\'")
                '\\' -> sb.append("\\\\")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                '\b' -> sb.append("\\b")
                '\u000C' -> sb.append("\\f")
                else -> if (ch < ' ') sb.append(String.format("\\u%04x", ch.code)) else sb.append(ch)
            }
        }
        sb.append('\'')
        return sb.toString()
    }
/// 日志实时显示 end
}