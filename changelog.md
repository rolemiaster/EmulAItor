****************************************************************************************************
01/06/2026 12:46 - Monetization and Play Billing v7 - v305
****************************************************************************************************
- Description:
  Migración a Play Billing 7.1.1, implementación del modelo de monetización y mejoras en la UI.

- Changes:
  Actualizado Google Play Billing Library a v7.1.1
  Implementado sistema de monetización (split app free/play)
  Traducción automatizada de listados Play Console
  Paginación en Home para carrusel y listas
  Fixes de permisos Legacy en Android 8
  Integración de correcciones del catálogo

****************************************************************************************************
23/05/2026 20:13 - Mejora de carpeta ROM y actualizaciones nativas - v304
****************************************************************************************************
- Description:
  Se mejora el diagnostico cuando Android no permite acceder a la carpeta de ROMs seleccionada y se simplifica el aviso de actualizacion para usar el mecanismo nativo de Google Play en movil, dejando Android TV a las actualizaciones normales de Play Store.

- Changes:
  - Mensaje claro cuando la carpeta de ROMs configurada no existe o Android bloquea el acceso.
  - Fallback en Android TV para mostrar el selector de carpetas nativo cuando la traduccion del sistema no expone bien el boton.
  - Aviso de nueva version en movil mediante Play Core App Update, sin version.json ni puente web externo.
  - Android TV queda sin detector propio de actualizaciones; Google Play Store gestiona la actualizacion.

****************************************************************************************************
23/05/2026 05:54 - final - v292
****************************************************************************************************
- Description:
  Preparacion de la siguiente version con mejoras de estabilidad, UX y QA de cores.

- Changes:
  - Corrige fallos en Catalog al guardar permisos de carpeta local
  - Corrige restauracion del menu de juego en Android TV
  - Mitiga bloqueo al abrir el menu in-game en sistemas sin multidisco
  - Evita descargas innecesarias de cores ya incluidos localmente
  - Mueve metadatos TheGamesDB a segundo plano para no bloquear la biblioteca
  - Anade aviso de actualizacion de metadatos en segundo plano
  - Anade interruptor HD en Home para movil y TV
  - Mejora visibilidad del estado HD en movil
  - Centra y estabiliza el carrusel movil
  - Valida PPSSPP con ISO PSP real en dispositivo ARM fisico

- Known Issues:
  - Homebrew PSP loose PBP multiarchivo queda observado como caso separado
  - PCSX ReARMed fdsan requiere reproduccion especifica antes de tocar VFS/native

****************************************************************************************************
22/05/2026 17:12 - Soporte y UX de reportes de usuarios - v291
****************************************************************************************************
- Description:
  Se corrige el aviso de borrado para reflejar que eliminar un juego borra biblioteca y ROM, se mejora la visualización de nombres largos, se añade ayuda visible para BIOS y se documenta el procedimiento de soporte para reportes de usuarios.

- Changes:
  Aviso de borrado actualizado en inglés y castellano
  Nombres de juegos visibles hasta dos líneas en UI móvil
  Ayuda de BIOS añadida en la pantalla de BIOS
  Procedimiento de soporte y memorias actualizadas para reportes de cores, 16KB y borrado

****************************************************************************************************
19/05/2026 13:35 - Mitigación ANR ForegroundService WorkManager - v290
****************************************************************************************************
- Description:
  Se mitiga el fallo de producción ForegroundServiceDidNotStartInTimeException asociado al servicio foreground interno de WorkManager actualizando WorkManager a 2.11.2 y verificando compilación, resolución de dependencia, instalación y prueba en emulador Android 15.

- Changes:
  - Actualizado WorkManager de 2.10.0 a 2.11.2
  - Verificada compilación Kotlin playBundleDebug
  - Verificado assemblePlayBundleDebug e instalación en emulador
  - Revisado logcat sin reproducción de ForegroundServiceDidNotStartInTimeException, RemoteServiceException, FATAL EXCEPTION ni ANR

