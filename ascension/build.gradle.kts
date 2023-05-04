import com.github.kjetilv.uplift.plugins.UpliftPlugin
import com.github.kjetilv.uplift.plugins.UpliftTask
import java.util.*

plugins {
    java
    id("com.github.kjetilv.uplift.plugins.uplift") version "0.1.0-SNAPSHOT"
}

dependencies {
    implementation("software.amazon.awscdk:aws-cdk-lib:2.78.0")
    implementation("software.constructs:constructs:10.2.15")
    testImplementation(platform("org.junit:junit-bom:5.9.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

apply<UpliftPlugin>()

tasks.withType<UpliftTask> {
    configure(
        stack = "taninim"
    )
    env(
        "fbSec" to get(name = "fbSec", false),
        "taninimBucket" to get(name = "taninimBucket", false)
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

fun get(name: String, needIt: Boolean = false): String =
    System.getenv(name)
        ?.takeIf { it.isNotBlank() }
        ?.takeIf { it.lowercase(Locale.ROOT) != "null" }
        ?: System.getProperty(name)
        ?: project.property(name)?.toString()
        ?: "$name-not-set".let {
            if (needIt) throw IllegalStateException(it) else it.also(logger::warn)
        }

tasks.test {
    useJUnitPlatform()
}
