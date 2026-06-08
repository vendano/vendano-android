plugins {
    alias(libs.plugins.android.application) apply false
    // kotlin-android is now built-in to AGP 9.x; no explicit apply needed
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt.android) apply false
    alias(libs.plugins.google.services) apply false
}
