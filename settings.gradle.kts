pluginManagement {
    repositories {
        mavenCentral()
        maven {
            url = uri("https://plugins.gradle.org/m2/")
        }
        mavenLocal()
        maven {
            url = uri("https://maven.pkg.github.com/kjetilv/uplift")
        }
    }
}

rootProject.name = "taninim"

include("fb")

include("yellin")
include("yellin-server")

include("kudu")
include("kudu-server")

include("taninim")
include("ascension")

include("lambda-test")
