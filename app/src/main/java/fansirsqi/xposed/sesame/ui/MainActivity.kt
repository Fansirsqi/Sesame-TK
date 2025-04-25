package fansirsqi.xposed.sesame.ui

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.util.Consumer
import fansirsqi.xposed.sesame.R
import fansirsqi.xposed.sesame.data.General
import fansirsqi.xposed.sesame.data.RunType
import fansirsqi.xposed.sesame.data.Statistics
import fansirsqi.xposed.sesame.data.UIConfig
import fansirsqi.xposed.sesame.data.ViewAppInfo
import fansirsqi.xposed.sesame.entity.FriendWatch
import fansirsqi.xposed.sesame.entity.UserEntity
import fansirsqi.xposed.sesame.model.SelectModelFieldFunc
import fansirsqi.xposed.sesame.ui.widget.ListDialog
import fansirsqi.xposed.sesame.util.AssetUtil
import fansirsqi.xposed.sesame.util.Detector
import fansirsqi.xposed.sesame.util.FansirsqiUtil
import fansirsqi.xposed.sesame.util.FansirsqiUtil.OneWordCallback
import fansirsqi.xposed.sesame.util.Files
import fansirsqi.xposed.sesame.util.Log
import fansirsqi.xposed.sesame.util.Maps.UserMap
import fansirsqi.xposed.sesame.util.PermissionUtil
import fansirsqi.xposed.sesame.util.ThreadUtil
import fansirsqi.xposed.sesame.util.ToastUtil
import java.util.Calendar
import java.util.Random
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

//   欢迎自己打包 欢迎大佬pr
//   项目开源且公益  维护都是自愿
//   但是如果打包改个名拿去卖钱忽悠小白
//   那我只能说你妈死了 就当开源项目给你妈烧纸钱了
class MainActivity : BaseActivity() {
    private var hasPermissions = false
    private var isClick = false
    private lateinit var tvStatistics: TextView
    private var userNameArray = arrayOf("默认")
    private var userEntityArray = arrayOf<UserEntity?>(null)
    private lateinit var oneWord: TextView
    val emojiList =
        listOf(
            "🍅", "🍓", "🥓", "🍂", "🍚", "🌰", "🟢", "🌴",
            "🥗", "🧀", "🥩", "🍍", "🌶️", "🍲", "🍆", "🥕",
            "✨", "🍑", "🍘", "🍀", "🥞", "🍈", "🥝", "🧅",
            "🌵", "🌾", "🥜", "🍇", "🌭", "🥑", "🥐", "🥖",
            "🍊", "🌽", "🍉", "🍖", "🍄", "🥚", "🥙", "🥦",
            "🍌", "🍱", "🍏", "🍎", "🌲", "🌿", "🍁", "🍒",
            "🥔", "🌯", "🌱", "🍐", "🍞", "🍳", "🍙", "🍋",
            "🍗", "🌮", "🍃", "🥘", "🥒", "🧄", "🍠", "🥥"
        )


    @SuppressLint("UnspecifiedRegisterReceiverFlag", "SetTextI18n", "UnsafeDynamicallyLoadedCode")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ToastUtil.init(this) // 初始化全局 Context
        hasPermissions = PermissionUtil.checkOrRequestFilePermissions(this)
        if (!hasPermissions) {
            Toast.makeText(this, "未获取文件读写权限", Toast.LENGTH_LONG).show()
            finish() // 如果权限未获取，终止当前 Activity
            return
        }
        setContentView(R.layout.activity_main)
        val mainImage = findViewById<View>(R.id.main_image)
        tvStatistics = findViewById(R.id.tv_statistics)
        val buildVersion = findViewById<TextView>(R.id.bulid_version)
        val buildTarget = findViewById<TextView>(R.id.bulid_target)
        oneWord = findViewById(R.id.one_word)
        // 获取并设置一言句子
        ViewAppInfo.checkRunType()
        updateSubTitle(ViewAppInfo.runType?.nickName ?: "未激活")
        try {
            if (!AssetUtil.copySoFileToStorage(this, "libchecker.so")) {
                Log.error("so file copy failed")
            }
            val libSesamePath = Detector.getLibPath(this)
            if (libSesamePath != null) {
                System.load(libSesamePath)
            }
            Log.runtime("Loading so from original path$libSesamePath")
            Detector.initDetector(this)
        } catch (e: Exception) {
            Log.error("load libSesame err:" + e.message)
        }

