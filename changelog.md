****************************************************************************************************
17/12/2025 17:34 - V8.9: SMB Deletion Support - Beta_v009
****************************************************************************************************
- Description:
  Implementation of ROM deletion for SMB shares, ensuring functionality on both TV and Mobile interfaces.

- Changes:
  - Implemented delete() in StorageProvider for SMB, Local, and SAF
  - Added deleteFile() to SmbClient
  - Centralized deletion logic in LemuroidLibrary
  - Refactored GameInteractor to support remote file deletion
  - Fixed compilation errors in DI modules

****************************************************************************************************
17/12/2025 13:26 - V8.8: Fix SMB Library Mobile + UI Improvements - Beta_v009
****************************************************************************************************
- Description:
  Corrección del sistema de biblioteca SMB para móvil y mejoras estéticas.

- Changes:
  [FIX] V8.8: Mobile ahora guarda KEY_SMB_LIBRARY_SHARE correctamente (alineado con TV)
  [FIX] Parseo de path SMB para extraer share y subpath en SettingsScreen.kt
  [UI] Cambio de 'Cloud/Nube' a 'Archive.org' en filtros del catálogo para transparencia Google Play
  [FIX] V8.7: Regeneración de coverFrontUrl para juegos existentes durante rescan
  [FIX] V8.6: Post-procesamiento de thumbnails automático si metadata existe pero thumbnail=null

****************************************************************************************************
17/12/2025 12:55 - V8.8: Fix SMB Library Mobile + UI Improvements - Beta_v009
****************************************************************************************************
- Description:
  Corrección del sistema de biblioteca SMB para móvil y mejoras estéticas.

- Changes:
  [FIX] V8.8: Mobile ahora guarda KEY_SMB_LIBRARY_SHARE correctamente (alineado con TV)
  [FIX] Parseo de path SMB para extraer share y subpath en SettingsScreen.kt
  [UI] Cambio de 'Cloud/Nube' a 'Archive.org' en filtros del catálogo para transparencia Google Play
  [FIX] V8.7: Regeneración de coverFrontUrl para juegos existentes durante rescan
  [FIX] V8.6: Post-procesamiento de thumbnails automático si metadata existe pero thumbnail=null

****************************************************************************************************
17/12/2025 05:48 - V8.6-V8.7: Fix Thumbnails y Metadata - Beta_v009
****************************************************************************************************
- Description:
  Corrección completa del sistema de thumbnails y metadata para ROMs cloud/SMB.

- Changes:
  [FIX] V8.6: Post-procesamiento de thumbnails - genera URL automáticamente si metadata existe pero thumbnail=null
  [FIX] V8.7: Regeneración de coverFrontUrl para juegos existentes durante rescan de biblioteca
  [FIX] TheGamesDB API key hardcodeada por defecto (fallback si usuario no configura)
  [FIX] Thumbnail fallback por nombre en LibretroDBMetadataProvider cuando CRC no disponible
  [FIX] V8.5 SMB library cache para checkmarks de archivos descargados

****************************************************************************************************
17/12/2025 05:11 - V8.3-V8.5 - Fix Descargas SMB Crítico - Beta_v009
****************************************************************************************************
- Description:
  Corrección crítica de descargas SMB que fallaban con JobCancellationException. RomDownloader ahora es Singleton con SmbClient interno y cache SMB para verificación de archivos existentes.

- Changes:
  [FIX] V8.3: RomDownloader promovido a Singleton (@PerApp)
  [FIX] V8.4: SmbClient ahora es interno a RomDownloader (no desde ViewModel)
  [FIX] V8.5: Cache de biblioteca SMB para verificación de archivos existentes
  [FIX] Resuelto JobCancellationException en uploads SMB grandes

****************************************************************************************************
17/12/2025 03:38 - V8.2 - Fix SMB Downloads & UI Ghosting - Beta_v022
****************************************************************************************************
- Description:
  Se ha implementado una arquitectura centralizada para RomDownloader y se ha solucionado el problema de 'Cerebro Dividido' que causaba que la UI no reconociera las descargas SMB.

