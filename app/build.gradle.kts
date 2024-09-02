plugins {
    id("com.android.application")
}

android {
    namespace = "io.github.abdurazaaqmohammed.ApkSigner"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.github.abdurazaaqmohammed.ApkSigner"
        minSdk = 1
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
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
    buildFeatures {
        viewBinding = false
    }
    dependencies {
        implementation("org.apache.commons:commons-compress:1.24.0")
    }
}