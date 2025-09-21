dependencies {
    implementation("com.github.kjetilv.uplift:uplift-uuid:0.1.1-SNAPSHOT")
    implementation("com.github.kjetilv.uplift:uplift-json:0.1.1-SNAPSHOT")
    implementation("com.github.kjetilv.uplift:uplift-json-anno:0.1.1-SNAPSHOT")
    implementation("com.github.kjetilv.uplift:uplift-json-gen:0.1.1-SNAPSHOT")
    implementation("com.github.kjetilv.uplift:uplift-flogs:0.1.1-SNAPSHOT")
    implementation("com.github.kjetilv.uplift:uplift-kernel:0.1.1-SNAPSHOT")
    implementation("com.restfb:restfb:2025.14.0")

    annotationProcessor("com.github.kjetilv.uplift:uplift-json-gen:0.1.1-SNAPSHOT")
}

tasks.compileJava {
    options.compilerArgs.addAll(
        listOf(
            "--add-reads", "taninim.fb=ALL-UNNAMED"
        )
    )
}

tasks.compileTestJava {
    options.compilerArgs.addAll(
        listOf(
            "--add-reads", "taninim.fb=ALL-UNNAMED"
        )
    )
}
