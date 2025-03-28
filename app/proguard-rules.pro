# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\tools\adt-bundle-windows-x86_64-20131030\sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}


-keep class de.robv.android.xposed.** { *; }

# Keep Logback classes
-keep class ch.qos.logback.** { *; }
-keep class org.slf4j.** { *; }
-dontwarn ch.qos.logback.**
-dontwarn org.slf4j.**

# Keep your application classes
-keep class fansirsqi.xposed.sesame.** { *; }
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions

# Keep Jackson classes and annotations
-keep class com.fasterxml.jackson.** { *; }
-keepattributes Signature
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations
-keepattributes RuntimeVisibleTypeAnnotations
-keepattributes AnnotationDefault

# Keep Java Beans classes
-keep class java.beans.** { *; }
-dontwarn java.beans.**

# Keep Jackson Java7 Support
-keep class com.fasterxml.jackson.databind.ext.Java7SupportImpl { *; }
-dontwarn com.fasterxml.jackson.databind.ext.Java7SupportImpl

# Keep ModelField classes and their members
-keep class fansirsqi.xposed.sesame.model.ModelField { *; }
-keep class fansirsqi.xposed.sesame.model.modelFieldExt.** { *; }
-keepclassmembers class * extends fansirsqi.xposed.sesame.model.ModelField {
    <fields>;
    <methods>;
}

# Additional serialization rules
-keepclassmembers class * {
    @com.fasterxml.jackson.annotation.* *;
}
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
