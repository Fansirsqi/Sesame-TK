package fansirsqi.xposed.sesame.task.antOrchard;
import android.util.Base64;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import fansirsqi.xposed.sesame.entity.AlipayUser;
import fansirsqi.xposed.sesame.model.BaseModel;
import fansirsqi.xposed.sesame.model.ModelFields;
import fansirsqi.xposed.sesame.model.ModelGroup;
import fansirsqi.xposed.sesame.model.modelFieldExt.BooleanModelField;
import fansirsqi.xposed.sesame.model.modelFieldExt.IntegerModelField;
import fansirsqi.xposed.sesame.model.modelFieldExt.SelectModelField;
import fansirsqi.xposed.sesame.task.ModelTask;
import fansirsqi.xposed.sesame.task.TaskCommon;
import fansirsqi.xposed.sesame.util.Files;
import fansirsqi.xposed.sesame.util.Log;
import fansirsqi.xposed.sesame.util.Maps.UserMap;
import fansirsqi.xposed.sesame.util.RandomUtil;
import fansirsqi.xposed.sesame.data.Status;
import fansirsqi.xposed.sesame.util.ThreadUtil;
public class AntOrchard extends ModelTask {
  private static final String TAG = AntOrchard.class.getSimpleName();
  private String userId;
  private String treeLevel;
  private String[] wuaList;
  private Integer executeIntervalInt;
  private IntegerModelField executeInterval;
  private BooleanModelField receiveOrchardTaskAward;
  private IntegerModelField orchardSpreadManureCount;
  private BooleanModelField batchHireAnimal;
  private SelectModelField dontHireList;
  private SelectModelField dontWeedingList;
  // 助力好友列表
  private SelectModelField assistFriendList;
  @Override
  public String getName() {
    return "农场";
  }
  @Override
  public ModelGroup getGroup() {
    return ModelGroup.ORCHARD;
  }
  @Override
  public String getIcon() {
    return "AntOrchard.png";
  }
  @Override
  public ModelFields getFields() {
    ModelFields modelFields = new ModelFields();
    modelFields.addField(executeInterval = new IntegerModelField("executeInterval", "执行间隔(毫秒)", 500));
    modelFields.addField(receiveOrchardTaskAward = new BooleanModelField("receiveOrchardTaskAward", "收取农场任务奖励", false));
    modelFields.addField(orchardSpreadManureCount = new IntegerModelField("orchardSpreadManureCount", "农场每日施肥次数", 0));
    modelFields.addField(assistFriendList = new SelectModelField("assistFriendList", "助力好友列表", new LinkedHashSet<>(), AlipayUser::getList));
    modelFields.addField(batchHireAnimal = new BooleanModelField("batchHireAnimal", "一键捉鸡除草", false));
    modelFields.addField(dontHireList = new SelectModelField("dontHireList", "除草 | 不雇佣好友列表", new LinkedHashSet<>(), AlipayUser::getList));
    modelFields.addField(dontWeedingList = new SelectModelField("dontWeedingList", "除草 | 不除草好友列表", new LinkedHashSet<>(), AlipayUser::getList));
    return modelFields;
  }
  @Override
  public Boolean check() {
    if (TaskCommon.IS_ENERGY_TIME){
      Log.record("⏸ 当前为只收能量时间【"+ BaseModel.getEnergyTime().getValue() +"】，停止执行" + getName() + "任务！");
      return false;
    }else if (TaskCommon.IS_MODULE_SLEEP_TIME) {
      Log.record("💤 模块休眠时间【"+ BaseModel.getModelSleepTime().getValue() +"】停止执行" + getName() + "任务！");
      return false;
    } else {
      return true;
    }
  }
  @Override
  public void run() {
    try {
      Log.record("执行开始-" + getName());
      executeIntervalInt = Math.max(executeInterval.getValue(), 500);
      String s = AntOrchardRpcCall.orchardIndex();
      JSONObject jo = new JSONObject(s);
      if ("100".equals(jo.getString("resultCode"))) {
        if (jo.optBoolean("userOpenOrchard")) {
          JSONObject taobaoData = new JSONObject(jo.getString("taobaoData"));
          treeLevel = Integer.toString(taobaoData.getJSONObject("gameInfo").getJSONObject("plantInfo").getJSONObject("seedStage").getInt("stageLevel"));
          JSONObject joo = new JSONObject(AntOrchardRpcCall.mowGrassInfo());
          if ("100".equals(jo.getString("resultCode"))) {
            userId = joo.getString("userId");
            if (jo.has("lotteryPlusInfo")) drawLotteryPlus(jo.getJSONObject("lotteryPlusInfo"));
            extraInfoGet();
            if (batchHireAnimal.getValue()) {
              if (!joo.optBoolean("hireCountOnceLimit", true) && !joo.optBoolean("hireCountOneDayLimit", true)) batchHireAnimalRecommend();
            }
            if (receiveOrchardTaskAward.getValue()) {
              doOrchardDailyTask(userId);
              triggerTbTask();
            }
            Integer orchardSpreadManureCountValue = orchardSpreadManureCount.getValue();
            if (orchardSpreadManureCountValue > 0 && Status.canSpreadManureToday(userId)) orchardSpreadManure();
            if (orchardSpreadManureCountValue >= 3 && orchardSpreadManureCountValue < 10) {
              querySubplotsActivity(3);
            } else if (orchardSpreadManureCountValue >= 10) {
              querySubplotsActivity(10);
            }
            // 助力
            orchardassistFriend();
          } else {
            Log.record(jo.getString("resultDesc"));
            Log.runtime(jo.toString());
          }
        } else {
          getEnableField().setValue(false);
          Log.other("请先开启芭芭农场！");
        }
      } else {
        Log.runtime(TAG, jo.getString("resultDesc"));
      }
    } catch (Throwable t) {
      Log.runtime(TAG, "start.run err:");
      Log.printStackTrace(TAG, t);
    }finally {
      Log.record("执行结束-" + getName());
    }
  }
  private String getWua() {
    if (wuaList == null) {
      try {
        String content = Files.readFromFile(Files.getWuaFile());
        wuaList = content.split("\n");
      } catch (Throwable ignored) {
        wuaList = new String[0];
      }
    }
    if (wuaList.length > 0) {
      return wuaList[RandomUtil.nextInt(0, wuaList.length - 1)];
    }
    return "null";
  }
  private boolean canSpreadManureContinue(int stageBefore, int stageAfter) {
    if (stageAfter - stageBefore > 1) {
      return true;
    }
    Log.record("施肥只加0.01%进度今日停止施肥！");
    return false;
  }
  private void orchardSpreadManure() {
    try {
      do {
        try {
          JSONObject jo = new JSONObject(AntOrchardRpcCall.orchardIndex());
          if (!"100".equals(jo.getString("resultCode"))) {
            Log.runtime(TAG, jo.getString("resultDesc"));
            return;
          }
          if (jo.has("spreadManureActivity")) {
            JSONObject spreadManureStage = jo.getJSONObject("spreadManureActivity").getJSONObject("spreadManureStage");
            if ("FINISHED".equals(spreadManureStage.getString("status"))) {
              String sceneCode = spreadManureStage.getString("sceneCode");
              String taskType = spreadManureStage.getString("taskType");
              int awardCount = spreadManureStage.getInt("awardCount");
              JSONObject joo = new JSONObject(AntOrchardRpcCall.receiveTaskAward(sceneCode, taskType));
              if (joo.optBoolean("success")) {
                Log.farm("丰收礼包🎁[肥料*" + awardCount + "]");
              } else {
                Log.record(joo.getString("desc"));
                Log.runtime(joo.toString());
              }
            }
          }
          String taobaoData = jo.getString("taobaoData");
          jo = new JSONObject(taobaoData);
          JSONObject plantInfo = jo.getJSONObject("gameInfo").getJSONObject("plantInfo");
          boolean canExchange = plantInfo.getBoolean("canExchange");
          if (canExchange) {
            Log.farm("🎉 农场果树似乎可以兑换了！");
            return;
          }
          JSONObject seedStage = plantInfo.getJSONObject("seedStage");
          treeLevel = Integer.toString(seedStage.getInt("stageLevel"));
          JSONObject accountInfo = jo.getJSONObject("gameInfo").getJSONObject("accountInfo");
          int happyPoint = Integer.parseInt(accountInfo.getString("happyPoint"));
          int wateringCost = accountInfo.getInt("wateringCost");
          int wateringLeftTimes = accountInfo.getInt("wateringLeftTimes");
          if (happyPoint > wateringCost && wateringLeftTimes > 0 && (200 - wateringLeftTimes < orchardSpreadManureCount.getValue())) {
            jo = new JSONObject(AntOrchardRpcCall.orchardSpreadManure(getWua()));
            if (!"100".equals(jo.getString("resultCode"))) {
              Log.record(jo.getString("resultDesc"));
              Log.runtime(jo.toString());
              return;
            }
            taobaoData = jo.getString("taobaoData");
            jo = new JSONObject(taobaoData);
            String stageText = jo.getJSONObject("currentStage").getString("stageText");
            Log.farm("农场施肥💩[" + stageText + "]");
            if (!canSpreadManureContinue(seedStage.getInt("totalValue"), jo.getJSONObject("currentStage").getInt("totalValue"))) {
              Status.spreadManureToday(userId);
              return;
            }
            continue;
          }
        } finally {
          ThreadUtil.sleep(executeIntervalInt);
        }
        break;
      } while (true);
    } catch (Throwable t) {
      Log.runtime(TAG, "orchardSpreadManure err:");
      Log.printStackTrace(TAG, t);
    }
  }
  private void extraInfoGet() {
    try {
      String s = AntOrchardRpcCall.extraInfoGet();
      JSONObject jo = new JSONObject(s);
      if ("100".equals(jo.getString("resultCode"))) {
        JSONObject fertilizerPacket = jo.getJSONObject("data").getJSONObject("extraData").getJSONObject("fertilizerPacket");
        if (!"todayFertilizerWaitTake".equals(fertilizerPacket.getString("status"))) return;
        int todayFertilizerNum = fertilizerPacket.getInt("todayFertilizerNum");
        jo = new JSONObject(AntOrchardRpcCall.extraInfoSet());
        if ("100".equals(jo.getString("resultCode"))) {
          Log.farm("每日肥料💩[" + todayFertilizerNum + "g]");
        } else {
          Log.runtime(jo.getString("resultDesc"), jo.toString());
        }
      } else {
        Log.runtime(jo.getString("resultDesc"), jo.toString());
      }
    } catch (Throwable t) {
      Log.runtime(TAG, "extraInfoGet err:");
      Log.printStackTrace(TAG, t);
    }
  }
  private void drawLotteryPlus(JSONObject lotteryPlusInfo) {
    try {
      if (!lotteryPlusInfo.has("userSevenDaysGiftsItem")) return;
      String itemId = lotteryPlusInfo.getString("itemId");
      JSONObject jo = lotteryPlusInfo.getJSONObject("userSevenDaysGiftsItem");
      JSONArray ja = jo.getJSONArray("userEverydayGiftItems");
      for (int i = 0; i < ja.length(); i++) {
        jo = ja.getJSONObject(i);
        if (jo.getString("itemId").equals(itemId)) {
          if (!jo.getBoolean("received")) {
            jo = new JSONObject(AntOrchardRpcCall.drawLottery());
            if ("100".equals(jo.getString("resultCode"))) {
              JSONArray userEverydayGiftItems = jo.getJSONObject("lotteryPlusInfo").getJSONObject("userSevenDaysGiftsItem").getJSONArray("userEverydayGiftItems");
              for (int j = 0; j < userEverydayGiftItems.length(); j++) {
                jo = userEverydayGiftItems.getJSONObject(j);
                if (jo.getString("itemId").equals(itemId)) {
                  int awardCount = jo.optInt("awardCount", 1);
                  Log.farm("七日礼包🎁[获得肥料]#" + awardCount + "g");
                  break;
                }
              }
            } else {
              Log.runtime(jo.getString("resultDesc"), jo.toString());
            }
          } else {
            Log.record("七日礼包已领取");
          }
          break;
        }
      }
    } catch (Throwable t) {
      Log.runtime(TAG, "drawLotteryPlus err:");
      Log.printStackTrace(TAG, t);
    }
  }
  private void doOrchardDailyTask(String userId) {
    try {
      String s = AntOrchardRpcCall.orchardListTask();
      JSONObject jo = new JSONObject(s);
      if ("100".equals(jo.getString("resultCode"))) {
        if (jo.has("signTaskInfo")) {
          JSONObject signTaskInfo = jo.getJSONObject("signTaskInfo");
          orchardSign(signTaskInfo);
        }
        JSONArray jaTaskList = jo.getJSONArray("taskList");
        for (int i = 0; i < jaTaskList.length(); i++) {
          jo = jaTaskList.getJSONObject(i);
          if (!"TODO".equals(jo.getString("taskStatus"))) continue;
          String title = jo.getJSONObject("taskDisplayConfig").getString("title");
          if ("TRIGGER".equals(jo.getString("actionType")) || "ADD_HOME".equals(jo.getString("actionType")) || "PUSH_SUBSCRIBE".equals(jo.getString("actionType"))) {
            String taskId = jo.getString("taskId");
            String sceneCode = jo.getString("sceneCode");
            jo = new JSONObject(AntOrchardRpcCall.finishTask(userId, sceneCode, taskId));
            if (jo.optBoolean("success")) {
              Log.farm("农场任务🧾[" + title + "]");
            } else {
              Log.record(jo.getString("desc"));
              Log.runtime(jo.toString());
            }
          }
        }
      } else {
        Log.record(jo.getString("resultCode"));
        Log.runtime(s);
      }
    } catch (Throwable t) {
      Log.runtime(TAG, "doOrchardDailyTask err:");
      Log.printStackTrace(TAG, t);
    }
  }
  private void orchardSign(JSONObject signTaskInfo) {
    try {
      JSONObject currentSignItem = signTaskInfo.getJSONObject("currentSignItem");
      if (!currentSignItem.getBoolean("signed")) {
        JSONObject joSign = new JSONObject(AntOrchardRpcCall.orchardSign());
        if ("100".equals(joSign.getString("resultCode"))) {
          int awardCount = joSign.getJSONObject("signTaskInfo").getJSONObject("currentSignItem").getInt("awardCount");
          Log.farm("农场签到📅[获得肥料]#" + awardCount + "g");
        } else {
          Log.runtime(joSign.getString("resultDesc"), joSign.toString());
        }
      } else {
        Log.record("农场今日已签到");
      }
    } catch (Throwable t) {
      Log.runtime(TAG, "orchardSign err:");
      Log.printStackTrace(TAG, t);
    }
  }
  private static void triggerTbTask() {
    try {
      String s = AntOrchardRpcCall.orchardListTask();
      JSONObject jo = new JSONObject(s);
      if ("100".equals(jo.getString("resultCode"))) {
        JSONArray jaTaskList = jo.getJSONArray("taskList");
        for (int i = 0; i < jaTaskList.length(); i++) {
          jo = jaTaskList.getJSONObject(i);
          if (!"FINISHED".equals(jo.getString("taskStatus"))) continue;
          String title = jo.getJSONObject("taskDisplayConfig").getString("title");
          int awardCount = jo.optInt("awardCount", 0);
          String taskId = jo.getString("taskId");
          String taskPlantType = jo.getString("taskPlantType");
          jo = new JSONObject(AntOrchardRpcCall.triggerTbTask(taskId, taskPlantType));
          if ("100".equals(jo.getString("resultCode"))) {
            Log.farm("领取奖励🎖️[" + title + "]#" + awardCount + "g肥料");
          } else {
            Log.record(jo.getString("resultDesc"));
            Log.runtime(jo.toString());
          }
        }
      } else {
        Log.record(jo.getString("resultDesc"));
        Log.runtime(s);
      }
    } catch (Throwable t) {
      Log.runtime(TAG, "triggerTbTask err:");
      Log.printStackTrace(TAG, t);
    }
  }
  private void querySubplotsActivity(int taskRequire) {
    try {
      String s = AntOrchardRpcCall.querySubplotsActivity(treeLevel);
      JSONObject jo = new JSONObject(s);
      if ("100".equals(jo.getString("resultCode"))) {
        JSONArray subplotsActivityList = jo.getJSONArray("subplotsActivityList");
        for (int i = 0; i < subplotsActivityList.length(); i++) {
          jo = subplotsActivityList.getJSONObject(i);
          if (!"WISH".equals(jo.getString("activityType"))) continue;
          String activityId = jo.getString("activityId");
          if ("NOT_STARTED".equals(jo.getString("status"))) {
            String extend = jo.getString("extend");
            jo = new JSONObject(extend);
            JSONArray wishActivityOptionList = jo.getJSONArray("wishActivityOptionList");
            String optionKey = null;
            for (int j = 0; j < wishActivityOptionList.length(); j++) {
              jo = wishActivityOptionList.getJSONObject(j);
              if (taskRequire == jo.getInt("taskRequire")) {
                optionKey = jo.getString("optionKey");
                break;
              }
            }
            if (optionKey != null) {
              jo = new JSONObject(AntOrchardRpcCall.triggerSubplotsActivity(activityId, "WISH", optionKey));
              if ("100".equals(jo.getString("resultCode"))) {
                Log.farm("农场许愿✨[每日施肥" + taskRequire + "次]");
              } else {
                Log.record(jo.getString("resultDesc"));
                Log.runtime(jo.toString());
              }
            }
          } else if ("FINISHED".equals(jo.getString("status"))) {
            jo = new JSONObject(AntOrchardRpcCall.receiveOrchardRights(activityId, "WISH"));
            if ("100".equals(jo.getString("resultCode"))) {
              Log.farm("许愿奖励✨[肥料" + jo.getInt("amount") + "g]");
              querySubplotsActivity(taskRequire);
              return;
            } else {
              Log.record(jo.getString("resultDesc"));
              Log.runtime(jo.toString());
            }
          }
        }
      } else {
        Log.record(jo.getString("resultDesc"));
        Log.runtime(s);
      }
    } catch (Throwable t) {
      Log.runtime(TAG, "triggerTbTask err:");
      Log.printStackTrace(TAG, t);
    }
  }
  /**
   * 创建动物信息JSON字符串。
   *
   * @param animalUserId   动物用户ID
   * @param earnManureCount 赚取肥料数量
   * @param groupId        组ID
   * @param orchardUserId  果园用户ID
   * @return 动物信息JSON字符串
   */
  private String createAnimalInfoJson(String animalUserId, int earnManureCount, String groupId, String orchardUserId) {
    return "{\"animalUserId\":\"" + animalUserId + "\",\"earnManureCount\":" + earnManureCount + ",\"groupId\":\"" + groupId + "\",\"orchardUserId\":\"" + orchardUserId + "\"}";
  }
  /** 一键捉鸡除草 */
  private void batchHireAnimalRecommend() {
    try {
      JSONObject jo = new JSONObject(AntOrchardRpcCall.batchHireAnimalRecommend(UserMap.getCurrentUid()));
      if ("100".equals(jo.getString("resultCode"))) {
        JSONArray recommendGroupList = jo.optJSONArray("recommendGroupList");
        if (recommendGroupList != null && recommendGroupList.length() > 0) {
          List<String> GroupList = new ArrayList<>();
          for (int i = 0; i < recommendGroupList.length(); i++) {
            jo = recommendGroupList.getJSONObject(i);
            String animalUserId = jo.getString("animalUserId");
            if (dontHireList.getValue().contains(animalUserId))
              continue;
            int earnManureCount = jo.getInt("earnManureCount");
            String groupId = jo.getString("groupId");
            String orchardUserId = jo.getString("orchardUserId");
            if (dontWeedingList.getValue().contains(orchardUserId)) {
              continue;
            }
            GroupList.add(createAnimalInfoJson(animalUserId, earnManureCount, groupId, orchardUserId));
          }
          if (!GroupList.isEmpty()) {
            jo = new JSONObject(AntOrchardRpcCall.batchHireAnimal(GroupList));
            if ("100".equals(jo.getString("resultCode"))) {
              Log.farm("一键捉鸡🐣[除草]");
            }
          }
        }
      } else {
        Log.record(jo.getString("resultDesc"));
        Log.runtime(jo.toString());
      }
    } catch (Throwable t) {
      Log.runtime(TAG, "batchHireAnimalRecommend err:");
      Log.printStackTrace(TAG, t);
    }
  }
  // 助力
  private void orchardassistFriend() {
    try {
      if (!Status.canAntOrchardAssistFriendToday()) {
        return;
      }
      Set<String> friendSet = assistFriendList.getValue();
      for (String uid : friendSet) {
        String shareId = Base64.encodeToString((uid + "-" + RandomUtil.getRandom(5) + "ANTFARM_ORCHARD_SHARE_P2P").getBytes(), Base64.NO_WRAP);
        String str = AntOrchardRpcCall.achieveBeShareP2P(shareId);
        JSONObject jsonObject = new JSONObject(str);
        ThreadUtil.sleep(800);
        String name = UserMap.getMaskName(uid);
        if (!jsonObject.optBoolean("success")) {
          String code = jsonObject.getString("code");
          if ("600000027".equals(code)) {
            Log.record("农场助力💪今日助力他人次数上限");
            Status.antOrchardAssistFriendToday();
            return;
          }
          Log.record("农场助力😔失败[" + name + "]" + jsonObject.optString("desc"));
          continue;
        }
        Log.farm("农场助力💪[助力:" + name + "]");
      }
      Status.antOrchardAssistFriendToday();
    } catch (Throwable t) {
      Log.runtime(TAG, "orchardassistFriend err:");
      Log.printStackTrace(TAG, t);
    }
  }
}
