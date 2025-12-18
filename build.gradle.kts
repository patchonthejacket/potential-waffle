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

sourceSets {
    create("friendTest") {
        kotlin.srcDir("src(other)/main/kotlin")
        kotlin.srcDir("src(other)/test/kotlin")
        resources.srcDir("src(other)/main/resources")
        resources.srcDir("src(other)/test/resources")
        compileClasspath += sourceSets["main"].output
        runtimeClasspath += sourceSets["main"].output
    }
}

val friendTestImplementation by configurations.getting {
    extendsFrom(configurations["testImplementation"])
}

tasks.register<Test>("friendTest") {
    description = "Run tests for friend's code in src(other)"
    group = "verification"
    testClassesDirs = sourceSets["friendTest"].output.classesDirs
    classpath = sourceSets["friendTest"].runtimeClasspath
    useJUnitPlatform()
}
