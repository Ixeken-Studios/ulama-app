plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    // apply code analysis tools
    // these plugins are declared in root build.gradle.kts (apply false)
}

// ktlint and detekt removed per user request

android {
    namespace = "com.ixeken.worldcupinfo"

    signingConfigs {
        create("release") {
            val propsFile = rootProject.file("app/keystore.properties")
            if (propsFile.exists()) {
                val lines = propsFile.readLines()
                fun getValue(key: String): String? {
                    val prefix = "$key="
                    return lines.firstOrNull { it.startsWith(prefix) }?.removePrefix(prefix)?.trim()
                }
                storeFile = rootProject.file(getValue("storeFile") ?: error("Missing storeFile in keystore.properties"))
                storePassword = getValue("storePassword") ?: error("Missing storePassword in keystore.properties")
                keyAlias = getValue("keyAlias") ?: error("Missing keyAlias in keystore.properties")
                keyPassword = getValue("keyPassword") ?: error("Missing keyPassword in keystore.properties")
            }
        }
    }
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ixeken.worldcupinfo"
        minSdk = 34
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release")
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
    lint {
        disable += "LocalContextGetResourceValueCall"
        disable += "UnusedMaterial3ScaffoldPaddingParameter"
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.icons.core)
    implementation(libs.androidx.compose.icons.extended)
    implementation(libs.androidx.compose.ui)

    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Ciclo de vida y extensiones de Compose
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Retrofit
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)

    // WorkManager
    implementation(libs.androidx.work.runtime)
    implementation(libs.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}