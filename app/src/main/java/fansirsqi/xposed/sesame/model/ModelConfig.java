package fansirsqi.xposed.sesame.model;
import java.io.Serial;
import java.io.Serializable;
import java.util.Map;
import fansirsqi.xposed.sesame.model.modelFieldExt.BooleanModelField;
import fansirsqi.xposed.sesame.model.modelFieldExt.PriorityModelField;
import lombok.Data;
/**
 * 模型配置类，用于保存每个模型的配置信息。
 * 包括模型的基本信息（如名称、组、字段等），并提供方法来访问和操作这些配置项。
 */
@Data
public final class ModelConfig implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    // 模型的唯一标识符，通常是模型类名
    private String code;
    // 模型名称
    private String name;
    // 模型所属的组
    private ModelGroup group;
    // 模型图标（可选）
    private String icon;
    // 模型的所有字段
    private final ModelFields fields = new ModelFields();
    /**
     * 默认构造函数
     * 目前没有定义额外的数据类型（dataType），可以通过此构造函数进行初始化。
     */
    public ModelConfig() {
        //dataType = TypeUtil.getTypeArgument(this.getClass().getGenericSuperclass(), 0);
    }
    /**
     * 使用给定的模型实例来初始化模型配置
     *
     * @param model 模型实例
     */
    public ModelConfig(Model model) {
        this();
        // 设置模型的简单类名作为唯一标识符
        this.code = model.getClass().getSimpleName();
        // 设置模型的名称
        this.name = model.getName();
        // 设置模型所属组
        this.group = model.getGroup();
        // 设置模型的图标文件名称（图标位置app/src/main/assets/web/images/icon/model[/selected]，正常状态和选中状态）
        // 无图标定义时使用default.svg
        this.icon = model.getIcon();
        // 获取模型的启用字段，并将其加入到字段列表中
        PriorityModelField enableField = model.getEnableField();
        fields.put(enableField.getCode(), enableField);
        // 获取模型的其他字段，并将其加入到字段列表中
        ModelFields modelFields = model.getFields();
        if (modelFields != null) {
            for (Map.Entry<String, ModelField<?>> entry : modelFields.entrySet()) {
                ModelField<?> modelField = entry.getValue();
                if (modelField != null) {
                    fields.put(modelField.getCode(), modelField);
                }
            }
        }
    }
    /**
     * 判断模型是否包含指定字段
     *
     * @param fieldCode 字段代码
     * @return 如果模型包含该字段，则返回true，否则返回false
     */
    public Boolean hasModelField(String fieldCode) {
        return fields.containsKey(fieldCode);
    }
    /**
     * 获取指定字段代码的模型字段
     *
     * @param fieldCode 字段代码
     * @return 返回指定字段代码的模型字段
     */
    public ModelField<?> getModelField(String fieldCode) {
        return fields.get(fieldCode);
    }
    /*
     * 以下方法暂时被注释掉，若需要可以在未来进行实现。
     */
    /*
    public void removeModelField(String fieldCode) {
        fields.remove(fieldCode);
    }
    */
    /*
    public Boolean addModelField(ModelField modelField) {
        fields.put(modelField.getCode(), modelField);
        return true;
    }
    */
    /**
     * 获取指定字段代码的模型字段扩展类型
     *
     * @param fieldCode 字段代码
     * @param <T>       字段类型
     * @return 返回指定字段代码的模型字段扩展类型
     */
    @SuppressWarnings("unchecked")
    public <T extends ModelField<?>> T getModelFieldExt(String fieldCode) {
        return (T) fields.get(fieldCode);
    }
}
