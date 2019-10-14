import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("com.github.johnrengelman.shadow") version "5.1.0"
    maven
    java
    `maven-publish`
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

dependencies {
    implementation("org.gagravarr", "vorbis-java-core", "0.8")
    implementation("io.netty", "netty-all", "4.1.41.Final")
    implementation("ch.qos.logback:logback-classic:1.2.3")
    implementation("org.antlr:ST4:4.1")
    implementation("io.minio:minio:6.0.11")

    testImplementation("junit:junit:4.12")
}

tasks.withType<ShadowJar>() {
    manifest {
        attributes(mapOf(Pair("Main-Class", "mediaserver.Main")))
    }
    mergeServiceFiles()
}
