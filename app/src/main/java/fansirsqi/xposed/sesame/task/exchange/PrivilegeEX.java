package fansirsqi.xposed.sesame.task.exchange;

import fansirsqi.xposed.sesame.data.Status;
import fansirsqi.xposed.sesame.hook.RequestManager;
import fansirsqi.xposed.sesame.model.BaseModel;
import fansirsqi.xposed.sesame.model.ModelGroup;
import fansirsqi.xposed.sesame.model.modelFieldExt.BooleanModelField;
import fansirsqi.xposed.sesame.model.ModelFields;
import fansirsqi.xposed.sesame.model.modelFieldExt.IntegerModelField;
import fansirsqi.xposed.sesame.task.TaskCommon;
import fansirsqi.xposed.sesame.task.otherTask.CompletedKeyEnum;
import fansirsqi.xposed.sesame.util.Log;
import fansirsqi.xposed.sesame.util.Notify;
import fansirsqi.xposed.sesame.util.NtpTimeUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class PrivilegeEX extends BaseFlashSaleTask {

    private static final String TAG = "青春特权兑换New🎓";

    @Override
    public String getName() {
        return "青春特权兑换New🎓";
    }

    @Override
    public ModelGroup getGroup() {
        return ModelGroup.PrivilegeEX;
    }

    @Override
    public String getIcon() {
        return "AntSports.png";
    }

    @Override
    public Boolean isSync() {
        return true;
    }

    private final BooleanModelField privilege = new BooleanModelField("privilege", "青春特权-兑换", false);
    private final BooleanModelField privilegeSmall = new BooleanModelField("privilegeSmall", "青春特权-兑换小额", false);
    private final IntegerModelField wakeUpMinuteBefore = new IntegerModelField("wakeUpMinuteBefore", "唤醒提前时间(分钟:值|最小|最大)", 4, 1, 30);

    @Override
    public ModelFields getFields() {
        ModelFields modelFields = super.getFields(); // 继承父类字段
        modelFields.addField(privilege);
        modelFields.addField(privilegeSmall);
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
        // 不再轮询等待，交给父类处理
        super.run(); // 调用 BaseFlashSaleTask 的 run()
    }




    @Override
    protected List<ExchangeItem> getExchangeItems() {
        List<ExchangeItem> items = new ArrayList<>();
        if (privilegeSmall.getValue()) {
            items.add(new ExchangeItem("small1", 0.05, 5));   // ratio=0.01
            items.add(new ExchangeItem("small2", 0.2, 2));     // ratio=0.1
            items.add(new ExchangeItem("small3", 0.5, 50));   // ratio=0.01
        } else {
            items.add(new ExchangeItem("large2", 50, 50));    // ratio=1
            items.add(new ExchangeItem("large3", 100, 100));  // ratio=1
            items.add(new ExchangeItem("large1", 20, 20));    // ratio=1
        }
        Collections.sort(items, new Comparator<ExchangeItem>() {
            @Override
            public int compare(ExchangeItem o1, ExchangeItem o2) {
                return Double.compare(o2.value, o1.value); // 降序排列
            }
        });
        return items;
    }

    @Override
    protected long getTargetHour() {
        return privilegeSmall.getValue() ? 0 : 10; // 小额：00:00，大额：10:00
    }

    @Override
    protected boolean tryExchange(BaseFlashSaleTask.ExchangeItem item, long targetTime, long deadline) {
        try {
            while (NtpTimeUtils.getCurrentTimeMillis() < deadline) {
                JSONArray requestData = new JSONArray();
                JSONObject benefit = new JSONObject();
                benefit.put("benefitId", item.code);
                requestData.put(benefit);

                String method = "alipay.membertangram.biz.rpc.student.largeCashExchangeTrigger";
                String methodSmall = "alipay.membertangram.biz.rpc.student.smallCashExchangeTrigger";
                String data = requestData.toString();
                if (privilegeSmall.getValue()){
                    method = methodSmall;
                }
                String res = RequestManager.requestString(method, data);
                JSONObject response = new JSONObject(res);
                String resultCode = response.optString("resultCode");
                String errorMsg = response.optString("errorMsg");
                String resultDesc = response.optString("resultDesc");

                if ("SUCCESS".equalsIgnoreCase(resultCode)) {
                    Log.other(getName() + "✅ 成功兑换：" + getItemDisplayName(item));
                    int id = 20261;
                    Notify.sendNewNotification(Notify.context, getName(), "成功兑换:" + getItemDisplayName(item), id);
                    return true;
                } else if ("ACTIVITY_AWARD_NOT_START".equalsIgnoreCase(resultCode)) {
                    Log.other(getName() + "⏳ 活动未开始，准备重试");
                    Thread.sleep(500); // 每次重试间隔0.5秒
                } else if("AMOUNT_NOT_NOT_ENOUGH".equalsIgnoreCase(errorMsg)){
                    Log.other(TAG + "❌ 兑换失败：" + item.value + "，错误：" + resultDesc);
                    Thread.sleep(500); // 每次重试间隔0.5秒
                }else {
                    Log.other(getName() + "❌ 兑换失败：" + item.code + " - " + errorMsg);
                    break;
                }
            }
        } catch (Exception e) {
            Log.error(TAG, "兑换[" + item.code + "]时发生异常：" + e.getMessage());
            return false;
        }
        return false;
    }

    @Override
    protected String getCompletedKey() {
        return privilegeSmall.getValue()
                ? CompletedKeyEnum.privilegeEXSmall.name()
                : CompletedKeyEnum.privilegeEXNew.name();
    }


    private String getItemDisplayName(ExchangeItem item) {
        if ("large1".equals(item.code)) return "20红包";
        if ("large2".equals(item.code)) return "50红包";
        if ("large3".equals(item.code)) return "100红包";
        if ("small1".equals(item.code)) return "0.05红包";
        if ("small2".equals(item.code)) return "0.2红包";
        if ("small3".equals(item.code)) return "0.5红包";
        return item.toString();
    }


    /**
     * 唤醒字段
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
    public Boolean check() {
        if (!super.check()) { // 先执行父类检查
            return false;
        }
        // 子类额外逻辑
        return true;
    }

}
