package fansirsqi.xposed.sesame.util;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
/** 列表工具类，提供对列表的常用操作。 */
public class ListUtil {
  /**
   * 创建一个新的ArrayList实例，并使用提供的元素进行初始化。 这是一个泛型方法，可以用于创建并初始化任何类型的列表。
   *
   * @param objects 要添加到列表中的元素。
   * @param <T> 列表元素的类型。
   * @return 返回包含所有提供元素的新ArrayList。
   */
  @SafeVarargs
  public static <T> List<T> newArrayList(T... objects) {
    // 创建一个新的ArrayList实例
    List<T> list = new ArrayList<>();
    // 如果提供了元素，则将它们添加到列表中
    if (objects != null) {
      Collections.addAll(list, objects);
    }
    return list;
  }
}
