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

## 🏗️ Estructura del Proyecto (Fork)

A continuación se detallan los módulos y archivos nuevos que componen las mejoras de EmulAItor:

```text
lemuroid-app/src/main/java/com/swordfish/lemuroid/app/mobile/feature/
├── catalog/
│   ├── ArchiveOrgClient.kt      # Cliente API REST de Archive.org
│   ├── CatalogViewModel.kt      # Lógica de búsqueda y filtrado
│   ├── CatalogScreen.kt         # UI Compose del catálogo online
│   ├── RomDownloader.kt         # Gestor de descargas con notificaciones
│   ├── SourceManager.kt         # Orquestador de fuentes (Local vs SMB)
│   ├── SmbClient.kt             # Cliente SMB (JCIFS-NG) para NAS
│   └── RomMetadataExtractor.kt  # Identificación inteligente por nombre/región
├── disclaimer/
│   └── DisclaimerScreen.kt      # Aviso legal obligatorio (Google Play)
└── main/
    └── GameEditDialog.kt        # Editor manual de metadatos de juegos
```

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
3.  **Opción 3:** Generar **APK** instalable para pruebas locales.

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

*   EmulAItor TM
*   Basado en Lemuroid Copyright (C) Filippo Scognamiglio (Swordfish90)
*   Los núcleos de Libretro tienen sus propias licencias individuales.

> **Importante:** EmulAItor no incluye juegos ni archivos de BIOS protegidos por derechos de autor. Los usuarios son responsables de proporcionar sus propios archivos legalmente adquiridos.
