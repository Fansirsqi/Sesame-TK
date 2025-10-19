package fansirsqi.xposed.sesame.task.antForest

import android.annotation.SuppressLint
import com.fasterxml.jackson.core.type.TypeReference
import de.robv.android.xposed.XposedHelpers
import fansirsqi.xposed.sesame.data.RuntimeInfo
import fansirsqi.xposed.sesame.data.Status
import fansirsqi.xposed.sesame.entity.AlipayUser
import fansirsqi.xposed.sesame.entity.CollectEnergyEntity
import fansirsqi.xposed.sesame.entity.KVMap
import fansirsqi.xposed.sesame.entity.OtherEntityProvider.listEcoLifeOptions
import fansirsqi.xposed.sesame.entity.OtherEntityProvider.listHealthcareOptions
import fansirsqi.xposed.sesame.entity.VitalityStore
import fansirsqi.xposed.sesame.entity.VitalityStore.Companion.getNameById
import fansirsqi.xposed.sesame.hook.RequestManager.requestString
import fansirsqi.xposed.sesame.hook.Toast
import fansirsqi.xposed.sesame.hook.rpc.intervallimit.FixedOrRangeIntervalLimit
import fansirsqi.xposed.sesame.hook.rpc.intervallimit.IntervalLimit
import fansirsqi.xposed.sesame.hook.rpc.intervallimit.RpcIntervalLimit.addIntervalLimit
import fansirsqi.xposed.sesame.model.BaseModel
import fansirsqi.xposed.sesame.model.ModelFields
import fansirsqi.xposed.sesame.model.ModelGroup
import fansirsqi.xposed.sesame.model.modelFieldExt.BooleanModelField
import fansirsqi.xposed.sesame.model.modelFieldExt.ChoiceModelField
import fansirsqi.xposed.sesame.model.modelFieldExt.IntegerModelField
import fansirsqi.xposed.sesame.model.modelFieldExt.ListModelField
import fansirsqi.xposed.sesame.model.modelFieldExt.ListModelField.ListJoinCommaToStringModelField
import fansirsqi.xposed.sesame.model.modelFieldExt.SelectAndCountModelField
import fansirsqi.xposed.sesame.model.modelFieldExt.SelectModelField
import fansirsqi.xposed.sesame.model.modelFieldExt.StringModelField
import fansirsqi.xposed.sesame.newutil.DataStore
import fansirsqi.xposed.sesame.newutil.DataStore.put
import fansirsqi.xposed.sesame.task.ModelTask
import fansirsqi.xposed.sesame.task.TaskCommon
import fansirsqi.xposed.sesame.task.TaskStatus
import fansirsqi.xposed.sesame.task.antForest.ForestUtil.hasBombCard
import fansirsqi.xposed.sesame.task.antForest.ForestUtil.hasShield
import fansirsqi.xposed.sesame.task.antForest.Privilege.studentSignInRedEnvelope
import fansirsqi.xposed.sesame.task.antForest.Privilege.youthPrivilege
import fansirsqi.xposed.sesame.task.antForest.TaskTimeChecker
import fansirsqi.xposed.sesame.task.antForest.ForestFriendManager
import fansirsqi.xposed.sesame.ui.ObjReference
import fansirsqi.xposed.sesame.util.Average
import fansirsqi.xposed.sesame.util.GlobalThreadPools
import fansirsqi.xposed.sesame.util.ListUtil
import fansirsqi.xposed.sesame.util.Log
import fansirsqi.xposed.sesame.util.Notify.updateLastExecText
import fansirsqi.xposed.sesame.util.Notify.updateStatusText
import fansirsqi.xposed.sesame.util.RandomUtil
import fansirsqi.xposed.sesame.util.ResChecker
import fansirsqi.xposed.sesame.util.TimeCounter
import fansirsqi.xposed.sesame.util.TimeFormatter
import fansirsqi.xposed.sesame.util.TimeUtil
import fansirsqi.xposed.sesame.util.maps.UserMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Collections
import java.util.Date
import java.util.Locale
import java.util.Objects
import java.util.Random
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer
import java.util.function.Supplier
import kotlin.concurrent.Volatile
import kotlin.math.abs
import kotlin.math.min

/// lzw add begin
/**
 * 蚂蚁森林V2
 */
class AntForest : ModelTask(), EnergyCollectCallback {
    private val taskCount = AtomicInteger(0)
    private val isEnergyLoopRunning = AtomicBoolean(false)
    private var selfId: String? = null
    private var tryCountInt: Int? = null
    private var retryIntervalInt: Int? = null
    private var collectIntervalEntity: IntervalLimit? = null
    private var doubleCollectIntervalEntity: IntervalLimit? = null

    /**
     * 双击卡结束时间
     */
    @Volatile
    private var doubleEndTime: Long = 0

    /**
     * 隐身卡结束时间
     */
    @Volatile
    private var stealthEndTime: Long = 0

    /**
     * 保护罩结束时间
     */
    @Volatile
    private var shieldEndTime: Long = 0

    /**
     * 炸弹卡结束时间
     */
    @Volatile
    private var energyBombCardEndTime: Long = 0

    /**
     * 1.1倍能量卡结束时间
     */
    @Volatile
    private var robExpandCardEndTime: Long = 0

    /** lzw add begin */
    @Volatile
    private var monday = false

    /** lzw add end */
    private val delayTimeMath = Average(5)
    private val collectEnergyLockLimit = ObjReference(0L)
    private val doubleCardLockObj = Any()

    // 并发控制信号量，限制同时处理的好友数量，避免过多并发导致性能问题
    // 优化：根据CPU核心数动态计算并发数，避免过度并发
    // 计算公式：核心数 * 8，最小16最大48
    private val optimalConcurrency = kotlin.math.min(48, kotlin.math.max(16, Runtime.getRuntime().availableProcessors() * 8))
    private val concurrencyLimiter = Semaphore(optimalConcurrency)


    private var collectEnergy: BooleanModelField? = null // 收集能量开关
    private var pkEnergy: BooleanModelField? = null // PK能量开关
    private var energyRain: BooleanModelField? = null // 能量雨开关
    private var advanceTime: IntegerModelField? = null // 提前时间（毫秒）
    private var tryCount: IntegerModelField? = null // 尝试收取次数
    private var retryInterval: IntegerModelField? = null // 重试间隔（毫秒）
    private var dontCollectList: SelectModelField? = null // 不收取能量的用户列表
    private var collectWateringBubble: BooleanModelField? = null // 收取浇水金球开关
    private var batchRobEnergy: BooleanModelField? = null // 批量收取能量开关
    private var balanceNetworkDelay: BooleanModelField? = null // 平衡网络延迟开关
    private var closeWhackMole: BooleanModelField? = null // 自动关闭6秒拼手速开关
    private var collectProp: BooleanModelField? = null // 收集道具开关
    private var queryInterval: StringModelField? = null // 查询间隔时间
    private var collectInterval: StringModelField? = null // 收取间隔时间
    private var doubleCollectInterval: StringModelField? = null // 双击间隔时间
    private var doubleCard: ChoiceModelField? = null // 双击卡类型选择
    private var doubleCardTime: ListJoinCommaToStringModelField? = null // 双击卡使用时间列表
    private var doubleCountLimit: IntegerModelField? = null // 双击卡使用次数限制

    /**
     * 获取双击卡使用次数限制
     * @return 双击卡使用次数限制字段
     */
    fun getDoubleCountLimit(): IntegerModelField? {
        return doubleCountLimit
    }
    
    private var doubleCardConstant: BooleanModelField? = null // 双击卡永动机
    private var stealthCard: ChoiceModelField? = null // 隐身卡
    private var stealthCardConstant: BooleanModelField? = null // 隐身卡永动机
    private var shieldCard: ChoiceModelField? = null // 保护罩
    private var shieldCardConstant: BooleanModelField? = null // 限时保护永动机
    private var helpFriendCollectType: ChoiceModelField? = null
    private var helpFriendCollectList: SelectModelField? = null

    /** lzw add begin */
    private var alternativeAccountList: SelectModelField? = null

    // 显示背包内容
    private var showBagList: BooleanModelField? = null

    /** lzw add end */
    private var vitalityExchangeList: SelectAndCountModelField? = null
    private var returnWater33: IntegerModelField? = null
    private var returnWater18: IntegerModelField? = null
    private var returnWater10: IntegerModelField? = null
    private var receiveForestTaskAward: BooleanModelField? = null
    private var waterFriendList: SelectAndCountModelField? = null
    private var waterFriendCount: IntegerModelField? = null
    private var notifyFriend: BooleanModelField? = null
    private var vitalityExchange: BooleanModelField? = null
    private var userPatrol: BooleanModelField? = null
    private var collectGiftBox: BooleanModelField? = null
    private var medicalHealth: BooleanModelField? = null //医疗健康开关
    private var forestMarket: BooleanModelField? = null
    private var combineAnimalPiece: BooleanModelField? = null
    private var consumeAnimalProp: BooleanModelField? = null
    private var whoYouWantToGiveTo: SelectModelField? = null
    private var dailyCheckIn: BooleanModelField? = null //青春特权签到
    private var bubbleBoostCard: ChoiceModelField? = null //加速卡
    private var youthPrivilege: BooleanModelField? = null //青春特权 森林道具
    private var ecoLife: BooleanModelField? = null
    private var ecoLifeTime: StringModelField? = null // 绿色行动执行时间
    private var giveProp: BooleanModelField? = null

    private var robExpandCard: ChoiceModelField? = null //1.1倍能量卡
    private val robExpandCardTime: ListModelField? = null //1.1倍能量卡时间

    private var cycleinterval: IntegerModelField? = null
    private var energyRainChance: BooleanModelField? = null
    private var energyRainTime: StringModelField? = null // 能量雨执行时间

    /**
     * 能量炸弹卡
     */
    private var energyBombCardType: ChoiceModelField? = null

    /**
     * 用户名缓存：userId -> userName 的映射
     */
    private val userNameCache: ConcurrentHashMap<String, String> = ConcurrentHashMap()
    
    /**
     * 已处理用户缓存：记录本轮已处理过的用户ID，避免重复处理
     */
    private val processedUsersCache: ConcurrentHashMap.KeySetView<String, Boolean> = 
        ConcurrentHashMap.newKeySet()

    /**
     * 空森林缓存，用于记录在本轮任务中已经确认没有能量的好友。
     * 在每轮蚂蚁森林任务开始时清空（见run方法finally块）。
     * “一轮任务”通常指由"执行间隔"触发的一次完整的好友遍历。
     */
    private val emptyForestCache: ConcurrentHashMap<String, Long> = ConcurrentHashMap<String, Long>()

    /**
     * 跳过用户缓存，用于记录有保护罩或其他需要跳过的用户
     * Key: 用户ID，Value: 跳过原因（如"baohuzhao"表示有保护罩）
     */
    private val skipUsersCache: ConcurrentHashMap<String, String> = ConcurrentHashMap<String, String>()

    private var forestChouChouLe: BooleanModelField? = null //森林抽抽乐

    /**
     * 加速器定时
     */
    private var bubbleBoostTime: ListJoinCommaToStringModelField? = null

    private val forestTaskTryCount: ConcurrentHashMap<String, AtomicInteger> =
        ConcurrentHashMap<String, AtomicInteger>()

    private var jsonCollectMap: MutableSet<String?> = HashSet()

    var emojiList: ArrayList<String> = ArrayList(
        listOf(
            "🍅", "🍓", "🥓", "🍂", "🍚", "🌰", "🟢", "🌴",
            "🥗", "🧀", "🥩", "🍍", "🌶️", "🍲", "🍆", "🥕",
            "✨", "🍑", "🍘", "🍀", "🥞", "🍈", "🥝", "🧅",
            "🌵", "🌾", "🥜", "🍇", "🌭", "🥑", "🥐", "🥖",
            "🍊", "🌽", "🍉", "🍖", "🍄", "🥚", "🥙", "🥦",
            "🍌", "🍱", "🍏", "🍎", "🌲", "🌿", "🍁", "🍒",
            "🥔", "🌯", "🌱", "🍐", "🍞", "🍳", "🍙", "🍋",
            "🍗", "🌮", "🍃", "🥘", "🥒", "🧄", "🍠", "🥥"
        )
    )
    private val random = Random()

    private var cachedBagObject: JSONObject? = null
    private var lastQueryPropListTime: Long = 0

    override fun getName(): String {
        return "森林"
    }

    override fun getGroup(): ModelGroup {
        return ModelGroup.FOREST
    }

    override fun getIcon(): String {
        return "AntForest.png"
    }

    interface ApplyPropType {
        companion object {
            const val CLOSE: Int = 0
            const val ALL: Int = 1
            const val ONLY_LIMIT_TIME: Int = 2
            val nickNames: Array<String?> = arrayOf<String?>("关闭", "所有道具", "限时道具")
        }
    }

    interface HelpFriendCollectType {
        companion object {
            const val NONE: Int = 0
            const val HELP: Int = 1
            val nickNames: Array<String?> = arrayOf<String?>("关闭", "选中复活", "选中不复活")
        }
    }

    override fun getFields(): ModelFields {
        val modelFields = ModelFields()
        modelFields.addField(
            BooleanModelField(
                "collectEnergy",
                "收集能量 | 开关",
                false
            ).also { collectEnergy = it })
        modelFields.addField(
            BooleanModelField(
                "batchRobEnergy",
                "一键收取 | 开关",
                true  // 默认启用一键收取
            ).also { batchRobEnergy = it })
        modelFields.addField(
            BooleanModelField(
                "pkEnergy",
                "Pk榜收取 | 开关",
                false
            ).also { pkEnergy = it })
        modelFields.addField(
            BooleanModelField(
                "closeWhackMole",
                "自动关闭6秒拼手速 | 开关",
                false
            ).also { closeWhackMole = it })
        modelFields.addField(
            BooleanModelField(
                "energyRain",
                "能量雨 | 开关",
                false
            ).also { energyRain = it })
        modelFields.addField(
            StringModelField(
                "energyRainTime",
                "能量雨 | 默认8点10分后执行",
                "0810"
            ).also { energyRainTime = it })
        modelFields.addField(
            SelectModelField(
                "dontCollectList",
                "不收能量 | 配置列表",
                LinkedHashSet<String?>()
            ) { AlipayUser.getList() }.also {
                dontCollectList = it
            })
        modelFields.addField(
            SelectModelField(
                "giveEnergyRainList",
                "赠送能量雨 | 配置列表",
                LinkedHashSet<String?>()
            ) { AlipayUser.getList() }.also {
                giveEnergyRainList = it
            })
        modelFields.addField(
            BooleanModelField(
                "energyRainChance",
                "兑换使用能量雨次卡 | 开关",
                false
            ).also { energyRainChance = it })
        modelFields.addField(
            BooleanModelField(
                "collectWateringBubble",
                "收取浇水金球 | 开关",
                false
            ).also { collectWateringBubble = it })
        modelFields.addField(
            ChoiceModelField(
                "doubleCard",
                "双击卡开关 | 消耗类型",
                ApplyPropType.CLOSE,
                ApplyPropType.nickNames
            ).also { doubleCard = it })
        modelFields.addField(
            IntegerModelField(
                "doubleCountLimit",
                "双击卡 | 使用次数",
                6
            ).also { doubleCountLimit = it })
        modelFields.addField(
            ListJoinCommaToStringModelField(
                "doubleCardTime", "双击卡 | 使用时间/范围", ListUtil.newArrayList(
                    "0700", "0730", "1200", "1230", "1700", "1730", "2000", "2030", "2359"
                )
            ).also { doubleCardTime = it })
        // 双击卡永动机
        modelFields.addField(
            BooleanModelField("DoubleCardConstant","限时双击永动机 | 开关",false
            ).also { doubleCardConstant = it }
        )

        modelFields.addField(
            ChoiceModelField(
                "bubbleBoostCard",
                "加速器开关 | 消耗类型",
                ApplyPropType.CLOSE,
                ApplyPropType.nickNames
            ).also { bubbleBoostCard = it })

        modelFields.addField(
            ListJoinCommaToStringModelField(
                "bubbleBoostTime", "加速器 | 使用时间/不能范围", ListUtil.newArrayList(
                    "0030,0630",
                    "0700",
                    "1200",
                    "1730",
                    "2359"
                )
            ).also { bubbleBoostTime = it })

        modelFields.addField(
            ChoiceModelField(
                "shieldCard",
                "保护罩开关 | 消耗类型",
                ApplyPropType.CLOSE,
                ApplyPropType.nickNames
            ).also { shieldCard = it })
        modelFields.addField(
            BooleanModelField(
                "shieldCardConstant",
                "限时保护永动机 | 开关",
                false
            ).also { shieldCardConstant = it })

        modelFields.addField(
            ChoiceModelField(
                "energyBombCardType", "炸弹卡开关 | 消耗类型", ApplyPropType.CLOSE,
                ApplyPropType.nickNames, "若开启了保护罩，则不会使用炸弹卡"
            ).also { energyBombCardType = it })

        modelFields.addField(
            ChoiceModelField(
                "robExpandCard",
                "1.1倍能量卡开关 | 消耗类型",
                ApplyPropType.CLOSE,
                ApplyPropType.nickNames
            ).also { robExpandCard = it })

        //1.1倍能量卡时间
        modelFields.addField(
            ListJoinCommaToStringModelField(
                "robExpandCardTime", "1.1倍能量卡 | 使用时间/不能范围",
                ListUtil.newArrayList(
                    "0700",
                    "0730",
                    "1200",
                    "1230",
                    "1700",
                    "1730",
                    "2000",
                    "2030",
                    "2359"
                )
            )
        )

        modelFields.addField(
            ChoiceModelField(
                "stealthCard",
                "隐身卡开关 | 消耗类型",
                ApplyPropType.CLOSE,
                ApplyPropType.nickNames
            ).also { stealthCard = it })
        modelFields.addField(
            BooleanModelField(
                "stealthCardConstant",
                "限时隐身永动机 | 开关",
                false
            ).also { stealthCardConstant = it })

        modelFields.addField(
            IntegerModelField(
                "returnWater10",
                "返水 | 10克需收能量(关闭:0)",
                0
            ).also { returnWater10 = it })
        modelFields.addField(
            IntegerModelField(
                "returnWater18",
                "返水 | 18克需收能量(关闭:0)",
                0
            ).also { returnWater18 = it })
        modelFields.addField(
            IntegerModelField(
                "returnWater33",
                "返水 | 33克需收能量(关闭:0)",
                0
            ).also { returnWater33 = it })
        modelFields.addField(
            SelectAndCountModelField(
                "waterFriendList",
                "浇水 | 好友列表",
                LinkedHashMap<String?, Int?>(),
                { AlipayUser.getList() },
                "设置浇水次数"
            ).also { waterFriendList = it })
        modelFields.addField(
            IntegerModelField(
                "waterFriendCount",
                "浇水 | 克数(10 18 33 66)",
                66
            ).also { waterFriendCount = it })
        modelFields.addField(
            BooleanModelField(
                "notifyFriend",
                "浇水 | 通知好友",
                false
            ).also { notifyFriend = it })
        modelFields.addField(
            BooleanModelField(
                "giveProp",
                "赠送道具",
                false
            ).also { giveProp = it })
        modelFields.addField(
            SelectModelField(
                "whoYouWantToGiveTo",
                "赠送 | 道具",
                LinkedHashSet<String?>(),
                { AlipayUser.getList() },
                "所有可赠送的道具将全部赠"
            ).also { whoYouWantToGiveTo = it })
        modelFields.addField(
            BooleanModelField(
                "collectProp",
                "收集道具",
                false
            ).also { collectProp = it })
        modelFields.addField(
            ChoiceModelField(
                "helpFriendCollectType",
                "复活能量 | 选项",
                HelpFriendCollectType.NONE,
                HelpFriendCollectType.nickNames
            ).also { helpFriendCollectType = it })
        modelFields.addField(
            SelectModelField(
                "helpFriendCollectList",
                "复活能量 | 好友列表",
                LinkedHashSet<String?>()
            ) { AlipayUser.getList() }.also {
                helpFriendCollectList = it
            })
        /** lzw add begin */
        modelFields.addField(
            SelectModelField(
                "alternativeAccountList",
                "小号列表",
                LinkedHashSet<String?>()
            ) { AlipayUser.getList() }.also {
                alternativeAccountList = it
            })
        /** lzw add end */
        modelFields.addField(
            BooleanModelField(
                "vitalityExchange",
                "活力值 | 兑换开关",
                false
            ).also { vitalityExchange = it })
        modelFields.addField(
            SelectAndCountModelField(
                "vitalityExchangeList",
                "活力值 | 兑换列表",
                LinkedHashMap<String?, Int?>(),
                VitalityStore::list,
                "兑换次数"
            ).also { vitalityExchangeList = it })
        modelFields.addField(
            BooleanModelField(
                "userPatrol",
                "保护地巡护",
                false
            ).also { userPatrol = it })
        modelFields.addField(
            BooleanModelField(
                "combineAnimalPiece",
                "合成动物碎片",
                false
            ).also { combineAnimalPiece = it })
        modelFields.addField(
            BooleanModelField(
                "consumeAnimalProp",
                "派遣动物伙伴",
                false
            ).also { consumeAnimalProp = it })
        modelFields.addField(
            BooleanModelField(
                "receiveForestTaskAward",
                "森林任务",
                false
            ).also { receiveForestTaskAward = it })

        modelFields.addField(
            BooleanModelField(
                "forestChouChouLe",
                "森林寻宝任务",
                false
            ).also { forestChouChouLe = it })

        modelFields.addField(
            BooleanModelField(
                "collectGiftBox",
                "领取礼盒",
                false
            ).also { collectGiftBox = it })

        modelFields.addField(
            BooleanModelField(
                "medicalHealth",
                "健康医疗任务 | 开关",
                false
            ).also { medicalHealth = it })
        modelFields.addField(
            SelectModelField(
                "medicalHealthOption",
                "健康医疗 | 选项",
                LinkedHashSet<String?>(),
                listHealthcareOptions(),
                "医疗健康需要先完成一次医疗打卡"
            ).also { medicalHealthOption = it })

        modelFields.addField(
            BooleanModelField(
                "forestMarket",
                "森林集市",
                false
            ).also { forestMarket = it })
        modelFields.addField(
            BooleanModelField(
                "youthPrivilege",
                "青春特权 | 森林道具",
                false
            ).also { youthPrivilege = it })
        modelFields.addField(
            BooleanModelField(
                "studentCheckIn",
                "青春特权 | 签到红包",
                false
            ).also { dailyCheckIn = it })
        modelFields.addField(
            BooleanModelField(
                "ecoLife",
                "绿色行动 | 开关",
                false
            ).also { ecoLife = it })
        modelFields.addField(
            StringModelField(
                "ecoLifeTime",
                "绿色行动 | 默认8点后执行",
                "0800"
            ).also { ecoLifeTime = it })
        modelFields.addField(
            BooleanModelField(
                "ecoLifeOpen",
                "绿色任务 |  自动开通",
                false
            ).also { ecoLifeOpen = it })
        modelFields.addField(
            SelectModelField(
                "ecoLifeOption",
                "绿色行动 | 选项",
                LinkedHashSet<String?>(),
                listEcoLifeOptions(),
                "光盘行动需要先完成一次光盘打卡"
            ).also { ecoLifeOption = it })

        modelFields.addField(
            StringModelField(
                "queryInterval",
                "查询间隔(毫秒或毫秒范围)",
                "1000-2000"
            ).also { queryInterval = it })
        modelFields.addField(
            StringModelField(
                "collectInterval",
                "收取间隔(毫秒或毫秒范围)",
                "1000-1500"
            ).also { collectInterval = it })
        modelFields.addField(
            StringModelField(
                "doubleCollectInterval",
                "双击间隔(毫秒或毫秒范围)",
                "800-2400"
            ).also { doubleCollectInterval = it })
        modelFields.addField(
            BooleanModelField(
                "balanceNetworkDelay",
                "平衡网络延迟",
                true
            ).also { balanceNetworkDelay = it })
        modelFields.addField(
            IntegerModelField(
                "advanceTime",
                "提前时间(毫秒)",
                0,
                Int.MIN_VALUE,
                500
            ).also { advanceTime = it })
        modelFields.addField(
            IntegerModelField(
                "tryCount",
                "尝试收取(次数)",
                1,
                0,
                5
            ).also { tryCount = it })
        modelFields.addField(
            IntegerModelField(
                "retryInterval",
                "重试间隔(毫秒)",
                1200,
                0,
                10000
            ).also { retryInterval = it })
        modelFields.addField(
            IntegerModelField(
                "cycleinterval",
                "循环间隔(毫秒)",
                5000,
                0,
                10000
            ).also { cycleinterval = it })
        modelFields.addField(
            BooleanModelField(
                "showBagList",
                "显示背包内容",
                true
            ).also { showBagList = it })

        return modelFields
    }

