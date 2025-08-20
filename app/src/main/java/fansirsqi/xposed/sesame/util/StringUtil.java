package fansirsqi.xposed.sesame.util;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

/**
 * 字符串工具类，提供字符串处理的常用方法
 */
public class StringUtil {
    /**
     * 检查字符串是否为空
     *
     * @param str 要检查的字符串
     * @return 如果字符串为null或空字符串则返回true
     */
    public static boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }

    /**
     * 将集合中的元素用指定分隔符连接成字符串
     *
     * @param conjunction 分隔符
     * @param collection  要连接的集合
     * @return 连接后的字符串
     */
    public static String collectionJoinString(CharSequence conjunction, Collection<?> collection) {
        if (!collection.isEmpty()) {
            StringBuilder b = new StringBuilder();
            Iterator<?> iterator = collection.iterator();
            b.append(toStringOrEmpty(iterator.next()));
            while (iterator.hasNext()) {
                b.append(conjunction).append(toStringOrEmpty(iterator.next()));
            }
            return b.toString();
        }
        return "";
    }

    /**
     * 将数组中的元素用指定分隔符连接成字符串
     *
     * @param conjunction 分隔符
     * @param array       要连接的数组
     * @return 连接后的字符串
     */
    public static String arrayJoinString(CharSequence conjunction, Object... array) {
        int length = array.length;
        if (length > 0) {
            StringBuilder b = new StringBuilder();
            b.append(toStringOrEmpty(array[0]));
            for (int i = 1; i < length; i++) {
                b.append(conjunction).append(toStringOrEmpty(array[i]));
            }
            return b.toString();
        }
        return "";
    }

    /**
     * 将数组转换为字符串，元素间用逗号分隔
     *
     * @param array 要转换的数组
     * @return 转换后的字符串
     */
    public static String arrayToString(Object... array) {
        return arrayJoinString(",", array);
    }

    /**
     * 将对象转换为字符串，如果为null则返回空字符串
     *
     * @param obj 要转换的对象
     * @return 转换后的字符串
     */
    private static String toStringOrEmpty(Object obj) {
        return Objects.toString(obj, "");
    }

    /**
     * 在整数左侧填充指定字符到指定长度
     *
     * @param str        要填充的整数
     * @param totalWidth 目标总长度
     * @param padChar    填充字符
     * @return 填充后的字符串
     */
    public static String padLeft(int str, int totalWidth, char padChar) {
        return padLeft(String.valueOf(str), totalWidth, padChar);
    }

    /**
     * 在整数右侧填充指定字符到指定长度
     *
     * @param str        要填充的整数
     * @param totalWidth 目标总长度
     * @param padChar    填充字符
     * @return 填充后的字符串
     */
    public static String padRight(int str, int totalWidth, char padChar) {
        return padRight(String.valueOf(str), totalWidth, padChar);
    }

    /**
     * 在字符串左侧填充指定字符到指定长度
     *
     * @param str        要填充的字符串
     * @param totalWidth 目标总长度
     * @param padChar    填充字符
     * @return 填充后的字符串
     */
    public static String padLeft(String str, int totalWidth, char padChar) {
        StringBuilder sb = new StringBuilder(str);
        while (sb.length() < totalWidth) {
            sb.insert(0, padChar);
        }
        return sb.toString();
    }

    /**
     * 在字符串右侧填充指定字符到指定长度
     *
     * @param str        要填充的字符串
     * @param totalWidth 目标总长度
     * @param padChar    填充字符
     * @return 填充后的字符串
     */
    public static String padRight(String str, int totalWidth, char padChar) {
        StringBuilder sb = new StringBuilder(str);
        while (sb.length() < totalWidth) {
            sb.append(padChar);
        }
        return sb.toString();
    }

    /**
     * 提取字符串中两个指定字符串之间的内容
     *
     * @param text  源字符串
     * @param left  左侧边界字符串
     * @param right 右侧边界字符串
     * @return 提取的子字符串
     */
    public static String getSubString(String text, String left, String right) {
        String result = "";
        int zLen;
        if (left == null || left.isEmpty()) {
            zLen = 0;
        } else {
            zLen = text.indexOf(left);
            if (zLen > -1) {
                zLen += left.length();
            } else {
                zLen = 0;
            }
        }
        int yLen = text.indexOf(right, zLen);
        if (yLen < 0 || right == null || right.isEmpty()) {
            yLen = text.length();
        }
        result = text.substring(zLen, yLen);
        return result;
    }
}
