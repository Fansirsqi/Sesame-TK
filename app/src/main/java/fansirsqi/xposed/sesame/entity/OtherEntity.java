package fansirsqi.xposed.sesame.entity;

import java.util.ArrayList;
import java.util.List;

public class OtherEntity extends ForestEntity {
    public OtherEntity(String i, String n) {
        super(i, n);
    }

    public static List<OtherEntity> listEcoLifeOptions() {
        List<OtherEntity> list = new ArrayList<>();
        list.add(new OtherEntity("tick", "绿色行动"));
        list.add(new OtherEntity("plate", "光盘行动"));
        return list;
    }

    public static List<OtherEntity> listHealthcareOptions() {
        List<OtherEntity> list = new ArrayList<>();
        list.add(new OtherEntity("FEEDS", "绿色医疗💉"));
        list.add(new OtherEntity("BILL", "电子小票🎫"));
        return list;
    }


}
