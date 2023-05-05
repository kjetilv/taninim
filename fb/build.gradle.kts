plugins {
    java
}

configure<JavaPluginExtension> {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation("com.github.kjetilv.uplift:uplift-flogs:0.1.0-SNAPSHOT")
    implementation("com.github.kjetilv.uplift:uplift-kernel:0.1.0-SNAPSHOT")

    implementation("com.restfb:restfb:2023.3.0") {
        exclude(group = "org.slf4j")
    }

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.2")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}
