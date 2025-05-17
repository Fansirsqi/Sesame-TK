package fansirsqi.xposed.sesame.model;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import fansirsqi.xposed.sesame.task.AnswerAI.AnswerAI;
import fansirsqi.xposed.sesame.task.ancientTree.AncientTree;
import fansirsqi.xposed.sesame.task.antCooperate.AntCooperate;
import fansirsqi.xposed.sesame.task.antDodo.AntDodo;
import fansirsqi.xposed.sesame.task.antFarm.AntFarm;
import fansirsqi.xposed.sesame.task.antForest.AntForest;
import fansirsqi.xposed.sesame.task.antMember.AntMember;
import fansirsqi.xposed.sesame.task.antOcean.AntOcean;
import fansirsqi.xposed.sesame.task.antOrchard.AntOrchard;
import fansirsqi.xposed.sesame.task.antSports.AntSports;
import fansirsqi.xposed.sesame.task.antStall.AntStall;
import fansirsqi.xposed.sesame.task.exchange.NeverLandEX;
import fansirsqi.xposed.sesame.task.exchange.PrivilegeEX;
import fansirsqi.xposed.sesame.task.greenFinance.GreenFinance;
import fansirsqi.xposed.sesame.task.otherTask.OtherTask;
import fansirsqi.xposed.sesame.task.otherTask2.OtherTask2;
import fansirsqi.xposed.sesame.task.reserve.Reserve;
import fansirsqi.xposed.sesame.task.welfareCenter.WelfareCenter;
import lombok.Getter;
public class ModelOrder {
    @SuppressWarnings("unchecked")
    private static final Class<Model>[] array = new Class[]{
            BaseModel.class,//基础设置
            AntForest.class,//森林
            AntFarm.class,//庄园
            AntOrchard.class,//农场
            AntOcean.class,//海洋
            AntDodo.class,//神奇物种
            AncientTree.class,//古树
            AntCooperate.class,//合种
            Reserve.class,//保护地
            AntSports.class,//运动
            AntMember.class,//会员
            AntStall.class,//蚂蚁新村
            GreenFinance.class,//绿色经营
            WelfareCenter.class,//福利中心
            PrivilegeEX.class,//青春特权兑换
            NeverLandEX.class,//健康岛兑换
            OtherTask.class, //其他任务
            OtherTask2.class, //其他任务2
            AnswerAI.class,//AI答题
//            AntBookRead.class,//读书
//            ConsumeGold.class,//消费金
//            OmegakoiTown.class,//小镇

    };
    @Getter private  static final List<Class<? extends Model>> clazzList = new ArrayList<>();
    static {
        Collections.addAll(clazzList, array);
    }
}