import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    // kotlin.android not needed — AGP 9.0+ includes Kotlin support built-in
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.yourcompany.kmptemplate"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.yourcompany.kmptemplate"
        minSdk = 26
        targetSdk = 37

        // Version driven by git tags in CI; falls back to local defaults
        val versionTag = System.getenv("GITHUB_REF_NAME") ?: "0.0.0-local"
        val buildNumber = System.getenv("GITHUB_RUN_NUMBER")?.toInt() ?: 1
        versionCode = buildNumber
        versionName = versionTag.removePrefix("v")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.compose.material3)
    implementation(libs.material.icons.extended)
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.navigation.compose)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
}
