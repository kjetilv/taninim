import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.github.johnrengelman.shadow") version "5.1.0"
    java
    `maven-publish`
    kotlin("jvm") version "1.3.70-eap-42"
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_13
    targetCompatibility = JavaVersion.VERSION_13
}

dependencies {
    implementation(platform("software.amazon.awssdk:bom:2.10.41"))
    implementation("software.amazon.awssdk:s3")

    implementation("org.gagravarr", "vorbis-java-core", "0.8")
    implementation("com.restfb", "restfb", "3.0.0")
    implementation("io.netty", "netty-all", "4.1.44.Final")
    implementation("ch.qos.logback:logback-classic:1.2.3")
    implementation("org.antlr:ST4:4.3")
    implementation("io.minio:minio:6.0.11")

    implementation("com.fasterxml.jackson.core:jackson-databind:2.10.0")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.10.0")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:2.10.0")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.10.0")

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
    jvmTarget = "1.8"
}
