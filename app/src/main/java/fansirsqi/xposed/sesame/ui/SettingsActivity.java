package fansirsqi.xposed.sesame.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;

import fansirsqi.xposed.sesame.R;
import fansirsqi.xposed.sesame.data.Config;
import fansirsqi.xposed.sesame.data.UIConfig;
import fansirsqi.xposed.sesame.entity.AlipayUser;
import fansirsqi.xposed.sesame.model.Model;
import fansirsqi.xposed.sesame.model.ModelConfig;
import fansirsqi.xposed.sesame.model.ModelField;
import fansirsqi.xposed.sesame.model.ModelFields;
import fansirsqi.xposed.sesame.model.SelectModelFieldFunc;
import fansirsqi.xposed.sesame.task.ModelTask;
import fansirsqi.xposed.sesame.util.Files;
import fansirsqi.xposed.sesame.util.LanguageUtil;
import fansirsqi.xposed.sesame.util.Log;
import fansirsqi.xposed.sesame.util.Maps.BeachMap;
import fansirsqi.xposed.sesame.util.Maps.CooperateMap;
import fansirsqi.xposed.sesame.util.Maps.IdMapManager;
import fansirsqi.xposed.sesame.util.Maps.ReserveaMap;
import fansirsqi.xposed.sesame.util.Maps.UserMap;
import fansirsqi.xposed.sesame.util.StringUtil;
import fansirsqi.xposed.sesame.util.ToastUtil;

public class SettingsActivity extends BaseActivity {

    private static final Integer EXPORT_REQUEST_CODE = 1; // 导出配置请求码
    private static final Integer IMPORT_REQUEST_CODE = 2; // 导入配置请求码

    private Context context; // 上下文对象
    private Boolean isDraw = false; // 标记是否已调整 Tab 的宽度
    private TabHost tabHost; // 用于显示多个选项卡的控件
    private ScrollView svTabs; // 滚动视图，用于容纳 Tab 选项卡内容
    private String userId; // 用户 ID
    private String userName; // 用户名

    @Override
    public String getBaseSubtitle() {
        return getString(R.string.settings); // 返回界面的副标题
    }

    //    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 初始化用户信息
        this.userId = null;
        this.userName = null;
        Intent intent = getIntent();
        if (intent != null) {
            this.userId = intent.getStringExtra("userId"); // 从 Intent 中获取用户 ID
            this.userName = intent.getStringExtra("userName"); // 从 Intent 中获取用户名
        }

        // 初始化各种配置数据
        Model.initAllModel();
        UserMap.setCurrentUserId(this.userId);
        UserMap.load(this.userId);
        CooperateMap.getInstance(CooperateMap.class).load(this.userId);
        IdMapManager.getInstance(ReserveaMap.class).load();
        IdMapManager.getInstance(BeachMap.class).load();
        Config.load(this.userId);

        // 设置语言和布局
        LanguageUtil.setLocale(this);
        setContentView(R.layout.activity_settings);

        // 如果用户名不为空，将其显示在副标题中
        if (this.userName != null) {
            setBaseSubtitle(getString(R.string.settings) + ": " + this.userName);
        }
        setBaseSubtitleTextColor(ContextCompat.getColor(this,R.color.textColorPrimary));
        context = this;
        // 初始化 TabHost
        tabHost = findViewById(R.id.tab_settings);
        tabHost.setup();
        svTabs = findViewById(R.id.sv_tabs);

