package fansirsqi.xposed.sesame.task.otherTask;

import static fansirsqi.xposed.sesame.util.Notify.context;

import android.content.Context;
import android.content.IntentFilter;
import android.os.Build;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import fansirsqi.xposed.sesame.data.Status;
import fansirsqi.xposed.sesame.hook.RequestManager;
import fansirsqi.xposed.sesame.model.modelFieldExt.BooleanModelField;
import fansirsqi.xposed.sesame.task.TaskCommon;
import fansirsqi.xposed.sesame.util.Log;
import fansirsqi.xposed.sesame.util.Notify;
import fansirsqi.xposed.sesame.util.TimeUtil;

public class PrivilegeExTest extends BaseCommTask {
    // 常量定义
    private static final String TAG = "青春特权兑换🎁";
    private static final String DISPLAY_NAME = "青春特权兑换🎓";
    private static final String TARGET_TIME_STR = "10:00:00";
    private static final SimpleDateFormat TIME_FORMATTER = new SimpleDateFormat("HH:mm:ss", Locale.CHINA);
    private static final long EXCHANGE_START = 10 * 60 * 60 * 1000L; // 10:00:00
    private static final long PREPARE_TIME = 5000L; // 提前 5 秒启动

    // 线程安全集合
    private final Set<String> exchangedIds = new HashSet<>();

    // 执行器服务
    private final ExecutorService executorService = Executors.newFixedThreadPool(3);
    // 静态方法供外部调用
    public static void runExchangeTask() {
        new PrivilegeExTest().executePrivilegeExchange();
    }

    // 将核心逻辑提取为公共方法
    /**
     * 执行特权兑换主逻辑
     */
    public void executePrivilegeExchange() {
        try {
            if (TaskCommon.IS_MODULE_SLEEP_TIME) {
                Log.other("模块休眠期间自动终止特权任务");
                return;
            }
            if (Status.hasFlagToday(CompletedKeyEnum.privilegeEX.name())){
                return;
            }
            // 获取服务器时间并计算目标时间
            //long serverTime = TimeUtil.getServerTime();
            long serverTime =  System.currentTimeMillis();
            long targetTime = calculateTargetTime(serverTime); // 计算10:00:00时刻

            // 提前唤醒等待，直到整点
            long now = System.currentTimeMillis();
            long waitTime = targetTime - now;

            if (waitTime > 0) {
                long sleepDuration = Math.max(waitTime - 3000, 0);
                if (sleepDuration > 0) {
                    Log.other(displayName + "⏰ 预计将在 09:59:57 进入兑换循环");
                    Thread.sleep(sleepDuration);
                }
            }

            // 设置最大可接受时间（10:01:00）
            long deadline = targetTime + TimeUnit.MINUTES.toMillis(1);

            // 获取兑换项目列表
            List<String> benefitIds = getBenefitIds();
            if (benefitIds == null || benefitIds.isEmpty()) {
                Log.other("🎁 没有可兑换项目");
                return;
            }

            // 执行兑换（带智能重试）
            boolean success = executeUntilDeadline(benefitIds, deadline);

            if (!success) {
                Log.other("❌ 所有兑换尝试均失败");
            }

        } catch (InterruptedException ie) {
            Log.other("💤 兑换任务被中断");
        } catch (Exception e) {
            Log.error(TAG, "兑换异常：" + e.getMessage());
        }
    }


    // 新增精确时间计算
    private long calculateTargetTime(long serverTime) {
        Calendar calendar = Calendar.getInstance(Locale.CHINA);
        calendar.setTimeInMillis(serverTime);

        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        // 如果已经过了10:00:00 或者 已经是10:05之后，跳到明天
        if (hour > 10 || (hour == 10 && minute >= 5)) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        // 设置为今天的10:00:00.000
        calendar.set(Calendar.HOUR_OF_DAY, 10);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        return calendar.getTimeInMillis();
    }

