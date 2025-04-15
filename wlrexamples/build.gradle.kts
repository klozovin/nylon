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
    implementation("org.jspecify", "jspecify", "1.0.0")
    implementation("io.github.jwharm.cairobindings", "cairo", "1.18.4.1")
    implementation(sourceSets.named("generated").get().output)
    testImplementation("io.kotest", "kotest-runner-junit5", "5.9.1")
    testImplementation("io.kotest", "kotest-assertions-core", "5.9.1")
}

tasks.withType<Test> {
    useJUnitPlatform()
    jvmArgs(
        "-ea",
        "--enable-native-access=ALL-UNNAMED"
    )
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
    mainClass.set("example.CairoBufferKt")
    applicationDefaultJvmArgs = listOf(
        "-ea",
        "--enable-native-access=ALL-UNNAMED",
//        "-Dforeign.restricted=permit",
    )
}