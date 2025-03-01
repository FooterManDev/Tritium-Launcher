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
            implementation(kotlin("stdlib-common"))
        }
        desktopMain.dependencies {
            implementation(kotlin("stdlib-jdk8"))
            implementation("com.formdev:flatlaf:3.5.4")

            // TODO: Move version values to central location

            // Ktor
            val ktor = "3.0.3"
            implementation("io.ktor:ktor-client-core:$ktor")
            implementation("io.ktor:ktor-client-cio:$ktor")
            implementation("io.ktor:ktor-client-auth:$ktor")
            implementation("io.ktor:ktor-client-json:$ktor")
            implementation("io.ktor:ktor-client-logging:$ktor")
            implementation("io.ktor:ktor-client-content-negotiation:$ktor")
            implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor")
            implementation("io.ktor:ktor-client-serialization:$ktor")

            // Toml serialization
            val kToml = "0.5.1"
            implementation("com.akuleshov7:ktoml-core:$kToml")
            implementation("com.akuleshov7:ktoml-file:$kToml")

            // MSAL4j
            val msal = "1.19.0"
            implementation("com.microsoft.azure:msal4j:$msal")

            implementation("ch.qos.logback:logback-classic:1.5.16") // Deals with a warning during launch that stalls the launcher.
        }
    }
}