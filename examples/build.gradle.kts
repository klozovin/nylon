import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_2

plugins {
    application
    kotlin("jvm") version "2.1.20"
}

allprojects {
    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "application")
    apply(plugin = "kotlin")

    dependencies {
        implementation("com.sklogw.nylon", "bindings", "0.1")
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_23
        targetCompatibility = JavaVersion.VERSION_23
        toolchain.languageVersion = JavaLanguageVersion.of(24)
    }

    kotlin.compilerOptions {
        jvmTarget = JvmTarget.JVM_23
        apiVersion = KOTLIN_2_2
        languageVersion = KOTLIN_2_2
    }

    application {
        applicationDefaultJvmArgs = listOf("-ea", "--enable-native-access=ALL-UNNAMED")
    }
}