- Changes:
  - Solucionado bug visual (Ghost Checks) en pestaña SMB.
  - Solucionado bug de destino (Descargas caían en /Documents en vez de NAS).
  - Unificada instancia RomDownloader en ViewModel y UI.

****************************************************************************************************
16/12/2025 10:09 - beta - Beta_v021
****************************************************************************************************
- Description:
  Nueva versión

- Changes:
  reparación del catalogo para el modo tv haciendo que sea navegable

****************************************************************************************************
16/12/2025 09:41 - v020 - Beta_v021
****************************************************************************************************
- Description:
  Nueva versión

- Changes:
  reparación del catalogo para el modo tv haciendo que sea navegable

****************************************************************************************************
16/12/2025 09:39 - v020 - Beta_v020
****************************************************************************************************
- Description:
  Nueva versión

- Changes:
  Cambios pendientes de especificar

****************************************************************************************************
15/12/2025 18:58 - v019 - Beta_v019
****************************************************************************************************
- Description:
  Fix cursor atrapado en buscador de Catálogo TV

- Changes:
  D-Pad ABAJO ahora sale del buscador
  Añadido onPreviewKeyEvent a OutlinedTextField

****************************************************************************************************
15/12/2025 16:41 - Fix Crítico TV: Permisos y Visibilidad ROMs - Beta_v018
****************************************************************************************************
- Description:
  Solución definitiva para la visibilidad de ROMs en Android TV (Scoped Storage). Implementación de MANAGE_EXTERNAL_STORAGE y fallback de identificación por extensión para juegos sin metadatos.

- Changes:
  - [FIX] Restaurada visibilidad de ROMs antiguas en Android 11+ (TV).
  - [FIX] Implementado permiso MANAGE_EXTERNAL_STORAGE en flujo de TV.
  - [FIX] Añadido fallback: Si falla metadata, identifica sistema por extensión (.sfc, .nes, etc).
  - [FIX] Corregidos crashes por intents de configuración no encontrados.

****************************************************************************************************
15/12/2025 14:06 - Fix Crítico TV: Permisos y Visibilidad ROMs - Beta_v017
****************************************************************************************************
- Description:
  Solución definitiva para la visibilidad de ROMs en Android TV (Scoped Storage). Implementación de MANAGE_EXTERNAL_STORAGE y fallback de identificación por extensión para juegos sin metadatos.

- Changes:
  - [FIX] Restaurada visibilidad de ROMs antiguas en Android 11+ (TV).
  - [FIX] Implementado permiso MANAGE_EXTERNAL_STORAGE en flujo de TV.
  - [FIX] Añadido fallback: Si falla metadata, identifica sistema por extensión (.sfc, .nes, etc).
  - [FIX] Corregidos crashes por intents de configuración no encontrados.

****************************************************************************************************
14/12/2025 16:43 - SAF Persistence & Library Fix - Beta_v016
****************************************************************************************************
- Description:
  Corrección crítica de persistencia SAF en móviles/tablets y visualización de biblioteca.

- Changes:
  Unificada persistencia a Harmony SharedPreferences (MainActivity y StorageFrameworkPickerLauncher)
  Añadido permiso de Escritura (FLAG_GRANT_WRITE_URI_PERMISSION) para SAF
  Corregido StorageAccessFrameworkProvider para leer la URI correcta (soluciona biblioteca vacía)
  Validado funcionamiento completo de descargas y borrado en modo SAF

****************************************************************************************************
13/12/2025 12:48 - Implementación TheGamesDB y Smart SMB - Beta_v015
****************************************************************************************************
- Description:
  Sustitución de ScreenScraper y mejoras en descargas

- Changes:
  Sustituido ScreenScraper (User/Pass) por TheGamesDB (API Key BYOK)
  Nueva sección de configuración de Metadatos con clave API
  Implementada Organización Inteligente para descargas SMB (Carpeta Temporal -> Escaneo -> Destino)
  Corregido color de texto en barra de búsqueda del Catálogo
  Automated Versioning: APK version now syncs with Changelog

