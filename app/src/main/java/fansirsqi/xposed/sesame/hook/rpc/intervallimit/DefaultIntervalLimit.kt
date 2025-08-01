package fansirsqi.xposed.sesame.hook.rpc.intervallimit

class DefaultIntervalLimit(override val interval: Int?) : IntervalLimit {
    override var time: Long = 0
}