# DigiD — App Móvil Nativa Android (Kotlin + Jetpack Compose)

Este proyecto ha sido migrado a **Android Studio nativo en Kotlin**, utilizando la arquitectura moderna recomendada por Google: **Clean Architecture + MVI/MVVM**, **Jetpack Compose**, **Material 3**, **Dagger Hilt**, **Room Database**, **Retrofit 2** y **Google ML Kit Document Scanner**.

---

## 📁 Estructura del Proyecto Android (`app/src/main/java/bo/gob/gamc/catastro/digid/`)

```
bo.gob.gamc.catastro.digid/
├── DigiDApp.kt                         # Application class (@HiltAndroidApp)
│
├── core/                               # Núcleo y utilitarios transversales
│   ├── network/
│   │   ├── AuthInterceptor.kt          # Inyecta cabecera Authorization: Bearer
│   │   ├── KeycloakAuthenticator.kt    # Refresco automático de tokens en 401
│   │   ├── NetworkMonitor.kt           # Monitoreo de red en tiempo real (Flow)
│   │   └── TokenManager.kt             # Almacenamiento seguro con EncryptedSharedPreferences
│   ├── security/
│   │   └── BiometricHelper.kt          # BiometricPrompt (huella, rostro o PIN del sistema)
│   ├── storage/
│   │   ├── PdfManager.kt               # Generación nativa de PDFs A4 (PdfDocument)
│   │   └── UserStorageManager.kt       # Sandbox aislado por usuario (/usuarios/{userId}/)
│   └── theme/
│       ├── Color.kt                    # Paleta oficial GAMC (#009ED0, #341A67)
│       ├── Type.kt                     # Tipografía institucional
│       └── Theme.kt                    # DigiDTheme (Material 3)
│
├── data/
│   ├── local/                          # Base de datos local (Room)
│   │   ├── DigiDDatabase.kt
│   │   ├── dao/
│   │   │   ├── DocumentDao.kt          # Consultas reactivas (Flow) de documentos
│   │   │   └── ResolucionDao.kt        # Consultas reactivas de resoluciones
│   │   └── entities/
│   │       ├── DocumentEntity.kt       # Tabla de documentos y trámites
│   │       └── ResolucionEntity.kt     # Tabla de resoluciones
│   ├── remote/                         # Clientes HTTP (Retrofit 2)
│   │   ├── api/
│   │   │   ├── KeycloakApi.kt          # Autenticación ROPC y logout
│   │   │   ├── ResolucionesApi.kt      # Subida y listado (FastAPI :8080)
│   │   │   ├── AiApi.kt                # Catálogo, clasificación y preguntas (Node :4000)
│   │   │   └── GeoextractApi.kt        # Subida y listado cartográfico (IDEC ERP :8061)
│   │   └── dto/                        # Modelos DTO serializables con kotlinx.serialization
│   └── repository/
│       ├── AuthRepository.kt           # Lógica de login, sesión y biometría
│       ├── DocumentRepository.kt       # Escaneo, guardado en sandbox y Room
│       ├── ResolucionesRepository.kt   # Subida multipart de páginas a FastAPI
│       ├── AiRepository.kt             # Redimensión a 1600px, base64 e inferencia IA
│       └── GeoextractRepository.kt     # Redimensión a 2400px y subida al ERP
│
├── di/                                 # Inyección de dependencias (Dagger Hilt)
│   ├── AppModule.kt                    # Provisión de Room, Storage y Managers
│   └── NetworkModule.kt                # Provisión de Retrofit, OkHttp y APIs
│
└── presentation/                       # Capa de Interfaz de Usuario (Jetpack Compose)
    ├── MainActivity.kt                 # Actividad principal (FragmentActivity)
    ├── navigation/
    │   ├── Screen.kt                   # Definición de rutas y argumentos
    │   └── AppNavHost.kt               # Grafo de navegación Compose
    ├── auth/
    │   ├── LoginScreen.kt              # Pantalla de Login con diseño institucional y biometría
    │   └── AuthViewModel.kt
    ├── home/
    │   ├── HomeScreen.kt               # Bandeja de documentos, búsqueda, escáner y adjuntos
    │   ├── HomeViewModel.kt
    │   └── components/
    │       ├── DocumentCard.kt         # Tarjeta de documento con miniaturas y acciones
    │       └── ModalDescripcionDialog.kt # Diálogos de descripción y confirmación
    ├── pdf/
    │   ├── PdfViewerScreen.kt          # Visor de PDF nativo embebido (PdfRenderer)
    │   └── PdfViewerViewModel.kt
    ├── resoluciones/
    │   ├── ResolucionesScreen.kt       # Captura multipágina y listado con estados
    │   └── ResolucionesViewModel.kt
    ├── ai/
    │   ├── AIStudyScreen.kt            # Clasificación IA y catálogo de preguntas
    │   └── AIStudyViewModel.kt
    └── geoextract/
        ├── GeoextractScreen.kt         # Captura cartográfica y envío a IDEC ERP
        └── GeoextractViewModel.kt
```

---

## 🚀 Cómo abrir y ejecutar en Android Studio

1. **Abrir Android Studio** (Ladybug, Meerkat o posterior).
2. Seleccionar **File -> Open...** y elegir esta carpeta raíz del proyecto:
   `submodulo-resoluciones-y-clasificacion-ia-refactor-reorganize-monorepo-into-backend-frontend`
3. Android Studio detectará automáticamente los archivos `settings.gradle.kts` y `build.gradle.kts`.
4. Dejar que Gradle sincronice las dependencias del catálogo `gradle/libs.versions.toml`.
5. Conectar un celular Android con depuración USB habilitada o iniciar un Emulador de Android.
6. Presionar el botón **Run ▶ (Shift + F10)**.

---

## ⚙️ Configuración de Backends y Red (`app/build.gradle.kts`)

Por defecto, para pruebas con el emulador oficial de Android Studio, la dirección `10.0.2.2` mapea al `localhost` de tu computadora:

| Backend | URL en Emulador (`10.0.2.2`) | URL en Celular Físico (Wi-Fi) |
|---|---|---|
| **Keycloak SSO** | `https://auth.catastrocbba.com` | `https://auth.catastrocbba.com` |
| **SisCat Clásico** | `https://bkdgd.catastrocbba.com` | `https://bkdgd.catastrocbba.com` |
| **Resoluciones API** | `http://10.0.2.2:8080` | `http://<IP_DE_TU_PC>:8080` |
| **Estudiar con la IA** | `http://10.0.2.2:4000` | `http://<IP_DE_TU_PC>:4000` |
| **IDEC ERP** | `http://10.0.2.2:8061` | `http://<IP_DE_TU_PC>:8061` |

Puedes modificar estas variables en `app/build.gradle.kts` dentro del bloque `defaultConfig`.