    protected List<String> getBenefitIds() {
        List<String> benefitIds = new ArrayList<>();
        Collections.addAll(benefitIds, "large2", "large3", "large1"); // 可替换为从配置文件读取
        return benefitIds;
    }
    private boolean executeUntilDeadline(List<String> benefitIds, long deadline) throws JSONException, InterruptedException {
        boolean anySuccess = false;

        for (String benefitId : benefitIds) {
            boolean success = false;

            while (!success && System.currentTimeMillis() < deadline) {
                try {
                    JSONObject response = exchangeSingle(benefitId);
                    String resultCode = response.optString("resultCode");

                    if ("SUCCESS".equals(resultCode)) {
                        String name="无";
                        if (benefitId=="large1"){
                            name="20红包";
                        }
                        if (benefitId=="large2"){
                            name="50红包";
                        }
                        if (benefitId=="large3"){
                            name="100红包";
                        }
                        Log.other(displayName+"✅ 成功兑换：" + name);
                        int id = 20261;
                        Notify.sendNewNotification(Notify.context, displayName, "成功兑换:" + name, id);
                        if (!Status.hasFlagToday(CompletedKeyEnum.privilegeEX.name())) {
                            Status.setFlagToday(CompletedKeyEnum.privilegeEX.name());
                        }// ✅ 设置已完成
                        success = true;
                        anySuccess = true;
                    } else if ("ACTIVITY_AWARD_NOT_START".equals(resultCode)) {
                        Log.other(displayName+"⏳ 活动未开始，准备重试");
                        Thread.sleep(500); // 每次重试间隔0.5秒
                    } else {
                        Log.other(displayName+"❌ 兑换失败：" + benefitId + " - " + resultCode);
                        break; // 非活动错误码直接退出
                    }

                } catch (Exception e) {
                    Log.error(TAG, "兑换[" + benefitId + "]时发生异常：" + e.getMessage());
                    Thread.sleep(500); // 出错也继续重试
                }
            }

            if (!success) {
                Log.other("🚫 兑换失败：" + benefitId + "，已超时或不可恢复");
                Status.setFlagToday(CompletedKeyEnum.privilegeEX.name()); // ✅ 设置已完成
            }
        }

        return anySuccess;
    }


    /**
     * 兑换单个项目
     * @param benefitId
     * @return
     * @throws JSONException
     */
    private JSONObject exchangeSingle(String benefitId) throws JSONException {
        JSONArray requestData = new JSONArray();
        JSONObject benefit = new JSONObject();
        benefit.put("benefitId", benefitId);
        requestData.put(benefit);

        String method = "alipay.membertangram.biz.rpc.student.largeCashExchangeTrigger";
        String data = requestData.toString();
        String res = RequestManager.requestString(method, data);
        return new JSONObject(res);
    }


