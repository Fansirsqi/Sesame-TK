package fansirsqi.xposed.sesame.util;

import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicLong;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class NtpTimeUtils {

    // 阿里云 NTP 服务器 IP 地址
    private static final String[] NTP_SERVERS = {
            "203.107.6.88", "203.107.6.89", "203.107.6.90", "203.107.6.91",
            "203.107.6.92", "203.107.6.93", "203.107.6.94"
    };

    // 存储当前系统与 NTP 时间的差值（毫秒）
    private static final AtomicLong timeOffset = new AtomicLong(0);

    // 是否已成功同步过一次时间
    private static volatile boolean synced = false;

    // 全局 OkHttpClient 实例
    private static final OkHttpClient httpClient = new OkHttpClient();

    /**
     * 异步同步网络时间（优先使用 NTP，失败后使用 HTTP）
     */
    public static void syncTimeAsync() {
        new Thread(NtpTimeUtils::syncTime).start();
    }

    /**
     * 主动同步一次网络时间（先尝试 NTP，再尝试 HTTP）
     */
    public static void syncTime() {
        if (synced) return;

        for (String server : NTP_SERVERS) {
            try {
                InetAddress hostAddr = InetAddress.getByName(server);
                NTPUDPClient client = new NTPUDPClient();
                client.setDefaultTimeout(2000);
                client.open();

                TimeInfo info = client.getTime(hostAddr);
                info.computeDetails();

                long offset = info.getOffset(); // 得到客户端和服务端的时间差
                long delay = info.getDelay();   // 得到网络延迟

                if (offset != Long.MIN_VALUE && delay != Long.MIN_VALUE) {
                    long ntpTime = info.getMessage().getTransmitTimeStamp().getTime();
                    long localTime = System.currentTimeMillis();
                    long calculatedOffset = ntpTime - localTime;

                    timeOffset.set(calculatedOffset);
                    synced = true;

                    Log.other("NtpTimeUtils", "🌐 成功从 [" + server + "] 同步时间");
                    Log.other("NtpTimeUtils", "⏰ 本地时间戳：" + localTime);
                    Log.other("NtpTimeUtils", "📉 时间矫正完成！当前偏移量：" + calculatedOffset + " ms");

                    // 格式化 NTP 服务器传来的时间（ntpTime）和本地时间（localTime）
                    String serverTimeFormatted = String.format("%tT.%<tL", ntpTime);     // HH:mm:ss.SSS
                    String localTimeFormatted = String.format("%tT.%<tL", localTime);   // HH:mm:ss.SSS

                    // 输出格式：服务器时间[15:30:45.123]--本地时间[15:30:45.001]
                    Log.other("服务器[" + serverTimeFormatted + "]本地[" + localTimeFormatted + "]");

                    return;
                }
            } catch (IOException e) {
                Log.error("NtpTimeUtils", "❌ 无法连接到 NTP 服务器 [" + server + "]：" + e.getMessage());
            }
        }

        // 如果所有 NTP 都失败，尝试 HTTP 接口
        try {
            Request request = new Request.Builder()
                    .url("http://worldtimeapi.org/api/ip")
                    .build();

            Response response = httpClient.newCall(request).execute();
            if (response.isSuccessful() && response.body() != null) {
                JSONObject json = new JSONObject(response.body().string());
                long httpTime = json.getLong("unixtime") * 1000;
                long localTime = System.currentTimeMillis();
                long calculatedOffset = httpTime - localTime;

                timeOffset.set(calculatedOffset);
                synced = true;

                Log.other("NtpTimeUtils", "🌐 成功通过 HTTP 获取时间：" + httpTime);
                Log.other("NtpTimeUtils", "📉 时间偏差：" + calculatedOffset + " ms");
                // 格式化 NTP 服务器传来的时间（ntpTime）和本地时间（localTime）
                String serverTimeFormatted = String.format("%tT.%<tL", httpTime);     // HH:mm:ss.SSS
                String localTimeFormatted = String.format("%tT.%<tL", localTime);   // HH:mm:ss.SSS

                // 输出格式：服务器时间[15:30:45.123]--本地时间[15:30:45.001]
                Log.other("服务器[" + serverTimeFormatted + "]本地[" + localTimeFormatted + "]");
                return;
            }
        } catch (Exception e) {
            Log.error("NtpTimeUtils", "HTTP 时间同步失败：" + e.getMessage());
        }

        // 最终 fallback：继续使用本地时间
        Log.other("NtpTimeUtils", "⚠️ 所有 NTP 和 HTTP 时间同步失败，将继续使用本地时间");
    }
    private static void syncTimeWithHttp() {
        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder().url("http://worldtimeapi.org/api/ip").build();
                Response response = client.newCall(request).execute();

                if (response.isSuccessful() && response.body() != null) {
                    JSONObject json = new JSONObject(response.body().string());
                    long httpTime = json.getLong("unixtime") * 1000;
                    timeOffset.set(httpTime - System.currentTimeMillis());
                    synced = true;

                    Log.other("NtpTimeUtils", "🌐 成功通过 HTTP 获取时间：" + httpTime);
                } else {
                    Log.other("NtpTimeUtils", "⚠️ 所有 NTP 和 HTTP 时间同步失败");
                }
            } catch (Exception e) {
                Log.error("NtpTimeUtils", "HTTP 时间同步失败：" + e.getMessage());
            }
        }).start();
    }

    /**
     * 获取矫正后的时间戳（推荐代替 System.currentTimeMillis()）
     */
    public static long getCurrentTimeMillis() {
        if (!synced) {
            syncTime();
        }
        return System.currentTimeMillis() + timeOffset.get();
    }

    /**
     * 获取当前偏移量（ms）
     */
    public static long getTimeOffset() {
        if (!synced) {
            syncTime();
        }
        return timeOffset.get();
    }

    /**
     * 是否已同步成功
     */
    public static boolean isSynced() {
        return synced;
    }
}
