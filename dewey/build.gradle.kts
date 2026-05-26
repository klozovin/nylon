import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    java
    application
    kotlin("jvm") version "2.3.21"
    id("org.jetbrains.kotlinx.benchmark") version "0.4.16"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin", "kotlin-reflect", "2.3.21")
    implementation("org.jetbrains.kotlinx", "kotlinx-benchmark-runtime", "0.4.16")
    implementation("org.java-gi", "gtk", "0.15.0")
    implementation("io.reactivex.rxjava3", "rxjava", "3.1.12")
}

sourceSets.main {
    java.srcDir("src")
    kotlin.srcDir("src")
}

benchmark.targets.register("main")

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

kotlin.compilerOptions {
    languageVersion = KotlinVersion.KOTLIN_2_3
    apiVersion = KotlinVersion.KOTLIN_2_3
    jvmTarget = JvmTarget.JVM_25
}

application {
    mainClass = "dewey.AppKt"
    applicationDefaultJvmArgs += listOf(
        "--enable-preview",
        "--enable-native-access=ALL-UNNAMED",
    )
}