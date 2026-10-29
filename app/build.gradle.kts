
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) {
        f.inputStream().use {
            load(it)
        }
    }
}

android {
    namespace = "udb.edu.sv.dsm.desafio03"
    compileSdk = 37

    defaultConfig {
        applicationId = "udb.edu.sv.dsm.desafio03"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            "String",
            "RESTCOUNTRIES_TOKEN",
            "\"${localProps.getProperty("RESTCOUNTRIES_TOKEN", "")}\""
        )

        buildConfigField(
            "String",
            "GEONAMES_USER",
            "\"${localProps.getProperty("GEONAMES_USER", "")}\""
        )

        buildConfigField(
            "String",
            "WEATHERAPI_KEY",
            "\"${localProps.getProperty("WEATHERAPI_KEY", "")}\""
        )
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
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
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)

    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("io.coil-kt:coil:2.6.0")
    implementation("org.osmdroid:osmdroid-android:6.1.18")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
