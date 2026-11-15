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
        // Web OAuth client ID; the backend verifies the ID token audience against it.
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"677062331013-1r5cf3etriii0pdsqrc1ks4q6ddhs67j.apps.googleusercontent.com\"")
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

    testOptions {
        unitTests.isIncludeAndroidResources = true
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
        // (IncompatibleClassChangeError). Disabling the whole task hides every
        // other finding, so only the crashing detector is disabled and the rest
        // still report.
        disable += "NullSafeMutableLiveData"

        // Release lint is skipped because lintVitalAnalyzeRelease crashes on the
        // same detector during the vital pass, which cannot be filtered.
        checkReleaseBuilds = false

        // Warnings do not fail the build, but errors do — that is what makes the
        // Lint step meaningful instead of decorative.
        abortOnError = true
        warningsAsErrors = false
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

    // Google Sign-In (Credential Manager)
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.0")
    
    // Room Database
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    implementation("androidx.room:room-paging:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Encrypted at rest. The local mirror is the user's whole ledger, so it is
    // not left as a plain SQLite file that any backup or rooted shell can read.
    implementation("net.zetetic:android-database-sqlcipher:4.5.4")
    implementation("androidx.sqlite:sqlite-ktx:2.4.0")

    // Offline sync: a queued write must survive the app being killed, so the
    // flush runs as WorkManager work rather than a coroutine tied to a screen.
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    implementation("androidx.hilt:hilt-work:1.2.0")
    ksp("androidx.hilt:hilt-compiler:1.2.0")

    // Paging over the local mirror: the transaction list is paginated and
    // filtered, and a plain Flow<List> would load the whole window at once.
    implementation("androidx.paging:paging-runtime-ktx:3.3.2")
    implementation("androidx.paging:paging-compose:3.3.2")
    
    // DataStore Preferences
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    
    // Splash Screen
    implementation("androidx.core:core-splashscreen:1.0.1")
    
    // Charts
    implementation("io.github.ehsannarmani:compose-charts:0.0.11")
    
    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
    testImplementation("androidx.room:room-testing:2.6.1")
    testImplementation("androidx.work:work-testing:2.9.1")
    testImplementation("org.robolectric:robolectric:4.13")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
    testImplementation("androidx.test:core:1.6.1")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation("io.mockk:mockk:1.13.9")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test:rules:1.6.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(composeBom)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("com.google.dagger:hilt-android-testing:2.52")
    kspAndroidTest("com.google.dagger:hilt-compiler:2.52")
}

kotlin {
    jvmToolchain(17)
}