****************************************************************************************************
13/12/2025 12:44 - Implementación TheGamesDB y Smart SMB - Beta_v014
****************************************************************************************************
- Description:
  Sustitución de ScreenScraper y mejoras en descargas

- Changes:
  Sustituido ScreenScraper (User/Pass) por TheGamesDB (API Key BYOK)
  Nueva sección de configuración de Metadatos con clave API
  Implementada Organización Inteligente para descargas SMB (Carpeta Temporal -> Escaneo -> Destino)
  Corregido color de texto en barra de búsqueda del Catálogo

****************************************************************************************************
13/12/2025 02:07 - Smart SMB Organization & Deletion Fixes - Beta_v013
****************************************************************************************************
- Description:
  Implemented intelligent SMB downloading (Temp->Scan->Move) using GameMetadataProvider to ensure ROMs are placed in correct system subfolders. Fixed persistent 'Ghost Games' issue by enforcing physical file deletion via SAF in GameInteractor. Resolved compilation issues in CatalogScreen and RomDownloader.

****************************************************************************************************
12/12/2025 17:32 - Carrusel Premium y Audio Finalizado - Beta_v012
****************************************************************************************************
- Description:
  Implementación completa de carrusel 3D Coverflow, 3 vistas (Carrusel, Lista, Grid) y sistema de audio ambiente.

- Changes:
  Carrusel 3D Coverflow ajustado y centrado
  3 modos de vista implementados
  Audio ambiente con intro y normalizado (22kHz, sin fadeout inicial)
  Correcciones visuales en lista (colores de texto)

****************************************************************************************************
12/12/2025 15:03 - Carrusel Premium y Audio - Beta_v011
****************************************************************************************************
- Description:
  Implementación de carrusel 3D, 3 modos de vista, sistema de música con intro

- Changes:
  Carrusel 3D Coverflow
  3 modos vista: Carrusel/Grid/Lista
  Música auto-start con intro
  Audio normalizado 22kHz

****************************************************************************************************
12/12/2025 12:06 - Session 12-Dic-2024 - Beta_v010
****************************************************************************************************
- Description:
  Icono y eliminación masiva

- Changes:
  Icono EmulAItor (logo_simple.png) con adaptive-icon correcto
  Background con degradado oscuro (biblioteca.jpg)
  FAB papelera en HomeScreen para modo selección múltiple
  Checkboxes en juegos para multiselección
  Diálogo confirmación eliminación masiva
  Eliminación individual desde menú contextual
  Strings eliminación EN/ES

****************************************************************************************************
12/12/2025 11:21 - Session 12-Dic-2024 - Alfa_v009
****************************************************************************************************
- Description:
  Icono y eliminación masiva

- Changes:
  Icono EmulAItor (logo_simple.png) con adaptive-icon correcto
  Background con degradado oscuro (biblioteca.jpg)
  FAB papelera en HomeScreen para modo selección múltiple
  Checkboxes en juegos para multiselección
  Diálogo confirmación eliminación masiva
  Eliminación individual desde menú contextual
  Strings eliminación EN/ES

****************************************************************************************************
12/12/2025 11:02 - Session 11-Dic-2024 - Beta_v008
****************************************************************************************************
- Description:
  Internacionalización completa y branding inicial

- Changes:
  Internacionalización español/inglés completa (Disclaimer, GameEdit, SourceDialogs, CatalogScreen)
  Integrado icono EmulAItor (Logo.png en 5 densidades)
  Copiados biblioteca.jpg y banner.jpg a drawable
  AndroidManifest actualizado con nuevo icono

****************************************************************************************************
11/12/2025 21:28 - Session 11-Dic-2024 - Beta_v007
****************************************************************************************************
- Description:
  Internacionalización completa y branding inicial