- Known Issues:
  - En instalación local debug, PlayCore puede devolver onError(-15) al probar dynamic feature cores; validar splits mediante el flujo canónico antes de release
  - Persisten avisos StrictMode de I/O en hilo principal como deuda de rendimiento no bloqueante

****************************************************************************************************
05/05/2026 18:26 - Versión final estable - v289
****************************************************************************************************
- Description:
  Salto de beta a versión final, con revisión completa de estabilidad y funcionamiento, mejoras internas de compatibilidad y fiabilidad general.

- Changes:
  Paso de beta a versión final estable
  Mejoras generales de estabilidad y rendimiento
  Mejor compatibilidad con sistemas, mandos y bibliotecas de ROMs
  Ajustes internos para reducir errores inesperados y mejorar el arranque

****************************************************************************************************
05/05/2026 18:25 - Revisión de estabilidad - v288
****************************************************************************************************
- Description:
  Revisión completa de estabilidad y funcionamiento, con mejoras internas de compatibilidad y fiabilidad general.

- Changes:
  Mejoras generales de estabilidad y rendimiento
  Mejor compatibilidad con sistemas, mandos y bibliotecas de ROMs
  Ajustes internos para reducir errores inesperados y mejorar el arranque

****************************************************************************************************
05/05/2026 17:22 - Protección LVL - Beta_v287
****************************************************************************************************
- Description:
  Protección LVL (Play release), localización de mensajes y fix de CoreUpdateWork.

- Changes:
  LVL Play release con caché y timeout
  Mensajes localizados multi idioma
  CoreUpdateWork ignora sistemas sin cores

****************************************************************************************************
05/05/2026 15:55 - Fix música ambiental en segundo plano y foreground workers - Beta_v286
****************************************************************************************************
- Description:
  Se corrige la música ambiental del Home para que se pause al mandar la app a segundo plano sin alterar el silencio manual del usuario. Se endurece el manejo de foreground workers para que SaveSyncWork, LibraryIndexWork y CoreUpdateWork no continúen trabajo pesado si setForeground falla, devolviendo retry controlado.

- Changes:
  La música ambiental del Home se pausa temporalmente en onPause y se reanuda solo si el usuario no la silenció manualmente
  SaveSyncWork no ejecuta sincronización si no consigue iniciar foreground correctamente
  LibraryIndexWork no indexa biblioteca si no consigue iniciar foreground correctamente
  CoreUpdateWork no continúa actualización/instalación de cores si no consigue iniciar foreground correctamente
  Compilación verificada con :lemuroid-app:compilePlayBundleDebugKotlin

- Known Issues:
  El ANR ForegroundServiceDidNotStartInTimeException de Play Console no fue reproducido localmente; el cambio reduce riesgo pero no confirma resolución definitiva

****************************************************************************************************
02/05/2026 13:16 - Evaluación mejoras Lemuroid 1.17.0 y auditoría autosave - Beta_v285
****************************************************************************************************
- Description:
  Se evaluó la factibilidad de incorporar mejoras de Lemuroid 1.17.0. Se confirmó que touch controls, immersive mode, MelonDS microphone, quicksave/quickload shortcuts y SDK 35 ya están integrados. Se auditó flujo de guardado local vs Google Drive sync, concluyendo que autosave robusto upstream no sustituye a nuestro sync. Se decide no tocar la APK en esta fase. Se actualizó documentación de 16KB ELF.

- Changes:
  Evaluadas mejoras Lemuroid 1.17.0: ya integradas touch controls, immersive mode, MelonDS mic, quicksave shortcuts
  Auditado flujo autosave local vs Google Drive sync
  Decisión: no tocar APK, priorizar pruebas de cierre desde recientes
  Actualizada documentación 16KB ELF (readme, reglas, GEMINI, justificación Google Play)
  Actualizadas memorias del proyecto

- Known Issues:
  Posible brecha: autosave no captura al cerrar desde recientes sin pasar por requestFinish()
  Upstream autosave robusto requiere reescritura parcial de GameService: riesgo alto

