
plugins {
    id("com.android.dynamic-feature")
    id("kotlin-android")
    id("kotlin-kapt")
}

android {
    namespace = "com.swordfish.lemuroid.core.citra"
    defaultConfig {
        missingDimensionStrategy("opensource", "play")
        missingDimensionStrategy("cores", "dynamic")
    }
    packagingOptions {
        jniLibs {
            useLegacyPackaging = true
        }

        doNotStrip("*/*/*_libretro_android.so")
    }
}

dependencies {
    implementation(project(":lemuroid-app"))
    implementation(kotlin(deps.libs.kotlin.stdlib))
}

