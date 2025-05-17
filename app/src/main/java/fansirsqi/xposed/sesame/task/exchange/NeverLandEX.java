package fansirsqi.xposed.sesame.task.exchange;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import fansirsqi.xposed.sesame.data.Status;
import fansirsqi.xposed.sesame.hook.RequestManager;
import fansirsqi.xposed.sesame.model.ModelFields;
import fansirsqi.xposed.sesame.model.ModelGroup;
import fansirsqi.xposed.sesame.model.modelFieldExt.BooleanModelField;
import fansirsqi.xposed.sesame.model.modelFieldExt.IntegerModelField;
import fansirsqi.xposed.sesame.task.TaskCommon;
import fansirsqi.xposed.sesame.task.otherTask.CompletedKeyEnum;
import fansirsqi.xposed.sesame.util.Log;
import fansirsqi.xposed.sesame.util.Notify;
import fansirsqi.xposed.sesame.util.NtpTimeUtils;

public class NeverLandEX extends BaseFlashSaleTask {

    private static final String TAG = "健康岛EX🍰";
    @Override
    public String getName() {
        return "健康岛兑换";
    }

    @Override
    public ModelGroup getGroup() {
        return ModelGroup.NeverLandEX;
    }

    @Override
    public String getIcon() {
        return "AntSports.png";
    }

    @Override
    public Boolean isSync() {
        return true;
    }
    // 缓存动态列表
    private List<ExchangeItem> dynamicItemsCache = null;

    private final BooleanModelField enableNeverLandEX = new BooleanModelField("enableNeverLandEX", "健康岛兑换", false);
    private final IntegerModelField wakeUpMinuteBefore = new IntegerModelField("wakeUpMinuteBefore", "唤醒提前时间(分钟:值|最小|最大)", 4, 1, 30);

    @Override
    public ModelFields getFields() {
        ModelFields modelFields = super.getFields(); // 继承父类字段
        modelFields.addField(enableNeverLandEX);
        modelFields.addField(wakeUpMinuteBefore);
        return modelFields;
    }
    @Override
    public void run() {
        if (Status.hasFlagToday(getCompletedKey())) {
            return;
        }
        if (TaskCommon.IS_MODULE_SLEEP_TIME) {
            Log.other(TAG + "💤 模块休眠期间自动终止");
            return;
        }
        // 第一步：预加载兑换列表（仅执行一次）
        try {
            loadExchangeList();
        } catch (Exception e) {
            Log.error(TAG, "获取兑换列表时发生异常：" + e.getMessage());
        }

        super.run(); // 直接调用 BaseFlashSaleTask 的 run()
    }



    @Override
    protected boolean tryExchange(BaseFlashSaleTask.ExchangeItem item, long targetTime, long deadline) {
        try {
            while (NtpTimeUtils.getCurrentTimeMillis() < deadline) {
                String data = "[{\"chInfo\":\"chappid20001003chsubpageidcom.alipay.android.phone.businesscommon.globalsearch.ui.MainSearchActivity\"," +
                        " \"cityCode\": \"330100\", \"itemId\": \""+item.code+"\"}]";
                String response = RequestManager.requestString("com.alipay.neverland.biz.rpc.createOrder", data);
                JSONObject json = new JSONObject(response);
                String resultCode = json.optString("resultCode");
                String errorMsg = json.optString("errorMsg");
                String errorCode = json.optString("errorCode");

                if ("SUCCESS".equals(resultCode)) {
                    Log.other(TAG + "✅ 成功兑换：" + item.value);
                    return true;
                } else if ("ACTIVITY_AWARD_NOT_START".equals(resultCode)) {
                    Log.other(TAG + "⏳ 活动未开始，准备重试");
                    Thread.sleep(500); // 延迟重试
                } else if ("ITEM_PURCHASE_ERROR".equals(errorCode)) {
                    Log.other(TAG + "❌ 兑换失败：" + item.value + "，错误：" + errorMsg);
                    Thread.sleep(100); // 延迟重试
                } else {
                    Log.other(TAG + "❌ 兑换失败：" + item.value + "，错误：" + response);
                   break;
                }
            }
        } catch (Exception e) {
            Log.error(TAG, "兑换[" + item.value + "]时发生异常：" + e.getMessage());
        }
        return false;
    }

    /**
     * 预加载兑换列表（仅执行一次）
     */
    private void loadExchangeList() throws Exception {
        synchronized (this) {

            try {
                dynamicItemsCache = getDynamicExchangeList();
                Log.other(TAG + "✅ 成功获取动态兑换列表，共" + dynamicItemsCache.size() + "项");
            } catch (Exception e) {
                dynamicItemsCache = defaultExchangeItems();
                Log.other(TAG + "⚠️ 动态列表获取失败，使用默认列表: " + e.getMessage());
            }

            // 按金额降序排序（确保每次调用顺序一致）
            Collections.sort(dynamicItemsCache, (o1, o2) -> Double.compare(o2.value, o1.value));
        }
    }

