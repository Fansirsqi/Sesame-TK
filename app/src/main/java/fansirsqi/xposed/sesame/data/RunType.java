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
    private final Integer code;
    private final String name;

    private static final Map<Integer, RunType> CODE_MAP = new HashMap<>();

    static {
        for (RunType runType : RunType.values()) {
            CODE_MAP.put(runType.code, runType);
        }
    }

    RunType(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    public static RunType getByCode(Integer code) {
        return CODE_MAP.get(code);
    }
}
