import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
    kotlin("plugin.serialization")
}

// Release signing is driven by keystore.properties at the repo root, which the release
// workflow writes from repository secrets and deletes afterwards. Absent the file
// (every push build, local dev) the release variant simply stays unsigned.
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) keystorePropsFile.inputStream().use { load(it) }
}
val hasSigningMaterial = keystoreProps.getProperty("storeFile") != null

android {
    namespace = "com.efina.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.efina.app"
        minSdk = 26
        targetSdk = 35
        // Supplied by the release workflow as -PversionCode / -PversionName.
        versionCode = (project.findProperty("versionCode") as String?)?.toIntOrNull() ?: 1
        versionName = (project.findProperty("versionName") as String?) ?: "1.0.0"

        testInstrumentationRunner = "com.efina.app.HiltTestRunner"

        // API Configuration
        buildConfigField("String", "API_BASE_URL", "\"https://efina.himz.my.id/api/\"")
    }

    signingConfigs {
        if (hasSigningMaterial) {
            create("release") {
                storeFile = file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            if (hasSigningMaterial) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "API_BASE_URL", "\"https://efina.himz.my.id/api/\"")
        }
        debug {
            isMinifyEnabled = false
            buildConfigField("String", "API_BASE_URL", "\"https://efina.himz.my.id/api/\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    lint {
        // Kotlin 2.0 + AGP lint crashes on NonNullableMutableLiveDataDetector
        // (IncompatibleClassChangeError). Disable lint for release builds until
        // AGP supports Kotlin 2.0.
        checkReleaseBuilds = false
        abortOnError = false
    }


    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Security
    // NOTE: security-crypto has no stable release; 1.1.0-alpha06 is the latest.
    // At-rest token encryption depends on it. Track androidx.security releases
    // and upgrade when a stable version ships.
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    
    // Core Android
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.0")
    implementation("androidx.activity:activity-compose:1.9.0")
    
    // Compose BOM
    val composeBom = platform("androidx.compose:compose-bom:2025.04.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    
    // Navigation
    implementation("androidx.navigation:navigation-compose:2.8.0")
    implementation("androidx.hilt:hilt-navigation-compose:1.3.0")
    
    // Hilt DI
    implementation("com.google.dagger:hilt-android:2.52")
    ksp("com.google.dagger:hilt-compiler:2.52")
    
    // Retrofit + OkHttp
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-kotlinx-serialization:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    
    // Kotlin Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.0")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.0")
    
    // Room Database
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    
    // DataStore Preferences
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    
    // Splash Screen
    implementation("androidx.core:core-splashscreen:1.0.1")
    
    // Charts
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    
    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
    testImplementation("io.mockk:mockk:1.13.9")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(composeBom)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("com.google.dagger:hilt-android-testing:2.52")
    kspAndroidTest("com.google.dagger:hilt-compiler:2.52")
}

kotlin {
    jvmToolchain(17)
}
