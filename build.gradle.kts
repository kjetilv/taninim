plugins {
    java
    kotlin("jvm") version "1.8.0"
}

allprojects {
    group = "com.github.kjetilv.flacsefugl"
    version = "1.0-SNAPSHOT"

    repositories {
        maven { setUrl("https://dl.bintray.com/kotlin/kotlin-eap") }
        gradlePluginPortal()
        mavenLocal()
        mavenCentral()
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/kjetilv/uplift")
            credentials {
                username = resolveUsername()
                password = resolveToken()
            }
        }
    }
}

java {
    modularity.inferModulePath.set(/* value = */ true)
}

configure<JavaPluginExtension> {
    sourceCompatibility = JavaVersion.VERSION_19
    targetCompatibility = JavaVersion.VERSION_19
}

val compileKotlin: org.jetbrains.kotlin.gradle.tasks.KotlinCompile by tasks

fun String.toCommand() = this.split(" ")

fun Exec.execute(command: String) = apply {
    commandLine = command.also { logger.info("Command to run: $it") }.toCommand()
}

compileKotlin.kotlinOptions {
    jvmTarget = "19"
}

fun resolveUsername() = System.getenv("GITHUB_ACTOR") ?: read(".github_user")

fun resolveToken() = System.getenv("GITHUB_TOKEN") ?: read(".github_token")

fun read(file: String): String =
    project.rootDir.resolve(file).readLines().firstOrNull() ?: "No file $file found"
