plugins {
    id("com.github.johnrengelman.shadow") version "5.1.0"
    maven
    java
    `maven-publish`
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

repositories {
    mavenCentral()
    maven(url = "https://dl.bintray.com/ijabz/maven")
}

dependencies {
    implementation("org.gagravarr", "vorbis-java-core", "0.8")
    implementation("io.netty", "netty-all", "4.1.41.Final")
    implementation("ch.qos.logback:logback-classic:1.2.3")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.9.9")
    implementation("org.antlr:ST4:4.1")

    testImplementation("junit:junit:4.12")
}
