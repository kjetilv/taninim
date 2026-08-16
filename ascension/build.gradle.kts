import com.github.kjetilv.uplift.plugins.UpliftTask

plugins {
    id("com.github.kjetilv.uplift.plugins.uplift") version "0.1.1-SNAPSHOT"
}

dependencies {
    implementation("software.amazon.awscdk:aws-cdk-lib:2.264.0")
    implementation("software.constructs:constructs:10.8.1")
}

tasks.withType<UpliftTask> {
    configure(stack = "taninim")
    env(
        "fbSec" to get("fbSec"),
        "taninimBucket" to get("taninimBucket")
    )
    stackWith("taninim.uplift.LambdaStacker")
    dependsOn(
        ":kudu:native-lambda",
        ":yellin:native-lambda",
        "build"
    )
}

fun get(name: String, needIt: Boolean = false): String =
    System.getenv(name)?.takeIf { it.isNotBlank() }?.takeIf { it.lowercase() != "null" }
        ?: System.getProperty(name)
        ?: project.takeIf { it.hasProperty(name) }
            ?.property(name)
            ?.toString()
        ?: "$name-not-set".let {
            if (needIt) throw IllegalStateException(it) else it.also(logger::warn)
        }
