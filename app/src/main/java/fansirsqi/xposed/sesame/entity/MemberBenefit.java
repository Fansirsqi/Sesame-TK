package fansirsqi.xposed.sesame.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fansirsqi.xposed.sesame.util.Maps.IdMapManager;
import fansirsqi.xposed.sesame.util.Maps.MemberBenefitsMap;

public class MemberBenefit extends MapperEntity {

    public MemberBenefit(String i, String n) {
        id = i;
        name = n;
    }

    public static List<MemberBenefit> getList() {
        List<MemberBenefit> list = new ArrayList<>();
        Map<String, String> idSet = IdMapManager.getInstance(MemberBenefitsMap.class).getMap();
        for (Map.Entry<String, String> entry: idSet.entrySet()) {
            list.add(new MemberBenefit(entry.getKey(), entry.getValue()));
        }
        return list;
    }
}