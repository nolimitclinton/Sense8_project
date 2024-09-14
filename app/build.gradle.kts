plugins {
    kotlin("kapt")
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.example.sense8"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.sense8"
        minSdk = 28
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.4.3"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    // Allow references to generated code for Dagger-Hilt
    kapt {
        correctErrorTypes = true
    }
}

dependencies {
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("com.google.android.libraries.mapsplatform.transportation:transportation-consumer:2.3.0")
    // Dependency Versions
    val splashScreenVersion = "1.0.1"
    val dataStoreVersion = "1.0.0"
    val navComposeVersion = "2.6.0"
    val daggerHiltVersion = "2.45"
    val tfLiteVersion = "0.4.0"
    val cameraxVersion = "1.3.0"

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui:1.6.8")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    // Splash-Screen Dependency
    implementation("androidx.core:core-splashscreen:$splashScreenVersion")

    // Preferences DataStore Dependency
    implementation("androidx.datastore:datastore-preferences:$dataStoreVersion")

    //Compose Navigation
    implementation("androidx.navigation:navigation-compose:$navComposeVersion")
    implementation("com.google.android.gms:play-services-maps:19.0.0")
    implementation("com.google.maps.android:maps-compose:2.11.2")
    implementation("com.google.maps.android:android-maps-utils:2.3.0")
    // Dagger Hilt Dependencies
    implementation("com.google.dagger:hilt-android:$daggerHiltVersion")
    kapt("com.google.dagger:hilt-android-compiler:$daggerHiltVersion")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Tensorflow-Lite Dependencies
    implementation("org.tensorflow:tensorflow-lite-task-vision:$tfLiteVersion")
    implementation("org.tensorflow:tensorflow-lite-gpu-delegate-plugin:$tfLiteVersion")
    implementation("org.tensorflow:tensorflow-lite-gpu:2.9.0")

    // Camera-X Dependencies
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-video:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")
    implementation("androidx.camera:camera-extensions:$cameraxVersion")

    // Accompanist Permission manager Dependency
    implementation("com.google.accompanist:accompanist-permissions:0.33.2-alpha")

    testImplementation("junit:junit:4.13.2")

    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.06.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}