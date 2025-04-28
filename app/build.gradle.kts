plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.fitness_app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.fitness_app"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation ("androidx.media3:media3-exoplayer:1.6.1")
    implementation ("androidx.media3:media3-ui:1.6.1")
    implementation ("com.github.imagekit-developer.imagekit-android:imagekit-android:3.0.0")
    implementation("org.json:json:20231013")
    implementation("androidx.activity:activity-compose:1.8.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation ("androidx.work:work-runtime-ktx:2.7.1")
    implementation ("com.google.firebase:firebase-firestore-ktx:24.9.1")
    implementation ("com.google.firebase:firebase-auth-ktx:22.3.0")
    implementation ("com.google.accompanist:accompanist-permissions:0.28.0")
    implementation("androidx.compose.material:material-icons-core:1.5.4")
    implementation("androidx.compose.material:material-icons-extended:1.5.4")
    implementation("io.coil-kt:coil-compose:2.4.0")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.firebase.storage.ktx)
    implementation(libs.androidx.runtime.livedata)
    implementation(libs.androidx.runtime)
    implementation(libs.play.services.base)
    implementation(libs.play.services.base)
    implementation(libs.play.services.base)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}