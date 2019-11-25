import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.3.50"
    id("com.github.johnrengelman.shadow") version "5.1.0"
    maven
    java
    `maven-publish`
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_13
    targetCompatibility = JavaVersion.VERSION_13
}

dependencies {
    implementation(project(":mediaserver"))
    implementation(kotlin("stdlib"))
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:2.10.0")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.10.0")
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
