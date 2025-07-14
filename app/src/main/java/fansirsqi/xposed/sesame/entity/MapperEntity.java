package fansirsqi.xposed.sesame.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.List;

import fansirsqi.xposed.sesame.util.HanziToPinyin;
import lombok.Data;

/**
 * 抽象基类，表示具有 ID 和名称的实体对象。
 * 提供按拼音排序的能力。
 */
@Data
public abstract class MapperEntity implements Comparable<MapperEntity> {
     public String name; // 实体名称
    public String id; // 实体 ID
    // 拼音缓存，用于提升多次比较时的性能
    private ArrayList<String> pinyin;
    /**
     * 获取名称的拼音列表（按汉字分解）。
     * 使用缓存避免重复计算。
     *
     * @return 名称的拼音列表
     */
    @JsonIgnore
    public ArrayList<String> getPinyin() {
        if (pinyin != null) {
            return pinyin;
        }
        // 将名称转换为拼音
        ArrayList<HanziToPinyin.Token> tokens = HanziToPinyin.getInstance().get(name);
        pinyin = new ArrayList<>(tokens.size());
        for (HanziToPinyin.Token token : tokens) {
            pinyin.add(token.target);
        }
        return pinyin;
    }
    /**
     * 按拼音顺序比较两个对象，用于排序。
     *
     * @param other 另一个 IdAndName 对象
     * @return 比较结果：负数表示小于，0 表示相等，正数表示大于
     */
    @Override
    public int compareTo(MapperEntity other) {
        List<String> list1 = this.getPinyin();
        List<String> list2 = other.getPinyin();
        int index = 0;
        while (index < list1.size() && index < list2.size()) {
            int compareResult = list1.get(index).compareTo(list2.get(index));
            if (compareResult != 0) {
                return compareResult;
            }
            index++;
        }
        // 如果前缀相同，长度短的优先
        return list1.size() - list2.size();
    }
}