****************************************************************************************************
02/05/2026 11:48 - Fix ANR/crash y orden del catálogo - Beta_v283
****************************************************************************************************
- Description:
  Correcciones de estabilidad tras revisar reportes de Google Play Console: foreground workers, preferencias Harmony, reinicio seguro del juego y orden de presentación del catálogo.

- Changes:
  Actualizado WorkManager a 2.10.0 y migrados los foreground workers a setForeground suspendido.
  Actualizado Harmony a 1.2.6 y añadido fallback seguro de SharedPreferences para evitar crashes de arranque.
  Sustituido el reset nativo de LibretroDroid por relanzamiento seguro del juego desde el menú.
  Invertido el orden del catálogo en filtro All para mostrar Archive.org antes que itch.io.

- Known Issues:
  Los fallos nativos puntuales de framebuffer/Citra quedan en observación por depender de núcleos, drivers o Android Beta.

****************************************************************************************************
02/05/2026 11:46 - Fix ANR/crash y orden del catálogo - Beta_v282
****************************************************************************************************
- Description:
  Correcciones de estabilidad tras revisar reportes de Google Play Console: foreground workers, preferencias Harmony, reinicio seguro del juego y orden de presentación del catálogo.

- Changes:
  Actualizado WorkManager a 2.10.0 y migrados los foreground workers a setForeground suspendido.
  Actualizado Harmony a 1.2.6 y añadido fallback seguro de SharedPreferences para evitar crashes de arranque.
  Sustituido el reset nativo de LibretroDroid por relanzamiento seguro del juego desde el menú.
  Invertido el orden del catálogo en filtro All para mostrar Archive.org antes que itch.io.

- Known Issues:
  Los fallos nativos puntuales de framebuffer/Citra quedan en observación por depender de núcleos, drivers o Android Beta.

****************************************************************************************************
18/04/2026 21:24 - Fix descarga itch.io y checkmarks en catalogo - Beta_v281
****************************************************************************************************
- Description:
  Soluciona IllegalFormatConversionException en descargas de itch.io y Archive.org. Añade indicador visual (checkmark) en tarjetas de catalogo cuando todos los archivos de un pack/juego están descargados. Fix crash IllegalArgumentException en LazyColumn/LazyVerticalGrid por colisión de claves con Paging 3 (reportado en Google Play Console).

- Changes:
  Fix: IllegalFormatConversionException en RomDownloader.kt para CRC calculation
  Feature: fullyDownloadedPackIds en CatalogUiState para tracking de packs completos
  Feature: Checkmark visual en PackCard e ItchGameCard cuando todo está descargado
  Añadido string resource pack_fully_downloaded (EN/ES)
  Observer reactivo en downloads flow para itch.io games
  Fix: IllegalArgumentException en GamesScreen, SearchScreen y FavoritesScreen — reemplazado key fallback a índice por games.itemKey { it.id } de paging-compose

****************************************************************************************************
15/04/2026 13:51 - Fijar Icono y Banner en MainTVActivity - Beta_v280
****************************************************************************************************
- Description:
  Se han asignado explícitamente android:banner, android:icon y android:logo directamente en la declaración de la actividad de Android TV (MainTVActivity) para forzar al Leanback Launcher a usar el icono cuadrado de 512x512 en lugar de heredar el icono circular adaptativo de la app móvil general.

- Changes:
  Añadidas propiedades android:banner, android:icon y android:logo en MainTVActivity dentro de AndroidManifest.xml

****************************************************************************************************
15/04/2026 00:28 - Fix rechazo Google Play: Android TV Icon Quality - Beta_v279
****************************************************************************************************
- Description:
  Añadido android:logo exclusivo para Android TV apuntando a un icono nativo a tamaño completo de 512x512 para resolver el rechazo normativo de Google Play Console sobre el encuadre de la imagen.

