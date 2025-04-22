pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        maven { url = uri("https://maven.aliyun.com/repository/public/") }
    }
    plugins {
        kotlin("jvm") version "2.2.0-Beta1"  // 使用与libs.versions.toml中kotlin-plugin相同的版本
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        maven { url = uri("https://maven.aliyun.com/repository/public/") }
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}
include(":app")
