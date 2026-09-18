package bo.gob.gamc.catastro.digid.presentation.home

import android.app.Activity
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import bo.gob.gamc.catastro.digid.core.theme.*
import bo.gob.gamc.catastro.digid.data.local.entities.DocumentEntity
import bo.gob.gamc.catastro.digid.presentation.home.components.DocumentCard
import bo.gob.gamc.catastro.digid.presentation.home.components.ModalDescripcionDialog
import bo.gob.gamc.catastro.digid.presentation.home.components.WarningDialog
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import java.io.File
import java.io.FileOutputStream

@Composable
fun HomeScreen(
    onNavigateToPdf: (filePath: String, fileName: String) -> Unit,
    onNavigateToResoluciones: () -> Unit,
    onNavigateToGeoextract: () -> Unit,
    onLoggedOut: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val documents by viewModel.documents.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val nroTramite by viewModel.nroTramite.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()

    var showDescModal by remember { mutableStateOf(false) }
    var documentToDelete by remember { mutableStateOf<DocumentEntity?>(null) }

    // Launcher para ML Kit Document Scanner
    val scannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val gmsResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            val pages = gmsResult?.pages?.mapNotNull { page ->
                val uri = page.imageUri
                val tempFile = File.createTempFile("scan_page_", ".jpg", context.cacheDir)
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(tempFile).use { output -> input.copyTo(output) }
                }
                tempFile
            } ?: emptyList()

            if (pages.isNotEmpty()) {
                viewModel.pendingScanImages = pages
                showDescModal = true
            }
        }
    }

    // Launcher para Adjuntar Archivos (SAF)
    val attachLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            var fileName = "archivo_${System.currentTimeMillis()}"
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex != -1) {
                    fileName = cursor.getString(nameIndex)
                }
            }
            viewModel.pendingAttachedFile = Pair(uri, fileName)
            showDescModal = true
        }
    }

    Scaffold(
        bottomBar = {
            Surface(
                color = Surface,
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Cerrar sesión
                    IconButton(onClick = { viewModel.logout(onLoggedOut) }) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Salir", tint = TextSecondary)
                    }

                    // Botón Principal de Escaneo (ML Kit)
                    Button(
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
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryDeep),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Icon(Icons.Default.DocumentScanner, contentDescription = null, tint = TextOnDark)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Escanear", color = TextOnDark, fontWeight = FontWeight.Bold)
                    }

                    // Adjuntar Archivo
                    IconButton(onClick = { attachLauncher.launch("*/*") }) {
                        Icon(Icons.Default.AttachFile, contentDescription = "Adjuntar", tint = PrimaryDeep)
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Background)
                .padding(paddingValues)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Banner de Sin Conexión
            if (!isOnline) {
                Surface(
                    color = StatusWarning.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(10.dp)
                    ) {
                        Icon(Icons.Default.WifiOff, contentDescription = null, tint = StatusWarning)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sin conexión a la red municipal.", style = Typography.bodySmall, color = StatusWarning)
                    }
                }
            }

            Text(
                text = "Documentos escaneados",
                style = Typography.headlineMedium,
                color = Secondary,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Accesos rápidos a submódulos
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onNavigateToResoluciones,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Surface),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.TableChart, contentDescription = null, tint = PrimaryDeep, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Resoluciones", style = Typography.bodySmall, color = TextPrimary)
                }
                OutlinedButton(
                    onClick = onNavigateToGeoextract,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Surface),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Map, contentDescription = null, tint = PrimaryDeep, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Geoextract", style = Typography.bodySmall, color = TextPrimary)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Filtro por N° de trámite
            OutlinedTextField(
                value = nroTramite,
                onValueChange = { viewModel.onTramiteChange(it) },
                label = { Text("Número de trámite (opcional)") },
                placeholder = { Text("Ej. 12345/2026") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryDeep,
                    unfocusedBorderColor = Border,
                    focusedContainerColor = Surface,
                    unfocusedContainerColor = Surface
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Buscador por nombre/descripción
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchChange(it) },
                placeholder = { Text("Buscar en documentos...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Limpiar")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryDeep,
                    unfocusedBorderColor = Border,
                    focusedContainerColor = Surface,
                    unfocusedContainerColor = Surface
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Lista de documentos
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryDeep)
                }
            } else if (documents.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (nroTramite.isNotBlank()) "No hay documentos asociados a este trámite."
                        else "No hay documentos aún. Presione 'Escanear' o 'Adjuntar'.",
                        style = Typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(documents, key = { it.id }) { doc ->
                        DocumentCard(
                            document = doc,
                            onClick = { onNavigateToPdf(doc.filePath, doc.fileName) },
                            onDelete = { documentToDelete = doc }
                        )
                    }
                }
            }
        }
    }

    // Modal para ingresar descripción
    ModalDescripcionDialog(
        visible = showDescModal,
        onDismiss = {
            showDescModal = false
            viewModel.pendingScanImages = emptyList()
            viewModel.pendingAttachedFile = null
        },
        onConfirm = { desc ->
            showDescModal = false
            if (viewModel.pendingScanImages.isNotEmpty()) {
                viewModel.savePendingScan(desc)
            } else if (viewModel.pendingAttachedFile != null) {
                viewModel.savePendingAttachment(desc)
            }
        }
    )

    // Modal de confirmación de eliminación
    WarningDialog(
        visible = documentToDelete != null,
        title = "Eliminar Documento",
        message = "¿Desea eliminar permanentemente '${documentToDelete?.fileName}'?",
        onDismiss = { documentToDelete = null },
        onConfirm = {
            documentToDelete?.let { viewModel.deleteDocument(it) }
            documentToDelete = null
        }
    )
}
