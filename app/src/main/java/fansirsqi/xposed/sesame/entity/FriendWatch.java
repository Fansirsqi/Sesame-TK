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
import fansirsqi.xposed.sesame.util.Maps.UserMap;
import fansirsqi.xposed.sesame.util.StringUtil;
import fansirsqi.xposed.sesame.util.TimeUtil;

public class FriendWatch extends MapperEntity {

    private static final String TAG = FriendWatch.class.getSimpleName();

    private static JSONObject joFriendWatch= new JSONObject();

    private String startTime;

    private int allGet;

    private int weekGet;

    public FriendWatch(String id, String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public int compareTo(MapperEntity o) {
        FriendWatch another = (FriendWatch) o;
        if (this.weekGet > another.weekGet) {
            return -1;
        } else if (this.weekGet < another.weekGet) {
            return 1;
        }
        return super.compareTo(o);
    }

    public static void friendWatch(String id, int collectedEnergy) {
        try {
            if (joFriendWatch == null) {
            joFriendWatch = new JSONObject();
        }
            JSONObject joSingle = joFriendWatch.optJSONObject(id);
            if (joSingle == null) {
                joSingle = new JSONObject();
                joSingle.put("name", UserMap.getMaskName(id));
                joSingle.put("allGet", 0);
                joSingle.put("startTime", TimeUtil.getDateStr());
                joFriendWatch.put(id, joSingle);
            }
            joSingle.put("weekGet", joSingle.optInt("weekGet", 0) + collectedEnergy);
        } catch (Throwable th) {
            Log.runtime(TAG, "friendWatch err:");
            Log.printStackTrace(TAG, th);
        }
    }

    public static synchronized void save(String userId) {
        try {
            if (joFriendWatch == null) {
            joFriendWatch = new JSONObject();
            Log.runtime(TAG, "初始化joFriendWatch对象");
        }
            String notformat = joFriendWatch.toString();
            String formattedJson = JsonUtil.formatJson(joFriendWatch);
//            Log.debug(TAG, "friendWatch save: " + formattedJson + " " + notformat);
            if (formattedJson != null && !formattedJson.trim().isEmpty()) {
                Files.write2File(formattedJson, Files.getFriendWatchFile(userId));
            } else {
                Files.write2File(notformat, Files.getFriendWatchFile(userId));
            }
        } catch (Exception e) {
            Log.runtime(TAG, "friendWatch save err:");
            Log.printStackTrace(TAG, e);
        }
    }

    public static void updateDay(String userId) {
        if (!needUpdateAll(Files.getFriendWatchFile(userId).lastModified())) {
            return;
        }
        JSONObject joSingle;
        try {
            String dateStr = TimeUtil.getDateStr();
            Iterator<String> ids = joFriendWatch.keys();
            while (ids.hasNext()) {
                String id = ids.next();
                joSingle = joFriendWatch.getJSONObject(id);
                joSingle.put("name", joSingle.optString("name"));
                joSingle.put("allGet", joSingle.optInt("allGet", 0) + joSingle.optInt("weekGet", 0));
                joSingle.put("weekGet", 0);
                if (!joSingle.has("startTime")) {
                    joSingle.put("startTime", dateStr);
                }
                joFriendWatch.put(id, joSingle);
            }
            Files.write2File(joFriendWatch.toString(), Files.getFriendWatchFile(userId));
        } catch (Throwable th) {
            Log.runtime(TAG, "friendWatchNewWeek err:");
            Log.printStackTrace(TAG, th);
        }
    }

    public static synchronized Boolean load(String userId) {
        try {
            String strFriendWatch = Files.readFromFile(Files.getFriendWatchFile(userId));
            if (!strFriendWatch.isEmpty()) {
                joFriendWatch = new JSONObject(strFriendWatch);
            } else {
                joFriendWatch = new JSONObject();
            }
            return true;
        } catch (JSONException e) {
            Log.printStackTrace(e);
            joFriendWatch = new JSONObject();
        }
        return false;
    }

    public static synchronized void unload() {
        joFriendWatch = new JSONObject();
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
                friendWatch.startTime = friend.optString("startTime", "无");
                friendWatch.weekGet = friend.optInt("weekGet", 0);
                friendWatch.allGet = friend.optInt("allGet", 0) + friendWatch.weekGet;
                String showText = name + "(开始统计时间:" + friendWatch.startTime + ")\n\n";
                showText = showText + "周收:" + friendWatch.weekGet + " 总收:" + friendWatch.allGet;
                friendWatch.name = showText;
                list.add(friendWatch);
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "FriendWatch getList: ");
            Log.printStackTrace(TAG, t);
            try {
                Files.write2File(new JSONObject().toString(), Files.getFriendWatchFile(userId));
            } catch (Exception e) {
                Log.printStackTrace(e);
            }
        }
        return list;
    }
}