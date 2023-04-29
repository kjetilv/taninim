plugins {
    java
}

dependencies {
    implementation("com.github.kjetilv.uplift:uplift-kernel:0.1.0-SNAPSHOT")
    implementation("com.github.kjetilv.uplift:uplift-flogs:0.1.0-SNAPSHOT")
    implementation("com.github.kjetilv.uplift:uplift-s3:0.1.0-SNAPSHOT")
    implementation("com.github.kjetilv.uplift:uplift-asynchttp:0.1.0-SNAPSHOT")
    implementation("com.github.kjetilv.uplift:uplift-json:0.1.0-SNAPSHOT")
    implementation("com.github.kjetilv.uplift:uplift-lambda:0.1.0-SNAPSHOT")
    implementation("com.github.kjetilv.uplift:uplift-flambda:0.1.0-SNAPSHOT")

    implementation(project(":fb"))
    implementation(project(":kudu"))
    implementation(project(":taninim"))
    implementation(project(":yellin"))

    implementation("com.fasterxml.jackson.core:jackson-core:2.14.2")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.14.2")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.14.2")
    implementation("org.junit.jupiter:junit-jupiter-api:5.9.2")

    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.2")
    testImplementation("org.assertj:assertj-core:3.24.2")
}

tasks.test {
    useJUnitPlatform()
}
