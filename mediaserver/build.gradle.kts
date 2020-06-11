import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.github.johnrengelman.shadow") version "5.2.0"
    java
    `maven-publish`
    kotlin("jvm")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_14
    targetCompatibility = JavaVersion.VERSION_14
}

dependencies {
    implementation(platform("software.amazon.awssdk:bom:2.10.69"))
    implementation("software.amazon.awssdk:s3")

    implementation("org.gagravarr", "vorbis-java-core", "0.8")
    implementation("com.restfb", "restfb", "3.2.0")
    implementation("io.netty", "netty-all", "4.1.48.Final")
    implementation("ch.qos.logback:logback-classic:1.2.3")
    implementation("org.antlr:ST4:4.3")
    implementation("io.minio:minio:6.0.13")
    implementation("se.michaelthelin.spotify:spotify-web-api-java:4.2.1")
    implementation("org.jsoup:jsoup:1.13.1")
    implementation("org.codehaus.woodstox:stax2-api:4.2.1")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.10.2")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.10.2")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:2.10.2")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.10.2")

    testImplementation("junit:junit:4.12")
    implementation(kotlin("stdlib-jdk8"))
}

tasks.withType<ShadowJar>() {
    manifest {
        attributes(mapOf(Pair("Main-Class", "mediaserver.Main")))
    }
    mergeServiceFiles()
}

repositories {
    maven { setUrl("https://dl.bintray.com/kotlin/kotlin-eap") }
    mavenCentral()
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "1.8"
}

val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "11"
}
