package fansirsqi.xposed.sesame.util;


import android.annotation.SuppressLint;
import android.os.Build;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.IntStream;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

@SuppressLint("SimpleDateFormat")
public final class SardineUtil {
    public static final String CUSTOM_NAMESPACE_PREFIX = "s";
    public static final String CUSTOM_NAMESPACE_URI = "SAR:";
    private static final List<ThreadLocal<SimpleDateFormat>> DATETIME_FORMATS;
    public static final String DEFAULT_NAMESPACE_PREFIX = "D";
    public static final String DEFAULT_NAMESPACE_URI = "DAV:";
    private static final String[] SUPPORTED_DATE_FORMATS;
    private static final DocumentBuilderFactory factory ;

    private SardineUtil() {
        throw new AssertionError("No SardineUtil instances allowed!");
    }

    static {
        // 初始化支持的日期格式数组
        SUPPORTED_DATE_FORMATS = new String[]{
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "EEE, dd MMM yyyy HH:mm:ss zzz",
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ssZ",
                "EEE MMM dd HH:mm:ss zzz yyyy",
                "EEEEEE, dd-MMM-yy HH:mm:ss zzz",
                "EEE MMMM d HH:mm:ss yyyy"
        };

        // 使用流式API和泛型安全地初始化日期格式化器列表
        List<ThreadLocal<SimpleDateFormat>> dateTimeFormatsList = new ArrayList<>(SUPPORTED_DATE_FORMATS.length);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                IntStream.range(0, SUPPORTED_DATE_FORMATS.length)
                        .forEach(i -> dateTimeFormatsList.add(ThreadLocal.withInitial(() -> new SimpleDateFormat(SUPPORTED_DATE_FORMATS[i]))));
            }
        }

        DATETIME_FORMATS = Collections.unmodifiableList(dateTimeFormatsList);

        // 配置安全的XML解析工厂
        factory = DocumentBuilderFactory.newInstance();
        try {
            factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        } catch (Exception e) {
            throw new ExceptionInInitializerError("Failed to configure XML parser: " + e.getMessage());
        }
    }


    public static Date parseDate(String str) {
        Date date = null;
        if (str == null) {
            return null;
        }
        int i = 0;
        while (true) {
            List<ThreadLocal<SimpleDateFormat>> list = DATETIME_FORMATS;
            if (i >= list.size()) {
                break;
            }
            ThreadLocal<SimpleDateFormat> threadLocal = list.get(i);
            SimpleDateFormat simpleDateFormat = threadLocal.get();
            if (simpleDateFormat == null) {
                simpleDateFormat = new SimpleDateFormat(SUPPORTED_DATE_FORMATS[i], Locale.US);
                simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                threadLocal.set(simpleDateFormat);
            }
            continue;
        }
        return date;
    }

    private static Document createDocument() {
        try {
            return factory.newDocumentBuilder().newDocument();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public static Map<QName, String> toQName(Map<String, String> map) {
        if (map == null) {
            return Collections.emptyMap();
        }
        HashMap<QName,String> hashMap = new HashMap<>(map.size());
        for (Map.Entry<String, String> entry : map.entrySet()) {
            hashMap.put(createQNameWithCustomNamespace(entry.getKey()), entry.getValue());
        }
        return hashMap;
    }

    public static List<QName> toQName(List<String> list) {
        if (list == null) {
            return Collections.emptyList();
        }
        ArrayList<QName> arrayList = new ArrayList<>(list.size());
        Iterator<String> it = list.iterator();
        while (it.hasNext()) {
            arrayList.add(createQNameWithCustomNamespace(it.next()));
        }
        return arrayList;
    }

    public static QName toQName(Element element) {
        if (element.getNamespaceURI() == null) {
            return new QName(DEFAULT_NAMESPACE_URI, element.getLocalName(), DEFAULT_NAMESPACE_PREFIX);
        }
        if (element.getPrefix() == null) {
            return new QName(element.getNamespaceURI(), element.getLocalName());
        }
        return new QName(element.getNamespaceURI(), element.getLocalName(), element.getPrefix());
    }

    public static QName createQNameWithCustomNamespace(String str) {
        return new QName(CUSTOM_NAMESPACE_URI, str, CUSTOM_NAMESPACE_PREFIX);
    }

    public static QName createQNameWithDefaultNamespace(String str) {
        return new QName(DEFAULT_NAMESPACE_URI, str, DEFAULT_NAMESPACE_PREFIX);
    }

    public static Element createElement(QName qName) {
        return createDocument().createElementNS(qName.getNamespaceURI(), qName.getPrefix() + ":" + qName.getLocalPart());
    }

    public static Element createElement(Element element, QName qName) {
        return element.getOwnerDocument().createElementNS(qName.getNamespaceURI(), qName.getPrefix() + ":" + qName.getLocalPart());
    }

    public static Charset standardUTF8() {
        return StandardCharsets.UTF_8;
    }
}
