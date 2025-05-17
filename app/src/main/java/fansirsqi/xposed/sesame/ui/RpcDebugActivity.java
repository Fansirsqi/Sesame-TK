package fansirsqi.xposed.sesame.ui;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import fansirsqi.xposed.sesame.R;
import fansirsqi.xposed.sesame.hook.ApplicationHook;
import fansirsqi.xposed.sesame.util.Files;
import fansirsqi.xposed.sesame.util.NtpTimeUtils;
import fansirsqi.xposed.sesame.util.RequestStorage;
import fansirsqi.xposed.sesame.util.ToastUtil;
import fansirsqi.xposed.sesame.util.Log;
/**
 * BroadcastReceiver 必须在广播发出 之前完成注册。
 * 如果你在 onCreate() 中注册，但在注册前就发出了广播，自然收不到。
 * ✅ LocalBroadcastManager 在注册后可以支持粘滞广播行为（类似 sendStickyBroadcast），即使广播早于 Receiver 注册也可以收到。
 */
public class RpcDebugActivity extends AppCompatActivity {
    private EditText txtData, txtMethod, txtTitle;
    private TextView txtResult;
    private Button btnPost, btnSaveRequest, btnToggleZoom;
    private RecyclerView requestList;
    private RequestAdapter adapter;
    private List<RequestItem> requests = new ArrayList<>();
    private boolean isZoomed = false;

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rpc_debug);

        // 初始化控件
        txtData = findViewById(R.id.data);
        txtMethod = findViewById(R.id.method);
        txtTitle = findViewById(R.id.title);
        txtResult = findViewById(R.id.result);
        btnPost = findViewById(R.id.post);
        btnSaveRequest = findViewById(R.id.save_request);
        btnToggleZoom = findViewById(R.id.toggle_zoom);
        requestList = findViewById(R.id.request_list);


        // 加载保存的请求列表
        requests = RequestStorage.loadRequests(this);

        // 设置 RecyclerView
        adapter = new RequestAdapter(requests, this::onRequestItemClick);
        requestList.setLayoutManager(new LinearLayoutManager(this));
        requestList.setAdapter(adapter);

        //NtpTimeUtils.syncTimeAsync(); // 只同步一次
        // 保存请求按钮
        btnSaveRequest.setOnClickListener(v -> {
            String title = txtTitle.getText().toString();
            String method = txtMethod.getText().toString();
            String data = txtData.getText().toString();

            if (!title.isEmpty() && !method.isEmpty()) {
                RequestItem item = new RequestItem(title, method, data);
                requests.add(item);
                adapter.notifyItemInserted(requests.size() - 1);
            } else {
                Toast.makeText(this, "请输入完整信息", Toast.LENGTH_SHORT).show();
            }
        });

        // 发送按钮
        btnPost.setOnClickListener(v -> sendRequest(txtMethod.getText().toString(), txtData.getText().toString()));

        // 测试按钮
        Button testButton = findViewById(R.id.test1);
        testButton.setOnClickListener(v -> testRequest(txtMethod.getText().toString(), txtData.getText().toString()));

        // 查看日志按钮
        //查抓包的日志
        Button viewLogButton = findViewById(R.id.view_log);
        viewLogButton.setOnClickListener(v -> viewLog());

        //查看Debug的日志
        Button viewLogButton2 = findViewById(R.id.view_log2);
        viewLogButton2.setOnClickListener(v -> viewLog2());

        // 放大/关闭放大按钮
        btnToggleZoom.setOnClickListener(v -> toggleZoom());

        // 设置广播接收器的 TextView
        RpcResponseReceiver.setTextView(txtResult);



