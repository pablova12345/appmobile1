package bo.gob.gamc.catastro.digid.presentation.home.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import bo.gob.gamc.catastro.digid.R
import bo.gob.gamc.catastro.digid.core.theme.*
import bo.gob.gamc.catastro.digid.data.local.entities.DocumentEntity
import coil3.compose.AsyncImage
import java.io.File

@Composable
fun DocumentCard(
    document: DocumentEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val isImage = document.fileName.endsWith(".jpg", ignoreCase = true) ||
            document.fileName.endsWith(".png", ignoreCase = true) ||
            document.fileName.endsWith(".jpeg", ignoreCase = true)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { onClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Thumbnail
            if (isImage) {
                AsyncImage(
                    model = File(document.filePath),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.pdf_icon),
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Información
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = document.fileName,
                    style = Typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = document.descripcion,
                    style = Typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!document.tramiteAsociado.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Trámite: ${document.tramiteAsociado}",
                        style = Typography.bodySmall,
                        color = PrimaryDeep
                    )
                }
            }

            // Botón Eliminar
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Eliminar",
                    tint = StatusDanger
                )
            }
        }
    }
}
