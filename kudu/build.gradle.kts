import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.kjetilv.uplift.plugins.NativeLambdaPlugin
import com.github.kjetilv.uplift.plugins.NativeLamdbdaTask

plugins {
    java
    id("com.github.johnrengelman.shadow") version "8.1.0"
    id("com.github.kjetilv.uplift.plugins.native") version "0.1.0-SNAPSHOT"
    `maven-publish`
}

dependencies {
    implementation("com.github.kjetilv.uplift:uplift-kernel:0.1.0-SNAPSHOT")
    implementation("com.github.kjetilv.uplift:uplift-lambda:0.1.0-SNAPSHOT")
    implementation("com.github.kjetilv.uplift:uplift-flogs:0.1.0-SNAPSHOT")
    implementation("com.github.kjetilv.uplift:uplift-s3:0.1.0-SNAPSHOT")

    implementation(project(":taninim"))

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.2")
}

apply<NativeLambdaPlugin>()

tasks.getByName<NativeLamdbdaTask>("native-lambda")
    .dependsOn("shadowJar")

configure<JavaPluginExtension> {
    sourceCompatibility = JavaVersion.VERSION_19
    targetCompatibility = JavaVersion.VERSION_19
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

tasks.withType<ShadowJar> {
    manifest {
        attributes(mapOf(Pair("Main-Class", "mediaserver.kudu.Main")))
    }
    mergeServiceFiles()
    minimize()
    dependsOn("build")
}
