import org.jetbrains.kotlin.gradle.tasks.KotlinCompile


plugins {
    kotlin("jvm") version "1.4.21"
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

java {
    modularity.inferModulePath.set(true)
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
    jvmTarget = "14"
}

val compileTestKotlin: KotlinCompile by tasks

compileTestKotlin.kotlinOptions {
    jvmTarget = "14"
}
