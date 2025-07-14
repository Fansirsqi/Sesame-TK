package fansirsqi.xposed.sesame.entity;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import fansirsqi.xposed.sesame.util.Maps.IdMapManager;
import fansirsqi.xposed.sesame.util.Maps.ReserveaMap;
/**
 * 表示支付宝保留项的实体类，包含 ID 和名称。
 */
public class ReserveEntity extends MapperEntity {
    // 使用 volatile 关键字确保多线程环境下的可见性
    private static volatile List<ReserveEntity> list;
    /**
     * 构造方法，根据给定的 ID 和名称初始化对象。
     * @param i 保留项的 ID
     * @param n 保留项的名称
     */
    public ReserveEntity(String i, String n) {
        id = i;
        name = n;
    }
    /**
     * 获取包含所有保留项的列表，首次调用时从 ReserveIdMapUtil 初始化。
     * 使用双重检查锁定机制实现懒加载以提高性能。
     * @return 包含所有 ReserveEntity 对象的不可变列表
     */
    public static List<ReserveEntity> getList() {
        if (list == null) {
            synchronized (ReserveEntity.class) {
                if (list == null) {
                    List<ReserveEntity> tempList = new ArrayList<>();
                    Set<Map.Entry<String, String>> idSet = IdMapManager.getInstance(ReserveaMap.class).getMap().entrySet();
                    for (Map.Entry<String, String> entry : idSet) {
                        tempList.add(new ReserveEntity(entry.getKey(), entry.getValue()));
                    }
                    list = Collections.unmodifiableList(tempList);
                }
            }
        }
        return list;
    }
    /**
     * 根据给定的 ID 删除相应的 ReserveEntity 对象。
     * 首次调用 getList 方法以确保列表已初始化。
     * @param id 要删除的保留项 ID
     */
    public static void remove(String id) {
        getList();
        synchronized (ReserveEntity.class) {
            List<ReserveEntity> tempList = new ArrayList<>(list); // 创建可变列表的副本
            Iterator<ReserveEntity> iterator = tempList.iterator();
            while (iterator.hasNext()) {
                ReserveEntity reserve = iterator.next();
                if (reserve.id.equals(id)) {
                    iterator.remove();
                }
            }
            list = Collections.unmodifiableList(tempList); // 确保返回不可变列表
        }
    }
}
