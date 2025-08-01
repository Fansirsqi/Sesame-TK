package fansirsqi.xposed.sesame.task.antForest;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import fansirsqi.xposed.sesame.util.GlobalThreadPools;
import fansirsqi.xposed.sesame.util.Log;
import fansirsqi.xposed.sesame.util.ResChecker;

/**
 * @author Byseven
 * @date 2025/3/7
 * @apiNote
 */
public class WhackMole {
    private static final String TAG = WhackMole.class.getSimpleName();

    /**
     * 6秒拼手速 打地鼠
     */
    public static void startWhackMole() {
        try {
            long startTime = System.currentTimeMillis();
            JSONObject response = new JSONObject(AntForestRpcCall.startWhackMole("senlinguangchangdadishu"));
            if (response.optBoolean("success")) {
                JSONArray moleInfoArray = response.optJSONArray("moleInfo");
                if (moleInfoArray != null) {
                    List<String> moleIdList = new ArrayList<>();
                    for (int i = 0; i < moleInfoArray.length(); i++) {
                        JSONObject mole = moleInfoArray.getJSONObject(i);
                        long moleId = mole.getLong("id");
                        moleIdList.add(String.valueOf(moleId)); // 收集每个地鼠的 ID
                    }
                    if (!moleIdList.isEmpty()) {
                        String token = response.getString("token"); // 获取令牌
                        long elapsedTime = System.currentTimeMillis() - startTime; // 计算已耗时间
                        GlobalThreadPools.sleep(Math.max(0, 6000 - elapsedTime)); // 睡眠至6秒
                        response = new JSONObject(AntForestRpcCall.settlementWhackMole(token, moleIdList, "senlinguangchangdadishu"));
                        if (ResChecker.checkRes(TAG, response)) {
                            int totalEnergy = response.getInt("totalEnergy");
                            Log.forest("森林能量⚡️[获得:6秒拼手速能量 " + totalEnergy + "g]");
                        }
                    }
                }
            } else {
                Log.runtime(TAG, response.getJSONObject("data").toString());
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "whackMole err");
            Log.printStackTrace(TAG, t);
        }
    }

    /**
     * 关闭6秒拼手速
     */
    public static Boolean closeWhackMole() {
        try {
            JSONObject jo = new JSONObject(AntForestRpcCall.closeWhackMole("senlinguangchangdadishu"));
            if (jo.optBoolean("success")) {
                return true;
            } else {
                Log.runtime(TAG, jo.getString("resultDesc"));
            }
        } catch (Throwable t) {
            Log.printStackTrace(t);
        }
        return false;
    }
}
