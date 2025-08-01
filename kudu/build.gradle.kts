import com.github.kjetilv.uplift.plugins.NativeLambdaPlugin
import com.github.kjetilv.uplift.plugins.NativeLamdbdaTask
import java.net.URI

plugins {
    id("com.github.kjetilv.uplift.plugins.lambda") version "0.1.1-SNAPSHOT"
    `maven-publish`
}

dependencies {
    implementation("com.github.kjetilv.uplift:uplift-uuid:0.1.1-SNAPSHOT")
    implementation("com.github.kjetilv.uplift:uplift-kernel:0.1.1-SNAPSHOT")
    implementation("com.github.kjetilv.uplift:uplift-lambda:0.1.1-SNAPSHOT")
    implementation("com.github.kjetilv.uplift:uplift-flogs:0.1.1-SNAPSHOT")
    implementation("com.github.kjetilv.uplift:uplift-s3:0.1.1-SNAPSHOT")

    implementation(project(":taninim"))

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.0")
}

apply<NativeLambdaPlugin>()

tasks.withType<NativeLamdbdaTask> {
    main = "taninim.kudu.Main"
    javaDist = URI.create("https://github.com/graalvm/graalvm-ce-builds/releases/download/jdk-24.0.2/graalvm-community-jdk-24.0.2_linux-x64_bin.tar.gz")
    arch = "amd64"
    enablePreview = true
    addModules = "jdk.incubator.vector"
    otherOptions = "-H:+ForeignAPISupport -H:+VectorAPISupport"
}
