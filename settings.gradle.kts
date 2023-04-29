pluginManagement {
    repositories {
        mavenCentral()
        mavenLocal()
        maven {
            url = uri("https://plugins.gradle.org/m2/")
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
