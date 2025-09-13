plugins {
    java
}

allprojects {
    group = "com.github.kjetilv.taninim"
    version = "0.1.1-SNAPSHOT"
}

subprojects {
    apply(plugin = "java")

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(25))
        }
        sourceCompatibility = JavaVersion.VERSION_25
        targetCompatibility = JavaVersion.VERSION_25
        withSourcesJar()
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

    tasks {
        withType<Test>().all {
            useJUnitPlatform()
        }
    }
}

fun String.toCommand() = this.split(" ")

fun resolveUsername() = resolveProperty("githubUser", "GITHUB_ACTOR")

fun resolveToken() = resolveProperty("githubToken", "GITHUB_TOKEN")

fun resolveProperty(property: String, variable: String? = null, defValue: String? = null) =
    System.getProperty(property)
        ?: variable?.let { System.getenv(it) }
        ?: project.takeIf { it.hasProperty(property) }?.property(property)?.toString()
        ?: defValue
        ?: property
