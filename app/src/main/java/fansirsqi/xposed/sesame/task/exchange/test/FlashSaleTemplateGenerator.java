package fansirsqi.xposed.sesame.task.exchange.test;

import fansirsqi.xposed.sesame.model.ModelGroup;
import fansirsqi.xposed.sesame.task.exchange.BaseFlashSaleTask.ExchangeItem;

import java.util.List;
import java.util.Locale;

public class FlashSaleTemplateGenerator {

    /**
     * 生成完整的秒杀任务 Java 类代码
     *
     * @param className       类名，如 "PrivilegeLargeEX"
     * @param modelGroup      所属模型组，如 ModelGroup.PrivilegeEX
     * @param exchangeItems   兑换项列表
     * @param enableWakeUp    是否启用唤醒逻辑
     * @param targetHour      目标兑换时间（小时）
     * @return                完整的 Java 类源码
     */
    public static String generate(
            String className,
            ModelGroup modelGroup,
            List<ExchangeItem> exchangeItems,
            boolean enableWakeUp,
            long targetHour) {

        StringBuilder sb = new StringBuilder();

        // 包路径 & 导包
        sb.append("package fansirsqi.xposed.sesame.task.exchange;\n\n");
        sb.append("import org.json.JSONArray;\n");
        sb.append("import org.json.JSONObject;\n");
        sb.append("import fansirsqi.xposed.sesame.data.Status;\n");
        sb.append("import fansirsqi.xposed.sesame.hook.RequestManager;\n");
        sb.append("import fansirsqi.xposed.sesame.model.BaseModel;\n");
        sb.append("import fansirsqi.xposed.sesame.model.ModelGroup;\n");
        sb.append("import fansirsqi.xposed.sesame.model.modelFieldExt.BooleanModelField;\n");
        sb.append("import fansirsqi.xposed.sesame.model.modelFieldExt.IntegerModelField;\n");
        sb.append("import fansirsqi.xposed.sesame.model.ModelFields;\n");
        sb.append("import fansirsqi.xposed.sesame.task.TaskCommon;\n");
        sb.append("import fansirsqi.xposed.sesame.task.otherTask.CompletedKeyEnum;\n");
        sb.append("import fansirsqi.xposed.sesame.util.Log;\n");
        sb.append("import fansirsqi.xposed.sesame.util.Notify;\n");
        sb.append("import fansirsqi.xposed.sesame.util.NtpTimeUtils;\n");
        sb.append("import java.util.ArrayList;\n");
        sb.append("import java.util.Collections;\n");
        sb.append("import java.util.Comparator;\n");
        sb.append("import java.util.List;\n");
        sb.append("import org.json.JSONArray;\n");
        sb.append("import org.json.JSONObject;\n\n");

        // 类定义
        sb.append("public class ").append(className).append(" extends BaseFlashSaleTask {\n\n");

        // TAG 常量
        sb.append("    private static final String TAG = \"").append(className).append("⚖️\";\n\n");

        // getName()
        sb.append("    @Override\n");
        sb.append("    public String getName() {\n");
        sb.append("        return \"").append(className).append("\";\n");
        sb.append("    }\n\n");

        // getGroup()
        sb.append("    @Override\n");
        sb.append("    public ModelGroup getGroup() {\n");
        sb.append("        return ModelGroup.").append(modelGroup.name()).append(";\n");
        sb.append("    }\n\n");

        // getIcon()
        sb.append("    @Override\n");
        sb.append("    public String getIcon() {\n");
        sb.append("        return \"AntSports.png\";\n");
        sb.append("    }\n\n");

        // isSync()
        sb.append("    @Override\n");
        sb.append("    public Boolean isSync() {\n");
        sb.append("        return true;\n");
        sb.append("    }\n\n");

        // 启用字段
        String fieldName = toCamelCase(className, false);
        sb.append("    private final BooleanModelField ").append(fieldName).append(" = new BooleanModelField(\"")
                .append(fieldName).append("\", \"").append(className).append("\", false);\n");

        if (enableWakeUp) {
            sb.append("    private final IntegerModelField wakeUpMinuteBefore = new IntegerModelField(\"wakeUpMinuteBefore\", \"唤醒提前时间(分钟:值|最小|最大)\", 4, 1, 30);\n");
        }

        // getFields()
        sb.append("\n    @Override\n");
        sb.append("    public ModelFields getFields() {\n");
        sb.append("        ModelFields modelFields = new ModelFields();\n");
        sb.append("        modelFields.addField(").append(fieldName).append(");\n");

        if (enableWakeUp) {
            sb.append("        modelFields.addField(wakeUpMinuteBefore);\n");
        }

        sb.append("        return modelFields;\n");
        sb.append("    }\n\n");

        // run()
        sb.append("    @Override\n");
        sb.append("    public void run() {\n");
        sb.append("        new Thread(() -> {\n");
        sb.append("            try {\n");
        sb.append("                if (Status.hasFlagToday(getCompletedKey())) {\n");
        sb.append("                    return;\n");
        sb.append("                }\n\n");
        sb.append("                if (TaskCommon.IS_MODULE_SLEEP_TIME) {\n");
        sb.append("                    Log.other(TAG + \"💤 模块休眠期间自动终止\");\n");
        sb.append("                    return;\n");
        sb.append("                }\n\n");
        sb.append("                long serverTime = NtpTimeUtils.getCurrentTimeMillis();\n");
        sb.append("                long targetTime = calculateTargetTime(serverTime);\n");
        sb.append("                long deadline = targetTime + EXCHANGE_DEADLINE_OFFSET;\n");
        sb.append("                long now = NtpTimeUtils.getCurrentTimeMillis();\n\n");
        sb.append("                if (targetTime - now > MAX_WAIT_TIME) {\n");
        sb.append("                    Log.other(TAG + \"⏰ 距离目标时间超过5分钟，跳过本次\");\n");
        sb.append("                    return;\n");
        sb.append("                }\n\n");
        sb.append("                if (now > deadline) {\n");
        sb.append("                    Log.other(TAG + \"⏰ 当前时间已超过截止时间，不再执行\");\n");
        sb.append("                    return;\n");
        sb.append("                }\n\n");
        sb.append("                super.run();\n");
        sb.append("            } catch (Exception e) {\n");
        sb.append("                Log.error(TAG, \"并行兑换异常：\" + e.getMessage());\n");
        sb.append("            }\n");
        sb.append("        }).start();\n");
        sb.append("    }\n\n");

        // getExchangeItems()
        sb.append("    @Override\n");
        sb.append("    protected List<ExchangeItem> getExchangeItems() {\n");
        sb.append("        List<ExchangeItem> items = new ArrayList<>();\n");

        for (ExchangeItem item : exchangeItems) {
            sb.append("        items.add(new ExchangeItem(\"").append(item.code)
                    .append("\", ").append(item.value).append(", ").append(item.cost).append("));\n");
        }

        sb.append("        Collections.sort(items, new Comparator<ExchangeItem>() {\n");
        sb.append("            @Override\n");
        sb.append("            public int compare(ExchangeItem o1, ExchangeItem o2) {\n");
        sb.append("                return Double.compare(o2.value, o1.value);\n");
        sb.append("            }\n");
        sb.append("        });\n");
        sb.append("        return items;\n");
        sb.append("    }\n\n");

        // getTargetHour()
        sb.append("    @Override\n");
        sb.append("    protected long getTargetHour() {\n");
        sb.append("        return ").append(targetHour).append(";\n");
        sb.append("    }\n\n");

        // getCompletedKey()
        sb.append("    @Override\n");
        sb.append("    protected String getCompletedKey() {\n");
        sb.append("        return CompletedKeyEnum.").append(toSnakeCase(className)).append(".name();\n");
        sb.append("    }\n\n");

        // getItemDisplayName()
        sb.append("    private String getItemDisplayName(ExchangeItem item) {\n");
        for (ExchangeItem item : exchangeItems) {
            sb.append("        if (\"").append(item.code).append("\".equals(item.code)) return \"")
                    .append(String.format(Locale.CHINA, "%.1f元[%d积分]", item.value, item.cost)).append("\";\n");
        }
        sb.append("        return item.toString();\n");
        sb.append("    }\n\n");

        // getWakeUpTime()
        if (enableWakeUp) {
            sb.append("    @Override\n");
            sb.append("    protected long getWakeUpTime(long targetTime) {\n");
            sb.append("        int minutesBefore = wakeUpMinuteBefore.getValue();\n");
            sb.append("        return Math.max(targetTime - minutesBefore * 60 * 1000L, System.currentTimeMillis());\n");
            sb.append("    }\n\n");
        }

        // check()
        sb.append("    @Override\n");
        sb.append("    public Boolean check() {\n");
        sb.append("        if (TaskCommon.IS_ENERGY_TIME) {\n");
        sb.append("            Log.record(\"⏸ 当前为只收能量时间【\" + BaseModel.getEnergyTime().getValue() + \"】，停止执行\" + getName() + \"任务！\");\n");
        sb.append("            return false;\n");
        sb.append("        } else if (TaskCommon.IS_MODULE_SLEEP_TIME) {\n");
        sb.append("            Log.record(\"💤 模块休眠时间【\" + BaseModel.getModelSleepTime().getValue() + \"】停止执行\" + getName() + \"任务！\");\n");
        sb.append("            return false;\n");
        sb.append("        } else {\n");
        sb.append("            return true;\n");
        sb.append("        }\n");
        sb.append("    }\n\n");

        // 结束类
        sb.append("}\n");

        return sb.toString();
    }

    /**
     * 将类名转换为 Snake Case 用于枚举键
     */
    private static String toSnakeCase(String className) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < className.length(); i++) {
            char c = className.charAt(i);
            if (Character.isUpperCase(c) && i != 0) {
                sb.append("_");
            }
            sb.append(Character.toUpperCase(c));
        }
        return sb.toString();
    }

    /**
     * 驼峰命名转换
     */
    private static String toCamelCase(String name, boolean upperFirst) {
        StringBuilder sb = new StringBuilder();
        boolean nextUpper = upperFirst;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c == '_' || c == '-') {
                nextUpper = true;
            } else {
                if (nextUpper) {
                    sb.append(Character.toUpperCase(c));
                    nextUpper = false;
                } else {
                    sb.append(Character.toLowerCase(c));
                }
            }
        }
        return sb.toString();
    }
}
