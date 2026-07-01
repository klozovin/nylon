dependencies {
    implementation(project(":wlroots4j"))
    implementation("org.jspecify:jspecify:1.0.0")
}

application {
    mainClass = "TinyKt"
    applicationDefaultJvmArgs += listOf(
        "-ea",
    )
}