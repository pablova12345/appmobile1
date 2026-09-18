package bo.gob.gamc.catastro.digid.presentation.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import bo.gob.gamc.catastro.digid.core.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIStudyScreen(
    fileName: String,
    onBack: () -> Unit,
    viewModel: AIStudyViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(fileName) {
        viewModel.initForDocument(fileName)
    }

    val matchedTipo = state.catalog.find { it.code == state.classifiedType }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Estudiar con la IA", style = Typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
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
            // Tarjeta de Clasificación
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Secondary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Clasificación del Documento", style = Typography.titleMedium, color = Secondary)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (state.isClassifying) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(color = PrimaryDeep, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Analizando documento con IA...", style = Typography.bodySmall)
                            }
                        } else if (state.classifiedType != null) {
                            Text(
                                text = matchedTipo?.title ?: state.classifiedType!!,
                                style = Typography.headlineMedium,
                                color = PrimaryDeep,
                                fontWeight = FontWeight.Bold
                            )
                            if (!state.classificationReason.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = state.classificationReason!!,
                                    style = Typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        } else if (state.classificationError != null) {
                            Text(
                                text = state.classificationError!!,
                                style = Typography.bodySmall,
                                color = StatusDanger
                            )
                        } else {
                            Text(
                                "No se detectaron páginas escaneadas para este documento.",
                                style = Typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            // Preguntas sugeridas del catálogo
            if (matchedTipo != null && matchedTipo.questions.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(20.dp))
                    Text("Preguntas sobre este documento", style = Typography.titleMedium, color = Secondary)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                items(matchedTipo.questions) { pregunta ->
                    OutlinedCard(
                        onClick = { viewModel.askQuestion(pregunta) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.outlinedCardColors(containerColor = Surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Icon(Icons.Default.HelpOutline, contentDescription = null, tint = PrimaryDeep, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(pregunta, style = Typography.bodyMedium, color = TextPrimary)
                        }
                    }
                }
            }

            // Respuestas generadas
            if (state.answers.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(20.dp))
                    Text("Respuestas de la IA", style = Typography.titleMedium, color = Secondary)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                items(state.answers) { answer ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "P: ${answer.pregunta}",
                                style = Typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Secondary
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            if (answer.isLoading) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(color = PrimaryDeep, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Generando respuesta...", style = Typography.bodySmall)
                                }
                            } else if (answer.respuesta != null) {
                                Text(
                                    text = answer.respuesta,
                                    style = Typography.bodyMedium,
                                    color = TextPrimary
                                )
                            } else if (answer.error != null) {
                                Text(
                                    text = answer.error,
                                    style = Typography.bodySmall,
                                    color = StatusDanger
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
