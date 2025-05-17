package fansirsqi.xposed.sesame.entity;
import java.util.Arrays;
import java.util.List;
public class EcoLifeEntity extends ForestEntity {
    public EcoLifeEntity(String i, String n) {
        super(i, n);
    }
    public static final EcoLifeEntity ECO_LIFE_TICK = new EcoLifeEntity("tick", "绿色行动打卡");
    public static final EcoLifeEntity ECO_LIFE_DISH = new EcoLifeEntity("plate", "光盘行动打卡");
    public static List<EcoLifeEntity> listEcoLifeOptions() {
        return Arrays.asList(ECO_LIFE_TICK, ECO_LIFE_DISH);
    }
}
