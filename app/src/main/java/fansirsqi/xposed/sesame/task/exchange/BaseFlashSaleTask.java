package fansirsqi.xposed.sesame.task.exchange;

import fansirsqi.xposed.sesame.data.Status;
import fansirsqi.xposed.sesame.model.BaseModel;
import fansirsqi.xposed.sesame.model.ModelFields;
import fansirsqi.xposed.sesame.model.modelFieldExt.IntegerModelField;
import fansirsqi.xposed.sesame.task.ModelTask;
import fansirsqi.xposed.sesame.task.TaskCommon;
import fansirsqi.xposed.sesame.util.Log;
import fansirsqi.xposed.sesame.util.Notify;
import fansirsqi.xposed.sesame.util.NtpTimeUtils;
import fansirsqi.xposed.sesame.util.TimeUtil;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public abstract class BaseFlashSaleTask extends ModelTask {

    // 时间常量定义
    protected static final long EXCHANGE_TARGET_TIME = 10 * 60 * 60 * 1000L; // 10:00:00
    protected static final long EXCHANGE_DEADLINE_OFFSET = TimeUnit.MINUTES.toMillis(1); // 截止时间偏移量（2分钟）
    // 线程池
    private final ExecutorService executorService = Executors.newFixedThreadPool(5);

    // 获取唤醒配置字段
    protected abstract IntegerModelField getWakeUpConfigField();
    private final IntegerModelField maxWaitTimeField = new IntegerModelField("maxWaitTime", "最大等待时间(分钟)", 20, 5, 60);

    //基础字段
    public BaseFlashSaleTask() {
        // 子类必须实现自己的字段和配置
    }
    @Override
    public ModelFields getFields() {
        ModelFields modelFields = new ModelFields();
        modelFields.addField(maxWaitTimeField); // 添加到公共字段
        return modelFields;
    }
    @Override
    public Boolean check() {
        if (TaskCommon.IS_ENERGY_TIME) {
            Log.record("⏸ 当前为只收能量时间【" + BaseModel.getEnergyTime().getValue() + "】，停止执行" + getName() + "任务！");
            return false;
        } else if (TaskCommon.IS_MODULE_SLEEP_TIME) {
            Log.record("💤 模块休眠时间【" + BaseModel.getModelSleepTime().getValue() + "】停止执行" + getName() + "任务！");
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void run() {
        executorService.submit(() -> {
            try {
                String taskName = getName();
                String threadName = Thread.currentThread().getName();

                if (Status.hasFlagToday(getCompletedKey())) {
                    Log.other(taskName + "⏰ [" + threadName + "] 今日已完成，跳过");
                    return;
                }

                if (TaskCommon.IS_MODULE_SLEEP_TIME) {
                    Log.other(taskName + "💤 模块休眠期间自动终止");
                    return;
                }

                long serverTime = NtpTimeUtils.getCurrentTimeMillis();
                long targetTime = calculateTargetTime(serverTime);
                long deadline = targetTime + EXCHANGE_DEADLINE_OFFSET;

                long now = NtpTimeUtils.getCurrentTimeMillis();

                if (now > deadline) {
                    Log.other(taskName + "⏰ [" + threadName + "] 当前时间已超过截止时间，不再执行");
                    Status.setFlagToday(getCompletedKey());
                    return;
                }
                if (targetTime - now > TimeUnit.MINUTES.toMillis(maxWaitTimeField.getValue())) {
                    Log.other(taskName + "⏰ [" + threadName + "] 距离目标时间超过" + maxWaitTimeField.getValue() + "分钟，跳过本次");
                    return;
                }
                String wakeUpTid = "WAKEUP_" + getClass().getSimpleName();
                if (isWakeUpNeeded(now, targetTime)) {
                    if (!hasChildTask(wakeUpTid)) {
                        long wakeUpTime = getWakeUpTime(targetTime);
                        addChildTask(new ChildModelTask(
                                wakeUpTid,
                                "秒杀唤醒",
                                () -> {
                                    synchronized (this) {
                                        this.notifyAll();
                                    }
                                    Log.other(taskName + "⏰ 唤醒任务执行完毕，准备进入兑换");
                                },
                                wakeUpTime
                        ));
                        Log.other(taskName + "⏰ [" + threadName + "] 已添加唤醒任务，将在 [" + TimeUtil.getCommonDate(wakeUpTime) + "] 唤醒");
                    }
                } else {
                    Log.other(taskName + "⏰ [" + threadName + "] 当前时间距目标<5分钟，无需唤醒");
                }

                // 唤醒或等待至目标时间前3秒
                synchronized (this) {
                    while (NtpTimeUtils.getCurrentTimeMillis() < targetTime - 3000) {
                        try {
                            wait(500);
                        } catch (InterruptedException e) {
                            Log.other(taskName + "💤 [" + threadName + "] 兑换任务被中断");
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }
                }

                Log.other(taskName + "⏰ [" + threadName + "] 开始进入兑换循环，目标时间：" + TimeUtil.getCommonDate(targetTime));

                List<ExchangeItem> exchangeItems = getExchangeItems();

                for (ExchangeItem item : exchangeItems) {
                    if (NtpTimeUtils.getCurrentTimeMillis() > deadline) {
                        Log.other(taskName + "⏰ [" + threadName + "] 超出截止时间");
                        break;
                    }

                    if (tryExchange(item, targetTime, deadline)) {
                        synchronized (this) {
                            if (!Status.hasFlagToday(getCompletedKey())) {
                                Status.setFlagToday(getCompletedKey());
                            }
                        }
                        onExchangeSuccess(item);
                        break;
                    }
                }

            } catch (Exception e) {
                Log.error(getName(), "兑换异常：" + e.getMessage());
            }
        });
    }



    /**
     * 判断是否需要添加唤醒子任务
     */
    protected boolean isWakeUpNeeded(long now, long targetTime) {
        long wakeUpTime = getWakeUpTime(targetTime); // 计算唤醒时间（目标时间-3分钟）
        return !Status.hasFlagToday(getCompletedKey()) && now < wakeUpTime; // 仅当当前时间早于唤醒时间
    }
    /**
     * 获取唤醒时间（提前3分钟）
     */
    protected long getWakeUpTime(long targetTime) {
        return Math.max(targetTime - 3 * 60 * 1000, System.currentTimeMillis());
    }


    /**
     * 计算目标时间（支持子类定制时间）
     */
    protected long calculateTargetTime(long serverTime) {
        Calendar calendar = Calendar.getInstance(Locale.CHINA);
        calendar.setTimeInMillis(serverTime);

        int currentHour = calendar.get(Calendar.HOUR_OF_DAY);
        int currentMinute = calendar.get(Calendar.MINUTE);

        long targetHour = getTargetHour();

        if (currentHour > targetHour || (currentHour == targetHour && currentMinute >= 1)) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        calendar.set(Calendar.HOUR_OF_DAY, (int) targetHour);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        return calendar.getTimeInMillis();
    }

    /**
     * 任务名
     * @return
     */
    public String getName() {
        return "公共模块";
    }
    /**
     * 子类可以重写此方法以指定不同的目标时间（如00:00或10:00）
     */
    protected long getTargetHour() {
        return 10; // 默认为10:00
    }
    /**
     * 获取兑换项列表（子类实现）
     */
    protected abstract List<ExchangeItem> getExchangeItems();

    /**
     * 尝试兑换一个项目（子类实现）
     */
    protected abstract boolean tryExchange(ExchangeItem item, long targetTime, long deadline);

    /**
     * 获取状态标记 key（子类实现）
     */
    protected abstract String getCompletedKey();

    /**
     * 兑换成功后回调（可选）
     */
    protected void onExchangeSuccess(ExchangeItem item) {
        // 默认空实现
        int id = 20262;
        Notify.sendNewNotification(Notify.context, getName(), "成功兑换:" + item.value, id);
    }

    // 内部类：兑换项（含价格和积分消耗）
    public static class ExchangeItem {
        public final String code;
        public final double value; // 价值（元）
        public final int cost;     // 积分花费

        public ExchangeItem(String code, double value, int cost) {
            this.code = code;
            this.value = value;
            this.cost = cost;
        }

        public double getRatio() {
            return value / cost; // 性价比 = 元 / 积分
        }

        @Override
        public String toString() {
            return String.format(Locale.CHINA, "%.1f元[%d积分]", value, cost);
        }
    }
}
