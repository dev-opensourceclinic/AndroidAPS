import kotlin.math.min

plugins {
    alias(libs.plugins.android.library)
    id("kotlin-android")
    id("android-module-dependencies")
}

android {
    namespace = "app.aaps.core.keys"
    defaultConfig {
        minSdk = min(Versions.minSdk, Versions.wearMinSdk)  // Compatible with wear module
    }
}
