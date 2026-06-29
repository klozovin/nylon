group = "com.sklogw.nylon"
version = "0.1"

plugins {
    kotlin("plugin.allopen") version "2.2.0"
    id("org.jetbrains.kotlinx.benchmark") version "0.4.17"
}

sourceSets.create("benchmark") {
    compileClasspath += sourceSets.main.get().output
    runtimeClasspath += sourceSets.main.get().output

    dependencies {
        implementation("org.jetbrains.kotlinx:kotlinx-benchmark-runtime:0.4.17")
    }

    configurations["benchmarkImplementation"].extendsFrom(configurations.implementation.get())

}


benchmark {
	targets.register("benchmark")

	configurations {
		register("targeted") {
			include("InputDeviceType")
		}
	}
}


allOpen {
    annotation("org.openjdk.jmh.annotations.State")
}


dependencies {
    implementation(project(":jextracted"))
    implementation("org.jspecify:jspecify:1.0.0")
    testImplementation("io.kotest:kotest-runner-junit5:5.9.1")
    testImplementation("io.kotest:kotest-assertions-core:5.9.1")
}

tasks {
    test {
        useJUnitPlatform()
        jvmArgs("-ea", "--enable-native-access=ALL-UNNAMED")
    }
}