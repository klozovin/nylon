dependencies {
    implementation(project(":wlroots4j"))
}

application {
    mainClass = "TinyKt"
    applicationDefaultJvmArgs += listOf(
        "-ea",
    )
}