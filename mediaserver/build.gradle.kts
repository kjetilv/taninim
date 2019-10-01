plugins {
    id("com.github.johnrengelman.shadow") version "5.1.0"
    maven
    java
    `maven-publish`
}

dependencies {
    implementation("io.netty", "netty-all", "4.1.41.Final")

    testImplementation("junit:junit:4.12")
}
