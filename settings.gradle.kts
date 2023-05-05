pluginManagement {
    repositories {
        mavenLocal()
        maven {
            url = uri("https://plugins.gradle.org/m2/")
        }
        maven {
            url = uri("https://maven.pkg.github.com/kjetilv/uplift")
        }
        mavenCentral()
    }
}

rootProject.name = "taninim"

// The river
include("taninim")

// Hell
include("fb")

// The gates
include("yellin")
include("yellin-server")

// The horns
include("kudu")
include("kudu-server")

// The cloud
include("ascension")

// The crucible
include("lambda-test")

