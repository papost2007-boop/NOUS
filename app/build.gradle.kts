plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.nous.tutoringapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.nous.tutoringapp" // 🎯 Το βασικό applicationId της εφαρμογής σου
        minSdk = 26
        targetSdk = 36
        versionCode = 2 // 🎯 Αυξάνουμε το versionCode σε 2 για να αναγνωριστεί ως ΕΝΗΜΕΡΩΣΗ (Update)!
        versionName = "1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        buildConfig = true
    }

    flavorDimensions += "brand"

    productFlavors {
        create("nousApp") {
            dimension = "brand"
            applicationId = "com.nous.tutoringapp" // 🎯 ΑΚΡΙΒΩΣ ΤΟ ΙΔΙΟ με την παλιά εφαρμογή στο κινητό!
            resValue("string", "app_name", "ΝΟΥΣ Φροντιστήριο")
            buildConfigField("boolean", "SHOW_LOGO", "true")
        }
        create("generic") {
            dimension = "brand"
            applicationId = "com.edumanager.app" // 🎯 Για την καινούργια/generic έκδοση στο Play Store
            resValue("string", "app_name", "EduManager")
            buildConfigField("boolean", "SHOW_LOGO", "false")
        }
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
    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
    implementation("androidx.work:work-runtime:2.9.0")
}