    @Override
    protected void handle() throws JSONException {
        try {
            // 计算目标时间
            long exchangeTime = calculateExchangeTime(TARGET_TIME_STR);

            // 时间有效性校验
            if (!validateExchangeTime(exchangeTime)) {
                return;
            }
            // 构建兑换参数（直接使用固定参数）
            List<String> benefitIds = new ArrayList<>();
            Collections.addAll(benefitIds, "large2", "large3", "large1"); // 替代List.of()

            // 调试日志
            Log.debug(TAG, "准备兑换参数: " + benefitIds);

            // 创建子任务并调度
            schedulePreciseExchange(exchangeTime, benefitIds);
            Log.other(DISPLAY_NAME + "将在 " + exchangeTime / 1000 + " 秒后执行兑换🔥");

            BooleanModelField privilegeNew = OtherTask.getPrivilegeNew();
            if (privilegeNew.getValue()) {
                // 使用新机制执行定时任务
                scheduleRunnableExchange(exchangeTime, benefitIds);
                Log.other(DISPLAY_NAME + "新机制法-->将在 " + exchangeTime / 1000 + " 秒后执行兑换🔥");
            }

        } catch (Exception e) {
            Log.error(TAG, "兑换任务处理异常: "+e.getMessage());
        }
    }
    //-------------------------------------------------------------------------------------------------------------------
    // 新增字段：任务线程池
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    /**
     * 使用 Runnable 实现定时兑换
     */
    private void scheduleRunnableExchange(long triggerTime, List<String> benefitIds) {
        long delay = triggerTime - System.currentTimeMillis();
        if (delay < 0) delay = 0;

        Log.other(DISPLAY_NAME + "将在 " + delay / 1000 + " 秒后执行兑换🔥");

        long finalDelay = delay;
        executor.execute(() -> {
            int retryCount = 0;
            boolean success = false;

            while (!success && retryCount < 5) {
                try {
                    Thread.sleep(finalDelay);
                    if (Status.hasFlagToday(CompletedKeyEnum.privilegeEX.name())) {
                        Log.other(DISPLAY_NAME + "该任务已执行过，跳过❌");
                        return;
                    }

                    Log.other(DISPLAY_NAME + "开始执行兑换🚀");
                    executeConcurrent(benefitIds);
                    Status.setFlagToday(CompletedKeyEnum.privilegeEX.name());
                    success = true;

                } catch (InterruptedException e) {
                    retryCount++;
                    Log.other(TAG, "任务被中断，第 " + retryCount + " 次重试...");
                    if (retryCount >= 3) {
                        Log.error(TAG, "任务重试失败");
                    }
                }
            }

        });
    }
    //-------------------------------------------------------------------------------------------------------------------

    /**
     * 验证兑换时间的有效性
     * @param exchangeTime 目标兑换时间戳
     * @return 是否有效
     */
    private boolean validateExchangeTime(long exchangeTime) {
        Calendar currentCal = Calendar.getInstance(Locale.CHINA);
        currentCal.setTime(new Date());

        Calendar targetCal = Calendar.getInstance(Locale.CHINA);
        targetCal.setTimeInMillis(exchangeTime);

        // 检查是否为目标日期的次日
        if (targetCal.get(Calendar.DAY_OF_YEAR) != currentCal.get(Calendar.DAY_OF_YEAR)) {
            Status.setFlagToday(CompletedKeyEnum.privilegeEX.name());
            Log.other(DISPLAY_NAME + "目标时间为明天，今日不执行");
            return false;
        }

        // 检查时间窗口（10:00:00 - 10:05:00）
        if (System.currentTimeMillis() > (exchangeTime + 2 * 60 * 1000L)) {
            Status.setFlagToday(CompletedKeyEnum.privilegeEX.name());
            Log.other(DISPLAY_NAME + "当前时间超过10:02，停止兑换");
            return false;
        }

        return true;
    }


    /**
     * 使用传统时间API计算目标时间戳
     * 完全兼容Android API 21+
     *
     * @param targetTime 目标时间字符串（格式：HH:mm:ss）
     * @return 目标时间的时间戳
     */
    private long calculateExchangeTime(String targetTime) {
        try {
            Date targetDate = TIME_FORMATTER.parse(targetTime);
            Calendar calendar = Calendar.getInstance(Locale.CHINA);
            calendar.setTime(new Date(System.currentTimeMillis()));
            calendar.set(Calendar.HOUR_OF_DAY, getHour(targetDate));
            calendar.set(Calendar.MINUTE, getMinute(targetDate));
            calendar.set(Calendar.SECOND, getSecond(targetDate));
            calendar.set(Calendar.MILLISECOND, 0);

            long targetTimestamp = calendar.getTimeInMillis();

            // 如果目标时间已过，则加一天
            if (System.currentTimeMillis() > targetTimestamp) {
                targetTimestamp += TimeUnit.DAYS.toMillis(1);
            }

            return targetTimestamp - PREPARE_TIME; // 提前 5 秒启动任务
        } catch (Exception e) {
            Log.error("解析目标时间失败: " + e.getMessage());
            return 0;
        }
    }

