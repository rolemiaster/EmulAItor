package com.swordfish.lemuroid.app.mobile.feature.settings.about

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.swordfish.lemuroid.BuildConfig

@Composable
fun AboutScreen(
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Header con logo/icon
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "EmulAItor",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Versión ${BuildConfig.VERSION_NAME}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Origen del software
        AboutSection(title = "Origen del software") {
            Text(
                text = "Esta aplicación es un fork del proyecto de código abierto Lemuroid, distribuido bajo licencia GPL-3.0.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Se han añadido funciones de exploración de contenidos disponibles públicamente en Archive.org.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Código fuente
        AboutSection(title = "Código fuente") {
            Text(
                text = "El código fuente de esta versión modificada está disponible bajo licencia GPL-3.0.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/rolemiaster/EmulAItor"))
                    context.startActivity(intent)
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Code,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Ver código fuente (GitHub)")
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Licencias de terceros
        AboutSection(title = "Licencias de terceros") {
            LicenseItem("Lemuroid", "GPL-3.0", "Swordfish90")
            LicenseItem("LibretroDroid", "GPL-3.0", "Swordfish90")
            LicenseItem("Libretro Cores", "Varias (GPL, LGPL, MIT)", "libretro")
            LicenseItem("Jetpack Compose", "Apache 2.0", "Google")
            LicenseItem("OkHttp", "Apache 2.0", "Square")
            LicenseItem("Gson", "Apache 2.0", "Google")
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Aviso legal
        AboutSection(title = "Aviso legal") {
            Text(
                text = "Esta aplicación no incluye ROMs ni descarga contenido automáticamente. " +
                        "La función de exploración solo muestra colecciones disponibles públicamente en Archive.org. " +
                        "El usuario es responsable de comprobar si posee los derechos para acceder o usar cualquier archivo externo.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun AboutSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            val columnScope = this
            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
                columnScope.content()
            }
        }
    }
}


@Composable
private fun LicenseItem(
    name: String,
    license: String,
    author: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = name,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            )
            Text(
                text = "por $author",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
        Text(
            text = license,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.tertiary
        )
    }
}
