package fansirsqi.xposed.sesame.model;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.json.JSONException;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.Objects;

import fansirsqi.xposed.sesame.R;
import fansirsqi.xposed.sesame.util.JsonUtil;
import fansirsqi.xposed.sesame.util.TypeUtil;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
public class ModelField<T> implements Serializable {
    @JsonIgnore
    public final Type valueType; // 存储字段值的类型
    @JsonIgnore
    public String code; // 字段代码
    @JsonIgnore
    public String name; // 字段名称
    @JsonIgnore
    public T defaultValue; // 默认值
    @JsonIgnore
    public String desc;
    @Setter
    @Getter
    public volatile T value; // 当前值
    /**
     * 默认构造函数，初始化字段值类型
     */
    public ModelField() {
        valueType = TypeUtil.getTypeArgument(this.getClass().getGenericSuperclass(), 0);
    }
    /**
     * 构造函数，接受初始值
     *
     * @param value 初始值
     */
    public ModelField(T value) {
        this(null, null, value);
    }
    /**
     * 构造函数，接受字段代码、名称和初始值
     *
     * @param code  字段代码
     * @param name  字段名称
     * @param value 字段初始值
     */
    public ModelField(String code, String name, T value) {
        this(); // 调用默认构造函数
        this.code = code;
        this.name = name;
        this.defaultValue = value; // 设置默认值
        this.desc = null;
        setObjectValue(value); // 设置当前值
    }
    public ModelField(String code, String name, T value, String desc) {
        this();
        this.code = code;
        this.name = name;
        this.defaultValue = value;
        this.desc = desc;
        setObjectValue(value);
    }
    /**
     * 设置当前值
     *
     * @param objectValue 要设置的值
     */
    public void setObjectValue(Object objectValue) {
        if (objectValue == null) {
            reset(); // 如果传入值为 null，则重置为默认值
            return;
        }
        value = JsonUtil.parseObject(objectValue, valueType); // 解析并设置当前值
    }
    /**
     * 获取字段类型
     *
     * @return 字段类型字符串
     */
    @JsonIgnore
    public String getType() {
        return "DEFAULT"; // 默认返回类型
    }
    /**
     * 获取扩展键
     *
     * @return 扩展键
     */
    @JsonIgnore
    public Object getExpandKey() {
        return null; // 默认返回 null
    }
    /**
     * 获取扩展值
     *
     * @return 扩展值
     */
    @JsonIgnore
    public Object getExpandValue() throws JSONException {
        return null; // 默认返回 null
    }
    /**
     * 将当前值转换为配置值
     *
     * @param value 当前值
     * @return 配置值
     */
    public Object toConfigValue(T value) {
        return value; // 默认返回当前值
    }
    /**
     * 从配置值转换为对象值
     *
     * @param value 配置值
     * @return 对象值
     */
    public Object fromConfigValue(String value) {
        return value; // 默认返回配置值
    }
    /**
     * 获取当前值的配置字符串表示
     *
     * @return 配置字符串
     */
    @JsonIgnore
    public String getConfigValue() {
        return JsonUtil.formatJson(toConfigValue(value)); // 转换为 JSON 字符串
    }
    /**
     * 设置配置值
     *
     * @param configValue 配置值字符串
     */
    @JsonIgnore
    public void setConfigValue(String configValue) {
        if (configValue == null) {
            reset(); // 如果配置值为 null，则重置为默认值
            return;
        }
        Object objectValue = fromConfigValue(configValue); // 从配置值转换为对象值
        // 如果对象值与配置值相等，则直接解析配置值
        if (Objects.equals(objectValue, configValue)) {
            value = JsonUtil.parseObject(configValue, valueType);
        } else {
            value = JsonUtil.parseObject(objectValue, valueType);
        }
    }

    /**
     * 重置当前值为默认值
     */
    public void reset() {
        value = defaultValue; // 设置当前值为默认值
    }
    /**
     * 获取字段的视图
     *
     * @param context 上下文对象
     * @return 生成的视图
     */
    @JsonIgnore
    public View getView(Context context) {
        TextView btn = new TextView(context); // 创建 TextView 控件
        btn.setText(getName()); // 设置文本为字段名称
        btn.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)); // 设置布局参数
        btn.setTextColor(ContextCompat.getColor(context, R.color.selection_color)); // 设置文本颜色
        btn.setBackground(ContextCompat.getDrawable(context, R.drawable.dialog_list_button));
        btn.setGravity(Gravity.START | Gravity.CENTER_VERTICAL); // 设置文本对齐方式
        btn.setMinHeight(150); // 设置最小高度
        btn.setMaxHeight(180); // 设置最大高度
        btn.setPaddingRelative(40, 0, 40, 0); // 设置左右内边距
        btn.setAllCaps(false); // 设置不全大写
        // 设置点击事件，显示无配置项的提示
        btn.setOnClickListener(v -> Toast.makeText(context, "无配置项", Toast.LENGTH_SHORT).show());
        return btn; // 返回生成的视图
    }
}
