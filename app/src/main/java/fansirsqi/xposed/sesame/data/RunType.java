package fansirsqi.xposed.sesame.data;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
/**
 * 运行状态枚举类，用于表示不同的运行状态。
 * 每个枚举值代表一种状态，包含状态码和状态描述。
 */
@Getter
public enum RunType {
    DISABLE(0, "已关闭"),   // 0: 表示已关闭状态
    MODEL(1, "已激活"),     // 1: 表示已激活状态
    PACKAGE(2, "已加载");   // 2: 表示已加载状态
    // 状态码
    private final Integer code;
    // 状态名称
    private final String name;
    // 静态初始化映射表，用于根据状态码获取对应的 RunType
    private static final Map<Integer, RunType> CODE_MAP = new HashMap<>();
    // 静态初始化块，在枚举加载时填充 CODE_MAP
    static {
        // 遍历枚举值并将每个枚举值的 code 和枚举实例放入 CODE_MAP 中
        for (RunType runType : RunType.values()) {
            CODE_MAP.put(runType.code, runType);
        }
    }
    /**
     * 构造函数，初始化状态码和状态名称
     *
     * @param code 状态码
     * @param name 状态名称
     */
    RunType(Integer code, String name) {
        this.code = code;
        this.name = name;
    }
    /**
     * 根据状态码获取对应的 RunType。
     *
     * @param code 状态码
     * @return 对应的 RunType 枚举，如果不存在该状态码，返回 null
     */
    public static RunType getByCode(Integer code) {
        return CODE_MAP.get(code);
    }
}