//        // 注册广播接收器
//        // 在 onCreate() 方法内
//        IntentFilter intentFilter = new IntentFilter("com.eg.android.AlipayGphone.sesame.rpcresponse");
//        BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
//            @Override
//            public void onReceive(Context context, Intent intent) {
//                Log.other("RpcDebugActivity", "✅ 收到广播！");
//                String result = intent.getStringExtra("result");
//                if (result != null) {
//                    txtResult.setText(result);
//                    Log.other("广播内容为：" + result);
//                } else {
//                    txtResult.setText("收到广播但无数据");
//                    Log.other("RpcDebugActivity", "广播内容为空");
//                }
//            }
//        };
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            registerReceiver(broadcastReceiver, intentFilter, Context.RECEIVER_NOT_EXPORTED);
//        } else {
//            registerReceiver(broadcastReceiver, intentFilter);
//        }
        IntentFilter intentFilter = new IntentFilter("com.eg.android.AlipayGphone.sesame.rpcresponse");
        BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                //Log.debug("RpcDebugActivity", "✅ 收到广播！");
                String action = intent.getAction();
                Log.runtime("post--查看广播:" + action + " intent:" + intent);
                txtResult.setText(intent.getStringExtra("result"));
            }
        };
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(broadcastReceiver, intentFilter, Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(broadcastReceiver, intentFilter);
        }
    }
    public static class RpcResponseReceiver extends BroadcastReceiver {
        private static WeakReference<TextView> txtResultRef;

        public static void setTextView(TextView textView) {
            if (textView != null) {
                txtResultRef = new WeakReference<>(textView);
            } else if (txtResultRef != null) {
                txtResultRef.clear();
            }
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (!"com.eg.android.AlipayGphone.sesame.rpcresponse".equals(intent.getAction())) {
                return;
            }

            String result = intent.getStringExtra("result");
            Log.debug("RpcDebugActivity", "✅ 收到广播！");

            TextView textView = txtResultRef != null ? txtResultRef.get() : null;
            if (textView != null && textView.getContext() instanceof RpcDebugActivity) {
                ((RpcDebugActivity) textView.getContext()).runOnUiThread(() -> {
                    if (textView != null && !((RpcDebugActivity) textView.getContext()).isFinishing()) {
                        textView.setText(result != null ? result : "收到广播但无数据");
                    }
                });
            }
        }
    }



    private void sendRequest(String method, String data) {
        try {
            Intent intent = new Intent("com.eg.android.AlipayGphone.sesame.rpctest");
            intent.putExtra("method", method);
            intent.putExtra("data", data);
            intent.putExtra("type", "Rpc");
            sendBroadcast(intent);

            Log.debug("RpcDebugActivity", "发送请求: 方法=" + method + ", 参数=" + data);
            ToastUtil.makeText(this, "发送--请求发送成功", Toast.LENGTH_SHORT).show();
        }  catch (Exception e) {
            txtResult.setText("发送请求错误");
            Log.other( "发送请求错误:"+ e);
        }
    }

    private void testRequest(String method, String data) {
        try {
            Intent intent = new Intent("com.eg.android.AlipayGphone.sesame.rpctest");
            intent.putExtra("method", method);
            intent.putExtra("data", data);
            intent.putExtra("type", "Rpc");
            sendBroadcast(intent);

            Log.debug("RpcDebugActivity", "测试请求: 方法=" + method + ", 参数=" + data);
            ToastUtil.makeText(this, "测试--请求发送成功", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            txtResult.setText("发送测试请求错误");
            Log.error("发送测试请求错误:"+e);
        }
    }

    private void viewLog() {
        Intent intent = new Intent(this, HtmlViewerActivity.class);
        Uri uri = Uri.parse("file://" + Files.getCaptureLogFile().getAbsolutePath());
        intent.setData(uri);
        intent.putExtra("showTest", false);
        startActivity(intent);
    }
    private void viewLog2() {
        Intent intent = new Intent(this, HtmlViewerActivity.class);
        Uri uri = Uri.parse("file://" + Files.getDebugLogFile().getAbsolutePath());
        intent.setData(uri);
        intent.putExtra("showTest", false);
        startActivity(intent);
    }

    private void toggleZoom() {
        if (isZoomed) {
            txtResult.setTextSize(16); // 恢复原始字体大小
            txtResult.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT)); // 恢复高度
            btnToggleZoom.setText("放大显示");
        } else {
            txtResult.setTextSize(24); // 放大字体大小
            txtResult.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT)); // 扩展高度
            btnToggleZoom.setText("关闭放大");
        }
        isZoomed = !isZoomed;
    }

    private void clearInputs() {
        txtMethod.setText("");
        txtData.setText("");
        txtTitle.setText("");
    }

    private void onRequestItemClick(RequestItem item, String action) {
        switch (action) {
            case "send":
                sendRequest(item.getMethod(), item.getData());
                break;
            case "test":
                testRequest(item.getMethod(), item.getData());
                break;
            case "delete":
                deleteRequest(item); // 删除请求
                break;
        }
    }
    // 删除请求
    private void deleteRequest(RequestItem item) {
        int index = requests.indexOf(item); // 找到要删除的请求索引
        if (index != -1) {
            requests.remove(index); // 从列表中移除
            adapter.notifyItemRemoved(index); // 通知适配器更新
        }
    }
    @Override
    protected void onPause() {
        super.onPause();
        RequestStorage.saveRequests(this, requests); // 👈 唯一的保存入口
    }
}