- Changes:
  Copiada imagen recursos/imagenes/Logo.png como tv_icon.png en el directorio de drawables xhdpi
  Inyectada etiqueta android:logo=@drawable/tv_icon en AndroidManifest.xml en Application scope

****************************************************************************************************
14/04/2026 16:36 - Fix rechazo Google Play: Foreground Service Policy - Beta_v278
****************************************************************************************************
- Description:
  Corregido rechazo de Google Play por uso incorrecto de foregroundServiceType. GameService cambiado de dataSync a specialUse ya que mantener el emulador vivo durante gameplay no es sincronizacion de datos. Video demo subido a GitHub para declaracion en Play Console.

- Changes:
  GameService foregroundServiceType cambiado de dataSync a specialUse en AndroidManifest.xml
  Añadido permiso FOREGROUND_SERVICE_SPECIAL_USE
  Añadido property PROPERTY_SPECIAL_USE_FGS_SUBTYPE con justificacion para Google Play
  Constante FOREGROUND_SERVICE_TYPE_DATA_SYNC cambiada a FOREGROUND_SERVICE_TYPE_SPECIAL_USE en GameService.kt
  Video demo2.mp4 subido a GitHub para declaracion de permisos en Play Console

****************************************************************************************************
12/04/2026 20:40 - Estabilidad v276: Fix ANRs, Compose Crashes y BadTokenException - Beta_v277
****************************************************************************************************
- Description:
  Corrección de errores críticos reportados en la versión 276. Se optimiza el manejo de hilos, la unicidad de claves en UI y la seguridad de diálogos.

- Changes:
  Movida lógica de comprobación de espacio a Dispatchers.IO en CatalogViewModel para evitar ANRs
  Implementado itemsIndexed en CatalogScreen para garantizar claves únicas y evitar crashes de Compose
  Añadida guarda isFinishing/isDestroyed en ActivityUtils para prevenir BadTokenException

****************************************************************************************************
26/03/2026 03:24 - Actualización de LibretroDroid a 0.13.2 - Beta_v276
****************************************************************************************************
- Description:
  Actualización para incluir thread guards en las interacciones con los cores, previniendo crashes en segundo plano y mejorando la estabilidad del autoguardado.

- Changes:
  Actualizada la dependencia LibretroDroid de 0.13.1 a 0.13.2 en deps.kt

****************************************************************************************************
26/03/2026 02:30 - Fix SAF: Sanitizacion de nombres de archivo para archive.org - Beta_v275
****************************************************************************************************
- Description:
  Corregidos errores al descargar ROMs desde archive.org en modo SAF. Los nombres de archivo con caracteres especiales como [!], (), &, #, etc. (tipicos de ROMs de archive.org) causaban fallos al crear archivos en el Storage Access Framework. Se implementa sanitizacion automatica del displayName para SAF, manteniendo el nombre original en modos Local y SMB.

- Changes:
  Nueva funcion sanitizeForSAF() en RomDownloader: reemplaza caracteres problematicos por _ en nombres de archivo para SAF
  Corregido error Cannot create SAF file con nombres que contienen [!], (), etc.
  Corregido error x != java.lang.String causado por caracteres especiales en el ContentProvider SAF
  Sanitizacion aplicada en createRomsFile, isFileDownloaded, checkBatchFilesExistence, isFileInRomsDir y getDownloadedFilePath
  Modos Local y SMB no se ven afectados: siguen usando el nombre original

****************************************************************************************************
18/03/2026 03:38 - Edicion de metadatos de ROMs - Beta_v274
****************************************************************************************************
- Description:
  Implementada funcionalidad completa de edicion de metadatos de ROMs: pulsacion larga abre menu contextual, dialogo de edicion con busqueda en TheGamesDB, actualizacion de caratula, titulo, descripcion, año y genero. Corregido crash por migracion de BD duplicada. Corregido long click en carrusel.