    /**
     * 获取动态兑换列表（无门槛红包）
     */
    private List<ExchangeItem> getDynamicExchangeList() throws Exception {
        List<ExchangeItem> items = new ArrayList<>();

        String method = "com.alipay.neverland.biz.rpc.queryFlashSaleItemList";
        String response = RequestManager.requestString(method, "[{}]");

        JSONObject json = new JSONObject(response);
        if (!json.optBoolean("success")) {
            throw new Exception("接口返回失败: " + json.optString("message"));
        }

        JSONArray itemVOList = json.optJSONObject("data").optJSONArray("itemVOList");
        if (itemVOList == null || itemVOList.length() == 0) {
            throw new Exception("itemVOList为空");
        }

        for (int i = 0; i < itemVOList.length(); i++) {
            JSONObject itemVO = itemVOList.getJSONObject(i);
            if ("coupon".equals(itemVO.optString("itemSpecialType")) &&
                    !"ITEM_SALE_LIMITED".equals(itemVO.optString("status"))) {

                String itemId = itemVO.optString("itemId");
                double value = itemVO.optDouble("originalPrice", 0.0);
                int cost = itemVO.optInt("couponSalePrice", 0);

                if (value > 0 && cost > 0 && !itemId.isEmpty()) {
                    items.add(new ExchangeItem(itemId, value, cost));
                }
            }
        }

        return items;
    }

    /**
     * 默认兑换列表（兜底方案）
     */
    private List<ExchangeItem> defaultExchangeItems() {
        List<ExchangeItem> items = new ArrayList<>();
        items.add(new ExchangeItem("IT20250403000300103020", 0.1, 30));
        items.add(new ExchangeItem("IT20250403000500103019", 0.5, 150));
        items.add(new ExchangeItem("IT20250403000700103018", 0.8, 240));
        items.add(new ExchangeItem("IT20250403000900103017", 1.0, 300));
        items.add(new ExchangeItem("IT20250403000100103002", 2.0, 600));
        items.add(new ExchangeItem("IT20250403000100103016", 3.0, 900));
        items.add(new ExchangeItem("IT20250422000600103913", 4.0, 1200));
        items.add(new ExchangeItem("IT20250403000300103001", 5.0, 1500));

        return items;
    }

    @Override
    protected List<BaseFlashSaleTask.ExchangeItem> getExchangeItems() {
        // 直接返回预加载的缓存数据
        return dynamicItemsCache != null ? dynamicItemsCache : defaultExchangeItems();
    }


    @Override
    public Boolean check() {
        if (!super.check()) { // 先执行父类检查
            return false;
        }
        // 子类额外逻辑
        return true;
    }

    /**
     * 获取唤醒配置字段
     * @return
     */
    @Override
    protected IntegerModelField getWakeUpConfigField() {
        return wakeUpMinuteBefore;
    }
    /**
     * 唤醒时间
     * @param targetTime
     * @return
     */
    @Override
    protected long getWakeUpTime(long targetTime) {
        int minutesBefore = wakeUpMinuteBefore.getValue();
        return Math.max(targetTime - minutesBefore * 60 * 1000L, System.currentTimeMillis());
    }

    @Override
    protected String getCompletedKey() {
        return CompletedKeyEnum.neverLandEX.name();
    }

    @Override
    protected long getTargetHour() {
        return 10;
    }
}

//@Override
//protected List<BaseFlashSaleTask.ExchangeItem> getExchangeItems() {
//    List<BaseFlashSaleTask.ExchangeItem> items = new ArrayList<>();
//    items.add(new BaseFlashSaleTask.ExchangeItem("IT20250403000300103020", 0.1, 30));     // 0.1元 - 30积分
//    items.add(new BaseFlashSaleTask.ExchangeItem("00500103019IT202504030", 0.5, 150));    // 0.5元 - 150积分
//    items.add(new BaseFlashSaleTask.ExchangeItem("IT20250403000700103018", 0.8, 240));    // 0.8元 - 240积分
//    items.add(new BaseFlashSaleTask.ExchangeItem("IT20250403000900103017", 1.0, 300));    // 1元 - 300积分
//    items.add(new BaseFlashSaleTask.ExchangeItem("IT20250403000100103002", 2.0, 600));    // 2元 - 600积分
//    items.add(new BaseFlashSaleTask.ExchangeItem("IT20250403000100103016", 3.0, 900));    // 3元 - 900积分
//    items.add(new BaseFlashSaleTask.ExchangeItem("IT20250422000600103913", 4.0, 1200));   // 4元 - 1200积分
//    items.add(new BaseFlashSaleTask.ExchangeItem("IT20250403000300103001", 5.0, 1500));   // 5元 - 1500积分
//    // 按金额从高到低排序
//    Collections.sort(items, new Comparator<BaseFlashSaleTask.ExchangeItem>() {
//        @Override
//        public int compare(BaseFlashSaleTask.ExchangeItem o1, BaseFlashSaleTask.ExchangeItem o2) {
//            return Double.compare(o2.value, o1.value); // 降序排列
//        }
//    });
//    return items;
//}