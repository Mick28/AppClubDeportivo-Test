plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.grupo5.clubdeportivo"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.grupo5.clubdeportivo"
        // minSdk 26 (Android 8.0): necesario para usar java.time (LocalDate) sin desugaring,
        // tal como lo propone el libro de la Etapa 4 (Tema I, 4.3 Uso de fechas).
        minSdk = 26
        targetSdk = 34
        versionCode = 2
        versionName = "2.0"

        // Runner de las pruebas de instrumentación (src/androidTest), ver PRUEBAS.md.
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    testOptions {
        // Las pruebas de Espresso son más estables sin animaciones en el emulador.
        animationsDisabled = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // ---- Plan de pruebas (sección 12) ----
    // Pruebas unitarias locales: corren en la JVM de la PC, sin emulador (src/test).
    testImplementation("junit:junit:4.13.2")

    // Pruebas de instrumentación: corren en el emulador o en un celular (src/androidTest).
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:core-ktx:1.6.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test:rules:1.6.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.test.espresso:espresso-contrib:3.6.1") // RecyclerView y DatePicker
    androidTestImplementation("androidx.test.espresso:espresso-intents:3.6.1") // Intent implícito (Compartir)
}
