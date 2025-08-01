package fansirsqi.xposed.sesame.entity;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import fansirsqi.xposed.sesame.util.Files;
import fansirsqi.xposed.sesame.util.JsonUtil;
import fansirsqi.xposed.sesame.util.Log;
import fansirsqi.xposed.sesame.util.maps.UserMap;
import fansirsqi.xposed.sesame.util.StringUtil;
import fansirsqi.xposed.sesame.util.TimeUtil;
import lombok.Getter;
import lombok.Setter;

@Setter
public class FriendWatch extends MapperEntity {

    @Getter
    private static final String TAG = FriendWatch.class.getSimpleName();

    @Getter
    private static JSONObject joFriendWatch = new JSONObject();

    @Getter
    private String startTime;

    @Getter
    private int allGet;

    @Getter
    private int weekGet;

    public FriendWatch(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public static void setJoFriendWatch(JSONObject joFriendWatch) {
        FriendWatch.joFriendWatch = joFriendWatch;
    }

    @Override
    public int compareTo(MapperEntity o) {
        FriendWatch another = (FriendWatch) o;
        if (this.getWeekGet() > another.getWeekGet()) {
            return -1;
        } else if (this.getWeekGet() < another.getWeekGet()) {
            return 1;
        }
        return super.compareTo(o);
    }

    public static void friendWatch(String id, int collectedEnergy) {
        try {
            if (getJoFriendWatch() == null) {
                setJoFriendWatch(new JSONObject());
            }
            JSONObject joSingle = getJoFriendWatch().optJSONObject(id);
            if (joSingle == null) {
                joSingle = new JSONObject();
                joSingle.put("name", UserMap.getMaskName(id));
                joSingle.put("allGet", 0);
                joSingle.put("startTime", TimeUtil.getDateStr());
                getJoFriendWatch().put(id, joSingle);
            }
            joSingle.put("weekGet", joSingle.optInt("weekGet", 0) + collectedEnergy);
        } catch (Throwable th) {
            Log.runtime(getTAG(), "friendWatch err:");
            Log.printStackTrace(getTAG(), th);
        }
    }

    public static synchronized void save(String userId) {
        try {
            if (getJoFriendWatch() == null) {
                setJoFriendWatch(new JSONObject());
                Log.runtime(getTAG(), "初始化joFriendWatch对象");
            }
            String notformat = getJoFriendWatch().toString();
            String formattedJson = JsonUtil.formatJson(getJoFriendWatch());
            if (formattedJson != null && !formattedJson.trim().isEmpty()) {
                Files.write2File(formattedJson, Files.getFriendWatchFile(userId));
            } else {
                Files.write2File(notformat, Files.getFriendWatchFile(userId));
            }
        } catch (Exception e) {
            Log.runtime(getTAG(), "friendWatch save err:");
            Log.printStackTrace(getTAG(), e);
        }
    }

    public static void updateDay(String userId) {
        if (!needUpdateAll(Files.getFriendWatchFile(userId).lastModified())) {
            return;
        }
        JSONObject joSingle;
        try {
            String dateStr = TimeUtil.getDateStr();
            Iterator<String> ids = getJoFriendWatch().keys();
            while (ids.hasNext()) {
                String id = ids.next();
                joSingle = getJoFriendWatch().getJSONObject(id);
                joSingle.put("name", joSingle.optString("name"));
                joSingle.put("allGet", joSingle.optInt("allGet", 0) + joSingle.optInt("weekGet", 0));
                joSingle.put("weekGet", 0);
                if (!joSingle.has("startTime")) {
                    joSingle.put("startTime", dateStr);
                }
                getJoFriendWatch().put(id, joSingle);
            }
            Files.write2File(getJoFriendWatch().toString(), Files.getFriendWatchFile(userId));
        } catch (Throwable th) {
            Log.runtime(getTAG(), "friendWatchNewWeek err:");
            Log.printStackTrace(getTAG(), th);
        }
    }

    public static synchronized Boolean load(String userId) {
        try {
            if (userId == null) {
                return false;
            }

            String strFriendWatch = Files.readFromFile(Files.getFriendWatchFile(userId));
            if (!strFriendWatch.isEmpty()) {
                setJoFriendWatch(new JSONObject(strFriendWatch));
            } else {
                setJoFriendWatch(new JSONObject());
            }
            return true;
        } catch (JSONException e) {
            Log.printStackTrace(e);
            setJoFriendWatch(new JSONObject());
        }
        return false;
    }

    public static synchronized void unload() {
        setJoFriendWatch(new JSONObject());
    }

    public static boolean needUpdateAll(long last) {
        if (last == 0L) {
            return true;
        }
        Calendar cLast = Calendar.getInstance();
        cLast.setTimeInMillis(last);
        Calendar cNow = Calendar.getInstance();
        if (cLast.get(Calendar.DAY_OF_YEAR) == cNow.get(Calendar.DAY_OF_YEAR)) {
            return false;
        }
        return cNow.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY;
    }

    public static List<FriendWatch> getList(String userId) {
        ArrayList<FriendWatch> list = new ArrayList<>();
        String strFriendWatch = Files.readFromFile(Files.getFriendWatchFile(userId));
        try {
            JSONObject joFriendWatch;
            if (StringUtil.isEmpty(strFriendWatch)) {
                joFriendWatch = new JSONObject();
            } else {
                joFriendWatch = new JSONObject(strFriendWatch);
            }
            Iterator<String> ids = joFriendWatch.keys();
            while (ids.hasNext()) {
                String id = ids.next();
                JSONObject friend = joFriendWatch.optJSONObject(id);
                if (friend == null) {
                    friend = new JSONObject();
                }
                String name = friend.optString("name");
                FriendWatch friendWatch = new FriendWatch(id, name);
                friendWatch.setStartTime(friend.optString("startTime", "无"));
                friendWatch.setWeekGet(friend.optInt("weekGet", 0));
                friendWatch.setAllGet(friend.optInt("allGet", 0) + friendWatch.getWeekGet());
                friendWatch.name = name + "(开始统计时间:" + friendWatch.getStartTime() + ")\n\n" + "周收:" + friendWatch.getWeekGet() + " 总收:" + friendWatch.getAllGet();
                list.add(friendWatch);
            }
        } catch (Throwable t) {
            Log.runtime(getTAG(), "FriendWatch getList: ");
            Log.printStackTrace(getTAG(), t);
            try {
                Files.write2File(new JSONObject().toString(), Files.getFriendWatchFile(userId));
            } catch (Exception e) {
                Log.printStackTrace(e);
            }
        }
        return list;
    }

}