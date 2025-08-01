package fansirsqi.xposed.sesame.ui;

import static fansirsqi.xposed.sesame.data.UIConfig.UI_OPTION_NEW;


import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.fasterxml.jackson.core.type.TypeReference;

import org.json.JSONException;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import fansirsqi.xposed.sesame.BuildConfig;
import fansirsqi.xposed.sesame.R;
import fansirsqi.xposed.sesame.data.Config;
import fansirsqi.xposed.sesame.data.UIConfig;
import fansirsqi.xposed.sesame.data.ViewAppInfo;
import fansirsqi.xposed.sesame.entity.AlipayUser;
import fansirsqi.xposed.sesame.model.Model;
import fansirsqi.xposed.sesame.model.ModelConfig;
import fansirsqi.xposed.sesame.model.ModelField;
import fansirsqi.xposed.sesame.model.ModelFields;
import fansirsqi.xposed.sesame.model.ModelGroup;
import fansirsqi.xposed.sesame.model.SelectModelFieldFunc;
import fansirsqi.xposed.sesame.task.ModelTask;
import fansirsqi.xposed.sesame.ui.dto.ModelDto;
import fansirsqi.xposed.sesame.ui.dto.ModelFieldInfoDto;
import fansirsqi.xposed.sesame.ui.dto.ModelFieldShowDto;
import fansirsqi.xposed.sesame.ui.dto.ModelGroupDto;
import fansirsqi.xposed.sesame.ui.widget.ListDialog;
import fansirsqi.xposed.sesame.util.Files;
import fansirsqi.xposed.sesame.util.JsonUtil;
import fansirsqi.xposed.sesame.util.LanguageUtil;
import fansirsqi.xposed.sesame.util.Log;
import fansirsqi.xposed.sesame.util.maps.BeachMap;
import fansirsqi.xposed.sesame.util.maps.CooperateMap;
import fansirsqi.xposed.sesame.util.maps.IdMapManager;
import fansirsqi.xposed.sesame.util.maps.MemberBenefitsMap;
import fansirsqi.xposed.sesame.util.maps.ParadiseCoinBenefitIdMap;
import fansirsqi.xposed.sesame.util.maps.ReserveaMap;
import fansirsqi.xposed.sesame.util.maps.UserMap;
import fansirsqi.xposed.sesame.util.maps.VitalityRewardsMap;
import fansirsqi.xposed.sesame.util.PortUtil;
import fansirsqi.xposed.sesame.util.StringUtil;

public class WebSettingsActivity extends BaseActivity {
    private static final String TAG = "WebSettingsActivity";
    private static final Integer EXPORT_REQUEST_CODE = 1;
    private static final Integer IMPORT_REQUEST_CODE = 2;
    private ActivityResultLauncher<Intent> exportLauncher;
    private ActivityResultLauncher<Intent> importLauncher;
    private WebView webView;
    private Context context;
    private String userId = null;
    private String userName = null;
    private final List<ModelDto> tabList = new ArrayList<>();
    private final List<ModelGroupDto> groupList = new ArrayList<>();

    @Override
    public String getBaseSubtitle() {
        return getString(R.string.settings);
    }

