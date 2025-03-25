import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    application
    kotlin("jvm") version "2.1.20"
}

repositories {
    mavenCentral()
}

sourceSets.create("generated")

dependencies {
    implementation(sourceSets.named("generated").get().output)
}

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
    mainClass.set("wrap.SimpleKt")
    applicationDefaultJvmArgs = listOf(
        "-ea",
        "--enable-preview",
        "--enable-native-access", "ALL-UNNAMED",
        "--enable-native-access=ALL-UNNAMED",
        "-Dforeign.restricted=permit",
    )
}