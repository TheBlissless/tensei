pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        maven { url = uri("https://repo.huaweicloud.com/repository/maven") }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    // TEMP (local compile only, reverted after): toolchain resolver not needed.
    // id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        maven { url = uri("https://repo.huaweicloud.com/repository/maven") }
        mavenCentral()
    }
}

rootProject.name = "tensei"
include(":app")
