package fansirsqi.xposed.sesame.task.antForest;

import static fansirsqi.xposed.sesame.task.antForest.AntForest.giveEnergyRainList;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Set;

import fansirsqi.xposed.sesame.hook.Toast;
import fansirsqi.xposed.sesame.util.Log;
import fansirsqi.xposed.sesame.util.Maps.UserMap;
import fansirsqi.xposed.sesame.util.ResUtil;
import fansirsqi.xposed.sesame.util.ThreadUtil;

public class EnergyRain {
    private static final String TAG = EnergyRain.class.getSimpleName();

    public static void startEnergyRain() {
        try {
            Log.forest("开始执行能量雨🌧️");
            JSONObject jo = new JSONObject(AntForestRpcCall.startEnergyRain());
            ThreadUtil.sleep(300);
            if (ResUtil.checkResultCode(jo)) {
                String token = jo.getString("token");
                JSONArray bubbleEnergyList = jo.getJSONObject("difficultyInfo").getJSONArray("bubbleEnergyList");
                int sum = 0;
                for (int i = 0; i < bubbleEnergyList.length(); i++) {
                    sum += bubbleEnergyList.getInt(i);
                }
                ThreadUtil.sleep(5000);
                String result = AntForestRpcCall.energyRainSettlement(sum, token);
                if (ResUtil.checkResultCode(result)) {
                    String s = "收获能量雨🌧️[" + sum + "g]";
                    Toast.show(s);
                    Log.forest(s);
                }
                ThreadUtil.sleep(300);
            }
        } catch (Throwable th) {
            Log.runtime(TAG, "执行能量雨出错:");
            Log.printStackTrace(TAG, th);
        }
    }

    static void energyRain() {
        try {
            JSONObject joEnergyRainHome = new JSONObject(AntForestRpcCall.queryEnergyRainHome());
            Thread.sleep(300);
            if (ResUtil.checkResultCode(joEnergyRainHome)) {
                if (joEnergyRainHome.getBoolean("canPlayToday")) {
                    startEnergyRain();
                }
                if (joEnergyRainHome.getBoolean("canGrantStatus")) {
                    Log.record("有送能量雨的机会");
                    JSONObject joEnergyRainCanGrantList = new JSONObject(AntForestRpcCall.queryEnergyRainCanGrantList());
                    Thread.sleep(300);
                    JSONArray grantInfos = joEnergyRainCanGrantList.getJSONArray("grantInfos");
                    Set<String> set = giveEnergyRainList.getValue();
                    String uid;
                    boolean granted = false;
                    for (int j = 0; j < grantInfos.length(); j++) {
                        JSONObject grantInfo = grantInfos.getJSONObject(j);
                        if (grantInfo.getBoolean("canGrantedStatus")) {
                            uid = grantInfo.getString("userId");
                            if (set.contains(uid)) {
                                JSONObject rainJsonObj = new JSONObject(AntForestRpcCall.grantEnergyRainChance(uid));
                                ThreadUtil.sleep(300);
                                Log.record("尝试送能量雨给【" + UserMap.getMaskName(uid) + "】");
                                granted = true;
                                if (ResUtil.checkResultCode(rainJsonObj)) {
                                    Log.forest("赠送能量雨机会给🌧️[" + UserMap.getMaskName(uid) + "]#" + UserMap.getMaskName(UserMap.getCurrentUid()));
                                    startEnergyRain();
                                } else {
                                    Log.record("送能量雨失败");
                                    Log.runtime(rainJsonObj.toString());
                                }
                                break;
                            }
                        }
                    }
                    if (!granted) {
                        Log.record("没有可以送的用户");
                    }
                }
            }
            Thread.sleep(300);
            joEnergyRainHome = new JSONObject(AntForestRpcCall.queryEnergyRainHome());
            if (ResUtil.checkResultCode(joEnergyRainHome) && joEnergyRainHome.getBoolean("canPlayToday")) {
                startEnergyRain();
            }
        } catch (Throwable th) {
            Log.runtime(TAG, "energyRain err:");
            Log.printStackTrace(TAG, th);
        }
    }
}
