import com.github.kjetilv.uplift.plugins.UpliftPlugin
import com.github.kjetilv.uplift.plugins.UpliftTask
import java.util.*

plugins {
    id("com.github.kjetilv.uplift.plugins.uplift") version "0.1.1-SNAPSHOT"
}

dependencies {
    implementation("software.amazon.awscdk:aws-cdk-lib:2.151.0")
    implementation("software.constructs:constructs:10.3.0")
    testImplementation(platform("org.junit:junit-bom:5.11.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

apply<UpliftPlugin>()

tasks.withType<UpliftTask> {
    configure(
        stack = "taninim",
        arch = "x86_64"
    )
    env(
        "fbSec" to get(name = "fbSec"),
        "taninimBucket" to get(name = "taninimBucket")
    )
    stackWith("taninim.uplift.LambdaStacker")
    dependsOn(
        ":kudu:native-lambda",
        ":yellin:native-lambda",
        "build"
    )
}

fun get(name: String, needIt: Boolean = false): String =
    System.getenv(name)?.takeIf { it.isNotBlank() }?.takeIf { it.lowercase(Locale.ROOT) != "null" }
        ?: System.getProperty(name) ?: project.takeIf { it.hasProperty(name) }?.property(name)?.toString()
        ?: "$name-not-set".let {
            if (needIt) throw IllegalStateException(it) else it.also(logger::warn)
        }
