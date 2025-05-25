package fansirsqi.xposed.sesame.util.Maps;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import fansirsqi.xposed.sesame.data.CompletedKeyEnum;
import fansirsqi.xposed.sesame.util.Files;
import fansirsqi.xposed.sesame.util.JsonUtil;
import fansirsqi.xposed.sesame.util.Log;
import fansirsqi.xposed.sesame.util.StringUtil;
import fansirsqi.xposed.sesame.util.TimeUtil;

public class Status {
    public static final Status INSTANCE = new Status();
    private static final String TAG = "Status";
    private ArrayList<String> ancientTreeCityCodeList = new ArrayList<>();
    private Set<String> dailyAnswerList = new HashSet<>();
    private Map<CompletedKeyEnum, Object> completedStatus = new HashMap<>();

    protected boolean canEqual(Object obj) {
        return obj instanceof Status;
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof Status status)) {
            return false;
        }
        if (!status.canEqual(this)) {
            return false;
        }
        ArrayList<String> ancientTreeCityCodeList = getAncientTreeCityCodeList();
        ArrayList<String> ancientTreeCityCodeList2 = status.getAncientTreeCityCodeList();
        if (!Objects.equals(ancientTreeCityCodeList, ancientTreeCityCodeList2)) {
            return false;
        }
        Set<String> dailyAnswerList = getDailyAnswerList();
        Set<String> dailyAnswerList2 = status.getDailyAnswerList();
        if (!Objects.equals(dailyAnswerList, dailyAnswerList2)) {
            return false;
        }
        Map<CompletedKeyEnum, Object> completedStatus = getCompletedStatus();
        Map<CompletedKeyEnum, Object> completedStatus2 = status.getCompletedStatus();
        return Objects.equals(completedStatus, completedStatus2);
    }

    public int hashCode() {
        ArrayList<String> ancientTreeCityCodeList = getAncientTreeCityCodeList();
        int hashCode = ancientTreeCityCodeList == null ? 43 : ancientTreeCityCodeList.hashCode();
        Set<String> dailyAnswerList = getDailyAnswerList();
        int hashCode2 = ((hashCode + 59) * 59) + (dailyAnswerList == null ? 43 : dailyAnswerList.hashCode());
        Map<CompletedKeyEnum, Object> completedStatus = getCompletedStatus();
        return (hashCode2 * 59) + (completedStatus != null ? completedStatus.hashCode() : 43);
    }

    public void setAncientTreeCityCodeList(ArrayList<String> arrayList) {
        this.ancientTreeCityCodeList = arrayList;
    }

    public void setCompletedStatus(Map<CompletedKeyEnum, Object> map) {
        this.completedStatus = map;
    }

    public void setDailyAnswerList(Set<String> set) {
        this.dailyAnswerList = set;
    }

    public String toString() {
        return "Status(ancientTreeCityCodeList=" + getAncientTreeCityCodeList() + ", dailyAnswerList=" + getDailyAnswerList() + ", completedStatus=" + getCompletedStatus() + ")";
    }

    public ArrayList<String> getAncientTreeCityCodeList() {
        return this.ancientTreeCityCodeList;
    }

    public Set<String> getDailyAnswerList() {
        return this.dailyAnswerList;
    }

    public Map<CompletedKeyEnum, Object> getCompletedStatus() {
        return this.completedStatus;
    }

    public static synchronized void setCompleted(CompletedKeyEnum completedKeyEnum, Object obj) {
        synchronized (Status.class) {
            INSTANCE.completedStatus.put(completedKeyEnum, obj);
            save();
        }
    }

    public static Object getCompleted(CompletedKeyEnum completedKeyEnum) {
        return INSTANCE.completedStatus.get(completedKeyEnum);
    }

    public static void setCompletedDay(CompletedKeyEnum completedKeyEnum) {
        setCompleted(completedKeyEnum, TimeUtil.getFormattedDate("yyyy-MM-dd"));
    }

    public static boolean getCompletedDay(CompletedKeyEnum completedKeyEnum) {
        return Objects.equals(TimeUtil.getFormattedDate("yyyy-MM-dd"), getCompleted(completedKeyEnum));
    }

    public static int setCompletedDayCount(CompletedKeyEnum completedKeyEnum) {
        String formattedDate = TimeUtil.getFormattedDate("yyyy-MM-dd");
        int completedDays = getCompletedDays(completedKeyEnum, formattedDate, "count") + 1;
        setCompleted(completedKeyEnum, new HashMap<String, Object>() { // from class: leo.xposed.sesameX.util.Status.1
            final /* synthetic */ int val$count;
            final /* synthetic */ String val$day;

            {
                this.val$count = completedDays;
                this.val$day = formattedDate;
                put("count", Integer.valueOf(completedDays));
                put("day", formattedDate);
            }
        });
        return completedDays;
    }

    public static boolean getCompletedDayCount(CompletedKeyEnum completedKeyEnum, int i) {
        return getCompletedDays(completedKeyEnum, TimeUtil.getFormattedDate("yyyy-MM-dd"), "count") >= i;
    }

    private static int getCompletedDays(CompletedKeyEnum completedKeyEnum, String str, String str2) {
        Object completed = getCompleted(completedKeyEnum);
        if (completed instanceof HashMap) {
            HashMap hashMap = (HashMap) completed;
            if (!str.equals(hashMap.get("day"))) {
                return 0;
            }
            Object obj = hashMap.get(str2);
            if (obj instanceof Integer) {
                return ((Integer) obj).intValue();
            }
        }
        return 0;
    }

    public static boolean getCompletedDaysByUser(CompletedKeyEnum completedKeyEnum, String str, int i) {
        return getCompletedDays(completedKeyEnum, TimeUtil.getFormattedDate("yyyy-MM-dd"), str) >= i;
    }

    public static int setCompletedDaysByUser(CompletedKeyEnum completedKeyEnum, String userId, Integer addCount) {
        // 使用泛型明确类型
        HashMap<String, Object> dataMap;
        int finalCount;

        String currentDay = TimeUtil.getFormattedDate("yyyy-MM-dd");
        Object completedData = getCompleted(completedKeyEnum);

        // 安全类型检查和转换
        if (completedData instanceof HashMap) {
            // 添加泛型类型转换注解
            @SuppressWarnings("unchecked")
            HashMap<String, Object> existingData = (HashMap<String, Object>) completedData;

            int currentCount = 0;
            if (!currentDay.equals(existingData.get("day"))) {
                // 创建新日期的数据结构
                dataMap = new HashMap<>();
                dataMap.put("day", currentDay);
            } else {
                // 安全获取并转换计数值
                Object countObj = existingData.get(userId);
                if (countObj instanceof Integer) {
                    currentCount = (Integer) countObj;
                }
            }

            // 计算最终数值
            finalCount = currentCount + (addCount != null ? addCount : 1);
            dataMap = existingData;  // 保持原有引用
            dataMap.put(userId, finalCount);
        } else {
            // 初始化新数据结构
            finalCount = (addCount != null ? addCount : 1);
            dataMap = new HashMap<>();
            dataMap.put(userId, finalCount);
            dataMap.put("day", currentDay);
        }

        setCompleted(completedKeyEnum, dataMap);
        return finalCount;
    }

    public static void setCompletedWeek(CompletedKeyEnum completedKeyEnum, int i) {
        setCompleted(completedKeyEnum, Integer.valueOf(TimeUtil.getWeekNumber(new Date(), i)));
    }

    public static boolean getCompletedWeek(CompletedKeyEnum completedKeyEnum, int i) {
        return Objects.equals(Integer.valueOf(TimeUtil.getWeekNumber(new Date(), i)), getCompleted(completedKeyEnum));
    }

    public static void setCompletedMonth(CompletedKeyEnum completedKeyEnum) {
        setCompleted(completedKeyEnum, TimeUtil.getFormattedDate("yyyy-MM"));
    }

    public static boolean getCompletedMonth(CompletedKeyEnum completedKeyEnum) {
        return Objects.equals(TimeUtil.getFormattedDate("yyyy-MM"), getCompleted(completedKeyEnum));
    }

    public static void setCompletedHours(CompletedKeyEnum completedKeyEnum) {
        setCompleted(completedKeyEnum, TimeUtil.getFormattedDate("yyyy-MM-dd HH"));
    }

    public static boolean getCompletedHours(CompletedKeyEnum completedKeyEnum) {
        return Objects.equals(TimeUtil.getFormattedDate("yyyy-MM-dd HH"), getCompleted(completedKeyEnum));
    }

    public static boolean canAncientTreeToday(String str) {
        return !INSTANCE.ancientTreeCityCodeList.contains(str);
    }

    public static synchronized void ancientTreeToday(String str) {
        synchronized (Status.class) {
            Status status = INSTANCE;
            if (!status.ancientTreeCityCodeList.contains(str)) {
                status.ancientTreeCityCodeList.add(str);
                save();
            }
        }
    }

    public static Set<String> getDadaDailySet() {
        return INSTANCE.dailyAnswerList;
    }

    public static synchronized void setDadaDailySet(Set<String> set) {
        synchronized (Status.class) {
            INSTANCE.dailyAnswerList = set;
            save();
        }
    }

    public static synchronized Status load() throws JsonProcessingException {
        Status status;
        synchronized (Status.class) {
            String currentUid = UserMap.getCurrentUid();
            try {
            } catch (Throwable th) {
                String str = TAG;
                Log.printStackTrace(str, th);
                Log.error(str, "状态文件格式有误，已重置");
                Log.system(str, "状态文件格式有误，已重置");
                try {
                    ObjectMapper copyMapper = JsonUtil.copyMapper();
                    Status status2 = INSTANCE;
                    copyMapper.updateValue(status2, new Status());
                    Files.write2File(JsonUtil.toFormatJsonString(status2), Files.getStatusFile(currentUid));
                } catch (JsonMappingException e) {
                    Log.printStackTrace(TAG, e);
                }
            }
            if (StringUtil.isEmpty(currentUid)) {
                Log.error(TAG, "用户为空，状态加载失败");
                throw new RuntimeException("用户为空，状态加载失败");
            }
            File statusFile = Files.getStatusFile(currentUid);
            if (statusFile.exists()) {
                String readFromFile = Files.readFromFile(statusFile);
                ObjectMapper copyMapper2 = JsonUtil.copyMapper();
                Status status3 = INSTANCE;
                copyMapper2.readerForUpdating(status3).readValue(readFromFile);
                String formatJsonString = JsonUtil.toFormatJsonString(status3);
                if (formatJsonString != null && !formatJsonString.equals(readFromFile)) {
                    String str2 = TAG;
                    Log.error(str2, "重新格式化 status.json");
                    Log.system(str2, "重新格式化 status.json");
                    Files.write2File(formatJsonString, Files.getStatusFile(currentUid));
                }
            } else {
                ObjectMapper copyMapper3 = JsonUtil.copyMapper();
                Status status4 = INSTANCE;
                copyMapper3.updateValue(status4, new Status());
                String str3 = TAG;
                Log.error(str3, "初始化 status.json");
                Log.system(str3, "初始化 status.json");
                Files.write2File(JsonUtil.toFormatJsonString(status4), Files.getStatusFile(currentUid));
            }
            status = INSTANCE;
        }
        return status;
    }

    public static synchronized void save() {
        synchronized (Status.class) {
            save(Calendar.getInstance());
        }
    }

    public static synchronized void save(Calendar calendar) {
        synchronized (Status.class) {
            String currentUid = UserMap.getCurrentUid();
            if (StringUtil.isEmpty(currentUid)) {
                Log.record("用户为空，状态保存失败");
                throw new RuntimeException("用户为空，状态保存失败");
            }
            Log.system(TAG, "保存 status.json");
            try {
                Files.write2File(JsonUtil.toFormatJsonString(INSTANCE), Files.getStatusFile(currentUid));
            } catch (Exception e) {
                throw e;
            }
        }
    }
}
