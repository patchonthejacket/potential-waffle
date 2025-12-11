package ru.yarsu

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.distribution.DistributionContainer

class ApplicationExportPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val distributionContainer = target.extensions.getByType(DistributionContainer::class.java)
        distributionContainer.create("Export") { distribution ->
            distribution.contents { copySpec ->
                copySpec.from(".editorconfig")
                copySpec.from("build.gradle.kts")
                copySpec.from("build.properties.json")
                copySpec.into("buildSrc/") {
                    it.from("buildSrc/build.gradle.kts")
                }
                copySpec.into("buildSrc/src") {
                    it.from("buildSrc/src")
                }
                copySpec.from("gradle.properties")
                copySpec.into("gradle") {
                    it.from("gradle")
                }
                copySpec.from("gradlew")
                copySpec.from("gradlew.bat")
                copySpec.from("readme.md")
                copySpec.into("src") {
                    it.from("src")
                }
                copySpec.from("settings.gradle.kts")
            }
        }
    }
}
