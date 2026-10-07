plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.mobileappdev"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.example.mobileappdev"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
}

dependencies {
        implementation("androidx.core:core-ktx:1.12.0")
        implementation("org.jetbrains.kotlin:kotlin-metadata-jvm:2.4.20")
        implementation("androidx.appcompat:appcompat:1.6.1")
        implementation(libs.activity.ktx)
        implementation("com.google.android.material:material:1.10.0")
        implementation("androidx.constraintlayout:constraintlayout:2.1.4")

        implementation("androidx.sqlite:sqlite:2.4.0")
        implementation("androidx.room:room-runtime:2.8.5")
        implementation(libs.room.common)
        annotationProcessor("androidx.room:room-compiler:2.8.5")
        implementation("androidx.lifecycle:lifecycle-livedata:2.6.2")
        implementation("androidx.work:work-runtime:2.9.0")
        implementation("androidx.security:security-crypto:1.1.0-alpha06")

        implementation("com.squareup.retrofit2:retrofit:2.9.0")
        implementation("com.squareup.retrofit2:converter-gson:2.9.0")
        implementation("com.google.code.gson:gson:2.10.1")

        testImplementation("junit:junit:4.13.2")
        testImplementation("androidx.arch.core:core-testing:2.2.0")
        testImplementation("org.robolectric:robolectric:4.13")
        testImplementation("androidx.test:core:1.6.1")
        androidTestImplementation("androidx.test.ext:junit:1.2.1")
        androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
