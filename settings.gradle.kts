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

include("kernel")
include("kudu")
include("yellin")
include("fb")
include("asynchttp")
include("yellin-server")
include("kudu-server")
include("json")
include("lambda-test")
include("flogs")
include("lambda")
include("flambda")
include("taninim")
include("s3")
include("ascension")
