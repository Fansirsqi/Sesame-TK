package fansirsqi.xposed.sesame.ui;

import static fansirsqi.xposed.sesame.data.UIConfig.UI_OPTION_WEB;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import fansirsqi.xposed.sesame.R;
import fansirsqi.xposed.sesame.data.Config;
import fansirsqi.xposed.sesame.data.UIConfig;
import fansirsqi.xposed.sesame.entity.AlipayUser;
import fansirsqi.xposed.sesame.model.Model;
import fansirsqi.xposed.sesame.model.ModelConfig;
import fansirsqi.xposed.sesame.model.SelectModelFieldFunc;
import fansirsqi.xposed.sesame.task.ModelTask;
import fansirsqi.xposed.sesame.ui.widget.ContentPagerAdapter;
import fansirsqi.xposed.sesame.ui.widget.TabAdapter;
import fansirsqi.xposed.sesame.util.Files;
import fansirsqi.xposed.sesame.util.LanguageUtil;
import fansirsqi.xposed.sesame.util.Log;
import fansirsqi.xposed.sesame.util.Maps.BeachMap;
import fansirsqi.xposed.sesame.util.Maps.CooperateMap;
import fansirsqi.xposed.sesame.util.Maps.IdMapManager;
import fansirsqi.xposed.sesame.util.Maps.ReserveaMap;
import fansirsqi.xposed.sesame.util.Maps.UserMap;
import fansirsqi.xposed.sesame.util.PortUtil;
import fansirsqi.xposed.sesame.util.StringUtil;
import fansirsqi.xposed.sesame.util.ToastUtil;

public class SettingActivity extends BaseActivity {
    private static final String TAG = SettingActivity.class.getSimpleName();
    private ActivityResultLauncher<Intent> exportLauncher;
    private ActivityResultLauncher<Intent> importLauncher;
    private String userId; // 用户 ID
    private String userName; // 用户名

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 初始化用户信息
        this.userId = null;
        this.userName = null;
        Intent intent = getIntent();
        if (intent != null) {
            this.userId = intent.getStringExtra("userId");
            this.userName = intent.getStringExtra("userName");
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
        // 处理返回键
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                save();
                finish();
            }
        });
        // 初始化导出逻辑
        exportLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        PortUtil.handleExport(this, result.getData().getData(), userId);
                    }
                }
        );
        // 初始化导入逻辑
        importLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        PortUtil.handleImport(this, result.getData().getData(), userId);
                    }
                }
        );
        // 设置副标题
        if (this.userName != null) {
            setBaseSubtitle(getString(R.string.settings) + ": " + this.userName);
        }
        setBaseSubtitleTextColor(ContextCompat.getColor(this, R.color.textColorPrimary));
        initializeTabs();
    }

    private void initializeTabs() {
        try {
            RecyclerView recyclerTabList = findViewById(R.id.recycler_tab_list);
            recyclerTabList.setLayoutManager(new LinearLayoutManager(this));
            Map<String, ModelConfig> modelConfigMap = ModelTask.getModelConfigMap();
            List<String> tabTitles = new ArrayList<>();
            for (ModelConfig config : modelConfigMap.values()) {
                tabTitles.add(config.getName());
            }
            TabAdapter tabAdapter = new TabAdapter(this, tabTitles, position -> {
                ViewPager2 viewPager = findViewById(R.id.view_pager_content);
                viewPager.setCurrentItem(position, true);
            });
            recyclerTabList.setAdapter(tabAdapter);
            ViewPager2 viewPager = findViewById(R.id.view_pager_content);
            ContentPagerAdapter contentAdapter = new ContentPagerAdapter(getSupportFragmentManager(), getLifecycle(), modelConfigMap);
            viewPager.setAdapter(contentAdapter);
            viewPager.setUserInputEnabled(false);// 禁止用户手动滑动
            viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageSelected(int position) {
                    recyclerTabList.smoothScrollToPosition(position);
                    tabAdapter.setSelectedPosition(position);
                }
            });
        } catch (Throwable t) {
            Log.error(TAG, "初始化Tabs失败: " + t.getMessage());
            Log.printStackTrace(TAG, t);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // 创建菜单选项
        menu.add(0, 1, 1, "导出配置");
        menu.add(0, 2, 2, "导入配置");
        menu.add(0, 3, 3, "删除配置");
        menu.add(0, 4, 4, "单向好友");
        menu.add(0, 5, 5, "切换WEBUI");
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
                exportLauncher.launch(exportIntent);
                break;
            case 2: // 导入配置
                Intent importIntent = new Intent(Intent.ACTION_GET_CONTENT);
                importIntent.addCategory(Intent.CATEGORY_OPENABLE);
                importIntent.setType("*/*");
                importIntent.putExtra(Intent.EXTRA_TITLE, "config_v2.json");
                importLauncher.launch(importIntent);
                break;
            case 3: // 删除配置
                new AlertDialog.Builder(this)
                        .setTitle("警告")
                        .setMessage("确认删除该配置？")
                        .setPositiveButton(R.string.ok, (dialog, id) -> {
                            File userConfigDirectoryFile;
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
                UIConfig.INSTANCE.setUiOption(UI_OPTION_WEB);
                if (UIConfig.save()) {
                    Intent intent = new Intent(this, UIConfig.INSTANCE.getTargetActivityClass());
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

    private void save() {
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
