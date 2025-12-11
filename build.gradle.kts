plugins {
    kotlin("jvm") version "2.2.10"
    id("ru.yarsu.json-project-properties")
    id("ru.yarsu.application-export-plugin")
    id("org.jlleitschuh.gradle.ktlint") version "13.1.0"
}

group = "ru.ac.uniyar"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}
val ktlintVersion: String by project
ktlint {
    version.set(ktlintVersion)
}
tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}
