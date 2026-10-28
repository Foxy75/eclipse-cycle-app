plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "io.github.foxy75.eclipse"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "io.github.foxy75.eclipse"
        minSdk = 23
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isShrinkResources = true
            optimization {
                enable = true
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = true
    }
    androidResources {
        localeFilters += listOf("id", "en")
    }
    lint {
        checkReleaseBuilds = false
    }
}

configurations.all {
    resolutionStrategy.force("androidx.constraintlayout:constraintlayout:2.2.2")
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.recyclerview)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}