    override fun check(): Boolean {
        val currentTime = System.currentTimeMillis()

        // -----------------------------
        // 先更新时间状态，保证 IS_ENERGY_TIME 正确
        // -----------------------------
        TaskCommon.update()

        // 1️⃣ 异常等待状态
        val forestPauseTime =
            RuntimeInfo.getInstance().getLong(RuntimeInfo.RuntimeInfoKey.ForestPauseTime)
        if (forestPauseTime > currentTime) {
            Log.record(name + "任务-异常等待中，暂不执行检测！")
            return false
        }
        // 2️⃣ 模块休眠时间
        if (TaskCommon.IS_MODULE_SLEEP_TIME) {
            Log.record(
                TAG,
                "💤 模块休眠时间【" + BaseModel.modelSleepTime
                    .value + "】停止执行" + name + "任务！"
            )
            return false
        }
        // -----------------------------
        // 3️⃣ 只收能量时间段判断
        // -----------------------------
        val now = Calendar.getInstance()
        val hour = now.get(Calendar.HOUR_OF_DAY)
        val minute = now.get(Calendar.MINUTE)
        val isEnergyTime = TaskCommon.IS_ENERGY_TIME || hour == 7 && minute < 30
        if (isEnergyTime) {
            // 关键改动：将循环放入后台线程，避免阻塞TaskRunner
            GlobalThreadPools.execute({ this.startEnergyCollectionLoop() })
            return false // 只收能量期间不执行正常任务，check()立刻返回
        }
        return true
    }

    /**
     * 只收能量时间的循环任务（协程版本）
     */
    private fun startEnergyCollectionLoop() {
        if (!isEnergyLoopRunning.compareAndSet(false, true)) {
            Log.record(TAG, "只收能量循环任务已在运行中，跳过重复启动。")
            return
        }
        try {
            val energyTimeStr = BaseModel.energyTime.value.toString()
            Log.record(
                TAG,
                "⏸ 当前为只收能量时间【$energyTimeStr】，开始循环收取自己、好友和PK好友的能量"
            )
            runBlocking {
                try {
                    while (true) {
                        // 每次循环更新状态
                        TaskCommon.update()
                        // 如果不在能量时间段，退出循环
                        val now = Calendar.getInstance()
                        val hour = now.get(Calendar.HOUR_OF_DAY)
                        val minute = now.get(Calendar.MINUTE)
                        if (!(TaskCommon.IS_ENERGY_TIME || hour == 7 && minute < 30)) {
                            Log.record(TAG, "当前不在只收能量时间段，退出循环")
                            break
                        }

                        // 收取自己能量（协程中执行）
                        Log.record(TAG, "🌳 开始收取自己的能量...")
                        val selfHomeObj = querySelfHome()
                        if (selfHomeObj != null) {
                            collectEnergy(UserMap.currentUid, selfHomeObj, "self")
                            Log.record(TAG, "✅ 收取自己的能量完成")
                        } else {
                            Log.error(TAG, "❌ 获取自己主页信息失败，跳过能量收取")
                        }

                        // 只收能量时间段，启用循环查找能量功能
                        Log.record(TAG, "👥 开始执行查找能量...")
                        try {
                            collectEnergyByTakeLook() // 查找能量（协程）
                        } catch (e: CancellationException) {
                            Log.runtime(TAG, "查找能量被取消，退出循环")
                            break
                        }

                        // 循环间隔（使用协程延迟）
                        val sleepMillis = cycleinterval!!.value.toLong()
                        Log.record(TAG, "✨ 只收能量时间一轮完成，等待 $sleepMillis 毫秒后开始下一轮")
                        GlobalThreadPools.sleepCompat(sleepMillis)
                    }
                } catch (e: CancellationException) {
                    Log.runtime(TAG, "只收能量循环被取消")
                }
            }
        } finally {
            Log.record(TAG, "🏁 只收能量时间循环结束")
            isEnergyLoopRunning.set(false)
        }
    }

    /**
     * 创建区间限制对象
     *
     * @param intervalStr 区间字符串，如 "1000-2000"
     * @param defaultMin 默认最小值
     * @param defaultMax 默认最大值
     * @param description 描述，用于日志
     * @return 区间限制对象
     */
    private fun createSafeIntervalLimit(
        intervalStr: String?,
        defaultMin: Int,
        defaultMax: Int,
        description: String?
    ): FixedOrRangeIntervalLimit {
        // 记录原始输入值
        Log.record(TAG, description + "原始设置值: [" + intervalStr + "]")

        // 使用自定义区间限制类，处理所有边界情况
        val limit = FixedOrRangeIntervalLimit(intervalStr, defaultMin, defaultMax)
        Log.record(TAG, description + "成功创建区间限制")
        return limit
    }

    override fun boot(classLoader: ClassLoader?) {
        super.boot(classLoader)


        // 安全创建各种区间限制
        val queryIntervalLimit = createSafeIntervalLimit(
            queryInterval!!.value, 10, 10000, "查询间隔"
        )

        // 添加RPC间隔限制
        addIntervalLimit("alipay.antforest.forest.h5.queryHomePage", queryIntervalLimit)
        addIntervalLimit("alipay.antforest.forest.h5.queryFriendHomePage", queryIntervalLimit)
        addIntervalLimit("alipay.antmember.forest.h5.collectEnergy", 300)
        addIntervalLimit("alipay.antmember.forest.h5.queryEnergyRanking", 300)
        addIntervalLimit("alipay.antforest.forest.h5.fillUserRobFlag", 500)

        // 设置其他参数
        tryCountInt = tryCount!!.value
        retryIntervalInt = retryInterval!!.value
        advanceTime!!.value


        jsonCollectMap = dontCollectList!!.value

        // 创建收取间隔实体
        collectIntervalEntity = createSafeIntervalLimit(
            collectInterval!!.value, 50, 10000, "收取间隔"
        )

        // 创建双击收取间隔实体
        doubleCollectIntervalEntity = createSafeIntervalLimit(
            doubleCollectInterval!!.value, 10, 5000, "双击间隔"
        )
        delayTimeMath.clear()


        AntForestRpcCall.init()

        // 设置蹲点管理器的回调
        EnergyWaitingManager.setEnergyCollectCallback(this)
    }

    override suspend fun runSuspend() {
        val runStartTime = System.currentTimeMillis()
        Log.record(TAG, "🌲🌲🌲 森林主任务开始执行 🌲🌲🌲")
        try {
            // 每次运行时检查并更新计数器
            checkAndUpdateCounters()
            
            // 优化：移除午夜任务，避免重复执行和耗时
            // 正常流程会自动处理所有收取任务，无需特殊处理

            errorWait = false

            // 计数器和时间记录
            monday = true
            val tc = TimeCounter(TAG)

            if (showBagList!!.value) showBag()

            Log.record(TAG, "执行开始-蚂蚁$name")
            taskCount.set(0)
            selfId = UserMap.currentUid

            // -------------------------------
            // 自己使用道具
            // -------------------------------
            // 先查询主页，更新道具状态（双击卡、保护罩等的剩余时间）
            updateSelfHomePage()
            tc.countDebug("查询道具状态")
            
            usePropBeforeCollectEnergy(selfId)
            tc.countDebug("使用自己道具卡")

            // -------------------------------
            // 收PK好友能量
            // -------------------------------
            Log.runtime(TAG, "🚀 异步执行PK好友能量收取")
            collectPKEnergyCoroutine()  // 好友道具在 collectFriendEnergy 内会自动处理
            tc.countDebug("收PK好友能量（同步）")

            // -------------------------------
            // 收自己能量
            // -------------------------------
            Log.record(TAG, "🌳 【正常流程】开始收取自己的能量...")
            val selfHomeObj = run {
                val obj = querySelfHome()
                tc.countDebug("获取自己主页对象信息")
                if (obj != null) {
                    collectEnergy(UserMap.currentUid, obj, "self")
                    Log.record(TAG, "✅ 【正常流程】收取自己的能量完成")
                    tc.countDebug("收取自己的能量")
                } else {
                    Log.error(TAG, "❌ 【正常流程】获取自己主页信息失败，跳过能量收取")
                    tc.countDebug("跳过自己的能量收取（主页获取失败）")
                }
                obj
            }

            // -------------------------------
            // 收好友能量
            // -------------------------------
            // 先尝试使用找能量功能快速定位有能量的好友（协程）
            Log.runtime(TAG, "🚀 执行找能量功能（协程）")
            collectEnergyByTakeLook()
            tc.countDebug("找能量收取（协程）")

            // 然后执行传统的好友排行榜收取（协程）
            Log.runtime(TAG, "🚀 执行好友能量收取（协程）")
            collectFriendEnergyCoroutine() // 内部会自动调用 usePropBeforeCollectEnergy(userId, false)
            tc.countDebug("收取好友能量（同步）")

            // -------------------------------
            // 后续任务流程
            // -------------------------------
            if (selfHomeObj != null) {
                if (collectWateringBubble!!.value) {
                    wateringBubbles(selfHomeObj)
                    tc.countDebug("收取浇水金球")
                }
                if (collectProp!!.value) {
                    givenProps(selfHomeObj)
                    tc.countDebug("收取道具")
                }
                if (userPatrol!!.value) {
                    queryUserPatrol()
                    tc.countDebug("动物巡护任务")
                }
                if (canConsumeAnimalProp && consumeAnimalProp!!.value) {
                    queryAndConsumeAnimal()
                    tc.countDebug("森林巡护")
                } else {
                    Log.record("已经有动物伙伴在巡护森林~")
                }

                handleUserProps(selfHomeObj)
                tc.countDebug("收取动物派遣能量")

                if (combineAnimalPiece!!.value) {
                    queryAnimalAndPiece()
                    tc.countDebug("合成动物碎片")
                }

                if (receiveForestTaskAward!!.value) {
                    receiveTaskAward()
                    tc.countDebug("森林任务")
                }
                if (ecoLife!!.value) {
                    // 检查是否到达执行时间
                    if (TaskTimeChecker.isTimeReached(ecoLifeTime?.value, "0800")) {
                        EcoLife.ecoLife()
                        tc.countDebug("绿色行动")
                    } else {
                        Log.record(TAG, "绿色行动未到执行时间，跳过")
                    }
                }

                waterFriends()
                tc.countDebug("给好友浇水")

                if (giveProp!!.value) {
                    giveProp()
                    tc.countDebug("赠送道具")
                }

                if (vitalityExchange!!.value) {
                    handleVitalityExchange()
                    tc.countDebug("活力值兑换")
                }

                if (energyRain!!.value) {
                    // 检查是否到达执行时间
                    if (TaskTimeChecker.isTimeReached(energyRainTime?.value, "0810")) {
                        EnergyRainCoroutine.execEnergyRainCompat()
                        if (energyRainChance!!.value) {
                            useEnergyRainChanceCard()
                            tc.countDebug("使用能量雨卡")
                        }
                        tc.countDebug("能量雨")
                    } else {
                        Log.record(TAG, "能量雨未到执行时间，跳过")
                    }
                }

                if (forestMarket!!.value) {
                    GreenLife.ForestMarket("GREEN_LIFE")
                    GreenLife.ForestMarket("ANTFOREST")
                    tc.countDebug("森林集市")
                }

                if (medicalHealth!!.value) {
                    if (medicalHealthOption!!.value.contains("FEEDS")) {
                        Healthcare.queryForestEnergy("FEEDS")
                        tc.countDebug("绿色医疗")
                    }
                    if (medicalHealthOption!!.value.contains("BILL")) {
                        Healthcare.queryForestEnergy("BILL")
                        tc.countDebug("电子小票")
                    }
                }

                //青春特权森林道具领取
                if (youthPrivilege!!.value) {
                    youthPrivilege()
                }

                if (dailyCheckIn!!.value) {
                    studentSignInRedEnvelope()
                }

                if (forestChouChouLe!!.value) {
                    val chouChouLe = ForestChouChouLe()
                    chouChouLe.chouChouLe()
                    tc.countDebug("抽抽乐")
                }

                tc.stop()
            }
        } catch (e: CancellationException) {
            // 协程被取消是正常行为，不记录错误日志
            Log.runtime(TAG, "蚂蚁森林任务协程被取消")
            throw e // 重新抛出，让协程系统处理
        } catch (t: Throwable) {
            Log.printStackTrace(TAG, "执行蚂蚁森林任务时发生错误: ", t)
        } finally {
            // 计算总耗时
            val totalTime = System.currentTimeMillis() - runStartTime
            val timeInSeconds = totalTime / 1000
            
            // 优化：不再等待蹲点任务完成，让主任务立即结束
            // 蹲点任务会在后台独立协程中继续运行，不影响其他模块
            val waitingTaskCount = EnergyWaitingManager.getWaitingTaskCount()
            
            Log.record(TAG, "=" .repeat(50))
            Log.record(TAG, "🌲🌲🌲 森林主任务执行完毕 🌲🌲🌲")
            Log.record(TAG, "⏱️ 主任务耗时: ${timeInSeconds}秒 (${totalTime}ms)")
            Log.record(TAG, "📊 收取统计: 收${totalCollected}g 帮${TOTAL_HELP_COLLECTED}g 浇${TOTAL_WATERED}g")
            if (waitingTaskCount > 0) {
                Log.record(TAG, "⏰ 后台蹲点任务: $waitingTaskCount 个 (将在指定时间自动收取)")
                // 输出详细的蹲点任务状态，帮助调试
                val taskStatus = EnergyWaitingManager.getWaitingTasksStatus()
                Log.record(TAG, "📋 $taskStatus")
            } else {
                Log.record(TAG, "✅ 无后台蹲点任务")
            }
            Log.record(TAG, "=" .repeat(50))
            
            userNameCache.clear()
            processedUsersCache.clear()
            // 清空本轮的空森林缓存，以便下一轮（如下次"执行间隔"到达）重新检查所有好友
            emptyForestCache.clear()
            // 清空跳过用户缓存，下一轮重新检测保护罩状态
            skipUsersCache.clear()
            // 清空好友主页缓存
            val strTotalCollected =
                "本次总 收:" + totalCollected + "g 帮:" + TOTAL_HELP_COLLECTED + "g 浇:" + TOTAL_WATERED + "g"
            updateLastExecText(strTotalCollected)
            
            // 打印森林统计报告
            try {
                ForestFriendManager.printReport()
            } catch (e: Exception) {
                Log.printStackTrace(TAG, "打印森林统计报告失败", e)
            }
            
            // 打印能量收取优化统计（使用try-catch确保不影响主流程）
            try {
                EnergyCollectionOptimizer.printStats()
            } catch (e: Exception) {
                Log.error(TAG, "打印优化统计失败: ${e.message}")
            }
        }
    }

    /**
     * 每日重置
     */
    // 上次检查的日期（用于判断是否跨天）
    private var lastCheckDate: String? = null
    
    private fun checkAndUpdateCounters() {
        val today = TimeUtil.getDateStr() // 获取当前日期，如 "2025-10-07"

        // 只在日期变化时重置计数器（跨天）
        if (lastCheckDate != today) {
            resetTaskCounters()
            lastCheckDate = today
            Log.record(TAG, "✅ 检测到新的一天[$today]，重置计数器")
        }
    }


    // 重置任务计数器（你需要根据具体任务的计数器来调整）
    private fun resetTaskCounters() {
        taskCount.set(0) // 重置任务计数
        // 每日重置时清空频率限制记录，让所有好友都有新的机会
        ForestUtil.clearAllFrequencyLimits()
        Log.record(TAG, "任务计数器已重置")
    }

    /**
     * 定义一个 处理器接口
     */
    private fun interface JsonArrayHandler {
        fun handle(array: JSONArray?)
    }

    private fun processJsonArray(
        initialObj: JSONObject?,
        arrayKey: String?,
        handler: JsonArrayHandler
    ) {
        var hasMore: Boolean
        var currentObj = initialObj
        do {
            val jsonArray = currentObj?.optJSONArray(arrayKey)
            if (jsonArray != null && jsonArray.length() > 0) {
                handler.handle(jsonArray)
                // 判断是否还有更多数据（比如返回满20个）
                hasMore = jsonArray.length() >= 20
            } else {
                hasMore = false
            }
            if (hasMore) {
                GlobalThreadPools.sleepCompat(2000L) // 防止请求过快被限制
                currentObj = querySelfHome() // 获取下一页数据
            }
        } while (hasMore)
    }

    private fun wateringBubbles(selfHomeObj: JSONObject?) {
        processJsonArray(
            selfHomeObj,
            "wateringBubbles"
        ) { wateringBubbles: JSONArray? ->
            this.collectWateringBubbles(
                wateringBubbles!!
            )
        }
    }

    private fun givenProps(selfHomeObj: JSONObject?) {
        processJsonArray(selfHomeObj, "givenProps") { givenProps: JSONArray? ->
            this.collectGivenProps(
                givenProps!!
            )
        }
    }

    /**
     * 收取回赠能量，好友浇水金秋，好友复活能量
     *
     * @param wateringBubbles 包含不同类型金球的对象数组
     */
    private fun collectWateringBubbles(wateringBubbles: JSONArray) {
        for (i in 0..<wateringBubbles.length()) {
            try {
                val wateringBubble = wateringBubbles.getJSONObject(i)
                when (val bizType = wateringBubble.getString("bizType")) {
                    "jiaoshui" -> collectWater(wateringBubble)
                    "fuhuo" -> collectRebornEnergy()
                    "baohuhuizeng" -> collectReturnEnergy(wateringBubble)
                    else -> {
                        Log.record(TAG, "未知bizType: $bizType")
                        continue
                    }
                }
                GlobalThreadPools.sleepCompat(1000L)
            } catch (e: JSONException) {
                Log.record(TAG, "浇水金球JSON解析错误: " + e.message)
            } catch (e: RuntimeException) {
                Log.record(TAG, "浇水金球处理异常: " + e.message)
            }
        }
    }

    private fun collectWater(wateringBubble: JSONObject) {
        try {
            val id = wateringBubble.getLong("id")
            val response = AntForestRpcCall.collectEnergy("jiaoshui", selfId, id)
            processCollectResult(response, "收取金球🍯浇水")
        } catch (e: JSONException) {
            Log.record(TAG, "收取浇水JSON解析错误: " + e.message)
        }
    }

    private fun collectRebornEnergy() {
        try {
            val response = AntForestRpcCall.collectRebornEnergy()
            processCollectResult(response, "收取金球🍯复活")
        } catch (e: RuntimeException) {
            Log.record(TAG, "收取金球运行时异常: " + e.message)
        }
    }

    private fun collectReturnEnergy(wateringBubble: JSONObject) {
        try {
            val friendId = wateringBubble.getString("userId")
            val id = wateringBubble.getLong("id")
            val response = AntForestRpcCall.collectEnergy("baohuhuizeng", selfId, id)
            processCollectResult(
                response,
                "收取金球🍯[" + UserMap.getMaskName(friendId) + "]复活回赠"
            )
        } catch (e: JSONException) {
            Log.record(TAG, "收取金球回赠JSON解析错误: " + e.message)
        }
    }

    /**
     * 处理金球-浇水、收取结果
     *
     * @param response       收取结果
     * @param successMessage 成功提示信息
     */
    private fun processCollectResult(response: String, successMessage: String?) {
        try {
            val joEnergy = JSONObject(response)
            if (ResChecker.checkRes(TAG + "收集能量失败:", joEnergy)) {
                val bubbles = joEnergy.getJSONArray("bubbles")
                if (bubbles.length() > 0) {
                    val collected = bubbles.getJSONObject(0).getInt("collectedEnergy")
                    if (collected > 0) {
                        val msg = successMessage + "[" + collected + "g]"
                        Log.forest(msg)
                        Toast.show(msg)
                    } else {
                        Log.record(successMessage + "失败")
                    }
                } else {
                    Log.record(successMessage + "失败: 未找到金球信息")
                }
            } else {
                Log.record(successMessage + "失败:" + joEnergy.getString("resultDesc"))
                Log.runtime(response)
            }
        } catch (e: JSONException) {
            Log.runtime(TAG, "JSON解析错误: " + e.message)
        } catch (e: Exception) {
            Log.runtime(TAG, "处理收能量结果错误: " + e.message)
        }
    }

    /**
     * 领取道具
     *
     * @param givenProps 给的道具
     */
    private fun collectGivenProps(givenProps: JSONArray) {
        try {
            for (i in 0..<givenProps.length()) {
                val jo = givenProps.getJSONObject(i)
                val giveConfigId = jo.getString("giveConfigId")
                val giveId = jo.getString("giveId")
                val propConfig = jo.getJSONObject("propConfig")
                val propName = propConfig.getString("propName")
                try {
                    val response = AntForestRpcCall.collectProp(giveConfigId, giveId)
                    val responseObj = JSONObject(response)
                    if (ResChecker.checkRes(TAG + "领取道具失败:", responseObj)) {
                        val str = "领取道具🎭[$propName]"
                        Log.forest(str)
                        Toast.show(str)
                    } else {
                        Log.record(
                            TAG,
                            "领取道具🎭[" + propName + "]失败:" + responseObj.getString("resultDesc")
                        )
                        Log.runtime(response)
                    }
                } catch (e: Exception) {
                    Log.record(TAG, "领取道具时发生错误: " + e.message)
                    Log.printStackTrace(e)
                }
                GlobalThreadPools.sleepCompat(1000L)
            }
        } catch (e: JSONException) {
            Log.record(TAG, "givenProps JSON解析错误: " + e.message)
            Log.printStackTrace(e)
        }
    }

