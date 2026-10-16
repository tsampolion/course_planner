plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.coursebook.pacer"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.coursebook.pacer"
        // 26 gives java.time without core-library desugaring and adaptive icons without PNG fallbacks.
        minSdk = 26
        targetSdk = 35
        versionCode = 19
        versionName = "1.8.0"
    }

    androidResources {
        // English only: drops the translated strings that Compose and AndroidX ship.
        localeFilters += "en"
    }

    // CI signs releases with the permanent key (docs/SIGNING_BRIEF.md): it decodes the keystore to the
    // path in SIGNING_KEYSTORE and passes its password in SIGNING_PASSWORD. Never change the alias.
    val releaseKeystore = providers.environmentVariable("SIGNING_KEYSTORE").orNull?.takeIf { it.isNotBlank() }
    signingConfigs {
        if (releaseKeystore != null) {
            create("release") {
                val password = providers.environmentVariable("SIGNING_PASSWORD").orNull
                require(!password.isNullOrEmpty()) { "SIGNING_KEYSTORE is set but SIGNING_PASSWORD is empty" }
                storeFile = file(releaseKeystore)
                storeType = "pkcs12"
                storePassword = password
                keyAlias = "release"
                keyPassword = password
            }
        }
    }

    buildTypes {
        debug {
            // A separate app, "Course Pacer (debug)", so debug builds never replace a release or its data.
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Without SIGNING_KEYSTORE (Android Studio, local builds) the debug key signs it, so it still installs,
            // but it can't install over a release.
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = false
    }
    packaging {
        resources.excludes += setOf("/META-INF/{AL2.0,LGPL2.1}", "DebugProbesKt.bin", "kotlin-tooling-metadata.json")
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)

    testImplementation(libs.junit)
}
