import sun.jvmstat.monitor.MonitoredVmUtil.jvmArgs

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
            languageVersion.set(JavaLanguageVersion.of(24))
        }
        sourceCompatibility = JavaVersion.VERSION_24
        targetCompatibility = JavaVersion.VERSION_24
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
            jvmArgs("--enable-preview", "--add-modules", "jdk.incubator.vector")
            useJUnitPlatform()
        }

        withType<JavaExec>().configureEach {
            jvmArgs("--enable-preview", "--add-modules", "jdk.incubator.vector")
        }
    }
}

fun String.toCommand() = this.split(" ")

fun Exec.execute(command: String) = apply {
    commandLine = command.also { logger.info("Command to run: $it") }.toCommand()
}

fun resolveUsername() = resolveProperty("githubUser", "GITHUB_ACTOR")

fun resolveToken() = resolveProperty("githubToken", "GITHUB_TOKEN")

fun resolveProperty(property: String, variable: String? = null, defValue: String? = null) =
    System.getProperty(property)
        ?: variable?.let { System.getenv(it) }
        ?: project.takeIf { it.hasProperty(property) }?.property(property)?.toString()
        ?: defValue
        ?: property