    /**
     * 安全获取小时数（替代已废弃的Date.getHours()）
     */
    private int getHour(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return cal.get(Calendar.HOUR_OF_DAY);
    }

    /**
     * 安全获取分钟数（替代已废弃的Date.getMinutes()）
     */
    private int getMinute(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return cal.get(Calendar.MINUTE);
    }

    /**
     * 安全获取秒数（替代已废弃的Date.getSeconds()）
     */
    private int getSecond(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return cal.get(Calendar.SECOND);
    }

    // 查询兑换信息
    private JSONObject query() throws JSONException {
        String method = "alipay.membertangram.biz.rpc.student.queryCashExchangeInfoResult";
        String data = "[{\"chInfo\":\"ch_appcenter__chsub_9patch\",\"skipTaskModule\":false}]";
        String res = RequestManager.requestString(method, data);
        return new JSONObject(res);
    }

    // 定时任务逻辑
//    private void schedulePreciseExchange(long delay, List<String> benefitIds) {
//        GlobalScheduler.getScheduler().schedule(() -> {
//            Log.other(DISPLAY_NAME + "开始秒杀兑换");
//            executeConcurrent(benefitIds);
//        }, delay, TimeUnit.MILLISECONDS);
//    }
    private void schedulePreciseExchange(long delay, List<String> benefitIds) {
        // 注册广播监听器（建议在模块加载时一次性注册）
        IntentFilter filter = new IntentFilter("com.example.ACTION_EXCHANGE");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(new ExchangeReceiver(), filter, Context.RECEIVER_EXPORTED);
        } else {
            context.registerReceiver(new ExchangeReceiver(), filter);
        }
        // 设置每日定时任务
        GlobalAlarmManager.scheduleDailyTask(context);
    }

    // 全局调度器
    public static class GlobalScheduler {
        private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor();

        public static ScheduledExecutorService getScheduler() {
            return SCHEDULER;
        }

        public static void shutdown() {
            SCHEDULER.shutdownNow();
        }
    }

    // 并发执行模式
    private void executeConcurrent(List<String> benefitIds) {
        Log.other(DISPLAY_NAME + "开始并发兑换，可兑换数量：" + benefitIds.size());

        for (String benefitId : benefitIds) {
            executorService.execute(() -> {
                try {
                    exchange(Collections.singletonList(benefitId));
                    Log.other(DISPLAY_NAME + "兑换请求已发送: " + benefitId);
                } catch (JSONException e) {
                    Log.error(DISPLAY_NAME + "兑换异常: " + e.getMessage());
                }
            });
        }
    }

    // 兑换方法
    private void exchange(List<String> benefitIds) throws JSONException {
        for (String benefitId : benefitIds) {
            try {
                // 构造请求参数
                JSONArray requestData = new JSONArray();
                JSONObject benefit = new JSONObject();
                benefit.put("benefitId", benefitId);
                requestData.put(benefit);

                // 调用兑换接口
                String method = "alipay.membertangram.biz.rpc.student.largeCashExchangeTrigger";
                String data = requestData.toString();
                String res = RequestManager.requestString(method, data);

                // 解析响应数据
                JSONObject response = new JSONObject(res);
                String resultCode = response.getString("resultCode");
                String resultDesc = response.getString("resultDesc");

                if ("SUCCESS".equals(resultCode)) {
                    Log.other(DISPLAY_NAME + "兑换成功: " + benefitId + " - " + resultDesc);
                } else {
                    Log.other(DISPLAY_NAME + "兑换失败: " + benefitId + " - " + resultDesc);
                }
            } catch (Exception e) {
                Log.error(DISPLAY_NAME + "处理兑换[" + benefitId + "]时发生异常: ", e.getMessage());
            }
        }
    }

}
