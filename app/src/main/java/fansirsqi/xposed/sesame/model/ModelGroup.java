package fansirsqi.xposed.sesame.model;
import lombok.Getter;
import java.util.HashMap;
import java.util.Map;
@Getter
public enum ModelGroup {
    BASE("BASE", "基础", "svg/group/base.svg")
    , FOREST("FOREST", "森林", "svg/group/forest.svg")
    , FARM("FARM", "庄园", "svg/group/farm.svg")
    , STALL("STALL", "新村", "svg/group/stall.svg")
    , ORCHARD("ORCHARD", "农场", "svg/group/orchard.svg")
    , SPORTS("SPORTS", "运动", "svg/group/sports.svg")
    , MEMBER("MEMBER", "会员", "svg/group/member.svg")
    , PrivilegeEX("PrivilegeEX", "青春特权", "svg/group/member.svg")
    , NeverLandEX("NeverLandEX", "健康岛", "svg/group/member.svg")
    , OTHER("OTHER", "其他", "svg/group/other.svg")
    , OTHER2("OTHER2", "其他2", "svg/group/other.svg")
    ;
    final String code;
    final String name;
    final String icon;
    ModelGroup(String code, String name, String icon) {
        this.code = code;
        this.name = name;
        this.icon = icon;
    }
    private static final Map<String, ModelGroup> MAP;
    static {
        MAP = new HashMap<>();
        ModelGroup[] values = ModelGroup.values();
        for (ModelGroup value : values) {
            MAP.put(value.code, value);
        }
    }
    public static ModelGroup getByCode(String code) {
        return MAP.get(code);
    }
    public static String getName(String code) {
        ModelGroup modelGroup = getByCode(code);
        if (modelGroup == null) {
            return null;
        }
        return modelGroup.name;
    }
}
