import com.github.kjetilv.uplift.plugins.NativeLambdaPlugin
import com.github.kjetilv.uplift.plugins.NativeLamdbdaTask

plugins {
    id("com.github.kjetilv.uplift.plugins.lambda") version "0.1.1-SNAPSHOT"
    `maven-publish`
}

dependencies {
    implementation(project(":fb"))
    implementation(project(":taninim"))

    implementation("com.github.kjetilv.uplift:uplift-hash:0.1.1-SNAPSHOT")
    implementation("com.github.kjetilv.uplift:uplift-kernel:0.1.1-SNAPSHOT")
    implementation("com.github.kjetilv.uplift:uplift-flogs:0.1.1-SNAPSHOT")
    implementation("com.github.kjetilv.uplift:uplift-s3:0.1.1-SNAPSHOT")
    implementation("com.github.kjetilv.uplift:uplift-json:0.1.1-SNAPSHOT")
    implementation("com.github.kjetilv.uplift:uplift-json-gen:0.1.1-SNAPSHOT")
    implementation("com.github.kjetilv.uplift:uplift-json-anno:0.1.1-SNAPSHOT")
    implementation("com.github.kjetilv.uplift:uplift-json-mame:0.1.1-SNAPSHOT")
    implementation("com.github.kjetilv.uplift:uplift-lambda:0.1.1-SNAPSHOT")
    implementation("com.github.kjetilv.uplift:uplift-util:0.1.1-SNAPSHOT")

    annotationProcessor("com.github.kjetilv.uplift:uplift-json-gen:0.1.1-SNAPSHOT")
}

apply<NativeLambdaPlugin>()

tasks.withType<NativeLamdbdaTask> { main = "yellin" }
