# EmulAItor

Fork de [Lemuroid](https://github.com/Swordfish90/Lemuroid) con mejoras adicionales para descarga y gestión de ROMs.

## Descripción

EmulAItor es un emulador Android de código abierto basado en Libretro. Extiende Lemuroid añadiendo funcionalidades avanzadas como exploración de Archive.org, soporte SMB/NAS y gestión inteligente de ROMs.

---

## 🆕 Funcionalidades Añadidas por EmulAItor

### ✅ Pantalla de Disclaimer Legal (Obligatorio)
- Pantalla de aviso legal que aparece al primer inicio
- Textos traducidos automáticamente según idioma del sistema
- Cumple requisitos de Google Play

### ✅ Catálogo Archive.org
Sistema integrado para explorar y descargar ROMs desde Internet Archive.

**Funcionalidades:**
- Búsqueda de paquetes de ROMs por sistema (SNES, NES, GBA, Genesis, N64, PSX, PSP, Arcade)
- Filtro por 11 regiones/idiomas (USA, EUR, JPN, ESP, FRA, GER, ITA, BRA, KOR, CHN, AUS)
- Ordenación (más descargados, nombre, tamaño)
- Paginación infinita
- Descargas múltiples simultáneas con progreso en tiempo real
- Re-escaneo automático de biblioteca tras cada descarga
- Detección de archivos ya descargados (evita duplicados)
- Panel de descargas con cancelación y limpieza

### ✅ Fuentes Locales y SMB/NAS
Sistema para escanear ROMs desde carpetas locales y recursos de red SMB.

**Funcionalidades:**
- Añadir carpetas locales como fuentes de ROMs
- Conectar a servidores SMB/NAS (con credenciales opcionales)
- Búsqueda recursiva hasta 10 niveles de profundidad
- Detección inteligente de metadatos:
  - Sistema/consola por nombre de carpeta
  - Región por nombre de archivo (USA, Europe, Japan, Spain...)
  - Limpieza automática del nombre del juego
  - Emoji de bandera según región
- Descarga de ROMs desde SMB a biblioteca local
- Re-escaneo automático después de cada descarga
### ✅ Compatibilidad Android TV (SAF/LocalStorage Fallback)
Sistema automático de fallback para almacenamiento en dispositivos sin soporte SAF.

**Funcionamiento:**
- Detecta automáticamente si el dispositivo soporta SAF (Storage Access Framework)
- **Dispositivos con SAF** (móviles/tablets): Usa selector de documentos estándar
- **Dispositivos sin SAF** (Android TV con Scoped Storage):
    - Requiere permiso `MANAGE_EXTERNAL_STORAGE` (Todas las carpetas)
    - Usa selector de carpetas legacy optimizado
    - Detección automática del sistema por extensión si no hay metadatos
- Las descargas funcionan en ambos modos automáticamente
- El escaneo de biblioteca funciona con ambos sistemas

**Archivos clave:**
- `TVHelper.isSAFSupported()` - Detecta soporte SAF
- `RomDownloader.isSAFMode()` - Determina modo de descarga
- `LocalStorageProvider` - Provider para almacenamiento local
- `CompositeMetadataProvider` - Lógica de fallback de identificación

---

### ✅ Editor de Metadatos de Juegos
Permite corregir manualmente la información de ROMs mal identificadas.

**Funcionalidades:**
- Opción "Edit" en el menú contextual de cada juego
- Editar: Título, Sistema/Consola, Desarrollador
- Cambios se guardan inmediatamente en la base de datos

### ✅ About/Ayuda Actualizado
El diálogo de ayuda ahora incluye:
- Información sobre EmulAItor como fork de Lemuroid
- Aviso legal sobre el contenido de Archive.org
- Instrucciones de uso
- Información de licencias (GPL-3.0)

---

## 📁 Estructura de Archivos Nuevos

```
lemuroid-app/src/main/java/com/swordfish/lemuroid/app/mobile/feature/
├── catalog/
│   ├── ArchiveOrgClient.kt      # Cliente API de Archive.org
│   ├── CatalogViewModel.kt      # ViewModel del catálogo
│   ├── CatalogScreen.kt         # UI Compose del catálogo
│   ├── RomDownloader.kt         # Gestor de descargas
│   ├── SourceManager.kt         # Gestión de fuentes (local/SMB)
│   ├── SourceDialogs.kt         # Diálogos de configuración de fuentes
│   ├── SmbClient.kt             # Cliente SMB para escaneo de NAS
│   ├── LocalFolderScanner.kt    # Escaneo de carpetas locales
│   └── RomMetadataExtractor.kt  # Extracción inteligente de metadatos
├── disclaimer/
│   └── DisclaimerScreen.kt      # Pantalla de aviso legal
└── main/
    └── GameEditDialog.kt        # Diálogo de edición de juegos
```

---

## 🌍 Sistema de Traducciones (Multi-idioma)

EmulAItor hereda el sistema de traducción de Lemuroid. Los textos están en archivos de recursos XML.

### Ubicación de archivos
```
lemuroid-app/src/main/res/
├── values/
│   └── strings.xml              # Inglés (por defecto)
├── values-es-rES/
│   └── strings.xml              # Español (España)
├── values-fr-rFR/
│   └── strings.xml              # Francés
├── values-de-rDE/
│   └── strings.xml              # Alemán
└── ... (31 idiomas soportados)
```

### Añadir nuevo texto traducible

1. **Añadir al inglés** (`values/strings.xml`):
```xml
<string name="mi_texto_nuevo">My new text</string>
```

2. **Añadir traducción** (`values-es-rES/strings.xml`):
```xml
<string name="mi_texto_nuevo">Mi texto nuevo</string>
```

3. **Usar en código Kotlin/Compose**:
```kotlin
import androidx.compose.ui.res.stringResource
import com.swordfish.lemuroid.R

Text(text = stringResource(R.string.mi_texto_nuevo))
```

### Strings añadidos por EmulAItor
- `disclaimer_title`, `disclaimer_software_origin_title/text`
- `disclaimer_no_content_title/text`, `disclaimer_archive_title/text`
- `disclaimer_user_responsibility_title/text`, `disclaimer_accept`
- `lemuroid_help_content` (actualizado con About de EmulAItor)

---

## 🛠️ Desarrollo

### Emulador para pruebas
- **AVD:** `Pixel_Tablet_API_35` (usar siempre este para tests)
- **Lanzar:** `$emulator -avd Pixel_Tablet_API_35`

⚠️ **IMPORTANTE:** NUNCA hardcodees URLs basándose en la estructura de archivos del emulador.

### Compilar
```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat :lemuroid-app:assembleFreeBundleDebug
```

### Instalar
```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb install -r "lemuroid-app\build\outputs\apk\freeBundle\debug\lemuroid-app-free-bundle-debug.apk"
```

### Backup y Registro de Cambios
```powershell
python registro_y_backup.py changelog.md "Título" "Descripción" --changes_list "Cambio 1" "Cambio 2"
```

---

## 🔄 Tareas Pendientes

- [x] ~~Rescraping automático después de editar metadatos~~ (descartado - el scraping usa CRC/nombre del archivo, no el título editado)
- [x] ~~Eliminación de juegos desde menú contextual (con confirmación)~~
- [x] ~~Eliminación masiva con FAB papelera y multiselección en HomeScreen~~
- [x] ~~Integrar imágenes de branding (icono, biblioteca.jpg, banner.jpg)~~
- [x] ~~Aplicar biblioteca.jpg como fondo con degradado oscuro en pantalla principal~~
- [x] ~~Refactorizar CatalogViewModel para unificar fuentes~~ (innecesario - CatalogScreen ya integra SourceManager, LocalFolderScanner y SmbClient)
- [x] ~~Branding completo (renombrar app y package a EmulAItor)~~ (nombre visible cambiado, package sin modificar por seguridad)
- [x] ~~Internacionalizar Disclaimer, GameEdit, SourceDialogs (español/inglés)~~
- [x] ~~Internacionalizar CatalogScreen (español/inglés)~~

---

## 📜 Sistemas Soportados

| Sistema | Core Libretro |
|---------|---------------|
| Atari 2600 | stella |
| Atari 7800 | prosystem |
| Atari Lynx | handy |
| Nintendo (NES) | fceumm |
| Super Nintendo (SNES) | snes9x |
| Game Boy | gambatte |
| Game Boy Color | gambatte |
| Game Boy Advance | mgba |
| Sega Genesis | genesis_plus_gx |
| Sega CD | genesis_plus_gx |
| Sega Master System | genesis_plus_gx |
| Sega Game Gear | genesis_plus_gx |
| Nintendo 64 | mupen64plus |
| PlayStation | PCSX-ReARMed |
| PlayStation Portable | ppsspp |
| FinalBurn Neo (Arcade) | fbneo |
| Nintendo DS | desmume/melonds |
| NEC PC Engine | beetle_pce_fast |
| Neo Geo Pocket | mednafen_ngp |
| WonderSwan | beetle_cygne |
| Nintendo 3DS | citra |

---

## 📄 Licencias

- **Lemuroid:** GPL-3.0 (Swordfish90)
- **EmulAItor (este fork):** GPL-3.0
- **LibretroDroid:** GPL-3.0
- **Libretro Cores:** Varias licencias

---

## 🔗 Enlaces

- [Lemuroid Original](https://github.com/Swordfish90/Lemuroid)
- [Crowdin (Traducciones)](https://crowdin.com/project/lemuroid)
- [Archive.org](https://archive.org)
