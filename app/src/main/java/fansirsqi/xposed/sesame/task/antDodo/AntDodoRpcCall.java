package fansirsqi.xposed.sesame.task.antDodo;
import fansirsqi.xposed.sesame.hook.RequestManager;
import fansirsqi.xposed.sesame.util.RandomUtil;

/**
 * 神奇物种RPC调用类
 * 负责封装所有与支付宝神奇物种相关的网络请求
 */
public class AntDodoRpcCall {
    
    /**
     * 查询动物收集状态
     * 检查用户今日是否已完成动物卡片收集
     * @return 包含收集状态的JSON响应
     */
    public static String queryAnimalStatus() {
        return RequestManager.requestString("alipay.antdodo.rpc.h5.queryAnimalStatus",
                "[{\"source\":\"chInfo_ch_appcenter__chsub_9patch\"}]");
    }
    
    /**
     * 获取神奇物种主页信息
     * 包含图鉴信息、限制条件、可收集次数等
     * @return 包含主页数据的JSON响应
     */
    public static String homePage() {
        return RequestManager.requestString("alipay.antdodo.rpc.h5.homePage",
                "[{}]");
    }
    
    /**
     * 获取任务入口信息
     * 查询待完成和已完成的任务状态
     * @return 包含任务入口信息的JSON响应
     */
    public static String taskEntrance() {
        return RequestManager.requestString("alipay.antdodo.rpc.h5.taskEntrance",
                "[{\"statusList\":[\"TODO\",\"FINISHED\"]}]");
    }
    
    /**
     * 收集动物卡片
     * 执行抽卡操作，获取随机动物卡片
     * @return 包含抽卡结果的JSON响应
     */
    public static String collect() {
        return RequestManager.requestString("alipay.antdodo.rpc.h5.collect",
                "[{}]");
    }
    
    /**
     * 获取任务列表
     * 查询所有可用的任务及其完成状态
     * @return 包含任务列表的JSON响应
     */
    public static String taskList() {
        return RequestManager.requestString("alipay.antdodo.rpc.h5.taskList",
                "[{}]");
    }
    
    /**
     * 完成任务
     * 标记指定任务为已完成状态
     * @param sceneCode 场景代码
     * @param taskType 任务类型
     * @return 完成任务的结果响应
     */
    public static String finishTask(String sceneCode, String taskType) {
        String uniqueId = getUniqueId();
        return RequestManager.requestString("com.alipay.antiep.finishTask",
                "[{\"outBizNo\":\"" + uniqueId + "\",\"requestType\":\"rpc\",\"sceneCode\":\""
                        + sceneCode + "\",\"source\":\"af-biodiversity\",\"taskType\":\""
                        + taskType + "\",\"uniqueId\":\"" + uniqueId + "\"}]");
    }
    
    /**
     * 生成唯一ID
     * 用于请求标识，避免重复提交
     * @return 基于时间戳和随机数的唯一ID
     */
    private static String getUniqueId() {
        return String.valueOf(System.currentTimeMillis()) + RandomUtil.nextLong();
    }
    
    /**
     * 领取任务奖励
     * 领取已完成任务的奖励物品
     * @param sceneCode 场景代码
     * @param taskType 任务类型
     * @return 领取奖励的结果响应
     */
    public static String receiveTaskAward(String sceneCode, String taskType) {
        return RequestManager.requestString("com.alipay.antiep.receiveTaskAward",
                "[{\"ignoreLimit\":0,\"requestType\":\"rpc\",\"sceneCode\":\"" + sceneCode
                        + "\",\"source\":\"af-biodiversity\",\"taskType\":\"" + taskType
                        + "\"}]");
    }
    
    /**
     * 获取道具列表
     * 查询用户拥有的所有道具及其数量
     * @return 包含道具列表的JSON响应
     */
    public static String propList() {
        return RequestManager.requestString("alipay.antdodo.rpc.h5.propList",
                "[{}]");
    }
    
    /**
     * 使用道具
     * 消耗指定道具，获得相应效果
     * @param propId 道具ID
     * @param propType 道具类型
     * @return 使用道具的结果响应
     */
    public static String consumeProp(String propId, String propType) {
        return RequestManager.requestString("alipay.antdodo.rpc.h5.consumeProp",
                "[{\"propId\":\"" + propId + "\",\"propType\":\"" + propType + "\"}]");
    }
    
    /**
     * 查询图鉴信息
     * 获取指定图鉴的详细信息，包括已收集的动物
     * @param bookId 图鉴ID
     * @return 包含图鉴信息的JSON响应
     */
    public static String queryBookInfo(String bookId) {
        return RequestManager.requestString("alipay.antdodo.rpc.h5.queryBookInfo",
                "[{\"bookId\":\"" + bookId + "\"}]");
    }
    
    /**
     * 社交功能 - 送卡片给好友
     * 将指定的动物卡片赠送给好友
     * @param targetAnimalId 目标动物ID
     * @param targetUserId 目标用户ID
     * @return 赠送卡片的结果响应
     */
    public static String social(String targetAnimalId, String targetUserId) {
        return RequestManager.requestString("alipay.antdodo.rpc.h5.social",
                "[{\"actionCode\":\"GIFT_TO_FRIEND\",\"source\":\"GIFT_TO_FRIEND_FROM_CC\",\"targetAnimalId\":\""
                        + targetAnimalId + "\",\"targetUserId\":\"" + targetUserId
                        + "\",\"triggerTime\":\"" + System.currentTimeMillis() + "\"}]");
    }
    
    /**
     * 查询好友列表
     * 获取可交互的好友列表及其状态
     * @return 包含好友列表的JSON响应
     */
    public static String queryFriend() {
        return RequestManager.requestString("alipay.antdodo.rpc.h5.queryFriend",
                "[{\"sceneCode\":\"EXCHANGE\"}]");
    }
    
    /**
     * 帮好友收集卡片
     * 为指定好友执行抽卡操作
     * @param targetUserId 目标用户ID
     * @return 帮好友抽卡的结果响应
     */
    public static String collect(String targetUserId) {
        return RequestManager.requestString("alipay.antdodo.rpc.h5.collect",
                "[{\"targetUserId\":" + targetUserId + "}]");
    }
    
    /**
     * 查询图鉴列表
     * 分页获取用户的图鉴列表
     * @param pageSize 每页大小
     * @param pageStart 起始页码
     * @return 包含图鉴列表的JSON响应
     */
    public static String queryBookList(int pageSize, int pageStart) {
        String args = "[{\"pageSize\":" + pageSize + ",\"pageStart\":\"" + pageStart + "\",\"v2\":\"true\"}]";
        return RequestManager.requestString("alipay.antdodo.rpc.h5.queryBookList", args);
    }
    
    /**
     * 生成图鉴勋章
     * 为已集齐的图鉴生成勋章
     * @param bookId 图鉴ID
     * @return 生成勋章的结果响应
     */
    public static String generateBookMedal(String bookId) {
        String args = "[{\"bookId\":\"" + bookId + "\"}]";
        return RequestManager.requestString("alipay.antdodo.rpc.h5.generateBookMedal", args);
    }
}