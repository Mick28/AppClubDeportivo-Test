# Plan de pruebas automatizadas — Club Deportivo (Grupo 5 · Com. 2D)

Este documento amplía la **sección 12 (Plan de pruebas)** del documento de la entrega. Los casos
PR01–PR20, que antes se ejecutaban solo a mano en el emulador, ahora también están automatizados
en clases `*Test.kt`. Además se suman pruebas unitarias de la lógica y pruebas de la base SQLite.

## 1. Objetivo y alcance

- Verificar automáticamente las funcionalidades de los **tres módulos** y la **navegación**
  (ítems 43 y 44 del checklist).
- Detectar regresiones cuando se sumen los módulos pendientes (HU03, HU09, HU10, HU11): alcanza con
  volver a correr las pruebas antes de cada entrega.
- Fuera de alcance por ahora: pruebas de usabilidad con usuarios reales (sección 14) y las pantallas
  que todavía no existen (no socios, configuración, historial).

## 2. Estrategia: tres niveles

| Nivel | Carpeta | Dónde corre | Herramientas | Qué prueba |
|---|---|---|---|---|
| **Unitarias locales** | `app/src/test/` | JVM de la PC, sin emulador (segundos) | JUnit 4 | Reglas de negocio y utilidades puras: estado del socio, formato de fechas e importes, hash con salt, modelos |
| **Instrumentación · datos** | `app/src/androidTest/.../data/` | Emulador o celular | AndroidX Test + JUnit 4 | `BaseDatosClub` sobre SQLite real: login, alta, cobro, carnet, listado, restricciones `CHECK`, transacciones |
| **Instrumentación · interfaz** | `app/src/androidTest/.../ui/` | Emulador o celular | Espresso (+ contrib, intents) | Casos PR01–PR20 del plan: pantallas, validaciones, navegación e Intents |

Las pruebas manuales con capturas de la sección 12 se mantienen: las automáticas no reemplazan la
revisión visual de colores, tipografía y textos.

## 3. Estructura de carpetas

Se usan los dos *source sets* estándar de Android, separados del código de la app (`src/main`).
Dentro de cada uno, los paquetes copian los de `src/main`, así cada prueba queda al lado de la clase
que prueba (en espejo) y es fácil de encontrar y mantener.

```text
app/src/
├── main/                                   # Código de la app (sin cambios)
├── test/java/com/grupo5/clubdeportivo/     # Pruebas unitarias locales (JVM)
│   ├── model/
│   │   ├── EstadoSocioTest.kt              # PU-01 · regla HABILITADO / INHABILITADO / PENDIENTE / SIN CUOTA / BAJA
│   │   └── ModeloTest.kt                   # PU-02 · Socio, SocioDetalle, Empleado
│   └── util/
│       ├── FormatoTest.kt                  # PU-03 · fechas, moneda, N° de comprobante, saludo
│       ├── SeguridadTest.kt                # PU-04 · salt y SHA-256
│       └── SesionTest.kt                   # PU-05 · cierre de sesión
└── androidTest/java/com/grupo5/clubdeportivo/   # Pruebas de instrumentación (emulador)
    ├── soporte/
    │   └── PruebaUi.kt                     # Reinicio de la base, sesión de prueba y matchers del RecyclerView
    ├── data/
    │   └── BaseDatosClubTest.kt            # PI-01 · capa de datos SQLite (37 pruebas)
    └── ui/
        ├── LoginTest.kt                    # PR01–PR04
        ├── RegistroSocioTest.kt            # PR05–PR08
        ├── FlujoAltaSocioTest.kt           # PR09–PR12 (de punta a punta)
        ├── CobroCuotaTest.kt               # PR13–PR15
        ├── ComprobanteTest.kt              # PR10b–PR10c (recibo e Intent implícito)
        ├── VencimientosTest.kt             # PR16–PR18
        ├── CarnetTest.kt                   # PR19
        └── NavegacionTest.kt               # PR20
```

**Total: 118 pruebas** — 30 unitarias locales y 88 de instrumentación (37 de datos y 51 de interfaz).

## 4. Configuración agregada al proyecto

En `app/build.gradle.kts` se agregó:

- `testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"`.
- `testOptions { animationsDisabled = true }` (Espresso es más estable sin animaciones).
- Dependencias de prueba: `junit:junit:4.13.2` (`testImplementation`) y AndroidX Test 1.6,
  `androidx.test.ext:junit` 1.2.1, Espresso 3.6.1 core, contrib e intents (`androidTestImplementation`).

No se modificó ninguna clase de `src/main`.

## 5. Cómo ejecutarlas

### Desde Android Studio

1. Esperar el **Gradle Sync** (descarga las dependencias de prueba la primera vez).
2. **Unitarias:** clic derecho en `app/src/test/java` › **Run 'Tests in 'java''**. No hace falta emulador.
3. **Instrumentación:** con el emulador encendido, clic derecho en `app/src/androidTest/java` ›
   **Run 'All Tests'**. También se puede correr una sola clase o un solo método con el ▶ verde
   que aparece al lado de su nombre.
4. Recomendado en el emulador: *Opciones de desarrollador* › poner en **0x** las tres escalas de
   animación (ventana, transición y duración del animador).

### Desde la consola

```bash
# Unitarias (JVM)
./gradlew testDebugUnitTest            # Windows: gradlew.bat testDebugUnitTest

# Instrumentación (con un emulador o celular conectado: ver `adb devices`)
./gradlew connectedDebugAndroidTest    # Windows: gradlew.bat connectedDebugAndroidTest
```

Reportes HTML:

- `app/build/reports/tests/testDebugUnitTest/index.html`
- `app/build/reports/androidTests/connected/debug/index.html`

## 6. Datos de prueba y aislamiento

- Cada prueba de instrumentación **borra `clubdeportivo.db` y cierra la sesión** antes de empezar
  (`PruebaUi.reiniciarBase()`), así la base se vuelve a crear con los usuarios de la sección 11 y el
  resultado no depende del orden de ejecución.
- **No correr las pruebas de instrumentación en un celular con datos reales**: se pierden.
- Los vencimientos de prueba son relativos al día de ejecución, igual que en la app. Las pruebas
  unitarias usan una fecha fija (30/09/2026) para no depender del calendario.
- Números fijos que usan las pruebas: socios 1 GOMEZ · 2 RUIZ · 3 PEREZ · 4 DIAZ · 5 LOPEZ · 6 SOSA ·
  7 BENITEZ · 8 ACOSTA · 9 RUBIO; el primer socio nuevo es el **10** y el primer recibo nuevo el **000009**.
  Si se cambia `DatosDePrueba.kt`, hay que actualizar estos valores esperados.

## 7. Matriz de trazabilidad: casos PR → clase de prueba

