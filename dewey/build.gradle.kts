import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    java
    application
    kotlin("jvm") version "2.1.20"
    id("org.jetbrains.kotlinx.benchmark") version "0.4.13"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin", "kotlin-reflect", "2.1.10")
    implementation("org.jetbrains.kotlinx", "kotlinx-benchmark-runtime", "0.4.13")
    implementation("io.github.jwharm.javagi", "gtk", "0.11.2")
    implementation("io.reactivex.rxjava3", "rxjava", "3.1.10")
}

sourceSets.main {
    java.srcDir("src")
    kotlin.srcDir("src")
}

benchmark.targets.register("main")

java {
    sourceCompatibility = JavaVersion.VERSION_22
    targetCompatibility = JavaVersion.VERSION_22
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
}