- Changes:
  Pulsacion larga en carrusel abre bottom sheet con opciones
  Dialogo de edicion con busqueda online en TheGamesDB
  Caratula, descripcion, año y genero se actualizan al seleccionar resultado del scraper
  Flag isUserLocked protege ediciones manuales del indexador
  Corregido crash duplicate column en migracion Room DB
  Eliminado campo developer incorrecto del dialogo de edicion

****************************************************************************************************
18/03/2026 02:24 - Edicion de metadatos de ROMs - Beta_v272
****************************************************************************************************
- Description:
  Implementada funcionalidad completa de edicion de metadatos de ROMs: pulsacion larga abre menu contextual, dialogo de edicion con busqueda en TheGamesDB, actualizacion de caratula, titulo, descripcion, año y genero. Corregido crash por migracion de BD duplicada. Corregido long click en carrusel.

- Changes:
  Pulsacion larga en carrusel abre bottom sheet con opciones
  Dialogo de edicion con busqueda online en TheGamesDB
  Caratula, descripcion, año y genero se actualizan al seleccionar resultado del scraper
  Flag isUserLocked protege ediciones manuales del indexador
  Corregido crash duplicate column en migracion Room DB
  Eliminado campo developer incorrecto del dialogo de edicion

****************************************************************************************************
17/03/2026 22:59 - Game Metadata Search UI & Persistence - Beta_v271
****************************************************************************************************
- Description:
  Implementado sistema de búsqueda online e interactiva para editar metadatos y portadas de los juegos.

- Changes:
  Añadida función de búsqueda de nombres de ROMs contra TheGamesDB
  Renovada la UI del diálogo de edición con sugerencias y búsqueda asíncrona
  Añadido flag isUserLocked a la BD para prevenir que el indexador sobrescriba portadas manualmente confirmadas
  Nueva migración Room DB V12

****************************************************************************************************
17/03/2026 21:57 - Fix Google Drive Sync & OAuth Configuration - Beta_v270
****************************************************************************************************
- Description:
  Implementada corrección para la sincronización de Google Drive. Se configuraron las credenciales OAuth 2.0 correctas (Android y Web) en Google Cloud Console. Se actualizó strings.xml con el Web Client ID válido para permitir el inicio de sesión sin Firebase. Se resolvieron discrepancias de SHA-1 y nombre de paquete en la versión de depuración (.debug).

- Changes:
  Configurado OAuth Client ID de tipo Web en strings.xml
  Resuelto Error 10 (Developer Error) en Google Sign-In
  Añadido soporte para package name con sufijo .debug en consola de Google

****************************************************************************************************
17/03/2026 20:04 - Google Play 16KB Compliance + Fix traducción idiomas - Beta_v269
****************************************************************************************************
- Description:
  Configuración definitiva para cumplir con requisitos de Google Play: librerías nativas comprimidas (useLegacyPackaging=true) y filtrado de idiomas soportados (resourceConfigurations). Se resuelve error de traducción automática a eslovaco (sk) mediante restricción de idiomas empaquetados a los 28 oficialmente soportados.

- Changes:
  Cambio useLegacyPackaging de false a true en app y 20 núcleos (recomendación oficial Google para librerías 4KB)
  Añadido resourceConfigurations en lemuroid-app, lemuroid-app-ext-free y lemuroid-app-ext-play para filtrar idiomas
  Activado auto-incremento de versionCode en build_creator.bat
  Actualizado README.md sección 7: documentación correcta de Runtime Extraction y estrategia 16KB compliance
  Purgado manual de cachés de compilación para forzar regeneración limpia

- Known Issues:
  Error de traducción SK persiste en Google Play Console - requiere desactivación manual en Aumentar usuarios > Traducciones

****************************************************************************************************
04/02/2026 18:10 - Fix Crash UI (LazyColumn) - Beta_v266
****************************************************************************************************
- Description:
  Solucionado crash crítico en Dialog de detalles de ROM (LazyColumn nest in AlertDialog). Migración a Dialog nativo.

