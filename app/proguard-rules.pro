# ---------- 框架 ----------
-keep class de.robv.android.xposed.** { *; }

# ---------- 日志 ----------
-keep class ch.qos.logback.** { *; }
-keep class org.slf4j.** { *; }
-dontwarn ch.qos.logback.**, org.slf4j.**

# ---------- 本工程 ----------
-keep class fansirsqi.xposed.sesame.** { *; }

# ---------- Jackson（最小必要） ----------
-keep class com.fasterxml.jackson.** { *; }
-keepattributes Signature, *Annotation*
-keepclassmembers class * {
    @com.fasterxml.jackson.annotation.** *;
}

# ---------- 序列化 & 缺失类 ----------
-keepnames class * implements java.io.Serializable
-keepclassmembers class * implements java.io.Serializable { *; }
-dontwarn java.beans.ConstructorProperties, java.beans.Transient