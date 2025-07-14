package fansirsqi.xposed.sesame.entity;
import fansirsqi.xposed.sesame.util.Log;
import fansirsqi.xposed.sesame.util.Maps.UserMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
/**
 * 表示支付宝用户的实体类，包含 ID 和名称。
 */
public class AlipayUser extends MapperEntity {
    /**
     * 构造方法，根据给定的 ID 和名称初始化用户对象。
     * @param i 用户的 ID
     * @param n 用户的名称
     */
    public AlipayUser(String i, String n) {
        id = i;
        name = n;
    }
    /**
     * 获取所有用户的列表，不使用任何过滤器。
     * @return 包含所有符合条件的 AlipayUser 对象的列表
     */
    public static List<AlipayUser> getList() {
        return getList(user -> true); // 默认不过滤
    }
    /**
     * 获取符合过滤条件的用户列表。
     * @param filterFunc 过滤函数，用于筛选用户
     * @return 符合条件的 AlipayUser 对象列表
     */
    public static List<AlipayUser> getList(Filter filterFunc) {
        List<AlipayUser> list = new ArrayList<>();
        Map<String, UserEntity> userIdMap = UserMap.getUserMap();
        for (Map.Entry<String, UserEntity> entry : userIdMap.entrySet()) {
            UserEntity userEntity = entry.getValue();
            try {
                // 使用过滤器判断是否添加用户
                if (filterFunc.apply(userEntity)) {
                    list.add(new AlipayUser(entry.getKey(), userEntity.getFullName()));
                }
            } catch (Throwable t) {
                Log.printStackTrace(t); // 捕获并记录异常
            }
        }
        return list;
    }
    /**
     * 过滤接口，用于筛选符合条件的用户。
     */
    public interface Filter {
        /**
         * 判断给定用户是否符合条件。
         * @param user 用户实体对象
         * @return 符合条件返回 true，否则返回 false
         */
        Boolean apply(UserEntity user);
    }
}
