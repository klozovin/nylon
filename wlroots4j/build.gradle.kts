import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion


plugins {
    application
    kotlin("jvm") version "2.3.21"
}


allprojects {
    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "application")
    apply(plugin = "kotlin")

    java {
        sourceCompatibility = JavaVersion.VERSION_25
        targetCompatibility = JavaVersion.VERSION_25
        toolchain.languageVersion = JavaLanguageVersion.of(25)
    }

    kotlin.compilerOptions {
        jvmTarget = JvmTarget.JVM_25
        apiVersion = KotlinVersion.KOTLIN_2_3
        languageVersion = KotlinVersion.KOTLIN_2_3
    }
}