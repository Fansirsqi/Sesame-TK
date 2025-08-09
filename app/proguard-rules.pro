# ---------- 框架 ----------
-keep class de.robv.android.xposed.** { *; }

# ---------- 日志 ----------
-keep class ch.qos.logback.** { *; }
-keep class org.slf4j.** { *; }
-dontwarn ch.qos.logback.**
-dontwarn org.slf4j.**

# ---------- 本工程 ----------
-keep class fansirsqi.xposed.sesame.** { *; }

# ---------- Jackson 必需 ----------
-keep class com.fasterxml.jackson.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
-keepclassmembers class * {
    @com.fasterxml.jackson.annotation.** *;
}

# ---------- 序列化 ----------
-keepnames class * implements java.io.Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ---------- 忽略 Android 缺失的 java.beans ----------
-dontwarn java.beans.ConstructorProperties
-dontwarn java.beans.Transient