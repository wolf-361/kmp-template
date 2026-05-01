plugins {
    // Declared here so the version catalog resolves them; applied false — each module opts in
    alias(libs.plugins.kotlin.multiplatform) apply false
    // kotlin.android omitted — AGP 9.0+ includes Kotlin support built-in
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.skie) apply false
    alias(libs.plugins.libres) apply false
    alias(libs.plugins.mokkery) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.spotless) apply false
}

// Apply Spotless and Detekt to every submodule
subprojects {
    apply(plugin = "io.gitlab.arturbosch.detekt")
    apply(plugin = "com.diffplug.spotless")

    configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension> {
        config.setFrom(rootProject.file("detekt/detekt.yml"))
        buildUponDefaultConfig = true
        parallel = true
        autoCorrect = false
    }

    configure<com.diffplug.gradle.spotless.SpotlessExtension> {
        kotlin {
            target("**/*.kt")
            targetExclude("**/build/**/*.kt", "**/generated/**/*.kt")
            ktlint(libs.versions.ktlint.get()).editorConfigOverride(
                mapOf("max_line_length" to "120"),
            )
        }
        kotlinGradle {
            target("**/*.gradle.kts")
            ktlint(libs.versions.ktlint.get())
        }
    }
}

// Pre-commit hook installation — triggered automatically by composeApp's preBuild
tasks.register("installGitHooks") {
    description = "Copies scripts/pre-commit.sh into .git/hooks/pre-commit"
    group = "git hooks"
    doLast {
        val source = rootProject.file("scripts/pre-commit.sh")
        val target = File(rootProject.file(".git/hooks"), "pre-commit")
        source.copyTo(target, overwrite = true)
        target.setExecutable(true)
        println("✓ pre-commit hook installed")
    }
}
