pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "mapkit-android"
include(":source:mapkit-for-android-core")
include(":source:mapkit-for-android-webview")
include(":source:mapkit-for-android-compose")
include(":source:mapkit-android")
project(":source:mapkit-android").projectDir = file("source/mapkit-for-android")
include(":example:app")
