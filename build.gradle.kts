plugins {
    alias(libs.plugins.android.application) apply false
    // Removed the Kotlin line!
    id("com.google.gms.google-services") version "4.4.1" apply false
}