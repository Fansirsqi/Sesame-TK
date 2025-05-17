package fansirsqi.xposed.sesame.util;

import android.content.Context;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import fansirsqi.xposed.sesame.ui.RequestItem;

public class RequestStorage {
    private static final String REQUEST_FILE_NAME = "requests.json";

    /**
     * 保存请求列表到文件
     */
    public static void saveRequests(Context context, List<RequestItem> requests) {
        // 使用 JsonUtil 将对象转换为 JSON 字符串
        String json = JsonUtil.formatJson(requests);
        File requestFile = new File(Files.MAIN_DIR, REQUEST_FILE_NAME);
        Files.write2File(json, requestFile);
    }
    /**
     * 追加单个请求到文件中
     */
    public static void appendRequest(Context context, RequestItem request) {
        List<RequestItem> requests = loadRequests(context); // 读取已有数据
        requests.add(request);                               // 添加新项
        saveRequests(context, requests);                     // 覆盖写入更新后的完整列表
    }

    /**
     * 从文件加载请求列表
     */
    public static List<RequestItem> loadRequests(Context context) {
        File requestFile = new File(Files.MAIN_DIR, REQUEST_FILE_NAME);
        if (!requestFile.exists()) {
            return new ArrayList<>();
        }
        String json = Files.readFromFile(requestFile);
        // 使用 JsonUtil 将 JSON 字符串解析为对象列表
        return JsonUtil.parseList(json, RequestItem.class);
    }
}