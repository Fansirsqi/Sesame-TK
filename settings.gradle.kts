pluginManagement {
    repositories {
        // —— 国内镜像优先 ——
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        // —— 官方源兜底 ——
        mavenCentral()
        google()
        gradlePluginPortal()
    }
    plugins {
        // 如无特别需求，建议用稳定版；Beta2 网络上更容易拉不到
        // kotlin("jvm") version "2.0.20"
        kotlin("jvm") version "2.2.0-Beta2"
    }
}

dependencyResolutionManagement {
    // 如果你确信根目录已配置好所有仓库，就保留 FAIL_ON_PROJECT_REPOS；
    // 若子模块还需要自定义仓库，可改为 PREFER_PROJECT
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        // —— 国内镜像优先 ——
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        // 之前的 spring 源一般用不到，可以删掉；留着也不影响
        // maven("https://maven.aliyun.com/repository/spring")

        // —— 官方源兜底 ——
        google()
        mavenCentral()

        // 仅从本地仓库解析 libxposed（保留你的定向，避免污染）
        mavenLocal {
            content { includeGroup("io.github.libxposed") }
        }
    }
    versionCatalogs {
        create("libs")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}

include(":app")
