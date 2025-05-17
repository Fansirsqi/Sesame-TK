package fansirsqi.xposed.sesame.task.otherTask;

import fansirsqi.xposed.sesame.util.JsonUtil;
import fansirsqi.xposed.sesame.util.Log;
import fansirsqi.xposed.sesame.util.RandomUtil;
import fansirsqi.xposed.sesame.util.TimeUtil;
import org.json.JSONArray;
import org.json.JSONObject;

public class LuckyCode extends BaseCommTask {
    private boolean isReceive;

    public LuckyCode() {
        this.isReceive = false;
        this.displayName = "收益天天乐💴";
        this.hoursKeyEnum = CompletedKeyEnum.LuckyCode;
    }

    private void dailyProfit(Object obj, String str) {
        if (obj == null) {
            TimeUtil.sleep((long) this.executeIntervalInt);
            return;
        }
        try {
            JSONObject jSONObject = (JSONObject) obj;
            String string = jSONObject.getString("prizeId");
            String string2 = jSONObject.getString("campId");
            int i = jSONObject.getInt("amount");
            StringBuilder stringBuilder = new StringBuilder();
            for (int i2 = 0; i2 < i; i2++) {
                stringBuilder.append(RandomUtil.nextInt(10,15));
                stringBuilder.append(",");
            }
            receiveNumber(string2, stringBuilder.substring(0, stringBuilder.length() - 1), string, str, "LUCKY_CODE_CAMP");
        } catch (Exception e) {
            Log.printStackTrace(e);
        } catch (Throwable th) {
            TimeUtil.sleep((long) this.executeIntervalInt);
        }
        TimeUtil.sleep((long) this.executeIntervalInt);
    }

