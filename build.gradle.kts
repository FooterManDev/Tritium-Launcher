plugins {
    kotlin("multiplatform") version "2.0.21"
    kotlin("plugin.serialization") version "2.0.21"
}

group = "io.github.footerman"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

kotlin {
    jvm("desktop") {
        withJava()
    }

    sourceSets {
        val desktopMain by getting

        commonMain.dependencies {
            implementation(libs.kotlin.stdlib.common)
        }
        desktopMain.dependencies {
            // Kotlin standard library
            implementation(libs.kotlin.stdlib.jdk8)

            // FlatLaf
            implementation(libs.flatlaf)

            // Ktor
            val ktor = libs.ktor
            implementation(ktor.client.core)
            implementation(ktor.client.cio)
            implementation(ktor.client.auth)
            implementation(ktor.client.json)
            implementation(ktor.client.logging)
            implementation(ktor.client.content.negotiation)
            implementation(ktor.client.serialization)
            implementation(ktor.serialization.kotlinx.json)

            // Toml serialization
            val kToml = libs.ktoml
            implementation(kToml.core)
            implementation(kToml.file)

            // MSAL4j
            implementation(libs.msal4j)

            // Logback
            implementation(libs.logback.classic)
        }
    }
}