package fansirsqi.xposed.sesame.util;

import org.json.JSONObject;

public class MessageUtil {
    private static final String TAG = MessageUtil.class.getSimpleName();
    private static final String UNKNOWN_TAG = "Unknown TAG";

    public static JSONObject newJSONObject(String str) {
        try {
            return new JSONObject(str);
        } catch (Throwable t) {
            Log.error(TAG, "newJSONObject err:");
            Log.printStackTrace(TAG, t);
        }
        return null;
    }

    public static void printErrorMessage(String tag, JSONObject jo, String errorMessageField) {
        try {
            String errMsg = tag + " error:";
            Log.record(errMsg + jo.getString(errorMessageField));
            Log.runtime(jo.getString(errorMessageField), jo.toString());
        } catch (Throwable t) {
            Log.error(TAG, "printErrorMessage err:");
            Log.printStackTrace(TAG, t);
        }
    }

    public static Boolean checkMemo(JSONObject jo) {
        return checkMemo(UNKNOWN_TAG, jo);
    }

    public static Boolean checkMemo(String tag, JSONObject jo) {
        try {
            if (!"SUCCESS".equals(jo.optString("memo"))) {
                if (jo.has("memo")) {
                    printErrorMessage(tag, jo, "memo");
                } else {
                    Log.runtime(tag, jo.toString());
                }
                return false;
            }
            return true;
        } catch (Throwable t) {
            Log.error(TAG, "checkMemo err:");
            Log.printStackTrace(TAG, t);
        }
        return false;
    }

    public static Boolean checkResultCode(JSONObject jo) {
        return checkResultCode(UNKNOWN_TAG, jo);
    }

    public static Boolean checkResultCode(String tag, JSONObject jo) {
        try {
            Object resultCode = jo.opt("resultCode");
            if (resultCode instanceof Integer) {
                return checkResultCodeInteger(tag, jo);
            } else if (resultCode instanceof String) {
                return checkResultCodeString(tag, jo);
            }
            Log.runtime(tag, jo.toString());
            return false;
        } catch (Throwable t) {
            Log.error(TAG, "checkResultCode err:");
            Log.printStackTrace(TAG, t);
        }
        return false;
    }

    public static Boolean checkResultCodeString(String tag, JSONObject jo) {
        try {
            String resultCode = jo.optString("resultCode");
            if (!resultCode.equalsIgnoreCase("SUCCESS") && !resultCode.equals("100")) {
                if (jo.has("resultDesc")) {
                    printErrorMessage(tag, jo, "resultDesc");
                } else if (jo.has("resultView")) {
                    printErrorMessage(tag, jo, "resultView");
                } else {
                    Log.runtime(tag, jo.toString());
                }
                return false;
            }
            return true;
        } catch (Throwable t) {
            Log.error(TAG, "checkResultCodeString err:");
            Log.printStackTrace(TAG, t);
        }
        return false;
    }

    public static Boolean checkResultCodeInteger(String tag, JSONObject jo) {
        try {
            int resultCode = jo.optInt("resultCode");
            if (resultCode != 200) {
                if (jo.has("resultMsg")) {
                    printErrorMessage(tag, jo, "resultMsg");
                } else {
                    Log.runtime(tag, jo.toString());
                }
                return false;
            }
            return true;
        } catch (Throwable t) {
            Log.error(TAG, "checkResultCodeInteger err:");
            Log.printStackTrace(TAG, t);
        }
        return false;
    }

    public static Boolean checkSuccess(JSONObject jo) {
        return checkSuccess(UNKNOWN_TAG, jo);
    }

    public static Boolean checkSuccess(String tag, JSONObject jo) {
        try {
            if (!jo.optBoolean("success") && !jo.optBoolean("isSuccess")) {
                if (jo.has("errorMsg")) {
                    printErrorMessage(tag, jo, "errorMsg");
                } else if (jo.has("errorMessage")) {
                    printErrorMessage(tag, jo, "errorMessage");
                } else if (jo.has("desc")) {
                    printErrorMessage(tag, jo, "desc");
                } else if (jo.has("resultDesc")) {
                    printErrorMessage(tag, jo, "resultDesc");
                } else if (jo.has("resultView")) {
                    printErrorMessage(tag, jo, "resultView");
                } else {
                    Log.runtime(tag, jo.toString());
                }
                return false;
            }
            return true;
        } catch (Throwable t) {
            Log.error(TAG, "checkSuccess err:");
            Log.printStackTrace(TAG, t);
        }
        return false;
    }
}
