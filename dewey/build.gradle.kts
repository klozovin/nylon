import org.gradle.launcher.daemon.protocol.Build
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    java
    kotlin("jvm") version "2.0.21"
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.github.jwharm.javagi", "gtk", "0.10.2")
}

sourceSets.main {
    kotlin.srcDir("src")
}

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
//        "-Djava.library.path=/usr/lib",
//        "-Dforeign.restricted=permit",
}


