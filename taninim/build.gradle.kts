plugins {
    java
}

dependencies {
    implementation("com.github.kjetilv.uplift:uplift-kernel:0.1.0-SNAPSHOT")
    implementation("com.github.kjetilv.uplift:uplift-flogs:0.1.0-SNAPSHOT")
    implementation("com.github.kjetilv.uplift:uplift-s3:0.1.0-SNAPSHOT")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.2")
    testImplementation("org.assertj:assertj-core:3.24.2")
}

tasks.test {
    useJUnitPlatform()
}
