package fansirsqi.xposed.sesame.hook.rpc.intervallimit; // 声明了接口所在的包
// 定义了一个名为 IntervalLimit 的接口
public interface IntervalLimit {
    // 定义了一个名为 getInterval 的方法，该方法返回一个 Integer 类型的值
    Integer getInterval();
    // 定义了一个名为 getTime 的方法，该方法返回一个 Long 类型的值
    Long getTime();
    // 定义了一个名为 setTime 的方法，该方法接受一个 Long 类型的参数 time，没有返回值
    void setTime(Long time);
}
