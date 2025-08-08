pluginManagement {
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        mavenCentral()
        gradlePluginPortal()
        google()
    }
    plugins {
        kotlin("jvm") version "2.2.0-Beta2"  // 使用与libs.versions.toml中kotlin-plugin相同的版本
    }
}

dependencyResolutionManagement {
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        mavenCentral()
        google()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}
include(":app")
