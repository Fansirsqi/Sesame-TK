package fansirsqi.xposed.sesame.util;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.regex.Pattern;

public class ResChecker {
    private static final String TAG = ResChecker.class.getSimpleName();

    private static boolean core(String TAG, JSONObject jo) {
        try {
            Log.runtime(TAG, "Checking JSON success: " + jo);
            // 检查 success 或 isSuccess 字段为 true
            if (jo.optBoolean("success") || jo.optBoolean("isSuccess")) {
                return true;
            }

            // 检查 resultCode
            Object resCode = jo.opt("resultCode");
            if (resCode != null) {
                if (resCode instanceof Integer && (Integer) resCode == 200) {
                    return true;
                } else if (resCode instanceof String &&
                        Pattern.matches("(?i)SUCCESS|100", (String) resCode)) {
                    return true;
                }
            }

            // 检查 memo 字段
            if ("SUCCESS".equalsIgnoreCase(jo.optString("memo", ""))) {
                return true;
            }

            Log.error(TAG, "Check failed: " + jo);
            return false;

        } catch (Throwable t) {
            Log.printStackTrace(TAG, "Error checking JSON success:", t);
            return false;
        }
    }

    /**
     * 检查JSON对象是否表示成功
     * <p>
     * 成功条件包括：<br/>
     * - success == true<br/>
     * - isSuccess == true<br/>
     * - resultCode == 200 或 "SUCCESS" 或 "100"<br/>
     * - memo == "SUCCESS"<br/>
     *
     * @param jo JSON对象
     * @return true 如果成功
     */
    private static boolean checkRes(JSONObject jo) {
        return core(TAG, jo);
    }

    /**
     * 检查JSON对象是否表示成功
     * <p>
     * 成功条件包括：<br/>
     * - success == true<br/>
     * - isSuccess == true<br/>
     * - resultCode == 200 或 "SUCCESS" 或 "100"<br/>
     * - memo == "SUCCESS"<br/>
     *
     * @param jo JSON对象
     * @return true 如果成功
     */
    public static boolean checkRes(String TAG, JSONObject jo) {
        return core(TAG, jo);
    }



    /**
     * 检查JSON对象是否表示成功
     * <p>
     * 成功条件包括：<br/>
     * - success == true<br/>
     * - isSuccess == true<br/>
     * - resultCode == 200 或 "SUCCESS" 或 "100"<br/>
     * - memo == "SUCCESS"<br/>
     *
     * @param jsonStr JSON对象的字符串表示
     * @return true 如果成功
     */
    public static boolean checkRes(String TAG, String jsonStr) throws JSONException {
        JSONObject jo = new JSONObject(jsonStr);
        return checkRes(TAG, jo);
    }


}
