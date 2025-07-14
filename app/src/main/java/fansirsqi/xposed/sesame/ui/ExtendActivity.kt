package fansirsqi.xposed.sesame.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import fansirsqi.xposed.sesame.R
import fansirsqi.xposed.sesame.data.DataCache
import fansirsqi.xposed.sesame.data.ViewAppInfo
import fansirsqi.xposed.sesame.entity.ExtendFunctionItem
import fansirsqi.xposed.sesame.ui.widget.ExtendFunctionAdapter
import fansirsqi.xposed.sesame.util.FansirsqiUtil
import fansirsqi.xposed.sesame.util.Log
import fansirsqi.xposed.sesame.util.ToastUtil

/**
 * 扩展功能页面
 */
class ExtendActivity : BaseActivity() {
    private var debugTips: String? = null
    private lateinit var recyclerView: RecyclerView
    private lateinit var extendFunctionAdapter: ExtendFunctionAdapter
    private val extendFunctions = mutableListOf<ExtendFunctionItem>()

    /**
     * 初始化Activity
     *
     * @param savedInstanceState 保存的实例状态
     */
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_extend) // 设置布局文件
        debugTips = getString(R.string.debug_tips)
        baseTitle = getString(R.string.extended_func)

        setupRecyclerView()
        populateExtendFunctions()
    }

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.recyclerView_extend_functions)
        recyclerView.layoutManager = LinearLayoutManager(this)
        extendFunctionAdapter = ExtendFunctionAdapter(extendFunctions)
        recyclerView.adapter = extendFunctionAdapter
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun populateExtendFunctions() {
        extendFunctions.add(
            ExtendFunctionItem(getString(R.string.query_the_remaining_amount_of_saplings)) {
                sendItemsBroadcast("getTreeItems")
                ToastUtil.makeText(this@ExtendActivity, debugTips, Toast.LENGTH_SHORT).show()
            }
        )
        extendFunctions.add(
            ExtendFunctionItem(getString(R.string.search_for_new_items_on_saplings)) {
                sendItemsBroadcast("getNewTreeItems")
                ToastUtil.makeText(this@ExtendActivity, debugTips, Toast.LENGTH_SHORT).show()
            }
        )
        extendFunctions.add(
            ExtendFunctionItem(getString(R.string.search_for_unlocked_regions)) {
                sendItemsBroadcast("queryAreaTrees")
                ToastUtil.makeText(this@ExtendActivity, debugTips, Toast.LENGTH_SHORT).show()
            }
        )
        extendFunctions.add(
            ExtendFunctionItem(getString(R.string.search_for_unlocked_items)) {
                sendItemsBroadcast("getUnlockTreeItems")
                ToastUtil.makeText(this@ExtendActivity, debugTips, Toast.LENGTH_SHORT).show()
            }
        )
        extendFunctions.add(
            ExtendFunctionItem(getString(R.string.clear_photo)) {
                AlertDialog.Builder(this)
                    .setTitle(R.string.clear_photo)
                    .setMessage("确认清空${DataCache.guangPanPhotoCount}组光盘行动图片？")
                    .setPositiveButton(R.string.ok) { _, _ ->
                        if (DataCache.clearGuangPanPhoto()) {
                            ToastUtil.showToast(this, "光盘行动图片清空成功")
                        } else {
                            ToastUtil.showToast(this, "光盘行动图片清空失败")
                        }
                    }
                    .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
                    .show()
            }
        )
        //调试功能往里加
        if(ViewAppInfo.isApkInDebug){
            extendFunctions.add(
                ExtendFunctionItem("写入光盘") {
                    val photos = HashMap<String, String>()
                    AlertDialog.Builder(this)
                        .setTitle("Test")
                        .setMessage("xxxx")
                        .setPositiveButton(R.string.ok) { _, _ ->
                            val randomStr = FansirsqiUtil.getRandomString(10)
                            photos["before"] = "before$randomStr"
                            photos["after"] = "after$randomStr"
                            if (DataCache.saveGuangPanPhoto(photos)) {
                                ToastUtil.showToast(this, "写入成功$photos")
                            } else {
                                ToastUtil.showToast(this, "写入失败$photos")
                            }
                        }
                        .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
                        .show()
                }
            )

            extendFunctions.add(
                ExtendFunctionItem("展示随机光盘") {
                    AlertDialog.Builder(this)
                        .setTitle("看看效果")
                        .setMessage("xxxx")
                        .setPositiveButton(R.string.ok) { _, _ ->
                            ToastUtil.showToast(this, "${DataCache.randomGuangPanPhoto}")
                        }
                        .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
                        .show()
                }
            )
        }
        extendFunctionAdapter.notifyDataSetChanged()
    }

    /**
     * 发送广播事件
     *
     * @param type 广播类型
     */
    private fun sendItemsBroadcast(type: String) {
        val intent = Intent("com.eg.android.AlipayGphone.sesame.rpctest")
        intent.putExtra("method", "")
        intent.putExtra("data", "")
        intent.putExtra("type", type)
        sendBroadcast(intent) // 发送广播
        Log.debug("扩展工具主动调用广播查询📢：$type")
    }
}
