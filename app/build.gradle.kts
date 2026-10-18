import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// Release signing: put the upload key details in keystore.properties (gitignored):
//   storeFile=/path/to/upload-keystore.jks
//   storePassword=…
//   keyAlias=upload
//   keyPassword=…
// Without it, release builds sign with the debug key so they can still be run locally.
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

android {
    namespace = "com.rowshr.siddur"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.rowshr.siddur"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"
        vectorDrawables.useSupportLibrary = true
    }

    signingConfigs {
        if (keystoreProperties.isNotEmpty()) {
            create("upload") {
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = if (keystoreProperties.isNotEmpty()) signingConfigs.getByName("upload") else signingConfigs.getByName("debug")
        }
    }

    bundle {
        language { enableSplit = false }
    }

    lint {
        lintConfig = file("lint.xml")
        warningsAsErrors = false
        abortOnError = true
        checkReleaseBuilds = true
    }

    testOptions { unitTests.isReturnDefaultValues = true }

    buildFeatures { compose = true }

    // The prayer texts are JSON read with a streaming parser; keep them uncompressed so
    // they can be memory-mapped from the APK instead of inflated on every launch.
    androidResources { noCompress += listOf("json") }

    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}

android.compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}



dependencies {
    implementation(libs.core.ktx)
    implementation(libs.core.splashscreen)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.navigation.compose)
    implementation(libs.datastore.preferences)
    implementation(libs.window)
    implementation(libs.window.core)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.material3.window.size)
    implementation(libs.compose.material3.adaptive)
    implementation(libs.compose.material3.adaptive.layout)
    implementation(libs.compose.material3.adaptive.navigation.suite)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    debugImplementation(libs.compose.ui.tooling)
    testImplementation(libs.junit)
}
