package bo.gob.gamc.catastro.digid.presentation.home.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import bo.gob.gamc.catastro.digid.core.theme.*

@Composable
fun ModalDescripcionDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (descripcion: String) -> Unit
) {
    if (!visible) return

    var descripcion by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Descripción del Documento", style = Typography.titleMedium) },
        text = {
            Column {
                Text(
                    "Ingrese una descripción o referencia para identificar este documento:",
                    style = Typography.bodySmall
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    placeholder = { Text("Ej: Plano de loteamiento, Cédula...") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(descripcion)
                    descripcion = ""
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryDeep)
            ) {
                Text("Guardar", color = TextOnDark)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = TextSecondary)
            }
        }
    )
}

@Composable
fun WarningDialog(
    visible: Boolean,
    title: String = "¿Está seguro?",
    message: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    if (!visible) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = Typography.titleMedium, color = StatusDanger) },
        text = { Text(message, style = Typography.bodyLarge) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = StatusDanger)
            ) {
                Text("Eliminar", color = TextOnDark)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = TextSecondary)
            }
        }
    )
}
