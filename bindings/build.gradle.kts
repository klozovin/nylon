import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

group = "com.sklogw.nylon"
version = "0.1"

plugins {
    kotlin("jvm") version "2.2.10"
    id("org.jetbrains.kotlinx.benchmark") version "0.4.14"
}

repositories {
    mavenCentral()
}

sourceSets.create("generated")

sourceSets.create("benchmark") {
    compileClasspath += sourceSets.main.get().output
    runtimeClasspath += sourceSets.main.get().output

//    implementation(sourceSets["generated"].output)

    dependencies {
        implementation("org.jetbrains.kotlinx", "kotlinx-benchmark-runtime", "0.4.14")
    }

    configurations["benchmarkImplementation"].extendsFrom(configurations.implementation.get())

}
benchmark.targets.register("benchmark")

dependencies {
    implementation(sourceSets["generated"].output)
    implementation("org.jspecify", "jspecify", "1.0.0")
    testImplementation("io.kotest", "kotest-runner-junit5", "5.9.1")
    testImplementation("io.kotest", "kotest-assertions-core", "5.9.1")
}

tasks {
    jar {
//        dependsOn(tasks.named("classes"))
//        dependsOn(tasks.name)
        from(sourceSets["generated"].output)
    }
    test {
        useJUnitPlatform()
        jvmArgs("-ea", "--enable-native-access=ALL-UNNAMED")
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_24
    targetCompatibility = JavaVersion.VERSION_24
    toolchain.languageVersion = JavaLanguageVersion.of(24)

    // TODO: Necessary?
    registerFeature("generated") {
        usingSourceSet(sourceSets["generated"])
    }
}

kotlin.compilerOptions {
    jvmTarget = JvmTarget.JVM_24
    apiVersion = KotlinVersion.KOTLIN_2_2
    languageVersion = KotlinVersion.KOTLIN_2_2
}