| ID | Caso (sección 12) | Clase › método | Resultado esperado que se verifica |
|---|---|---|---|
| PR01 | Ingreso de empleado | `LoginTest › pr01_ingresoDeEmpleado_abreElMenuPrincipal` | Menú con saludo, rol ADMIN y tarjeta "2 socios con cuota que vence hoy" |
| PR02 | Ingreso de socio | `LoginTest › pr02_ingresoDeSocio_abreSoloSuCarnet` | Carnet de GOMEZ, Ana, HABILITADO, sin botón volver ni acciones de empleado |
| PR03 | Credenciales incorrectas | `LoginTest › pr03_credencialesIncorrectas_muestraErrorSinAvanzar` | "Usuario o contraseña incorrectos." sin avanzar |
| PR04 | Rol de consulta | `LoginTest › pr04_rolConsulta_noPuedeRegistrarNiCobrar` · `VencimientosTest › pr18b_…` | Registro y Cobro deshabilitados, aviso de rol, sin COBRAR en el listado |
| PR05 | Campos obligatorios | `RegistroSocioTest › pr05_camposObligatoriosVacios_seMarcanYNoSeGuarda` | Campos marcados, foco en el primero, no se inserta |
| PR06 | Alias repetido | `RegistroSocioTest › pr06_aliasRepetido_marcaElAliasYNoSeGuarda` | "Alias en uso" con foco en el alias |
| PR07 | Documento repetido | `RegistroSocioTest › pr07_documentoRepetido_marcaElDocumento` | "Documento ya registrado" |
| PR08 | Formatos inválidos | `RegistroSocioTest › pr08_…` a `pr08f_…` | Error en DNI, correo, contraseñas, alias y teléfono |
| PR09 | Alta completa | `FlujoAltaSocioTest › pr09_a_pr12_…` | Paso automático a PRIMERA CUOTA con el socio cargado y monto 18000 |
| PR10 | Cobro de 1ª cuota | `FlujoAltaSocioTest › pr09_a_pr12_…` · `ComprobanteTest` | Recibo N° 000009 con los 7 datos, vencimiento hoy + 1 mes, carnet emitido |
| PR11 | Carnet tras el alta | `FlujoAltaSocioTest › pr09_a_pr12_…` | Carnet PRUEBA, Tomás, HABILITADO, vencimiento hoy + 1 mes |
| PR12 | Socio nuevo entra a la app | `FlujoAltaSocioTest › pr09_a_pr12_…` | Login con el alias nuevo → su carnet |
| PR13 | Búsqueda por documento | `CobroCuotaTest › pr13_busquedaPorDocumento_…` | SOSA, Julio, INHABILITADO, 14 días de atraso |
| PR14 | Cuotas solo con tarjeta | `CobroCuotaTest › pr14_cuotasSoloConTarjeta` | 3 y 6 deshabilitadas con efectivo, habilitadas con tarjeta |
| PR15 | Pago adelantado | `CobroCuotaTest › pr15_pagoAdelantado_noPierdeDias` | Nuevo vencimiento = actual + 1 día + 1 mes |
| PR16 | Listado del día | `VencimientosTest › pr16_listadoDelDia_ordenadoPorApellidoYNombre` | Fecha de hoy y DIAZ, LOPEZ, PEREZ, RUIZ en ese orden |
| PR17 | Vencidas acumuladas | `VencimientosTest › pr17_vencidasAcumuladas` | BENITEZ (40 días) y SOSA (14 días) |
| PR18 | Interacción del listado | `VencimientosTest › pr18_interaccionDelListado_carnetYCobro` | Tocar abre el carnet; tras cobrar, LOPEZ desaparece al volver |
| PR19 | Apto físico pendiente | `CarnetTest › pr19_registrarAptoFisico_pasaDePendienteAHabilitado` | ACOSTA pasa de PENDIENTE a HABILITADO |
| PR20 | Salir y control automático | `NavegacionTest › pr20_…` y `pr20c_…` | Vuelve a la pantalla inicial; al abrir la app se inhabilitan los vencidos |

### Casos complementarios (nuevos)

| ID | Clase › método | Qué agrega |
|---|---|---|
| PR02b | `LoginTest › pr02b_aliasEnMayusculas_tambienIngresa` | El alias no distingue mayúsculas |
| PR03b–c | `LoginTest › pr03b_…`, `pr03c_…` | Clave de socio incorrecta; campos vacíos y cambio de etiqueta por perfil |
| PR06b | `RegistroSocioTest › pr06b_…` | "AGomez" también se detecta como repetido |
| — | `RegistroSocioTest › rolConsulta_…`, `cancelar_…` | Consulta no puede abrir el alta; CANCELAR no guarda |
| PR10b–c | `ComprobanteTest` | 7 datos del recibo por `putExtra`, estado PENDIENTE, **Intent implícito ACTION_SEND** de Compartir, VER CARNET |
| PR13b–d | `CobroCuotaTest` | Búsqueda por N° de socio, socio inexistente, búsqueda vacía |
| PR14b | `CobroCuotaTest › pr14b_…` | Tarjeta en 6 cuotas queda en el recibo y en la base |
| PR15b–c | `CobroCuotaTest` | Importe en cero; CANCELAR en la confirmación |
| PR16b | `VencimientosTest › pr16b_…` | Estado vacío del listado |
| PR19b–f | `CarnetTest` | Inhabilitado con motivo y COBRAR; socio sin 1ª cuota (empleado y socio); un socio no ve el carnet de otro; consulta sin acciones; cerrar sesión |
| PR20b, d, e | `NavegacionTest` | Cancelar la salida; cada botón del menú abre su pantalla; menú sin sesión vuelve al inicio |

