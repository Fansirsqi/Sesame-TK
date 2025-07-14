package fansirsqi.xposed.sesame.entity;
import fansirsqi.xposed.sesame.util.Maps.BeachMap;
import fansirsqi.xposed.sesame.util.Maps.IdMapManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
/**
 * 表示支付宝海滩的实体类，包含 ID 和名称。
 */
public class AlipayBeach extends MapperEntity {
    // 使用 volatile 关键字确保多线程环境下的可见性
    private static volatile List<AlipayBeach> list;
    /**
     * 构造方法，根据给定的 ID 和名称初始化对象。
     * @param i 海滩的 ID
     * @param n 海滩的名称
     */
    public AlipayBeach(String i, String n) {
        id = i;
        name = n;
    }
    /**
     * 获取包含所有海滩的列表，首次调用时从 BeachMap 初始化。
     * 使用双重检查锁定机制实现懒加载以提高性能。
     * @return 包含所有 AlipayBeach 对象的不可变列表
     */
    public static List<AlipayBeach> getList() {
        if (list == null) {
            synchronized (AlipayBeach.class) {
                if (list == null) {
                    List<AlipayBeach> tempList = new ArrayList<>();
                    for (Map.Entry<String, String> entry : IdMapManager.getInstance(BeachMap.class).getMap().entrySet()) {
                        tempList.add(new AlipayBeach(entry.getKey(), entry.getValue()));
                    }
                    list = Collections.unmodifiableList(tempList);
                }
            }
        }
        return list;
    }
    /**
     * 根据给定的 ID 删除相应的 AlipayBeach 对象。
     * 首次调用 getList 方法以确保列表已初始化。
     * @param id 要删除的海滩 ID
     */
    public static void remove(String id) {
        getList();
        synchronized (AlipayBeach.class) {
            list = new ArrayList<>(list); // 创建可变列表的副本
            list.removeIf(beach -> beach.id.equals(id)); // 使用流简化移除操作
            list = Collections.unmodifiableList(list); // 确保返回不可变列表
        }
    }
}
