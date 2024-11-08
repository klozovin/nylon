import org.gradle.launcher.daemon.protocol.Build
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    java
    kotlin("jvm") version "2.0.21"
    id("org.jetbrains.kotlinx.benchmark") version "0.4.12"
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin", "kotlin-reflect", "2.0.21")
    implementation("org.jetbrains.kotlinx", "kotlinx-benchmark-runtime", "0.4.12")
    implementation("io.github.jwharm.javagi", "gtk", "0.11.0")
}

sourceSets.main {
    kotlin.srcDir("src")
}

benchmark.targets.register("main")

kotlin.compilerOptions {
    languageVersion = KotlinVersion.KOTLIN_2_0
    apiVersion = KotlinVersion.KOTLIN_2_0
    jvmTarget = JvmTarget.JVM_22
}

application {
    mainClass = "dewey.AppKt"
    applicationDefaultJvmArgs += listOf(
        "--enable-preview",
        "--enable-native-access=ALL-UNNAMED",
    )
}


