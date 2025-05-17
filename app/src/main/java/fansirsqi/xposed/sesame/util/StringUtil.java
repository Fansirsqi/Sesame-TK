package fansirsqi.xposed.sesame.util;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
public class StringUtil {
    public static boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }
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
    public static String arrayToString(Object... array) {
        return arrayJoinString(",", array);
    }
    private static String toStringOrEmpty(Object obj) {
        return Objects.toString(obj, "");
    }
    public static String padLeft(int str, int totalWidth, char padChar) {
        return padLeft(String.valueOf(str), totalWidth, padChar);
    }
    public static String padRight(int str, int totalWidth, char padChar) {
        return padRight(String.valueOf(str), totalWidth, padChar);
    }
    public static String padLeft(String str, int totalWidth, char padChar) {
        StringBuilder sb = new StringBuilder(str);
        while (sb.length() < totalWidth) {
            sb.insert(0, padChar);
        }
        return sb.toString();
    }
    public static String padRight(String str, int totalWidth, char padChar) {
        StringBuilder sb = new StringBuilder(str);
        while (sb.length() < totalWidth) {
            sb.append(padChar);
        }
        return sb.toString();
    }
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
    //新增
    public static String getJsonString(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder("[{");
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Object value = entry.getValue();
            if (value != null) {
                sb.append("\"").append(entry.getKey()).append("\": ");
                if (value instanceof String) {
                    sb.append("\"").append(value).append("\"");
                } else {
                    sb.append(value);
                }
                sb.append(",");
            }
        }
        if (sb.length() > 2) {
            sb.deleteCharAt(sb.length() - 1);
        }
        sb.append("}]");
        return sb.toString();
    }
}