    @SuppressLint({"MissingInflatedId", "SetJavaScriptEnabled"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        userId = null;
        userName = null;
        Intent intent = getIntent();
        if (intent != null) {
            userId = intent.getStringExtra("userId");
            userName = intent.getStringExtra("userName");
            intent.getBooleanExtra("debug", BuildConfig.DEBUG);
        }
        Model.initAllModel();
        UserMap.setCurrentUserId(userId);
        UserMap.load(userId);
        CooperateMap.getInstance(CooperateMap.class).load(userId);
        IdMapManager.getInstance(VitalityRewardsMap.class).load(this.userId);
        IdMapManager.getInstance(MemberBenefitsMap.class).load(this.userId);
        IdMapManager.getInstance(ParadiseCoinBenefitIdMap.class).load(this.userId);
        IdMapManager.getInstance(ReserveaMap.class).load();
        IdMapManager.getInstance(BeachMap.class).load();
        Config.load(userId);
        LanguageUtil.setLocale(this);
        setContentView(R.layout.activity_web_settings);
        //处理返回键
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (webView.canGoBack()) {
                    Log.runtime(TAG,"WebSettingsActivity.handleOnBackPressed: go back");
                    webView.goBack();
                } else {
                    Log.runtime(TAG,"WebSettingsActivity.handleOnBackPressed: save");
                    save();
                    finish();
                }
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
        if (userName != null) {
            setBaseSubtitle(getString(R.string.settings) + ": " + userName);
        }
        context = this;
        webView = findViewById(R.id.webView);
        WebSettings settings = webView.getSettings();
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setAllowFileAccess(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setLoadsImagesAutomatically(true);
        settings.setDefaultTextEncodingName(StandardCharsets.UTF_8.name());
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                // 强制在当前 WebView 中加载 url
                Uri requestUrl = request.getUrl();
                String scheme = requestUrl.getScheme();
                assert scheme != null;
                if (
                        scheme.equalsIgnoreCase("http")
                                || scheme.equalsIgnoreCase("https")
                                || scheme.equalsIgnoreCase("ws")
                                || scheme.equalsIgnoreCase("wss")
                ) {
                    view.loadUrl(requestUrl.toString());
                    return true;
                }
                view.stopLoading();
                Toast.makeText(context, "Forbidden Scheme:\"" + scheme + "\"", Toast.LENGTH_SHORT).show();
                return false;
            }
        });
        if (BuildConfig.DEBUG) {
            WebView.setWebContentsDebuggingEnabled(true);
//            webView.loadUrl("http://192.168.31.69:5500/app/src/main/assets/web/index.html");
            webView.loadUrl("file:///android_asset/web/index.html");
        } else {
            webView.loadUrl("file:///android_asset/web/index.html");
        }
        webView.addJavascriptInterface(new WebViewCallback(), "HOOK");

