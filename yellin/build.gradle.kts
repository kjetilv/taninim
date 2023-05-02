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
    implementation("com.github.kjetilv.uplift:uplift-flogs:0.1.0-SNAPSHOT")
    implementation("com.github.kjetilv.uplift:uplift-s3:0.1.0-SNAPSHOT")
    implementation("com.github.kjetilv.uplift:uplift-asynchttp:0.1.0-SNAPSHOT")
    implementation("com.github.kjetilv.uplift:uplift-json:0.1.0-SNAPSHOT")
    implementation("com.github.kjetilv.uplift:uplift-lambda:0.1.0-SNAPSHOT")

    implementation(project(":fb"))
    implementation(project(":taninim"))

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.2")
    testImplementation("org.assertj:assertj-core:3.24.2")
}

configure<JavaPluginExtension> {
    sourceCompatibility = JavaVersion.VERSION_19
    targetCompatibility = JavaVersion.VERSION_19
}

apply<NativeLambdaPlugin>()

tasks.getByName<NativeLamdbdaTask>("native-lambda")
    .dependsOn("shadowJar")

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

tasks.withType<ShadowJar> {
    manifest {
        attributes(mapOf(Pair("Main-Class", "mediaserver.yellin.Main")))
    }
    mergeServiceFiles()
    minimize()
    dependsOn("build")
}
