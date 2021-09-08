pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        jcenter()
    }
}

rootProject.name = ("locorepo-gradle-plugin")

include(":example")
includeBuild("plugin-build")
