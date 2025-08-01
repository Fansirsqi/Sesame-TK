package fansirsqi.xposed.sesame.util;

import java.util.Random;

/**
 * 随机数工具类，提供生成随机数和随机字符串的方法。
 */
public class RandomUtil {
    private static final Random rnd = new Random();

    /**
     * 生成一个随机延迟时间（100到300毫秒之间）。
     *
     * @return 生成的随机延迟时间（毫秒）。
     */
    public static int delay() {
        return nextInt(100, 300);
    }

    /**
     * 生成一个指定范围内的随机整数。
     *
     * @param min 最小值（包含）。
     * @param max 最大值（不包含）。
     * @return 生成的随机整数。
     */
    public static int nextInt(int min, int max) {
        if (min >= max) return min;
        return rnd.nextInt(max - min) + min;
    }

    /**
     * 生成一个随机的长整数。
     *
     * @return 生成的随机长整数。
     */
    public static long nextLong() {
        return rnd.nextLong();
    }

    /**
     * 生成一个指定范围内的随机长整数。
     *
     * @param min 最小值（包含）。
     * @param max 最大值（不包含）。
     * @return 生成的随机长整数。
     */
    public static long nextLong(long min, long max) {
        if (min >= max) return min;
        long o = max - min;
        return (rnd.nextLong() % o) + min;
    }

    /**
     * 生成一个随机的双精度浮点数。
     *
     * @return 生成的随机双精度浮点数。
     */
    public static double nextDouble() {
        return rnd.nextDouble();
    }

    /**
     * 生成一个指定长度的随机数字字符串。
     *
     * @param len 随机字符串的长度。
     * @return 生成的随机数字字符串。
     */
    public static String getRandomInt(int len) {
        StringBuilder rs = new StringBuilder();
        for (int i = 0; i < len; i++) {
            rs.append(rnd.nextInt(10));
        }
        return rs.toString();
    }

    /**
     * 生成一个指定长度的随机字符串，包含小写字母和数字。
     *
     * @param length 随机字符串的长度。
     * @return 生成的随机字符串。
     */
    public static String getRandomString(int length) {
        String chars = "abcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int number = nextInt(0, chars.length());
            sb.append(chars.charAt(number));
        }
        return sb.toString();
    }

    public static String getRandomTag() {
        return "_" + System.currentTimeMillis() + "_" + RandomUtil.getRandomString(8);
    }


}