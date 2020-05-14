import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("com.github.johnrengelman.shadow") version "5.1.0"
    java
    `maven-publish`
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_14
    targetCompatibility = JavaVersion.VERSION_14
}

dependencies {
    implementation(project(":mediaserver"))
    implementation(kotlin("stdlib"))
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:2.10.0")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.10.0")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.10.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.10.0")
    testImplementation("junit:junit:4.12")
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "11"
}

val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "11"
}