    /**
     * 处理用户派遣道具, 如果用户有派遣道具，则收取派遣动物滴能量
     *
     * @param selfHomeObj 用户主页信息的JSON对象
     */
    private fun handleUserProps(selfHomeObj: JSONObject) {
        try {
            val usingUserProps = selfHomeObj.optJSONArray("usingUserPropsNew")
            if (usingUserProps == null || usingUserProps.length() == 0) {
                return  // 如果没有使用中的用户道具，直接返回
            }
            //            Log.runtime(TAG, "尝试遍历使用中的道具:" + usingUserProps);
            for (i in 0..<usingUserProps.length()) {
                val jo = usingUserProps.getJSONObject(i)
                if ("animal" != jo.getString("propGroup")) {
                    continue  // 如果当前道具不是动物类型，跳过
                }
                val extInfo = JSONObject(jo.getString("extInfo"))
                if (extInfo.optBoolean("isCollected")) {
                    Log.runtime(TAG, "动物派遣能量已被收取")
                    continue  // 如果动物能量已经被收取，跳过
                }
                canConsumeAnimalProp = false // 设置标志位，表示不可再使用动物道具
                val propId = jo.getString("propId")
                val propType = jo.getString("propType")
                val shortDay = extInfo.getString("shortDay")
                val animalName = extInfo.getJSONObject("animal").getString("name")
                val response = AntForestRpcCall.collectAnimalRobEnergy(propId, propType, shortDay)
                val responseObj = JSONObject(response)
                if (ResChecker.checkRes(TAG + "收取动物派遣能量失败:", responseObj)) {
                    val energy = extInfo.optInt("energy", 0)
                    totalCollected += energy
                    val str = "收取[" + animalName + "]派遣能量🦩[" + energy + "g]"
                    Toast.show(str)
                    Log.forest(str)
                } else {
                    Log.record(TAG, "收取动物能量失败: " + responseObj.getString("resultDesc"))
                    Log.runtime(response)
                }
                GlobalThreadPools.sleepCompat(300L)
                break // 收取到一个动物能量后跳出循环
            }
        } catch (e: JSONException) {
            Log.printStackTrace(e)
        } catch (e: Exception) {
            Log.runtime(TAG, "handleUserProps err")
            Log.printStackTrace(e)
        }
    }

    /**
     * 给好友浇水
     */
    private fun waterFriends() {
        try {
            val friendMap = waterFriendList!!.value
            val notify = notifyFriend!!.value // 获取通知开关状态

            for (friendEntry in friendMap.entries) {
                val uid = friendEntry.key
                if (selfId == uid) {
                    continue
                }
                var waterCount = friendEntry.value
                if (waterCount == null || waterCount <= 0) {
                    continue
                }
                waterCount = min(waterCount, 3)

                if (Status.canWaterFriendToday(uid, waterCount)) {
                    try {
                        val response = AntForestRpcCall.queryFriendHomePage(uid, null)
                        val jo = JSONObject(response)
                        if (ResChecker.checkRes(TAG, jo)) {
                            val bizNo = jo.getString("bizNo")

                            // ✅ 关键改动：传入通知开关
                            val waterCountKVNode = returnFriendWater(
                                uid, bizNo, waterCount, waterFriendCount!!.value, notify
                            )

                            val actualWaterCount: Int = waterCountKVNode.key!!
                            if (actualWaterCount > 0) {
                                Status.waterFriendToday(uid, actualWaterCount)
                            }
                            if (java.lang.Boolean.FALSE == waterCountKVNode.value) {
                                break
                            }
                        } else {
                            Log.record(jo.getString("resultDesc"))
                        }
                    } catch (e: JSONException) {
                        Log.runtime(TAG, "waterFriends JSON解析错误: " + e.message)
                    } catch (t: Throwable) {
                        Log.printStackTrace(TAG, t)
                    }
                }
            }
        } catch (e: Exception) {
            Log.record(TAG, "未知错误: " + e.message)
            Log.printStackTrace(TAG, e)
        }
    }

    private fun handleVitalityExchange() {
        try {
//            JSONObject bag = getBag();

            Vitality.initVitality("SC_ASSETS")
            val exchangeList = vitalityExchangeList!!.value
            //            Map<String, Integer> maxLimitList = vitalityExchangeMaxList.value;
            for (entry in exchangeList.entries) {
                val skuId = entry.key
                val count = entry.value
                if (count == null || count <= 0) {
                    Log.record(TAG, "无效的count值: skuId=$skuId, count=$count")
                    continue
                }
                // 处理活力值兑换
                while (Status.canVitalityExchangeToday(skuId, count)) {
                    if (!Vitality.handleVitalityExchange(skuId)) {
                        Log.record(TAG, "活力值兑换失败: " + getNameById(skuId))
                        break
                    }
                    GlobalThreadPools.sleepCompat(5000L)
                }
            }
        } catch (t: Throwable) {
            handleException("handleVitalityExchange", t)
        }
    }

    private fun notifyMain() {
        if (taskCount.decrementAndGet() < 1) {
            synchronized(this@AntForest) {
                (this@AntForest as Object).notifyAll()
            }
        }
    }

    /**
     * 获取自己主页对象信息
     *
     * @return 用户的主页信息，如果发生错误则返回null。
     */
    private fun querySelfHome(): JSONObject? {
        var userHomeObj: JSONObject? = null
        try {
            val start = System.currentTimeMillis()
            val response = AntForestRpcCall.queryHomePage()
            if (response.trim { it <= ' ' }.isEmpty()) {
                Log.error(TAG, "获取自己主页信息失败：响应为空$response")
                return null
            }

            userHomeObj = JSONObject(response)

            // 检查响应是否成功
            if (!ResChecker.checkRes(TAG + "查询自己主页失败:", userHomeObj)) {
                Log.error(
                    TAG,
                    "查询自己主页失败: " + userHomeObj.optString("resultDesc", "未知错误")
                )
                return null
            }

            updateSelfHomePage(userHomeObj)
            val end = System.currentTimeMillis()
            // 安全获取服务器时间，如果没有则使用当前时间
            val serverTime = userHomeObj.optLong("now", System.currentTimeMillis())
            val offsetTime = offsetTimeMath.nextInteger(((start + end) / 2 - serverTime).toInt())
            Log.runtime(TAG, "服务器时间：$serverTime，本地与服务器时间差：$offsetTime")
        } catch (t: Throwable) {
            Log.printStackTrace(TAG, "查询自己主页异常", t)
        }
        return userHomeObj
    }

    /**
     * 更新好友主页信息
     *
     * @param userId 好友ID
     * @return 更新后的好友主页信息，如果发生错误则返回null。
     */
    private fun queryFriendHome(userId: String?, fromAct: String?): JSONObject? {
        var friendHomeObj: JSONObject? = null
        try {
            val start = System.currentTimeMillis()
            val response = AntForestRpcCall.queryFriendHomePage(userId, fromAct)
            if (response.trim { it <= ' ' }.isEmpty()) {
                Log.error(
                    TAG,
                    "获取好友主页信息失败：响应为空, userId: " + UserMap.getMaskName(userId) + "，可能是网络问题或用户不存在"
                )
                // 添加延迟避免触发限流
                GlobalThreadPools.sleepCompat(800L)
                return null
            }

            friendHomeObj = JSONObject(response)
            // 检查响应是否成功
            if (!ResChecker.checkRes(TAG + "查询好友主页失败:", friendHomeObj)) {
                // 检测并记录"手速太快"错误，避免日志刷屏
                ForestUtil.checkAndRecordFrequencyError(userId, friendHomeObj)
                return null
            }
            val end = System.currentTimeMillis()
            // 安全获取服务器时间，如果没有则使用当前时间
            val serverTime = friendHomeObj.optLong("now", System.currentTimeMillis())
            val offsetTime = offsetTimeMath.nextInteger(((start + end) / 2 - serverTime).toInt())
            Log.runtime(TAG, "服务器时间：$serverTime，本地与服务器时间差：$offsetTime")
        } catch (t: Throwable) {
            Log.printStackTrace(TAG, "查询好友主页异常, userId: " + UserMap.getMaskName(userId), t)
        }
        return friendHomeObj // 返回用户主页对象
    }

    /**
     * 格式化时间差为人性化的字符串（保持向后兼容）
     * @param milliseconds 时差毫秒
     */
    private fun formatTimeDifference(milliseconds: Long): String {
        return TimeFormatter.formatTimeDifference(milliseconds)
    }

    /**
     * 收集能量前，是否执行拼手速操作（协程版本）
     *
     * @return 首次收取后用户的能量信息，如果发生错误则返回null。
     */
    private fun collectSelfEnergy(): JSONObject? {
        try {
            val selfHomeObj = querySelfHome()
            if (selfHomeObj != null) {
                if (closeWhackMole!!.value) {
                    val propertiesObject = selfHomeObj.optJSONObject("properties")
                    if (propertiesObject != null) {
                        // 如果用户主页的属性中标记了"whackMole"
                        if ("Y" == propertiesObject.optString("whackMoleEntry")) {
                            // 尝试关闭"6秒拼手速"功能
                            val success = WhackMole.closeWhackMole()
                            Log.record(if (success) "6秒拼手速关闭成功" else "6秒拼手速关闭失败")
                        }
                    }
                }
                val nextAction = selfHomeObj.optString("nextAction")
                if ("WhackMole".equals(nextAction, ignoreCase = true)) {
                    Log.record(TAG, "检测到6秒拼手速强制弹窗，先执行拼手速")
                    WhackMole.startWhackMole()
                }
                return collectEnergy(UserMap.currentUid, selfHomeObj, "self")
            }
        } catch (t: Throwable) {
            Log.printStackTrace(t)
        }
        return null
    }

    /**
     * 收取用户的蚂蚁森林能量。
     *
     * @param userId      用户ID
     * @param userHomeObj 用户主页的JSON对象，包含用户的蚂蚁森林信息
     * @return 更新后的用户主页JSON对象，如果发生异常返回null
     */
    private fun collectEnergy(
        userId: String?,
        userHomeObj: JSONObject?,
        fromTag: String?
    ): JSONObject? {
        try {
            if (userHomeObj == null) {
                return null
            }
            // 1. 检查接口返回是否成功
            if (!ResChecker.checkRes(TAG + "载入用户主页失败:", userHomeObj)) {
                Log.debug(TAG, "载入失败: " + userHomeObj.optString("resultDesc", "未知错误"))
                return userHomeObj
            }
            val serverTime = userHomeObj.optLong("now", System.currentTimeMillis())
            val isSelf = userId == UserMap.currentUid

            // 自己的能量不受缓存限制，好友的能量检查缓存避免重复处理
            if (!isSelf && !userId.isNullOrEmpty() && processedUsersCache.contains(userId)) {
                return userHomeObj
            }
            
            // 标记用户为已处理（无论是否成功收取能量）
            if (!isSelf && !userId.isNullOrEmpty()) {
                processedUsersCache.add(userId)
            }
            val userName = getAndCacheUserName(userId, userHomeObj, fromTag)

            // 3. 判断是否允许收取能量
            if (!collectEnergy!!.value || jsonCollectMap.contains(userId)) {
                Log.debug(TAG, "[$userName] 不允许收取能量，跳过")
                return userHomeObj
            }
            // 4. 获取所有可收集的能量球
            val availableBubbles: MutableList<Long> = ArrayList()
            extractBubbleInfo(userHomeObj, serverTime, availableBubbles, userId)
            if (availableBubbles.isEmpty()) {
                // 记录空森林的时间戳，避免本轮重复检查
                if (!userId.isNullOrEmpty()) {
                    emptyForestCache[userId] = System.currentTimeMillis()
                }
                return userHomeObj
            }
            // 检查是否有能量罩保护（影响当前收取）
            var hasProtection = false
            if (!isSelf) {
                if (hasShield(userHomeObj, serverTime)) {
                    hasProtection = true
                    Log.record(TAG, "[$userName]被能量罩❤️保护着哟，跳过收取 状态判断:${false} $userId == ${UserMap.currentUid}")
                }
                if (hasBombCard(userHomeObj, serverTime)) {
                    hasProtection = true
                    Log.record(TAG, "[$userName]开着炸弹卡💣，跳过收取")
                }
            }
            // 7. 只有没有保护时才收集当前可用能量
            if (!hasProtection) {
                collectVivaEnergy(userId, userHomeObj, availableBubbles,  fromTag)
            }

            return userHomeObj
        } catch (e: JSONException) {
            Log.printStackTrace(TAG, "collectUserEnergy JSON解析错误", e)
            return null
        } catch (e: NullPointerException) {
            Log.printStackTrace(TAG, "collectUserEnergy JSON解析错误", e)
            return null
        } catch (t: Throwable) {
            Log.printStackTrace(TAG, "collectUserEnergy 出现异常", t)
            return null
        }
    }

    /**
     * 检查保护罩是否覆盖能量成熟期
     * 
     * @param userHomeObj 用户主页对象
     * @param produceTime 能量成熟时间
     * @param serverTime 服务器时间
     * @return true表示应该跳过蹲点（保护罩覆盖），false表示可以蹲点
     */
    private fun shouldSkipWaitingTaskDueToProtection(
        userHomeObj: JSONObject,
        produceTime: Long,
        serverTime: Long
    ): Boolean {
        val shieldEndTime = ForestUtil.getShieldEndTime(userHomeObj)
        val bombEndTime = ForestUtil.getBombCardEndTime(userHomeObj)
        val protectionEndTime = maxOf(shieldEndTime, bombEndTime)
        return protectionEndTime > produceTime
    }

    /**
     * 提取能量球状态
     *
     * @param userHomeObj      用户主页的JSON对象
     * @param serverTime       服务器时间
     * @param availableBubbles 可收集的能量球ID列表
     * @param userId          用户ID
     * @throws JSONException JSON解析异常
     */
    @Throws(JSONException::class)
    private fun extractBubbleInfo(
        userHomeObj: JSONObject,
        serverTime: Long,
        availableBubbles: MutableList<Long>,
        userId: String?
    ) {
        if (!userHomeObj.has("bubbles")) return
        val jaBubbles = userHomeObj.getJSONArray("bubbles")
        if (jaBubbles.length() == 0) return

        val userName = getAndCacheUserName(userId, userHomeObj, null)
        var waitingBubblesCount = 0

        val isSelf = selfId == userId
        var protectionLog = ""
        if (!isSelf) {
            val shieldEndTime = ForestUtil.getShieldEndTime(userHomeObj)
            val bombEndTime = ForestUtil.getBombCardEndTime(userHomeObj)
            val hasShield = shieldEndTime > serverTime
            val hasBomb = bombEndTime > serverTime
            if (hasShield || hasBomb) {
                if (hasShield) {
                    val remainingTime = formatTimeDifference(shieldEndTime - serverTime)
                    protectionLog += " 保护罩剩余: $remainingTime. "
                }
                if (hasBomb) {
                    val remainingTime = formatTimeDifference(bombEndTime - serverTime)
                    protectionLog += " 炸弹卡剩余: $remainingTime."
                }
            }
        }

        for (i in 0..<jaBubbles.length()) {
            val bubble = jaBubbles.getJSONObject(i)
            val bubbleId = bubble.getLong("id")
            val statusStr = bubble.getString("collectStatus")
            val status = CollectStatus.valueOf(statusStr)
            val bubbleCount = bubble.getInt("fullEnergy")

            when (status) {
                CollectStatus.AVAILABLE -> {
                    // 可收集的能量球
                    availableBubbles.add(bubbleId)
                }
                CollectStatus.WAITING -> {
                    if (bubbleCount <= 0) {
                        Log.debug(TAG, "跳过数量为[$bubbleId]的等待能量球的蹲点任务")
                        continue
                    }
                    // 等待成熟的能量球，添加到蹲点队列
                    val produceTime = bubble.optLong("produceTime", 0L)
                    if (produceTime > 0 && produceTime > serverTime) {
                        // 检查保护罩时间（仅好友）：如果保护罩覆盖整个成熟期，跳过蹲点
                        // 自己的账号：无论是否有保护罩都要添加蹲点（到时间后直接收取）
                        if (!isSelf && shouldSkipWaitingTaskDueToProtection(userHomeObj, produceTime, serverTime)) {
                            val shieldEndTime = ForestUtil.getShieldEndTime(userHomeObj)
                            val bombEndTime = ForestUtil.getBombCardEndTime(userHomeObj)
                            val protectionEndTime = maxOf(shieldEndTime, bombEndTime)
                            val remainingHours = (protectionEndTime - serverTime) / (1000 * 60 * 60)
                            Log.record(
                                TAG,
                                "⏭️ 跳过好友蹲点[$userName]球[$bubbleId]：保护罩覆盖整个成熟期(保护还剩${remainingHours}h，能量${TimeUtil.getCommonDate(produceTime)}成熟)"
                            )
                            continue
                        }
                        
                        waitingBubblesCount++
                        // 添加蹲点任务
                        EnergyWaitingManager.addWaitingTask(
                            userId = userId ?: "",
                            userName = userName ?: "未知用户",
                            bubbleId = bubbleId,
                            produceTime = produceTime,
                            fromTag = "蹲点收取"
                        )
                        Log.debug(
                            TAG,
                            "添加蹲点: [$userName] 能量球[$bubbleId] 将在[${TimeUtil.getCommonDate(produceTime)}]成熟$protectionLog"
                        )
                    }
                }
                else -> {
                    // 其他状态（INSUFFICIENT, ROBBED等）跳过
                    continue
                }
            }
        }

        // 打印调试信息
        Log.debug(TAG, "[$userName] 可收集能量球: ${availableBubbles.size}个")
        if (waitingBubblesCount > 0) {
            Log.debug(TAG, "[$userName] 等待成熟能量球: ${waitingBubblesCount}个")
        }
    }

    /**
     * 批量或逐一收取能量
     *
     * @param userId      用户ID
     * @param userHomeObj 用户主页的JSON对象
     * @param bubbleIds   能量球ID列表
     * @param fromTag     收取来源标识
     */
    @Throws(JSONException::class)
    /**
     * 收取活力能量
     * @param userId 用户ID
     * @param userHomeObj 用户主页对象
     * @param bubbleIds 能量球ID列表
     * @param fromTag 来源标识
     * @param skipPropCheck 是否跳过道具检查（用于蹲点收取快速通道）
     */
    private fun collectVivaEnergy(
        userId: String?,
        userHomeObj: JSONObject?,
        bubbleIds: MutableList<Long>,
        fromTag: String?,
        skipPropCheck: Boolean = false
    ) {
        val bizType = "GREEN"
        if (bubbleIds.isEmpty()) return
        val isBatchCollect = batchRobEnergy!!.value
        
        if (isBatchCollect) {
            // 使用自适应批量大小（使用try-catch确保安全）
            val batchSize = try {
                EnergyCollectionOptimizer.calculateOptimalBatchSize(bubbleIds.size)
            } catch (e: Exception) {
                Log.error(TAG, "计算批量大小失败，使用默认值: ${e.message}")
                MAX_BATCH_SIZE
            }
            
            Log.debug(TAG, "使用自适应批量大小: $batchSize (总共${bubbleIds.size}个能量球)")
            
            var i = 0
            while (i < bubbleIds.size) {
                val subList: MutableList<Long> =
                    bubbleIds.subList(i, min(i + batchSize, bubbleIds.size))
                
                val startTime = System.currentTimeMillis()
                var energyCollected = 0
                try {
                    // 批量收取能量
                    collectEnergy(
                        CollectEnergyEntity(
                            userId,
                            userHomeObj,
                            AntForestRpcCall.batchEnergyRpcEntity(bizType, userId, subList),
                            fromTag,
                            skipPropCheck  // 🚀 传递快速通道标记
                        )
                    )
                    // 从userHomeObj中获取收取的能量总量
                    energyCollected = extractCollectedEnergy(userHomeObj)
                    
                    // 记录批量收取成功（使用try-catch确保安全）
                    try {
                        val duration = System.currentTimeMillis() - startTime
                        EnergyCollectionOptimizer.recordBatchCollect(true, duration, energyCollected)
                        EnergyCollectionOptimizer.recordRpcLatency(duration)
                    } catch (e: Exception) {
                        Log.error(TAG, "记录统计失败: ${e.message}")
                    }
                } catch (e: Exception) {
                    // 记录批量收取失败
                    try {
                        val duration = System.currentTimeMillis() - startTime
                        EnergyCollectionOptimizer.recordBatchCollect(false, duration, energyCollected)
                    } catch (ex: Exception) {
                        Log.error(TAG, "记录失败统计失败: ${ex.message}")
                    }
                    throw e
                }
                i += batchSize
            }
        } else {
            for (id in bubbleIds) {
                val startTime = System.currentTimeMillis()
                var energyCollected = 0
                try {
                    // 单个收取能量
                    collectEnergy(
                        CollectEnergyEntity(
                            userId,
                            userHomeObj,
                            AntForestRpcCall.energyRpcEntity(bizType, userId, id),
                            fromTag,
                            skipPropCheck  // 🚀 传递快速通道标记
                        )
                    )
                    // 从userHomeObj中获取收取的能量
                    energyCollected = extractCollectedEnergy(userHomeObj)
                    
                    // 记录单个收取（使用try-catch确保安全）
                    try {
                        val duration = System.currentTimeMillis() - startTime
                        EnergyCollectionOptimizer.recordSingleCollect(duration, energyCollected)
                        EnergyCollectionOptimizer.recordRpcLatency(duration)
                    } catch (e: Exception) {
                        Log.error(TAG, "记录单个收取统计失败: ${e.message}")
                    }
                } catch (e: Exception) {
                    throw e
                }
            }
        }
        
        // 记录好友能量（用于预测，使用try-catch确保安全）
        if (userId != null && fromTag != "self") {
            try {
                EnergyCollectionOptimizer.recordFriendEnergy(userId)
            } catch (e: Exception) {
                Log.error(TAG, "记录好友能量失败: ${e.message}")
            }
        }
    }
    
    /**
     * 从返回的JSON对象中提取收取的能量总量
     */
    private fun extractCollectedEnergy(userHomeObj: JSONObject?): Int {
        if (userHomeObj == null) return 0
        
        try {
            // 尝试从 bubbles 数组中获取收集的能量
            if (userHomeObj.has("bubbles")) {
                val bubbles = userHomeObj.getJSONArray("bubbles")
                var totalEnergy = 0
                for (i in 0 until bubbles.length()) {
                    val bubble = bubbles.getJSONObject(i)
                    val collected = bubble.optInt("collectedEnergy", 0)
                    totalEnergy += collected
                }
                return totalEnergy
            }
        } catch (e: Exception) {
            Log.debug(TAG, "提取收集能量失败: ${e.message}")
        }
        
        return 0
    }

