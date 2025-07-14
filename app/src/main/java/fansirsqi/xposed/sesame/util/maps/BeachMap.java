package fansirsqi.xposed.sesame.util.Maps;
/**
 * 沙滩ID映射工具类。
 * 提供了一个线程安全的ID映射，支持添加、删除、加载和保存ID映射。
 */
public class BeachMap extends IdMapManager {
    @Override
    public String thisFileName() {
        return "BeachMap.json";//海洋ID映射文件
    }
}