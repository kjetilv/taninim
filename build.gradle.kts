import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.3.70-eap-42"
}

allprojects {
    group = "com.github.kjetilv.flacsefugl"
    version = "1.0-SNAPSHOT"

    repositories {
        mavenCentral()
        jcenter()
        google()
    }
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
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
