package fansirsqi.xposed.sesame.util;

import fansirsqi.xposed.sesame.util.Log;

import java.time.Instant;
import java.time.Duration;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class TimeCounter {

    private final String name;
    private Instant start;
    private Instant lastCheckpoint;
    private boolean stopped = false;
    private int unexceptCnt = 0;
    private StringBuilder resultMsg = new StringBuilder();
    private Consumer<String> _logger;

    public TimeCounter(String name) {
        this.name = name;
        this.start = Instant.now();
        this.lastCheckpoint = this.start;
    }

    // 类似 C++ 析构的手动调用逻辑
    public void close() {
        if (stopped) {
            return;
        }
        if (unexceptCnt > 0) {
            stop();
        }
    }

    public void stop() {
        Instant end = Instant.now();
        long durationMs = Duration.between(start, end).toMillis();
        Log.record(name,String.format("========================\n%s 耗时: %d ms (%s)", 
                name, durationMs, resultMsg.toString()));
        stopped = true;
    }

    public void countDebug(String msg) {
        Instant now = Instant.now();
        long durationMs = Duration.between(lastCheckpoint, now).toMillis();
        Log.record(name,String.format("========================\n%s 耗时: %d ms", msg, durationMs));
        lastCheckpoint = now;
    }

    public void count(String msg) {
        Instant now = Instant.now();
        long durationMs = Duration.between(lastCheckpoint, now).toMillis();
        resultMsg.append(msg).append(":").append(durationMs).append(" ms, ");
        lastCheckpoint = now;
    }

    public void countUnexcept(String msg, long exceptMs) {
        Instant now = Instant.now();
        long durationMs = Duration.between(lastCheckpoint, now).toMillis();
        if (durationMs > exceptMs) {
            resultMsg.append(msg).append(":").append(durationMs)
                     .append(" ms(except:").append(exceptMs).append("ms), ");
            unexceptCnt++;
        }
        lastCheckpoint = now;
    }
}
