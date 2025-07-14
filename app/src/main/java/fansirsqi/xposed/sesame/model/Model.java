package fansirsqi.xposed.sesame.model;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import fansirsqi.xposed.sesame.model.modelFieldExt.BooleanModelField;
import fansirsqi.xposed.sesame.task.ModelTask;
import fansirsqi.xposed.sesame.util.Log;
import lombok.Getter;

public abstract class Model {
    private static final String TAG = Model.class.getSimpleName();
    private static final Map<String, ModelConfig> modelConfigMap = new LinkedHashMap<>();
    private static final Map<String, ModelConfig> readOnlyModelConfigMap = Collections.unmodifiableMap(modelConfigMap);
    private static final Map<ModelGroup, Map<String, ModelConfig>> groupModelConfigMap = new LinkedHashMap<>();
    private static final Map<Class<? extends Model>, Model> modelMap = new ConcurrentHashMap<>();
    private static final List<Class<? extends Model>> modelClazzList = ModelOrder.getClazzList();
    @Getter
    private static final Model[] modelArray = new Model[modelClazzList.size()];
    //    private static final List<Model> modelList = new LinkedList<>(Arrays.asList(modelArray));
//    @Getter public static final List<Model> readOnlyModelList = Collections.unmodifiableList(modelList);
    private final BooleanModelField enableField;

    public final BooleanModelField getEnableField() {
        return enableField;
    }

    public Model() {
        this.enableField = new BooleanModelField("enable", getEnableFieldName(), false);
    }

    public String getEnableFieldName() {
        return "开启" + getName();
    }

    public final Boolean isEnable() {
        return enableField.getValue();
    }

    public ModelType getType() {
        return ModelType.NORMAL;
    }

    public abstract String getName();

    public abstract ModelGroup getGroup();

    public abstract String getIcon();

    public abstract ModelFields getFields();

    public void prepare() {}

    public void boot(ClassLoader classLoader) {}

    public void destroy() {}

    public static Map<String, ModelConfig> getModelConfigMap() {
        return readOnlyModelConfigMap;
    }

    public static Set<ModelGroup> getGroupModelConfigGroupSet() {
        return groupModelConfigMap.keySet();
    }

    public static List<Map<String, ModelConfig>> getGroupModelConfigMapList() {
        List<Map<String, ModelConfig>> list = new ArrayList<>();
        for (Map<String, ModelConfig> modelConfigMap : groupModelConfigMap.values()) {
            list.add(Collections.unmodifiableMap(modelConfigMap));
        }
        return list;
    }

    public static Map<String, ModelConfig> getGroupModelConfig(ModelGroup modelGroup) {
        Map<String, ModelConfig> map = groupModelConfigMap.get(modelGroup);
        if (map == null) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(map);
    }

    public static Boolean hasModel(Class<? extends Model> modelClazz) {
        return modelMap.containsKey(modelClazz);
    }

    public static <T extends Model> T getModel(Class<T> modelClazz) {
        Model model = modelMap.get(modelClazz);
        if (modelClazz.isInstance(model)) {
            return modelClazz.cast(model);
        } else {
            Log.error("Model " + modelClazz.getSimpleName() + " not found.");
            return null;
        }
    }

    public static synchronized void initAllModel() {
        destroyAllModel();
        Log.runtime(TAG, "初始化所有模块数据");
        for (int i = 0, len = modelClazzList.size(); i < len; i++) {
            Class<? extends Model> modelClazz = modelClazzList.get(i);
            try {
                Model model = modelClazz.getDeclaredConstructor().newInstance();
                ModelConfig modelConfig = new ModelConfig(model);
                modelArray[i] = model;
                modelMap.put(modelClazz, model);
                String modelCode = modelConfig.getCode();
                modelConfigMap.put(modelCode, modelConfig);
                ModelGroup group = modelConfig.getGroup();
                Map<String, ModelConfig> modelConfigMap = groupModelConfigMap.get(group);
                if (modelConfigMap == null) {
                    modelConfigMap = new LinkedHashMap<>();
                    groupModelConfigMap.put(group, modelConfigMap);
                }
                modelConfigMap.put(modelCode, modelConfig);
            } catch (IllegalAccessException | InstantiationException | NoSuchMethodException |
                     InvocationTargetException e) {
                Log.printStackTrace(e);
            }
        }
    }

    public static synchronized void bootAllModel(ClassLoader classLoader) {
        for (Model model : modelArray) {
            try {
                model.prepare();
            } catch (Exception e) {
                Log.printStackTrace(e);
            }
            try {
                if (model.getEnableField().getValue()) {
                    model.boot(classLoader);
                }
            } catch (Exception e) {
                Log.printStackTrace(e);
            }
        }
    }

    public static synchronized void destroyAllModel() {
        Log.runtime(TAG, "销毁所有模块数据");
        for (int i = 0, len = modelArray.length; i < len; i++) {
            Model model = modelArray[i];
            if (model != null) {
                try {
                    if (ModelType.TASK == model.getType()) {
                        ((ModelTask) model).stopTask();
                    }
                    model.destroy();
                } catch (Exception e) {
                    Log.printStackTrace(e);
                }
                modelArray[i] = null;
            }
            modelMap.clear();
            modelConfigMap.clear();
        }
    }
}