****************************************************************************************************
02/02/2026 16:14 - Soporte Split APKs (Google Play) - Beta_v264
****************************************************************************************************
- Description:
  Corrección crítica en GameLoader para soportar la extracción de núcleos desde App Bundles (Split APKs).
  [CHANGE] Activado "Install-Time Delivery" para todos los núcleos: Se instalan junto con la app, eliminando errores de descarga.

****************************************************************************************************
02/02/2026 05:56 - Corrección de Crashes Críticos en Android - Beta_v263
****************************************************************************************************
- Description:
  Solución a cierres inesperados en selección de núcleos y volumen. Estabilidad de build mejorada.

- Changes:
  Fix: Crash en CoresSelection (Flow concurrency)
  Fix: Crash en MusicPlayerManager (MediaPlayer released)
  Fix: Build packaging conflict (extractNativeLibs)

****************************************************************************************************
24/01/2026 11:23 - Fix Libretro Core Loading - Beta_v020
****************************************************************************************************
- Description:
  Reverted 16KB page support configuration (extractNativeLibs=true) to resolve compatibility with GameLoader.

- Changes:
  Reverted extractNativeLibs to true in all manifests
  Removed android.bundle.enableUncompressedNativeLibs from gradle.properties

****************************************************************************************************
24/01/2026 11:22 - Fix Libretro Core Loading - Beta_v019
****************************************************************************************************
- Description:
  Reverted 16KB page support configuration (extractNativeLibs=true) to resolve compatibility with GameLoader.

- Changes:
  Reverted extractNativeLibs to true in all manifests
  Removed android.bundle.enableUncompressedNativeLibs from gradle.properties

****************************************************************************************************
23/01/2026 04:12 - Finalización Migración Source Sets y Fixes UI - Beta_v018
****************************************************************************************************
- Description:
  Implementación completa del sistema de overrides con source sets. Corrección de bugs de UI (Icono, Strings Localizados, SMB Dialog, Navegación Settings). Checkpoint antes de limpieza de legacy.

****************************************************************************************************
23/01/2026 02:42 - Fix crash NoSuchElementException en produccion - Beta_v017
****************************************************************************************************
- Description:
  Corregido bug que causaba crash cuando un sistema no tiene cores configurados (SystemID.UNKNOWN). El fix usa firstOrNull() en lugar de first() para manejar listas vacias de forma segura.

- Changes:
  Fix defensivo en CoresSelection.kt linea 100
  Preparacion para merge con upstream de Lemuroid

****************************************************************************************************
22/12/2025 17:19 - Formulario de Reporte de Bugs Integrado - v016
****************************************************************************************************
- Description:
  Implementado WebView interno para el formulario de reporte de bugs, evitando dependencia del navegador externo

- Changes:
  BugReportActivity con WebView para formulario de bugs
  Subida de imagenes multiples desde el formulario
  Soporte completo para movil y TV
  Eliminada dependencia del navegador externo

****************************************************************************************************
22/12/2025 02:23 - Mejoras UI Catálogo + Sistema de Espacio + v014 Fix - v015
****************************************************************************************************
- Description:
  Corrección del scroll en diálogo de paquetes, verificación de espacio antes de descargas masivas, y aplicación correcta de cambios v014 (LazyColumn, búsqueda, contador).

- Changes:
  Fix: Descripción de paquete ahora scrollable junto con búsqueda y ROMs (no bloquea UI)
  Fix: v014 correctamente aplicado - LazyColumn+items para archivos (evita ANR)
  Fix: v014 correctamente aplicado - Campo de búsqueda dentro del diálogo de paquete
  Fix: v014 correctamente aplicado - Contador de archivos filtrados (X de Y)
  Nuevo: Verificación de espacio disponible antes de descargar Todo
  Nuevo: Diálogo de alerta cuando no hay espacio suficiente (muestra tamaño requerido, disponible y faltante)
  Nuevo: Funciones getAvailableSpace y checkStorageForDownload en RomDownloader
  Nuevo: Icono de libro para Manual en TV (reemplaza interrogación)