    /**
     * 函数式接口，用于提供RPC调用
     */
    private fun interface RpcSupplier<T> {
        @Throws(Exception::class)
        fun get(): T?
    }

    /**
     * 函数式接口，用于对JSON对象进行断言
     */
    private fun interface JsonPredicate<T> {
        @Throws(Exception::class)
        fun test(t: T?): Boolean
    }

    /**
     * 协程版本的排行榜收取方法
     */

    private suspend fun collectRankingsCoroutine(
        rankingName: String?,
        rpcCall: RpcSupplier<String?>,
        jsonArrayKey: String?,
        flag: String,
        preCondition: JsonPredicate<JSONObject?>?
    ) = withContext(Dispatchers.Default) {
        try {
            Log.record(TAG, "开始处理$rankingName...")
            val tc = TimeCounter(TAG)
            var rankingObject: JSONObject? = null
            for (i in 0..2) {
                var response: String? = null
                try {
                    response = rpcCall.get()
                    if (response != null && !response.isEmpty()) {
                        rankingObject = JSONObject(response)
                        break
                    }
                } catch (e: Exception) {
                    Log.printStackTrace(
                        TAG,
                        "collectRankings $rankingName, response: $response",
                        e
                    )
                }
                if (i < 2) {
                    Log.record(TAG, "获取" + rankingName + "失败，" + (5 * (i + 1)) + "秒后重试")
                    GlobalThreadPools.sleepCompat(5000L * (i + 1))
                }
            }

            if (rankingObject == null) {
                Log.error(TAG, "获取" + rankingName + "失败")
                return@withContext
            }
            if (!ResChecker.checkRes(TAG + "获取" + rankingName + "失败:", rankingObject)) {
                Log.error(
                    TAG,
                    "获取" + rankingName + "失败: " + rankingObject.optString("resultDesc")
                )
                return@withContext
            }
            val totalDatas = rankingObject.optJSONArray(jsonArrayKey)
            if (totalDatas == null) {
                Log.record(TAG, rankingName + "数据为空，跳过处理。")
                return@withContext
            }
            Log.record(
                TAG,
                "成功获取" + rankingName + "数据，共发现" + totalDatas.length() + "位好友。"
            )
            tc.countDebug("获取$rankingName")
            if (preCondition != null && !preCondition.test(rankingObject)) {
                return@withContext
            }
            // 处理前20个  超过会报错
            Log.record(TAG, "开始处理" + rankingName + "前20位好友...")
            val friendRanking = rankingObject.optJSONArray("friendRanking")
            if (friendRanking != null) {
                processFriendsEnergyCoroutine(friendRanking, flag, "${rankingName}前20位")
            }
            tc.countDebug("处理" + rankingName + "靠前的好友")
            // 分批并行处理后续的（协程版本）
            if (totalDatas.length() <= 20) {
                Log.record(TAG, rankingName + "没有更多的好友需要处理，跳过")
                return@withContext
            }
            
            // 优化：限制处理好友数量，加快主任务完成
            // 只处理前60个好友（前20个已处理，再处理40个即2批）
            val maxFriendsToProcess = 60 // 可调整：60/100/150等
            val remainingToProcess = minOf(totalDatas.length() - 20, maxFriendsToProcess - 20)
            
            if (remainingToProcess <= 0) {
                Log.record(TAG, rankingName + "已处理前20位好友，跳过后续处理")
                return@withContext
            }
            
            val idList: MutableList<String?> = ArrayList()
            val batchSize = 20
            val batches = (remainingToProcess + batchSize - 1) / batchSize
            Log.record(
                TAG,
                "⚡ 快速模式：处理" + rankingName + "前${maxFriendsToProcess}位好友中的后续${remainingToProcess}位，共" + batches + "批（跳过${totalDatas.length() - maxFriendsToProcess}位好友）"
            )

            // 串行处理批次，避免总并发数过高
            var batchCount = 0

            for (pos in 20..<minOf(totalDatas.length(), maxFriendsToProcess)) {
                // 检查协程是否被取消
                if (!isActive) {
                    Log.runtime(TAG, "协程被取消，停止处理${rankingName}批次")
                    return@withContext
                }
                
                val friend = totalDatas.getJSONObject(pos)
                val userId = friend.getString("userId")
                if (userId == selfId) continue
                idList.add(userId)

                if (idList.size == batchSize) {
                    val batch: MutableList<String?> = ArrayList(idList)
                    val currentBatchNum = ++batchCount

                    // 串行执行：等待当前批次完成再处理下一批次
                    Log.record(TAG, "[批次$currentBatchNum/$batches] 开始处理...")
                    try {
                        processFriendsEnergyCoroutine(batch, flag, "批次$currentBatchNum")
                        Log.record(TAG, "[批次$currentBatchNum/$batches] 处理完成")
                    } catch (e: CancellationException) {
                        Log.runtime(TAG, "[批次$currentBatchNum/$batches] 被取消")
                        throw e
                    }

                    idList.clear()
                }
            }

            // 处理剩余的用户
            if (idList.isNotEmpty()) {
                // 检查协程是否被取消
                if (!isActive) {
                    Log.runtime(TAG, "协程被取消，跳过${rankingName}剩余用户处理")
                    return@withContext
                }
                
                val currentBatchNum = ++batchCount
                Log.record(TAG, "[批次$currentBatchNum/$batches] 开始处理...")
                try {
                    processFriendsEnergyCoroutine(idList, flag, "批次$currentBatchNum")
                    Log.record(TAG, "[批次$currentBatchNum/$batches] 处理完成")
                } catch (e: CancellationException) {
                    Log.runtime(TAG, "[批次$currentBatchNum/$batches] 被取消")
                    throw e
                }
            }
            tc.countDebug("分批处理" + rankingName + "其他好友")
            Log.record(TAG, "收取" + rankingName + "能量完成！")
        } catch (e: CancellationException) {
            // 协程被取消是正常行为，不记录错误日志
            Log.runtime(TAG, "处理" + rankingName + "时协程被取消")
            throw e // 重新抛出，让协程系统处理
        } catch (e: Exception) {
            Log.error(TAG, "处理" + rankingName + "时发生异常")
            Log.printStackTrace(TAG, "collectRankings 异常", e)
        }
    }

    /**
     * 协程版本：收取PK好友能量
     */
    private suspend fun collectPKEnergyCoroutine() {
        collectRankingsCoroutine(
            "PK排行榜",
            { AntForestRpcCall.queryTopEnergyChallengeRanking() },
            "totalData",
            "pk",
            JsonPredicate { pkObject: JSONObject? ->
                if (pkObject!!.getString("rankMemberStatus") != "JOIN") {
                    Log.runtime(TAG, "未加入PK排行榜,跳过,尝试关闭")
                    pkEnergy!!.value = false
                    return@JsonPredicate false
                }
                true
            }
        )
    }

    /**
     * 使用找能量功能收取好友能量（协程版本）
     * 这是一个更高效的收取方式，可以直接找到有能量的好友
     */
    private fun collectEnergyByTakeLook() {
        try {
            // 检查是否还在冷却期（使用动态冷却时间）
            val currentTime = System.currentTimeMillis()
            val dynamicCooldown = ForestFriendManager.getCurrentCooldown()
            if (currentTime < nextTakeLookTime) {
                val remainingMinutes = (nextTakeLookTime - currentTime) / 60000
                val remainingSeconds = ((nextTakeLookTime - currentTime) % 60000) / 1000
                Log.record(TAG, "找能量功能冷却中，还需等待 ${remainingMinutes}分${remainingSeconds}秒 (动态冷却${dynamicCooldown/60000}分钟)")
                return
            }

            val tc = TimeCounter(TAG)
            var foundCount = 0
            var totalEnergyFound = 0L // 统计找到的总能量
            val maxAttempts = 10 // 减少到10次，避免过度循环
            var consecutiveEmpty = 0 // 连续空结果计数
            var shouldCooldown = false // 标记是否需要冷却
            Log.record(TAG, "开始使用找能量功能收取好友能量（冷却时间：${dynamicCooldown/60000}分钟）")
            for (attempt in 1..maxAttempts) {
                // 构建跳过用户列表（有保护罩的用户）
                val skipUsers = JSONObject()
                // 调用找能量接口
                val takeLookResponse: String?
                try {
                    takeLookResponse = AntForestRpcCall.takeLook(skipUsers)
                } catch (e: NullPointerException) {
                    shouldCooldown = true
                    val cooldown = ForestFriendManager.getCurrentCooldown()
                    nextTakeLookTime = System.currentTimeMillis() + cooldown
                    Log.error(TAG, "找能量接口调用异常，休息${cooldown/60000}分钟")
                    Log.printStackTrace(TAG, e)
                    break
                }
                if (takeLookResponse.isEmpty()) {
                    consecutiveEmpty++
                    if (consecutiveEmpty >= 3) {
                        shouldCooldown = true
                        val cooldown = ForestFriendManager.getCurrentCooldown()
                        nextTakeLookTime = System.currentTimeMillis() + cooldown
                        Log.record(TAG, "连续" + consecutiveEmpty + "次接口返回空结果，提前结束找能量，休息${cooldown/60000}分钟")
                        break
                    }
                    continue
                }
                val takeLookResult: JSONObject?
                try {
                    takeLookResult = JSONObject(takeLookResponse)
                } catch (e: JSONException) {
                    Log.error(TAG, "找能量返回的不是有效的JSON，response: $takeLookResponse")
                    Log.printStackTrace(TAG, e)
                    continue
                }
                if (!ResChecker.checkRes(TAG + "找能量失败:", takeLookResult)) {
                    Log.error(TAG, "找能量失败: " + takeLookResult.optString("resultDesc"))
                    break
                }
                // 获取找到的好友ID
                val friendId = takeLookResult.optString("friendId")
                if (friendId.isEmpty() || friendId == selfId) {
                    consecutiveEmpty++
                    if (attempt % 3 == 0) {
                        Log.record(
                            TAG,
                            "第" + attempt + "次找能量没有发现新好友，继续尝试:" + skipUsers
                        )
                    }
                    // 连续3次没有发现新好友就提前结束，避免浪费时间
                    if (consecutiveEmpty >= 3) {
                        shouldCooldown = true
                        val cooldown = ForestFriendManager.getCurrentCooldown()
                        nextTakeLookTime = System.currentTimeMillis() + cooldown
                        Log.record(TAG, "连续" + consecutiveEmpty + "次未发现新好友，提前结束找能量，休息${cooldown/60000}分钟")
                        break
                    }
                    continue
                }
                if (skipUsersCache.containsKey(friendId)) {
                    continue
                }
                // 查询好友主页并收取能量
                val friendHomeObj = queryFriendHome(friendId, "TAKE_LOOK")
                if (friendHomeObj != null) {
                    foundCount++
                    var friendName = UserMap.getMaskName(friendId)
                    if (friendName == null || friendName.isEmpty() || friendName == friendId) {
                        // 如果UserMap没有返回有效的用户名，使用通用的获取用户名方法
                        friendName = getAndCacheUserName(friendId, friendHomeObj, null)
                    }
                    val currentTime = System.currentTimeMillis()
                    // 检查是否有保护，如果有则添加到跳过列表
                    val hasShieldProtection = hasShield(friendHomeObj, currentTime)
                    val hasBombProtection = hasBombCard(friendHomeObj, currentTime)
                    if (hasShieldProtection || hasBombProtection) {
                        val protectionType = if (hasShieldProtection) "保护罩" else "炸弹卡"
                        addToSkipUsers(friendId)
                        Log.record(
                            TAG,
                            "找能量第" + attempt + "次发现好友[" + friendName + friendId + "]有" + protectionType + "，跳过收取"
                        )
                    } else {
                        // 没有保护才进行收取处理
                        val beforeEnergy = totalEnergyFound
                        collectEnergy(friendId, friendHomeObj, "takeLook")
                        // 尝试统计收取的能量（简化处理，实际能量在collectEnergy中统计）
                    }
                    // 优化间隔：找到好友时减少等待时间，提高效率
                    GlobalThreadPools.sleepCompat(1200L)
                    consecutiveEmpty = 0 // 重置连续空结果计数
                } else {
                    consecutiveEmpty++
                    // 检查friendId是否为null或空，给出更详细的信息
                    Log.record(TAG, "找能量第" + attempt + "次：获取好友信息失败（可能是网络波动或用户已删除），跳过")
                    // 增加容错次数：连续5次空结果才提前结束，避免因个别失败中断整个流程
                    if (consecutiveEmpty >= 5) {
                        Log.record(TAG, "连续" + consecutiveEmpty + "次获取失败，提前结束找能量（可能存在网络问题）")
                        break
                    }
                    // 失败时增加延迟，避免触发限流
                    GlobalThreadPools.sleepCompat(500L)
                }
            }
            tc.countDebug("找能量收取完成")
            
            // 记录找能量统计到ForestFriendManager
            ForestFriendManager.recordFindEnergyAttempt(
                foundEnergy = totalEnergyFound,
                friendsChecked = foundCount
            )
            
            // 使用动态冷却时间
            val dynamicCooldownMs = ForestFriendManager.getCurrentCooldown()
            Log.record(TAG, "找能量功能完成，共发现 $foundCount 个好友，下次冷却时间：${dynamicCooldownMs/60000}分钟")
            
            // 如果没有触发冷却，使用动态冷却时间
            if (!shouldCooldown) {
                nextTakeLookTime = System.currentTimeMillis() + dynamicCooldownMs
            }
        } catch (e: Exception) {
            Log.error(TAG, "找能量过程中发生异常")
            Log.printStackTrace(TAG, "collectEnergyByTakeLook 异常", e)
        }
    }

    /**
     * 将用户添加到跳过列表（内存缓存）
     *
     * @param userId 用户ID
     */
    private fun addToSkipUsers(userId: String?) {
        try {
            if (!userId.isNullOrEmpty()) {
                skipUsersCache[userId] = "baohuzhao"
            }
        } catch (e: Exception) {
            Log.printStackTrace(TAG, "添加跳过用户失败", e)
        }
    }

    /**
     * 协程版本：收取好友能量
     */
    private suspend fun collectFriendEnergyCoroutine() {
        collectRankingsCoroutine(
            "好友排行榜",
            { AntForestRpcCall.queryFriendsEnergyRanking() },
            "totalDatas",
            "普通好友",
            null
        )
    }

    /**
     * 统一的协程批量好友处理方法
     *
     * @param friendSource 好友数据源，可以是：
     *   - JSONArray: 直接的好友列表
     *   - MutableList<String?>: 用户ID列表，需要通过API获取
     * @param flag 标记（空字符串=普通好友，"pk"=PK好友）
     * @param sourceName 数据源名称（用于日志）
     */
    private suspend fun processFriendsEnergyCoroutine(
        friendSource: Any,
        flag: String,
        sourceName: String = "好友"
    ) = withContext(Dispatchers.Default) {
        try {
            if (errorWait) return@withContext

            val friendList: JSONArray? = when (friendSource) {
                is JSONArray -> {
                    // 直接的好友列表
                    friendSource
                }
                is MutableList<*> -> {
                    // 用户ID列表，需要通过API获取详细信息
                    @Suppress("UNCHECKED_CAST")
                    val userIds = friendSource as MutableList<String?>
                    val jsonStr = if (flag == "pk") {
                        AntForestRpcCall.fillUserRobFlag(JSONArray(userIds), true)
                    } else {
                        AntForestRpcCall.fillUserRobFlag(JSONArray(userIds))
                    }
                    val batchObj = JSONObject(jsonStr)
                    batchObj.optJSONArray("friendRanking")
                }
                else -> {
                    Log.error(TAG, "不支持的好友数据源类型: ${friendSource.javaClass.simpleName}")
                    return@withContext
                }
            }

            if (friendList == null) {
                Log.debug(TAG, "${sourceName}数据为空，跳过处理")
                return@withContext
            }

            if (friendList.length() == 0) {
                Log.debug(TAG, "${sourceName}列表为空，跳过处理")
                return@withContext
            }

            // 先收集并显示所有好友名单
            val friendNames = mutableListOf<String>()
            for (i in 0..<friendList.length()) {
                val friendObj = friendList.getJSONObject(i)
                val userId = friendObj.optString("userId", "")
                val displayName = friendObj.optString("displayName", UserMap.getMaskName(userId))
                friendNames.add(displayName)
            }

            Log.record(TAG, "📋 开始处理${friendList.length()}个${sourceName}（并发数:60）")
            Log.record(TAG, "👥 ${friendNames.joinToString(" | ")}")
            val startTime = System.currentTimeMillis()

            // 使用协程并发处理每个好友（带并发控制）
            val friendJobs = mutableListOf<Deferred<Unit>>()
            for (i in 0..<friendList.length()) {
                val friendObj = friendList.getJSONObject(i)
                val job = async {
                    concurrencyLimiter.acquire()
                    try {
                        // 直接调用内部方法，减少一层包装以提高性能
                        processEnergyInternal(friendObj, flag)
                    } catch (e: Exception) {
                        Log.printStackTrace(TAG, "处理好友异常", e)
                    } finally {
                        concurrencyLimiter.release()
                    }
                }
                friendJobs.add(job)
            }

            // 等待所有好友处理完成
            friendJobs.awaitAll()
            val elapsed = System.currentTimeMillis() - startTime
            Log.record(TAG, "✅ ${sourceName}处理完成，耗时${elapsed}ms，平均${elapsed/friendList.length()}ms/人")

        } catch (e: CancellationException) {
            // 协程被取消是正常行为，不记录错误日志
            Log.runtime(TAG, "处理${sourceName}时协程被取消")
            throw e // 重新抛出，让协程系统处理
        } catch (e: JSONException) {
            Log.printStackTrace(TAG, "解析${sourceName}数据失败", e)
        } catch (e: Exception) {
            Log.printStackTrace(TAG, "处理${sourceName}出错", e)
        }
    }

    /**
     * 处理单个好友的能量收取（协程版本）
     * 已注释：直接调用 processEnergyInternal 以提高性能
     */
//    private suspend fun processSingleFriendEnergy(obj: JSONObject, flag: String?) = withContext(Dispatchers.Default) {
//        try {
//            processEnergyInternal(obj, flag)
//        } catch (e: Exception) {
//            Log.printStackTrace(TAG, "处理好友异常", e)
//        }
//    }

    /**
     * 处理单个好友的核心逻辑（无锁）
     *
     * @param obj  好友/PK好友 的JSON对象
     * @param flag 标记是普通好友还是PK好友
     */
    @Throws(Exception::class)
    private fun processEnergyInternal(obj: JSONObject, flag: String?) {
        if (errorWait) return
        val userId = obj.getString("userId")
        if (userId == selfId) return  // 跳过自己

        // 检查是否在"手速太快"冷却期
        if (ForestUtil.isUserInFrequencyCooldown(userId)) {
            return  // 跳过处理
        }

        var userName = obj.optString("displayName", UserMap.getMaskName(userId))
        if (emptyForestCache.containsKey(userId)) { //本轮已知为空的树林
            return
        }

        val isPk = "pk" == flag
        if (isPk) {
            userName = "PK榜好友|$userName"
        }
        //  Log.record(TAG, "  processEnergy 开始处理用户: [" + userName + "], 类型: " + (isPk ? "PK" : "普通"));
        if (isPk) {
            val needCollectEnergy = collectEnergy!!.value && pkEnergy!!.value
            if (!needCollectEnergy) {
                Log.record(TAG, "    PK好友: [$userName$userId], 不满足收取条件，跳过")
                return
            }
            Log.debug(TAG, "  正在查询PK好友 [$userName$userId] 的主页...")
            collectEnergy(userId, queryFriendHome(userId, "PKContest"), "pk")
        } else { // 普通好友
            val needCollectEnergy =
                collectEnergy!!.value && !jsonCollectMap.contains(userId)
            val needHelpProtect =
                helpFriendCollectType!!.value != HelpFriendCollectType.NONE && obj.optBoolean(
                    "canProtectBubble"
                ) && Status.canProtectBubbleToday(selfId)
            val needCollectGiftBox =
                collectGiftBox!!.value && obj.optBoolean("canCollectGiftBox")
            if (!needCollectEnergy && !needHelpProtect && !needCollectGiftBox) {
                //   Log.record(TAG, "    普通好友: [$userName$userId], 所有条件不满足，跳过")
                return
            }
            var userHomeObj: JSONObject? = null
            // 只要开启了收能量，就进去看看，以便添加蹲点
            if (needCollectEnergy) {
                // 即使排行榜信息显示没有可收能量，也进去检查，以便添加蹲点任务
                Log.debug(TAG, "  正在查询好友 [$userName$userId] 的主页...")
                userHomeObj = collectEnergy(userId, queryFriendHome(userId, null), "friend")
            }
            if (needHelpProtect) {
                val isProtected = isIsProtected(userId)
                /** lzw add end */
                if (isProtected) {
                    if (userHomeObj == null) {
                        userHomeObj = queryFriendHome(userId, null)
                    }
                    if (userHomeObj != null) {
                        protectFriendEnergy(userHomeObj)
                    }
                }
            }
            // 尝试领取礼物盒
            if (needCollectGiftBox) {
                if (userHomeObj == null) {
                    userHomeObj = queryFriendHome(userId, null)
                }
                if (userHomeObj != null) {
                    collectGiftBox(userHomeObj)
                }
            }
        }
    }

    private fun isIsProtected(userId: String?): Boolean {
        var isProtected: Boolean
        // Log.forest("is_monday:"+_is_monday);
        if (monday) {
            isProtected = alternativeAccountList!!.value.contains(userId)
        } else {
            isProtected = helpFriendCollectList!!.value.contains(userId)
            if (helpFriendCollectType!!.value != HelpFriendCollectType.HELP) {
                isProtected = !isProtected
            }
        }
        return isProtected
    }

