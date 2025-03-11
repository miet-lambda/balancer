val kotlinVersion: String by project
val logbackVersion: String by project

plugins {
    kotlin("jvm") version "2.1.10"
    application
    id("org.jlleitschuh.gradle.ktlint") version "12.2.0"
}

group = "miet.lambda"
version = "0.0.1"

application {
    mainClass.set("miet.lambda.ApplicationKt")
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlinVersion")
}

kotlin {
    jvmToolchain(21)
}

ktlint {
    version.set("1.5.0")
    debug.set(true)
}
