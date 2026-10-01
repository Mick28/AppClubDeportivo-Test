# Plan de Acción: Actualización de ClubDeportivo a Android 17 (API 37)

Este documento detalla el plan paso a paso para actualizar la aplicación **ClubDeportivo** desde Android 14 (API 34) hasta **Android 17 (API 37)**.

## 1. Fase de Preparación y Herramientas
- **Android Studio:** Actualizar a la última versión estable (Android Studio Meerkat / Jellyfish o superior).
- **Android SDK Platform & Build-Tools:** Descargar el SDK para **Android 17 (API 37)** desde el SDK Manager de Android Studio.
- **Gradle Wrapper:** Actualizar en `gradle/wrapper/gradle-wrapper.properties` a Gradle 8.9+ o superior.

## 2. Actualización de Plugins Raíz (`build.gradle.kts`)
Actualizar los plugins de compilación en el archivo `build.gradle.kts` raíz:
```kotlin
plugins {
    id("com.android.application") version "8.7.0" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
}
```

## 3. Configuración del Módulo App (`app/build.gradle.kts`)
Modificar los SDKs objetivo y de compilación:
```kotlin
android {
    namespace = "com.grupo5.clubdeportivo"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.grupo5.clubdeportivo"
        minSdk = 26 // Mantenido en 26 para soportar java.time (LocalDate) sin desugaring
        targetSdk = 37
        versionCode = 2
        versionName = "2.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}
```

## 4. Actualización de Dependencias AndroidX y Pruebas
Actualizar las librerías principales de AndroidX y de pruebas a sus versiones estables compatibles con API 37:
```kotlin
dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:core-ktx:1.6.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test.rules:1.6.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.test.espresso:espresso-contrib:3.6.1")
    androidTestImplementation("androidx.test.espresso:espresso-intents:3.6.1")
}
```

## 5. Adaptaciones de Comportamiento (Android 15 / 16 / 17)
1. **Edge-to-Edge Obligatorio:**
   - Asegurar que todas las Activities (`MainActivity`, `LoginActivity`, `MenuPrincipalActivity`, `RegistroSocioActivity`, `CobroCuotaActivity`, `ComprobanteActivity`, `CarnetActivity`, `VencimientosActivity`) manejen adecuadamente los `WindowInsets` o utilicen contenedores con insets para evitar solapamientos con las barras del sistema.
2. **Gestos de Retroceso Predictivo:**
   - Verificar el funcionamiento del sistema de retroceso en Activities con navegación personalizada y habilitar `android:enableOnBackInvokedCallback="true"` en el `AndroidManifest.xml` si es necesario.

## 6. Plan de Validación y Pruebas
1. Sincronizar el proyecto con Gradle (**Gradle Sync**).
2. Compilar el proyecto: `./gradlew assembleDebug` (o `gradlew.bat assembleDebug` en Windows).
3. Ejecutar las **Pruebas Unitarias**: `./gradlew testDebugUnitTest` (30 pruebas).
4. Ejecutar las **Pruebas de Instrumentación**: `./gradlew connectedDebugAndroidTest` con un emulador en API 37 (88 pruebas).
