package fansirsqi.xposed.sesame.entity;
import lombok.Data;
import java.io.Serializable;
/**
 * 通用键值对节点类。
 * 支持泛型类型，用于存储键值对信息，并支持序列化。
 *
 * @param <K> 键的类型
 * @param <V> 值的类型
 */
@Data
public class KVNode<K, V> implements Serializable {
    private static final long serialVersionUID = 1L; // 序列化版本号
    private K key; // 键
    private V value; // 值
    /**
     * 无参构造方法，用于序列化或其他框架调用。
     */
    public KVNode() {
    }
    /**
     * 全参构造方法，初始化键值对。
     *
     * @param key   键
     * @param value 值
     */
    public KVNode(K key, V value) {
        this.key = key;
        this.value = value;
    }
}
