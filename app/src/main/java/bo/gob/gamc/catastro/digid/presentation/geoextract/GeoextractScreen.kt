package bo.gob.gamc.catastro.digid.presentation.geoextract

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import bo.gob.gamc.catastro.digid.core.theme.*
import bo.gob.gamc.catastro.digid.data.remote.dto.GeoextractDto
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeoextractScreen(
    onBack: () -> Unit,
    viewModel: GeoextractViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()

    var currentPhotoPath by rememberSaveable { mutableStateOf<String?>(null) }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && !currentPhotoPath.isNullOrEmpty()) {
            val file = File(currentPhotoPath!!)
            if (file.exists() && file.length() > 0) {
                viewModel.uploadCaptura(file)
            } else {
                viewModel.setError("No se pudo guardar la fotografía capturada.")
            }
        }
    }

    fun launchCamera() {
        try {
            val file = File.createTempFile("geo_capture_", ".jpg", context.cacheDir)
            currentPhotoPath = file.absolutePath
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
            takePictureLauncher.launch(uri)
        } catch (e: Exception) {
            viewModel.setError("Error al abrir la cámara: ${e.localizedMessage ?: "No se pudo iniciar la cámara"}")
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launchCamera()
        } else {
            viewModel.setError("Se requiere permiso de la cámara para tomar fotografías del plano catastral.")
        }
    }

    val pickGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val tempFile = File.createTempFile("geo_gallery_", ".jpg", context.cacheDir)
                context.contentResolver.openInputStream(uri)?.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                if (tempFile.exists() && tempFile.length() > 0) {
                    viewModel.uploadCaptura(tempFile)
                } else {
                    viewModel.setError("No se pudo leer la imagen seleccionada.")
                }
            } catch (e: Exception) {
                viewModel.setError("Error al cargar la imagen: ${e.localizedMessage ?: "Error desconocido"}")
            }
        }
    }

    fun handleTakePhotoClick() {
        viewModel.clearMessages()
        val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            launchCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Geoextract — ERP", style = Typography.titleMedium) },
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Map, contentDescription = null, tint = Secondary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Captura Cartográfica", style = Typography.titleMedium, color = Secondary)
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Tome una fotografía nítida del plano catastral o selecciónela de su galería. El recorte del área de coordenadas y el OCR se procesan en la plataforma web.",
                            style = Typography.bodySmall,
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { handleTakePhotoClick() },
                            enabled = !state.isUploading,
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryDeep),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            if (state.isUploading) {
                                CircularProgressIndicator(color = TextOnDark, modifier = Modifier.size(24.dp))
                            } else {
                                Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = TextOnDark)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Tomar Foto y Enviar al ERP", color = TextOnDark, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = {
                                viewModel.clearMessages()
                                pickGalleryLauncher.launch("image/*")
                            },
                            enabled = !state.isUploading,
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = PrimaryDeep)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Seleccionar de Galería", color = PrimaryDeep, fontWeight = FontWeight.SemiBold)
                        }

                        if (state.errorMessage != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(state.errorMessage!!, style = Typography.bodySmall, color = StatusDanger)
                        }

                        if (state.successMessage != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(state.successMessage!!, style = Typography.bodySmall, color = StatusSuccess)
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
                Text("Capturas Pendientes de Procesamiento", style = Typography.headlineMedium, color = Secondary)
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (state.pendientes.isEmpty()) {
                item {
                    Text(
                        "No tiene capturas pendientes en el ERP.",
                        style = Typography.bodyMedium,
                        color = TextSecondary,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
            } else {
                items(state.pendientes) { cap ->
                    CapturaCard(cap)
                }
            }
        }
    }
}

@Composable
fun CapturaCard(captura: GeoextractDto) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Image, contentDescription = null, tint = PrimaryDeep, modifier = Modifier.size(36.dp))
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                val idDisplay = if (captura.idCaptura.length > 12) {
                    "${captura.idCaptura.take(12)}..."
                } else {
                    captura.idCaptura.ifEmpty { "Sin ID" }
                }
                Text(
                    text = "Captura ID: $idDisplay",
                    style = Typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                val fechaDisplay = captura.fechaCreacion?.take(16)?.replace('T', ' ') ?: "Sin fecha"
                Text(
                    text = "Fecha: $fechaDisplay",
                    style = Typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}
