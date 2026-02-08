dependencies {
    implementation("com.github.kjetilv.uplift:uplift-hash:0.1.1-SNAPSHOT")
    implementation("com.github.kjetilv.uplift:uplift-kernel:0.1.1-SNAPSHOT")
    implementation("com.github.kjetilv.uplift:uplift-flogs:0.1.1-SNAPSHOT")
    implementation("com.github.kjetilv.uplift:uplift-s3:0.1.1-SNAPSHOT")
    implementation("com.github.kjetilv.uplift:uplift-synchttp:0.1.1-SNAPSHOT")
    implementation("com.github.kjetilv.uplift:uplift-json:0.1.1-SNAPSHOT")
    implementation("com.github.kjetilv.uplift:uplift-json-gen:0.1.1-SNAPSHOT")
    implementation("com.github.kjetilv.uplift:uplift-lambda:0.1.1-SNAPSHOT")
    implementation("com.github.kjetilv.uplift:uplift-flambda:0.1.1-SNAPSHOT")
    implementation("com.github.kjetilv.uplift:uplift-util:0.1.1-SNAPSHOT")

    implementation(project(":fb"))
    implementation(project(":kudu"))
    implementation(project(":taninim"))
    implementation(project(":yellin"))
}
