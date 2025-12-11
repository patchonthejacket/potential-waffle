plugins {
    `java-gradle-plugin`
    kotlin("jvm") version "2.0.0"
}

gradlePlugin {
    plugins {
        create("JsonProjectPropertiesPlugin") {
            id = "ru.yarsu.json-project-properties"
            implementationClass = "ru.yarsu.JsonProjectPropertiesPlugin"
        }
        create("ApplicationExportPlugin") {
            id = "ru.yarsu.application-export-plugin"
            implementationClass = "ru.yarsu.ApplicationExportPlugin"
        }
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.20.0")
}


