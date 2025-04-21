package fansirsqi.xposed.sesame.ui

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import fansirsqi.xposed.sesame.R
import fansirsqi.xposed.sesame.data.ViewAppInfo

open class BaseActivity : AppCompatActivity() {
    private lateinit var toolbar: Toolbar

    /**
     * 在创建活动时调用，初始化基础设置。
     *
     * @param savedInstanceState 之前保存的实例状态
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ViewAppInfo.init(applicationContext)
        
        // 使用WindowInsetsControllerCompat处理状态栏
        WindowCompat.setDecorFitsSystemWindows(window, false)
        @Suppress("DEPRECATION")
        window.statusBarColor = Color.TRANSPARENT
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.isAppearanceLightStatusBars = true
        }
    }

    /**
     * 当活动的内容视图发生变化时调用。
     * 设置工具栏并初始化标题和副标题。
     */
    override fun onContentChanged() {
        super.onContentChanged()
        // 查找并设置工具栏
        toolbar = findViewById(R.id.x_toolbar)
        toolbar.setTitle(baseTitle)
        toolbar.setSubtitle(baseSubtitle)
        // 设置工具栏为支持操作栏
        setSupportActionBar(toolbar)
    }

    var baseTitle: String?
        get() = ViewAppInfo.appTitle
        set(title) {
            toolbar.title = title
        }
    open var baseSubtitle: String?
        get() = null
        set(subTitle) {
            toolbar.subtitle = subTitle
        }

    fun setBaseTitleTextColor(color: Int) {
        toolbar.setTitleTextColor(color)
    }

    fun setBaseSubtitleTextColor(color: Int) {
        toolbar.setSubtitleTextColor(color)
    }

    override fun attachBaseContext(newBase: Context) {
        val configuration = newBase.resources.configuration
        // 创建新的Configuration对象
        val configurationNew = Configuration(configuration)
        // 创建新的Context配置
        val context = newBase.createConfigurationContext(configurationNew)
        super.attachBaseContext(context)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // 当UI模式（日夜模式）发生变化时，重建Activity以应用新主题
        if ((newConfig.diff(resources.configuration) and Configuration.UI_MODE_NIGHT_MASK) != 0) {
            recreate()
        } else {
            toolbar.title = baseTitle
            toolbar.subtitle = baseSubtitle
        }
    }
}
