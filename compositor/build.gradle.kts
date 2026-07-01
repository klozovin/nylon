import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    java
    application
    kotlin("jvm") version "2.3.21"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.sklogw.nylon:wlroots4j:0.1")
    implementation("org.java-gi:gtk:0.15.0")
}

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

kotlin.compilerOptions {
    jvmTarget = JvmTarget.JVM_25
    apiVersion = KotlinVersion.KOTLIN_2_4
    languageVersion = KotlinVersion.KOTLIN_2_4
}

tasks.withType<JavaExec> {
    enableAssertions = true
}

application {
    mainClass = "compositor.CompositorKt"
    applicationDefaultJvmArgs += listOf(
        "-ea",
        "--enable-preview",
        "--enable-native-access=ALL-UNNAMED",
    )
}