        mainImage?.setOnLongClickListener { v: View ->
            // 当视图被长按时执行的操作
            if (v.id == R.id.main_image) {
                val data = "file://" + Files.getDebugLogFile().absolutePath
                val it = Intent(this@MainActivity, HtmlViewerActivity::class.java)
                it.putExtra("nextLine", false)
                it.putExtra("canClear", true)
                it.data = data.toUri()
                startActivity(it)
                return@setOnLongClickListener true // 表示事件已处理
            }
            false // 如果不是目标视图，返回false
        }
        val broadcastReceiver: BroadcastReceiver =
            object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    val action = intent.action
                    Log.runtime("module got broadcast:$action intent:$intent")
                    if (action != null) {
                        when (action) {
                            "fansirsqi.xposed.sesame.status" -> {
                                // 收到来自支付宝进程的确认广播
                                val gotRunType: String? = intent.getStringExtra("EXTRA_RUN_TYPE")
                                if (gotRunType != null) {
                                    when (gotRunType){
                                        RunType.DISABLE.nickName -> {
                                            ViewAppInfo.runType = RunType.DISABLE // 更新状态为 DISABLE
                                            updateSubTitle(RunType.DISABLE.nickName) // 更新 UI 显示为“未激活”
                                            Log.runtime("MainActivity received status confirmation: DISABLE")
                                        }
                                        RunType.ACTIVE.nickName -> {
                                            ViewAppInfo.runType = RunType.ACTIVE // 更新状态为 ACTIVE
                                            updateSubTitle(RunType.ACTIVE.nickName) // 更新 UI 显示为“已激活”
                                            Log.runtime("MainActivity received status confirmation: ACTIVE")
                                        }
                                        RunType.LOADED.nickName -> {
                                            ViewAppInfo.runType = RunType.LOADED // 更新状态为 LOADED
                                            updateSubTitle(RunType.LOADED.nickName) // 更新 UI 显示为“已加载”
                                            Log.runtime("MainActivity received status confirmation: LOADED")
                                        }
                                    }
                                }
                                if (isClick) {
                                    Handler(Looper.getMainLooper()).post {
                                        Toast.makeText(context, "${emojiList.random()} 一切看起来都很棒！", Toast.LENGTH_SHORT).show()
                                        Thread {
                                            ThreadUtil.sleep(200)
                                            runOnUiThread { isClick = false }
                                        }.start()
                                    }
                                }
                            }

                            "fansirsqi.xposed.sesame.update" -> {
                                Statistics.load()
                                tvStatistics.text = Statistics.getText()
                            }
                        }
                    }
                }
            }
        val intentFilter = IntentFilter()
        intentFilter.addAction("fansirsqi.xposed.sesame.status")
        intentFilter.addAction("fansirsqi.xposed.sesame.update")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(broadcastReceiver, intentFilter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(broadcastReceiver, intentFilter)
        }
        Statistics.load()
        tvStatistics.text = Statistics.getText()
        FansirsqiUtil.getOneWord(
            object : OneWordCallback {
                override fun onSuccess(result: String?) {
                    runOnUiThread { oneWord.text = result } // 在主线程中更新UI
                }

                override fun onFailure(error: String?) {
                    runOnUiThread { oneWord.text = error } // 在主线程中更新UI
                }
            })
        buildVersion.text = "Build Version: " + ViewAppInfo.appVersion // 版本信息
        buildTarget.text = "Build Target: " + ViewAppInfo.appBuildTarget // 编译日期信息
    }

    override fun onResume() {
        super.onResume()
        if (hasPermissions) {
            // 每次进入界面时都发送状态查询广播给支付宝进程，以确认 Hook 是否加载
            try {
                Log.runtime("MainActivity onResume: Sending status ping to Alipay process.")
                sendBroadcast(Intent("com.eg.android.AlipayGphone.sesame.status"))
            } catch (th: Throwable) {
                Log.runtime("view sendBroadcast status err:")
                Log.printStackTrace(th)
            }
            try { //打开设置前需要确认设置了哪个UI
                UIConfig.load()
            } catch (e: Exception) {
                Log.printStackTrace(e)
            }
            try {
                val userNameList: MutableList<String> = ArrayList()
                val userEntityList: MutableList<UserEntity?> = ArrayList()
                val configFiles = Files.CONFIG_DIR.listFiles()
                if (configFiles != null) {
                    for (configDir in configFiles) {
                        if (configDir.isDirectory) {
                            val userId = configDir.name
                            UserMap.loadSelf(userId)
                            val userEntity = UserMap.get(userId)
                            val userName = if (userEntity == null) {
                                userId
                            } else {
                                userEntity.showName + ": " + userEntity.account
                            }
                            userNameList.add(userName)
                            userEntityList.add(userEntity)
                        }
                    }
                }
                userNameList.add(0, "默认")
                userEntityList.add(0, null)
                userNameArray = userNameList.toTypedArray<String>()
                userEntityArray = userEntityList.toTypedArray<UserEntity?>()
            } catch (e: Exception) {
                userNameArray = arrayOf("默认")
                userEntityArray = arrayOf(null)
                Log.printStackTrace(e)
            }
            try {
                Statistics.load()
                Statistics.updateDay(Calendar.getInstance())
                tvStatistics.text = Statistics.getText()
            } catch (e: Exception) {
                Log.printStackTrace(e)
            }
        }
    }

    @SuppressLint("NonConstantResourceId")
    fun onClick(v: View) {
        if (v.id == R.id.main_image) {
            try {
                sendBroadcast(Intent("com.eg.android.AlipayGphone.sesame.status"))
                isClick = true
            } catch (th: Throwable) {
                Log.runtime("view sendBroadcast status err:")
                Log.printStackTrace(th)
            }
            return
        }
        var data = "file://"
        val id = v.id
        when (id) {
            R.id.btn_forest_log -> {
                data += Files.getForestLogFile().absolutePath
            }

            R.id.btn_farm_log -> {
                data += Files.getFarmLogFile().absolutePath
            }

            R.id.btn_other_log -> {
                data += Files.getOtherLogFile().absolutePath
            }

            R.id.btn_github -> {
                data = "https://github.com/Fansirsqi/Sesame-TK"
            }

            R.id.btn_settings -> {
                showSelectionDialog(
                    "📌 请选择配置",
                    userNameArray,
                    { index: Int -> this.goSettingActivity(index) },
                    "😡 老子就不选",
                    {},
                    true
                )
                return
            }

            R.id.btn_friend_watch -> {

                showSelectionDialog(
                    "🤣 请选择有效账户[别选默认]",
                    userNameArray,
                    { index: Int -> this.goFriendWatch(index) },
                    "😡 老子不选了，滚",
                    {},
                    false
                )


                return
            }

            R.id.one_word -> {
                Thread {
                    ToastUtil.showToastWithDelay(this@MainActivity, "😡 正在获取句子，请稍后……", 800)
                    ThreadUtil.sleep(5000)
                    FansirsqiUtil.getOneWord(
                        object : OneWordCallback {
                            override fun onSuccess(result: String?) {
                                runOnUiThread { oneWord.text = result } // 在主线程中更新UI
                            }

                            override fun onFailure(error: String?) {
                                runOnUiThread { oneWord.text = error } // 在主线程中更新UI
                            }
                        })
                }.start()
                return
            }
        }
        val it = Intent(this, HtmlViewerActivity::class.java)
        it.data = data.toUri()
        startActivity(it)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        try {
            // 使用清单文件中定义的完整别名
            val aliasComponent = ComponentName(this, General.MODULE_PACKAGE_UI_ICON)
            val state = packageManager.getComponentEnabledSetting(aliasComponent)
            // 注意状态判断逻辑修正
            val isEnabled = state != PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            menu.add(0, 1, 1, R.string.hide_the_application_icon)
                .setCheckable(true).isChecked = !isEnabled
            menu.add(0, 2, 2, R.string.view_error_log_file)
            menu.add(0, 3, 3, R.string.view_all_log_file)
            menu.add(0, 4, 4, R.string.view_runtim_log_file)
            menu.add(0, 5, 5, R.string.export_the_statistic_file)
            menu.add(0, 6, 6, R.string.import_the_statistic_file)
            menu.add(0, 7, 7, R.string.view_capture)
            menu.add(0, 8, 8, R.string.extend)
            menu.add(0, 9, 9, R.string.settings)
            menu.add(0, 10, 10, "🧹 清空配置")
        } catch (e: Exception) {
            Log.printStackTrace(e)
            ToastUtil.makeText(this, "菜单创建失败，请重试", Toast.LENGTH_SHORT).show()
            return false
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            1 -> {
                val shouldHide = !item.isChecked
                item.isChecked = shouldHide

                val aliasComponent = ComponentName(this, General.MODULE_PACKAGE_UI_ICON)
                val newState = if (shouldHide) {
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                } else {
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                }

                packageManager.setComponentEnabledSetting(
                    aliasComponent,
                    newState,
                    PackageManager.DONT_KILL_APP
                )

                // 提示用户需要重启启动器才能看到效果
                Toast.makeText(this, "设置已保存，可能需要重启桌面才能生效", Toast.LENGTH_SHORT).show()
                return true
            }

            2 -> {
                var errorData = "file://"
                errorData += Files.getErrorLogFile().absolutePath
                val errorIt = Intent(this, HtmlViewerActivity::class.java)
                errorIt.putExtra("nextLine", false)
                errorIt.putExtra("canClear", true)
                errorIt.data = errorData.toUri()
                startActivity(errorIt)
            }

            3 -> {
                var recordData = "file://"
                recordData += Files.getRecordLogFile().absolutePath
                val otherIt = Intent(this, HtmlViewerActivity::class.java)
                otherIt.putExtra("nextLine", false)
                otherIt.putExtra("canClear", true)
                otherIt.data = recordData.toUri()
                startActivity(otherIt)
            }

            4 -> {
                var runtimeData = "file://"
                runtimeData += Files.getRuntimeLogFile().absolutePath
                val allIt = Intent(this, HtmlViewerActivity::class.java)
                allIt.putExtra("nextLine", false)
                allIt.putExtra("canClear", true)
                allIt.data = runtimeData.toUri()
                startActivity(allIt)
            }

            5 -> {
                val statisticsFile = Files.exportFile(Files.getStatisticsFile())
                if (statisticsFile != null) {
                    ToastUtil.makeText(this, "文件已导出到: " + statisticsFile.path, Toast.LENGTH_SHORT).show()
                }
            }

            6 -> if (Files.copyTo(Files.getExportedStatisticsFile(), Files.getStatisticsFile())) {
                tvStatistics.text = Statistics.getText()
                ToastUtil.makeText(this, "导入成功！", Toast.LENGTH_SHORT).show()
            }

            7 -> {
                var captureData = "file://"
                captureData += Files.getCaptureLogFile().absolutePath
                val captureIt = Intent(this, HtmlViewerActivity::class.java)
                captureIt.putExtra("nextLine", false)
                captureIt.putExtra("canClear", true)
                captureIt.data = captureData.toUri()
                startActivity(captureIt)
            }

            8 ->                 // 扩展功能
                startActivity(Intent(this, ExtendActivity::class.java))

            9 -> selectSettingUid()
            10 -> AlertDialog.Builder(this)
                .setTitle("⚠️ 警告")
                .setMessage("🤔 确认清除所有模块配置？")
                .setPositiveButton(R.string.ok) { _: DialogInterface?, _: Int ->
                    if (Files.delFile(Files.CONFIG_DIR)) {
                        Toast.makeText(this, "🙂 清空配置成功", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "😭 清空配置失败", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton(R.string.cancel) { dialog: DialogInterface, _: Int -> dialog.dismiss() }
                .create()
                .show()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun selectSettingUid() {
        val latch = CountDownLatch(1)
        val dialog = StringDialog.showSelectionDialog(
            this,
            "📌 请选择配置",
            userNameArray,
            { dialog1: DialogInterface, which: Int ->
                goSettingActivity(which)
                dialog1.dismiss()
                latch.countDown()
            },
            "返回",
            { dialog1: DialogInterface ->
                dialog1.dismiss()
                latch.countDown()
            })

        val length = userNameArray.size
        if (length in 1..2) {
            // 定义超时时间（单位：毫秒）
            val timeoutMillis: Long = 800
            Thread {
                try {
                    if (!latch.await(timeoutMillis, TimeUnit.MILLISECONDS)) {
                        runOnUiThread {
                            if (dialog.isShowing) {
                                goSettingActivity(length - 1)
                                dialog.dismiss()
                            }
                        }
                    }
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                }
            }.start()
        }
    }

    private fun showSelectionDialog(
        title: String?, options: Array<String>,
        onItemSelected: Consumer<Int>,
        negativeButtonText: String?,
        onNegativeButtonClick: Runnable,
        showDefaultOption: Boolean
    ) {
        val latch = CountDownLatch(1)
        val dialog = StringDialog.showSelectionDialog(
            this,
            title,
            options,
            { dialog1: DialogInterface, which: Int ->
                onItemSelected.accept(which)
                dialog1.dismiss()
                latch.countDown()
            },
            negativeButtonText,
            { dialog1: DialogInterface ->
                onNegativeButtonClick.run()
                dialog1.dismiss()
                latch.countDown()
            })

        val length = options.size
        if (showDefaultOption && length > 0 && length < 3) {
            // 定义超时时间（单位：毫秒）
            val timeoutMillis: Long = 800
            Thread {
                try {
                    if (!latch.await(timeoutMillis, TimeUnit.MILLISECONDS)) {
                        runOnUiThread {
                            if (dialog.isShowing) {
                                onItemSelected.accept(length - 1)
                                dialog.dismiss()
                            }
                        }
                    }
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                }
            }.start()
        }
    }


    private fun goFriendWatch(index: Int) {
        val userEntity = userEntityArray[index]
        if (userEntity != null) {
            ListDialog.show(
                this, getString(R.string.friend_watch), FriendWatch.getList(userEntity.userId), SelectModelFieldFunc.newMapInstance(), false,
                ListDialog.ListType.SHOW
            )
        } else {
            ToastUtil.makeText(this, "😡 别他妈选默认！！！！！！！！", Toast.LENGTH_LONG).show()
        }
    }

    private fun goSettingActivity(index: Int) {
        if (Detector.loadLibrary("checker")) {
            val userEntity = userEntityArray[index]
            val targetActivity = UIConfig.INSTANCE.targetActivityClass
            val intent = Intent(this, targetActivity)
            if (userEntity != null) {
                intent.putExtra("userId", userEntity.userId)
                intent.putExtra("userName", userEntity.showName)
            } else {
                intent.putExtra("userName", userNameArray[index])
            }
            startActivity(intent)
        } else {
            Detector.tips(this, "缺少必要依赖！")
        }
    }

    fun updateSubTitle(runType: String) {
        Log.runtime("updateSubTitle$runType")
        baseTitle = ViewAppInfo.appTitle + "[" + runType + "]"
        when (runType) {
            RunType.DISABLE.nickName -> setBaseTitleTextColor(ContextCompat.getColor(this, R.color.not_active_text))
            RunType.ACTIVE.nickName -> setBaseTitleTextColor(ContextCompat.getColor(this, R.color.active_text))
            RunType.LOADED.nickName -> setBaseTitleTextColor(ContextCompat.getColor(this, R.color.textColorPrimary))
        }
    }
}