        // 动态生成选项卡并填充内容
        Map<String, ModelConfig> modelConfigMap = ModelTask.getModelConfigMap();
        for (Map.Entry<String, ModelConfig> configEntry : modelConfigMap.entrySet()) {
            String modelCode = configEntry.getKey();
            ModelConfig modelConfig = configEntry.getValue();
            ModelFields modelFields = modelConfig.getFields();
            tabHost.addTab(tabHost.newTabSpec(modelCode)
                    .setIndicator(modelConfig.getName()) // 设置选项卡名称
                    .setContent(new TabHost.TabContentFactory() {
                        @Override
                        public View createTabContent(String tag) {
                            // 创建选项卡的内容视图
                            LinearLayout linearLayout = new LinearLayout(context);
                            linearLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                            linearLayout.setGravity(Gravity.CENTER_HORIZONTAL);
                            linearLayout.setOrientation(LinearLayout.VERTICAL);
                            // 遍历字段并动态生成对应的视图
                            for (ModelField<?> modelField : modelFields.values()) {
                                View view = modelField.getView(context);
                                if (view != null) {
                                    linearLayout.addView(view);
                                }
                            }
                            return linearLayout;
                        }
                    })
            );
        }
        tabHost.setCurrentTab(0); // 设置默认选项卡
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        save(); // 返回时保存配置
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (!isDraw && hasFocus) {
            // 动态调整 Tab 的宽度
            int width = svTabs.getWidth();
            TabWidget tabWidget = tabHost.getTabWidget();
            int childCount = tabWidget.getChildCount();
            for (int i = 0; i < childCount; i++) {
                tabWidget.getChildAt(i).getLayoutParams().width = width;
            }
            tabWidget.requestLayout();
            isDraw = true; // 标记已调整宽度
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // 创建菜单选项
        menu.add(0, 1, 1, "导出配置");
        menu.add(0, 2, 2, "导入配置");
        menu.add(0, 3, 3, "删除配置");
        menu.add(0, 4, 4, "单向好友");
        menu.add(0, 5, 5, "切换至新UI"); // 允许切换到新 UI
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // 处理菜单项点击事件
        switch (item.getItemId()) {
            case 1: // 导出配置
                Intent exportIntent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                exportIntent.addCategory(Intent.CATEGORY_OPENABLE);
                exportIntent.setType("*/*");
                exportIntent.putExtra(Intent.EXTRA_TITLE, "[" + this.userName + "]-config_v2.json");
                startActivityForResult(exportIntent, EXPORT_REQUEST_CODE);
                break;
            case 2: // 导入配置
                Intent importIntent = new Intent(Intent.ACTION_GET_CONTENT);
                importIntent.addCategory(Intent.CATEGORY_OPENABLE);
                importIntent.setType("*/*");
                importIntent.putExtra(Intent.EXTRA_TITLE, "config_v2.json");
                startActivityForResult(importIntent, IMPORT_REQUEST_CODE);
                break;
            case 3: // 删除配置
                new AlertDialog.Builder(context)
                        .setTitle("警告")
                        .setMessage("确认删除该配置？")
                        .setPositiveButton(R.string.ok, (dialog, id) -> {
                            java.io.File userConfigDirectoryFile;
                            if (StringUtil.isEmpty(this.userId)) {
                                userConfigDirectoryFile = Files.getDefaultConfigV2File();
                            } else {
                                userConfigDirectoryFile = Files.getUserConfigDir(this.userId);
                            }
                            if (Files.delFile(userConfigDirectoryFile)) {
                                ToastUtil.makeText(this, "配置删除成功", Toast.LENGTH_SHORT).show();
                            } else {
                                ToastUtil.makeText(this, "配置删除失败", Toast.LENGTH_SHORT).show();
                            }
                            finish();
                        })
                        .setNegativeButton(R.string.cancel, (dialog, id) -> dialog.dismiss())
                        .create()
                        .show();
                break;
            case 4: // 查看单向好友列表
                ListDialog.show(this, "单向好友列表", AlipayUser.getList(user -> user.getFriendStatus() != 1), SelectModelFieldFunc.newMapInstance(), false, ListDialog.ListType.SHOW);
                break;
            case 5: // 切换到新 UI
                UIConfig.INSTANCE.setNewUI(true);
                if (UIConfig.save()) {
                    Intent intent = new Intent(this, NewSettingsActivity.class);
                    intent.putExtra("userId", this.userId);
                    intent.putExtra("userName", this.userName);
                    finish();
                    startActivity(intent);
                } else {
                    ToastUtil.makeText(this, "切换失败", Toast.LENGTH_SHORT).show();
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) {
            return;
        }
        if (requestCode == EXPORT_REQUEST_CODE) {
            // 处理导出逻辑
            Uri uri = data.getData();
            if (uri != null) {
                try {
                    java.io.File configV2File;
                    if (StringUtil.isEmpty(this.userId)) {
                        configV2File = Files.getDefaultConfigV2File();
                    } else {
                        configV2File = Files.getConfigV2File(this.userId);
                    }
                    FileInputStream inputStream = new FileInputStream(configV2File);
                    if (Files.streamTo(inputStream, getContentResolver().openOutputStream(data.getData()))) {
                        ToastUtil.makeText(this, "导出成功！", Toast.LENGTH_SHORT).show();
                    } else {
                        ToastUtil.makeText(this, "导出失败！", Toast.LENGTH_SHORT).show();
                    }
                } catch (IOException e) {
                    Log.printStackTrace(e);
                    ToastUtil.makeText(this, "导出失败！", Toast.LENGTH_SHORT).show();
                }
            }
        } else if (requestCode == IMPORT_REQUEST_CODE) {
            // 处理导入逻辑
            Uri uri = data.getData();
            if (uri != null) {
                try {
                    java.io.File configV2File;
                    if (StringUtil.isEmpty(this.userId)) {
                        configV2File = Files.getDefaultConfigV2File();
                    } else {
                        configV2File = Files.getConfigV2File(this.userId);
                    }
                    FileOutputStream outputStream = new FileOutputStream(configV2File);
                    if (Files.streamTo(Objects.requireNonNull(getContentResolver().openInputStream(data.getData())), outputStream)) {
                        ToastUtil.makeText(this, "导入成功！", Toast.LENGTH_SHORT).show();
                        if (!StringUtil.isEmpty(this.userId)) {
                            try {
                                Intent intent = new Intent("com.eg.android.AlipayGphone.sesame.restart");
                                intent.putExtra("userId", this.userId);
                                sendBroadcast(intent);
                            } catch (Throwable th) {
                                Log.printStackTrace(th);
                            }
                        }
                        Intent intent = getIntent();
                        finish();
                        startActivity(intent);
                    } else {
                        ToastUtil.makeText(this, "导入失败！", Toast.LENGTH_SHORT).show();
                    }
                } catch (IOException e) {
                    Log.printStackTrace(e);
                    ToastUtil.makeText(this, "导入失败！", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void save() {
        // 保存当前用户的配置信息
        try {
            if (Config.isModify(this.userId) && Config.save(this.userId, false)) {
                ToastUtil.showToastWithDelay(this, "保存成功！", 100);
                if (!StringUtil.isEmpty(this.userId)) {
                    Intent intent = new Intent("com.eg.android.AlipayGphone.sesame.restart");
                    intent.putExtra("userId", this.userId);
                    sendBroadcast(intent);
                }
            }
            if (!StringUtil.isEmpty(this.userId)) {
                UserMap.save(this.userId);
                CooperateMap.getInstance(CooperateMap.class).save(this.userId);
            }
        } catch (Throwable th) {
            Log.printStackTrace(th);
        }
    }
}
