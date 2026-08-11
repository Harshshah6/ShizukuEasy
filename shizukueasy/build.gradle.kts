plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.harshshah6.shizukueasy"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    api(libs.shizuku.api)
    api(libs.shizuku.provider)
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.core.ktx)
}
