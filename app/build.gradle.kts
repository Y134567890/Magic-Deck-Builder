
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)

    id("org.jetbrains.kotlin.plugin.parcelize")

    alias(libs.plugins.kotlinxSerialization)

    alias(libs.plugins.googleServices)
    alias(libs.plugins.crashlytics)

    id("org.jetbrains.dokka")
}

android {
    namespace = "com.alejandro.magicdeckbuilder"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.alejandro.magicdeckbuilder"
        minSdk = 28
        targetSdk = 35
        versionCode = 3
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.runtime.livedata)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.savedstate)


    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.credentials)
    implementation(libs.firebase.play.services)
    implementation(libs.firebase.googleid)

    implementation("com.google.android.gms:play-services-auth:21.0.0")
    
    implementation(libs.firebase.firestore)

    // Retrofit para llamadas a la API
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)

    // Coil para carga de imágenes
    implementation(libs.coil.compose)

    // Material Icons Extended para iconos adicionales
    implementation(libs.androidx.material.icons.extended)

    // Kotlinx Serialization
    implementation(libs.kotlinx.serialization.json)

    implementation("com.google.code.gson:gson:2.10.1")

}
