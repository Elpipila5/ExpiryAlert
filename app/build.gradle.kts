plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
}

// La URL no es secreta. Configúrala en ~/.gradle/gradle.properties o -P.
val recetasApiUrl = providers.gradleProperty("RECETAS_API_BASE_URL")
    .orElse("").get().trim().trimEnd('/')
require(recetasApiUrl.isEmpty() || (
    recetasApiUrl.startsWith("https://") &&
        !recetasApiUrl.contains('"') && !recetasApiUrl.contains('\\')
)) { "RECETAS_API_BASE_URL debe ser HTTPS y no contener comillas ni barras inversas." }

android {
    namespace = "com.example.expiryalert"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.expiryalert"
        minSdk = 24
        targetSdk = 37
        versionCode = 6
        versionName = "1.3.0"

        // La clave de OpenAI permanece solo en el backend.
        buildConfigField("String", "RECETAS_API_BASE_URL", "\"$recetasApiUrl\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    sourceSets {
        named("test") { kotlin.directories += "src/sharedTest/java" }
        named("androidTest") { kotlin.directories += "src/sharedTest/java" }
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.all {
            it.maxHeapSize = "1g"
            it.systemProperty("robolectric.graphicsMode", "NATIVE")
            it.systemProperty("expiry.qa.dir", layout.buildDirectory.dir("reports/diseno").get().asFile.absolutePath)
        }
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform("com.google.firebase:firebase-bom:34.19.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.mlkit:text-recognition:16.0.1")
    implementation("com.google.android.gms:play-services-code-scanner:16.1.0")
    implementation("androidx.work:work-runtime-ktx:2.10.5")

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    testImplementation("org.robolectric:robolectric:4.16.1")
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
