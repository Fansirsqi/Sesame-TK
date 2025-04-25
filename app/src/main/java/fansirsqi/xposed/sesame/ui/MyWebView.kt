package fansirsqi.xposed.sesame.ui

import android.content.Context
import android.util.AttributeSet
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient

/**
 * 自定义 WebView 类，提供默认的初始化设置和滚动到底部的功能。
 */
class MyWebView : WebView {
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
        // 设置 WebViewClient 以处理页面加载完成事件
        webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                // 如果 URL 以 .log 结尾，则尝试滚动到底部
                if (url.endsWith(".log")) {
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
}