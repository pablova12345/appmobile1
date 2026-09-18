package bo.gob.gamc.catastro.digid.presentation.resoluciones

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import bo.gob.gamc.catastro.digid.core.theme.*
import bo.gob.gamc.catastro.digid.data.local.entities.ResolucionEntity
import coil3.compose.AsyncImage
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResolucionesScreen(
    onBack: () -> Unit,
    viewModel: ResolucionesViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val resoluciones by viewModel.resoluciones.collectAsState()

    val scannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val gmsResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            val pages = gmsResult?.pages?.mapNotNull { page ->
                val uri = page.imageUri
                val tempFile = File.createTempFile("res_page_", ".jpg", context.cacheDir)
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(tempFile).use { output -> input.copyTo(output) }
                }
                tempFile
            } ?: emptyList()

            if (pages.isNotEmpty()) {
                viewModel.addScannedPages(pages)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Módulo Resoluciones", style = Typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Recargar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Background)
                .padding(paddingValues)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Escanear y Subir Resolución",
                            style = Typography.titleMedium,
                            color = Secondary
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = uiState.nroResolucion,
                            onValueChange = { viewModel.onNroResolucionChange(it) },
                            label = { Text("N° de Resolución (Ej: 123/2026)") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = uiState.nombre,
                            onValueChange = { viewModel.onNombreChange(it) },
                            label = { Text("Nombre / Descripción") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Botón de Escanear páginas
                        OutlinedButton(
                            onClick = {
                                val options = GmsDocumentScannerOptions.Builder()
                                    .setGalleryImportAllowed(true)
                                    .setPageLimit(20)
                                    .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
                                    .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
                                    .build()

                                GmsDocumentScanning.getClient(options)
                                    .getStartScanIntent(context as Activity)
                                    .addOnSuccessListener { intentSender ->
                                        scannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
                                    }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = PrimaryDeep)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Capturar Páginas con Escáner", color = PrimaryDeep)
                        }

                        // Previsualización de páginas escaneadas
                        if (uiState.scannedPages.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "Páginas escaneadas (${uiState.scannedPages.size}):",
                                style = Typography.bodySmall
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                itemsIndexed(uiState.scannedPages) { index, file ->
                                    Box(modifier = Modifier.size(70.dp)) {
                                        AsyncImage(
                                            model = file,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(8.dp))
                                                .border(1.dp, Border, RoundedCornerShape(8.dp))
                                        )
                                        IconButton(
                                            onClick = { viewModel.removePage(index) },
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .size(22.dp)
                                                .background(StatusDanger, RoundedCornerShape(50))
                                        ) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = null,
                                                tint = TextOnDark,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (uiState.errorMessage != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(uiState.errorMessage!!, style = Typography.bodySmall, color = StatusDanger)
                        }

                        if (uiState.successMessage != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(uiState.successMessage!!, style = Typography.bodySmall, color = StatusSuccess)
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = { viewModel.upload() },
                            enabled = !uiState.isUploading,
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryDeep),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            if (uiState.isUploading) {
                                CircularProgressIndicator(color = TextOnDark, modifier = Modifier.size(24.dp))
                            } else {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, tint = TextOnDark)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Subir al Backend de Resoluciones", color = TextOnDark, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
                Text("Mis Resoluciones Registradas", style = Typography.headlineMedium, color = Secondary)
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (resoluciones.isEmpty()) {
                item {
                    Text(
                        "No tiene resoluciones registradas aún.",
                        style = Typography.bodyMedium,
                        color = TextSecondary,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
            } else {
                items(resoluciones, key = { it.idResolucion }) { res ->
                    ResolucionCard(res)
                }
            }
        }
    }
}

@Composable
fun ResolucionCard(res: ResolucionEntity) {
    val (badgeBg, badgeText, badgeColor) = when (res.estado) {
        "listo" -> Triple(StatusSuccess.copy(alpha = 0.15f), "Listo", StatusSuccess)
        "en_proceso" -> Triple(PrimaryDeep.copy(alpha = 0.15f), "En proceso", PrimaryDeep)
        else -> Triple(StatusWarning.copy(alpha = 0.15f), "Pendiente", StatusWarning)
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Res. ${res.nroResolucion}",
                    style = Typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    color = badgeBg,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = badgeText,
                        color = badgeColor,
                        style = Typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(res.nombre, style = Typography.bodyMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Páginas: ${res.totalPaginas} | Fecha: ${res.fechaCreacion.take(10)}",
                style = Typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}
