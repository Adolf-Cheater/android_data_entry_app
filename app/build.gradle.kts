plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("kotlin-android")
    id("kotlin-kapt")

}

android {
    namespace = "com.example.data_entry_android"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.data_entry_android"
        minSdk = 28
        targetSdk = 34
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.ui.geometry.android)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(libs.androidx.appcompat)
    implementation(libs.gson)
    implementation(libs.retrofit)
    implementation(libs.retrofitGson)
    implementation("com.squareup.okhttp3:okhttp:4.9.2")
    implementation("com.squareup.okhttp3:logging-interceptor:4.9.2")
    implementation("com.squareup.moshi:moshi:1.12.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.12.0")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.9.0")
    implementation("com.github.bumptech.glide:glide:4.12.0")
    kapt("com.github.bumptech.glide:compiler:4.12.0")
    implementation ("com.google.code.gson:gson:2.8.9")
    //implementation(libs.  androidx. activity. ComponentActivity)
    //implementation ("com.github.ArthurHub:Android-Image-Cropper:2.8.0")
    //implementation ("com.github.CanHub:Android-Image-Cropper:4.3.2")
    //implementation ("com.theartofdev.edmodo:android-image-cropper:2.8.0")
    //implementation ("com.github.yalantis:uCrop:2.2.8-native")
    implementation ("androidx.core:core-ktx:1.7.0")
    implementation ("androidx.appcompat:appcompat:1.4.1")
    //implementation ("org.apache.poi:poi-ooxml:5.2.3")
    implementation ("org.apache.poi:poi-ooxml:5.2.0")
    implementation ("org.apache.poi:poi:5.2.0")
    implementation ("com.github.CanHub:Android-Image-Cropper:4.3.0")
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.5.2")

}