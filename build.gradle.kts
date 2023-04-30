plugins {
    java
}

allprojects {

    group = "com.github.kjetilv.flacsefugl"
    version = "1.0-SNAPSHOT"
}

subprojects {
    buildscript {
        repositories {
            mavenLocal()
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/kjetilv/uplift")
                credentials {
                    username = resolveUsername()
                    password = resolveToken()
                }
            }
            dependencies {
                classpath("com.github.johnrengelman:shadow:8.1.0")
                classpath("com.github.kjetilv.uplift:uplift-gradle-plugins:0.1.0-SNAPSHOT")
            }
        }
    }

    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
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

configure<JavaPluginExtension> {
    sourceCompatibility = JavaVersion.VERSION_19
    targetCompatibility = JavaVersion.VERSION_19
}

fun String.toCommand() = this.split(" ")

fun Exec.execute(command: String) = apply {
    commandLine = command.also { logger.info("Command to run: $it") }.toCommand()
}

fun resolveUsername() = System.getenv("GITHUB_ACTOR") ?: read(".github_user")

fun resolveToken() = System.getenv("GITHUB_TOKEN") ?: read(".github_token")

fun read(file: String): String =
    project.rootDir.listFiles()
        ?.find { it.name.equals(file) }
        ?.readLines()
        ?.firstOrNull()
        ?: "No file $file found"