    private void queryHistoryV3() {
        try {
            JSONObject requestString = requestString("com.alipay.finpromobff.luckycode.queryHistoryV3", "\"pageIndex\": 1");
            if (requestString != null) {
                Object valueByPathObject = JsonUtil.getValueByPathObject(requestString, "result.recordList");
                if (valueByPathObject != null) {
                    JSONArray jSONArray = (JSONArray) valueByPathObject;
                    for (int i = 0; i < jSONArray.length(); i++) {
                        if (jSONArray.getJSONObject(i).has("divideRecordDTO")) {
                            JSONObject jSONObject = jSONArray.getJSONObject(i).getJSONObject("divideRecordDTO");
                            if ("WAIT_RECEIVE".equals(jSONObject.getString("status"))) {
                                String string = jSONObject.getString("batchId");
                                String string2 = jSONObject.getString("playId");
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append("\"batchId\": \"");
                                stringBuilder.append(string);
                                stringBuilder.append("\",\"playId\": \"");
                                stringBuilder.append(string2);
                                stringBuilder.append("\"");
                                jSONObject = requestString("com.alipay.finpromobff.luckycode.receiveAward", stringBuilder.toString());
                                if (jSONObject != null) {
                                    StringBuilder stringBuilder2 = new StringBuilder();
                                    stringBuilder2.append(this.displayName);
                                    stringBuilder2.append("中奖领取成功[");
                                    stringBuilder2.append(toChinese(JsonUtil.getValueByPath(jSONObject, "result.calculateResult.dividePrizeDecidedDTOList.[0].extInfo.prizeLevel.name")));
                                    stringBuilder2.append("]");
                                    Log.other(stringBuilder2.toString());
                                }
                            }
                        } else {
                            Log.other(displayName+"divideRecordDTO 不存在，跳过处理");
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.printStackTrace(e);
        }
    }


    private void queryV3() {
        try {
            JSONObject requestString = requestString("com.alipay.finpromobff.luckycode.queryV3", "");
            if (requestString == null) {
                TimeUtil.sleep((long) this.executeIntervalInt);
                return;
            }
            this.isReceive = false;
            requestString = requestString.getJSONObject("result");
            JSONArray optJSONArray = requestString.optJSONArray("commonTaskList");
            JSONArray optJSONArray2 = requestString.optJSONArray("recommendTaskList");
            String valueByPath = JsonUtil.getValueByPath(requestString, "currBatchInfo.batchId");
            dailyProfit(JsonUtil.getValueByPathObject(requestString, "activity.dailyProfit"), valueByPath);
            sendTask(optJSONArray, valueByPath);
            sendTask(optJSONArray2, valueByPath);
            updateSlot(requestString, valueByPath);
            queryHistoryV3();
            TimeUtil.sleep((long) this.executeIntervalInt);
        } catch (Exception e) {
            Log.printStackTrace(e);
        } catch (Throwable th) {
            TimeUtil.sleep((long) this.executeIntervalInt);
        }
    }

    private void receiveNumber(String str, String str2, String str3, String str4, String str5) {
        try {
            StringBuilder stringBuilder = new StringBuilder("\"id\": \"");
            stringBuilder.append(str);
            stringBuilder.append("\",\"number\": \"");
            stringBuilder.append(str2);
            stringBuilder.append("\",\"numberType\": \"PROFIT_LUCKY_CODE_COMMON\",\"period\": \"");
            stringBuilder.append(str4);
            stringBuilder.append("\",\"subId\": \"");
            stringBuilder.append(str3);
            stringBuilder.append("\",\"triggerType\": \"");
            stringBuilder.append(str5);
            stringBuilder.append("\"");
            if (requestString("com.alipay.finpromobff.luckycode.receiveNumber", stringBuilder.toString()) == null) {
                TimeUtil.sleep((long) this.executeIntervalInt);
                return;
            }
            this.isReceive = true;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(this.displayName);
            stringBuilder2.append("获得好运码[");
            stringBuilder2.append(str2);
            stringBuilder2.append("]");
            Log.other(stringBuilder2.toString());
            TimeUtil.sleep((long) this.executeIntervalInt);
        } catch (Exception e) {
            Log.printStackTrace(e);
        } catch (Throwable th) {
            TimeUtil.sleep((long) this.executeIntervalInt);
        }
    }

    private void sendTask(JSONArray jSONArray, String str) {
        if (jSONArray == null) {
            TimeUtil.sleep((long) this.executeIntervalInt);
            return;
        }
        int i = 0;
        while (i < jSONArray.length()) {
            try {
                JSONObject jSONObject = jSONArray.getJSONObject(i);
                if (!"RECEIVE_SUCCESS".equals(jSONObject.getString("taskProcessStatus"))) {
                    String valueByPath = JsonUtil.getValueByPath(jSONObject, "TASK_MORPHO_DETAIL.taskType");
                    if (!"SUPER".equals(valueByPath)) {
                        if (!"COMMON".equals(valueByPath)) {
                            if (!"TRANSFORMER".equals(JsonUtil.getValueByPath(jSONObject, "taskExtProps.TASK_TYPE"))) {
                                String string = jSONObject.getString("taskId");
                                valueByPath = jSONObject.getString("appletName");
                                String string2 = jSONObject.getString("appletId");
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append("\"taskCenId\": \"");
                                stringBuilder.append(string2);
                                stringBuilder.append("\",\"taskId\": \"");
                                stringBuilder.append(string);
                                stringBuilder.append("\"");
                                if (requestString("com.alipay.finpromobff.luckycode.sendTask", stringBuilder.toString()) != null) {
                                    StringBuilder stringBuilder2 = new StringBuilder();
                                    stringBuilder2.append(this.displayName);
                                    stringBuilder2.append("完成任务[");
                                    stringBuilder2.append(valueByPath);
                                    stringBuilder2.append("]");
                                    Log.other(stringBuilder2.toString());
                                    receiveNumber(string2, String.valueOf(RandomUtil.nextInt(10,15)), string, str, "LUCKY_CODE_TASK");
                                }
                            }
                        }
                    }
                }
                i++;
            } catch (Exception e) {
                Log.printStackTrace(e);
            } catch (Throwable th) {
                TimeUtil.sleep((long) this.executeIntervalInt);
            }
        }
        TimeUtil.sleep((long) this.executeIntervalInt);
    }

    private String toChinese(String str) {
        str = str.toUpperCase();
        str.hashCode();
        Object obj = -1;
        switch (str.hashCode()) {
            case -1852950412:
                if (!str.equals("SECOND")) {
                    break;
                }
                obj = null;
                break;
            case 66902672:
                if (!str.equals("FIRST")) {
                    break;
                }
                obj = 1;
                break;
            case 79793479:
                if (!str.equals("THIRD")) {
                    break;
                }
                obj = 2;
                break;
            case 2079612442:
                if (!str.equals("FOURTH")) {
                    break;
                }
                obj = 3;
                break;
            default:
                break;
        }
        switch (obj.hashCode()) {
            case -1:
                return "二等奖";
            case 1:
                return "一等奖";
            case 2:
                return "三等奖";
            case 3:
                return "四等奖";
            default:
                return "其他";
        }
    }

    private void updateSlot(JSONObject jSONObject, String str) {
        String str2 = "[-1,-1,-1,-1,-1]";
        try {
            if (this.isReceive) {
                jSONObject = requestString("com.alipay.finpromobff.luckycode.queryV3", "");
                if (jSONObject != null) {
                    this.isReceive = false;
                    jSONObject = jSONObject.getJSONObject("result");
                }
                TimeUtil.sleep((long) this.executeIntervalInt);
            }
            JSONArray optJSONArray = jSONObject.optJSONArray("slotLuckyCode");
            Object valueByPathObject = JsonUtil.getValueByPathObject(jSONObject, "totalLuckyCode.[0].number");
            if (valueByPathObject != null) {
                JSONArray jSONArray = (JSONArray) valueByPathObject;
                if (optJSONArray == null) {
                    optJSONArray = new JSONArray();
                }
                String jSONArray2 = optJSONArray.toString();
                if (optJSONArray.length() == 0) {
                    optJSONArray.put(new JSONArray(str2)); // 修复点：将 StringBuilder 转换为 JSONArray
                    optJSONArray.put(new JSONArray(str2));
                    optJSONArray.put(new JSONArray(str2));
                }
                int i = 0;
                int i2 = 0;
                for (int i3 = 0; i3 < jSONArray.length(); i3++) {
                    int parseInt = Integer.parseInt(jSONArray.getString(i3));
                    JSONArray jSONArray3 = optJSONArray.getJSONArray(i);
                    jSONArray3.put(i2, parseInt);
                    if (i2 < jSONArray3.length()) {
                        i2++;
                    } else {
                        i++;
                        i2 = 0;
                    }
                }
                if (!jSONArray2.equals(optJSONArray.toString())) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("\"batchId\": \"");
                    stringBuilder.append(str);
                    stringBuilder.append("\",\"bizCode\": \"PROFIT_LUCKY_CODE_DAILY\",\"certificate\": \"");
                    stringBuilder.append(optJSONArray);
                    stringBuilder.append("\"");
                    if (requestString("com.alipay.finpromobff.luckycode.updateSlot", stringBuilder.toString()) != null) {
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append(this.displayName);
                        stringBuilder2.append("更新幸运码");
                        Log.other(stringBuilder2.toString());
                        TimeUtil.sleep((long) this.executeIntervalInt);
                        return;
                    }
                }
            }
            TimeUtil.sleep((long) this.executeIntervalInt);
        } catch (Exception e) {
            Log.printStackTrace(e);
        } catch (Throwable th) {
            TimeUtil.sleep((long) this.executeIntervalInt);
        }
    }


    protected void handle() {
        queryV3();
    }
}
