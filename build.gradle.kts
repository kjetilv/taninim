plugins {
    java
}

allprojects {
    group = "com.github.kjetilv.taninim"
    version = "0.1.0-SNAPSHOT"
}

subprojects {
    apply(plugin = "java")

    configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
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
            mavenContent {
                includeGroup("com.github.kjetilv.uplift")
            }
        }
    }

    tasks.test {
        useJUnitPlatform()
    }
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
