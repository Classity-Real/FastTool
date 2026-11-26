package dev.fasttool.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class FlashTemplate(
    val id: String,
    val title: String,
    val description: String,
    val partitions: List<String>
)

val TEMPLATES = listOf(
    FlashTemplate(
        id = "boot_userdata",
        title = "Boot + Userdata",
        description = "Flash boot and userdata partitions",
        partitions = listOf("boot", "userdata")
    ),
    FlashTemplate(
        id = "boot_userdata_cache",
        title = "Boot + Userdata + Cache",
        description = "Flash boot, userdata and cache partitions",
        partitions = listOf("boot", "userdata", "cache")
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplatesSheet(
    onDismiss: () -> Unit,
    onFlash: (template: FlashTemplate, images: Map<String, Uri>) -> Unit
) {
    var selected by remember { mutableStateOf<FlashTemplate?>(null) }
    val imageUris = remember { mutableStateMapOf<String, Uri>() }
    var pickingPartition by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        val part = pickingPartition
        if (uri != null && part != null) {
            imageUris[part] = uri
        }
        pickingPartition = null
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A1A),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = if (selected == null) "Templates" else selected!!.title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFECECEC)
            )

            Text(
                text = if (selected == null) {
                    "Choose a flash profile"
                } else {
                    selected!!.description
                },
                fontSize = 13.sp,
                color = Color(0xFF767676)
            )

            val current = selected
            if (current == null) {
                TEMPLATES.forEach { template ->
                    TemplateCard(
                        template = template,
                        onClick = {
                            selected = template
                            imageUris.clear()
                        }
                    )
                }
            } else {
                current.partitions.forEach { partition ->
                    val uri = imageUris[partition]
                    Surface(
                        onClick = {
                            pickingPartition = partition
                            picker.launch(arrayOf("*/*"))
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF222222),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = partition,
                                    color = Color(0xFFECECEC),
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = uri?.lastPathSegment ?: "Tap to select image",
                                    color = if (uri != null) Color(0xFF4ADE80) else Color(0xFF767676),
                                    fontSize = 12.sp
                                )
                            }
                            if (uri != null) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color(0xFF4ADE80)
                                )
                            }
                        }
                    }
                }

                val allSelected = current.partitions.all { imageUris.containsKey(it) }

                Button(
                    onClick = {
                        if (allSelected) {
                            onFlash(current, imageUris.toMap())
                            onDismiss()
                        }
                    },
                    enabled = allSelected,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE4E4E4),
                        contentColor = Color(0xFF161616),
                        disabledContainerColor = Color(0xFF333333),
                        disabledContentColor = Color(0xFF666666)
                    )
                ) {
                    Text(text = "Flash", fontWeight = FontWeight.SemiBold)
                }

                TextButton(
                    onClick = { selected = null },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(text = "Back", color = Color(0xFF767676))
                }
            }
        }
    }
}

@Composable
private fun TemplateCard(
    template: FlashTemplate,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF222222),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = template.title,
                color = Color(0xFFECECEC),
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
            Text(
                text = template.description,
                color = Color(0xFF767676),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = template.partitions.joinToString(" · "),
                color = Color(0xFF4ADE80),
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
