plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "gpsplus.rtkgps"
    compileSdk = 34

    defaultConfig {
        applicationId = "gpsplus.rtkgps"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64"))
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

    ndkVersion = "28.2.13676358"
//    ndkVersion = "21.1.6352462"

    externalNativeBuild {
        ndkBuild {
            path = file("jni/Android.mk")
        }
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation (files("libs/d2xx.jar"))
    implementation("androidx.core:core-ktx:1.13.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.android.material:material:1.13.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("androidx.preference:preference-ktx:1.2.1")

    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.9.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.4")

    implementation("androidx.navigation:navigation-fragment-ktx:2.9.4")
    implementation("androidx.navigation:navigation-ui-ktx:2.9.4")

    implementation("com.google.android.gms:play-services-gcm:17.0.0")
    implementation("com.google.gms:google-services:4.4.3")
    implementation ("androidx.viewpager2:viewpager2:1.1.0")
    implementation ("com.google.android.material:material:1.13.0")

    implementation ("com.squareup.okhttp3:okhttp:5.1.0")

    implementation ("org.osmdroid:osmdroid-android:6.1.20")
    implementation("commons-net:commons-net:3.12.0")
    implementation("org.slf4j:slf4j-api:2.0.17")
    implementation("cz.msebera.android:httpclient:4.5.8")
    implementation("com.infstory:proguard-annotations:1.0.1")
    implementation("com.karumi:dexter:6.2.3")
    implementation("ch.acra:acra:4.11.1")
    implementation("com.google.code.findbugs:jsr305:3.0.2")
    implementation("com.google.firebase:firebase-firestore:26.0.1")

    testImplementation("junit:junit:4.13.2")

    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
}
