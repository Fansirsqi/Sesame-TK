package fansirsqi.xposed.sesame.util;

import android.annotation.SuppressLint;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 时间工具类。 提供了一系列方法来处理时间相关的操作，包括时间范围检查、时间比较、日期格式化等。
 */
public class TimeUtil {
    public static Boolean checkNowInTimeRange(String timeRange) {
        return checkInTimeRange(System.currentTimeMillis(), timeRange);
    }

    public static Boolean checkInTimeRange(Long timeMillis, List<String> timeRangeList) {
        for (String timeRange : timeRangeList) {
            if (checkInTimeRange(timeMillis, timeRange)) {
                return true;
            }
        }
        return false;
    }

    public static Boolean checkInTimeRange(Long timeMillis, String timeRange) {
        try {
            String[] timeRangeArray = timeRange.split("-");
            if (timeRangeArray.length == 2) {
                String min = timeRangeArray[0];
                String max = timeRangeArray[1];
                return isAfterOrCompareTimeStr(timeMillis, min) && isBeforeOrCompareTimeStr(timeMillis, max);
            }
        } catch (Exception e) {
            Log.printStackTrace(e);
        }
        return false;
    }

    public static Boolean isNowBeforeTimeStr(String beforeTimeStr) {
        return isBeforeTimeStr(System.currentTimeMillis(), beforeTimeStr);
    }

    public static Boolean isNowAfterTimeStr(String afterTimeStr) {
        return isAfterTimeStr(System.currentTimeMillis(), afterTimeStr);
    }

    public static Boolean isNowBeforeOrCompareTimeStr(String beforeTimeStr) {
        return isBeforeOrCompareTimeStr(System.currentTimeMillis(), beforeTimeStr);
    }

    public static Boolean isNowAfterOrCompareTimeStr(String afterTimeStr) {
        return isAfterOrCompareTimeStr(System.currentTimeMillis(), afterTimeStr);
    }

    public static Boolean isBeforeTimeStr(Long timeMillis, String beforeTimeStr) {
        Integer compared = isCompareTimeStr(timeMillis, beforeTimeStr);
        if (compared != null) {
            return compared < 0;
        }
        return false;
    }

    public static Boolean isAfterTimeStr(Long timeMillis, String afterTimeStr) {
        Integer compared = isCompareTimeStr(timeMillis, afterTimeStr);
        if (compared != null) {
            return compared > 0;
        }
        return false;
    }

    public static Boolean isBeforeOrCompareTimeStr(Long timeMillis, String beforeTimeStr) {
        Integer compared = isCompareTimeStr(timeMillis, beforeTimeStr);
        if (compared != null) {
            return compared <= 0;
        }
        return false;
    }

    public static Boolean isAfterOrCompareTimeStr(Long timeMillis, String afterTimeStr) {
        Integer compared = isCompareTimeStr(timeMillis, afterTimeStr);
        if (compared != null) {
            return compared >= 0;
        }
        return false;
    }

    public static Integer isCompareTimeStr(Long timeMillis, String compareTimeStr) {
        try {
            Calendar timeCalendar = Calendar.getInstance();
            timeCalendar.setTimeInMillis(timeMillis);
            Calendar compareCalendar = getTodayCalendarByTimeStr(compareTimeStr);
            if (compareCalendar != null) {
                return timeCalendar.compareTo(compareCalendar);
            }
        } catch (Exception e) {
            Log.printStackTrace(e);
        }
        return null;
    }

    public static Calendar getTodayCalendarByTimeStr(String timeStr) {
        return getCalendarByTimeStr((Long) null, timeStr);
    }

    public static Calendar getCalendarByTimeStr(Long timeMillis, String timeStr) {
        try {
            Calendar timeCalendar = getCalendarByTimeMillis(timeMillis);
            return getCalendarByTimeStr(timeCalendar, timeStr);
        } catch (Exception e) {
            Log.printStackTrace(e);
        }
        return null;
    }

