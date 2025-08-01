package fansirsqi.xposed.sesame.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import fansirsqi.xposed.sesame.util.maps.IdMapManager;
import fansirsqi.xposed.sesame.util.maps.ParadiseCoinBenefitIdMap;

public class ParadiseCoinBenefit extends MapperEntity {

    public ParadiseCoinBenefit(String i, String n) {
        id = i;
        name = n;
    }

    public static List<ParadiseCoinBenefit> getList() {
        List<ParadiseCoinBenefit> list = new ArrayList<>();
        Map<String, String> idSet = IdMapManager.getInstance(ParadiseCoinBenefitIdMap.class).getMap();
        for (Map.Entry<String, String> entry: idSet.entrySet()) {
            list.add(new ParadiseCoinBenefit(entry.getKey(), entry.getValue()));
        }
        return list;
    }
}