### Pruebas unitarias (src/test)

| ID | Clase | Pruebas | Qué cubre |
|---|---|---|---|
| PU-01 | `EstadoSocioTest` | 8 | Baja, sin cuota, vencida ayer, vence hoy (sigue habilitado), sin apto → PENDIENTE, cuota vencida con prioridad sobre el apto, etiquetas |
| PU-02 | `ModeloTest` | 7 | `apellidoYNombre`, `documento`, `estado()` de SocioDetalle, `esAdministrador` |
| PU-03 | `FormatoTest` | 8 | dd/MM/aaaa, fecha nula, fecha larga en español, `$ 18.000`, `$ 18.000,50`, `000009`, saludo en los límites de horario |
| PU-04 | `SeguridadTest` | 6 | Salt de 16 bytes aleatorio, vector conocido SHA-256, mismo salt = mismo hash, distinto salt = distinto hash |
| PU-05 | `SesionTest` | 1 | `cerrar()` borra empleado y socio |

### Pruebas de la capa de datos (PI-01 · `BaseDatosClubTest`, 37 pruebas)

| Grupo | Métodos probados | Ejemplos de lo que se verifica |
|---|---|---|
| Estructura | `onCreate`, `onUpgrade`, `DatosDePrueba` | 8 tablas + índice; 2 empleados, 9 socios, 8 cuotas; contraseñas nunca en texto plano y un hash distinto por socio |
| Login | `validarEmpleado`, `validarSocio` | Roles ADMIN/CONSULTA; clave errónea; empleado inactivo y socio dado de baja no ingresan |
| Registro | `buscaAlias`, `buscaDocumentoSocio`, `insertarSocio`, `registrarAptoFisico` | Socio nuevo inactivo, sin cuota ni carnet; alias repetido deshace la transacción completa; reutiliza la persona existente |
| Cobro | `buscarSocio…`, `obtenerValorCuota`, `calcularPeriodo`, `registrarPago`, `obtenerCarnet` | Carnet con el 1er pago; pago adelantado; reactivación; renovación de carnet vencido; `CHECK` rechaza efectivo en 3 cuotas y monto 0 |
| Control automático | `inhabilitarVencidos` | Solo afecta a los vencidos antes de la fecha; incluye socios activos sin cuotas |
| Listado | `listarVencimientosDelDia`, `listarVencidasAcumuladas`, `contarVencenHoy` | Orden por apellido; quien renovó o está de baja no aparece; acumuladas del más atrasado al más reciente |

## 8. Criterios de aceptación

- Antes de cada entrega: **100 % de las pruebas en verde** (unitarias e instrumentación) y los
  reportes HTML guardados en la carpeta de la entrega.
- Una prueba que falla se registra en la tabla de resultados con la observación y se corrige el
  código (o la prueba, si cambió el requisito) antes de entregar.
- Toda funcionalidad nueva se entrega con al menos una prueba en la carpeta que le corresponda.

## 9. Registro de resultados

| Fecha | Dispositivo / API | Unitarias | Instrumentación | Observaciones |
|---|---|---|---|---|
| 30/09/2026 | JVM (fuera de Android Studio) | 30 / 30 OK | — | Unitarias compiladas con Kotlin 2.0.21 y ejecutadas con JUnit 4.13.2. Las consultas SQL esperadas se verificaron sobre SQLite con los mismos datos de prueba. |
| | Emulador Pixel 7 · API 34 | | ___ / 88 | *A completar por el equipo al correr `connectedDebugAndroidTest`* |

## 10. Convenciones para mantener las pruebas

- Nombre del archivo: `<ClaseProbada>Test.kt`, en el **mismo paquete** que la clase de `src/main`.
- Nombre del método: `queSePrueba_condicion_resultadoEsperado` (o `prNN_…` si corresponde a un caso
  del plan), y en el KDoc el ID del caso.
- Cada prueba prepara sus datos y no depende de otra (`@Before` reinicia la base).
- Lógica pura (sin `Context`) → `src/test`. Todo lo que usa `Context`, SQLite o pantallas → `src/androidTest`.
- Código compartido entre pruebas (reinicio de base, matchers) → paquete `soporte`, nunca en `src/main`.
