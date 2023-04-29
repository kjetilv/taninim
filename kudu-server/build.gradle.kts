import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.google.cloud.tools.jib.gradle.JibTask

plugins {
    java
    id("com.github.johnrengelman.shadow") version "8.1.0"
    id("com.google.cloud.tools.jib") version "3.3.1"
    `maven-publish`
}

apply { plugin("com.github.johnrengelman.shadow") }

dependencies {
    implementation("com.github.kjetilv.uplift:uplift-kernel:0.1.0-SNAPSHOT")
    implementation("com.github.kjetilv.uplift:uplift-flogs:0.1.0-SNAPSHOT")
    implementation("com.github.kjetilv.uplift:uplift-s3:0.1.0-SNAPSHOT")
    implementation("com.github.kjetilv.uplift:uplift-asynchttp:0.1.0-SNAPSHOT")

    implementation(project(":taninim"))
    implementation(project(":kudu"))

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.2")
}

configure<JavaPluginExtension> {
    sourceCompatibility = JavaVersion.VERSION_19
    targetCompatibility = JavaVersion.VERSION_19
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

tasks.withType<ShadowJar> {
    manifest {
        attributes(mapOf(Pair("Main-Class", "mediaserver.kudu.LambdaKudu")))
    }
    mergeServiceFiles()
    minimize()
    dependsOn("build")
}

tasks.withType<JibTask> {
    dependsOn("shadowJar")
}

val timestamp = "${System.currentTimeMillis()}"
fun head(prefix: Int = 0) =
    File("${rootDir}/.git/HEAD").readLines(Charsets.UTF_8)[0].let { head ->
        head.split(" ")[1]
    }.let { head ->
        File("${rootDir}/.git/${head}").readLines(Charsets.UTF_8)[0]
    }.let { head ->
        if (prefix > 0)
            head.substring(0, prefix)
        else
            head
    }

File(File(System.getProperty("user.home")), ".latest_kudu").printWriter(Charsets.UTF_8)
    .apply {
        println(timestamp)
    }
    .run {
        close()
    }

jib {
    from {
        image = "openjdk:19-alpine"
    }
    to {
        image = "732946774009.dkr.ecr.eu-north-1.amazonaws.com/kudu-server"
        tags = setOf("latest", timestamp, head(8))
    }
    container {
        mainClass = "mediaserver.kudu.server.ServerKudu"
        ports = listOf("80", "8080")
        jvmFlags = listOf(
            "-Dgithash=${head()}",
            "-Dhashepoch=${timestamp}"
        )
    }
}