    public static Calendar getCalendarByTimeStr(Calendar timeCalendar, String timeStr) {
        try {
            int length = timeStr.length();
            switch (length) {
                case 6:
                    timeCalendar.set(Calendar.SECOND, Integer.parseInt(timeStr.substring(4)));
                    timeCalendar.set(Calendar.MINUTE, Integer.parseInt(timeStr.substring(2, 4)));
                    timeCalendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(timeStr.substring(0, 2)));
                    break;
                case 4:
                    timeCalendar.set(Calendar.SECOND, 0);
                    timeCalendar.set(Calendar.MINUTE, Integer.parseInt(timeStr.substring(2, 4)));
                    timeCalendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(timeStr.substring(0, 2)));
                    break;
                case 2:
                    timeCalendar.set(Calendar.SECOND, 0);
                    timeCalendar.set(Calendar.MINUTE, 0);
                    timeCalendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(timeStr.substring(0, 2)));
                    break;
                default:
                    return null;
            }
            timeCalendar.set(Calendar.MILLISECOND, 0);
            return timeCalendar;
        } catch (Exception e) {
            Log.printStackTrace(e);
        }
        return null;
    }

    public static Calendar getCalendarByTimeMillis(Long timeMillis) {
        Calendar timeCalendar = Calendar.getInstance();
        if (timeMillis != null) {
            timeCalendar.setTimeInMillis(timeMillis);
        }
        return timeCalendar;
    }

    /**
     * 获取当前时间的字符串表示
     *
     * @param ts 时间戳
     * @return "下午8:00:00"（东八区）或 "8:00:00 PM"（英语环境）
     */
    public static String getTimeStr(long ts) {
        return DateFormat.getTimeInstance().format(new Date(ts));
    }


    /**
     * 获取当前时间的字符串表示
     *
     * @return "下午8:00:00"（东八区）或 "8:00:00 PM"（英语环境）
     */
    public static String getTimeStr() {
        return getTimeStr(System.currentTimeMillis());
    }

    /**
     * 获取当前日期的字符串表示
     *
     * @return 格式：yyyy年*M月*d日
     */
    public static String getDateStr() {
        return getDateStr(0);
    }


    /**
     * 获取日期的字符串表示
     *
     * @param plusDay 日期偏移量
     * @return 格式：yyyy年*M月*d日
     */
    public static String getDateStr(int plusDay) {
        Calendar c = Calendar.getInstance();
        if (plusDay != 0) {
            c.add(Calendar.DATE, plusDay);
        }
        return DateFormat.getDateInstance().format(c.getTime());
    }

    /**
     * 默认获取今天
     *
     * @return yyyy-MM-dd
     */
    public static String getDateStr2() {
        return getDateStr2(0);
    }

    /**
     * 默认获取今天
     *
     * @param plusDay 日期偏移量
     * @return yyyy-MM-dd
     */
    public static String getDateStr2(int plusDay) {
        Calendar c = Calendar.getInstance();
        if (plusDay != 0) {
            c.add(Calendar.DATE, plusDay);
        }
        Date date = c.getTime();

        // 使用固定格式 yyyy-MM-dd
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(date);
    }

    public static Calendar getToday() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c;
    }

    public static Calendar getNow() {
        return Calendar.getInstance();
    }

    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取指定时间的周数
     *
     * @param dateTime 时间
     * @return 当前年的第几周
     */
    public static int getWeekNumber(Date dateTime) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(dateTime);
        // 设置周的第一天为周一
        calendar.setFirstDayOfWeek(Calendar.MONDAY);
        return calendar.get(Calendar.WEEK_OF_YEAR);
    }

    /**
     * 比较第一个日历的天数小于第二个日历的天数
     *
     * @param firstCalendar  第一个日历
     * @param secondCalendar 第二个日历
     * @return Boolean 如果小于，则为true，否则为false
     */
    public static Boolean isLessThanSecondOfDays(Calendar firstCalendar, Calendar secondCalendar) {
        return (firstCalendar.get(Calendar.YEAR) < secondCalendar.get(Calendar.YEAR))
                || (firstCalendar.get(Calendar.YEAR) == secondCalendar.get(Calendar.YEAR)
                && firstCalendar.get(Calendar.DAY_OF_YEAR) < secondCalendar.get(Calendar.DAY_OF_YEAR));
    }

    /**
     * 比较第一个时间戳的天数是否小于第二个时间戳的天数
     *
     * @param firstTimestamp  第一个时间戳
     * @param secondTimestamp 第二个时间戳
     * @return Boolean 如果小于，则为true，否则为false
     */
    public static Boolean isLessThanSecondOfDays(Long firstTimestamp, Long secondTimestamp) {
        Calendar firstCalendar = getCalendarByTimeMillis(firstTimestamp);
        Calendar secondCalendar = getCalendarByTimeMillis(secondTimestamp);
        return isLessThanSecondOfDays(firstCalendar, secondCalendar);
    }

    /**
     * 通过时间戳比较传入的时间戳的天数是否小于当前时间戳的天数
     *
     * @param timestamp 时间戳
     * @return Boolean 如果小于当前时间戳所计算的天数，则为true，否则为false
     */
    public static Boolean isLessThanNowOfDays(Long timestamp) {
        return isLessThanSecondOfDays(getCalendarByTimeMillis(timestamp), getNow());
    }

    /**
     * 判断两个日历对象是否为同一天
     *
     * @param firstCalendar  第一个日历对象
     * @param secondCalendar 第二个日历对象
     * @return 两个日历对象是否为同一天
     */
    public static Boolean isSameDay(Calendar firstCalendar, Calendar secondCalendar) {
        return firstCalendar.get(Calendar.YEAR) == secondCalendar.get(Calendar.YEAR)
                && firstCalendar.get(Calendar.DAY_OF_YEAR) == secondCalendar.get(Calendar.DAY_OF_YEAR);
    }

    /**
     * 判断两个时间戳是否为同一天
     *
     * @param firstTimestamp  第一个时间戳
     * @param secondTimestamp 第二个时间戳
     * @return 两个时间戳是否为同一天
     */
    public static Boolean isSameDay(Long firstTimestamp, Long secondTimestamp) {
        Calendar firstCalendar = getCalendarByTimeMillis(firstTimestamp);
        Calendar secondCalendar = getCalendarByTimeMillis(secondTimestamp);
        return isSameDay(firstCalendar, secondCalendar);
    }

    /**
     * 判断日历对象是否为今天
     *
     * @param calendar 日历对象
     * @return 日历对象是否为今天
     */
    public static Boolean isToday(Calendar calendar) {
        return isSameDay(getToday(), calendar);
    }

    /**
     * 判断时间戳是否为今天
     *
     * @param timestamp 时间戳
     * @return 时间戳是否为今天
     */
    public static Boolean isToday(Long timestamp) {
        return isToday(getCalendarByTimeMillis(timestamp));
    }

    @SuppressLint("SimpleDateFormat")
    public static DateFormat getCommonDateFormat() {
        return new SimpleDateFormat("dd日HH:mm:ss");
    }

    @SuppressLint("SimpleDateFormat")
    public static String getCommonDate(Long timestamp) {
        return getCommonDateFormat().format(timestamp);
    }


    public static final ThreadLocal<SimpleDateFormat> DATE_TIME_FORMAT_THREAD_LOCAL = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        }
    };

    public static final ThreadLocal<SimpleDateFormat> OTHER_DATE_TIME_FORMAT_THREAD_LOCAL = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("yyyy.MM.dd HH:mm:ss", Locale.getDefault());
        }
    };

    public static long timeToStamp(String timers) {
        Date d = new Date();
        long timeStemp;
        try {
            SimpleDateFormat simpleDateFormat = OTHER_DATE_TIME_FORMAT_THREAD_LOCAL.get();
            if (simpleDateFormat == null) {
                simpleDateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss", Locale.getDefault());
            }
            Date newD = simpleDateFormat.parse(timers);
            if (newD != null) {
                d = newD;
            }
        } catch (ParseException ignored) {
        }
        timeStemp = d.getTime();
        return timeStemp;
    }

    /**
     * 获取格式化的日期 时间字符串yyyy-MM-dd HH:mm:ss
     *
     * @return
     */
    public static String getFormatDateTime() {
        SimpleDateFormat simpleDateFormat = DATE_TIME_FORMAT_THREAD_LOCAL.get();
        if (simpleDateFormat == null) {
            simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss", Locale.getDefault());
        }
        return simpleDateFormat.format(new Date());
    }


    /**
     * 获取格式化的日期符串yyyy-MM-dd
     *
     * @return
     */
    public static String getFormatDate() {
        return getFormatDateTime().split(" ")[0];
    }

    /**
     * 获取格式化的时间字符串HH:mm:ss
     *
     * @return
     */
    public static String getFormatTime() {
        return getFormatDateTime().split(" ")[1];
    }

    /**
     * 根据传入的格式化字符串获取格式化后的时间字符串
     *
     * @param offset 日期偏移量
     * @param format 格式化字符串
     * @return 格式化后的时间字符串
     */
    public static String getFormatTime(int offset, String format) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, offset);
        @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat(format);
        return sdf.format(calendar.getTime());
    }
}
