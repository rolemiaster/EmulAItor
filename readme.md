# EmulAItor

**EmulAItor** es un emulador "todo en uno" de código abierto para Android, basado en [Libretro](https://www.libretro.com/).
Es un *fork* avanzado de [Lemuroid](https://github.com/Swordfish90/Lemuroid), diseñado para ofrecer una experiencia de usuario superior con descarga de juegos integrada (archivos alojados en archive.org, sin relación alguna con este proyecto), soporte para nube/NAS y compatibilidad total con Android TV.

---

## Diferencias Clave con Lemuroid

EmulAItor extiende la funcionalidad base añadiendo características premium totalmente gratuitas:

*   **☁️ Descarga de Juegos Integrada:** Explorador nativo de **Archive.org** para buscar y descargar ROMs legalmente preservadas sin salir de la app.
*   **📂 Soporte SMB/NAS:** Escanea y juega directamente desde tu servidor local o NAS.
*   **📺 Android TV First:** Interfaz y selectores de archivos optimizados para TV, incluyendo soporte para dispositivos antiguos sin SAF (Storage Access Framework).
*   **🤖 Editor de Metadatos:** Corrige nombres y carátulas de juegos mal identificados manualmente.

---

## Funcionalidades

*   **Guardado Automático:** Guarda y restaura el estado del juego automáticamente.
*   **Escaneo de ROMs:** Indexación recursiva rápida (local y red).
*   **Controles Táctiles:** Optimizados y personalizables.
*   **Gamepad Support:** Compatibilidad nativa con mandos Bluetooth y USB.
*   **Shaders:** Simulación de pantallas CRT/LCD para nostalgia visual.
*   **Cloud Save Sync:** Sincronización de partidas guardadas (experimental).
*   **Sin Publicidad:** Proyecto 100% libre y sin tracking.

---

### 7. Protocolo de Compilación y Compliance (16KB Page Size)
**IMPORTANTE:** Para cumplir con el requisito de Android 15 (API 35+) sobre alineación de memoria de 16KB, el proyecto mantiene una solución conservadora. **NO MODIFICAR SIN LEER ESTO.**

#### A. Contexto Técnico
- **Problema detectado por Google Play:** `base/lib/x86_64/libppsspp_libretro_android.so` no era compatible con páginas de 16KB porque sus segmentos ELF estaban alineados a 4KB.
- **Causa:** El problema no está en la descarga de cores ni en LibretroDroid, sino en un binario concreto del core PPSSPP para `x86_64`.
- **Solución aplicada:** Se ha seguido la solución upstream de LemuroidCores posterior a `1.17.0`: neutralizar el core PPSSPP `x86_64` no compatible para que no sea analizado como ELF inválido por Google Play.
- **Alcance:** No se modifica la descarga de cores, Play Dynamic Features ni la lógica de Runtime Extraction.

#### B. Configuración Actual del Proyecto
1.  **Manifests (App + Cores):** `android:extractNativeLibs="false"`
    - Esta configuración forma parte del flujo actual de publicación y no debe cambiarse sin una investigación específica.

2.  **Gradle (App + Cores):**
    ```kotlin
    packagingOptions {
        jniLibs {
            useLegacyPackaging = true
        }
    }
    ```
    - Esta configuración se mantiene deliberadamente porque permitió publicar builds anteriores y estabilizó el empaquetado de cores.
    - No debe mezclarse su limpieza con fixes de alineación ELF.
    - **Importante:** `useLegacyPackaging = true` no convierte un `.so` ELF de 4KB en compatible con 16KB; solo afecta al empaquetado.

3.  **Runtime Extraction (`GameLoader.kt`):**
    - Al cargar un juego, primero busca el núcleo en `nativeLibraryDir`.
    - Si no lo encuentra, extrae manualmente el `.so` desde el APK/base/splits a `codeCacheDir/bundled_cores/`.
    - Esta lógica es crítica para que las ROMs funcionen y no debe tocarse salvo aprobación explícita y pruebas completas.
    - Ver método `extractBundledCore()` en `retrograde-app-shared/src/main/java/com/swordfish/lemuroid/lib/game/GameLoader.kt`.

4.  **PPSSPP `x86_64`:**
    - `lemuroid_core_ppsspp/src/main/jniLibs/x86_64/libppsspp_libretro_android.so` queda neutralizado porque upstream lo retiró por no ser compatible con páginas grandes.
    - PSP sigue soportado en ABIs compatibles como `arm64-v8a`.
    - En `x86_64`, PPSSPP queda pendiente de un core upstream compatible.

#### C. Estado de Compliance con Google Play
- **Estado Actual:** El proyecto está en "prórroga" hasta el 31 de mayo de 2026.
- **Advertencia histórica de Google:** El escáner detectó el core PPSSPP `x86_64` no compatible con 16KB.
- **Estrategia actual:** Mantener intacto el flujo de cores y neutralizar únicamente el artefacto nativo problemático, siguiendo upstream.

#### D. Scripts de Mantenimiento
- `update_cores_gradle.ps1`: Inyecta `useLegacyPackaging = true` en los núcleos. No usarlo para intentar corregir alineación ELF.
- `toggle_16kb_mode.ps1`: Script histórico. Revisar antes de usar porque puede no reflejar el estado actual del proyecto.
- `check_elf.py`: Verifica alineación ELF de `.so` locales.
- `check_compression.py`: Verifica compresión de `.so` dentro de un AAB.

#### E. Pasos para Release (Google Play)
1.  Asegurar `targetSdkVersion = 35` en `deps.kt`.
2.  No cambiar descarga/instalación de cores ni Runtime Extraction.
3.  Comprobar que PPSSPP `x86_64` sigue neutralizado.
4.  Limpiar y compilar usando el flujo estándar del proyecto (`build_creator.bat`).
5.  Verificar el AAB resultante antes de subirlo a Google Play.

#### F. Solución Futura
Cuando upstream publique cores plenamente compatibles:
1.  Actualizar cores de forma controlada, no "a granel" sin validación.
2.  Verificar alineación con `check_elf.py`, `readelf -l` o herramienta equivalente.
3.  Evaluar en una tarea separada si conviene ordenar `useLegacyPackaging`.
4.  Mantener Runtime Extraction como fallback salvo que se rediseñe y pruebe todo el flujo de carga de cores.

---

## 🏗️ Estructura del Proyecto (Fork)

A continuación se detalla la estructura del proyecto con el sistema de overrides:

```text
EmulAItor/
├── _upstream_lemuroid/          # Copia de referencia del upstream (para comparar)
├── lemuroid-app/
│   └── src/
│       ├── main/                # ⚠️ LEMUROID ORIGINAL - NO MODIFICAR DIRECTAMENTE
│       └── emulaitor/           # ✅ PERSONALIZACIONES EMULAITOR - EDITAR AQUÍ
│           └── java/com/swordfish/lemuroid/app/mobile/feature/
│               ├── catalog/         # Catálogo Archive.org y SMB
│               ├── metadata/        # Proveedores de metadatos
│               ├── disclaimer/      # Aviso legal
│               └── main/            # Overrides de navegación
├── retrograde-app-shared/       # Lógica compartida (con overrides en src/emulaitor/)
├── catalog/                     # JSONs de configuración de fuentes
└── docs/                        # Documentación técnica
```

> **Ver sección "Arquitectura de Overrides" más abajo para detalles completos.**

---

## 🏛️ Arquitectura de Source Sets (v017+)

> **IMPORTANTE:** A partir de la versión v017, EmulAItor separa el código personalizado en `src/emulaitor/`. Esto facilita identificar qué es código nuevo de EmulAItor vs código original de Lemuroid.

### ¿Cómo funciona?

Android Gradle fusiona múltiples source sets en tiempo de compilación. EmulAItor configura:

```kotlin
// En build.gradle.kts
sourceSets {
    getByName("main") {
        java.srcDirs("src/main/java", "src/emulaitor/java")
        res.srcDirs("src/main/res", "src/emulaitor/res")
    }
}
```

Esto significa que **ambos directorios se combinan** en el build final:

```text
lemuroid-app/src/
├── main/                    ← LEMUROID + MODIFICACIONES
│   └── java/
│       └── com/swordfish/lemuroid/
│           ├── app/                    # Código de Lemuroid (algunas clases modificadas)
│           └── lib/                    # Librerías compartidas (algunas modificadas)
│
├── emulaitor/               ← CÓDIGO 100% NUEVO DE EMULAITOR
│   └── java/
│       └── com/swordfish/lemuroid/
│           ├── app/mobile/feature/catalog/     # Catálogo Archive.org, SMB, descargas
│           ├── app/mobile/feature/metadata/    # Proveedor TheGamesDB
│           ├── app/mobile/feature/disclaimer/  # Aviso legal
│           └── app/shared/bugreport/           # Reporte de errores
│
└── res/
    ├── main/                # Recursos (algunos personalizados)
    └── emulaitor/           # Recursos exclusivos EmulAItor
```

### Reglas del Sistema

| Tipo de código | Ubicación | Descripción |
|----------------|-----------|-------------|
| **Código NUEVO** | `src/emulaitor/` | Funciones que NO existen en Lemuroid (catálogo, SMB, etc.) |
| **Código MODIFICADO** | `src/main/` | Archivos de Lemuroid con cambios de EmulAItor |
| **Código ORIGINAL** | `src/main/` | Archivos de Lemuroid sin modificar |

### Archivos Nuevos en `src/emulaitor/` (14 archivos)

| Directorio | Archivos | Propósito |
|------------|----------|-----------|
| `catalog/` | 10 archivos | Catálogo Archive.org, SMB, descargas |
| `metadata/` | 2 archivos | Proveedor TheGamesDB |
| `disclaimer/` | 1 archivo | Pantalla de aviso legal |
| `bugreport/` | 1 archivo | Sistema de reporte de errores |

### Archivos Modificados en `src/main/` (vs Lemuroid original)

| Archivo | Motivo de la Modificación |
|---------|---------------------------|
| `lib/library/SystemID.kt` | Añade `UNKNOWN` para ROMs no identificadas |
| `lib/library/GameSystem.kt` | Define entrada para sistema `UNKNOWN` |
| `lib/core/CoresSelection.kt` | Fix defensivo para listas vacías de cores |
| `app/mobile/feature/main/MainActivity.kt` | Navegación con pestaña Catálogo |
| `app/mobile/feature/settings/about/AboutScreen.kt` | Branding EmulAItor |

### Cómo Actualizar Lemuroid

1. **Comparar** archivos de [`_upstream_lemuroid/`](file:///_upstream_lemuroid/) con `src/main/`
2. **Actualizar** archivos NO modificados directamente
3. **Merge manual** para archivos modificados (ver lista arriba)
4. **Compilar** y probar: `./gradlew :lemuroid-app:assemblePlayBundleDebug`

> **Nota:** Los archivos en `src/emulaitor/` NUNCA necesitan merge - son 100% código EmulAItor.

---

## 🎮 Sistemas Soportados

| Sistema | Core (Motor) |
| :--- | :--- |
| **Nintendo** | NES, SNES, N64, GB, GBC, GBA, DS, 3DS |
| **Sega** | Master System, Genesis, CD, Game Gear |
| **Sony** | PlayStation (PSX), PSP |
| **Arcade** | FinalBurn Neo |
| **Atari** | 2600, 7800, Lynx |
| **Otros** | Neo Geo Pocket, WonderSwan, PC Engine |

---

## 🛠️ Cómo Compilar (Build)

Hemos simplificado el proceso de construcción con scripts automatizados.

### Requisitos Previos
*   Android Studio Ladybug (o superior)
*   JDK 17 (Recomendado: JetBrains Runtime incluido en Android Studio)

### Método Recomendado (Automático)

Ejecuta el script `build_creator.bat` en la raíz del proyecto y sigue el menú:

1.  **Opción 1:** Preparar código fuente limpio (para publicar/compartir).
2.  **Opción 2:** Generar **AAB** (Android App Bundle) para subir a Google Play.
3.  **Opción 3:** Generar **APK** instalable para pruebas locales (Release + Logs).
4.  **Opción 4:** Generar **APK** de desarrollo rápido (Debug Fast Build).
5.  **Opción 6 (NUEVO):** Instalar **Split APKs** en dispositivo (Simulación Play Store). **OBLIGATORIO** antes de subir a producción.

Los archivos generados se guardarán automáticamente en la carpeta:
`\BUILDS_preparadas\`

### Método Manual (Gradle)

```powershell
# Configurar entorno (ajustar ruta según instalación)
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio1\jbr"

# Generar APK de depuración
.\gradlew.bat :lemuroid-app:assembleFreeBundleDebug

# Generar AAB de producción (requiere claves en local.properties)
.\gradlew.bat :lemuroid-app:bundlePlayBundleRelease
```

---

## 📄 Licencia

Este proyecto se distribuye bajo la licencia **GNU General Public License v3.0 (GPLv3)**.

*   EmulAItor Copyright (C) 2024
*   Basado en Lemuroid Copyright (C) Filippo Scognamiglio (Swordfish90)
*   Los núcleos de Libretro tienen sus propias licencias individuales.

> **Importante:** EmulAItor no incluye juegos ni archivos de BIOS protegidos por derechos de autor. Los usuarios son responsables de proporcionar sus propios archivos legalmente adquiridos.
