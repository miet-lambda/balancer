rootProject.name = "balancer"

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        kotlin("jvm") version extra["kotlinVersion"] as String
        kotlin("plugin.serialization") version extra["kotlinVersion"] as String
        id("io.ktor.plugin") version extra["ktorVersion"] as String
        id("org.jlleitschuh.gradle.ktlint") version "12.2.0"
    }
}
