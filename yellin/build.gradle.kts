import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.kjetilv.uplift.plugins.NativeLambdaPlugin
import com.github.kjetilv.uplift.plugins.NativeLamdbdaTask

plugins {
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("com.github.kjetilv.uplift.plugins.native") version "0.1.1-SNAPSHOT"
    `maven-publish`
}

dependencies {
    implementation("com.github.kjetilv.uplift:uplift-kernel:0.1.1-SNAPSHOT")
    implementation("com.github.kjetilv.uplift:uplift-flogs:0.1.1-SNAPSHOT")
    implementation("com.github.kjetilv.uplift:uplift-s3:0.1.1-SNAPSHOT")
    implementation("com.github.kjetilv.uplift:uplift-asynchttp:0.1.1-SNAPSHOT")
    implementation("com.github.kjetilv.uplift:uplift-json:0.1.1-SNAPSHOT")
    implementation("com.github.kjetilv.uplift:uplift-lambda:0.1.1-SNAPSHOT")

    implementation(project(":fb"))
    implementation(project(":taninim"))

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.3")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.3")
    testImplementation("org.assertj:assertj-core:3.24.2")
}

apply<NativeLambdaPlugin>()

tasks.getByName<NativeLamdbdaTask>("native-lambda")
    .dependsOn("shadowJar")

tasks.withType<ShadowJar> {
    manifest {
        attributes(mapOf(Pair("Main-Class", "taninim.yellin.Main")))
    }
    mergeServiceFiles()
    minimize()
    dependsOn("build")
}
