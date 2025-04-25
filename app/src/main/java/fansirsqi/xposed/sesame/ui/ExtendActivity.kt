package fansirsqi.xposed.sesame.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import fansirsqi.xposed.sesame.R
import fansirsqi.xposed.sesame.util.Log
import fansirsqi.xposed.sesame.util.ToastUtil

/**
 * 扩展功能页面
 */
class ExtendActivity : BaseActivity() {
    private var debugTips: String? = null

    /**
     * 初始化Activity
     *
     * @param savedInstanceState 保存的实例状态
     */
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_extend) // 设置布局文件
        debugTips = getString(R.string.debug_tips)
        // 初始化按钮并设置点击事件
        initButtonsAndSetListeners()
    }

    /**
     * 初始化按钮并设置监听器
     */
    private fun initButtonsAndSetListeners() {
        // 定义按钮变量并绑定按钮到对应的View
        val btnGetTreeItems = findViewById<Button>(R.id.get_tree_items)
        val btnGetNewTreeItems = findViewById<Button>(R.id.get_newTree_items)
        //完善下面这两个按钮对应功能
        val btnQueryAreaTrees = findViewById<Button>(R.id.query_area_trees)
        val btnGetUnlockTreeItems = findViewById<Button>(R.id.get_unlock_treeItems)
        // 设置Activity标题
        baseTitle = getString(R.string.extended_func)
        // 为每个按钮设置点击事件
        btnGetTreeItems.setOnClickListener(TreeItemsOnClickListener())
        btnGetNewTreeItems.setOnClickListener(NewTreeItemsOnClickListener())
        btnQueryAreaTrees.setOnClickListener(AreaTreesOnClickListener())
        btnGetUnlockTreeItems.setOnClickListener(UnlockTreeItemsOnClickListener())
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

    /**
     * 获取树项目按钮的点击监听器
     */
    private inner class TreeItemsOnClickListener : View.OnClickListener {
        override fun onClick(v: View) {
            sendItemsBroadcast("getTreeItems")
            ToastUtil.makeText(this@ExtendActivity, debugTips, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 获取新树项目按钮的点击监听器
     */
    private inner class NewTreeItemsOnClickListener : View.OnClickListener {
        override fun onClick(v: View) {
            sendItemsBroadcast("getNewTreeItems")
            ToastUtil.makeText(this@ExtendActivity, debugTips, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 查询未解锁🔓地区
     */
    private inner class AreaTreesOnClickListener : View.OnClickListener {
        override fun onClick(v: View) {
            sendItemsBroadcast("queryAreaTrees")
            ToastUtil.makeText(this@ExtendActivity, debugTips, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 查询未解锁🔓🌳木项目
     */
    private inner class UnlockTreeItemsOnClickListener : View.OnClickListener {
        override fun onClick(v: View) {
            sendItemsBroadcast("getUnlockTreeItems")
            ToastUtil.makeText(this@ExtendActivity, debugTips, Toast.LENGTH_SHORT).show()
        }
    }
}
