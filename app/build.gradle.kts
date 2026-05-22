plugins {
    alias(libs.plugins.androidApplication)
    id("com.google.gms.google-services")  // new
}

android {
    namespace = "com.example.spottermobile"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.finalproject"
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation("com.google.zxing:core:3.5.3")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation("androidx.core:core:1.13.1")
    implementation("androidx.work:work-runtime:2.9.0") // removed duplicate

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.1.0")) // downgraded — stable
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-firestore")

    // Required by Firebase internally
    implementation("androidx.datastore:datastore-preferences:1.0.0") // use 1.0.0 not 1.1.1
    implementation("androidx.datastore:datastore-core:1.0.0")        // add this too
}