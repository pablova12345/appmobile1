package bo.gob.gamc.catastro.digid.presentation.pdf

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import bo.gob.gamc.catastro.digid.core.theme.*
import coil3.compose.AsyncImage
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(
    filePath: String,
    fileName: String,
    onBack: () -> Unit,
    onStudyWithAi: () -> Unit,
    viewModel: PdfViewerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(filePath) {
        viewModel.loadDocument(filePath)
    }

    val file = remember(filePath) { File(filePath) }

    fun shareOrOpenExternal() {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, if (state.isPdf) "application/pdf" else "image/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Abrir con..."))
        } catch (_: Exception) {}
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = fileName,
                        style = Typography.titleMedium,
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    IconButton(onClick = { shareOrOpenExternal() }) {
                        Icon(Icons.Default.Share, contentDescription = "Compartir", tint = PrimaryDeep)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)
            )
        },
        bottomBar = {
            if (state.isPdf && state.totalPages > 1) {
                Surface(
                    color = Surface,
                    tonalElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewModel.prevPage() },
                            enabled = state.currentPage > 0
                        ) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Anterior")
                        }

                        Text(
                            text = "Página ${state.currentPage + 1} de ${state.totalPages}",
                            style = Typography.bodyMedium
                        )

                        IconButton(
                            onClick = { viewModel.nextPage() },
                            enabled = state.currentPage < state.totalPages - 1
                        ) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Siguiente")
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Background)
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Botón "Estudiar con la IA"
            if (state.isPdf) {
                Button(
                    onClick = onStudyWithAi,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = Secondary),
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .height(44.dp)
                ) {
                    Icon(Icons.Default.Psychology, contentDescription = null, tint = TextOnDark)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Estudiar con la IA", color = TextOnDark, style = Typography.labelLarge)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(color = PrimaryDeep)
                } else if (state.isPdf) {
                    state.currentPageBitmap?.let { bitmap ->
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Página del PDF",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    AsyncImage(
                        model = file,
                        contentDescription = "Vista previa",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