        webView.requestFocus();
        Map<String, ModelConfig> modelConfigMap = ModelTask.getModelConfigMap();
        for (Map.Entry<String, ModelConfig> configEntry : modelConfigMap.entrySet()) {
            ModelConfig modelConfig = configEntry.getValue();
            tabList.add(new ModelDto(configEntry.getKey(), modelConfig.getName(), modelConfig.getIcon(), modelConfig.getGroup().getCode(), null));
        }
        for (ModelGroup modelGroup : ModelGroup.values()) {
            groupList.add(new ModelGroupDto(modelGroup.getCode(), modelGroup.getName(), modelGroup.getIcon()));
        }
    }


    public class WebAppInterface {
        @JavascriptInterface
        public void onBackPressed() {
            runOnUiThread(() -> {
                if (webView.canGoBack()) {
                    webView.goBack();
                } else {
                    Log.runtime(TAG,"WebAppInterface onBackPressed: save");
                    save();
                    WebSettingsActivity.this.finish();
                }
            });
        }

        @JavascriptInterface
        public void onExit() {
            runOnUiThread(WebSettingsActivity.this::finish);
        }
    }

    private class WebViewCallback {
        @JavascriptInterface
        public String getTabs() {
            String result = JsonUtil.formatJson(tabList, false);
            if (BuildConfig.DEBUG) {
                Log.runtime(TAG,"WebSettingsActivity.getTabs: " + result);
            }
            return result;
        }

        @JavascriptInterface
        public String getBuildInfo() {
            return BuildConfig.APPLICATION_ID + ":" + BuildConfig.VERSION_NAME;
        }

        @JavascriptInterface
        public String getGroup() {
            String result = JsonUtil.formatJson(groupList, false);
            if (BuildConfig.DEBUG) {
                Log.runtime(TAG,"WebSettingsActivity.getGroup: " + result);
            }
            return result;
        }

        @JavascriptInterface
        public String getModelByGroup(String groupCode) {
            Collection<ModelConfig> modelConfigCollection = ModelTask.getGroupModelConfig(ModelGroup.getByCode(groupCode)).values();
            List<ModelDto> modelDtoList = new ArrayList<>();
            for (ModelConfig modelConfig : modelConfigCollection) {
                List<ModelFieldShowDto> modelFields = new ArrayList<>();
                for (ModelField<?> modelField : modelConfig.getFields().values()) {
                    modelFields.add(ModelFieldShowDto.toShowDto(modelField));
                }
                modelDtoList.add(new ModelDto(modelConfig.getCode(), modelConfig.getName(), modelConfig.getIcon(), groupCode, modelFields));
            }
            String result = JsonUtil.formatJson(modelDtoList, false);
            if (BuildConfig.DEBUG) {
                Log.runtime(TAG,"WebSettingsActivity.getModelByGroup: " + result);
            }
            return result;
        }

        @JavascriptInterface
        public String setModelByGroup(String groupCode, String modelsValue) {
            List<ModelDto> modelDtoList = JsonUtil.parseObject(modelsValue, new TypeReference<List<ModelDto>>() {
            });
            Map<String, ModelConfig> modelConfigSet = ModelTask.getGroupModelConfig(ModelGroup.getByCode(groupCode));
            for (ModelDto modelDto : modelDtoList) {
                ModelConfig modelConfig = modelConfigSet.get(modelDto.getModelCode());
                if (modelConfig != null) {
                    List<ModelFieldShowDto> modelFields = modelDto.getModelFields();
                    if (modelFields != null) {
                        for (ModelFieldShowDto newModelField : modelFields) {
                            if (newModelField != null) {
                                ModelField<?> modelField = modelConfig.getModelField(newModelField.getCode());
                                if (modelField != null) {
                                    modelField.setConfigValue(newModelField.getConfigValue());
                                }
                            }
                        }
                    }
                }
            }
            return "SUCCESS";
        }

        @JavascriptInterface
        public String getModel(String modelCode) {
            ModelConfig modelConfig = ModelTask.getModelConfigMap().get(modelCode);
            if (modelConfig != null) {
                ModelFields modelFields = modelConfig.getFields();
                List<ModelFieldShowDto> list = new ArrayList<>();
                for (ModelField<?> modelField : modelFields.values()) {
                    list.add(ModelFieldShowDto.toShowDto(modelField));
                }
                String result = JsonUtil.formatJson(list, false);
                if (BuildConfig.DEBUG) {
                    Log.runtime(TAG,"WebSettingsActivity.getModel: " + result);
                }
                return result;
            }
            return null;
        }

        @JavascriptInterface
        public String setModel(String modelCode, String fieldsValue) {
            ModelConfig modelConfig = ModelTask.getModelConfigMap().get(modelCode);
            if (modelConfig != null) {
                try {
                    ModelFields modelFields = modelConfig.getFields();
                    Map<String, ModelFieldShowDto> map = JsonUtil.parseObject(fieldsValue,
                            new TypeReference<Map<String, ModelFieldShowDto>>() {
                            });
                    if (map != null) {
                        for (Map.Entry<String, ModelFieldShowDto> entry : map.entrySet()) {
                            ModelFieldShowDto newModelField = entry.getValue();
                            if (newModelField != null) {
                                ModelField<?> modelField = modelFields.get(entry.getKey());
                                if (modelField != null) {
                                    String configValue = newModelField.getConfigValue();
                                    if (configValue == null || configValue.trim().isEmpty()) {
                                        continue;
                                    }
                                    modelField.setConfigValue(configValue);
                                }
                            }
                        }
                        return "SUCCESS";
                    }
                } catch (Exception e) {
                    Log.printStackTrace("WebSettingsActivity", e);
                }
            }
            return "FAILED";
        }

        @JavascriptInterface
        public String getField(String modelCode, String fieldCode) throws JSONException {
            ModelConfig modelConfig = ModelTask.getModelConfigMap().get(modelCode);
            if (modelConfig != null) {
                ModelField<?> modelField = modelConfig.getModelField(fieldCode);
                if (modelField != null) {
                    String result = JsonUtil.formatJson(ModelFieldInfoDto.toInfoDto(modelField), false);
                    if (BuildConfig.DEBUG) {
                        Log.runtime(TAG,"WebSettingsActivity.getField: " + result);
                    }
                    return result;
                }
            }
            return null;
        }

        @JavascriptInterface
        public String setField(String modelCode, String fieldCode, String fieldValue) {
            ModelConfig modelConfig = ModelTask.getModelConfigMap().get(modelCode);
            if (modelConfig != null) {
                try {
                    ModelField<?> modelField = modelConfig.getModelField(fieldCode);
                    if (modelField != null) {
                        modelField.setConfigValue(fieldValue);
                        return "SUCCESS";
                    }
                } catch (Exception e) {
                    Log.printStackTrace(e);
                }
            }
            return "FAILED";
        }

        @JavascriptInterface
        public void Log(String log) {
            Log.record(TAG,"设置：" + log);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 1, 1, "导出配置");
        menu.add(0, 2, 2, "导入配置");
        menu.add(0, 3, 3, "删除配置");
        menu.add(0, 4, 4, "单向好友");
        menu.add(0, 5, 5, "切换UI");
        menu.add(0, 6, 6, "保存");
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 1:
                Intent exportIntent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                exportIntent.addCategory(Intent.CATEGORY_OPENABLE);
                exportIntent.setType("*/*");
                exportIntent.putExtra(Intent.EXTRA_TITLE, "[" + userName + "]-config_v2.json");
//                startActivityForResult(exportIntent, EXPORT_REQUEST_CODE);
                exportLauncher.launch(exportIntent);
                break;
            case 2:
                Intent importIntent = new Intent(Intent.ACTION_GET_CONTENT);
                importIntent.addCategory(Intent.CATEGORY_OPENABLE);
                importIntent.setType("*/*");
                importIntent.putExtra(Intent.EXTRA_TITLE, "config_v2.json");
//                startActivityForResult(importIntent, IMPORT_REQUEST_CODE);
                importLauncher.launch(importIntent);
                break;
            case 3:
                new AlertDialog.Builder(context)
                        .setTitle("警告")
                        .setMessage("确认删除该配置？")
                        .setPositiveButton(R.string.ok, (dialog, id) -> {
                            File userConfigDirectoryFile;
                            if (StringUtil.isEmpty(userId)) {
                                userConfigDirectoryFile = Files.getDefaultConfigV2File();
                            } else {
                                userConfigDirectoryFile = Files.getUserConfigDir(userId);
                            }
                            if (Files.delFile(userConfigDirectoryFile)) {
                                Toast.makeText(this, "配置删除成功", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(this, "配置删除失败", Toast.LENGTH_SHORT).show();
                            }
                            finish();
                        })
                        .setNegativeButton(R.string.cancel, (dialog, id) -> dialog.dismiss())
                        .create()
                        .show();
                break;
            case 4:
                ListDialog.show(this, "单向好友列表", AlipayUser.getList(user -> user.getFriendStatus() != 1), SelectModelFieldFunc.newMapInstance(), false,
                        ListDialog.ListType.SHOW);
                break;
            case 5:
                UIConfig.INSTANCE.setUiOption(UI_OPTION_NEW);
                if (UIConfig.save()) {
                    Intent intent = new Intent(this, UIConfig.INSTANCE.getTargetActivityClass());
                    intent.putExtra("userId", userId);
                    intent.putExtra("userName", userName);
                    finish();
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "切换失败", Toast.LENGTH_SHORT).show();
                }
                break;
            case 6:
                // 在调用 save() 之前，先调用 JS 函数同步 WebView 中的数据到 Java 端
                Log.runtime(TAG,"WebSettingsActivity.onOptionsItemSelected: Calling handleData() in WebView");
                webView.evaluateJavascript("if(typeof handleData === 'function'){ handleData(); } else { console.error('handleData function not found'); }", null);
                // 使用 Handler 延迟执行 save()，给 JS 一点时间完成异步操作
                // 200 毫秒是一个经验值，如果仍然有问题可以适当增加
                new Handler(Looper.getMainLooper()).postDelayed(this::save, 200); // 延迟 200 毫秒
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void save() {
        if (Config.isModify(userId)) {
            if (Config.save(userId, false)) {
                Toast.makeText(context, "保存成功！", Toast.LENGTH_SHORT).show();
                if (!StringUtil.isEmpty(userId)) {
                    try {
                        Intent intent = new Intent("com.eg.android.AlipayGphone.sesame.restart");
                        intent.putExtra("userId", userId);
                        sendBroadcast(intent);
                    } catch (Throwable th) {
                        Log.printStackTrace(th);
                    }
                }
            }else{
                Toast.makeText(context, "保存失败！", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(context, "配置未修改，无需保存！", Toast.LENGTH_SHORT).show();
        }
        if (!StringUtil.isEmpty(userId)) {
            UserMap.save(userId);
            CooperateMap.getInstance(CooperateMap.class).save(userId);
        }
    }
}
