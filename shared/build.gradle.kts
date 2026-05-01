import io.gitlab.arturbosch.detekt.Detekt
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    // AGP 9.0+: combined plugin replaces android.library + kotlin.multiplatform (developer.android.com/kotlin/multiplatform/plugin)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.libres)
    alias(libs.plugins.mokkery)
}

// ─── Kotlin Multiplatform ─────────────────────────────────────────────────────

kotlin {
    android {
        namespace = "com.yourcompany.kmptemplate.shared"
        compileSdk = 37
        minSdk = 26
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
        // Opt-in to JVM host tests (Robolectric) — creates the androidHostTest source set
        withHostTest {
            isIncludeAndroidResources = true
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { target ->
        target.binaries.framework {
            baseName = "shared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            // Compose Multiplatform
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.components.resources)

            // KotlinX
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)

            // Ktor — abstract NetworkClient (see ADR-008)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.serialization.json)

            // Room KMP
            implementation(libs.room.runtime)

            // DataStore — must be a Koin `single`, never factory (see ADR-003)
            implementation(libs.datastore.preferences.core)

            // Koin + Annotations (see ADR-007)
            implementation(libs.koin.core)
            implementation(libs.koin.annotations)

            // Kermit logging (see ADR-002)
            implementation(libs.kermit)
        }

        androidMain.dependencies {
            implementation(libs.ktor.client.android)
            implementation(libs.koin.android)
            implementation(libs.koin.androidx.compose)
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
            // Bundled SQLite driver — no system dependency required on iOS
            implementation(libs.sqlite.bundled)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotest.assertions)
            implementation(libs.turbine)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
            implementation(libs.koin.test)
        }
    }

    // Koin Annotations: generate module code in commonMain via KSP metadata pass
    sourceSets.commonMain {
        kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
    }
}

// Make KSP metadata generation run before any compilation task
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask<*>>().configureEach {
    if (name != "kspCommonMainKotlinMetadata") {
        dependsOn("kspCommonMainKotlinMetadata")
    }
}

// ─── Room ────────────────────────────────────────────────────────────────────

room {
    // Schema files committed to git — required for AutoMigrations
    schemaDirectory("$projectDir/schemas")
}

// ─── Libres (string resources) ───────────────────────────────────────────────

libres {
    generatedClassName = "Res"
    generateNamedArguments = true
    baseLocaleLanguageCode = "en"
}

// ─── KSP ─────────────────────────────────────────────────────────────────────

dependencies {
    // Koin Annotations — generate via commonMain metadata (single source of truth)
    add("kspCommonMainMetadata", libs.koin.ksp.compiler)

    // Room compiler — per-platform KSP pass
    add("kspAndroid", libs.room.compiler)
    add("kspIosX64", libs.room.compiler)
    add("kspIosArm64", libs.room.compiler)
    add("kspIosSimulatorArm64", libs.room.compiler)

    // androidHostTest — Robolectric for JVM unit tests that need Android Context (e.g. Room in-memory)
    add("androidHostTestImplementation", libs.robolectric)
    add("androidHostTestImplementation", libs.androidx.test.core)
}

// ─── Detekt — layer boundary enforcement ─────────────────────────────────────

tasks.withType<Detekt>().configureEach {
    jvmTarget = "17"
}

// Checks that domain packages don't import data or UI framework classes
val detektDomainImports by tasks.registering(Detekt::class) {
    description = "Verifies forbidden imports in domain layer"
    group = "verification"
    config.setFrom(rootProject.file("detekt/detekt-domain.yml"))
    buildUponDefaultConfig = false
    setSource(fileTree("src/commonMain/kotlin") { include("**/domain/**/*.kt") })
    classpath = files()
    reports {
        html.required.set(false)
        xml.required.set(false)
        txt.required.set(false)
        sarif.required.set(false)
    }
}

// Checks that presentation packages don't import data framework classes directly
val detektPresentationImports by tasks.registering(Detekt::class) {
    description = "Verifies forbidden imports in presentation layer"
    group = "verification"
    config.setFrom(rootProject.file("detekt/detekt-presentation.yml"))
    buildUponDefaultConfig = false
    setSource(fileTree("src/commonMain/kotlin") { include("**/presentation/**/*.kt") })
    classpath = files()
    reports {
        html.required.set(false)
        xml.required.set(false)
        txt.required.set(false)
        sarif.required.set(false)
    }
}

// Wire layer checks into standard verification lifecycle
tasks.named("check") {
    dependsOn(detektDomainImports, detektPresentationImports)
}