****************************************************************************************************
21/12/2025 04:20 - Fix ANR Catálogo + Búsqueda en Paquetes - v014
****************************************************************************************************
- Description:
  Resuelto el error ANR que bloqueaba la app al abrir paquetes grandes (347k+ archivos) en el catálogo. Añadido campo de búsqueda dentro del diálogo de paquete.

- Changes:
  Fix: Cambiado renderizado de archivos de verticalScroll+forEach a LazyColumn+items (solo renderiza ~20 items visibles)
  Nuevo: Campo de búsqueda para filtrar ROMs por nombre dentro de un paquete
  Nuevo: Contador de archivos filtrados cuando hay búsqueda activa

****************************************************************************************************
19/12/2025 01:41 - Mejora del Carrusel Móvil - v013
****************************************************************************************************
- Description:
  Simplificación del carrusel de juegos en la interfaz móvil, eliminando efectos 3D para mejorar la compatibilidad con swipe en todos los dispositivos.

****************************************************************************************************
18/12/2025 22:01 - Corrección Script Versionado - Beta_v012
****************************************************************************************************
- Description:
  Ajustes al script para compatibilidad con este proyecto.

- Changes:
  Regex corregido para detectar Beta_vXXX y Alfa_vXXX
  Eliminado código de version.txt innecesario

****************************************************************************************************
18/12/2025 21:51 - Optimización de UX de Descargas (UI Optimista) - Beta_v011
****************************************************************************************************
- Description:
  Mejora de feedback visual inmediato en descargas de Archive.org. Al pulsar descargar, el indicador cambia instantáneamente a círculo de progreso.

- Changes:
  UI Optimista en Catálogo (CircularProgressIndicator inmediato)
  Corrección de isFileDownloaded para considerar descargas en memoria
  Unificación Mobile/TV via CatalogScreen compartido

****************************************************************************************************
18/12/2025 17:06 - Backup Pre-Fix UI Descargas - v010
****************************************************************************************************
- Description:
  Backup de seguridad solicitado antes de corregir indicadores de descarga y refresco de biblioteca.

****************************************************************************************************
18/12/2025 15:00 - Fix Archive.org Parsing (R8) - Beta_v012
****************************************************************************************************
- Description:
  Corrección crítica en GameLoader para soportar la extracción de núcleos desde App Bundles (Split APKs). Inclusión de herramienta de validación local.
  [CHANGE] Activado "Install-Time Delivery" para todos los núcleos: Se instalan junto con la app, eliminando errores de descarga bajo demanda. [FIX] Añadidas anotaciones @Keep a modelos de datos de Archive.org para evitar ofuscación por R8

****************************************************************************************************
18/12/2025 05:43 - Final Polish & Music Persistence - Beta_v011
****************************************************************************************************
- Description:
  Correcciones finales de UI y persistencia de música para Release Candidate.

- Changes:
  [FIX] Persistencia de estado de música (no se reactiva sola)
  [FIX] Layout botón Guardar en diálogo SMB (móvil)
  [FIX] Scroll y versión en diálogo About

****************************************************************************************************
18/12/2025 04:56 - Fix Critical Mobile Permissions - Beta_v010
****************************************************************************************************
- Description:
  Solución crítica para selección de carpetas en Android 10 (Legacy Storage) y manejo de errores en Catálogo.

- Changes:
  [FIX] Implementado Failback de permisos Legacy (Android 10) para móviles con SAF roto (bypass del selector de sistema)
  [FIX] Manejo explícito de errores de red en ArchiveOrgClient (evita lista vacía silenciosa)
  [FIX] Configuración automática de carpeta EmulAI_Roms si se otorga permiso legado

****************************************************************************************************
17/12/2025 20:53 - Production Release - v009
****************************************************************************************************
- Description:
  Preparacion completa para Google Play y publicacion de codigo fuente.

- Changes:
  Script unificado build_creator.bat
  Automatizacion de GitHub Sync
  Generacion de AAB Signed
  Correccion de reglas R8/ProGuard
  Actualizacion de README.md

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