- Changes:
  Internacionalización español/inglés completa (Disclaimer, GameEdit, SourceDialogs, CatalogScreen)
  Integrado icono EmulAItor (Logo.png en 5 densidades)
  Copiados biblioteca.jpg y banner.jpg a drawable
  AndroidManifest actualizado con nuevo icono

****************************************************************************************************
11/12/2025 18:31 - SMB/Local Sources Integration - Beta_v006
****************************************************************************************************
- Description:
  Implementación completa de fuentes SMB y locales con descarga y rescan automático

- Changes:
  Formulario SMB simplificado (eliminado Share Name redundante)
  Detección inteligente de ROMs (sistema por carpeta, región por nombre)
  Búsqueda recursiva hasta 10 niveles
  Descarga de ROMs desde SMB a biblioteca local
  Rescan automático después de descarga
  Iconos según estado (descargando/descargado/disponible)
  Edición funcional de fuentes SMB

****************************************************************************************************
11/12/2025 13:55 - Fix Libretro Core Loading - Beta_v005
****************************************************************************************************
- Description:
  Corregido problema de carga de cores de libretro en Windows

- Changes:
  Eliminados symlinks incompatibles con Windows en bundled-cores
  Configurado jniLibs.srcDirs para cargar cores desde directorios originales
  APK ahora incluye todos los cores de libretro (220MB)
  Los juegos ahora se ejecutan correctamente

****************************************************************************************************
10/12/2025 21:49 - Catálogo Archive.org - Beta_v004
****************************************************************************************************
- Description:
  Integración completa del catálogo de Archive.org para buscar y descargar ROMs

- Changes:
  Añadido cliente API Archive.org (ArchiveOrgClient.kt)
  Añadido sistema de descargas múltiples (RomDownloader.kt)
  Añadida UI del catálogo con Compose (CatalogScreen.kt)
  Filtros por sistema, región e idioma
  Ordenación por descargas, nombre y tamaño
  Paginación infinita
  Re-escaneo automático tras descarga
  Detección de archivos ya descargados

****************************************************************************************************
10/12/2025 13:10 - SMB y Biblioteca Externa - Beta_v003
****************************************************************************************************
- Description:
  Implementado soporte para importar bibliotecas de ROMs desde rutas externas incluyendo SMB. Escaneo en background con smbj. Corregido core N64 a mupen64plus_next_gles3.

- Changes:
  - Importar bibliotecas externas (local, SAF, SMB)
  - Dialogo credenciales SMB (usuario/contraseña)
  - Escaneo SMB con biblioteca smbj
  - Escaneo en corrutina IO para no bloquear UI
  - Core N64 corregido a mupen64plus_next_gles3
  - Soporte rutas manuales y selector carpetas

****************************************************************************************************
09/12/2025 21:44 - SMB y Biblioteca Externa - Beta_v002
****************************************************************************************************
- Description:
  Implementado soporte para importar bibliotecas de ROMs desde rutas externas incluyendo SMB. Escaneo en background con smbj. Corregido core N64 a mupen64plus_next_gles3.

- Changes:
  - Importar bibliotecas externas (local, SAF, SMB)
  - Dialogo credenciales SMB (usuario/contraseña)
  - Escaneo SMB con biblioteca smbj
  - Escaneo en corrutina IO para no bloquear UI
  - Core N64 corregido a mupen64plus_next_gles3
  - Soporte rutas manuales y selector carpetas

****************************************************************************************************
09/12/2025 20:45 - RetroArch Lanzamiento Directo - Alfa_v001
****************************************************************************************************
- Description:
  Implementado lanzamiento directo de juegos via RetroArch usando Intent con extras ROM y LIBRETRO. El path del core se obtiene dinamicamente via packageManager.getPackageInfo().applicationInfo.dataDir para compatibilidad con cualquier dispositivo Android.

- Changes:
  - Lanzamiento directo de juegos desde biblioteca a RetroArch
  - Path de cores dinamico via packageInfo.dataDir
  - Documentacion actualizada con solucion correcta
  - Intent con ROM y LIBRETRO extras funcional

