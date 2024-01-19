plugins {
    id("com.android.application")
}

android {
    namespace = "com.bely.dominogpt"
    compileSdk = 23

    defaultConfig {
        applicationId = "com.bely.dominogpt"
        minSdk = 23
        targetSdk = 23
        versionCode = 1
        versionName = "1.0"

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
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {


    testImplementation("junit:junit:4.13.2")

}