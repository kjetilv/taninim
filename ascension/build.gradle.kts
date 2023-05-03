import com.github.kjetilv.uplift.plugins.UpliftPlugin
import com.github.kjetilv.uplift.plugins.UpliftTask
import java.util.*

plugins {
    java
    id("com.github.kjetilv.uplift.plugins.uplift") version "0.1.0-SNAPSHOT"
}

dependencies {
    implementation("software.amazon.awscdk:aws-cdk-lib:2.73.0")
    implementation("software.constructs:constructs:10.1.301")
    testImplementation(platform("org.junit:junit-bom:5.9.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

apply<UpliftPlugin>()

tasks.withType<UpliftTask> {
    configure(
        stack = "taninim"
    )
    env(
        "fbSec" to fbSec()
    )
    stackWith(
        "taninim.uplift.LambdaStacker"
    )
    dependsOn(
        ":kudu:native-lambda",
        ":yellin:native-lambda",
        "build"
    )
}

fun fbSec(needIt: Boolean = false): String = System.getenv("fbSec")
    ?.takeIf { it.isNotBlank() }
    ?.takeIf { it.lowercase(Locale.ROOT) != "null" }
    ?: System.getProperty("fbSec")
    ?: project.property("fbSec")?.toString()
    ?: "fbSec must be set in environment".let {
        if (needIt) throw IllegalStateException(it) else it.also(logger::warn)
    }

tasks.test {
    useJUnitPlatform()
}
