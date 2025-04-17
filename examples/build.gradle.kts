import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    application
    kotlin("jvm") version "2.1.20"
}

repositories {
    mavenCentral()
}



dependencies {
    implementation("com.sklogw.nylon", "bindings", "0.1")
//    implementation(project("bindings"))
    implementation("io.github.jwharm.cairobindings", "cairo", "1.18.4.1")
}

java {
    sourceCompatibility = JavaVersion.VERSION_23
    targetCompatibility = JavaVersion.VERSION_23
    toolchain.languageVersion = JavaLanguageVersion.of(24)
}

kotlin.compilerOptions {
    jvmTarget = JvmTarget.JVM_23
    apiVersion = KotlinVersion.KOTLIN_2_1
    languageVersion = KotlinVersion.KOTLIN_2_1
}

application {
    mainClass.set("example.wrap.SimpleKt")
    applicationDefaultJvmArgs = listOf(
        "-ea",
        "--enable-native-access=ALL-UNNAMED",
    )
}
