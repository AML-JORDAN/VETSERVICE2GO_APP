plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.appvetservice"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.appvetservice"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.cardview)
    implementation(libs.androidx.exifinterface)
    // Firebase Firestore: la base de datos en internet que comparten la app y la página web.
    // El "BoM" elige versiones de Firebase que funcionan bien juntas.
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)
    // await(): esperar la respuesta de Firebase sin congelar la pantalla
    implementation(libs.kotlinx.coroutines.play.services)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}