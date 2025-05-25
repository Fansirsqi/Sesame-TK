plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
}

allprojects {
    tasks.withType<JavaCompile>().configureEach {
        options.compilerArgs.addAll(listOf("-Xlint:unchecked", "-Xlint:deprecation"))
    }
}

repositories {
    maven("https://maven.aliyun.com/repository/spring")
    mavenCentral()
    google()
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
