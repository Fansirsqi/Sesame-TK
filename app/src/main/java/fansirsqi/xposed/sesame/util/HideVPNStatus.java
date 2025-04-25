package fansirsqi.xposed.sesame.util;

import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;

import java.net.NetworkInterface;
import java.util.Random;
import java.util.regex.Pattern;


public class HideVPNStatus {
    private static final String LOG_PREFIX = "[HideVPNStatus] ";
    private static final String EMPTY_STRING = "";
    private static final String DEFAULT_PROXY_PORT = "-1";
    private static final Pattern VPN_INTERFACE = Pattern.compile("^(tun[0-9]+|ppp[0-9]+)");

    private static final String HTTP_PROXY_HOST = "http.proxyHost";
    private static final String HTTP_PROXY_PORT = "http.proxyPort";
    private static final String VPN = "VPN";
    private static final String WIFI = "WIFI";

    public static void proxy() {
    try {
        Log.record(LOG_PREFIX + "开始应用VPN隐藏功能");
        bypassSystemProxyCheck();
        Log.record(LOG_PREFIX + "系统代理检查已绕过");
        modifyNetworkInfo();
        Log.record(LOG_PREFIX + "网络信息已修改");
        modifyNetworkCapabilities();
        Log.record(LOG_PREFIX + "网络能力已修改");
        Log.record(LOG_PREFIX + "VPN隐藏功能应用完成");
    } catch (Throwable t) {
        Log.error(LOG_PREFIX + "应用VPN隐藏功能时出错");
        Log.printStackTrace(t);
    }
}

    private static String getRandomString(int length) {
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        StringBuilder sb = new StringBuilder(length);
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            sb.append(alphabet.charAt(random.nextInt(alphabet.length())));
        }
        return sb.toString();
    }

    private static void bypassSystemProxyCheck() {
        hookSystemProperty("getProperty", HTTP_PROXY_HOST, EMPTY_STRING);
        hookSystemProperty("getProperty", HTTP_PROXY_PORT, DEFAULT_PROXY_PORT);
        hookSystemProperty("setProperty", HTTP_PROXY_HOST, EMPTY_STRING);
        hookSystemProperty("setProperty", HTTP_PROXY_PORT, EMPTY_STRING);
    }

    private static void hookSystemProperty(String methodName, String propertyName, String value) {
        HookUtil.hookAllMethods(System.class, methodName, "before", param -> {
            String property = (String) param.args[0];
            if (property.equals(propertyName)) {
                param.setResult(value);
            }
        });
    }

    private static void modifyNetworkInfo() {
        hookNetworkInfoMethod("getType", ConnectivityManager.TYPE_VPN, ConnectivityManager.TYPE_WIFI);
        hookNetworkInfoMethod("getTypeName", VPN, WIFI);
        hookNetworkInfoMethod("getSubtypeName", VPN, WIFI);
        hookGetNetworkInfo();
    }

    private static void hookNetworkInfoMethod(String methodName, Object target, Object replacement) {
        HookUtil.hookAllMethods(NetworkInfo.class, methodName, "before", param -> {
            Object result = param.getResult();
            if (target.equals(result)) {
                param.setResult(replacement);
            }
        });
    }

    private static void hookGetNetworkInfo() {
        HookUtil.hookAllMethods(ConnectivityManager.class, "getNetworkInfo", "before", param -> {
            if (param.args.length > 0 && param.args[0] instanceof Integer && (int) param.args[0] == ConnectivityManager.TYPE_VPN) {
                param.setResult(null);
            }
        });
    }

    private static void modifyNetworkCapabilities() {
        hookNetworkCapabilitiesMethod("hasTransport", NetworkCapabilities.TRANSPORT_VPN, false);
        hookNetworkCapabilitiesMethod("hasCapability", NetworkCapabilities.NET_CAPABILITY_NOT_VPN, true);
        hookNetworkInterfaceMethods();
    }

    private static void hookNetworkCapabilitiesMethod(String methodName, Object arg, Object result) {
        HookUtil.hookAllMethods(NetworkCapabilities.class, methodName, "before", param -> {
            Object arg0 = param.args[0];
            if (arg.equals(arg0)) {
                param.setResult(result);
            }
        });
    }

    private static void hookNetworkInterfaceMethods() {
        HookUtil.hookAllMethods(NetworkInterface.class, "isVirtual", "before", param -> param.setResult(false));
        HookUtil.hookAllMethods(NetworkInterface.class, "isUp", "before", param -> {
            NetworkInterface networkInterface = (NetworkInterface) param.thisObject;
            if (networkInterface != null && VPN_INTERFACE.matcher(networkInterface.getName()).matches()) {
                param.setResult(false);
            }
        });

        HookUtil.hookAllMethods(NetworkInterface.class, "getName", "before", param -> {
            String originalName = (String) param.getResult();
            if (originalName != null && VPN_INTERFACE.matcher(originalName).matches()) {
                param.setResult(getRandomString(originalName.length()));
            }
        });
    }
}