    /** lzw add end */
    /**
     * 协程版本：收取排名靠前好友能量
     */
    private fun collectGiftBox(userHomeObj: JSONObject) {
        try {
            val giftBoxInfo = userHomeObj.optJSONObject("giftBoxInfo")
            val userEnergy = userHomeObj.optJSONObject("userEnergy")
            val userId =
                if (userEnergy == null) UserMap.currentUid else userEnergy.optString("userId")
            if (giftBoxInfo != null) {
                val giftBoxList = giftBoxInfo.optJSONArray("giftBoxList")
                if (giftBoxList != null && giftBoxList.length() > 0) {
                    for (ii in 0..<giftBoxList.length()) {
                        try {
                            val giftBox = giftBoxList.getJSONObject(ii)
                            val giftBoxId = giftBox.getString("giftBoxId")
                            val title = giftBox.getString("title")
                            val giftBoxResult =
                                JSONObject(AntForestRpcCall.collectFriendGiftBox(giftBoxId, userId))
                            if (!ResChecker.checkRes(TAG + "领取好友礼盒失败:", giftBoxResult)) {
                                Log.record(giftBoxResult.getString("resultDesc"))
                                Log.runtime(giftBoxResult.toString())
                                continue
                            }
                            val energy = giftBoxResult.optInt("energy", 0)
                            Log.forest("礼盒能量🎁[" + UserMap.getMaskName(userId) + "-" + title + "]#" + energy + "g")
                        } catch (t: Throwable) {
                            Log.printStackTrace(t)
                            break
                        } finally {
                            GlobalThreadPools.sleepCompat(500L)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.printStackTrace(e)
        }
    }

    private fun protectFriendEnergy(userHomeObj: JSONObject) {
        try {
            val wateringBubbles = userHomeObj.optJSONArray("wateringBubbles")
            val userEnergy = userHomeObj.optJSONObject("userEnergy")
            val userId =
                if (userEnergy == null) UserMap.currentUid else userEnergy.optString("userId")
            if (wateringBubbles != null && wateringBubbles.length() > 0) {
                for (j in 0..<wateringBubbles.length()) {
                    try {
                        val wateringBubble = wateringBubbles.getJSONObject(j)
                        if ("fuhuo" != wateringBubble.getString("bizType")) {
                            continue
                        }
                        if (wateringBubble.getJSONObject("extInfo").optInt("restTimes", 0) == 0) {
                            Status.protectBubbleToday(selfId)
                        }
                        if (!wateringBubble.getBoolean("canProtect")) {
                            continue
                        }
                        val joProtect = JSONObject(AntForestRpcCall.protectBubble(userId))
                        if (!ResChecker.checkRes(TAG + "复活能量失败:", joProtect)) {
                            Log.record(joProtect.getString("resultDesc"))
                            Log.runtime(joProtect.toString())
                            continue
                        }
                        val vitalityAmount = joProtect.optInt("vitalityAmount", 0)
                        val fullEnergy = wateringBubble.optInt("fullEnergy", 0)
                        val str =
                            "复活能量🚑[" + UserMap.getMaskName(userId) + "-" + fullEnergy + "g]" + (if (vitalityAmount > 0) "#活力值+$vitalityAmount" else "")
                        Log.forest(str)
                        break
                    } catch (t: Throwable) {
                        Log.printStackTrace(t)
                        break
                    } finally {
                        GlobalThreadPools.sleepCompat(500)
                    }
                }
            }
        } catch (e: Exception) {
            Log.printStackTrace(e)
        }
    }

    private fun collectEnergy(collectEnergyEntity: CollectEnergyEntity) {
        if (errorWait) {
            Log.record(TAG, "异常⌛等待中...不收取能量")
            return
        }
        val runnable = Runnable {
            try {
                val userId = collectEnergyEntity.userId
                // 从 CollectEnergyEntity 中读取是否跳过道具检查的标记
                val skipPropCheck = collectEnergyEntity.skipPropCheck ?: false
                usePropBeforeCollectEnergy(userId, skipPropCheck)
                val rpcEntity = collectEnergyEntity.rpcEntity
                val needDouble = collectEnergyEntity.needDouble
                val needRetry = collectEnergyEntity.needRetry
                val tryCount = collectEnergyEntity.addTryCount()
                var collected = 0
                val startTime: Long

                synchronized(collectEnergyLockLimit) {
                    val sleep: Long
                    if (needDouble) {
                        collectEnergyEntity.unsetNeedDouble()
                        val interval = doubleCollectIntervalEntity!!.interval
                        sleep =
                            (interval ?: 1000) - System.currentTimeMillis() + collectEnergyLockLimit.get()!!
                    } else if (needRetry) {
                        collectEnergyEntity.unsetNeedRetry()
                        sleep =
                            retryIntervalInt!! - System.currentTimeMillis() + collectEnergyLockLimit.get()!!
                    } else {
                        val interval = collectIntervalEntity!!.interval
                        sleep =
                            (interval ?: 1000) - System.currentTimeMillis() + collectEnergyLockLimit.get()!!
                    }
                    if (sleep > 0) {
                        GlobalThreadPools.sleepCompat(sleep)
                    }
                    startTime = System.currentTimeMillis()
                    collectEnergyLockLimit.setForce(startTime)
                }

                requestString(rpcEntity, 0, 0)
                val spendTime = System.currentTimeMillis() - startTime
                if (balanceNetworkDelay!!.value) {
                    delayTimeMath.nextInteger((spendTime / 3).toInt())
                }

                if (rpcEntity.hasError) {
                    val errorCode = XposedHelpers.callMethod(
                        rpcEntity.responseObject,
                        "getString",
                        "error"
                    ) as String?
                    if ("1004" == errorCode) {
                        if (BaseModel.waitWhenException.value > 0) {
                            val waitTime =
                                System.currentTimeMillis() + BaseModel.waitWhenException.value
                            RuntimeInfo.getInstance()
                                .put(RuntimeInfo.RuntimeInfoKey.ForestPauseTime, waitTime)
                            updateStatusText("异常")
                            Log.record(TAG, "触发异常,等待至" + TimeUtil.getCommonDate(waitTime))
                            errorWait = true
                            return@Runnable
                        }
                        GlobalThreadPools.sleepCompat((600 + RandomUtil.delay()).toLong())
                    }
                    if (tryCount < tryCountInt!!) {
                        collectEnergyEntity.setNeedRetry()
                        collectEnergy(collectEnergyEntity)
                    }
                    return@Runnable
                }

                val responseString: String = rpcEntity.responseString ?: ""
                val jo = JSONObject(responseString)
                val resultCode = jo.getString("resultCode")
                if (!"SUCCESS".equals(resultCode, ignoreCase = true)) {
                    if ("PARAM_ILLEGAL2" == resultCode) {
                        Log.record(
                            TAG,
                            "[" + getAndCacheUserName(userId) + "]" + "能量已被收取,取消重试 错误:" + jo.getString(
                                "resultDesc"
                            )
                        )
                        return@Runnable
                    }

                    // 检测并记录"手速太快"错误
                    if (ForestUtil.checkAndRecordFrequencyError(userId, jo)) {
                        return@Runnable
                    }

                    Log.record(
                        TAG,
                        "[" + getAndCacheUserName(userId) + "]" + jo.optString("resultDesc", "")
                    )
                    if (tryCount < tryCountInt!!) {
                        collectEnergyEntity.setNeedRetry()
                        collectEnergy(collectEnergyEntity)
                    }
                    return@Runnable
                }

                // --- 收能量逻辑保持原样 ---
                val jaBubbles = jo.getJSONArray("bubbles")
                val jaBubbleLength = jaBubbles.length()
                if (jaBubbleLength > 1) {
                    val newBubbleIdList: MutableList<Long?> = ArrayList()
                    for (i in 0..<jaBubbleLength) {
                        val bubble = jaBubbles.getJSONObject(i)
                        if (bubble.getBoolean("canBeRobbedAgain")) {
                            newBubbleIdList.add(bubble.getLong("id"))
                        }
                        collected += bubble.getInt("collectedEnergy")
                    }
                    if (collected > 0) {
                        val randomIndex = random.nextInt(emojiList.size)
                        val randomEmoji = emojiList[randomIndex]
                        val collectType = when (collectEnergyEntity.fromTag) {
                            "takeLook" -> "找能量一键收取️"
                            "蹲点收取" -> "蹲点一键收取️"
                            else -> "一键收取️"
                        }
                        val str =
                            collectType + randomEmoji + collected + "g[" + getAndCacheUserName(
                                userId
                            ) + "]#"
                        totalCollected += collected
                        if (needDouble) {
                            Log.forest(str + "耗时[" + spendTime + "]ms[双击]")
                            Toast.show("$str[双击]")
                        } else {
                            Log.forest(str + "耗时[" + spendTime + "]ms")
                            Toast.show(str)
                        }
                    }
                    if (!newBubbleIdList.isEmpty()) {
                        collectEnergyEntity.rpcEntity = AntForestRpcCall.batchEnergyRpcEntity(
                            "",
                            userId,
                            newBubbleIdList
                        )
                        collectEnergyEntity.setNeedDouble()
                        collectEnergyEntity.resetTryCount()
                        collectEnergy(collectEnergyEntity)
                    }
                } else if (jaBubbleLength == 1) {
                    val bubble = jaBubbles.getJSONObject(0)
                    collected += bubble.getInt("collectedEnergy")
                    if (collected > 0) {
                        val randomIndex = random.nextInt(emojiList.size)
                        val randomEmoji = emojiList[randomIndex]
                        val collectType = when (collectEnergyEntity.fromTag) {
                            "takeLook" -> "找能量收取"
                            "蹲点收取" -> "蹲点收取"
                            else -> "普通收取"
                        }
                        val str =
                            collectType + randomEmoji + collected + "g[" + getAndCacheUserName(
                                userId
                            ) + "]"
                        totalCollected += collected
                        if (needDouble) {
                            Log.forest(str + "耗时[" + spendTime + "]ms[双击]")
                            Toast.show("$str[双击]")
                        } else {
                            Log.forest(str + "耗时[" + spendTime + "]ms")
                            Toast.show(str)
                        }
                    }
                    if (bubble.getBoolean("canBeRobbedAgain")) {
                        collectEnergyEntity.setNeedDouble()
                        collectEnergyEntity.resetTryCount()
                        collectEnergy(collectEnergyEntity)
                        return@Runnable
                    }

                    val userHome = collectEnergyEntity.userHome
                    if (userHome != null) {
                        val bizNo = userHome.optString("bizNo")
                        if (bizNo.isNotEmpty()) {
                            val returnCount = getReturnCount(collected)
                            if (returnCount > 0) {
                                // ✅ 调用 returnFriendWater 增加通知好友开关
                                val notify = notifyFriend!!.value // 从配置获取
                                returnFriendWater(userId, bizNo, 1, returnCount, notify)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.runtime(TAG, "collectEnergy err")
                Log.printStackTrace(e)
            } finally {
                val strTotalCollected =
                    "本次总 收:" + totalCollected + "g 帮:" + TOTAL_HELP_COLLECTED + "g 浇:" + TOTAL_WATERED + "g"
                updateLastExecText(strTotalCollected)
                notifyMain()
            }
        }
        taskCount.incrementAndGet()
        runnable.run()
    }

    private fun getReturnCount(collected: Int): Int {
        var returnCount = 0
        if (returnWater33!!.value in 1..collected) {
            returnCount = 33
        } else if (returnWater18!!.value in 1..collected) {
            returnCount = 18
        } else if (returnWater10!!.value in 1..collected) {
            returnCount = 10
        }
        return returnCount
    }

    /**
     * 更新使用中的的道具剩余时间
     */
    @Throws(JSONException::class)
    private fun updateSelfHomePage() {
        val s = AntForestRpcCall.queryHomePage()
        GlobalThreadPools.sleepCompat(100)
        val joHomePage = JSONObject(s)
        updateSelfHomePage(joHomePage)
    }

    /**
     * 更新使用中的的道具剩余时间
     *
     * @param joHomePage 首页 JSON 对象
     */
    private fun updateSelfHomePage(joHomePage: JSONObject) {
        try {
            var usingUserPropsNew = joHomePage.getJSONArray("loginUserUsingPropNew")
            if (usingUserPropsNew.length() == 0) {
                usingUserPropsNew = joHomePage.getJSONArray("usingUserPropsNew")
            }
            for (i in 0..<usingUserPropsNew.length()) {
                val userUsingProp = usingUserPropsNew.getJSONObject(i)
                val propGroup = userUsingProp.getString("propGroup")
                when (propGroup) {
                    "doubleClick" -> {
                        doubleEndTime = userUsingProp.getLong("endTime")
                        Log.runtime(
                            TAG,
                            "双击卡剩余时间⏰：" + formatTimeDifference(doubleEndTime - System.currentTimeMillis())
                        )
                    }

                    "stealthCard" -> {
                        stealthEndTime = userUsingProp.getLong("endTime")
                        Log.runtime(
                            TAG,
                            "隐身卡剩余时间⏰️：" + formatTimeDifference(stealthEndTime - System.currentTimeMillis())
                        )
                    }

                    "shield" -> {
                        shieldEndTime = userUsingProp.getLong("endTime")
                        Log.runtime(
                            TAG,
                            "保护罩剩余时间⏰：" + formatTimeDifference(shieldEndTime - System.currentTimeMillis())
                        )
                    }

                    "energyBombCard" -> {
                        energyBombCardEndTime = userUsingProp.getLong("endTime")
                        Log.runtime(
                            TAG,
                            "能量炸弹卡剩余时间⏰：" + formatTimeDifference(energyBombCardEndTime - System.currentTimeMillis())
                        )
                    }

                    "robExpandCard" -> {
                        val extInfo = userUsingProp.optString("extInfo")
                        robExpandCardEndTime = userUsingProp.getLong("endTime")
                        Log.runtime(
                            TAG,
                            "1.1倍能量卡剩余时间⏰：" + formatTimeDifference(robExpandCardEndTime - System.currentTimeMillis())
                        )
                        if (!extInfo.isEmpty()) {
                            val extInfoObj = JSONObject(extInfo)
                            val leftEnergy = extInfoObj.optString("leftEnergy", "0").toDouble()
                            if (leftEnergy > 3000 || ("true" == extInfoObj.optString(
                                    "overLimitToday",
                                    "false"
                                ) && leftEnergy >= 1)
                            ) {
                                val propId = userUsingProp.getString("propId")
                                val propType = userUsingProp.getString("propType")
                                val jo = JSONObject(
                                    AntForestRpcCall.collectRobExpandEnergy(
                                        propId,
                                        propType
                                    )
                                )
                                if (ResChecker.checkRes(TAG, jo)) {
                                    val collectEnergy = jo.optInt("collectEnergy")
                                    Log.forest("额外能量🌳[" + collectEnergy + "g][1.1倍能量卡]")
                                }
                            }
                        }
                    }
                }
            }
        } catch (th: Throwable) {
            Log.runtime(TAG, "updateDoubleTime err")
            Log.printStackTrace(TAG, th)
        }
    }

    /**
     * 为好友浇水并返回浇水次数和是否可以继续浇水的状态。
     *
     * @param userId       好友的用户ID
     * @param bizNo        业务编号
     * @param count        需要浇水的次数
     * @param waterEnergy  每次浇水的能量值
     * @param notifyFriend 是否通知好友
     * @return KVMap 包含浇水次数和是否可以继续浇水的状态
     */
    private fun returnFriendWater(
        userId: String?,
        bizNo: String?,
        count: Int,
        waterEnergy: Int,
        notifyFriend: Boolean
    ): KVMap<Int?, Boolean?> {
        // bizNo为空直接返回默认
        if (bizNo == null || bizNo.isEmpty()) {
            return KVMap(0, true)
        }

        var wateredTimes = 0 // 已浇水次数
        var isContinue = true // 是否可以继续浇水

        try {
            val energyId = getEnergyId(waterEnergy)

            var waterCount = 1
            label@ while (waterCount <= count) {
                // 调用RPC进行浇水，并传入是否通知好友
                val rpcResponse =
                    AntForestRpcCall.transferEnergy(userId, bizNo, energyId, notifyFriend)

                if (rpcResponse.isEmpty()) {
                    Log.record(TAG, "好友浇水返回空: " + UserMap.getMaskName(userId))
                    isContinue = false
                    break
                }

                val jo = JSONObject(rpcResponse)

                // 先处理可能的错误码
                val errorCode = jo.optString("error")
                if ("1009" == errorCode) { // 访问被拒绝
                    Log.record(TAG, "好友浇水🚿访问被拒绝: " + UserMap.getMaskName(userId))
                    isContinue = false
                    break
                } else if ("3000" == errorCode) { // 系统错误
                    Log.record(TAG, "好友浇水🚿系统错误，稍后重试: " + UserMap.getMaskName(userId))
                    Thread.sleep(500)
                    waterCount-- // 重试当前次数
                    waterCount++
                    continue
                }

                // 处理正常返回
                val resultCode = jo.optString("resultCode")
                when (resultCode) {
                    "SUCCESS" -> {
                        val treeEnergy = jo.optJSONObject("treeEnergy")
                        val currentEnergy = if (treeEnergy != null) treeEnergy.optString(
                            "currentEnergy",
                            "未知"
                        ) else "未知"
                        Log.forest("好友浇水🚿[" + UserMap.getMaskName(userId) + "]#" + waterEnergy + "g，剩余能量[" + currentEnergy + "g]")
                        wateredTimes++
                        GlobalThreadPools.sleepCompat(1200L)
                    }

                    "WATERING_TIMES_LIMIT" -> {
                        Log.record(TAG, "好友浇水🚿今日已达上限: " + UserMap.getMaskName(userId))
                        wateredTimes = 3 // 上限假设3次
                        break@label
                    }

                    "ENERGY_INSUFFICIENT" -> {
                        Log.record(TAG, "好友浇水🚿" + jo.optString("resultDesc"))
                        isContinue = false
                        break@label
                    }

                    else -> {
                        Log.record(TAG, "好友浇水🚿" + jo.optString("resultDesc"))
                        Log.runtime(jo.toString())
                    }
                }
                waterCount++
            }
        } catch (t: Throwable) {
            Log.runtime(TAG, "returnFriendWater err")
            Log.printStackTrace(TAG, t)
        }

        return KVMap(wateredTimes, isContinue)
    }

    /**
     * 获取能量ID
     */
    private fun getEnergyId(waterEnergy: Int): Int {
        if (waterEnergy <= 0) return 0
        if (waterEnergy >= 66) return 42
        if (waterEnergy >= 33) return 41
        if (waterEnergy >= 18) return 40
        return 39
    }

    /**
     * 兑换能量保护罩
     * 类别 spuid skuid price
     * 限时 CR20230517000497  CR20230516000370  166
     * 永久 CR20230517000497  CR20230516000371  500
     */
    private fun exchangeEnergyShield(): Boolean {
        val spuId = "CR20230517000497"
        val skuId = "CR20230516000370"
        if (!Status.canVitalityExchangeToday(skuId, 1)) {
            return false
        }
        return Vitality.VitalityExchange(spuId, skuId, "保护罩")
    }

    /**
     * 兑换隐身卡
     */
    private fun exchangeStealthCard(): Boolean {
        val skuId = "SK20230521000206"
        val spuId = "SP20230521000082"
        if (!Status.canVitalityExchangeToday(skuId, 1)) {
            return false
        }
        return Vitality.VitalityExchange(spuId, skuId, "隐身卡")
    }

    /**
     * 兑换双击卡
     * 优先兑换31天双击卡，失败后尝试限时双击卡
     */
    private fun exchangeDoubleCard(): Boolean {
        // 尝试兑换31天双击卡
        if (Vitality.handleVitalityExchange("SK20240805004754")) {
            return true
        }
        // 失败后尝试兑换限时双击卡
        return Vitality.handleVitalityExchange("CR20230516000363")
    }

    /**
     * 执行当天森林签到任务
     *
     * @param forestSignVOList 森林签到列表
     * @return 获得的能量，如果签到失败或已签到则返回 0
     */
    private fun dailyTask(forestSignVOList: JSONArray): Int {
        try {
            val forestSignVO = forestSignVOList.getJSONObject(0)
            val currentSignKey = forestSignVO.getString("currentSignKey") // 当前签到的 key
            val signId = forestSignVO.getString("signId") // 签到ID
            val sceneCode = forestSignVO.getString("sceneCode") // 场景代码
            val signRecords = forestSignVO.getJSONArray("signRecords") // 签到记录
            for (i in 0..<signRecords.length()) { //遍历签到记录
                val signRecord = signRecords.getJSONObject(i)
                val signKey = signRecord.getString("signKey")
                val awardCount = signRecord.optInt("awardCount", 0)
                if (signKey == currentSignKey && !signRecord.getBoolean("signed")) {
                    val joSign = JSONObject(
                        AntForestRpcCall.antiepSign(
                            signId,
                            UserMap.currentUid,
                            sceneCode
                        )
                    )
                    GlobalThreadPools.sleepCompat(300) // 等待300毫秒
                    if (ResChecker.checkRes(TAG + "森林签到失败:", joSign)) {
                        Log.forest("森林签到📆成功")
                        return awardCount
                    }
                    break
                }
            }
            return 0 // 如果没有签到，则返回 0
        } catch (e: Exception) {
            Log.printStackTrace(e)
            return 0
        }
    }

    /**
     * 森林任务:
     * 逛支付宝会员,去森林寻宝抽1t能量
     * 防治荒漠化和干旱日,给随机好友一键浇水
     * 开通高德活动领,去吉祥林许个愿
     * 逛森林集市得能量,逛一逛618会场
     * 逛一逛点淘得红包,去一淘签到领红包
     */
    private fun receiveTaskAward() {
        try {
            // 修复：使用new HashSet包装从缓存获取的数据，兼容List/Set类型
            val presetBad: MutableSet<String?> = LinkedHashSet(
                mutableListOf<String?>(
                    "ENERGYRAIN",  //能量雨
                    "ENERGY_XUANJIAO",  //践行绿色行为
                    "FOREST_TOTAL_COLLECT_ENERGY_3",  //累积3天收自己能量
                    "TEST_LEAF_TASK",  //逛农场得落叶肥料
                    "SHARETASK" //邀请好友助力
                )
            )

            /* 3️⃣ 失败任务集合：空文件时自动创建空 HashSet 并立即落盘 */
            val typeRef: TypeReference<MutableSet<String?>> =
                object : TypeReference<MutableSet<String?>>() {}
            val badTaskSet: MutableSet<String?> =
                DataStore.getOrCreate("badForestTaskSet", typeRef)
            /* 3️⃣ 首次运行时把预设黑名单合并进去并立即落盘 */
            if (badTaskSet.isEmpty()) {
                badTaskSet.addAll(presetBad)
                put("badForestTaskSet", badTaskSet) // 持久化
            }

            while (true) {
                var doubleCheck = false // 标记是否需要再次检查任务
                val s = AntForestRpcCall.queryTaskList() // 查询任务列表
                val jo = JSONObject(s) // 解析响应为 JSON 对象

                if (!ResChecker.checkRes(TAG + "查询森林任务失败:", jo)) {
                    Log.record(jo.getString("resultDesc")) // 记录失败描述
                    Log.runtime(s) // 打印响应内容
                    break
                }

                // 提取森林任务列表
                val forestSignVOList = jo.getJSONArray("forestSignVOList")
                var sumawardCount = 0
                val dailyawardCount = dailyTask(forestSignVOList) // 执行每日任务
                sumawardCount += dailyawardCount

                // 提取森林任务
                val forestTasksNew = jo.optJSONArray("forestTasksNew")
                if (forestTasksNew == null || forestTasksNew.length() == 0) {
                    break // 如果没有新任务，则返回
                }

                // 遍历任务
                for (i in 0..<forestTasksNew.length()) {
                    val forestTask = forestTasksNew.getJSONObject(i)
                    val taskInfoList = forestTask.getJSONArray("taskInfoList") // 获取任务信息列表

                    for (j in 0..<taskInfoList.length()) {
                        val taskInfo = taskInfoList.getJSONObject(j)

                        val taskBaseInfo = taskInfo.getJSONObject("taskBaseInfo") // 获取任务基本信息
                        val taskType = taskBaseInfo.getString("taskType") // 获取任务类型
                        val sceneCode = taskBaseInfo.getString("sceneCode") // 获取场景代码
                        val taskStatus = taskBaseInfo.getString("taskStatus") // 获取任务状态

                        val bizInfo = JSONObject(taskBaseInfo.getString("bizInfo")) // 获取业务信息
                        val taskTitle = bizInfo.optString("taskTitle", taskType) // 获取任务标题

                        val taskRights = JSONObject(taskInfo.getString("taskRights")) // 获取任务权益
                        val awardCount = taskRights.optInt("awardCount", 0) // 获取奖励数量

                        // 判断任务状态
                        if (TaskStatus.FINISHED.name == taskStatus) {
                            // 领取任务奖励
                            val joAward = JSONObject(
                                AntForestRpcCall.receiveTaskAward(
                                    sceneCode,
                                    taskType
                                )
                            ) // 领取奖励请求
                            if (ResChecker.checkRes(TAG + "领取森林任务奖励失败:", joAward)) {
                                Log.forest("森林奖励🎖️[" + taskTitle + "]# " + awardCount + "活力值")
                                sumawardCount += awardCount
                                doubleCheck = true // 标记需要重新检查任务
                            } else {
                                Log.error(TAG, "领取失败: $taskTitle") // 记录领取失败信息
                                Log.runtime(joAward.toString()) // 打印奖励响应
                            }
                            GlobalThreadPools.sleepCompat(500)
                        } else if (TaskStatus.TODO.name == taskStatus) {
                            // 跳过已失败的任务
                            if (badTaskSet.contains(taskType)) continue

                            if (!badTaskSet.contains(taskType)) {
                                val bizKey = sceneCode + "_" + taskType
                                val count = forestTaskTryCount
                                    .computeIfAbsent(bizKey) { _: String? ->
                                        AtomicInteger(
                                            0
                                        )
                                    }
                                    .incrementAndGet()

                                // 完成任务请求
                                val joFinishTask = JSONObject(
                                    AntForestRpcCall.finishTask(
                                        sceneCode,
                                        taskType
                                    )
                                ) // 完成任务请求
                                if (count > 1) {
                                    Log.error(
                                        TAG,
                                        "完成森林任务失败超过1次$taskTitle\n$joFinishTask"
                                    ) // 记录完成任务失败信息
                                    badTaskSet.add(taskType)
                                    put("badForestTaskSet", badTaskSet)
                                } else {
                                    Log.forest("森林任务🧾️[$taskTitle]")
                                    doubleCheck = true // 标记需要重新检查任务
                                }
                            }
                        }

                        // 如果是游戏任务类型，查询并处理游戏任务
                        if ("mokuai_senlin_hlz" == taskType) {
                            // 游戏任务跳转
                            val gameUrl = bizInfo.getString("taskJumpUrl")
                            Log.runtime(TAG, "跳转到游戏: $gameUrl")

                            // 模拟跳转游戏任务URL（根据需要可能需要在客户端实际触发）
                            Log.runtime(TAG, "等待30S")
                            GlobalThreadPools.sleepCompat(30000) // 等待任务完成
                            // 完成任务请求
                            val joFinishTask = JSONObject(
                                AntForestRpcCall.finishTask(
                                    sceneCode,
                                    taskType
                                )
                            ) // 完成任务请求
                            if (ResChecker.checkRes(TAG + "完成游戏任务失败:", joFinishTask)) {
                                Log.forest("游戏任务完成 🎮️[" + taskTitle + "]# " + awardCount + "活力值")
                                sumawardCount += awardCount
                                doubleCheck = true // 标记需要重新检查任务
                            } else {
                                Log.error(TAG, "游戏任务完成失败: $taskTitle") // 记录任务完成失败信息
                            }
                        }
                    }
                }

                if (!doubleCheck) break
            }
        } catch (t: Throwable) {
            handleException("receiveTaskAward", t)
        }
    }

    /**
     * 在收集能量之前使用道具。
     * 这个方法检查是否需要使用增益卡
     * 并在需要时使用相应的道具。
     *
     * @param userId 用户的ID。
     */
    /**
     * 在收集能量之前决定是否使用增益类道具卡
     * @param userId 用户ID
     * @param skipPropCheck 是否跳过道具检查（快速收取通道）
     */
    private fun usePropBeforeCollectEnergy(userId: String?, skipPropCheck: Boolean = false) {
        try {
            // 🚀 快速收取通道：跳过道具检查，直接返回
            if (skipPropCheck) {
                Log.record(TAG, "⚡ 快速收取通道：跳过道具检查，加速蹲点收取")
                return
            }
            
            /*
             * 在收集能量之前决定是否使用增益类道具卡。
             *
             * 主要逻辑:
             * 1. 定义时间常量，用于判断道具剩余有效期。
             * 2. 获取当前时间及各类道具的到期时间，计算剩余时间。
             * 3. 根据以下条件判断是否需要使用特定道具:
             *    - needDouble: 双击卡开关已打开，且当前没有生效的双击卡。
             *    - needrobExpand: 1.1倍能量卡开关已打开，且当前没有生效的卡。
             *    - needStealth: 隐身卡开关已打开，且当前没有生效的隐身卡。
             *    - needShield: 保护罩开关已打开，炸弹卡开关已关闭，且保护罩剩余时间不足一天。
             *    - needEnergyBombCard: 炸弹卡开关已打开，保护罩开关已关闭，且炸弹卡剩余时间不足三天。
             *    - needBubbleBoostCard: 加速卡开关已打开。
             * 4. 如果有任何一个道具需要使用，则同步查询背包信息，并调用相应的使用道具方法。
             */

            val now = System.currentTimeMillis()
            // 双击卡判断
            val needDouble =
                doubleCard!!.value != ApplyPropType.CLOSE && shouldRenewDoubleCard(
                    doubleEndTime,
                    now
                )

            val needrobExpand =
                robExpandCard!!.value != ApplyPropType.CLOSE && robExpandCardEndTime < now
            val needStealth =
                stealthCard!!.value != ApplyPropType.CLOSE && stealthEndTime < now

            // 保护罩判断
            val needShield =
                (shieldCard!!.value != ApplyPropType.CLOSE) && energyBombCardType!!.value == ApplyPropType.CLOSE
                        && shouldRenewShield(shieldEndTime, now)
            // 炸弹卡判断
            val needEnergyBombCard =
                (energyBombCardType!!.value != ApplyPropType.CLOSE) && shieldCard!!.value == ApplyPropType.CLOSE
                        && shouldRenewEnergyBomb(energyBombCardEndTime, now)

            val needBubbleBoostCard = bubbleBoostCard!!.value != ApplyPropType.CLOSE

            Log.runtime(
                TAG, "道具使用检查: needDouble=" + needDouble + ", needrobExpand=" + needrobExpand +
                        ", needStealth=" + needStealth + ", needShield=" + needShield +
                        ", needEnergyBombCard=" + needEnergyBombCard + ", needBubbleBoostCard=" + needBubbleBoostCard
            )
            if (needDouble || needStealth || needShield || needEnergyBombCard || needrobExpand || needBubbleBoostCard) {
                synchronized(doubleCardLockObj) {
                    val bagObject = queryPropList()
                    // Log.runtime(TAG, "bagObject=" + (bagObject == null ? "null" : bagObject.toString()));
                    if (needDouble) useDoubleCard(bagObject!!) // 使用双击卡

                    if (needrobExpand) userobExpandCard() // 使用1.1倍能量卡

                    if (needStealth) useStealthCard(bagObject) // 使用隐身卡

                    if (needBubbleBoostCard) useCardBoot(
                        bubbleBoostTime!!.value,
                        "加速卡"
                    ) { this.useBubbleBoostCard() } // 使用加速卡
                    if (needShield) {
                        Log.runtime(TAG, "尝试使用保护罩罩")
                        useShieldCard(bagObject)
                    } else if (needEnergyBombCard) {
                        Log.runtime(TAG, "准备使用能量炸弹卡")
                        useEnergyBombCard(bagObject)
                    }
                }
            } else {
                Log.runtime(TAG, "没有需要使用的道具")
            }
        } catch (e: Exception) {
            Log.printStackTrace(e)
        }
    }

    fun useCardBoot(targetTimeValue: MutableList<String?>, propName: String?, func: Runnable) {
        for (targetTimeStr in targetTimeValue) {
            if ("-1" == targetTimeStr) {
                return
            }
            val targetTimeCalendar = TimeUtil.getTodayCalendarByTimeStr(targetTimeStr) ?: return
            val targetTime = targetTimeCalendar.getTimeInMillis()
            val now = System.currentTimeMillis()
            if (now > targetTime) {
                continue
            }
            val targetTaskId = "TAGET|$targetTime"
            if (!hasChildTask(targetTaskId)) {
                addChildTask(ChildModelTask(targetTaskId, "TAGET", func, targetTime))
                Log.record(
                    TAG,
                    "添加定时使用" + propName + "[" + UserMap.getCurrentMaskName() + "]在[" + TimeUtil.getCommonDate(
                        targetTime
                    ) + "]执行"
                )
            } else {
                addChildTask(ChildModelTask(targetTaskId, "TAGET", func, targetTime))
            }
        }
    }

    /**
     * 保护罩剩余时间判断
     * 以整数 HHmm 指定保护罩续写阈值。
     * 例如：2355 表示 23 小时 55 分钟，0955 可直接写为 955。
     * 校验规则：0 ≤ HH ≤ 99，0 ≤ mm ≤ 59；非法值将回退为默认值。
     */
    @SuppressLint("DefaultLocale")
    private fun shouldRenewShield(shieldEnd: Long, nowMillis: Long): Boolean {
        // 解析阈值配置
        var hours: Int
        var minutes: Int
        try {
            val abs = abs(SHIELD_RENEW_THRESHOLD_HHMM)
            hours = abs / 100 // 提取小时部分
            minutes = abs % 100 // 提取分钟部分
            // 可以添加分钟有效性检查
            if (minutes > 59) {
                Log.record(TAG, "[保护罩] 分钟数无效: $minutes, 使用默认值")
                hours = 23
                minutes = 59
            }
        } catch (e: Exception) {
            Log.record(TAG, "[保护罩] 解析阈值配置异常: " + e.message + ", 使用默认值")
            hours = 23
            minutes = 59
        }
        val thresholdMs = hours * TimeFormatter.ONE_HOUR_MS + minutes * TimeFormatter.ONE_MINUTE_MS
        
        // 检测异常数据
        if (shieldEnd > 0 && shieldEnd < nowMillis - 365 * TimeFormatter.ONE_DAY_MS) {
            Log.record(TAG, "[保护罩] ⚠️ 检测到异常时间数据(${TimeUtil.getCommonDate(shieldEnd)})，跳过检查")
            return false
        }
        
        if (shieldEnd in 1..nowMillis) { // 已过期
            Log.record(
                TAG,
                "[保护罩] 已过期，立即续写；end=" + TimeUtil.getCommonDate(shieldEnd) + ", now=" + TimeUtil.getCommonDate(
                    nowMillis
                )
            )
            return true
        }
        
        if (shieldEnd == 0L) { // 未生效
            Log.record(TAG, "[保护罩] 未生效，尝试使用")
            return true
        }
        val remain = shieldEnd - nowMillis
        val needRenew = remain <= thresholdMs
        // 格式化剩余时间和阈值时间为更直观的显示
        val remainTimeStr = TimeFormatter.formatRemainingTime(remain)
        val thresholdTimeStr = String.format("%02d小时%02d分", hours, minutes)
        if (needRenew) {
            Log.record(
                TAG, String.format(
                    "[保护罩] 🔄 需要续写 - 剩余时间[%s] ≤ 续写阈值[%s]",
                    remainTimeStr, thresholdTimeStr
                )
            )
        } else {
            Log.record(
                TAG, String.format(
                    "[保护罩] ✅ 无需续写 - 剩余时间[%s] > 续写阈值[%s]",
                    remainTimeStr, thresholdTimeStr
                )
            )
        }
        // 详细调试信息（可选）
        Log.runtime(
            TAG, String.format(
                "[保护罩] 详细对比: %dms ≤ %dms = %s",
                remain, thresholdMs, needRenew
            )
        )
        return needRenew
    }

    /**
     * 炸弹卡剩余时间判断
     * 当炸弹卡剩余时间低于3天时，需要续用
     * 最多可续用到4天
     */
    @SuppressLint("DefaultLocale")
    private fun shouldRenewEnergyBomb(bombEnd: Long, nowMillis: Long): Boolean {
        // 炸弹卡最长有效期为4天
        val maxBombDuration = 4 * TimeFormatter.ONE_DAY_MS
        // 炸弹卡续用阈值为3天
        val bombRenewThreshold = 3 * TimeFormatter.ONE_DAY_MS
        // 检测异常数据
        if (bombEnd > 0 && bombEnd < nowMillis - 365 * TimeFormatter.ONE_DAY_MS) {
            Log.record(TAG, "[炸弹卡] ⚠️ 检测到异常时间数据(${TimeUtil.getCommonDate(bombEnd)})，跳过检查")
            return false
        }
        
        if (bombEnd in 1..nowMillis) { // 已过期
            Log.runtime(
                TAG,
                "[炸弹卡] 已过期，立即续写；end=" + TimeUtil.getCommonDate(bombEnd) + ", now=" + TimeUtil.getCommonDate(
                    nowMillis
                )
            )
            return true
        }
        
        if (bombEnd == 0L) { // 未生效
            Log.record(TAG, "[炸弹卡] 未生效，尝试使用")
            return true
        }
        val remain = bombEnd - nowMillis
        // 如果剩余时间小于阈值且续写后总时长未超过最大有效期，则需要续用
        // 续写后结束时间 = bombEnd + 1天，续写后总时长 = 续写后结束时间 - 现在时间
        val renewDuration = TimeFormatter.ONE_DAY_MS // 每次续写增加1天
        val afterRenewRemain = remain + renewDuration // 续写后的剩余时间
        val needRenew =
            remain <= bombRenewThreshold && afterRenewRemain <= maxBombDuration

        val remainTimeStr = TimeFormatter.formatRemainingTime(remain)
        val thresholdTimeStr = TimeFormatter.formatRemainingTime(bombRenewThreshold)

        if (needRenew) {
            Log.runtime(
                TAG, String.format(
                    "[炸弹卡] 🔄 需要续写 - 剩余时间[%s] ≤ 续写阈值[%s]",
                    remainTimeStr, thresholdTimeStr
                )
            )
        } else {
            Log.runtime(
                TAG, String.format(
                    "[炸弹卡] ✅ 无需续写 - 剩余时间[%s] > 续写阈值[%s]",
                    remainTimeStr, thresholdTimeStr
                )
            )
        }

        // 详细调试信息
        Log.runtime(
            TAG, String.format(
                "[炸弹卡] 详细对比: 剩余时间=%dms ≤ 阈值=%dms = %s, 续写后时长=%dms ≤ 最大时长=%dms = %s",
                remain, bombRenewThreshold, (remain <= bombRenewThreshold),
                afterRenewRemain, maxBombDuration, (afterRenewRemain <= maxBombDuration)
            )
        )

        return needRenew
    }

    /**
     * 双击卡剩余时间判断
     * 当双击卡剩余时间低于31天时，需要续用
     * 最多可续用到31+31天，但不建议，因为平时有5分钟、3天、7天等短期双击卡
     */
    @SuppressLint("DefaultLocale")
    private fun shouldRenewDoubleCard(doubleEnd: Long, nowMillis: Long): Boolean {
        // 双击卡最长有效期为62天（31+31）
        // 双击卡续用阈值为31天
        val doubleRenewThreshold = 31 * TimeFormatter.ONE_DAY_MS  // 改为小写开头
        
        // 如果doubleEnd为0或很久以前的时间（超过1年），说明数据未初始化或有问题
        if (doubleEnd > 0 && doubleEnd < nowMillis - 365 * TimeFormatter.ONE_DAY_MS) {
            Log.record(TAG, "[双击卡] ⚠️ 检测到异常时间数据(${TimeUtil.getCommonDate(doubleEnd)})，跳过检查")
            return false // 数据异常，不续用
        }
        
        if (doubleEnd in 1..nowMillis) { // 已过期
            Log.record(
                TAG,
                "[双击卡] 已过期，立即续写；end=" + TimeUtil.getCommonDate(doubleEnd) + ", now=" + TimeUtil.getCommonDate(
                    nowMillis
                )
            )
            return true
        }
        
        if (doubleEnd == 0L) { // 未生效（初始值）
            Log.record(TAG, "[双击卡] 未生效，尝试使用")
            return true
        }

        val remain = doubleEnd - nowMillis
        // 如果剩余时间小于阈值，则需要续用
        val needRenew = remain <= doubleRenewThreshold  // 使用修正后的变量名
        val remainTimeStr = TimeFormatter.formatRemainingTime(remain)
        val thresholdTimeStr = TimeFormatter.formatRemainingTime(doubleRenewThreshold)  // 使用修正后的变量名

        if (needRenew) {
            Log.record(
                TAG, String.format(
                    "[双击卡] 🔄 需要续写 - 剩余时间[%s] ≤ 续写阈值[%s]",
                    remainTimeStr, thresholdTimeStr
                )
            )
        } else {
            Log.record(
                TAG, String.format(
                    "[双击卡] ✅ 无需续写 - 剩余时间[%s] > 续写阈值[%s]",
                    remainTimeStr, thresholdTimeStr
                )
            )
        }
        return needRenew
    }

    /**
     * 检查当前时间是否在设置的使用双击卡时间内
     *
     * @return 如果当前时间在双击卡的有效时间范围内，返回true；否则返回false。
     */
    private fun hasDoubleCardTime(): Boolean {
        val currentTimeMillis = System.currentTimeMillis()
        return TimeUtil.checkInTimeRange(currentTimeMillis, doubleCardTime!!.value)
    }

    private fun giveProp() {
        val set = whoYouWantToGiveTo!!.value
        if (!set.isEmpty()) {
            for (userId in set) {
                if (selfId != userId) {
                    giveProp(userId)
                    break
                }
            }
        }
    }

    /**
     * 向指定用户赠送道具。 这个方法首先查询可用的道具列表，然后选择一个道具赠送给目标用户。 如果有多个道具可用，会尝试继续赠送，直到所有道具都赠送完毕。
     *
     * @param targetUserId 目标用户的ID。
     */
    private fun giveProp(targetUserId: String?) {
        try {
            do {
                // 查询道具列表
                val propListJo = JSONObject(AntForestRpcCall.queryPropList(true))
                if (ResChecker.checkRes(TAG + "查询道具列表失败:", propListJo)) {
                    val forestPropVOList = propListJo.optJSONArray("forestPropVOList")
                    if (forestPropVOList != null && forestPropVOList.length() > 0) {
                        val propJo = forestPropVOList.getJSONObject(0)
                        val giveConfigId =
                            propJo.getJSONObject("giveConfigVO").getString("giveConfigId")
                        val holdsNum = propJo.optInt("holdsNum", 0)
                        val propName = propJo.getJSONObject("propConfigVO").getString("propName")
                        val propId = propJo.getJSONArray("propIdList").getString(0)
                        val giveResultJo = JSONObject(
                            AntForestRpcCall.giveProp(
                                giveConfigId,
                                propId,
                                targetUserId
                            )
                        )
                        if (ResChecker.checkRes(TAG + "赠送道具失败:", giveResultJo)) {
                            Log.forest("赠送道具🎭[" + UserMap.getMaskName(targetUserId) + "]#" + propName)
                            GlobalThreadPools.sleepCompat(1500)
                        } else {
                            val rt = giveResultJo.getString("resultDesc")
                            Log.record(rt)
                            Log.runtime(giveResultJo.toString())
                            if (rt.contains("异常")) {
                                return
                            }
                        }
                        // 如果持有数量大于1或道具列表中有多于一个道具，则继续赠送
                        if (holdsNum <= 1 && forestPropVOList.length() == 1) {
                            break
                        }
                    }
                } else {
                    // 如果查询道具列表失败，则记录失败的日志
                    Log.record(TAG, "赠送道具查询结果" + propListJo.getString("resultDesc"))
                }
                // 等待1.5秒后再继续
            } while (true)
        } catch (th: Throwable) {
            // 打印异常信息
            Log.runtime(TAG, "giveProp err")
            Log.printStackTrace(TAG, th)
        }
    }

    /**
     * 查询并管理用户巡护任务
     */
    private fun queryUserPatrol() {
        val waitTime = 300L //增大查询等待时间，减少异常
        try {
            do {
                // 查询当前巡护任务
                var jo = JSONObject(AntForestRpcCall.queryUserPatrol())
                // GlobalThreadPools.sleepCompat(waitTime);
                // 如果查询成功
                if (ResChecker.checkRes(TAG + "查询巡护任务失败:", jo)) {
                    // 查询我的巡护记录
                    var resData = JSONObject(AntForestRpcCall.queryMyPatrolRecord())
                    // GlobalThreadPools.sleepCompat(waitTime);
                    if (resData.optBoolean("canSwitch")) {
                        val records = resData.getJSONArray("records")
                        for (i in 0..<records.length()) {
                            val record = records.getJSONObject(i)
                            val userPatrol = record.getJSONObject("userPatrol")
                            // 如果存在未到达的节点，且当前模式为"silent"，则尝试切换巡护地图
                            if (userPatrol.getInt("unreachedNodeCount") > 0) {
                                if ("silent" == userPatrol.getString("mode")) {
                                    val patrolConfig = record.getJSONObject("patrolConfig")
                                    val patrolId = patrolConfig.getString("patrolId")
                                    resData =
                                        JSONObject(AntForestRpcCall.switchUserPatrol(patrolId))
                                    GlobalThreadPools.sleepCompat(waitTime)
                                    // 如果切换成功，打印日志并继续
                                    if (ResChecker.checkRes(TAG + "切换巡护地图失败:", resData)) {
                                        Log.forest("巡护⚖️-切换地图至$patrolId")
                                    }
                                    continue  // 跳过当前循环
                                }
                                break // 如果当前不是silent模式，则结束循环
                            }
                        }
                    }
                    // 获取用户当前巡护状态信息
                    val userPatrol = jo.getJSONObject("userPatrol")
                    val currentNode = userPatrol.getInt("currentNode")
                    val currentStatus = userPatrol.getString("currentStatus")
                    val patrolId = userPatrol.getInt("patrolId")
                    val chance = userPatrol.getJSONObject("chance")
                    val leftChance = chance.getInt("leftChance")
                    val leftStep = chance.getInt("leftStep")
                    val usedStep = chance.getInt("usedStep")
                    if ("STANDING" == currentStatus) { // 当前巡护状态为"STANDING"
                        if (leftChance > 0) { // 如果还有剩余的巡护次数，则开始巡护
                            jo = JSONObject(AntForestRpcCall.patrolGo(currentNode, patrolId))
                            GlobalThreadPools.sleepCompat(waitTime)
                            patrolKeepGoing(jo.toString(), currentNode, patrolId) // 继续巡护
                            continue  // 跳过当前循环
                        } else if (leftStep >= 2000 && usedStep < 10000) { // 如果没有剩余的巡护次数但步数足够，则兑换巡护次数
                            jo = JSONObject(AntForestRpcCall.exchangePatrolChance(leftStep))
                            // GlobalThreadPools.sleepCompat(waitTime);
                            if (ResChecker.checkRes(TAG + "兑换巡护次数失败:", jo)) { // 兑换成功，增加巡护次数
                                val addedChance = jo.optInt("addedChance", 0)
                                Log.forest("步数兑换⚖️[巡护次数*$addedChance]")
                                continue  // 跳过当前循环
                            } else {
                                Log.runtime(TAG, jo.getString("resultDesc"))
                            }
                        }
                    } else if ("GOING" == currentStatus) {
                        patrolKeepGoing(null, currentNode, patrolId)
                    }
                } else {
                    Log.runtime(TAG, jo.getString("resultDesc"))
                }
                break // 完成一次巡护任务后退出循环
            } while (true)
        } catch (t: Throwable) {
            Log.runtime(TAG, "queryUserPatrol err")
            Log.printStackTrace(TAG, t) // 打印异常堆栈
        }
    }

    /**
     * 持续巡护森林，直到巡护状态不再是"进行中"
     *
     * @param s         巡护请求的响应字符串，若为null将重新请求
     * @param nodeIndex 当前节点索引
     * @param patrolId  巡护任务ID
     */
    private fun patrolKeepGoing(s: String?, nodeIndex: Int, patrolId: Int) {
        var s = s
        try {
            do {
                if (s == null) {
                    s = AntForestRpcCall.patrolKeepGoing(nodeIndex, patrolId, "image")
                }
                val jo: JSONObject?
                try {
                    jo = JSONObject(s)
                } catch (e: JSONException) {
                    Log.record(TAG, "JSON解析错误: " + e.message)
                    Log.printStackTrace(TAG, e)
                    return  // 解析失败，退出循环
                }
                if (!ResChecker.checkRes(TAG, jo)) {
                    Log.runtime(TAG, jo.getString("resultDesc"))
                    break
                }
                val events = jo.optJSONArray("events")
                if (events == null || events.length() == 0) {
                    return  // 无事件，退出循环
                }
                val event = events.getJSONObject(0)
                val userPatrol = jo.getJSONObject("userPatrol")
                val currentNode = userPatrol.getInt("currentNode")
                // 获取奖励信息，并处理动物碎片奖励
                val rewardInfo = event.optJSONObject("rewardInfo")
                if (rewardInfo != null) {
                    val animalProp = rewardInfo.optJSONObject("animalProp")
                    if (animalProp != null) {
                        val animal = animalProp.optJSONObject("animal")
                        if (animal != null) {
                            Log.forest("巡护森林🏇🏻[" + animal.getString("name") + "碎片]")
                        }
                    }
                }
                // 如果巡护状态不是"进行中"，则退出循环
                if ("GOING" != jo.getString("currentStatus")) {
                    return
                }
                // 请求继续巡护
                val materialInfo = event.getJSONObject("materialInfo")
                val materialType = materialInfo.optString("materialType", "image")
                s = AntForestRpcCall.patrolKeepGoing(currentNode, patrolId, materialType)
                GlobalThreadPools.sleepCompat(100) // 等待100毫秒后继续巡护
            } while (true)
        } catch (t: Throwable) {
            Log.runtime(TAG, "patrolKeepGoing err")
            Log.printStackTrace(TAG, t)
        }
    }

    /**
     * 查询并派遣伙伴
     */
    private fun queryAndConsumeAnimal() {
        try {
            // 查询动物属性列表
            var jo = JSONObject(AntForestRpcCall.queryAnimalPropList())
            if (!ResChecker.checkRes(TAG, jo)) {
                Log.runtime(TAG, jo.getString("resultDesc"))
                return
            }
            // 获取所有动物属性并选择可以派遣的伙伴
            val animalProps = jo.getJSONArray("animalProps")
            var bestAnimalProp: JSONObject? = null
            for (i in 0..<animalProps.length()) {
                jo = animalProps.getJSONObject(i)
                if (bestAnimalProp == null || jo.getJSONObject("main")
                        .getInt("holdsNum") > bestAnimalProp.getJSONObject("main")
                        .getInt("holdsNum")
                ) {
                    bestAnimalProp = jo // 默认选择最大数量的伙伴
                }
            }
            // 派遣伙伴
            consumeAnimalProp(bestAnimalProp)
        } catch (t: Throwable) {
            Log.runtime(TAG, "queryAnimalPropList err")
            Log.printStackTrace(TAG, t)
        }
    }

    /**
     * 派遣伙伴进行巡护
     *
     * @param animalProp 选择的动物属性
     */
    private fun consumeAnimalProp(animalProp: JSONObject?) {
        if (animalProp == null) return  // 如果没有可派遣的伙伴，则返回

        try {
            // 获取伙伴的属性信息
            val propGroup = animalProp.getJSONObject("main").getString("propGroup")
            val propType = animalProp.getJSONObject("main").getString("propType")
            val name = animalProp.getJSONObject("partner").getString("name")
            // 调用API进行伙伴派遣
            val jo = JSONObject(AntForestRpcCall.consumeProp(propGroup, propType, false))
            if (ResChecker.checkRes(TAG + "巡护派遣失败:", jo)) {
                Log.forest("巡护派遣🐆[$name]")
            } else {
                Log.runtime(TAG, jo.getString("resultDesc"))
            }
        } catch (t: Throwable) {
            Log.runtime(TAG, "consumeAnimalProp err")
            Log.printStackTrace(TAG, t)
        }
    }

    /**
     * 查询动物及碎片信息，并尝试合成可合成的动物碎片。
     */
    private fun queryAnimalAndPiece() {
        try {
            // 调用远程接口查询动物及碎片信息
            val response = JSONObject(AntForestRpcCall.queryAnimalAndPiece(0))
            val resultCode = response.optString("resultCode")
            // 检查接口调用是否成功
            if ("SUCCESS" != resultCode) {
                Log.runtime(TAG, "查询失败: " + response.optString("resultDesc"))
                return
            }
            // 获取动物属性列表
            val animalProps = response.optJSONArray("animalProps")
            if (animalProps == null || animalProps.length() == 0) {
                Log.runtime(TAG, "动物属性列表为空")
                return
            }
            // 遍历动物属性
            for (i in 0..<animalProps.length()) {
                val animalObject = animalProps.optJSONObject(i) ?: continue
                val pieces = animalObject.optJSONArray("pieces")
                if (pieces == null || pieces.length() == 0) {
                    Log.runtime(TAG, "动物碎片列表为空")
                    continue
                }
                val animalId =
                    animalObject.optJSONObject("animal")?.optInt("id", -1) ?: -1
                if (animalId == -1) {
                    Log.runtime(TAG, "动物ID缺失")
                    continue
                }
                // 检查碎片是否满足合成条件
                if (canCombinePieces(pieces)) {
                    combineAnimalPiece(animalId)
                }
            }
        } catch (e: Exception) {
            Log.runtime(TAG, "查询动物及碎片信息时发生错误:")
            Log.printStackTrace(TAG, e)
        }
    }

    /**
     * 检查碎片是否满足合成条件。
     *
     * @param pieces 动物碎片数组
     * @return 如果所有碎片满足合成条件，返回 true；否则返回 false
     */
    private fun canCombinePieces(pieces: JSONArray): Boolean {
        for (j in 0..<pieces.length()) {
            val pieceObject = pieces.optJSONObject(j)
            if (pieceObject == null || pieceObject.optInt("holdsNum", 0) <= 0) {
                return false
            }
        }
        return true
    }

    /**
     * 合成动物碎片。
     *
     * @param animalId 动物ID
     */
    private fun combineAnimalPiece(animalId: Int) {
        var animalId = animalId
        try {
            while (true) {
                // 查询动物及碎片信息
                val response = JSONObject(AntForestRpcCall.queryAnimalAndPiece(animalId))
                var resultCode = response.optString("resultCode")
                if ("SUCCESS" != resultCode) {
                    Log.runtime(TAG, "查询失败: " + response.optString("resultDesc"))
                    break
                }
                val animalProps = response.optJSONArray("animalProps")
                if (animalProps == null || animalProps.length() == 0) {
                    Log.runtime(TAG, "动物属性数据为空")
                    break
                }
                // 获取第一个动物的属性
                val animalProp = animalProps.getJSONObject(0)
                val animal: JSONObject = checkNotNull(animalProp.optJSONObject("animal"))
                val id = animal.optInt("id", -1)
                val name = animal.optString("name", "未知动物")
                // 获取碎片信息
                val pieces = animalProp.optJSONArray("pieces")
                if (pieces == null || pieces.length() == 0) {
                    Log.runtime(TAG, "碎片数据为空")
                    break
                }
                var canCombineAnimalPiece = true
                val piecePropIds = JSONArray()
                // 检查所有碎片是否可用
                for (j in 0..<pieces.length()) {
                    val piece = pieces.optJSONObject(j)
                    if (piece == null || piece.optInt("holdsNum", 0) <= 0) {
                        canCombineAnimalPiece = false
                        Log.runtime(TAG, "碎片不足，无法合成动物")
                        break
                    }
                    // 添加第一个道具ID
                    piece.optJSONArray("propIdList")?.optString(0, "")?.let { propId ->
                        piecePropIds.put(propId)
                    }
                }
                // 如果所有碎片可用，则尝试合成
                if (canCombineAnimalPiece) {
                    val combineResponse =
                        JSONObject(AntForestRpcCall.combineAnimalPiece(id, piecePropIds.toString()))
                    resultCode = combineResponse.optString("resultCode")
                    if ("SUCCESS" == resultCode) {
                        Log.forest("成功合成动物💡[$name]")
                        animalId = id
                        GlobalThreadPools.sleepCompat(100) // 等待一段时间再查询
                        continue
                    } else {
                        Log.runtime(TAG, "合成失败: " + combineResponse.optString("resultDesc"))
                    }
                }
                break // 如果不能合成或合成失败，跳出循环
            }
        } catch (e: Exception) {
            Log.runtime(TAG, "合成动物碎片时发生错误:")
            Log.printStackTrace(TAG, e)
        }
    }

    /**
     * 获取背包信息
     */
    private fun queryPropList(): JSONObject? {
        return queryPropList(false)
    }

    @Synchronized
    private fun queryPropList(forceRefresh: Boolean): JSONObject? {
        val now = System.currentTimeMillis()
        if (!forceRefresh && cachedBagObject != null && now - lastQueryPropListTime < 5000) {
            return cachedBagObject
        }
        try {
            Log.record(TAG, "刷新背包...")
            val response = AntForestRpcCall.queryPropList(false)
            // 检查响应是否为空，避免解析空字符串导致异常
            if (response.isNullOrBlank()) {
                Log.record(TAG, "刷新背包失败: 响应为空")
                return null
            }
            val bagObject = JSONObject(response)
            if (bagObject.optBoolean("success")) {
                cachedBagObject = bagObject
                lastQueryPropListTime = now
                return bagObject
            } else {
                Log.record(TAG, "刷新背包失败: " + bagObject.optString("resultDesc"))
            }
        } catch (th: Throwable) {
            handleException("queryPropList", th)
        }
        return null
    }

    /**
     * 查找背包道具
     *
     * @param bagObject 背包对象
     * @param propType  道具类型 LIMIT_TIME_ENERGY_SHIELD_TREE,...
     */
    private fun findPropBag(bagObject: JSONObject?, propType: String): JSONObject? {
        if (Objects.isNull(bagObject)) {
            return null
        }
        try {
            val forestPropVOList = bagObject!!.getJSONArray("forestPropVOList")
            for (i in 0..<forestPropVOList.length()) {
                val forestPropVO = forestPropVOList.getJSONObject(i)
                val propConfigVO = forestPropVO.getJSONObject("propConfigVO")
                val currentPropType = propConfigVO.getString("propType")
                // String propName = propConfigVO.getString("propName");
                if (propType == currentPropType) {
                    return forestPropVO // 找到后直接返回
                }
            }
        } catch (e: Exception) {
            Log.error(TAG, "查找背包道具出错:")
            Log.printStackTrace(TAG, e)
        }

        return null // 未找到或出错时返回 null
    }

    /**
     * 返回背包道具信息
     */
    private fun showBag() {
        val bagObject = queryPropList(true)
        if (Objects.isNull(bagObject)) {
            return
        }
        try {
            val forestPropVOList = bagObject?.optJSONArray("forestPropVOList") ?: return

            val logBuilder = StringBuilder("\n======= 背包道具列表 =======\n")
            for (i in 0..<forestPropVOList.length()) {
                val prop = forestPropVOList.optJSONObject(i) ?: continue

                val propConfig = prop.optJSONObject("propConfigVO") ?: continue

                val propName = propConfig.optString("propName")
                val propType = prop.optString("propType")
                val holdsNum = prop.optInt("holdsNum")
                val expireTime = prop.optLong("recentExpireTime", 0)
                logBuilder.append("道具: ").append(propName)
                    .append(" | 数量: ").append(holdsNum)
                    .append(" | 类型: ").append(propType)
                if (expireTime > 0) {
                    val formattedDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        .format(Date(expireTime))
                    logBuilder.append(" | 过期时间: ").append(formattedDate)
                }
                logBuilder.append("\n")
            }
            logBuilder.append("==========================")
            Log.record(TAG, logBuilder.toString())
        } catch (e: Exception) {
            Log.error(TAG, "解析背包道具出错:")
            Log.printStackTrace(TAG, e)
        }
    }

    /**
     * 使用背包道具
     *
     * @param propJsonObj 道具对象
     */
    private fun usePropBag(propJsonObj: JSONObject?): Boolean {
        if (propJsonObj == null) {
            Log.record(TAG, "要使用的道具不存在！")
            return false
        }
        try {
            val propId = propJsonObj.getJSONArray("propIdList").getString(0)
            val propConfigVO = propJsonObj.getJSONObject("propConfigVO")
            val propType = propConfigVO.getString("propType")
            val propName = propConfigVO.getString("propName")
            propEmoji(propName)
            val jo: JSONObject?
            val isRenewable = isRenewableProp(propType)
            Log.record(
                TAG,
                "道具 $propName (类型: $propType), 是否可续用: $isRenewable"
            )
            val propGroup = AntForestRpcCall.getPropGroup(propType)
            if (isRenewable) {
                // 第一步：发送检查/尝试使用请求 (secondConfirm=false)
                val checkResponseStr =
                    AntForestRpcCall.consumeProp(propGroup, propId, propType, false)
                val checkResponse = JSONObject(checkResponseStr)
                // Log.record(TAG, "发送检查请求: " + checkResponse);
                var resData = checkResponse.optJSONObject("resData")
                if (resData == null) {
                    resData = checkResponse
                }
                val status = resData.optString("usePropStatus")
                if ("NEED_CONFIRM_CAN_PROLONG" == status) {
                    // 情况1: 需要二次确认 (真正地续写)
                    Log.record(TAG, propName + "需要二次确认，发送确认请求...")
                    GlobalThreadPools.sleepCompat(2000)
                    val confirmResponseStr =
                        AntForestRpcCall.consumeProp(propGroup, propId, propType, true)
                    jo = JSONObject(confirmResponseStr)
                    Log.record(TAG, "发送确认请求: $jo")
                } else {
                    // 其他所有情况都视为最终结果，通常是失败
                    Log.record(TAG, "道具状态异常或使用失败。")
                    jo = checkResponse
                }
            } else {
                // 非续用类道具，直接使用
                Log.record(TAG, "非续用类道具，直接使用")
                val consumeResponse = AntForestRpcCall.consumeProp2(propGroup, propId, propType)
                jo = JSONObject(consumeResponse)
            }

            // 统一结果处理
            if (ResChecker.checkRes(TAG + "使用道具失败:", jo)) {
                updateSelfHomePage()
                return true
            } else {
                var errorData = jo.optJSONObject("resData")
                if (errorData == null) {
                    errorData = jo
                }
                val resultDesc = errorData.optString("resultDesc", "未知错误")
                Log.record("使用道具失败: $resultDesc")
                Toast.show(resultDesc)
                return false
            }
        } catch (th: Throwable) {
            Log.runtime(TAG, "usePropBag err")
            Log.printStackTrace(TAG, th)
            return false
        }
    }

    /**
     * 判断是否是可续用类道具
     */
    private fun isRenewableProp(propType: String): Boolean {
        return propType.contains("SHIELD") // 保护罩
                || propType.contains("BOMB_CARD") // 炸弹卡
                || propType.contains("DOUBLE_CLICK") // 双击卡
    }

    /**
     * 使用双击卡道具
     * 功能：在指定时间内，使好友的一个能量球可以收取两次
     *
     * @param bagObject 背包的JSON对象
     */
    private fun useDoubleCard(bagObject: JSONObject) {
        try {
            // 前置检查1: 检查今日使用次数是否已达上限
            if (!Status.canDoubleToday()) {
                Log.record(TAG, "双击卡使用条件检查: 今日次数已达上限")
                return
            }
            // 前置检查2: 校验背包数据是否有效
            if (!bagObject.optBoolean("success")) {
                Log.record(TAG, "背包数据异常，无法使用双击卡$bagObject")
                return
            }

            val forestPropVOList = bagObject.optJSONArray("forestPropVOList") ?: return

            // 永动机逻辑：如果背包内没有双击卡且开启了永动机，尝试兑换
            var hasProp = false
            for (i in 0..<forestPropVOList.length()) {
                val prop = forestPropVOList.optJSONObject(i)
                if (prop != null && "doubleClick" == prop.optString("propGroup")) {
                    hasProp = true
                    break
                }
            }

            if (!hasProp && doubleCardConstant!!.value) {
                Log.runtime(TAG, "背包中没有双击卡，尝试兑换...")
                if (exchangeDoubleCard()) {
                    // 重新获取背包数据
                    val newBagObject = queryPropList()
                    if (newBagObject != null) {
                        val newForestPropVOList = newBagObject.optJSONArray("forestPropVOList")
                        if (newForestPropVOList != null) {
                            // 递归调用，使用新的背包数据
                            useDoubleCard(newBagObject)
                            return
                        }
                    }
                }
            }

            // 步骤1: 根据用户UI设置，筛选出需要使用的双击卡
            val doubleClickProps: MutableList<JSONObject> = ArrayList()
            val choice = doubleCard!!.value
            for (i in 0..<forestPropVOList.length()) {
                val prop = forestPropVOList.optJSONObject(i)
                if (prop != null && "doubleClick" == prop.optString("propGroup")) {
                    if (choice == ApplyPropType.ALL) {
                        // 设置为"所有道具": 添加所有双击卡
                        doubleClickProps.add(prop)
                    } else if (choice == ApplyPropType.ONLY_LIMIT_TIME) {
                        // 设置为"限时道具": 只添加用于续期的卡 (名字含LIMIT_TIME或DAYS)
                        val propType = prop.optString("propType")
                        if (propType.contains("LIMIT_TIME") || propType.contains("DAYS")) {
                            doubleClickProps.add(prop)
                        }
                    }
                }
            }
            if (doubleClickProps.isEmpty()) {
                Log.record(TAG, "根据设置，背包中没有需要使用的双击卡")
                return
            }

            // 步骤2: 按过期时间升序排序，优先使用即将过期的卡，避免浪费
            Collections.sort(
                doubleClickProps,
                Comparator { p1: JSONObject?, p2: JSONObject? ->
                    val expireTime1 = p1!!.optLong("recentExpireTime", Long.MAX_VALUE)
                    val expireTime2 = p2!!.optLong("recentExpireTime", Long.MAX_VALUE)
                    expireTime1.compareTo(expireTime2)
                })

            Log.record(TAG, "扫描到" + doubleClickProps.size + "种双击卡，将按过期顺序尝试使用...")

            // 步骤3: 遍历筛选并排序后的列表，逐个尝试使用
            var success = false
            for (propObj in doubleClickProps) {
                val propType = propObj.optString("propType")
                val propName =
                    propObj.optJSONObject("propConfigVO")?.optString("propName") ?: ""

                // 特定条件检查1: 如果是普通的5分钟卡，需要检查是否在指定时间段内
                if ("ENERGY_DOUBLE_CLICK" == propType && !hasDoubleCardTime()) {
                    Log.record(TAG, "跳过[$propName]，当前不在指定使用时间段内")
                    continue  // 跳过，尝试下一张
                }

                // 特定条件检查2: 如果是限时卡，并且设置为"仅限时道具"，则只在过期前1天内使用
                if ("LIMIT_TIME_ENERGY_DOUBLE_CLICK" == propType && choice == ApplyPropType.ONLY_LIMIT_TIME) {
                    val expireTime = propObj.optLong("recentExpireTime", 0)
                    if (expireTime > 0 && (expireTime - System.currentTimeMillis() > 24 * 60 * 60 * 1000L)) {
                        Log.record(TAG, "跳过[$propName]，该卡有效期剩余超过1天 (仅限时模式)")
                        continue  // 跳过，尝试下一张
                    }
                }

                // 尝试使用道具
                Log.runtime(TAG, "尝试使用卡: $propName")
                if (usePropBag(propObj)) {
                    // 使用成功，更新状态并结束循环
                    doubleEndTime = System.currentTimeMillis() + 5 * TimeFormatter.ONE_MINUTE_MS
                    Status.DoubleToday()
                    success = true
                    break
                }
            }

            if (!success) {
                Log.record(TAG, "所有可用的双击卡均不满足使用条件")
            }
        } catch (th: Throwable) {
            handleException("useDoubleCard", th)
        }
    }

    /**
     * 使用隐身卡道具
     * 功能：隐藏收取行为，避免被好友发现偷取能量
     *
     * @param bagObject 背包的JSON对象
     */
    private fun useStealthCard(bagObject: JSONObject?) {
        val config = PropConfig(
            "隐身卡",
            arrayOf<String>("LIMIT_TIME_STEALTH_CARD", "STEALTH_CARD"),
            null,  // 无特殊条件
            { this.exchangeStealthCard() },
            { time: Long? -> stealthEndTime = time!! + TimeFormatter.ONE_DAY_MS }
        )

        usePropTemplate(bagObject, config, stealthCardConstant!!.value)
    }

    /**
     * 使用保护罩道具
     * 功能：保护自己的能量不被好友偷取，防止能量被收走
     * 一般是限时保护罩，可通过青春特权森林道具领取
     *
     * @param bagObject 背包的JSON对象
     */
    private fun useShieldCard(bagObject: JSONObject?) {
        try {
            Log.record(TAG, "尝试使用保护罩...")
            var jo = findPropBag(bagObject, "LIMIT_TIME_ENERGY_SHIELD_TREE")
            if (jo == null) {
                Log.record(
                    TAG,
                    "背包中没有森林保护罩(LIMIT_TIME_ENERGY_SHIELD_TREE)，继续查找其他类型..."
                )
                if (youthPrivilege!!.value) {
                    Log.runtime(TAG, "尝试通过青春特权获取保护罩...")
                    if (youthPrivilege()) {
                        jo = findPropBag(querySelfHome(), "LIMIT_TIME_ENERGY_SHIELD_TREE")
                    }
                }
            }
            if (jo == null) {
                if (shieldCardConstant!!.value) {
                    Log.record(TAG, "尝试通过活力值兑换保护罩...")
                    if (exchangeEnergyShield()) {
                        jo = findPropBag(querySelfHome(), "LIMIT_TIME_ENERGY_SHIELD")
                    }
                }
            }
            if (jo == null) {
                Log.record(TAG, "尝试能量保护罩(ENERGY_SHIELD)...")
                jo = findPropBag(bagObject, "ENERGY_SHIELD")
            }
            if (jo != null) {
                Log.runtime(TAG, "找到保护罩，准备使用: $jo")
                if (usePropBag(jo)) {
                    return  // 使用成功，直接返回
                }
            }
            Log.record(TAG, "背包中未找到任何可用保护罩。")
            // 如果未使用成功，也刷新一次
            updateSelfHomePage()
        } catch (th: Throwable) {
            Log.error(TAG + "使用能量保护罩， err")
            Log.printStackTrace(th)
        }
    }


    /**
     * 使用加速卡道具
     * 功能：加速能量球成熟时间，让等待中的能量球提前成熟，并立即收取自己的能量
     */
    private fun useBubbleBoostCard(bag: JSONObject? = queryPropList()) {
        try {
            // 在背包中查询限时加速器
            var jo = findPropBag(bag, "LIMIT_TIME_ENERGY_BUBBLE_BOOST")
            if (jo == null) {
                youthPrivilege()
                jo = findPropBag(queryPropList(), "LIMIT_TIME_ENERGY_BUBBLE_BOOST") // 重新查找
                if (jo == null) {
                    jo = findPropBag(bag, "BUBBLE_BOOST") // 尝试查找 普通加速器，一般用不到
                }
            }
            if (jo != null) {
                val propName = jo.getJSONObject("propConfigVO").getString("propName")

                // 使用加速卡
                if (usePropBag(jo)) {
                    Log.forest("使用加速卡🌪[$propName]")

                    // 🚀 关键改动：加速卡立即生效，立即尝试收取自己能量两次
                    Log.record(TAG, "🚀 加速卡使用成功，立即尝试收取自己能量...")

                    // 第一次立即收取
                    collectSelfEnergyImmediately("第一次收取")

                    // 等待1秒后第二次收取
                    GlobalThreadPools.sleepCompat(1000L)
                    collectSelfEnergyImmediately("第二次收取")

                    Log.record(TAG, "✅ 加速卡自收能量完成（尝试两次）")
                }
            } else {
                Log.record(TAG, "背包中无可用加速卡")
            }
        } catch (th: Throwable) {
            Log.runtime(TAG, "useBubbleBoostCard err")
            Log.printStackTrace(TAG, th)
        }
    }

    /**
     * 立即收取自己能量（专用方法）
     */
    private fun collectSelfEnergyImmediately(tag: String = "立即收取") {
        try {
            val selfHomeObj = querySelfHome()
            if (selfHomeObj != null) {
                Log.record(TAG, "🎯 $tag：开始收取自己能量...")

                // 使用快速收取模式，跳过道具检查
                val availableBubbles: MutableList<Long> = ArrayList()
                val serverTime = selfHomeObj.optLong("now", System.currentTimeMillis())
                extractBubbleInfo(selfHomeObj, serverTime, availableBubbles, UserMap.currentUid)

                if (availableBubbles.isNotEmpty()) {
                    Log.record(TAG, "🎯 $tag：找到${availableBubbles.size}个可收能量球")
                    collectVivaEnergy(UserMap.currentUid, selfHomeObj, availableBubbles, "加速卡$tag", skipPropCheck = true)
                } else {
                    Log.record(TAG, "🎯 $tag：无可收能量球")
                }
            } else {
                Log.error(TAG, "❌ $tag：获取自己主页信息失败")
            }
        } catch (e: Exception) {
            Log.printStackTrace(TAG, "$tag 能量异常", e)
        }
    }
    /**
     * 使用1.1倍能量卡道具
     * 功能：增加能量收取倍数，收取好友能量时获得1.1倍效果
     */
    private fun userobExpandCard(bag: JSONObject? = queryPropList()) {
        try {
            var jo = findPropBag(bag, "VITALITY_ROB_EXPAND_CARD_1.1_3DAYS")
            if (jo != null && usePropBag(jo)) {
                robExpandCardEndTime = System.currentTimeMillis() + 1000 * 60 * 5
            }
            jo = findPropBag(bag, "SHAMO_ROB_EXPAND_CARD_1.5_1DAYS")
            if (jo != null && usePropBag(jo)) {
                robExpandCardEndTime = System.currentTimeMillis() + 1000 * 60 * 5
            }
        } catch (th: Throwable) {
            Log.runtime(TAG, "useBubbleBoostCard err")
            Log.printStackTrace(TAG, th)
        }
    }

    private fun useEnergyRainChanceCard() {
        try {
            if (Status.hasFlagToday("AntForest::useEnergyRainChanceCard")) {
                return
            }
            // 背包查找 限时能量雨机会
            var jo = findPropBag(queryPropList(), "LIMIT_TIME_ENERGY_RAIN_CHANCE")
            // 活力值商店兑换
            if (jo == null) {
                val skuInfo = Vitality.findSkuInfoBySkuName("能量雨次卡") ?: return
                val skuId = skuInfo.getString("skuId")
                if (Status.canVitalityExchangeToday(
                        skuId,
                        1
                    ) && Vitality.VitalityExchange(
                        skuInfo.getString("spuId"),
                        skuId,
                        "限时能量雨机会"
                    )
                ) {
                    jo = findPropBag(queryPropList(), "LIMIT_TIME_ENERGY_RAIN_CHANCE")
                }
            }
            // 使用 道具
            if (jo != null && usePropBag(jo)) {
                Status.setFlagToday("AntForest::useEnergyRainChanceCard")
                GlobalThreadPools.sleepCompat(500)
                EnergyRainCoroutine.execEnergyRainCompat()
            }
        } catch (th: Throwable) {
            Log.runtime(TAG, "useEnergyRainChanceCard err")
            Log.printStackTrace(TAG, th)
        }
    }

    /**
     * 使用炸弹卡道具
     * 功能：对有保护罩的好友使用，可以破坏其保护罩并收取能量
     * 注意：与保护罩功能冲突，通常二选一使用
     *
     * @param bagObject 背包的JSON对象
     */
    private fun useEnergyBombCard(bagObject: JSONObject?) {
        try {
            Log.runtime(TAG, "尝试使用炸弹卡...")
            var jo = findPropBag(bagObject, "ENERGY_BOMB_CARD")
            if (jo == null) {
                Log.runtime(TAG, "背包中没有炸弹卡，尝试兑换...")
                val skuInfo = Vitality.findSkuInfoBySkuName("能量炸弹卡")
                if (skuInfo == null) {
                    Log.runtime(TAG, "活力值商店中未找到炸弹卡。")
                    return
                }

                val skuId = skuInfo.getString("skuId")
                if (Status.canVitalityExchangeToday(skuId, 1)) {
                    if (Vitality.VitalityExchange(
                            skuInfo.getString("spuId"),
                            skuId,
                            "能量炸弹卡"
                        )
                    ) {
                        jo = findPropBag(queryPropList(), "ENERGY_BOMB_CARD")
                    }
                } else {
                    Log.runtime(TAG, "今日炸弹卡兑换次数已达上限。")
                }
            }

            if (jo != null) {
                Log.runtime(TAG, "找到炸弹卡，准备使用: $jo")
                if (usePropBag(jo)) {
                    // 使用成功后刷新真实结束时间
                    updateSelfHomePage()
                    Log.runtime(TAG, "能量炸弹卡使用成功，已刷新结束时间")
                }
            } else {
                Log.runtime(TAG, "背包中未找到任何可用炸弹卡。")
                updateSelfHomePage()
            }
        } catch (th: Throwable) {
            Log.error(TAG + "useEnergyBombCard err")
            Log.printStackTrace(th)
        }
    }

    /**
     * 收取状态的枚举类型
     */
    enum class CollectStatus {
        AVAILABLE, WAITING, INSUFFICIENT, ROBBED
    }

    /**
     * 统一获取和缓存用户名的方法
     * @param userId 用户ID
     * @param userHomeObj 用户主页对象（可选）
     * @param fromTag 来源标记（可选）
     * @return 用户名
     */
    private fun getAndCacheUserName(
        userId: String?,
        userHomeObj: JSONObject?,
        fromTag: String?
    ): String? {
        // 输入验证：userId为空时直接返回
        if (userId.isNullOrEmpty()) {
            return null
        }

        // 1. 尝试从缓存获取
        val cachedUserName = userNameCache.get(userId)
        if (!cachedUserName.isNullOrEmpty() && cachedUserName != userId) {
            // 如果缓存的不是userId本身，且不为空，则返回缓存值
            return cachedUserName
        }

        // 2. 根据上下文解析用户名
        var userName = resolveUserNameFromContext(userId, userHomeObj, fromTag)
        
        // 3. Fallback处理：如果解析失败，使用userId作为显示名
        if (userName.isNullOrEmpty()) {
            userName = userId
        }

        // 4. 存入缓存（只缓存有效的用户名）
        if (userName.isNotEmpty()) {
            userNameCache[userId] = userName
        }
        
        return userName
    }

    /**
     * 统一获取用户名的简化方法（无上下文）
     */
    private fun getAndCacheUserName(userId: String?): String? {
        return getAndCacheUserName(userId, null, null)
    }

    /**
     * 通用错误处理器
     * @param operation 操作名称
     * @param throwable 异常对象
     */
    private fun handleException(operation: String?, throwable: Throwable) {
        if (throwable is JSONException) {
            // JSON解析错误通常是网络响应问题，只记录错误信息不打印堆栈，避免刷屏
            Log.error(TAG, operation + " JSON解析错误: " + throwable.message)
        } else {
            Log.error(TAG, operation + " 错误: " + throwable.message)
            Log.printStackTrace(TAG, throwable)
        }
    }

    /**
     * 道具使用配置类
     */
    @JvmRecord
    private data class PropConfig(
        val propName: String?, val propTypes: Array<String>?,
        val condition: Supplier<Boolean?>?,
        val exchangeFunction: Supplier<Boolean?>?,
        val endTimeUpdater: Consumer<Long?>?
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as PropConfig
            if (propName != other.propName) return false
            if (!propTypes.contentEquals(other.propTypes)) return false
            if (condition != other.condition) return false
            if (exchangeFunction != other.exchangeFunction) return false
            if (endTimeUpdater != other.endTimeUpdater) return false
            return true
        }

        override fun hashCode(): Int {
            var result = propName?.hashCode() ?: 0
            result = 31 * result + (propTypes?.contentHashCode() ?: 0)
            result = 31 * result + (condition?.hashCode() ?: 0)
            result = 31 * result + (exchangeFunction?.hashCode() ?: 0)
            result = 31 * result + (endTimeUpdater?.hashCode() ?: 0)
            return result
        }
    }

    /**
     * 通用道具使用模板方法
     *
     * @param bagObject    背包对象
     * @param config       道具配置
     * @param constantMode 是否开启永动机模式
     */
    private fun usePropTemplate(bagObject: JSONObject?, config: PropConfig, constantMode: Boolean) {
        try {
            if (config.condition != null && !config.condition.get()!!) {
                Log.record(TAG, "不满足使用" + config.propName + "的条件")
                return
            }
            Log.runtime(TAG, "尝试使用" + config.propName + "...")
            // 按优先级查找道具
            var propObj: JSONObject? = null
            for (propType in config.propTypes!!) {
                propObj = findPropBag(bagObject, propType)
                if (propObj != null) break
            }
            // 如果背包中没有道具且开启永动机，尝试兑换
            if (propObj == null && constantMode && config.exchangeFunction != null) {
                Log.runtime(TAG, "背包中没有" + config.propName + "，尝试兑换...")
                if (config.exchangeFunction.get() == true) {
                    // 重新查找兑换后的道具
                    for (propType in config.propTypes) {
                        propObj = findPropBag(queryPropList(), propType)
                        if (propObj != null) break
                    }
                }
            }
            if (propObj != null) {
                // 针对限时双击卡的时间检查
                if ("双击卡" == config.propName) {
                    val propType = propObj.optString("propType")
                    if ("ENERGY_DOUBLE_CLICK" == propType && !hasDoubleCardTime()) {
                        Log.record(TAG, "跳过双击卡[$propType]，当前不在指定使用时间段内")
                        return
                    }
                }
                Log.runtime(TAG, "找到" + config.propName + "，准备使用: " + propObj)
                if (usePropBag(propObj)) {
                    config.endTimeUpdater?.accept(System.currentTimeMillis())
                }
            } else {
                Log.runtime(TAG, "背包中未找到任何可用的" + config.propName)
                updateSelfHomePage()
            }
        } catch (th: Throwable) {
            handleException("use" + config.propName, th)
        }
    }

    /**
     * 从上下文中解析用户名
     */
    private fun resolveUserNameFromContext(
        userId: String?,
        userHomeObj: JSONObject?,
        fromTag: String?
    ): String? {
        var userName: String? = null

        if ("pk" == fromTag && userHomeObj != null) {
            val userEnergy = userHomeObj.optJSONObject("userEnergy")
            if (userEnergy != null) {
                userName = "PK榜好友|" + userEnergy.optString("displayName")
            }
        } else {
            userName = UserMap.getMaskName(userId)
            if ((userName == null || userName == userId) && userHomeObj != null) {
                val userEnergy = userHomeObj.optJSONObject("userEnergy")
                if (userEnergy != null) {
                    val displayName = userEnergy.optString("displayName")
                    if (!displayName.isEmpty()) {
                        userName = displayName
                    }
                }
            }
        }
        return userName
    }

    companion object {
        val TAG: String = AntForest::class.java.getSimpleName()


        private val offsetTimeMath = Average(5)

        // 保持向后兼容
        /** 保护罩续写阈值（HHmm），例如 2355 表示 23小时55分  */
        private const val SHIELD_RENEW_THRESHOLD_HHMM = 2359
        var giveEnergyRainList: SelectModelField? = null //能量雨赠送列表
        var medicalHealthOption: SelectModelField? = null //医疗健康选项
        var ecoLifeOption: SelectModelField? = null

        /**
         * 异常返回检测开关
         */
        private var errorWait = false
        var ecoLifeOpen: BooleanModelField? = null
        private var canConsumeAnimalProp = false
        private var totalCollected = 0
        private const val TOTAL_HELP_COLLECTED = 0
        private const val TOTAL_WATERED = 0
        private const val MAX_BATCH_SIZE = 6

        // 找能量功能的冷却时间（毫秒），15分钟
        private const val TAKE_LOOK_COOLDOWN_MS = 15 * 60 * 1000L

        /**
         * 下次可以执行找能量的时间戳
         * 使用 @Volatile 确保多线程环境下的可见性
         */
        @Volatile
        private var nextTakeLookTime: Long = 0

        private fun propEmoji(propName: String): String {
            val tag: String = if (propName.contains("保")) {
                "🛡️"
            } else if (propName.contains("双")) {
                "👥"
            } else if (propName.contains("加")) {
                "🌪"
            } else if (propName.contains("雨")) {
                "🌧️"
            } else if (propName.contains("炸")) {
                "💥"
            } else {
                "🥳"
            }
            return tag
        }
    }

    /**
     * 检查用户是否有保护罩或炸弹（按照原有逻辑）
     */
    private fun checkUserShieldAndBomb(userHomeObj: JSONObject, userName: String?, userId: String, serverTime: Long): Boolean {
        var hasProtection = false
        val isSelf = userId == UserMap.currentUid

        if (!isSelf) {
            val shieldEndTime = ForestUtil.getShieldEndTime(userHomeObj)
            val bombEndTime = ForestUtil.getBombCardEndTime(userHomeObj)
            maxOf(shieldEndTime, bombEndTime)

            if (shieldEndTime > serverTime) {
                hasProtection = true
                val remainingHours = (shieldEndTime - serverTime) / (1000 * 60 * 60)
                Log.record(TAG, "[$userName]被能量罩❤️保护着哟(还剩${remainingHours}h)，跳过收取")
            }
            if (bombEndTime > serverTime) {
                hasProtection = true
                val remainingHours = (bombEndTime - serverTime) / (1000 * 60 * 60)
                Log.record(TAG, "[$userName]开着炸弹卡💣(还剩${remainingHours}h)，跳过收取")
            }
        }

        return hasProtection
    }

    /**
     * 专门用于蹲点的能量收取方法
     */
    @SuppressLint("SimpleDateFormat")
    private fun collectEnergyForWaiting(
        userId: String,
        userHomeObj: JSONObject,
        fromTag: String?,
        userName: String?
    ): CollectResult {
        try {
            Log.debug(TAG, "蹲点收取开始：用户[${userName}] userId[${userId}] fromTag[${fromTag}]")
            // 获取服务器时间
            val serverTime = userHomeObj.optLong("now", System.currentTimeMillis())
            // 判断是否是自己的账号
            val isSelf = userId == UserMap.currentUid
            
            // 先检查保护罩和炸弹（仅对好友检查）
            val shieldEndTime = ForestUtil.getShieldEndTime(userHomeObj)
            val bombEndTime = ForestUtil.getBombCardEndTime(userHomeObj)
            val hasShield = shieldEndTime > serverTime
            val hasBomb = bombEndTime > serverTime
            val hasProtection = hasShield || hasBomb

            Log.debug(TAG, "蹲点收取保护检查详情：")
            Log.debug(TAG, "  是否是主号: $isSelf")
            Log.debug(TAG, "  服务器时间: $serverTime (${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(
                Date(serverTime)
            )})")
            Log.debug(TAG, "  保护罩结束时间: $shieldEndTime (${if (shieldEndTime > 0) SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(
                Date(shieldEndTime)
            ) else "无保护罩"})")
            Log.debug(TAG, "  炸弹卡结束时间: $bombEndTime (${if (bombEndTime > 0) SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(
                Date(bombEndTime)
            ) else "无炸弹卡"})")
            Log.debug(TAG, "  是否有保护罩: $hasShield")
            Log.debug(TAG, "  是否有炸弹卡: $hasBomb")
            Log.debug(TAG, "  总体保护状态: $hasProtection")

            // 只对好友账号进行保护检查，主号无视保护罩
            if (!isSelf && hasProtection) {
                // 调用原有的日志输出方法
                checkUserShieldAndBomb(userHomeObj, userName, userId, serverTime)
                return CollectResult(
                    success = false,
                    userName = userName,
                    message = "有保护，已跳过",
                    hasShield = hasShield,
                    hasBomb = hasBomb
                )
            }
            
            // 主号的保护罩不影响收取自己的能量
            if (isSelf && hasProtection) {
                Log.debug(TAG, "  ⭐ 主号有保护罩，但可以收取自己的能量")
            }

            // 先查询用户能量状态
            val queryResult = collectEnergy(userId, userHomeObj, fromTag) ?: return CollectResult(
                success = false,
                userName = userName,
                message = "无法查询用户能量信息"
            )

            // Log.debug(TAG, "蹲点收取查询结果: $queryResult")

            // 提取可收取的能量球ID
            val availableBubbles: MutableList<Long> = ArrayList()
            val queryServerTime = queryResult.optLong("now", System.currentTimeMillis())
            extractBubbleInfo(queryResult, queryServerTime, availableBubbles, userId)

            if (availableBubbles.isEmpty()) {
                return CollectResult(
                    success = false,
                    userName = userName,
                    message = "用户无可收取的能量球"
                )
            }

            Log.debug(TAG, "蹲点收取找到${availableBubbles.size}个可收取能量球: $availableBubbles")

            // 记录收取前的总能量
            val beforeTotal = totalCollected

            // 🚀 启用快速收取通道：跳过道具检查，加速蹲点收取
            collectVivaEnergy(userId, queryResult, availableBubbles, fromTag, skipPropCheck = true)

            // 计算收取的能量数量
            val collectedEnergy = totalCollected - beforeTotal

            return if (collectedEnergy > 0) {
                CollectResult(
                    success = true,
                    userName = userName,
                    energyCount = collectedEnergy,
                    totalCollected = totalCollected,
                    message = "收取成功，共收取${availableBubbles.size}个能量球，${collectedEnergy}g能量"
                )
            } else {
                CollectResult(
                    success = false,
                    userName = userName,
                    message = "未收取到任何能量"
                )
            }
        } catch (e: Exception) {
            Log.printStackTrace(TAG, "蹲点能量收取异常", e)
            return CollectResult(
                success = false,
                userName = userName,
                message = "收取异常：${e.message}"
            )
        }
    }

    /**
     * 实现EnergyCollectCallback接口
     * 为蹲点管理器提供能量收取功能（增强版）
     */
    override fun addToTotalCollected(energyCount: Int) {
        totalCollected += energyCount
    }

    override fun getWaitingCollectDelay(): Long {
        return 0L // 立即收取，无延迟
    }
    override suspend fun collectUserEnergyForWaiting(task: EnergyWaitingManager.WaitingTask): CollectResult {
        return try {
            withContext(Dispatchers.Default) {
                // 查询好友主页
                val friendHomeObj = queryFriendHome(task.userId, task.fromTag)
                if (friendHomeObj != null) {
                    // 获取真实用户名
                    val realUserName = getAndCacheUserName(task.userId, friendHomeObj, task.fromTag)
                    val isSelf = task.userId == UserMap.currentUid
                    Log.debug(TAG, "蹲点收取：用户[${realUserName}] userId=${task.userId} currentUid=${UserMap.currentUid} isSelf=${isSelf}")
                    // 直接执行能量收取，让原有的collectEnergy方法处理保护罩和炸弹检查
                    val result = collectEnergyForWaiting(task.userId, friendHomeObj, task.fromTag, realUserName)
                    result.copy(userName = realUserName)
                } else {
                    CollectResult(
                        success = false,
                        userName = task.userName,
                        message = "无法获取用户主页信息"
                    )
                }
            }
        } catch (e: CancellationException) {
            // 协程取消是正常现象，不记录为错误
            Log.debug(TAG, "collectUserEnergyForWaiting 协程被取消")
            throw e  // 必须重新抛出以保证取消机制正常工作
        } catch (e: Exception) {
            Log.printStackTrace(TAG, "蹲点收取异常", e)
            CollectResult(
                success = false,
                userName = task.userName,
                message = "异常：${e.message}"
            )
        }
    }
}
