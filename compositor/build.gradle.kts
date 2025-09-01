import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    java
    application
    kotlin("jvm") version "2.2.10"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.sklogw.nylon", "wlroots4j", "0.1")
}

java {
    sourceCompatibility = JavaVersion.VERSION_24
    targetCompatibility = JavaVersion.VERSION_24
    toolchain.languageVersion = JavaLanguageVersion.of(24)

}

kotlin.compilerOptions {
    jvmTarget = JvmTarget.JVM_24
    apiVersion = KotlinVersion.KOTLIN_2_2
    languageVersion = KotlinVersion.KOTLIN_2_2
}

application {
    mainClass = "compositor.CompositorKt"
    applicationDefaultJvmArgs += listOf(
        "--enable-preview",
        "--enable-native-access=ALL-UNNAMED",
    )
}
