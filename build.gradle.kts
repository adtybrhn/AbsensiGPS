plugins {
    // Plugin dari Version Catalog
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.jetbrainsKotlinAndroid) apply false

    // Plugin Google Services
    id("com.google.gms.google-services") version "4.4.1" apply false
}