plugins {
    `maven-publish`
}

dependencies {
    implementation("com.github.kjetilv.uplift:uplift-hash:0.1.1-SNAPSHOT")
    implementation("com.github.kjetilv.uplift:uplift-kernel:0.1.1-SNAPSHOT")
    implementation("com.github.kjetilv.uplift:uplift-flogs:0.1.1-SNAPSHOT")
    implementation("com.github.kjetilv.uplift:uplift-s3:0.1.1-SNAPSHOT")
    implementation("com.github.kjetilv.uplift:uplift-synchttp:0.1.1-SNAPSHOT")
    implementation("com.github.kjetilv.uplift:uplift-json:0.1.1-SNAPSHOT")
    implementation("com.github.kjetilv.uplift:uplift-json-anno:0.1.1-SNAPSHOT")
    implementation("com.github.kjetilv.uplift:uplift-json-gen:0.1.1-SNAPSHOT")
    implementation("com.github.kjetilv.uplift:uplift-util:0.1.1-SNAPSHOT")

    implementation(project(":taninim"))
    implementation(project(":yellin"))
    implementation(project(":fb"))

    testImplementation("com.github.kjetilv.uplift:uplift-asynchttp:0.1.1-SNAPSHOT")
}
