import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    application
    kotlin("jvm") version "2.0.21"
}

repositories {
    mavenCentral()
}

sourceSets.main {
    java.srcDir("generated/main/java")
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

//tasks.withType<JavaCompile> {
//    options.compilerArgs.addAll(
//        listOf(
//            "--enable-preview"
//        )
//    )
//}

application {
    mainClass.set("helloworld.HelloWorldKt")
    applicationDefaultJvmArgs = listOf(
        "--enable-preview",
        "--enable-native-access", "ALL-UNNAMED",
        "--enable-native-access=ALL-UNNAMED",
        "-Dforeign.restricted=permit",
    )
}