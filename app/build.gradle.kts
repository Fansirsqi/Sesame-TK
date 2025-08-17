import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}
var isCIBuild: Boolean = System.getenv("CI").toBoolean()

//isCIBuild = true // 没有c++源码时开启CI构建, push前关闭

android {
    namespace = "fansirsqi.xposed.sesame"
    compileSdk = 36
    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }
    val gitCommitCount: Int = runCatching {
        val process = ProcessBuilder("git", "rev-list", "--count", "HEAD")
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().use { it.readText().trim() }
        output.toInt()
    }.getOrElse {
        println("获取 git 提交数失败: ${it.message}")
        1
    }
    defaultConfig {
        vectorDrawables.useSupportLibrary = true
        applicationId = "fansirsqi.xposed.sesame"
        minSdk = 23
        targetSdk = 36

        if (!isCIBuild) {
            ndk {
                abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64"))
            }
        }


        val buildDate = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).apply {
            timeZone = TimeZone.getTimeZone("GMT+8")
        }.format(Date())

        val buildTime = SimpleDateFormat("HH:mm:ss", Locale.CHINA).apply {
            timeZone = TimeZone.getTimeZone("GMT+8")
        }.format(Date())

        val buildTargetCode = try {
            buildDate.replace("-", ".") + "." + buildTime.replace(":", ".")
        } catch (_: Exception) {
            "0000"
        }

        versionCode = gitCommitCount
        val buildTag = "beta"
        versionName = "v0.2.7.rc$gitCommitCount-$buildTag"

        buildConfigField("String", "BUILD_DATE", "\"$buildDate\"")
        buildConfigField("String", "BUILD_TIME", "\"$buildTime\"")
        buildConfigField("String", "BUILD_NUMBER", "\"$buildTargetCode\"")
        buildConfigField("String", "BUILD_TAG", "\"$buildTag\"")
        buildConfigField("String", "VERSION", "\"$versionName\"")

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
        compose = true
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
    compileOptions {
        // 全局默认设置
        isCoreLibraryDesugaringEnabled = true // 启用脱糖
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        compilerOptions {
            jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
        }
    }

    productFlavors.all {
        when (name) {
            "normal" -> {
                compileOptions {
                    sourceCompatibility = JavaVersion.VERSION_17
                    targetCompatibility = JavaVersion.VERSION_17
                }
                kotlin {
                    compilerOptions {
                        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
                    }
                }
            }

            "compatible" -> {
                compileOptions {
                    sourceCompatibility = JavaVersion.VERSION_11
                    targetCompatibility = JavaVersion.VERSION_11
                }
                kotlin {
                    compilerOptions {
                        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11
                    }
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
    val cmakeFile = file("src/main/cpp/CMakeLists.txt")
    if (!isCIBuild && cmakeFile.exists()) {
        externalNativeBuild {
            cmake {
                path = cmakeFile
                version = "3.31.6"
                ndkVersion = "29.0.13113456"
            }
        }
    }

    applicationVariants.all {
        val variant = this
        variant.outputs.all {
            val flavorName = variant.flavorName.replaceFirstChar { it.uppercase() }
            val fileName = "Sesame-TK-$flavorName-${variant.versionName}.apk"
            (this as com.android.build.gradle.internal.api.BaseVariantOutputImpl).outputFileName = fileName
        }
    }
}

dependencies {
    implementation(libs.ui.tooling.preview.android)
    val composeBom = platform("androidx.compose:compose-bom:2025.05.00")
    implementation(composeBom)
    testImplementation(composeBom)
    androidTestImplementation(composeBom)
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.5")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.2")
    implementation("androidx.compose.runtime:runtime-livedata")
    implementation("org.nanohttpd:nanohttpd:2.3.1")


    implementation(libs.androidx.constraintlayout)

    implementation(libs.activity.compose)

    implementation(libs.core.ktx)
    implementation(libs.kotlin.stdlib)
    implementation(libs.slf4j.api)
    implementation(libs.logback.android)
    implementation(libs.appcompat)
    implementation(libs.recyclerview)
    implementation(libs.viewpager2)
    implementation(libs.material)
    implementation(libs.webkit)
//    compileOnly(libs.xposed.api)
//    compileOnly(libs.libxposed.api)
    compileOnly(files("libs/api-100.aar"))
    compileOnly(files("libs/api-82.jar"))
//    implementation(libs.libxposed.service)
//    implementation(files("libs/framework.jar"))

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    implementation(libs.okhttp)
    implementation(libs.dexkit)
    implementation(libs.jackson.kotlin)
    implementation("com.tencent:mmkv:2.2.2")

    coreLibraryDesugaring(libs.desugar)

    add("normalImplementation", libs.jackson.core)
    add("normalImplementation", libs.jackson.databind)
    add("normalImplementation", libs.jackson.annotations)

    add("compatibleImplementation", libs.jackson.core.compatible)
    add("compatibleImplementation", libs.jackson.databind.compatible)
    add("compatibleImplementation", libs.jackson.annotations.compatible)
}
