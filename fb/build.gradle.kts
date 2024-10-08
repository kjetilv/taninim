dependencies {
    implementation("com.github.kjetilv.uplift:uplift-uuid:0.1.1-SNAPSHOT")
    implementation("com.github.kjetilv.uplift:uplift-json:0.1.1-SNAPSHOT")
    implementation("com.github.kjetilv.uplift:uplift-flogs:0.1.1-SNAPSHOT")
    implementation("com.github.kjetilv.uplift:uplift-kernel:0.1.1-SNAPSHOT")
    annotationProcessor("com.github.kjetilv.uplift:uplift-json:0.1.1-SNAPSHOT")

    implementation("com.restfb:restfb:2024.9.0") {
        exclude(group = "org.slf4j")
    }

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.0")
}
