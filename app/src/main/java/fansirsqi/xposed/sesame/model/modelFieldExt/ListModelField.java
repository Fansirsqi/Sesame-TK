package fansirsqi.xposed.sesame.model.modelFieldExt;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.core.content.ContextCompat;

import com.fasterxml.jackson.core.type.TypeReference;

import java.util.ArrayList;
import java.util.List;

import fansirsqi.xposed.sesame.R;
import fansirsqi.xposed.sesame.model.ModelField;
import fansirsqi.xposed.sesame.ui.StringDialog;
/**
 * 表示一个存储字符串列表的字段模型，用于管理和展示列表数据。
 * 提供基本的获取类型、配置值以及视图展示的方法。
 */
public class ListModelField extends ModelField<List<String>> {
    // JSON 类型引用，用于序列化和反序列化 List<String>
    private static final TypeReference<List<String>> typeReference = new TypeReference<List<String>>() {};
    /**
     * 构造方法，初始化字段模型。
     *
     * @param code  字段的唯一标识符
     * @param name  字段的名称
     * @param value 字段的默认值（字符串列表）
     */
    public ListModelField(String code, String name, List<String> value) {
        super(code, name, value);
    }
    /**
     * 获取字段的类型。
     *
     * @return 返回字段类型 "LIST"
     */
    @Override
    public String getType() {
        return "LIST";
    }
    /**
     * 获取用于展示该字段的视图组件。
     *
     * @param context 上下文环境
     * @return 返回一个按钮视图，用于触发编辑功能
     */
    @Override
    public View getView(Context context) {
        Button btn = new Button(context);
        btn.setText(getName());
        btn.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        btn.setTextColor(ContextCompat.getColor(context, R.color.button));
        // 根据API版本选择合适的方法获取Drawable资源
        Drawable drawable;
        drawable = context.getResources().getDrawable(R.drawable.button, context.getTheme());
        btn.setBackground(drawable);
        btn.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        btn.setMinHeight(150);
        btn.setMaxHeight(180);
        btn.setPaddingRelative(40, 0, 40, 0);
        btn.setAllCaps(false);
        // 设置按钮点击事件，打开编辑对话框
        btn.setOnClickListener(v -> StringDialog.showEditDialog(v.getContext(), ((Button) v).getText(), this));
        return btn;
    }
    /**
     * 一个子类，用于将字符串列表转换为逗号分隔的字符串，并实现相应的设置和获取功能。
     */
    public static class ListJoinCommaToStringModelField extends ListModelField {
        /**
         * 构造方法，初始化字段模型。
         *
         * @param code  字段的唯一标识符
         * @param name  字段的名称
         * @param value 字段的默认值（字符串列表）
         */
        public ListJoinCommaToStringModelField(String code, String name, List<String> value) {
            super(code, name, value);
        }
        /**
         * 设置配置值，将逗号分隔的字符串转换为字符串列表。
         *
         * @param configValue 配置值，逗号分隔的字符串
         */
        @Override
        public void setConfigValue(String configValue) {
            if (configValue == null) {
                reset();
                return;
            }
            // 根据逗号分隔符解析字符串，并过滤掉空字符串
            List<String> list = new ArrayList<>();
            for (String str : configValue.split(",")) {
                if (!str.isEmpty()) {
                    list.add(str);
                }
            }
            value = list;
        }
        /**
         * 获取配置值，将字符串列表拼接为逗号分隔的字符串。
         *
         * @return 配置值，逗号分隔的字符串
         */
        @Override
        public String getConfigValue() {
            return String.join(",", value);
        }
    }
}
