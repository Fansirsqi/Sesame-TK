package fansirsqi.xposed.sesame.model.modelFieldExt;
import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import androidx.core.content.ContextCompat;
import fansirsqi.xposed.sesame.util.Log;
import lombok.Getter;
import fansirsqi.xposed.sesame.R;
import fansirsqi.xposed.sesame.model.ModelField;
import fansirsqi.xposed.sesame.ui.StringDialog;
/**
 * Integer 类型字段类，继承自 ModelField<Integer>
 * 该类用于表示具有最小值和最大值限制的整数字段。
 */
@Getter
public class IntegerModelField extends ModelField<Integer> {
    /** 最小值限制 */
    protected final Integer minLimit;
    /** 最大值限制 */
    protected final Integer maxLimit;
    /**
     * 构造函数：创建一个没有最小值和最大值限制的 Integer 类型字段
     *
     * @param code 字段代码
     * @param name 字段名称
     * @param value 字段初始值
     */
    public IntegerModelField(String code, String name, Integer value) {
        super(code, name, value);  // 调用父类的构造函数
        this.minLimit = null;  // 无最小值限制
        this.maxLimit = null;  // 无最大值限制
    }
    /**
     * 构造函数：创建一个具有最小值和最大值限制的 Integer 类型字段
     *
     * @param code 字段代码
     * @param name 字段名称
     * @param value 字段初始值
     * @param minLimit 最小值限制
     * @param maxLimit 最大值限制
     */
    public IntegerModelField(String code, String name, Integer value, Integer minLimit, Integer maxLimit) {
        super(code, name, value);  // 调用父类的构造函数
        this.minLimit = minLimit;  // 设置最小值限制
        this.maxLimit = maxLimit;  // 设置最大值限制
    }
    /**
     * 获取字段类型
     *
     * @return 返回字段类型的字符串表示 "INTEGER"
     */
    @Override
    public String getType() {
        return "INTEGER";
    }
    /**
     * 获取字段的配置值（将当前的值转换为字符串）
     *
     * @return 返回字段的字符串形式的配置值
     */
    @Override
    public String getConfigValue() {
        return String.valueOf(value);  // 返回字段值的字符串表示
    }
    /**
     * 设置字段的配置值（根据配置值设置新的值，并且在有最小/最大值限制的情况下进行限制）
     *
     * @param configValue 字段的配置值
     */
    @Override
    public void setConfigValue(String configValue) {
        Integer newValue;
        // 如果配置值为空，使用默认值
        if (configValue == null || configValue.trim().isEmpty()) {
            newValue = defaultValue;
        } else {
            try {
                // 尝试将配置值转换为整数
                newValue = Integer.parseInt(configValue);
            } catch (Exception e) {
                Log.printStackTrace(e);  // 异常处理，打印栈追踪
                newValue = defaultValue;  // 如果转换失败，使用默认值
            }
        }
        // 根据最小值限制调整新值
        if (minLimit != null) {
            newValue = Math.max(minLimit, newValue);
        }
        // 根据最大值限制调整新值
        if (maxLimit != null) {
            newValue = Math.min(maxLimit, newValue);
        }
        // 设置字段值
        this.value = newValue;
    }
    /**
     * 获取视图（返回一个 Button，点击后弹出编辑框）
     *
     * @param context 上下文
     * @return 按钮视图
     */
    @Override
    public View getView(Context context) {
        Button btn = new Button(context);
        // 设置按钮的文本为字段名称
        btn.setText(getName());
        // 设置按钮的布局参数
        btn.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        // 设置按钮的文本颜色
        btn.setTextColor(ContextCompat.getColor(context, R.color.selection_color));
        // 设置按钮的背景
        btn.setBackground(ContextCompat.getDrawable(context, R.drawable.dialog_list_button));
        // 设置按钮的文本对齐方式
        btn.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        // 设置按钮的最小高度
        btn.setMinHeight(150);
        // 设置按钮的最大高度
        btn.setMaxHeight(180);
        // 设置按钮的左右内边距
        btn.setPaddingRelative(40, 0, 40, 0);
        // 设置按钮的文本不全大写
        btn.setAllCaps(false);
        // 设置点击事件，弹出编辑对话框
        btn.setOnClickListener(v -> StringDialog.showEditDialog(v.getContext(), ((Button) v).getText(), this));
        return btn;
    }
    /**
     * MultiplyIntegerModelField 类，继承自 IntegerModelField，处理带乘数的整数类型字段
     * 该类在设置值时会乘以指定的倍数。
     */
    @Getter
    public static class MultiplyIntegerModelField extends IntegerModelField {
        /** 乘数，用于计算最终值 */
        private final Integer multiple;
        /**
         * 构造函数：创建一个带乘数限制的整数类型字段
         *
         * @param code 字段代码
         * @param name 字段名称
         * @param value 默认设置值
         * @param minLimit 最小值限制
         * @param maxLimit 最大值限制
         * @param multiple 乘数 eg:用于将字段值从分钟转换为毫秒 1min * 60_000
         */
        public MultiplyIntegerModelField(String code, String name, Integer value, Integer minLimit, Integer maxLimit, Integer multiple) {
            super(code, name, value * multiple, minLimit, maxLimit);  // 调用父类构造函数，并且初始值乘以 multiple
            this.multiple = multiple;  // 设置乘数
        }
        /**
         * 获取字段类型
         *
         * @return 返回字段类型的字符串表示 "MULTIPLY_INTEGER"
         */
        @Override
        public String getType() {
            return "MULTIPLY_INTEGER";
        }
        /**
         * 设置字段的配置值（乘数影响最终值）
         *
         * @param configValue 字段的配置值
         */
        @Override
        public void setConfigValue(String configValue) {
            if (configValue == null || configValue.trim().isEmpty()) {
                reset();  // 如果配置值为空，则重置字段
                return;
            }
            super.setConfigValue(configValue);  // 调用父类的 setConfigValue 方法
            try {
                // 根据乘数调整值
                value = value * multiple;  // 使用乘数调整字段值
                return;
            } catch (Exception e) {
                Log.printStackTrace(e);  // 异常处理
            }
            reset();  // 如果出现异常，重置字段
        }
        /**
         * 获取字段的配置值（返回值除以乘数）
         *
         * @return 配置值（字段值除以乘数）
         */
        @Override
        public String getConfigValue() {
            return String.valueOf(value / multiple);  // 使用乘数获取实际配置值
        }
    }
}
