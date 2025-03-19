// app/build.gradle.kts
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.chatapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.chatapp"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17 // Обновлено до Java 17
        targetCompatibility = JavaVersion.VERSION_17 // Обновлено до Java 17
    }

    kotlinOptions {
        jvmTarget = "17" // Корректный способ установки jvmTarget
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.3.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-analytics")
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.firestore.ktx)

    // RecyclerView
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // Material3
    implementation(libs.androidx.material3.android)

    // Glide
    implementation("com.github.bumptech.glide:glide:4.16.0")
}
