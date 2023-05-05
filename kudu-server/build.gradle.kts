import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("com.github.johnrengelman.shadow") version "8.1.1"
    `maven-publish`
}

apply { plugin("com.github.johnrengelman.shadow") }

dependencies {
    implementation("com.github.kjetilv.uplift:uplift-kernel:0.1.0-SNAPSHOT")
    implementation("com.github.kjetilv.uplift:uplift-flogs:0.1.0-SNAPSHOT")
    implementation("com.github.kjetilv.uplift:uplift-s3:0.1.0-SNAPSHOT")
    implementation("com.github.kjetilv.uplift:uplift-asynchttp:0.1.0-SNAPSHOT")

    implementation(project(":taninim"))
    implementation(project(":kudu"))

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.2")
}

tasks.withType<ShadowJar> {
    manifest {
        attributes(mapOf(Pair("Main-Class", "taninim.kudu.LambdaKudu")))
    }
    mergeServiceFiles()
    minimize()
    dependsOn("build")
}
