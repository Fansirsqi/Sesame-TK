import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "fansirsqi.xposed.sesame"
    compileSdk = 36

    defaultConfig {
        vectorDrawables.useSupportLibrary = true
        applicationId = "fansirsqi.xposed.sesame"
        minSdk = 21
        targetSdk = 36
        
        if (!System.getenv("CI").toBoolean()) {
            ndk {
                abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64"))
            }
        }
        
        // 版本配置
        val major = 0
        val minor = 2
        val patch = 5
        val buildTag = "beta7"
        
        val buildDate = SimpleDateFormat("yy-MM-dd", Locale.CHINA).apply {
            timeZone = TimeZone.getTimeZone("GMT+8")
        }.format(Date())
        
        val buildTime = SimpleDateFormat("HH:mm:ss", Locale.CHINA).apply {
            timeZone = TimeZone.getTimeZone("GMT+8")
        }.format(Date())
        
        val buildTargetCode = try {
            MessageDigest.getInstance("MD5")
                .digest(buildTime.toByteArray())
                .joinToString("") { "%02x".format(it) }
                .substring(0, 4)
        } catch (_: Exception) {
            "0000"
        }

        val gitCommitCount = try {
            val process = Runtime.getRuntime().exec(arrayOf("git", "rev-list", "--count", "HEAD"))
            val output = process.inputStream.bufferedReader().use { it.readText() }.trim()
            process.waitFor()
            if (process.exitValue() == 0) {
                output
            } else {
                process.errorStream.bufferedReader().use { it.readText() }
                "0"
            }
        } catch (_: Exception) {
            "0"
        }


        versionCode = if (gitCommitCount.isEmpty()) 0 else gitCommitCount.toInt()
        versionName = if (buildTag.contains("alpha") || buildTag.contains("beta")) {
            "$major.$minor.$patch-$buildTag.$buildTargetCode"
        } else {
            "$major.$minor.$patch-$buildTag"
        }

        buildConfigField("String", "BUILD_DATE", "\"$buildDate\"")
        buildConfigField("String", "BUILD_TIME", "\"$buildTime\"")
        buildConfigField("String", "BUILD_NUMBER", "\"$buildTargetCode\"")
        buildConfigField("String", "BUILD_TAG", "\"$buildTag\"")

        ndk {
            abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64"))
        }

        testOptions {
            unitTests.all {
                it.enabled = false
            }
        }
    }

    buildFeatures {
        buildConfig = true
    }

    flavorDimensions += "default"
    productFlavors {
        create("normal") {
            dimension = "default"
            extra.set("applicationType", "Normal")
        }
        create("compatible") {
            dimension = "default"
            extra.set("applicationType", "Compatible")
        }
    }

    productFlavors.all {
        when (name) {
            "normal" -> {
                compileOptions {
                    sourceCompatibility = JavaVersion.VERSION_23
                    targetCompatibility = JavaVersion.VERSION_23
                }
                kotlinOptions {
                    jvmTarget = "23"
                }
            }
            "compatible" -> {
                compileOptions {
                    sourceCompatibility = JavaVersion.VERSION_17
                    targetCompatibility = JavaVersion.VERSION_17
                }
                kotlinOptions {
                    jvmTarget = "17"
                }
            }
        }
    }

    signingConfigs {
        getByName("debug") {
        }
    }

    buildTypes {
        getByName("debug") {
            isDebuggable = true
            versionNameSuffix = "-debug"
            isShrinkResources = false
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("debug")
        }
        getByName("release") {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    sourceSets {
        getByName("main") {
            jniLibs.srcDirs("src/main/jniLibs")
        }
    }

    if (!System.getenv("CI").toBoolean()) {
        externalNativeBuild {
            cmake {
                path = file("src/main/cpp/CMakeLists.txt")
                version = "3.31.6"
                ndkVersion = "29.0.13113456"
            }
        }
    }

    applicationVariants.all {
        val variant = this
        variant.outputs.all {
            val flavorName = variant.flavorName.replaceFirstChar { it.uppercase() }
            val fileName = "Sesame-$flavorName-${variant.versionName}.apk"
            (this as com.android.build.gradle.internal.api.BaseVariantOutputImpl).outputFileName = fileName
        }
    }
}

dependencies {
    implementation(libs.core.ktx)
    implementation(libs.kotlin.stdlib)
    implementation(libs.slf4j.api)
    implementation(libs.logback.android)
    implementation(libs.appcompat)
    implementation(libs.recyclerview)
    implementation(libs.viewpager2)
    implementation(libs.material)
    implementation(libs.webkit)
    compileOnly(libs.xposed.api)
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    implementation(libs.okhttp)

    add("normalImplementation", libs.jackson.core)
    add("normalImplementation", libs.jackson.databind)
    add("normalImplementation", libs.jackson.annotations)

    add("compatibleImplementation", libs.jackson.core.compatible)
    add("compatibleImplementation", libs.jackson.databind.compatible)
    add("compatibleImplementation", libs.jackson.annotations.compatible)
}
