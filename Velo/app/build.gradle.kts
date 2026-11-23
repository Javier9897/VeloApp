import java.net.URI
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp") version "2.2.10-2.0.2"
    alias(libs.plugins.kotlin.android)
}

val veloApiUrl = providers.gradleProperty("veloApiUrl").orElse("").get().trim().trimEnd('/')
val veloApiUri = veloApiUrl.takeIf(String::isNotEmpty)?.let(::URI)
val esApiLocal = veloApiUri?.scheme == "http" && veloApiUri.host in setOf("127.0.0.1", "localhost", "10.0.2.2")
require(veloApiUri == null || (
    (veloApiUri.scheme == "https" || esApiLocal) &&
        veloApiUri.host != null && veloApiUri.userInfo == null &&
        veloApiUri.path.isEmpty() && veloApiUri.query == null && veloApiUri.fragment == null
)) { "Configura veloApiUrl con el origen HTTPS del servidor o con HTTP de loopback para desarrollo." }

val releaseStoreFile = providers.environmentVariable("VELO_SIGNING_STORE_FILE").orNull
val releaseStorePassword = providers.environmentVariable("VELO_SIGNING_STORE_PASSWORD").orNull
val releaseKeyAlias = providers.environmentVariable("VELO_SIGNING_KEY_ALIAS").orNull
val releaseKeyPassword = providers.environmentVariable("VELO_SIGNING_KEY_PASSWORD").orNull
val releaseSigningValues = listOf(releaseStoreFile, releaseStorePassword, releaseKeyAlias, releaseKeyPassword)
require(releaseSigningValues.all { it.isNullOrBlank() } || releaseSigningValues.none { it.isNullOrBlank() }) {
    "Para firmar Velo en release, configura las cuatro variables VELO_SIGNING_*."
}

android {
    namespace = "com.duoc.velo"
    compileSdk {
        version = release(37) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.duoc.velo"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "VELO_API_URL", "\"$veloApiUrl\"")
    }

    val veloReleaseSigning = releaseStoreFile?.takeIf(String::isNotBlank)?.let { path ->
        signingConfigs.create("veloRelease") {
            storeFile = file(path)
            storeType = "JKS"
            storePassword = releaseStorePassword
            keyAlias = releaseKeyAlias
            keyPassword = releaseKeyPassword
        }
    }

    buildTypes {
        debug {
            enableUnitTestCoverage = true
        }
        release {
            signingConfig = veloReleaseSigning
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin{
    compilerOptions{
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.core.ktx)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.json.jvm)
    testImplementation(libs.mockwebserver)
    implementation(libs.retrofit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.compose.material.icons.extended)

    val roomVersion = "2.8.4"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

}
