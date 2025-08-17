package fansirsqi.xposed.sesame.ui

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.appbar.MaterialToolbar
import fansirsqi.xposed.sesame.R
import fansirsqi.xposed.sesame.data.ViewAppInfo
import fansirsqi.xposed.sesame.util.PermissionUtil

open class BaseActivity : AppCompatActivity() {
    companion object {
        private const val REQUEST_EXTERNAL_STORAGE = 1
    }
    // Toolbar 懒加载
    protected val toolbar: MaterialToolbar by lazy { findViewById(R.id.x_toolbar) }

    // 基础标题
    open var baseTitle: String?
        get() = ViewAppInfo.appTitle
        set(value) {
            toolbar.title = value
        }

    // 基础副标题
    open var baseSubtitle: String?
        get() = null
        set(value) {
            toolbar.subtitle = value
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (PermissionUtil.checkFilePermissions(this)) {
            initialize()
        } else {
            PermissionUtil.checkOrRequestFilePermissions(this)
            ViewAppInfo.init(applicationContext)
        }
    }

    private fun initialize() {
        ViewAppInfo.init(applicationContext)
        // Edge-to-Edge 支持
        WindowCompat.setDecorFitsSystemWindows(window, false)
        // 控制状态栏文字颜色
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_EXTERNAL_STORAGE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                initialize()
            } else {
                Toast.makeText(this, "未获取文件读写权限", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    override fun onContentChanged() {
        super.onContentChanged()
        setSupportActionBar(toolbar)

        // 文字居中显示，MaterialToolbar 会自动处理状态栏高度
        toolbar.setContentInsetsAbsolute(0, 0)
        toolbar.title = baseTitle
        toolbar.subtitle = baseSubtitle
    }

    fun setBaseTitleTextColor(color: Int) {
        toolbar.setTitleTextColor(color)
    }

    override fun attachBaseContext(newBase: Context) {
        val configurationNew = Configuration(newBase.resources.configuration)
        val context = newBase.createConfigurationContext(configurationNew)
        super.attachBaseContext(context)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // 夜间模式变化时刷新 Activity
        if ((newConfig.diff(resources.configuration) and Configuration.UI_MODE_NIGHT_MASK) != 0) {
            recreate()
        } else {
            toolbar.title = baseTitle
            toolbar.subtitle = baseSubtitle
        }
    }
}
